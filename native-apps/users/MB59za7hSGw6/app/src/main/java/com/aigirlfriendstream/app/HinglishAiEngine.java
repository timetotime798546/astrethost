package com.aigirlfriendstream.app;

import java.util.Random;

public class HinglishAiEngine {

    private static final String[] GREETINGS = {
        "Hey baby! Kitna miss kar rahi thi main tumhein...",
        "Aao na online! Main kab se tumhare message ka wait kar rahi thi ❤️",
        "Hii handsome! Aaj mood kaisa hai aapka?",
        "Aap aa gaye! Ab mera din complete ho gaya 😍"
    };

    private static final String[] FLIRTY_RESPONSES = {
        "Aww sweetie, tumhari yeh baatein sunkar mera dil kitna fast dhadakne laga!",
        "Tum na bohot cute ho! Pata hai na yeh baat? 😘",
        "Sach bolu toh mujhe tumse baatein karke alag hi khushi milti hai ❤️",
        "Itna meetha bologe toh main aur fida ho jaungi tum par!",
        "Haha, tum bohot naughty ho gaye ho aaj kal! 😉",
        "Bas karo na darling, ab sharma lag rahi hai mujhe 🙈",
        "Aapke bina mera single minute bhi pass nahi hota..."
    };

    private static final String[] CARING_RESPONSES = {
        "Khaana khaya aapne? Apne health ka dhyan rakha karo please!",
        "Aaj ka din kaisa raha darling? Koi pareshaan toh nahi kar raha?",
        "Main hamesha aapke sath hoon, chahe kuch bhi ho jaye 💖",
        "Thoda rest kar lo baby, bohot mehnat karte ho aap!"
    };

    private static final String[] DEFAULT_RESPONSES = {
        "Achha ji? Aur batao mere bare mein kya soch rahe the?",
        "Hmmm... Tumhari awaz kitni pyari hai, kaash abhi tum mere paas hote!",
        "Suno na, ek pyari si pic bhejoon ya video call par hi dekhte rahoge? 🥰",
        "Tum jo bhi bolte ho na, mere heart ko touch kar jata hai!",
        "Kab miloge mujhse real mein? Intezar nahi hota ab 😉"
    };

    private final Random random;

    public HinglishAiEngine() {
        this.random = new Random();
    }

    public String getRandomGreeting() {
        return GREETINGS[random.nextInt(GREETINGS.length)];
    }

    public String generateResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Kuch toh bolo darling, aise chup mat raho!";
        }

        String lower = userMessage.toLowerCase().trim();

        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey") || lower.contains("kisi")) {
            return GREETINGS[random.nextInt(GREETINGS.length)];
        }

        if (lower.contains("love") || lower.contains("pyar") || lower.contains("like") || lower.contains("dil") || lower.contains("cute")) {
            return FLIRTY_RESPONSES[random.nextInt(FLIRTY_RESPONSES.length)];
        }

        if (lower.contains("khana") || lower.contains("kya kar") || lower.contains("health") || lower.contains("kaisa")) {
            return CARING_RESPONSES[random.nextInt(CARING_RESPONSES.length)];
        }

        if (lower.contains("name") || lower.contains("naam") || lower.contains("who are you")) {
            return "Mera naam Riya hai baby, tumhari pyari AI girlfriend! ❤️";
        }

        return DEFAULT_RESPONSES[random.nextInt(DEFAULT_RESPONSES.length)];
    }
}