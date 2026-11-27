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
                page.open(shop);
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
                } catch (Exception diagnosticFailure) { failure.addSuppressed(diagnosticFailure); }
                throw failure;
            } finally { driver.quit(); }
        }
    }
    @Test(groups = {"regression", "ui"})
    public void shopperUpdatesMultiItemCart() throws Exception {
        try (var shop = new Shop()) {
            var config = BrowserConfig.load();
            var driver = DriverFactory.open(config);
            try {
                var page = new CatalogPage(driver, config.waitTimeout());
                page.open(shop);
                assertCart(page, 0, 0);
                page.addProduct();
                assertCart(page, 1, 0);
                page.search("pen");
                page.awaitProduct("Ink pen");
                page.addProduct();
                assertCart(page, 1, 1);
                page.addProduct();
                assertCart(page, 1, 2);
                page.removeItem("notebook");
                assertCart(page, 0, 2);
                page.removeItem("pen");
                assertCart(page, 0, 0);
            } finally { driver.quit(); }
        }
    }

    private static String dollars(int cents) {
        return "$" + java.math.BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private static void assertCart(CatalogPage page, int notebooks, int pens) {
        assertEquals(page.awaitQuantity(notebooks + pens), notebooks + pens);
        var expected = new java.util.ArrayList<String>();
        if (notebooks > 0) expected.add("Notebook × " + notebooks + " @ $5.00 = " + dollars(500 * notebooks));
        if (pens > 0) expected.add("Ink pen × " + pens + " @ $1.75 = " + dollars(175 * pens));
        assertEquals(page.cartLines(), expected);
        assertEquals(page.cartTotal(), dollars(500 * notebooks + 175 * pens));
        assertEquals(page.emptyCartVisible(), expected.isEmpty());
    }
}
