package com.rohit.hellobuddy.model;

public class ChatList {

    private String id,NameForSearch,date;

    public ChatList() {
    }

    public ChatList(String id, String nameForSearch, String date) {
        this.id = id;
        NameForSearch = nameForSearch;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameForSearch() {
        return NameForSearch;
    }

    public void setNameForSearch(String nameForSearch) {
        NameForSearch = nameForSearch;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
