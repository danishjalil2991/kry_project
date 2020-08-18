package se.kry.codetest.models;


import java.time.Instant;
import java.time.format.DateTimeFormatter;


public class Service {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    private String name;
    private String url;
    private String created_at;

    public enum Status {
        UNKNOWN,OK,FAIL
    };

    private Status status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUrl() {
        return url;
    }

    public String getCreated_at() {
        return created_at;
    }

    public Service(String name, String url) {
        this.name = name;
        this.url = url;
        this.status = Status.UNKNOWN;
        this.created_at = DateTimeFormatter.ISO_INSTANT.format(Instant.now());;
    }

    public Service() {

    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

}
