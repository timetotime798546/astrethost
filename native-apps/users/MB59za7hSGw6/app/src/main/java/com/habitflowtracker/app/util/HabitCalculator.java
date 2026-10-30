package com.habitflowtracker.app.util;

import com.habitflowtracker.app.model.Habit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HabitCalculator {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static String getTodayString() {
        return DATE_FORMAT.format(new Date());
    }

    public static String getPastDateString(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return DATE_FORMAT.format(cal.getTime());
    }

    /**
     * Calculates the completion percentage for today.
     * Formula: (completedToday / totalHabits) * 100
     */
    public static int calculateTodayCompletionRate(List<Habit> habits) {
        if (habits == null || habits.isEmpty()) {
            return 0;
        }
        int total = habits.size();
        int completed = countCompletedToday(habits);
        return (int) Math.round(((double) completed / (double) total) * 100.0);
    }

    /**
     * Counts how many habits are completed today.
     */
    public static int countCompletedToday(List<Habit> habits) {
        if (habits == null || habits.isEmpty()) {
            return 0;
        }
        String todayStr = getTodayString();
        int count = 0;
        for (int i = 0; i < habits.size(); i++) {
            Habit habit = habits.get(i);
            if (habit.isCompletedOn(todayStr)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Calculates current consecutive streak in days.
     * If completed today, checks consecutive backwards starting today.
     * If not completed today, checks if completed yesterday to maintain the streak.
     */
    public static int calculateCurrentStreak(Habit habit) {
        if (habit == null || habit.getCompletedDates() == null || habit.getCompletedDates().isEmpty()) {
            return 0;
        }
        Set<String> dates = habit.getCompletedDates();
        String todayStr = getTodayString();
        String yesterdayStr = getPastDateString(1);

        int streak = 0;
        int checkDaysAgo = 0;

        if (dates.contains(todayStr)) {
            streak++;
            checkDaysAgo = 1;
        } else if (dates.contains(yesterdayStr)) {
            checkDaysAgo = 1;
        } else {
            return 0;
        }

        while (true) {
            String checkDate = getPastDateString(checkDaysAgo);
            if (dates.contains(checkDate)) {
                if (checkDaysAgo > 0 && !dates.contains(todayStr) && checkDaysAgo == 1) {
                    streak++;
                } else if (checkDaysAgo > 0 && dates.contains(todayStr)) {
                    streak++;
                } else if (checkDaysAgo > 1) {
                    streak++;
                }
                checkDaysAgo++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Calculates the best all-time streak for a habit over the last 365 days.
     */
    public static int calculateBestStreak(Habit habit) {
        if (habit == null || habit.getCompletedDates() == null || habit.getCompletedDates().isEmpty()) {
            return 0;
        }
        Set<String> dates = habit.getCompletedDates();
        int bestStreak = 0;
        int currentCount = 0;

        for (int i = 365; i >= 0; i--) {
            String dateStr = getPastDateString(i);
            if (dates.contains(dateStr)) {
                currentCount++;
                if (currentCount > bestStreak) {
                    bestStreak = currentCount;
                }
            } else {
                currentCount = 0;
            }
        }
        return bestStreak;
    }

    /**
     * Calculates total all-time check-ins for the habit.
     */
    public static int calculateTotalCheckIns(Habit habit) {
        if (habit == null || habit.getCompletedDates() == null) {
            return 0;
        }
        return habit.getCompletedDates().size();
    }

    /**
     * Calculates weekly completion rate across all habits for the past 7 days.
     */
    public static int calculateWeeklyCompletionRate(List<Habit> habits) {
        if (habits == null || habits.isEmpty()) {
            return 0;
        }
        int totalPossible = habits.size() * 7;
        int actualCompletions = 0;

        for (int day = 0; day < 7; day++) {
            String dateStr = getPastDateString(day);
            for (int i = 0; i < habits.size(); i++) {
                if (habits.get(i).isCompletedOn(dateStr)) {
                    actualCompletions++;
                }
            }
        }

        if (totalPossible == 0) return 0;
        return (int) Math.round(((double) actualCompletions / (double) totalPossible) * 100.0);
    }

    /**
     * Finds highest active streak across all habits.
     */
    public static int calculateHighestActiveStreak(List<Habit> habits) {
        if (habits == null || habits.isEmpty()) {
            return 0;
        }
        int highest = 0;
        for (int i = 0; i < habits.size(); i++) {
            int streak = calculateCurrentStreak(habits.get(i));
            if (streak > highest) {
                highest = streak;
            }
        }
        return highest;
    }
}