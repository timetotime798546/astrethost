package com.arrowflowcrashcourse.app;

import java.util.List;

public class CollisionDetector {
    public static Arrow checkCollision(Arrow movingArrow, List<Arrow> allArrows, float cellWidth, float cellHeight, float tolerance) {
        if (movingArrow.state != ArrowState.MOVING) {
            return null;
        }

        for (int i = 0; i < allArrows.size(); i++) {
            Arrow other = allArrows.get(i);
            if (other.id == movingArrow.id || other.state == ArrowState.EXITED) {
                continue;
            }

            float dx = movingArrow.posX - other.posX;
            float dy = movingArrow.posY - other.posY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float collisionDistance = (cellWidth + cellHeight) / 2.0f * 0.70f;

            if (distance < collisionDistance) {
                return other;
            }
        }
        return null;
    }
}