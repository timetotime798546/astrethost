package com.studymasterplanner.app.utils;

import com.studymasterplanner.app.models.StudyTask;
import java.util.List;

public class ProgressCalculator {

    public static class Summary {
        public int totalTasks = 0;
        public int completedTasks = 0;
        public int pendingTasks = 0;
        public int totalMinutes = 0;
        public int completedMinutes = 0;
        public int remainingMinutes = 0;
        public int completionPercentage = 0;
    }

    public static Summary calculate(List<StudyTask> taskList) {
        Summary sum = new Summary();
        if (taskList == null || taskList.isEmpty()) {
            return sum;
        }

        sum.totalTasks = taskList.size();

        for (int i = 0; i < taskList.size(); i++) {
            StudyTask task = taskList.get(i);
            int mins = task.getEstimatedMinutes();
            sum.totalMinutes += mins;

            if (task.isCompleted()) {
                sum.completedTasks++;
                sum.completedMinutes += mins;
            } else {
                sum.pendingTasks++;
            }
        }

        sum.remainingMinutes = sum.totalMinutes - sum.completedMinutes;

        if (sum.totalTasks > 0) {
            sum.completionPercentage = (int) Math.round(((double) sum.completedTasks / (double) sum.totalTasks) * 100.0);
        }

        return sum;
    }

    public static int getSubjectCompletion(List<StudyTask> taskList, String subjectName) {
        if (taskList == null || subjectName == null) return 0;
        int subTotal = 0;
        int subDone = 0;

        for (int i = 0; i < taskList.size(); i++) {
            StudyTask task = taskList.get(i);
            if (subjectName.equalsIgnoreCase(task.getSubjectName())) {
                subTotal++;
                if (task.isCompleted()) {
                    subDone++;
                }
            }
        }
        if (subTotal == 0) return 0;
        return (int) Math.round(((double) subDone / (double) subTotal) * 100.0);
    }
}