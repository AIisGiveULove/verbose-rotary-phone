package com.example.model;

import java.io.Serializable;

public class Vector2 implements Serializable {
    public float x;
    public float y;

    public Vector2() {
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Getters and Setters
    // 这里省略了getter和setter，实际项目中应该添加
}