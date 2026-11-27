package shop;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.time.Clock;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class CatalogDiagnosticsTest {
    @Test(groups = {"ui", "regression"})
    public void cartStartupFailureIsDiagnosed() throws Exception {
        var rejectCart = new Filter() {
            @Override public String description() { return "Reject the initial cart read"; }

            @Override public void doFilter(HttpExchange request, Chain chain) throws IOException {
                if (request.getRequestURI().getPath().equals("/api/cart")) {
                    try (request) { request.sendResponseHeaders(503, -1); }
                } else {
                    chain.doFilter(request);
                }
            }
        };
        try (var shop = new Shop(Clock.systemUTC(), () -> {}, rejectCart)) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                var page = new CatalogPage(driver, config.waitTimeout());
                var failure = expectThrows(IllegalStateException.class, () -> page.open(shop));
                assertEquals(failure.getMessage(), "Shop reported initialization failure");
                assertEquals(failure.getSuppressed().length, 1);
                assertEquals(failure.getSuppressed()[0].getMessage(),
                    "documentRequested=true\ndocumentSent=true\nproductsSent=true\ncartSent=false\npageState=error\nlocation=fixture\n");
            } finally {
                driver.quit();
            }
        }
    }
}
