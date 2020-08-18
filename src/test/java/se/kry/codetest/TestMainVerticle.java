package se.kry.codetest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    vertx.deployVerticle(new PersistenceVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          testContext.completeNow();
        }));
  }

  private String buildUrl(String random_string) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("https://www.").append(random_string).append(".com");
      return stringBuilder.toString();
  }

    @Test
    @DisplayName("create a new service")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void create_new_service(Vertx vertx, VertxTestContext testContext) {
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
                            .send(r -> testContext.verify(() -> {
                                assertEquals(200, r.result().statusCode());
                                JsonArray b = r.result().bodyAsJsonArray();
                                List<Service> serviceList = ServiceUtil.toServiceList(b.toString());
                                List<String> serviceNames = serviceList.stream().map(Service::getName).collect(Collectors.toList());
                                assertTrue(serviceNames.stream().anyMatch(serviceName -> randomString.equals(serviceName)));
                                //Random gen string url and name
                                for (Service service :serviceList) {
                                    if (service.getName() == randomString) {
                                        assertEquals(service.getUrl(), url);
                                        assertNotNull(service.getCreated_at());
                                    }
                                }
                                testContext.completeNow();
                            }));
                }));
    }


    @Test
    @DisplayName("create a new service with empty name")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void create_new_service_empty_name(Vertx vertx, VertxTestContext testContext) {
        String randomString = RandomStringUtils.random(10,true,true);
        String url = buildUrl(randomString);
        JsonObject payload = new JsonObject().put("url", url);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(payload, response -> testContext.verify(() -> {
                    assertEquals(500, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("create a new service with empty url")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void create_new_service_empty_url(Vertx vertx, VertxTestContext testContext) {
        String randomString = RandomStringUtils.random(10,true,true);
        String url = buildUrl(randomString);
        JsonObject payload = new JsonObject().put("name", randomString);
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(payload, response -> testContext.verify(() -> {
                    assertEquals(400, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    assertEquals(body, "Invalid URL");
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("create a new service with invalid url")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void create_new_service_invalid_url(Vertx vertx, VertxTestContext testContext) {
        String randomString = RandomStringUtils.random(10,true,true);
        JsonObject payload = new JsonObject().put("name", randomString).put("url", randomString);

        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(payload, response -> testContext.verify(() -> {
                    assertEquals(400, response.result().statusCode());
                    String body = response.result().bodyAsString();
                    assertEquals(body, "Invalid URL");
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("delete a service")
    @Timeout(value = 20, timeUnit = TimeUnit.SECONDS)
    void delete_new_service(Vertx vertx, VertxTestContext testContext) {
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
                            .delete(8080, "::1", "/service/" + randomString)
                            .send(r -> testContext.verify(() -> {
                                assertEquals(200, r.result().statusCode());
                                //Random gen string url and name

                                WebClient.create(vertx)
                                        .get(8080, "::1", "/service")
                                        .send(res -> testContext.verify(() -> {
                                            assertEquals(200, res.result().statusCode());
                                            JsonArray b = res.result().bodyAsJsonArray();
                                            List<Service> serviceList = ServiceUtil.toServiceList(b.toString());
                                            List<String> serviceNames = serviceList.stream().map(Service::getName).collect(Collectors.toList());
                                            assertFalse(serviceNames.stream().anyMatch(serviceName -> randomString.equals(serviceName)));
                                            testContext.completeNow();
                                        }));
                            }));
                }));
    }

    @Test
    @DisplayName("Update a service")
    @Timeout(value = 90, timeUnit = TimeUnit.SECONDS)
    void update_new_service(Vertx vertx, VertxTestContext testContext) {
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
                                            //Random gen string url and name
                                            for (Service service :serviceList) {
                                                if (service.getName().equals(randomString)) {
                                                    assertEquals(service.getUrl(), url);
                                                    service.setStatus(Service.Status.OK);
                                                    JsonObject updateMessage = new JsonObject()
                                                            .put("action", "update-service")
                                                            .put("service", JsonObject.mapFrom(service));
                                                    vertx.eventBus().send("persistence-address", updateMessage, ar-> {
                                                        if(ar.succeeded()) {
                                                            assertTrue(true);
                                                        } else {
                                                            assertTrue(false);
                                                        }
                                                        testContext.completeNow();
                                                    });
                                                }
                                            }
                                        }));
                            }));
    }
}
