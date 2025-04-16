package com.demo.mota.engine.enums;

public enum KeyColor {
    RED("red"),
    GREEN("green"),
    BLUE("blue"),
    YELLOW("yellow");

    private final String value;

    KeyColor(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static KeyColor fromString(String color) {
        return switch (color.toUpperCase()) {
            case "RED" -> RED;
            case "GREEN" -> GREEN;
            case "BLUE" -> BLUE;
            case "YELLOW" -> YELLOW;
            default -> throw new IllegalArgumentException("Invalid color: " + color);
        };
    }
}
