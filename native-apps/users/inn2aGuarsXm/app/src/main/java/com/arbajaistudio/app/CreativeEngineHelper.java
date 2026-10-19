package com.arbajaistudio.app;

import java.util.ArrayList;
import java.util.List;

public class CreativeEngineHelper {

    public static class SceneItem {
        public int sceneNumber;
        public String visualDescription;
        public String cameraInstruction;
        public String dialogue;
        public String aiVideoPrompt;
    }

    public static double calculateSpeechDurationSeconds(String text, double wordsPerMinute) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        String[] words = text.trim().split("\\s+");
        int wordCount = words.length;
        if (wordsPerMinute <= 0) {
            wordsPerMinute = 140.0;
        }
        // Local deterministic calculation: (word_count / speech_rate_wpm) * 60
        return ((double) wordCount / wordsPerMinute) * 60.0;
    }

    public static String getResolutionForAspect(String aspect) {
        if ("9:16".equals(aspect)) {
            return "1080 x 1920 (Portrait / Reels / Shorts)";
        } else if ("1:1".equals(aspect)) {
            return "1080 x 1080 (Square / Feed)";
        } else {
            return "1920 x 1080 (Landscape / YouTube 16:9)";
        }
    }

    public static List<SceneItem> parseStoryIntoScenes(String rawStory, String characterContext, String cameraStyle) {
        List<SceneItem> scenes = new ArrayList<SceneItem>();
        if (rawStory == null || rawStory.trim().isEmpty()) {
            return scenes;
        }

        String[] rawSentences = rawStory.split("(?<=[.!?\\n])\\s*");
        List<String> cleanSentences = new ArrayList<String>();
        for (int i = 0; i < rawSentences.length; i++) {
            String s = rawSentences[i].trim();
            if (!s.isEmpty()) {
                cleanSentences.add(s);
            }
        }

        if (cleanSentences.isEmpty()) {
            cleanSentences.add(rawStory.trim());
        }

        String[] angles = new String[] {
            "Cinematic Wide Establishing Shot, 24fps, golden hour lighting",
            "Slow Forward Dolly-in, Medium Close-up, shallow depth of field",
            "Dynamic Low Angle Pan, anamorphic lens flare, high contrast",
            "Over-the-Shoulder Tracking Shot, soft directional key light",
            "High Angle Aerial Pull-back, dramatic atmospheric haze"
        };

        for (int i = 0; i < cleanSentences.size(); i++) {
            SceneItem scene = new SceneItem();
            scene.sceneNumber = i + 1;
            String sentence = cleanSentences.get(i);

            scene.visualDescription = "Visual environment depicting: " + sentence;
            scene.cameraInstruction = angles[i % angles.length] + ", style: " + cameraStyle;
            scene.dialogue = sentence;

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Ultra realistic cinematic movie clip, 8k resolution. ");
            if (characterContext != null && !characterContext.trim().isEmpty()) {
                promptBuilder.append("Featuring character: ").append(characterContext).append(". ");
            }
            promptBuilder.append(sentence).append(". ");
            promptBuilder.append("Camera movement: ").append(scene.cameraInstruction);
            scene.aiVideoPrompt = promptBuilder.toString();

            scenes.add(scene);
        }

        return scenes;
    }
}