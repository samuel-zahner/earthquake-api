package com.earthquake_processor_api.app.util;

public enum JsonResponseEnum {
    FEATURE("Feature"),
    FEATURE_COLLECTION("FeatureCollection");

    private final String value;

    JsonResponseEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
