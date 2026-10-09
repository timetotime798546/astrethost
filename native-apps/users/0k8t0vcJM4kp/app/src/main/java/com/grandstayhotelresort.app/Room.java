package com.grandstayhotelresort.app;

import java.io.Serializable;

public class Room implements Serializable {
    public String name;
    public String description;
    public double pricePerNight;
    public double rating;
    public String[] imageUrls;

    public Room(String name, String description, double pricePerNight, double rating, String[] imageUrls) {
        this.name = name;
        this.description = description;
        this.pricePerNight = pricePerNight;
        this.rating = rating;
        this.imageUrls = imageUrls;
    }
}