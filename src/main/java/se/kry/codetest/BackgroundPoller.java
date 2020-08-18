package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import se.kry.codetest.models.Service;

import java.util.List;

public class BackgroundPoller {

  private WebClient webClient;
  private Vertx vertx;

  public BackgroundPoller(Vertx vertx) {
    this.vertx = vertx;
    webClient = WebClient.create(vertx);
  }

  public void pollServices() {

    JsonObject message = new JsonObject()
            .put("action", "get-services");
    vertx.eventBus().send("persistence-address", message, ar -> {
      if (ar.succeeded()) {
        List<Service> serviceList = ServiceUtil.toServiceList(ar.result().body().toString());
        serviceList.forEach(service -> poll(service).setHandler(res -> {
          Service result = res.result();
          System.out.println(result.getUrl());
          System.out.println(result.getStatus());
          JsonObject updateMessage = new JsonObject()
                  .put("action", "update-service")
                  .put("service", JsonObject.mapFrom(service));
          vertx.eventBus().send("persistence-address", updateMessage);
        }));
      }
    });
  }

  public Future<Service> poll(Service service) {
    String url = service.getUrl();
    Promise<Service> serviceStatus = Promise.promise();
    try {
      webClient.getAbs(url).timeout(20 * 1000)
              .send(response -> {
                if (response.succeeded()) {
                  System.out.println(response.result().statusCode());
                  service.setStatus(response.result().statusCode() == 200 ? Service.Status.OK : Service.Status.FAIL);
                  serviceStatus.complete(service);
                } else {
                  service.setStatus(Service.Status.FAIL);
                  serviceStatus.complete(service);
                }
              });
    } catch (Exception e) {
      service.setStatus(Service.Status.FAIL);
      serviceStatus.complete(service);
    }
    return serviceStatus.future();
  }
}