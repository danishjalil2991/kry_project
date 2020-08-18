package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.models.Service;

import java.net.MalformedURLException;
import java.net.URL;

public class MainVerticle extends AbstractVerticle {


  private DBConnector connector;
  private BackgroundPoller poller;
  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    poller = new BackgroundPoller(vertx);
    Router router = Router.router(vertx);
    vertx.deployVerticle(new PersistenceVerticle());
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 10, timerId -> poller.pollServices());
    setRoutes(router);
    vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
              if (result.succeeded()) {
                System.out.println("KRY code test service started");
                startFuture.complete();
              } else {
                startFuture.fail(result.cause());
              }
            });
  }

  private boolean checkIfURLIsValid(String url) {
    try {
      URL isValid = new URL(url);
      return true;
    } catch (MalformedURLException e) {
     return false;
    }
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(req -> {
      JsonObject message = new JsonObject()
              .put("action", "get-services");

              vertx.eventBus().send("persistence-address", message, ar -> {
                if(ar.succeeded()) {
                  req.response()
                          .putHeader("content-type", "application/json")
                          .end(ar.result().body().toString());
                } else {
                  req.response().setStatusCode(500)
                          .putHeader("content-type", "application/json")
                          .end(Json.encodePrettily(ar.cause().getMessage()));
                }
              });
    });
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      Service service = new Service(jsonBody.getString("name"), jsonBody.getString("url"));

      if(checkIfURLIsValid(service.getUrl())) {
        JsonObject message = new JsonObject()
                .put("action", "add-service")
                .put("service", JsonObject.mapFrom(service));

        vertx.eventBus().send("persistence-address", message, ar -> {
          if (ar.succeeded()) {
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end(ar.result().body().toString());
          } else {
            req.response().setStatusCode(500)
                    .putHeader("content-type", "text/plain")
                    .end(ar.cause().getLocalizedMessage());
          }

        });
      } else {
        req.response().setStatusCode(400)
                .putHeader("content-type", "text/plain")
                .end("Invalid URL");
      }
    });

    router.delete("/service/:name").handler(req->{
      String serviceName = req.request().getParam("name");
      JsonObject message = new JsonObject()
              .put("action", "delete-service")
              .put("name", serviceName);

      vertx.eventBus().send("persistence-address", message, ar -> {
        if (ar.succeeded()) {
          req.response()
                  .putHeader("content-type", "text/plain")
                  .end(ar.result().body().toString());
        } else {
          req.response().setStatusCode(500)
                  .putHeader("content-type", "text/plain")
                  .end(ar.cause().getLocalizedMessage());
        }
      });
    });
  }
}

