package com.demo.mota.engine.enums;

public enum TileType {
    BACKGROUND("background"),
    WALL("wall"),
    DOOR("door"),
    FLOOR_SWITCHER("floor_switcher"),
    TRICKY("tricky");

    private final String value;

    TileType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TileType fromString(String type) {
        return switch (type.toUpperCase()) {
            case "BACKGROUND" -> BACKGROUND;
            case "WALL" -> WALL;
            case "DOOR" -> DOOR;
            case "FLOOR_SWITCHER" -> FLOOR_SWITCHER;
            case "TRICKY" -> TRICKY;
            default -> throw new IllegalArgumentException("Invalid tile type: " + type);
        };
    }
}
