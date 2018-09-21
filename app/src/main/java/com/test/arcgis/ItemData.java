package com.test.arcgis;

/**
 * Created by Superman on 2018/7/31.
 */

public class ItemData {
    private final String text;
    private final Integer imageId;

    public ItemData(String text, Integer imageId) {
        this.text = text;
        this.imageId = imageId;
    }

    public String getText() {
        return text;
    }

    public Integer getImageId() {
        return imageId;
    }
}
