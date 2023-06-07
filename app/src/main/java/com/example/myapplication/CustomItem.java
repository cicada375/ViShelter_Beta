package com.example.myapplication;

public class CustomItem {
    private String customName;
    private int customNum;

    public CustomItem(String customName, int customNum) {
        this.customName = customName;
        this.customNum = customNum;
    }

    public String getCustomName() {
        return customName;
    }

    public int getCustomNum() {
        return customNum;
    }
}
