package com.demo.mota.engine.skill.effect;

/**
 * 战斗中的阵营。{@link com.demo.mota.engine.battle.BattleEffect} 的钩子是按
 * 「玩家 / 怪物」而非「自己 / 对手」划分的，效果实现需要在构造时就把
 * 技能配置里的相对视角（{@link EffectTarget}）折算成这里的绝对阵营。
 */
public enum BattleSide {
    PLAYER,
    MONSTER;

    public BattleSide opposite() {
        return this == PLAYER ? MONSTER : PLAYER;
    }
}
