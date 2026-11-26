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
            } finally { driver.quit(); }
        }
    }
}