package com.arrowflowcrashcourse.app;

import java.util.ArrayList;
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

        Random rand = new Random(1000L * level + 42L);
        int arrowCount = 8 + (level * 5);

        boolean[][] occupied = new boolean[cols][rows];
        int idCounter = 1;

        for (int i = 0; i < arrowCount; i++) {
            int attempts = 0;
            while (attempts < 100) {
                int x = rand.nextInt(cols);
                int y = rand.nextInt(rows);
                if (!occupied[x][y]) {
                    occupied[x][y] = true;
                    ArrowDirection dir = ArrowDirection.values()[rand.nextInt(4)];
                    data.arrows.add(new Arrow(idCounter++, x, y, dir));
                    break;
                }
                attempts++;
            }
        }
        return data;
    }
}