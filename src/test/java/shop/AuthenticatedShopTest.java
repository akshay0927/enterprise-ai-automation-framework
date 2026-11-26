package shop;

import org.openqa.selenium.By;
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
                page.open(shop.url());
                page.addProduct();
                page.awaitQuantity(1);
                driver.findElement(By.id("user")).sendKeys("alice");
                driver.findElement(By.id("password")).sendKeys(shop.fixturePassword("alice"));
                driver.findElement(By.cssSelector("#login button")).click();
                wait.until(ExpectedConditions.textToBe(By.id("profile"),"alice"));
                page.awaitQuantity(1);
                driver.findElement(By.id("checkout")).click();
                wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("message"),"Order confirmed:"));
                page.awaitQuantity(0);
                driver.findElement(By.id("logout")).click();
                wait.until(ExpectedConditions.textToBe(By.id("profile"),"Guest"));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("checkout")));
            } finally { driver.quit(); }
        }
    }
}