package com.turbo3dracer.app;

public class RoadSegment {
    public int index;
    public float p1WorldX, p1WorldY, p1WorldZ;
    public float p2WorldX, p2WorldY, p2WorldZ;

    // Projected screen points
    public float p1ScreenX, p1ScreenY, p1ScreenW;
    public float p2ScreenX, p2ScreenY, p2ScreenW;

    public float curve;       // Lateral curvature of segment
    public float clipY;       // Maximum rendered Y for pseudo-3D hill clipping
    public int colorGrass;
    public int colorRumble;
    public int colorRoad;
    public int colorLane;

    public boolean hasCoin;
    public float coinX;       // -1.0 to 1.0 (left to right lane)
    public boolean coinCollected;

    public boolean hasObstacle;
    public float obstacleX;

    public RoadSegment(int index) {
        this.index = index;
        this.coinCollected = false;
        this.hasCoin = false;
        this.hasObstacle = false;
    }

    public void project(float cameraX, float cameraY, float cameraZ, float cameraDepth, int width, int height, float roadWidth) {
        // Point 1
        float z1 = p1WorldZ - cameraZ;
        if (z1 <= 0) z1 = 0.01f;
        float scale1 = cameraDepth / z1;
        p1ScreenX = (width / 2.0f) + (scale1 * (p1WorldX - cameraX) * (width / 2.0f));
        p1ScreenY = (height / 2.0f) - (scale1 * (p1WorldY - cameraY) * (height / 2.0f));
        p1ScreenW = scale1 * roadWidth * (width / 2.0f);

        // Point 2
        float z2 = p2WorldZ - cameraZ;
        if (z2 <= 0) z2 = 0.01f;
        float scale2 = cameraDepth / z2;
        p2ScreenX = (width / 2.0f) + (scale2 * (p2WorldX - cameraX) * (width / 2.0f));
        p2ScreenY = (height / 2.0f) - (scale2 * (p2WorldY - cameraY) * (height / 2.0f));
        p2ScreenW = scale2 * roadWidth * (width / 2.0f);
    }
}