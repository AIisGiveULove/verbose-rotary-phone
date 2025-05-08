package com.example.model;

import java.io.Serializable;

public class Vector3 implements Serializable {
    public float x;
    public float y;
    public float z;

    public Vector3() {
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getters and Setters
    // 这里省略了getter和setter，实际项目中应该添加
}