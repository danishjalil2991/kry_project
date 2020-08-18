package se.kry.codetest;

import io.vertx.core.json.JsonArray;
import se.kry.codetest.models.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceUtil {

    public static List<Service> toServiceList(String json) {
        JsonArray jsonArray = new JsonArray(json);
        List<Service> services = new ArrayList<>();
        for(int i = 0; i < jsonArray.size(); i ++) {
            Service service = jsonArray.getJsonObject(i).mapTo(Service.class);
            services.add(service);
        }
        return services;
    }
}
