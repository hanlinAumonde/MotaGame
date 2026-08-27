package com.demo.mota.engine.state.monster;

/**
 * 伤害等级：按战斗总伤害占玩家<b>生命上限</b>的比例划分。
 *
 * <p>以生命上限为基准，等级只随玩家属性变化而变化，不随当前生命值波动。
 * 是否会在本次战斗中死亡由当前生命值单独判定，与本枚举无关。
 */
public enum DamageRange {
    NONE(0), // 无伤，damage == 0
    LOW(1), // 低伤害，0% < damage <= 30% 生命上限
    MEDIUM(2), // 中等伤害，30% < damage <= 60% 生命上限
    HIGH(3), // 高伤害，60% < damage < 100% 生命上限
    DEATH(4), // 致死伤害，damage >= 100% 生命上限，满血也打不过
    OVER_KILL(5);// 无法击败：打不动（攻击 <= 怪物防御）或回合数超限

    private final int value;
    DamageRange(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
