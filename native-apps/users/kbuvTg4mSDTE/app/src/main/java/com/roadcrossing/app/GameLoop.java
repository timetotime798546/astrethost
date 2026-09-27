package com.roadcrossing.app;

public class GameLoop extends Thread {
    private final GameView gameView;
    private boolean isRunning = false;
    private final long targetDelay = 1000 / 60; // target 60FPS tick

    public GameLoop(GameView gameView) {
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    @Override
    public void run() {
        while (isRunning) {
            long loopStart = System.currentTimeMillis();

            gameView.update();
            gameView.postInvalidate();

            long timeDiff = System.currentTimeMillis() - loopStart;
            long waitTime = targetDelay - timeDiff;

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    // Ignore interruption exceptions
                }
            }
        }
    }
}