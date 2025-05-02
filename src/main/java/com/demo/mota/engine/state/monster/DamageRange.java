package com.demo.mota.engine.state.monster;

public enum DamageRange {
    NONE(0), // No damage
    LOW(1), // Low damage, 0% < damage <= 30%
    MEDIUM(2), // Medium damage, 30% < damage <= 60%
    HIGH(3), // High damage, 60% < damage < 100%
    DEATH(4), // Death damage, 100% < damage <= 200%
    OVER_KILL(5);// Overkill damage, 200% < damage

    private final int value;
    DamageRange(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
