package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.models.Service;
import se.kry.codetest.repository.ServiceRepository;

public class PersistenceVerticle extends AbstractVerticle{

    private ServiceRepository serviceRepository;
    @Override
    public void start(Future<Void> startFuture) {
        EventBus eventBus = vertx.eventBus();
        serviceRepository = new ServiceRepository(vertx);
        MessageConsumer<JsonObject> consumer = eventBus.consumer("persistence-address");

        consumer.handler(message -> {

            String action = message.body().getString("action");

            switch (action) {
                case "delete-service":
                    deleteService(message);
                    break;
                case "get-services":
                    getServices(message);
                    break;
                case "add-service":
                    addService(message);
                    break;
                case "update-service":
                    updateService(message);
                    break;
                default:
                    message.fail(1, "Unkown action: " + message.body());
            }
        });

        startFuture.complete();

    }

    private void deleteService(Message<JsonObject> message) {
        String nameToDelete = message.body().getString("name");
        serviceRepository.deleteService(nameToDelete).setHandler(ar-> {
            if(ar.succeeded()) {
                message.reply("Ok");
            } else {
                message.fail(500,ar.cause().getLocalizedMessage());
            }
        });
    }

    private void getServices(Message<JsonObject> message) {
        serviceRepository.getServices().compose(this::mapToJsonArray).setHandler(ar -> {
            if(ar.succeeded()) {
                message.reply(ar.result());
            } else {
                message.fail(500, ar.cause().getLocalizedMessage());
            }
        });
    }

    private void addService(Message<JsonObject> message) {
        serviceRepository.addService(message.body().getJsonObject("service").mapTo(Service.class)).setHandler(ar -> {
            if(ar.succeeded()) {
                message.reply("Ok");
            } else {
                message.fail(500,ar.cause().getLocalizedMessage());
            }
        });
    }

    private void updateService(Message<JsonObject> message) {
        serviceRepository.updateService(message.body().getJsonObject("service").mapTo(Service.class)).setHandler(ar -> {
            if(ar.succeeded()) {
                message.reply("Ok");
            } else {
                message.fail(500,ar.cause().getLocalizedMessage());
            }
        });
    }

    Future<JsonArray> mapToJsonArray(ResultSet rs) {
        return Future.succeededFuture(new JsonArray(rs.getRows()));
    }
}
