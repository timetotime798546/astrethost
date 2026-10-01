package com.saloonbooking.app;

import java.util.HashMap;
import java.util.Map;

public class Lang {
    private static String currentLang = "en"; // "en" or "hi"

    private static final Map<String, String> en = new HashMap<>();
    private static final Map<String, String> hi = new HashMap<>();

    static {
        // App title & login
        en.put("app_name", "Salon Ease");
        hi.put("app_name", "सैलून ईज़");

        en.put("login_title", "Login to Your Account");
        hi.put("login_title", "अपने खाते में लॉगिन करें");

        en.put("signup_title", "Create a New Account");
        hi.put("signup_title", "नया खाता बनाएं");

        en.put("email_hint", "Email Address");
        hi.put("email_hint", "ईमेल पता");

        en.put("password_hint", "Password");
        hi.put("password_hint", "पासवर्ड");

        en.put("btn_login", "LOG IN");
        hi.put("btn_login", "लॉग इन करें");

        en.put("btn_signup", "SIGN UP");
        hi.put("btn_signup", "साइन अप करें");

        en.put("switch_to_signup", "Don't have an account? Sign Up");
        hi.put("switch_to_signup", "खाता नहीं है? साइन अप करें");

        en.put("switch_to_login", "Already have an account? Log In");
        hi.put("switch_to_login", "पहले से खाता है? लॉगिन करें");

        // Dashboard/Booking strings
        en.put("welcome", "Welcome to Salon Ease");
        hi.put("welcome", "सैलून ईज़ में आपका स्वागत है");

        en.put("book_appointment", "Book a New Appointment");
        hi.put("book_appointment", "नई नियुक्ति बुक करें");

        en.put("select_service", "1. Select Salon Service:");
        hi.put("select_service", "1. सैलून सेवा चुनें:");

        en.put("select_stylist", "2. Choose Stylist:");
        hi.put("select_stylist", "2. स्टाइलिस्ट चुनें:");

        en.put("select_date_time", "3. Select Date & Time:");
        hi.put("select_date_time", "3. दिनांक और समय चुनें:");

        en.put("base_price", "Base Price: ₹");
        hi.put("base_price", "मूल्य: ₹");

        en.put("gst_tax", "GST Tax (18%): ₹");
        hi.put("gst_tax", "जीएसटी टैक्स (18%): ₹");

        en.put("total_price", "Total Price: ₹");
        hi.put("total_price", "कुल कीमत: ₹");

        en.put("btn_confirm_booking", "CONFIRM BOOKING");
        hi.put("btn_confirm_booking", "बुकिंग की पुष्टि करें");

        en.put("my_bookings", "My Appointment History");
        hi.put("my_bookings", "मेरा नियुक्ति इतिहास");

        en.put("btn_logout", "LOGOUT");
        hi.put("btn_logout", "लॉगआउट");

        en.put("btn_cancel", "CANCEL");
        hi.put("btn_cancel", "रद्द करें");

        en.put("booking_success", "Appointment booked successfully!");
        hi.put("booking_success", "नियुक्ति सफलतापूर्वक बुक की गई!");

        en.put("booking_error", "Failed to complete booking.");
        hi.put("booking_error", "बुकिंग पूरी करने में विफल।");

        en.put("cancelling", "Cancelling booking...");
        hi.put("cancelling", "बुकिंग रद्द की जा रही है...");

        en.put("auth_failed", "Authentication failed. Try again.");
        hi.put("auth_failed", "प्रमाणीकरण विफल रहा। पुनः प्रयास करें।");

        en.put("fill_all", "Please fill all credential fields.");
        hi.put("fill_all", "कृपया सभी क्रेडेंशियल फ़ील्ड भरें।");

        en.put("reg_success_login", "Registration successful! Logging you in...");
        hi.put("reg_success_login", "पंजीकरण सफल! लॉग इन किया जा रहा है...");
    }

    public static void setLanguage(String lang) {
        currentLang = lang;
    }

    public static String getLanguage() {
        return currentLang;
    }

    public static String get(String key) {
        if ("hi".equalsIgnoreCase(currentLang)) {
            String val = hi.get(key);
            return val != null ? val : en.get(key);
        }
        return en.get(key);
    }
}