package com.example.model;

public class NetChatData {
    public long timestamp;
    public int roomId;
    public String name;
    public String content;

    public int getRoomId() {
        return roomId;
    }

    public String getName(){
        return name;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
