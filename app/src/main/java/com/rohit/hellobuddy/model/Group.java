package com.rohit.hellobuddy.model;

public class Group {

    private String id,name,image,date,status;

    public Group() {
    }

    public Group(String id, String name, String image, String date,String status) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.date = date;
        this.status=status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
