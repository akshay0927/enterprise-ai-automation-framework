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
    private final Map<String,Integer> carts = new HashMap<>();

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
            boolean add = path.equals("/api/cart/items/notebook");
            String method = add ? "POST" : "GET";
            int status = 200;
            String type = "application/json";
            String body;
            if (!method.equals(request.getRequestMethod())) {
                status = 405;
                request.getResponseHeaders().set("Allow", method);
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
                    carts.put(id, 0);
                    request.getResponseHeaders().set("Set-Cookie", "guest=" + id + "; Path=/; HttpOnly; SameSite=Strict");
                }
                int count = add ? carts.merge(id, 1, Integer::sum) : carts.get(id);
                body = "{\"quantity\":" + count + ",\"totalCents\":" + count * 500L + "}";
            } else if (path.equals("/health")) {
                body = "{\"status\":\"up\"}";
            } else if (path.equals("/api/products")) {
                body = "[{\"id\":\"notebook\",\"name\":\"Notebook\",\"unitPriceCents\":500}]";
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
                      el('add').disabled = !match;
                    }
                    el('search').oninput = filter;
                    el('add').onclick = async () => cart(await fetch('/api/cart/items/notebook',{method:'POST'}));
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