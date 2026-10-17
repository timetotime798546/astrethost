package com.turbo3dracer.app;

import android.graphics.Color;

public class CarModel {
    private String name;
    private int primaryColor;
    private int accentColor;
    private int topSpeed;       // e.g. 180 to 320 km/h
    private int acceleration;   // 1 to 10
    private int handling;       // 1 to 10
    private int nitroCapacity;  // 1 to 10
    private int unlockCost;
    private boolean unlocked;

    public CarModel(String name, int primaryColor, int accentColor, int topSpeed,
                    int acceleration, int handling, int nitroCapacity, int unlockCost, boolean unlocked) {
        this.name = name;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.topSpeed = topSpeed;
        this.acceleration = acceleration;
        this.handling = handling;
        this.nitroCapacity = nitroCapacity;
        this.unlockCost = unlockCost;
        this.unlocked = unlocked;
    }

    public String getName() { return name; }
    public int getPrimaryColor() { return primaryColor; }
    public int getAccentColor() { return accentColor; }
    public int getTopSpeed() { return topSpeed; }
    public int getAcceleration() { return acceleration; }
    public int getHandling() { return handling; }
    public int getNitroCapacity() { return nitroCapacity; }
    public int getUnlockCost() { return unlockCost; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public void upgradeSpeed() { this.topSpeed += 10; }
    public void upgradeAccel() { if (acceleration < 10) acceleration++; }
    public void upgradeHandling() { if (handling < 10) handling++; }
    public void upgradeNitro() { if (nitroCapacity < 10) nitroCapacity++; }

    public static CarModel[] getDefaultFleet() {
        return new CarModel[] {
            new CarModel("PHANTOM GT", Color.rgb(0, 229, 255), Color.rgb(255, 255, 255), 210, 6, 7, 6, 0, true),
            new CarModel("VIPER GTS", Color.rgb(255, 0, 85), Color.rgb(255, 214, 0), 245, 8, 8, 7, 800, false),
            new CarModel("APEX HYPER", Color.rgb(0, 230, 118), Color.rgb(124, 77, 255), 290, 9, 9, 9, 2000, false),
            new CarModel("CYBER TITAN", Color.rgb(255, 171, 0), Color.rgb(213, 0, 0), 320, 10, 9, 10, 4500, false)
        };
    }
}