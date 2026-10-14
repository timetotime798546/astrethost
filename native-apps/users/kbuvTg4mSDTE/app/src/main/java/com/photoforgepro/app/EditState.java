package com.photoforgepro.app;

public class EditState {
    public float exposure;     // -100 to 100
    public float contrast;     // -100 to 100
    public float saturation;   // -100 to 100
    public float shadows;      // -100 to 100
    public float highlights;   // -100 to 100
    public float warmth;       // -100 to 100
    public float vignette;     // 0 to 100
    public int rotation;       // 0, 90, 180, 270
    public String activePreset; // "Normal", "Cinematic", "Cyberpunk", "GoldenHour", "Noir"

    public EditState() {
        reset();
    }

    public void reset() {
        exposure = 0.0f;
        contrast = 0.0f;
        saturation = 0.0f;
        shadows = 0.0f;
        highlights = 0.0f;
        warmth = 0.0f;
        vignette = 0.0f;
        rotation = 0;
        activePreset = "Normal";
    }

    public EditState copy() {
        EditState next = new EditState();
        next.exposure = this.exposure;
        next.contrast = this.contrast;
        next.saturation = this.saturation;
        next.shadows = this.shadows;
        next.highlights = this.highlights;
        next.warmth = this.warmth;
        next.vignette = this.vignette;
        next.rotation = this.rotation;
        next.activePreset = this.activePreset;
        return next;
    }
}