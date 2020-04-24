package com.rohit.hellobuddy.model;

public class ChatList {

    private String id,NameForSearch,date,lastMessage,lastMessageDate,messageSeen,unseenMsgCount;

    public ChatList() {
    }

    public ChatList(String id, String NameForSearch, String date, String lastMessage, String lastMessageDate, String messageSeen, String unseenMsgCount) {
        this.id = id;
        this.NameForSearch = NameForSearch;
        this.date = date;
        this.lastMessage = lastMessage;
        this.lastMessageDate = lastMessageDate;
        this.messageSeen = messageSeen;
        this.unseenMsgCount = unseenMsgCount;
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

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(String lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public String getMessageSeen() {
        return messageSeen;
    }

    public void setMessageSeen(String messageSeen) {
        this.messageSeen = messageSeen;
    }

    public String getUnseenMsgCount() {
        return unseenMsgCount;
    }

    public void setUnseenMsgCount(String unseenMsgCount) {
        this.unseenMsgCount = unseenMsgCount;
    }
}
