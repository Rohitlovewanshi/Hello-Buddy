package com.rohit.hellobuddy.model;

public class GroupChat {

    private String sender,message,date;

    public GroupChat() {
    }

    public GroupChat(String sender, String message, String date) {
        this.sender = sender;
        this.message = message;
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
