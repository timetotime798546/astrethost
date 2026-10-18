package com.radiofmlive.app;

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
        this.id = id;
        this.name = name;
        this.streamUrl = streamUrl;
        this.genre = genre;
        this.language = language;
        this.frequency = frequency;
        this.country = country;
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
}