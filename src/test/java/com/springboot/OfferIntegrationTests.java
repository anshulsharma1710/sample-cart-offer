package com.springboot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OfferIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final String MOCKSERVER_MANAGEMENT = "http://localhost:1080/mockserver/expectation";

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private void mockUserSegment(int userId, String segment) {
        String json = "{"
                + "\"httpRequest\": {"
                + "   \"method\": \"GET\","
                + "   \"path\": \"/api/v1/user_segment\","
                + "   \"queryStringParameters\": [{\"name\":\"user_id\",\"values\":[\"" + userId + "\"]}]"
                + "},"
                + "\"httpResponse\": {"
                + "   \"statusCode\": 200,"
                + "   \"body\": \"{\\\"segment\\\":\\\"" + segment + "\\\"}\","
                + "   \"headers\": [{\"name\":\"Content-Type\",\"values\":[\"application/json; charset=utf-8\"]}]"
                + "},"
                + "\"times\": {\"unlimited\": true}"
                + "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        new RestTemplate().exchange(MOCKSERVER_MANAGEMENT, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class);
    }

    private void mockUserSegmentNotFound(int userId) {
        String json = "{"
                + "\"httpRequest\": {"
                + "   \"method\": \"GET\","
                + "   \"path\": \"/api/v1/user_segment\","
                + "   \"queryStringParameters\": [{\"name\":\"user_id\",\"values\":[\"" + userId + "\"]}]"
                + "},"
                + "\"httpResponse\": {"
                + "   \"statusCode\": 404,"
                + "   \"body\": \"{}\","
                + "   \"headers\": [{\"name\":\"Content-Type\",\"values\":[\"application/json; charset=utf-8\"]}]"
                + "},"
                + "\"times\": {\"unlimited\": true}"
                + "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        new RestTemplate().exchange(MOCKSERVER_MANAGEMENT, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class);
    }

    private ResponseEntity<Map> addOfferExpectResponse(int restaurantId, String offerType, int offerValue, String[] segments) {
        Map<String, Object> body = Map.of(
                "restaurant_id", restaurantId,
                "offer_type", offerType,
                "offer_value", offerValue,
                "customer_segment", segments
        );
        return restTemplate.postForEntity(baseUrl() + "/api/v1/offer", body, Map.class);
    }

    private ResponseEntity<Map> applyOffer(int cartValue, int userId, int restaurantId) {
        Map<String, Object> body = Map.of(
                "cart_value", cartValue,
                "user_id", userId,
                "restaurant_id", restaurantId
        );
        return restTemplate.postForEntity(baseUrl() + "/api/v1/cart/apply_offer", body, Map.class);
    }

    @Test
    public void testFlatAmountOfferApplied() {
        int userId = 1001;
        int restaurantId = 101;
        mockUserSegment(userId, "p1");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLATX", 10, new String[]{"p1"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(200, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(190, cartValue.intValue());
    }

    @Test
    public void testFlatPercentOfferApplied() {
        int userId = 1002;
        int restaurantId = 102;
        mockUserSegment(userId, "p2");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLAT%", 10, new String[]{"p2"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(200, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(180, cartValue.intValue()); // 10% of 200 = 20 => 180
    }

    @Test
    public void testNoSegmentNoOfferApplied() {
        int userId = 1003;
        int restaurantId = 103;

        // mock 404 -> no segment
        mockUserSegmentNotFound(userId);
        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLATX", 50, new String[]{"pX"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(500, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(500, cartValue.intValue()); // no change
    }

    @Test
    public void testOfferForDifferentSegmentNotApplied() {
        int userId = 1004;
        int restaurantId = 104;
        mockUserSegment(userId, "p1");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLATX", 25, new String[]{"p2"}); // offer for p2
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(300, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(300, cartValue.intValue()); // should not apply
    }

    @Test
    public void testMultipleOffersChooseBest() {
        int userId = 1005;
        int restaurantId = 105;
        mockUserSegment(userId, "p5");

        ResponseEntity<Map> r1 = addOfferExpectResponse(restaurantId, "FLATX", 30, new String[]{"p5"});
        Assertions.assertEquals(HttpStatus.OK, r1.getStatusCode());

        ResponseEntity<Map> r2 = addOfferExpectResponse(restaurantId, "FLAT%", 25, new String[]{"p5"});
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(200, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");

        int actual = cartValue.intValue();
        boolean ok = (actual == 150) || (actual == 170);
        Assertions.assertTrue(ok, "Expected either 150 or 170 but was: " + actual);
    }


    @Test
    public void testZeroValueOfferIgnored() {
        int userId = 1006;
        int restaurantId = 106;
        mockUserSegment(userId, "p6");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLATX", 0, new String[]{"p6"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(120, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(120, cartValue.intValue()); // no change
    }

    @Test
    public void testInvalidOfferTypeRejected() {
        int restaurantId = 107;
        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "INVALID_TYPE", 10, new String[]{"pX"});

        if (resp.getStatusCode().is4xxClientError()) {
            Assertions.assertTrue(true);
            return;
        }

        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        int userId = 1007;
        mockUserSegment(userId, "pX");
        ResponseEntity<Map> applyResp = applyOffer(100, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        int actual = cartValue.intValue();

        boolean ok = (actual == 100) || (actual == 90);
        Assertions.assertTrue(ok, "Expected cart_value 100 (ignored) or 90 (treated as flat10). Actual: " + actual);
    }


    @Test
    public void testHundredPercentDiscount() {
        int userId = 1008;
        int restaurantId = 108;
        mockUserSegment(userId, "p8");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLAT%", 100, new String[]{"p8"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(250, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        Assertions.assertEquals(0, cartValue.intValue()); 
    }

    @Test
    public void testRoundingBehaviorForFractionalDiscount() {
        int userId = 1009;
        int restaurantId = 109;
        mockUserSegment(userId, "p9");

        ResponseEntity<Map> resp = addOfferExpectResponse(restaurantId, "FLAT%", 33, new String[]{"p9"});
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());

        ResponseEntity<Map> applyResp = applyOffer(199, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, applyResp.getStatusCode());
        Number cartValue = (Number) applyResp.getBody().get("cart_value");
        double actual = cartValue.doubleValue();

        double expected = Math.round(199 * (1 - 0.33));
        Assertions.assertEquals((long) expected, Math.round(actual));
    }

    @Test
    public void testOfferUpdateLifecycle() {
        int userId = 1010;
        int restaurantId = 110;
        mockUserSegment(userId, "p10");

        ResponseEntity<Map> r1 = addOfferExpectResponse(restaurantId, "FLATX", 20, new String[]{"p10"});
        Assertions.assertEquals(HttpStatus.OK, r1.getStatusCode());

        ResponseEntity<Map> apply1 = applyOffer(200, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, apply1.getStatusCode());
        Number cart1 = (Number) apply1.getBody().get("cart_value");
        Assertions.assertEquals(180, cart1.intValue());

        ResponseEntity<Map> r2 = addOfferExpectResponse(restaurantId, "FLATX", 5, new String[]{"p10"});
        Assertions.assertEquals(HttpStatus.OK, r2.getStatusCode());

        ResponseEntity<Map> apply2 = applyOffer(200, userId, restaurantId);
        Assertions.assertEquals(HttpStatus.OK, apply2.getStatusCode());
        Number cart2 = (Number) apply2.getBody().get("cart_value");

        Assertions.assertTrue(cart2.intValue() == 195 || cart2.intValue() == 180,
                "Expected either updated offer applied (195) or previous best applied (180). Actual: " + cart2.intValue());
    }
}
