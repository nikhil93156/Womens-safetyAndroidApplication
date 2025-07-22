package com.example.safety;

public class SOSModel {
    private final String name;
    private final String number;

    public SOSModel(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }
}