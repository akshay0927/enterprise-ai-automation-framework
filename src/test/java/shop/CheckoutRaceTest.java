package shop;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.*;
import org.testng.annotations.Test;

public class CheckoutRaceTest {
    @Test(groups = {"api","regression"}, timeOut = 60000)
    public void concurrentCheckouts() throws Exception {
        var arrivals = new CyclicBarrier(2);
        Runnable rendezvous = () -> {
            try { arrivals.await(10, TimeUnit.SECONDS); }
            catch (Exception e) { throw new IllegalStateException("Both checkout handlers must arrive", e); }
        };
        try (var shop = new Shop(Clock.systemUTC(), rendezvous);
             var clients = Executors.newFixedThreadPool(2)) {
            var config = ApiConfig.forFixture(shop.url());
            var tokens = new java.util.ArrayList<String>();
            for (String user : List.of("alice","bob")) {
                String token = given().spec(config.request()).body(user + ":" + shop.fixturePassword(user))
                    .post("/api/login").then().statusCode(200).extract().path("token");
                tokens.add(token);
                given().spec(config.request()).header("Authorization","Bearer "+token).body("4").put("/api/cart/items/pen").then().statusCode(200);
            }
            var futures = new java.util.ArrayList<Future<Integer>>();
            for (String token : tokens) {
                futures.add(clients.submit(() -> given().spec(config.request()).header("Authorization","Bearer "+token)
                    .post("/api/checkout").statusCode()));
            }
            var statuses = List.of(futures.get(0).get(15,TimeUnit.SECONDS), futures.get(1).get(15,TimeUnit.SECONDS));
            assertEquals(statuses.stream().sorted().toList(), List.of(201,409));
            for (int i=0;i<2;i++) {
                var client = config.request().header("Authorization","Bearer "+tokens.get(i));
                boolean winner = statuses.get(i) == 201;
                given().spec(client).get("/api/orders").then().body("size()",equalTo(winner ? 1 : 0));
                given().spec(client).get("/api/cart").then().body("quantity",equalTo(winner ? 0 : 4));
            }
            given().spec(config.request()).get("/api/products/pen").then().body("stock",equalTo(0));
        }
    }
}
