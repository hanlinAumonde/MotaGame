package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;

/**
 * 战斗双方属性的不可变快照。
 *
 * <p>{@code maxHp} 用于伤害分级：分级以生命上限为基准而非当前生命值，
 * 使同一只怪物的伤害等级不随玩家掉血而漂移。怪物没有独立的上限概念，
 * 走三参构造时 {@code maxHp} 即为 {@code hp}。
 */
public record BattleSnapshot(GameNumber hp, GameNumber maxHp, GameNumber atk, GameNumber def) {

    public BattleSnapshot(GameNumber hp, GameNumber atk, GameNumber def) {
        this(hp, hp, atk, def);
    }
}
