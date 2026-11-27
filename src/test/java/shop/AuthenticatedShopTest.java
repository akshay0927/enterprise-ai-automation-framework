package shop;

import static org.testng.Assert.assertTrue;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class AuthenticatedShopTest {
    @Test(groups={"ui","authentication","regression"})
    public void browserCheckoutAndLogout() throws Exception {
        try (var shop = new Shop()) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                var page = new CatalogPage(driver, config.waitTimeout());
                var wait = new WebDriverWait(driver, config.waitTimeout());
                page.open(shop);
                page.addProduct();
                page.awaitQuantity(1);
                signIn(driver, wait, shop);
                page.awaitQuantity(1);
                var preview = java.nio.file.Path.of("target", "browser-preview", "shop.png");
                java.nio.file.Files.createDirectories(preview.getParent());
                java.nio.file.Files.write(preview, ((org.openqa.selenium.TakesScreenshot) driver)
                    .getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
                driver.findElement(By.id("checkout")).click();
                wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("message"),"Order confirmed:"));
                page.awaitQuantity(0);
                driver.findElement(By.id("logout")).click();
                wait.until(ExpectedConditions.textToBe(By.id("profile"),"Guest"));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("checkout")));
            } finally { driver.quit(); }
        }
    }

    @Test(groups={"ui","authentication","regression"})
    public void expiredCheckoutReturnsToGuest() throws Exception {
        var time = new FixtureClock();
        try (var shop = new Shop(time)) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                var page = new CatalogPage(driver, config.waitTimeout());
                var wait = new WebDriverWait(driver, config.waitTimeout());
                page.open(shop);
                signIn(driver, wait, shop);
                page.addProduct();
                page.awaitQuantity(1);

                time.advance(Duration.ofMinutes(15));
                driver.findElement(By.id("checkout")).click();
                wait.until(ExpectedConditions.textToBe(By.id("message"), "Session expired. Sign in again."));
                wait.until(ExpectedConditions.textToBe(By.id("profile"), "Guest"));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("checkout")));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("logout")));
                page.awaitQuantity(0);
                assertTrue(driver.findElement(By.id("login")).isDisplayed());

                // A fresh guest can shop. Reauthentication merges that guest item
                // with the account item left intact by the rejected checkout.
                page.addProduct();
                page.awaitQuantity(1);
                signIn(driver, wait, shop);
                page.awaitQuantity(2);
            } finally {
                driver.quit();
            }
        }
    }

    static final class SignInFailure extends AssertionError {
        SignInFailure() { super("The shop reported sign-in failure"); }
    }

    private static void signIn(WebDriver driver, WebDriverWait wait, Shop shop) {
        driver.findElement(By.id("user")).sendKeys("alice");
        driver.findElement(By.id("password")).sendKeys(shop.fixturePassword("alice"));
        driver.findElement(By.cssSelector("#login button")).click();
        wait.until(browser -> {
            if ("Sign-in failed".equals(browser.findElement(By.id("message")).getText())) throw new SignInFailure();
            return "alice".equals(browser.findElement(By.id("profile")).getText());
        });
    }
}
