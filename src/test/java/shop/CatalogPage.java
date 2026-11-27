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
            String progress = shop.catalogProgress() + "\npageState=" + observedState + "\n";
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
}