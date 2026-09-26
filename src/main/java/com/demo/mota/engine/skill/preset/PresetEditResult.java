package com.demo.mota.engine.skill.preset;

/**
 * 修改预设某一格的结果。除 {@link #OK} 外都表示预设未被改动，界面据此给出提示。
 */
public enum PresetEditResult {
    OK(""),
    NOT_EDITABLE("当前的塔不允许修改技能组合"),
    OUT_OF_RANGE(""),
    NOT_OWNED("尚未掌握该技能"),
    NOT_ACTIVE("被动技能常驻生效，无需编排"),
    CAST_LIMIT_REACHED("该技能在一场战斗中的释放次数已用完");

    private final String message;

    PresetEditResult(String message) {
        this.message = message;
    }

    public boolean isOk() {
        return this == OK;
    }

    /** 给玩家看的提示，成功时为空串 */
    public String getMessage() {
        return message;
    }
}
