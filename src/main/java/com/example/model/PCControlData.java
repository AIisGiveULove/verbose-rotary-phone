package com.example.model;

import java.io.Serializable;

public class PCControlData extends ControlData implements Serializable {
    public int PCinFollow;
    public Vector2 PCmoveAngle;

    public PCControlData() {
        super();
    }

    // Getters and Setters
    public int getPCinFollow() {
        return PCinFollow;
    }

    public void setPCinFollow(int PCinFollow) {
        this.PCinFollow = PCinFollow;
    }

    public Vector2 getPCmoveAngle() {
        return PCmoveAngle;
    }

    public void setPCmoveAngle(Vector2 PCmoveAngle) {
        this.PCmoveAngle = PCmoveAngle;
    }
}