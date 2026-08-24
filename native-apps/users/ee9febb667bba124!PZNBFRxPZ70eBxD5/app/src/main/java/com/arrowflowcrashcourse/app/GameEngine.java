package com.arrowflowcrashcourse.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private int currentLevel;
    private int score;
    private LevelManager.LevelData currentLevelData;
    private ScoreManager scoreManager;
    private boolean isGameOver;
    private boolean isLevelComplete;
    private List<Particle> particles;
    private GameListener listener;

    public interface GameListener {
        void onLevelComplete();
        void onGameOver();
        void onScoreUpdated(int score);
        void onParticlesSpawned();
    }

    public GameEngine(Context context, int level, GameListener listener) {
        this.currentLevel = level;
        this.scoreManager = new ScoreManager(context);
        this.score = 0;
        this.particles = new ArrayList<>();
        this.listener = listener;
        this.isGameOver = false;
        this.isLevelComplete = false;
        loadLevel(level);
    }

    public void loadLevel(int level) {
        this.currentLevel = level;
        this.currentLevelData = LevelManager.getLevel(level);
        this.isGameOver = false;
        this.isLevelComplete = false;
        this.particles.clear();
    }

    public int getCurrentLevel() { 
        return currentLevel;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        score += points;
        scoreManager.updateHighScore(score);
        if (listener != null) {
            listener.onScoreUpdated(score);
        }
    }

    public LevelManager.LevelData getLevelData() {
        return currentLevelData;
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isLevelComplete() {
        return isLevelComplete;
    }

    public boolean triggerArrow(Arrow arrow, float cellWidth, float cellHeight) {
        if (isGameOver || isLevelComplete) return false;
        if (arrow.state != ArrowState.IDLE) return false;

        arrow.state = ArrowState.MOVING;
        arrow.speed = (cellWidth + cellHeight) * 3.0f;
        SoundManager.playLaunchSound();
        return true;
    }

    public void update(float dt, float cellWidth, float cellHeight, int width, int height) {
        if (isGameOver) {
            updateParticles(dt);
            return;
        }

        boolean anyMoving = false;
        boolean anyIdle = false;
        List<Arrow> arrows = currentLevelData.arrows;

        for (int i = 0; i < arrows.size(); i++) {
            Arrow arrow = arrows.get(i);
            if (arrow.state == ArrowState.MOVING) {
                anyMoving = true;
                switch (arrow.direction) {
                    case UP:
                        arrow.posY -= arrow.speed * dt;
                        break;
                    case DOWN:
                        arrow.posY += arrow.speed * dt;
                        break;
                    case LEFT:
                        arrow.posX -= arrow.speed * dt;
                        break;
                    case RIGHT:
                        arrow.posX += arrow.speed * dt;
                        break;
                }

                boolean exited = false;
                if (arrow.direction == ArrowDirection.UP && arrow.posY < -cellHeight) exited = true;
                else if (arrow.direction == ArrowDirection.DOWN && arrow.posY > height + cellHeight) exited = true;
                else if (arrow.direction == ArrowDirection.LEFT && arrow.posX < -cellWidth) exited = true;
                else if (arrow.direction == ArrowDirection.RIGHT && arrow.posX > width + cellWidth) exited = true;

                if (exited) {
                    arrow.state = ArrowState.EXITED;
                    addScore(100);
                    continue;
                }

                Arrow crashedWith = CollisionDetector.checkCollision(arrow, arrows, cellWidth, cellHeight, 1.0f);
                if (crashedWith != null) {
                    arrow.state = ArrowState.CRASHED;
                    crashedWith.state = ArrowState.CRASHED;
                    isGameOver = true;
                    spawnExplosion(arrow.posX, arrow.posY, 25);
                    spawnExplosion(crashedWith.posX, crashedWith.posY, 25);
                    SoundManager.playCrashSound();
                    if (listener != null) {
                        listener.onGameOver();
                    }
                    break;
                }
            } else if (arrow.state == ArrowState.IDLE) {
                anyIdle = true;
            }
        }

        updateParticles(dt);

        if (!isGameOver && !anyMoving && !anyIdle) {
            isLevelComplete = true;
            scoreManager.unlockLevel(currentLevel + 1);
            SoundManager.playWinSound();
            if (listener != null) {
                listener.onLevelComplete();
            }
        }
    }

    private void spawnExplosion(float x, float y, int count) {
        int[] colors = {
            ThemeManager.getSecondaryAccent(true),
            0xFFFF5500,
            0xFFFFAA00,
            0xFFFFFFFF
        };
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < count; i++) {
            float speed = 100f + r.nextFloat() * 400f;
            double angle = r.nextDouble() * 2 * Math.PI;
            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed);
            int color = colors[r.nextInt(colors.length)];
            float size = 8f + r.nextFloat() * 14f;
            particles.add(new Particle(x, y, vx, vy, color, size));
        }
        if (listener != null) {
            listener.onParticlesSpawned();
        }
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }
}