package com.example.model;

import java.io.Serializable;

public class Quaternion implements Serializable {
    public float x;
    public float y;
    public float z;
    public float w;

    public Quaternion() {
    }

    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    // Getters and Setters
    // 这里省略了getter和setter，实际项目中应该添加
}