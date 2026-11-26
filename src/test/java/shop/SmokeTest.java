package shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

public class SmokeTest {
    @Test(groups = {"smoke", "api"})
    public void guestCartIsIsolated() throws Exception {
        try (Shop shop = new Shop()) {
            String guest = given().baseUri(shop.url()).get("/api/cart").then()
                .statusCode(200).body("quantity", equalTo(0)).extract().cookie("guest");
            given().baseUri(shop.url()).cookie("guest", guest).post("/api/cart/items/notebook")
                .then().statusCode(200).body("totalCents", equalTo(500));
            given().baseUri(shop.url()).get("/api/cart").then().statusCode(200).body("quantity", equalTo(0));
        }
    }

    @Test(groups = {"smoke", "ui"})
    public void shopperAddsNotebook() throws Exception {
        try (Shop shop = new Shop()) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                driver.get(shop.url());
                var wait = new WebDriverWait(driver, config.waitTimeout());
                wait.until(ExpectedConditions.textToBe(By.id("product"), "Notebook"));
                var search = driver.findElement(By.id("search"));
                search.sendKeys("pencil");
                wait.until(ExpectedConditions.textToBe(By.id("product"), "No matching products"));
                search.clear();
                search.sendKeys("BOOK");
                wait.until(ExpectedConditions.elementToBeClickable(By.id("add"))).click();
                wait.until(ExpectedConditions.textToBe(By.id("quantity"), "1"));
            } finally { driver.quit(); }
        }
    }
}