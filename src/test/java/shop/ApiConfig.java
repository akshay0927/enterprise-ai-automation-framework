package shop;

import java.net.URI;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;

record ApiConfig(URI baseUri, int connectMillis, int responseMillis) {
    ApiConfig {
        if (!("http".equals(baseUri.getScheme()) || "https".equals(baseUri.getScheme()))
                || baseUri.getHost() == null || baseUri.getUserInfo() != null)
            throw new IllegalArgumentException("Expected an HTTP base URI without credentials");
        if (connectMillis < 1 || responseMillis < 1 || connectMillis > 300000 || responseMillis > 300000)
            throw new IllegalArgumentException("API timeouts must be within 1–300000 ms");
    }

    static ApiConfig forFixture(String url) {
        return new ApiConfig(URI.create(url), timeout("connect", "5000"), timeout("response", "10000"));
    }

    private static int timeout(String name, String fallback) {
        return Integer.parseInt(System.getProperty("shop.api." + name + ".millis",
            System.getenv().getOrDefault("SHOP_API_" + name.toUpperCase(java.util.Locale.ROOT) + "_MILLIS", fallback)));
    }

    RequestSpecification request() {
        var client = HttpClientConfig.httpClientConfig()
            .setParam("http.connection.timeout", connectMillis)
            .setParam("http.socket.timeout", responseMillis);
        return new RequestSpecBuilder().setBaseUri(baseUri.toString())
            .setConfig(RestAssuredConfig.config().httpClient(client)).build();
    }
}