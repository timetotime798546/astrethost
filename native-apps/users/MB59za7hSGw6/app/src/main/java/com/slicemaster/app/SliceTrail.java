package com.slicemaster.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

public class SliceTrail {

    public static class Point {
        public float x;
        public float y;
        public long time;

        public Point(float x, float y, long time) {
            this.x = x;
            this.y = y;
            this.time = time;
        }
    }

    private static final long TRAIL_LIFETIME = 180; // milliseconds
    private final List<Point> points = new ArrayList<Point>();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public SliceTrail() {
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setColor(Color.parseColor("#00E5FF"));
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);

        corePaint.setStyle(Paint.Style.STROKE);
        corePaint.setColor(Color.WHITE);
        corePaint.setStrokeCap(Paint.Cap.ROUND);
        corePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void addPoint(float x, float y) {
        points.add(new Point(x, y, System.currentTimeMillis()));
    }

    public void clear() {
        points.clear();
    }

    public void update() {
        long now = System.currentTimeMillis();
        for (int i = points.size() - 1; i >= 0; i--) {
            if (now - points.get(i).time > TRAIL_LIFETIME) {
                points.remove(i);
            }
        }
    }

    public boolean hasRecentSegment() {
        return points.size() >= 2;
    }

    public Point getLastPoint() {
        if (points.isEmpty()) return null;
        return points.get(points.size() - 1);
    }

    public Point getPenultimatePoint() {
        if (points.size() < 2) return null;
        return points.get(points.size() - 2);
    }

    public void draw(Canvas canvas) {
        if (points.size() < 2) return;

        long now = System.currentTimeMillis();

        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            float ageProgress = (float) (now - p2.time) / TRAIL_LIFETIME;
            float life = Math.max(0f, Math.min(1f, 1f - ageProgress));

            int alphaGlow = (int) (180 * life);
            int alphaCore = (int) (255 * life);
            float width = 16f * life + 2f;

            glowPaint.setStrokeWidth(width);
            glowPaint.setAlpha(alphaGlow);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, glowPaint);

            corePaint.setStrokeWidth(Math.max(2f, width * 0.45f));
            corePaint.setAlpha(alphaCore);
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, corePaint);
        }
    }

    /**
     * Test intersection between segment (p1 -> p2) and circle (cx, cy, radius).
     */
    public static boolean checkCircleIntersect(float x1, float y1, float x2, float y2, float cx, float cy, float r) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;

        if (lenSq == 0) {
            float distSq = (cx - x1) * (cx - x1) + (cy - y1) * (cy - y1);
            return distSq <= r * r;
        }

        float t = Math.max(0, Math.min(1, ((cx - x1) * dx + (cy - y1) * dy) / lenSq));
        float projX = x1 + t * dx;
        float projY = y1 + t * dy;

        float distSq = (cx - projX) * (cx - projX) + (cy - projY) * (cy - projY);
        return distSq <= r * r;
    }
}