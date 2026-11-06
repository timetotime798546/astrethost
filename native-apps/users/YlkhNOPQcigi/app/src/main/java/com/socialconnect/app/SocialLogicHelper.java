package com.socialconnect.app;

public class SocialLogicHelper {

    public static String formatTimeAgo(long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timestampMillis;

        if (diff < 0) {
            diff = 0;
        }

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else {
            return days + "d ago";
        }
    }

    public static int calculateEngagementScore(int likes, int commentsCount) {
        return likes + (commentsCount * 2);
    }

    public static String formatLikesCount(int likes) {
        if (likes <= 0) {
            return "0 Likes";
        } else if (likes == 1) {
            return "1 Like";
        } else {
            return likes + " Likes";
        }
    }
}