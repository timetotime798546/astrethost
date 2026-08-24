package com.arrowflowcrashcourse.app;

public class Particle {
    public float x, y;
    public float vx, vy;
    public int color;
    public float size;
    public float alpha;
    public float life;

    public Particle(float x, float y, float vx, float vy, int color, float size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.size = size;
        this.alpha = 1.0f;
        this.life = 1.0f;
    }

    public void update(float dt) {
        x += vx * dt;
        y += vy * dt;
        life -= dt * 1.5f;
        if (life < 0) life = 0;
        alpha = life;
    }
}