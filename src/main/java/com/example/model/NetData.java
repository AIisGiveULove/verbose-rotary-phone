package com.example.model;

import java.io.Serializable;

public class NetData implements Serializable {
    public long timestamp;
    public String Type;
    public NetAccount netAccount;
    public PCControlData playerData;

    public NetData() {
        // 默认构造函数用于反序列化
    }

    public NetData(NetAccount netAccount, PCControlData playerData) {
        this.netAccount = netAccount;
        this.playerData = playerData;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return Type;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public NetAccount getNetAccount() {
        return netAccount;
    }

    public void setNetAccount(NetAccount netAccount) {
        this.netAccount = netAccount;
    }

    public PCControlData getPlayerData() {
        return playerData;
    }

    public void setPlayerData(PCControlData playerData) {
        this.playerData = playerData;
    }
}