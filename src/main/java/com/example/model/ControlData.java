package com.example.model;

import java.io.Serializable;

public class ControlData implements Serializable {
    // 基础属性
    public float speed;
    public float maxHP;
    public float maxMP;
    public float HP;
    public float MP;
    public float attack;
    public float defense;
    public float knockbackResistance;
    public float knockbackForce;
    public int exp;
    public int expNow;
    public float bossRaise;
    public float attackCooldown;

    // 变换信息
    public Vector3 position;
    public Quaternion rotation;
    public Vector3 scale;

    public ControlData() {
        // 默认构造函数用于反序列化
    }

    // Getters and Setters
    // 这里省略了getter和setter，实际项目中应该添加
}