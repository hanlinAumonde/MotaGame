package com.demo.mota.engine.enums;

public enum WallType {
    NORMAL("normal"), //普通类型，即可能被破墙镐或炸药等道具破坏的墙
    MAGMA("magma"), //岩浆， 可被冰冻护符冻结
    DARK("dark"); //暗墙，常态下外貌和普通墙一样，但直接接触时会变为可通过的地面

    private final String value;

    WallType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WallType fromString(String wallType) {
        return switch (wallType.toUpperCase()) {
            case "NORMAL" -> NORMAL;
            case "MAGMA" -> MAGMA;
            case "DARK" -> DARK;
            default -> throw new IllegalArgumentException("Invalid wall type: " + wallType);
        };
    }
}
