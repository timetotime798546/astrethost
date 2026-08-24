package com.arrowflowcrashcourse.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LevelManager {
    public static class LevelData {
        public int levelNumber;
        public int cols;
        public int rows;
        public List<Arrow> arrows;

        public LevelData(int levelNumber, int cols, int rows) {
            this.levelNumber = levelNumber;
            this.cols = cols;
            this.rows = rows;
            this.arrows = new ArrayList<>();
        }
    }

    public static LevelData getLevel(int level) {
        int cols = 5 + (level / 3);
        int rows = 6 + (level / 3);
        LevelData data = new LevelData(level, cols, rows);

        // Stable seed generation per level configuration
        Random rand = new Random(1000L * level + 42L);

        // Dynamic constraint scaling to avoid deadlocks & maintain sequence challenge
        int maxPossibleArrows = (int) (cols * rows * 0.55f);
        int arrowCount = 8 + (level * 4);
        if (arrowCount > maxPossibleArrows) {
            arrowCount = maxPossibleArrows;
        }
        if (arrowCount < 5) {
            arrowCount = 5;
        }

        List<Arrow> generatedArrows = new ArrayList<>();
        boolean success = false;
        int outerAttempts = 0;

        // Constructive Guaranteed Playout Generator
        while (!success && outerAttempts < 50) {
            generatedArrows.clear();
            boolean[][] occupied = new boolean[cols][rows];
            success = true;

            int idCounter = 1;
            for (int i = 0; i < arrowCount; i++) {
                boolean placed = false;
                // Attempt placement search
                for (int attempt = 0; attempt < 200; attempt++) {
                    int x = rand.nextInt(cols);
                    int y = rand.nextInt(rows);

                    if (!occupied[x][y]) {
                        ArrowDirection dir = ArrowDirection.values()[rand.nextInt(4)];
                        // Verify exit path is empty of previous reverse steps to prevent unwinnable deadlocks
                        if (isPathClear(x, y, dir, occupied, cols, rows)) {
                            occupied[x][y] = true;
                            generatedArrows.add(new Arrow(idCounter++, x, y, dir));
                            placed = true;
                            break;
                        } 
                    }
                }

                if (!placed) {
                    // If board space is congested, lower count slightly and recreate configuration
                    arrowCount = Math.max(5, arrowCount - 1);
                    success = false;
                    break;
                }
            }
            outerAttempts++;
        }

        // Shuffle the result list to disguise sequence order from mechanical memory cheats
        Collections.shuffle(generatedArrows, rand);
        data.arrows.addAll(generatedArrows);

        return data;
    }

    private static boolean isPathClear(int x, int y, ArrowDirection dir, boolean[][] occupied, int cols, int rows) {
        int curX = x;
        int curY = y;
        while (true) {
            switch (dir) {
                case UP:
                    curY--;
                    break;
                case DOWN:
                    curY++;
                    break;
                case LEFT:
                    curX--;
                    break;
                case RIGHT:
                    curX++;
                    break;
            }
            // Reached safety border with no collision
            if (curX < 0 || curX >= cols || curY < 0 || curY >= rows) {
                break;
            }
            // Trajectory collision occurs, blocking forward progression
            if (occupied[curX][curY]) {
                return false;
            }
        }
        return true;
    }
}