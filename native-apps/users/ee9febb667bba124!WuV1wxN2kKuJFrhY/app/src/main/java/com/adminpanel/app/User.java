package com.adminpanel.app;

public class User {
    private String name;
    private String role;
    private boolean active;

    public User(String name, String role, boolean active) {
        this.name = name;
        this.role = role;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " (" + role + ") - " + (active ? "ACTIVE" : "OFFLINE");
    }
}