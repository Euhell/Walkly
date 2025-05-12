package com.example.samsungproject.types;

import java.io.Serializable;

public class POI implements Serializable {
    public final String key;
    public final String value;
    public final String label;
    public boolean isSelected = false;

    public POI(String key, String value, String label) {
        this.key = key;
        this.value = value;
        this.label = label;
    }

    public String getTag() {
        return value != null ? "[" + key + "=" + value + "]" : "[" + key + "]";
    }

    @Override
    public String toString() {
        return label;
    }
}