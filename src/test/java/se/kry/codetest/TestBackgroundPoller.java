package se.kry.codetest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.kry.codetest.models.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ExtendWith(VertxExtension.class)
public class TestBackgroundPoller {
    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Poll service")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void poll_new_service(Vertx vertx, VertxTestContext testContext) {
        String randomString = RandomStringUtils.random(10,true,true);
        String url = buildUrl(randomString);
        JsonObject payload = new JsonObject().put("url", url).put("name", randomString);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(payload, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    assertEquals("Ok", body);
                                WebClient.create(vertx)
                                        .get(8080, "::1", "/service")
                                        .send(res -> testContext.verify(() -> {
                                            assertEquals(200, res.result().statusCode());
                                            JsonArray b = res.result().bodyAsJsonArray();
                                            List<Service> serviceList = ServiceUtil.toServiceList(b.toString());
                                            List<String> serviceNames = serviceList.stream().map(Service::getName).collect(Collectors.toList());
                                            assertTrue(serviceNames.stream().anyMatch(serviceName -> randomString.equals(serviceName)));
                                            for (Service service :serviceList) {
                                                if (service.getName().equals(randomString)) {
                                                    BackgroundPoller backgroundPoller = new BackgroundPoller(vertx);
                                                    backgroundPoller.poll(service).setHandler(ar-> {
                                                        Service result = ar.result();
                                                        assertEquals(Service.Status.FAIL, result.getStatus());
                                                        testContext.completeNow();
                                                    });
                                                    break;
                                                }
                                            }
                                        }));
                }));
    }

    private String buildUrl(String random_string) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://www.").append(random_string).append(".com");
        return stringBuilder.toString();
    }
}
