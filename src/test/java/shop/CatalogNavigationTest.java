package shop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CatalogNavigationTest {
    enum Document { READY, BLANK, EMPTY_DATA, RECEIVED_BUT_BLANK, ERROR, OTHER }

    @DataProvider
    public Object[][] navigationCases() {
        return new Object[][] {
            {Document.READY, List.of(Document.READY), 1},
            {Document.READY, List.of(Document.BLANK, Document.READY), 2},
            {Document.BLANK, List.of(Document.BLANK, Document.EMPTY_DATA), 2},
            {Document.RECEIVED_BUT_BLANK, List.of(Document.RECEIVED_BUT_BLANK), 1},
            {Document.ERROR, List.of(Document.ERROR), 1},
            {Document.OTHER, List.of(Document.OTHER), 1}
        };
    }

    @Test(dataProvider = "navigationCases", timeOut = 10000)
    public void catalogNavigationIsBounded(Document outcome, List<Document> script, int count) throws Exception {
        try (var shop = new Shop(); var client = HttpClient.newHttpClient()) {
            var driver = new ScriptedDriver(shop, client, script);
            var page = new CatalogPage(driver, Duration.ZERO);
            if (outcome == Document.READY) {
                page.open(shop);
                assertTrue(shop.documentRequested());
            } else {
                RuntimeException failure = outcome == Document.ERROR
                    ? expectThrows(IllegalStateException.class, () -> page.open(shop))
                    : expectThrows(TimeoutException.class, () -> page.open(shop));
                boolean received = outcome == Document.ERROR || outcome == Document.RECEIVED_BUT_BLANK;
                assertEquals(shop.documentRequested(), received);
                String location = switch (outcome) {
                    case OTHER -> "other";
                    case ERROR -> "fixture";
                    default -> "blank";
                };
                String state = outcome == Document.ERROR ? "error" : "unmarked";
                assertEquals(failure.getSuppressed().length, 1);
                assertEquals(failure.getSuppressed()[0].getMessage(),
                    shop.catalogProgress() + "\npageState=" + state + "\nlocation=" + location + "\n");
            }
            assertTrue(driver.readinessChecks > 0);
            assertEquals(driver.targets, Collections.nCopies(count, shop.url()));
        }
    }

    // Only the navigation and DOM reads used by CatalogPage.open are supported.
    // Other browser commands cannot succeed without a real driver session.
    private static final class ScriptedDriver extends RemoteWebDriver {
        private final Shop shop;
        private final HttpClient client;
        private final List<Document> script;
        private final List<String> targets = new ArrayList<>();
        private Document document;
        private int readinessChecks;

        ScriptedDriver(Shop shop, HttpClient client, List<Document> script) {
            this.shop = shop;
            this.client = client;
            this.script = script;
        }

        @Override public void get(String url) {
            assertEquals(url, shop.url(), "Only the catalog root may be replayed");
            assertTrue(targets.size() < script.size(), "Unexpected extra navigation");
            document = script.get(targets.size());
            targets.add(url);
            if (document == Document.READY || document == Document.RECEIVED_BUT_BLANK || document == Document.ERROR) {
                var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build();
                try {
                    assertEquals(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode(), 200);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                } catch (java.io.IOException failure) {
                    throw new AssertionError(failure);
                }
            }
        }

        @Override public String getCurrentUrl() {
            return switch (document) {
                case READY, ERROR -> shop.url() + "/";
                case EMPTY_DATA -> "data:,";
                case OTHER -> "about:version";
                default -> "about:blank";
            };
        }

        @Override public WebElement findElement(By by) {
            assertEquals(by, By.tagName("html"));
            return new RemoteWebElement() {
                @Override public String getDomAttribute(String name) {
                    assertEquals(name, "data-state");
                    readinessChecks++;
                    return switch (document) {
                        case READY -> "ready";
                        case ERROR -> "error";
                        default -> null;
                    };
                }
            };
        }
    }
}
