package com.example.model;

import java.io.Serializable;

public class NetAccount implements Serializable {
    public int netAccountId;
    public String netAccountName;

    public NetAccount() {
        // 默认构造函数用于反序列化
    }

    public NetAccount(int netAccountId, String netAccountName) {
        this.netAccountId = netAccountId;
        this.netAccountName = netAccountName;
    }

    // Getters and Setters
    public int getId() {
        return netAccountId;
    }

    public void setId(int id) {
        this.netAccountId = id;
    }

    public String getName() {
        return netAccountName;
    }

    public void setName(String netAccountName) {
        this.netAccountName = netAccountName;
    }
}