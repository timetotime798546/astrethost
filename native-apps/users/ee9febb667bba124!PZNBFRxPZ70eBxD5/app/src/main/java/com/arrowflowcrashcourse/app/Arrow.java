package com.arrowflowcrashcourse.app;

public class Arrow {
    public int id;
    public int gridX;
    public int gridY;
    public float posX;
    public float posY;
    public float startX;
    public float startY;
    public ArrowDirection direction;
    public ArrowState state;
    public float speed;

    public Arrow(int id, int gridX, int gridY, ArrowDirection direction) {
        this.id = id;
        this.gridX = gridX;
        this.gridY = gridY;
        this.direction = direction;
        this.state = ArrowState.IDLE;
        this.speed = 0.0f;
    }
}