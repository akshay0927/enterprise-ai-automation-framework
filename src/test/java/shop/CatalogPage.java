package shop;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

final class CatalogPage {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private String observedState;

    CatalogPage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        wait = new WebDriverWait(driver, timeout);
    }

    void open(Shop shop) {
        observedState = "unobserved";
        try {
            driver.get(shop.url());
            // On Windows ChromeDriver can return from get() on its startup blank
            // document without sending a request. Only repeat this read in that case.
            if (isBlankDocument(driver.getCurrentUrl()) && !shop.documentRequested()) {
                System.out.println("Catalog navigation returned a blank document before reaching the fixture; retrying once.");
                driver.get(shop.url());
            }
            wait.withMessage("Shop initialization did not complete").until(browser -> {
                String state = browser.findElement(By.tagName("html")).getDomAttribute("data-state");
                observedState = switch (state == null ? "" : state) {
                    case "ready", "loading", "error" -> state;
                    default -> "unmarked";
                };
                if ("error".equals(observedState)) throw new IllegalStateException("Shop reported initialization failure");
                return "ready".equals(observedState);
            });
        } catch (RuntimeException failure) {
            String location = "unavailable";
            try {
                String current = driver.getCurrentUrl();
                if ((shop.url() + "/").equals(current)) location = "fixture";
                else if (isBlankDocument(current)) location = "blank";
                else location = "other";
            } catch (RuntimeException ignored) {
                // Keep the fixture evidence even if WebDriver has disconnected.
            }
            String progress = shop.catalogProgress() + "\npageState=" + observedState + "\nlocation=" + location + "\n";
            try {
                var directory = java.nio.file.Path.of("target", "surefire-reports", "browser-diagnostics");
                java.nio.file.Files.createDirectories(directory);
                var report = java.nio.file.Files.createTempFile(directory, "catalog-open-", ".txt");
                java.nio.file.Files.writeString(report, progress);
            } catch (java.io.IOException | SecurityException unavailable) {
                failure.addSuppressed(unavailable);
            }
            failure.addSuppressed(new IllegalStateException(progress));
            throw failure;
        }
    }

    private static boolean isBlankDocument(String url) {
        return "about:blank".equals(url) || "data:,".equals(url);
    }

    void search(String query) {
        var input = wait.until(ExpectedConditions.elementToBeClickable(By.id("search")));
        input.clear();
        input.sendKeys(query);
    }

    String awaitProduct(String expected) {
        wait.until(ExpectedConditions.textToBe(By.id("product"), expected));
        return driver.findElement(By.id("product")).getText();
    }

    void addProduct() { wait.until(ExpectedConditions.elementToBeClickable(By.id("add"))).click(); }

    int awaitQuantity(int expected) {
        wait.until(ExpectedConditions.textToBe(By.id("quantity"), Integer.toString(expected)));
        return Integer.parseInt(driver.findElement(By.id("quantity")).getText());
    }
    void removeItem(String id) {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-item='" + id + "'] button"))).click();
    }

    java.util.List<String> cartLines() {
        return driver.findElements(By.cssSelector("#lines li span")).stream().map(e -> e.getText()).toList();
    }

    String cartTotal() { return driver.findElement(By.id("total")).getText(); }

    boolean emptyCartVisible() {
        var message = driver.findElement(By.id("empty"));
        return message.isDisplayed() && message.getText().equals("Cart is empty");
    }
}
