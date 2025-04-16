package com.demo.mota.engine.enums;

public enum SpecType {
    UPSTAIRS("upstairs"),
    DOWNSTAIRS("downstairs"),
    TRICK_TILE("trick_tile");

    private final String value;

    SpecType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SpecType fromString(String specType) {
        return switch (specType.toUpperCase()) {
            case "UPSTAIRS" -> UPSTAIRS;
            case "DOWNSTAIRS" -> DOWNSTAIRS;
            case "TRICK_TILE" -> TRICK_TILE;
            default -> throw new IllegalArgumentException("Invalid spec type: " + specType);
        };
    }
}
