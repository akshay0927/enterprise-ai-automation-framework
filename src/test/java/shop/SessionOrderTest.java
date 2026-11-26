package shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import java.time.*;
import org.testng.annotations.Test;

public class SessionOrderTest {
    private static class FixtureClock extends Clock {
        Instant now = Instant.EPOCH;
        public Instant instant() { return now; }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    }

    @Test(groups={"authentication","api","regression"})
    public void isolatedOrdersAndStock() throws Exception {
        var time = new FixtureClock();
        try (var shop = new Shop(time)) {
            var config = ApiConfig.forFixture(shop.url());
            var api = config.request();
            given().spec(api).get("/api/profile").then().statusCode(401);
            given().spec(api).body("alice:wrong").post("/api/login").then().statusCode(401);
            String cookie = given().spec(api).body("4").put("/api/cart/items/pen").then()
                .statusCode(200).extract().cookie("guest");
            String a = given().spec(api).cookie("guest",cookie).body("alice:"+shop.fixturePassword("alice"))
                .post("/api/login").then().statusCode(200).extract().path("token");
            var alice = config.request().header("Authorization","Bearer "+a);
            given().spec(alice).get("/api/cart").then().body("quantity",equalTo(4));
            given().spec(api).cookie("guest",cookie).get("/api/cart").then().body("quantity",equalTo(0));
            String b = given().spec(api).body("bob:"+shop.fixturePassword("bob")).post("/api/login")
                .then().statusCode(200).extract().path("token");
            var bob = config.request().header("Authorization","Bearer "+b);
            given().spec(bob).post("/api/cart/items/notebook").then().statusCode(200);
            given().spec(bob).post("/api/cart/items/pen").then().statusCode(200);
            given().spec(alice).post("/api/checkout").then().statusCode(201).body("cart.totalCents",equalTo(700));
            given().spec(alice).post("/api/checkout").then().statusCode(409);
            given().spec(bob).post("/api/checkout").then().statusCode(409);
            given().spec(api).get("/api/products/notebook").then().body("stock",equalTo(8));
            given().spec(bob).get("/api/cart").then().body("quantity",equalTo(2));
            given().spec(bob).get("/api/orders").then().body("size()",equalTo(0));
            given().spec(alice).get("/api/orders").then().body("size()",equalTo(1));
            given().spec(alice).post("/api/logout").then().statusCode(200);
            given().spec(alice).get("/api/profile").then().statusCode(401);
            time.now = time.now.plusSeconds(900);
            given().spec(bob).get("/api/profile").then().statusCode(401);
        }
    }
}
