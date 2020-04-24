package com.rohit.hellobuddy.model;

public class User {

    private String name,uid,status,phone,image,currentStatus,lastSeenDate,NameForSearch;

    private boolean isSelected=false;

    public User() {
    }

    public User(String name, String uid, String status, String phone, String image, String currentStatus, String lastSeenDate, String NameForSearch,boolean isSelected) {
        this.name = name;
        this.uid = uid;
        this.status = status;
        this.phone = phone;
        this.image = image;
        this.currentStatus = currentStatus;
        this.lastSeenDate = lastSeenDate;
        this.NameForSearch = NameForSearch;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getLastSeenDate() {
        return lastSeenDate;
    }

    public void setLastSeenDate(String lastSeenDate) {
        this.lastSeenDate = lastSeenDate;
    }

    public String getNameForSearch() {
        return NameForSearch;
    }

    public void setNameForSearch(String nameForSearch) {
        NameForSearch = nameForSearch;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
