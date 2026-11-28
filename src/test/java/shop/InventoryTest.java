package shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import org.testng.annotations.Test;

public class InventoryTest {
    @Test(groups = {"api","regression"})
    public void inventoryLimitsPreserveCart() throws Exception {
        try (Shop shop = new Shop()) {
            var config = ApiConfig.forFixture(shop.url());
            var api = config.request();
            String guest = given().spec(api).get("/api/cart").then().extract().cookie("guest");
            var cart = config.request().cookie("guest", guest);
            given().spec(api).get("/api/products/pen").then().statusCode(200).body("stock", equalTo(4));
            given().spec(api).get("/api/products/missing").then().statusCode(404);
            given().spec(cart).post("/api/cart/items/notebook").then().statusCode(200);
            given().spec(cart).body("4").put("/api/cart/items/pen").then().statusCode(200)
                .body("quantity", equalTo(5)).body("totalCents", equalTo(1200));
            given().spec(cart).post("/api/cart/items/pen").then().statusCode(409);
            given().spec(cart).post("/api/cart/items/folder").then().statusCode(409);
            given().spec(cart).body("-1").put("/api/cart/items/pen").then().statusCode(400);
            given().spec(cart).get("/api/cart").then().body("totalCents", equalTo(1200));
            given().spec(cart).delete("/api/cart/items/notebook").then().statusCode(200)
                .body("quantity", equalTo(4)).body("totalCents", equalTo(700));
        }
    }
}
