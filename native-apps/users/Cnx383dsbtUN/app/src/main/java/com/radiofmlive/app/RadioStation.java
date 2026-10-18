package com.radiofmlive.app;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

public class RadioStation implements Serializable {
    private final String id;
    private final String name;
    private final String streamUrl;
    private final String genre;
    private final String language;
    private final String frequency;
    private final String country;

    public RadioStation(String id, String name, String streamUrl, String genre, String language, String frequency, String country) {
        this.id = id != null ? id : "";
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Unknown Station";
        this.streamUrl = streamUrl != null ? streamUrl.trim() : "";
        this.genre = genre != null && !genre.trim().isEmpty() ? genre.trim() : "General";
        this.language = language != null && !language.trim().isEmpty() ? language.trim() : "Global";
        this.frequency = frequency != null && !frequency.trim().isEmpty() ? frequency.trim() : "LIVE FM";
        this.country = country != null && !country.trim().isEmpty() ? country.trim() : "Worldwide";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getGenre() {
        return genre;
    }

    public String getLanguage() {
        return language;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getCountry() {
        return country;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name);
            obj.put("streamUrl", streamUrl);
            obj.put("genre", genre);
            obj.put("language", language);
            obj.put("frequency", frequency);
            obj.put("country", country);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static RadioStation fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        String id = obj.optString("id", "");
        String name = obj.optString("name", "Unknown Station");
        String streamUrl = obj.optString("streamUrl", "");
        String genre = obj.optString("genre", "General");
        String language = obj.optString("language", "Global");
        String frequency = obj.optString("frequency", "LIVE FM");
        String country = obj.optString("country", "Worldwide");
        return new RadioStation(id, name, streamUrl, genre, language, frequency, country);
    }
}