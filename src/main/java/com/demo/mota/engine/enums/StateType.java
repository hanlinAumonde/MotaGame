package com.demo.mota.engine.enums;

public enum StateType {
    HP("health"),
    ATK("attack"),
    DEF("defense");

    private final String value;

    StateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StateType fromString(String stateType) {
        return switch (stateType.toUpperCase()) {
            case "HEALTH" -> HP;
            case "ATTACK" -> ATK;
            case "DEFENSE" -> DEF;
            default -> throw new IllegalArgumentException("Invalid state type: " + stateType);
        };
    }
}
