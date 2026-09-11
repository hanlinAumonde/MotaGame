package com.demo.mota.engine.skill;

/**
 * 技能类型。展示层用 {@link #getDisplayName()} 标注在技能名之后。
 */
public enum SkillType {
    PASSIVE("被动技能"),
    ACTIVE("主动技能");

    private final String displayName;

    SkillType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SkillType fromString(String value) {
        if (value == null || value.isBlank()) {
            return PASSIVE;
        }
        return SkillType.valueOf(value.trim().toUpperCase());
    }
}
