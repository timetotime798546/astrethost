package com.fmradio.app;

public class RadioStation {
    private final double frequency;
    private final String name;
    private final String genre;
    private final String city;
    private final String streamUrl;

    public RadioStation(double frequency, String name, String genre, String city, String streamUrl) {
        this.frequency = frequency;
        this.name = name;
        this.genre = genre;
        this.city = city;
        this.streamUrl = streamUrl;
    }

    public double getFrequency() {
        return frequency;
    }

    public String getName() {
        return name;
    }

    public String getGenre() {
        return genre;
    }

    public String getCity() {
        return city;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getFormattedFrequency() {
        return String.format(java.util.Locale.US, "%.1f", frequency);
    }
}