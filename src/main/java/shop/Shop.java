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
    private final java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(4);
    private final Runnable beforeCheckout;
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

    private record Session(String user, java.time.Instant expires) {}
    private final java.time.Clock clock;
    private final Map<String,Session> sessions = new HashMap<>();
    private final Map<String,String> passwords = Map.of("alice", UUID.randomUUID().toString(), "bob", UUID.randomUUID().toString());
    private final Map<String,Map<String,Integer>> accounts = new HashMap<>();
    private final Map<String,Integer> remaining = new HashMap<>();
    private final Map<String,java.util.List<String>> orders = new HashMap<>();

    // Test fixture provisioning only; never exposed through HTTP.
    String fixturePassword(String user) { return passwords.get(user); }

    private static String guestId(HttpExchange request) {
        for (String header : request.getRequestHeaders().getOrDefault("Cookie", java.util.List.of()))
            for (String field : header.split(";"))
                if (field.strip().startsWith("guest=")) return field.strip().substring(6);
        return "";
    }

    private String productJson(Product p) {
        return new Product(p.id(), p.name(), p.cents(), remaining.get(p.id())).json();
    }

    public Shop() throws IOException { this(java.time.Clock.systemUTC()); }

    Shop(java.time.Clock clock) throws IOException { this(clock, () -> {}); }

    Shop(java.time.Clock clock, Runnable beforeCheckout) throws IOException {
        this.beforeCheckout = beforeCheckout;
        this.clock = clock;
        catalog.forEach(p -> remaining.put(p.id(), p.stock()));
        server = bindBrowserFixture();
        server.setExecutor(workers);
        server.createContext("/", request -> {
            if (request.getRequestMethod().equals("POST") && request.getRequestURI().getPath().equals("/api/checkout")) beforeCheckout.run();
            handle(request);
        });
        server.start();
    }

    private static HttpServer bindBrowserFixture() throws IOException {
        // Avoid browser-restricted ports that an OS ephemeral allocation may select.
        for (int attempt = 0; attempt < 32; attempt++) {
            int port = java.util.concurrent.ThreadLocalRandom.current().nextInt(20000, 30000);
            try { return HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0); }
            catch (java.net.BindException occupied) { if (attempt == 31) throw occupied; }
        }
        throw new IOException("Could not allocate fixture port");
    }

    public static void main(String[] args) throws Exception {
        var shop = new Shop();
        Runtime.getRuntime().addShutdownHook(new Thread(shop::close));
        System.out.println("Fixture shop: " + shop.url());
        if (java.util.Arrays.asList(args).contains("--open") && java.awt.Desktop.isDesktopSupported())
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(shop.url()));
        new java.util.concurrent.CountDownLatch(1).await();
    }

    public String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    public void close() { server.stop(0); workers.shutdownNow(); }

    private synchronized void handle(HttpExchange request) throws IOException {
        try (request) {
            String path = request.getRequestURI().getPath();
            boolean add = path.startsWith("/api/cart/items/");
            boolean login = path.equals("/api/login"), logout = path.equals("/api/logout"), checkout = path.equals("/api/checkout");
            String method = add || login || logout || checkout ? "POST" : "GET";
            int status = 200;
            String type = "application/json";
            String body;
            sessions.values().removeIf(session -> !session.expires().isAfter(clock.instant()));
            String auth = request.getRequestHeaders().getFirst("Authorization");
            String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
            Session session = sessions.get(token);
            if (!(add && java.util.Set.of("DELETE","PUT").contains(request.getRequestMethod())) && !method.equals(request.getRequestMethod())) {
                status = 405;
                request.getResponseHeaders().set("Allow", add ? "POST, PUT, DELETE" : method);
                body = "{}";
            } else if (login) {
                String credentials = new String(request.getRequestBody().readNBytes(257), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length != 2 || credentials.length() > 256 || !parts[1].equals(passwords.get(parts[0]))) {
                    status = 401;
                    body = "{}";
                } else {
                    String user = parts[0];
                    var account = accounts.computeIfAbsent(user, key -> new HashMap<>());
                    var guest = carts.get(guestId(request));
                    if (guest != null && guest.entrySet().stream().anyMatch(e ->
                            account.getOrDefault(e.getKey(), 0) + e.getValue() > remaining.get(e.getKey()))) {
                        status = 409;
                        body = "{}";
                    } else {
                        if (guest != null) {
                            guest.forEach((key, value) -> account.merge(key, value, Integer::sum));
                            carts.remove(guestId(request));
                        }
                        String issued = UUID.randomUUID().toString();
                        sessions.put(issued, new Session(user, clock.instant().plusSeconds(900)));
                        body = "{\"token\":\"" + issued + "\"}";
                    }
                }
            } else if (session == null && (auth != null || logout || checkout || path.equals("/api/profile") || path.equals("/api/orders"))) {
                status = 401;
                body = "{}";
            } else if (logout) {
                sessions.remove(token);
                body = "{}";
            } else if (path.equals("/api/profile")) {
                body = "{\"user\":\"" + session.user() + "\"}";
            } else if (path.equals("/api/orders")) {
                body = "[" + String.join(",", orders.getOrDefault(session.user(), java.util.List.of())) + "]";
            } else if (checkout) {
                var cart = accounts.get(session.user());
                if (cart.isEmpty() || cart.entrySet().stream().anyMatch(e -> e.getValue() > remaining.get(e.getKey()))) {
                    status = 409;
                    body = "{}";
                } else {
                    body = "{\"id\":\"" + UUID.randomUUID() + "\",\"cart\":" + cartJson(cart) + "}";
                    cart.forEach((key, value) -> remaining.compute(key, (k, available) -> available - value));
                    orders.computeIfAbsent(session.user(), key -> new java.util.ArrayList<>()).add(body);
                    cart.clear();
                    status = 201;
                }
            } else if (add || path.equals("/api/cart")) {
                Map<String,Integer> cart;
                if (session != null) cart = accounts.get(session.user());
                else {
                    String id = guestId(request);
                    if (!carts.containsKey(id)) {
                        id = UUID.randomUUID().toString();
                        carts.put(id, new HashMap<>());
                        request.getResponseHeaders().set("Set-Cookie", "guest=" + id + "; Path=/; HttpOnly; SameSite=Strict");
                    }
                    cart = carts.get(id);
                }
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
                        if (status == 200 && qty > remaining.get(item)) status = 409;
                        if (status == 200) cart.put(item, qty);
                    }
                }
                body = status == 200 ? cartJson(cart) : "{\"error\":\"cart update rejected\"}";
            } else if (path.equals("/health")) {
                body = "{\"status\":\"up\"}";
            } else if (path.equals("/api/products")) {
                body = catalog.stream().map(this::productJson).collect(java.util.stream.Collectors.joining(",", "[", "]"));
            } else if (path.startsWith("/api/products/")) {
                var found = catalog.stream().filter(p -> path.equals("/api/products/" + p.id())).findFirst();
                status = found.isPresent() ? 200 : 404;
                body = found.map(this::productJson).orElse("{}");
            } else if (path.equals("/")) {
                type = "text/html";
                body = """
                    <!doctype html><html lang="en" data-state="loading"><title>Fixture shop</title><h1>Catalog</h1>
                    <label>Search <input id="search"></label><p id="product"></p>
                    <button id="add" disabled>Add notebook</button><output id="quantity"></output>
                    <form id="login"><label>User <input id="user" required></label>
                    <label>Password <input id="password" type="password" required></label><button>Sign in</button></form>
                    <output id="profile">Guest</output><button id="logout" hidden>Sign out</button>
                    <button id="checkout" hidden>Checkout</button><output id="message" aria-live="polite"></output>
                    <script type="module">
                    const el = id => document.getElementById(id);
                    async function cart(response) { el('quantity').textContent = (await response.json()).quantity; }
                    let token = null;
                    function api(path, options = {}) {
                      return fetch(path, {...options, headers: token ? {Authorization: 'Bearer ' + token} : {}});
                    }
                    function signedIn(user) {
                      el('profile').textContent = user || 'Guest';
                      el('login').hidden = !!user;
                      el('checkout').hidden = el('logout').hidden = !user;
                    }
                    el('login').onsubmit = async event => {
                      event.preventDefault();
                      const response = await api('/api/login', {method:'POST', body:el('user').value + ':' + el('password').value});
                      el('password').value = '';
                      if (!response.ok) { el('message').textContent = 'Sign-in failed'; return; }
                      token = (await response.json()).token;
                      const profile = await (await api('/api/profile')).json();
                      await cart(await api('/api/cart'));
                      el('message').textContent = '';
                      signedIn(profile.user);
                    };
                    el('checkout').onclick = async () => {
                      const response = await api('/api/checkout',{method:'POST'});
                      if (!response.ok) { el('message').textContent = 'Checkout unavailable'; return; }
                      const order = await response.json();
                      await cart(await api('/api/cart'));
                      el('message').textContent = 'Order confirmed: ' + order.id;
                    };
                    el('logout').onclick = async () => {
                      if (!(await api('/api/logout',{method:'POST'})).ok) return;
                      token = null;
                      await cart(await api('/api/cart'));
                      el('message').textContent = '';
                      signedIn(null);
                    };
                    try {
                    const catalogResponse = await fetch('/api/products');
                    if (!catalogResponse.ok) throw new Error('Catalog unavailable');
                    const products = await catalogResponse.json();
                    const initialCart = await fetch('/api/cart');
                    if (!initialCart.ok) throw new Error('Cart unavailable');
                    await cart(initialCart);
                    function filter() {
                      const match = products.find(p => p.name.toLowerCase().includes(el('search').value.trim().toLowerCase()));
                      el('product').textContent = match?.name ?? 'No matching products';
                      el('add').disabled = !match || match.stock === 0;
                      el('add').dataset.product = match?.id ?? '';
                      el('add').textContent = match ? 'Add ' + match.name : 'Add product';
                    }
                    el('search').oninput = filter;
                    el('add').onclick = async () => cart(await api('/api/cart/items/' + el('add').dataset.product,{method:'POST'}));
                    filter();
                    document.documentElement.dataset.state = 'ready';
                    } catch (failure) {
                      document.documentElement.dataset.state = 'error';
                      el('message').textContent = 'Shop initialization failed';
                    }
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