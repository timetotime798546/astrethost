package com.perfectcirclechallenge.app;

import java.util.List;

public class ScoreCalculator {

    public static class EvaluationResult {
        public double overallScore;
        public double roundnessScore;
        public double consistencyScore;
        public double closureScore;
        public String feedbackBadge;
        public float centroidX;
        public float centroidY;
        public float idealRadius;

        public EvaluationResult(double overall, double roundness, double consistency, double closure,
                                String badge, float cx, float cy, float radius) {
            this.overallScore = overall;
            this.roundnessScore = roundness;
            this.consistencyScore = consistency;
            this.closureScore = closure;
            this.feedbackBadge = badge;
            this.centroidX = cx;
            this.centroidY = cy;
            this.idealRadius = radius;
        }
    }

    public static EvaluationResult evaluateCircle(List<CircleCanvasView.Point> points) {
        if (points == null || points.size() < 15) {
            return new EvaluationResult(0.0, 0.0, 0.0, 0.0, "Too Short!", 0f, 0f, 0f);
        }

        int count = points.size();

        // 1. Calculate Centroid (center of mass)
        double sumX = 0;
        double sumY = 0;
        for (int i = 0; i < count; i++) {
            CircleCanvasView.Point p = points.get(i);
            sumX += p.x;
            sumY += p.y;
        }
        float centroidX = (float) (sumX / count);
        float centroidY = (float) (sumY / count);

        // 2. Compute Radii from centroid for every point
        double sumRadius = 0;
        double[] radii = new double[count];
        for (int i = 0; i < count; i++) {
            CircleCanvasView.Point p = points.get(i);
            double dx = p.x - centroidX;
            double dy = p.y - centroidY;
            double r = Math.sqrt(dx * dx + dy * dy);
            radii[i] = r;
            sumRadius += r;
        }

        double avgRadius = sumRadius / count;
        if (avgRadius < 25.0) {
            return new EvaluationResult(0.0, 0.0, 0.0, 0.0, "Too Small!", centroidX, centroidY, (float) avgRadius);
        }

        // 3. Calculate Radius Variance / Mean Absolute Deviation for Roundness
        double varianceSum = 0;
        for (int i = 0; i < count; i++) {
            double diff = Math.abs(radii[i] - avgRadius);
            varianceSum += diff;
        }
        double meanRadiusDeviation = varianceSum / count;
        double relativeRadiusError = meanRadiusDeviation / avgRadius;
        double roundnessScore = Math.max(0.0, 100.0 - (relativeRadiusError * 220.0));

        // 4. Calculate Radial Consistency (Smoothness of adjacent points)
        double stepVarianceSum = 0;
        for (int i = 1; i < count; i++) {
            double stepDiff = Math.abs(radii[i] - radii[i - 1]);
            stepVarianceSum += stepDiff;
        }
        double avgStepDiff = stepVarianceSum / (count - 1);
        double relativeSmoothnessError = avgStepDiff / avgRadius;
        double consistencyScore = Math.max(0.0, 100.0 - (relativeSmoothnessError * 450.0));

        // 5. Calculate Closure Gap (Distance between first and last touch point)
        CircleCanvasView.Point first = points.get(0);
        CircleCanvasView.Point last = points.get(count - 1);
        double gapX = first.x - last.x;
        double gapY = first.y - last.y;
        double gapDistance = Math.sqrt(gapX * gapX + gapY * gapY);
        double relativeGap = gapDistance / (avgRadius * 2.0);
        double closureScore = Math.max(0.0, 100.0 - (relativeGap * 180.0));

        // 6. Angular Coverage Evaluation (Verify user covered approximately full 360 degrees)
        boolean[] angleBuckets = new boolean[12];
        for (int i = 0; i < count; i++) {
            CircleCanvasView.Point p = points.get(i);
            double angle = Math.atan2(p.y - centroidY, p.x - centroidX); // -PI to PI
            if (angle < 0) {
                angle += 2 * Math.PI;
            }
            int bucket = (int) ((angle / (2 * Math.PI)) * 12);
            if (bucket >= 0 && bucket < 12) {
                angleBuckets[bucket] = true;
            }
        }
        int coveredBuckets = 0;
        for (int b = 0; b < 12; b++) {
            if (angleBuckets[b]) {
                coveredBuckets++;
            }
        }
        double angularMultiplier = (double) coveredBuckets / 12.0;

        // Combine using deterministic native formula: 50% Roundness + 30% Consistency + 20% Closure
        double combined = (roundnessScore * 0.50) + (consistencyScore * 0.30) + (closureScore * 0.20);
        double finalScore = Math.max(0.0, Math.min(100.0, combined * angularMultiplier));

        // Round to 1 decimal place
        finalScore = Math.round(finalScore * 10.0) / 10.0;
        roundnessScore = Math.round(roundnessScore * 10.0) / 10.0;
        consistencyScore = Math.round(consistencyScore * 10.0) / 10.0;
        closureScore = Math.round(closureScore * 10.0) / 10.0;

        // Dynamic Feedback Badges
        String badge;
        if (finalScore >= 95.0) {
            badge = "Legendary!";
        } else if (finalScore >= 90.0) {
            badge = "Near Perfect!";
        } else if (finalScore >= 80.0) {
            badge = "Great Precision!";
        } else if (finalScore >= 70.0) {
            badge = "Good Effort!";
        } else if (finalScore >= 50.0) {
            badge = "Keep Practicing!";
        } else {
            badge = "Wobbly Shape!";
        }

        return new EvaluationResult(finalScore, roundnessScore, consistencyScore, closureScore,
                badge, centroidX, centroidY, (float) avgRadius);
    }
}