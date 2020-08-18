package se.kry.codetest.repository;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import se.kry.codetest.DBConnector;
import se.kry.codetest.models.Service;

public class ServiceRepository {

    private DBConnector dbConnector;

    private static final String ADD_SERVICE = "INSERT INTO service (url, name, status, created_at) VALUES (?, ?, ?, ?)";
    private static final String DELETE_SERVICE = "DELETE FROM service WHERE name = ?";
    private static final String GET_SERVICES = "SELECT * FROM service;";
    private static final String UPDATE_SERVICE = "UPDATE service SET status = ? WHERE name = ?;";


    public ServiceRepository(Vertx vertx) {
        dbConnector = new DBConnector(vertx);
    }

    public Future<ResultSet> getServices() {
        return dbConnector.query(GET_SERVICES);
    }


    public Future<ResultSet> addService(Service service) {
        JsonArray params = new JsonArray()
                .add(service.getUrl())
                .add(service.getName())
                .add(service.getStatus())
                .add(service.getCreated_at());
        return dbConnector.query(ADD_SERVICE,params);
    }

    public Future<ResultSet> deleteService(String serviceName) {
        JsonArray params = new JsonArray().add(serviceName);
        return dbConnector.query(DELETE_SERVICE, params);
    }

    public Future<ResultSet> updateService(Service service) {
        JsonArray params = new JsonArray().add(service.getStatus())
                                          .add(service.getName());
        return dbConnector.query(UPDATE_SERVICE, params);
    }
}
