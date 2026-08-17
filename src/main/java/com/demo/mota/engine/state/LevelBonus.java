package com.demo.mota.engine.state;

import com.demo.mota.engine.enums.StateType;

public record LevelBonus(StateType stat, BonusType type, int value) {

    public enum BonusType {
        FLAT,
        PERCENTAGE;

        public static BonusType fromString(String s) {
            return switch (s.toLowerCase()) {
                case "flat" -> FLAT;
                case "percentage" -> PERCENTAGE;
                default -> throw new IllegalArgumentException("Invalid bonus type: " + s);
            };
        }
    }

    public GameNumber apply(GameNumber currentBaseValue) {
        return switch (type) {
            case FLAT -> currentBaseValue.plus(GameNumber.of(value));
            case PERCENTAGE -> {
                GameNumber increment = currentBaseValue.times(GameNumber.of(value))
                        .dividedBy(GameNumber.of(100), true);
                yield currentBaseValue.plus(increment);
            }
        };
    }
}
