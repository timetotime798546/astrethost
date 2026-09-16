package com.nativeimagetextreplacer.app;

import android.graphics.PointF;
import java.util.ArrayList;

public class BrushStroke {
    public ArrayList<PointF> points;
    public ArrayList<Integer> colors;
    public float radius;

    public BrushStroke(float radius) {
        this.points = new ArrayList<PointF>();
        this.colors = new ArrayList<Integer>();
        this.radius = radius;
    }
}