package shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SmokeTest {
    @Test(groups = {"smoke", "api"})
    public void guestCartIsIsolated() throws Exception {
        try (Shop shop = new Shop()) {
            var api = ApiConfig.forFixture(shop.url());
            String guest = given().spec(api.request()).get("/api/cart").then()
                .statusCode(200).body("quantity", equalTo(0)).extract().cookie("guest");
            given().spec(api.request()).cookie("guest", guest).post("/api/cart/items/notebook")
                .then().statusCode(200).body("totalCents", equalTo(500));
            given().spec(api.request()).get("/api/cart").then().statusCode(200).body("quantity", equalTo(0));
        }
    }

    @Test(groups = {"smoke", "ui"})
    public void shopperAddsNotebook() throws Exception {
        try (Shop shop = new Shop()) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                var page = new CatalogPage(driver, config.waitTimeout());
                page.open(shop.url());
                assertEquals(page.awaitProduct("Notebook"), "Notebook");
                page.search("pencil");
                assertEquals(page.awaitProduct("No matching products"), "No matching products");
                page.search("BOOK");
                assertEquals(page.awaitProduct("Notebook"), "Notebook");
                page.addProduct();
                assertEquals(page.awaitQuantity(1), 1);
            } catch (RuntimeException | AssertionError failure) {
                try {
                    var directory = java.nio.file.Path.of("target","surefire-reports","browser-diagnostics");
                    java.nio.file.Files.createDirectories(directory);
                    java.nio.file.Files.write(directory.resolve("catalog.png"),
                        ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
                    var html = driver.findElement(org.openqa.selenium.By.tagName("html"));
                    String state = html.getDomAttribute("data-state");
                    java.nio.file.Files.writeString(directory.resolve("catalog.txt"),
                        "Browser: " + ((org.openqa.selenium.HasCapabilities) driver).getCapabilities().getBrowserVersion()
                        + "\nInitialization state: " + state + "\n");
                } catch (Exception diagnosticFailure) { failure.addSuppressed(diagnosticFailure); }
                throw failure;
            } finally { driver.quit(); }
        }
    }
}