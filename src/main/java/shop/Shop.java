package shop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Shop implements AutoCloseable {
    private final HttpServer server;
    private final Map<String,Map<String,Integer>> carts = new HashMap<>();

    private record Product(String id, String name, int cents, int stock) {
        String json() {
            return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"unitPriceCents\":" + cents + ",\"stock\":" + stock + "}";
        }
    }
    private static final java.util.List<Product> catalog = java.util.List.of(
        new Product("notebook", "Notebook", 500, 8),
        new Product("pen", "Ink pen", 175, 4),
        new Product("folder", "Folder", 250, 0));

    private static String cartJson(Map<String,Integer> cart) {
        int units = 0, total = 0;
        var lines = new java.util.StringJoiner(",", "[", "]");
        for (Product product : catalog) {
            int qty = cart.getOrDefault(product.id(), 0);
            if (qty == 0) continue;
            units += qty;
            total += qty * product.cents();
            lines.add("{\"productId\":\"" + product.id() + "\",\"quantity\":" + qty + "}");
        }
        return "{\"quantity\":" + units + ",\"items\":" + lines + ",\"totalCents\":" + total + "}";
    }

    public Shop() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void close() { server.stop(0); }

    private synchronized void handle(HttpExchange request) throws IOException {
        try (request) {
            String path = request.getRequestURI().getPath();
            boolean add = path.startsWith("/api/cart/items/");
            String method = add ? "POST" : "GET";
            int status = 200;
            String type = "application/json";
            String body;
            if (!(add && java.util.Set.of("DELETE","PUT").contains(request.getRequestMethod())) && !method.equals(request.getRequestMethod())) {
                status = 405;
                request.getResponseHeaders().set("Allow", add ? "POST, PUT, DELETE" : method);
                body = "{}";
            } else if (add || path.equals("/api/cart")) {
                String id = "";
                for (String header : request.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
                    for (String field : header.split(";")) {
                        if (field.strip().startsWith("guest=")) id = field.strip().substring(6);
                    }
                }
                if (!carts.containsKey(id)) {
                    id = UUID.randomUUID().toString();
                    carts.put(id, new HashMap<>());
                    request.getResponseHeaders().set("Set-Cookie", "guest=" + id + "; Path=/; HttpOnly; SameSite=Strict");
                }
                var cart = carts.get(id);
                if (add) {
                    String item = path.substring("/api/cart/items/".length());
                    var product = catalog.stream().filter(p -> p.id().equals(item)).findFirst();
                    if (product.isEmpty()) status = 404;
                    else if (request.getRequestMethod().equals("DELETE")) cart.remove(item);
                    else {
                        int qty = cart.getOrDefault(item, 0) + 1;
                        if (request.getRequestMethod().equals("PUT")) {
                            byte[] input = request.getRequestBody().readNBytes(9);
                            String number = new String(input, StandardCharsets.US_ASCII);
                            if (!number.matches("[1-9][0-9]{0,6}")) status = 400;
                            else qty = Integer.parseInt(number);
                        }
                        if (status == 200 && qty > product.get().stock()) status = 409;
                        if (status == 200) cart.put(item, qty);
                    }
                }
                body = status == 200 ? cartJson(cart) : "{\"error\":\"cart update rejected\"}";
            } else if (path.equals("/health")) {
                body = "{\"status\":\"up\"}";
            } else if (path.equals("/api/products")) {
                body = catalog.stream().map(Product::json).collect(java.util.stream.Collectors.joining(",", "[", "]"));
            } else if (path.startsWith("/api/products/")) {
                var found = catalog.stream().filter(p -> path.equals("/api/products/" + p.id())).findFirst();
                status = found.isPresent() ? 200 : 404;
                body = found.map(Product::json).orElse("{}");
            } else if (path.equals("/")) {
                type = "text/html";
                body = """
                    <!doctype html><html lang="en"><title>Fixture shop</title><h1>Catalog</h1>
                    <label>Search <input id="search"></label><p id="product"></p>
                    <button id="add" disabled>Add notebook</button><output id="quantity"></output>
                    <script type="module">
                    const el = id => document.getElementById(id);
                    const products = await (await fetch('/api/products')).json();
                    async function cart(response) { el('quantity').textContent = (await response.json()).quantity; }
                    await cart(await fetch('/api/cart'));
                    function filter() {
                      const match = products.find(p => p.name.toLowerCase().includes(el('search').value.trim().toLowerCase()));
                      el('product').textContent = match?.name ?? 'No matching products';
                      el('add').disabled = !match || match.stock === 0;
                      el('add').dataset.product = match?.id ?? '';
                      el('add').textContent = match ? 'Add ' + match.name : 'Add product';
                    }
                    el('search').oninput = filter;
                    el('add').onclick = async () => cart(await fetch('/api/cart/items/' + el('add').dataset.product,{method:'POST'}));
                    filter();
                    </script></html>
                    """;
            } else {
                status = 404;
                body = "{}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            request.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
            request.getResponseHeaders().set("Cache-Control", "no-store");
            request.sendResponseHeaders(status, bytes.length);
            request.getResponseBody().write(bytes);
        }
    }
}