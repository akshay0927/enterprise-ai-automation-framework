package shop;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

final class CatalogPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    CatalogPage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        wait = new WebDriverWait(driver, timeout);
    }

    void open(String url) {
        driver.get(url);
        wait.withMessage("Shop initialization did not complete").until(browser -> {
            String state = browser.findElement(By.tagName("html")).getDomAttribute("data-state");
            if ("error".equals(state)) throw new IllegalStateException("Shop reported initialization failure");
            return "ready".equals(state);
        });
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