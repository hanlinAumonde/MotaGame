package com.demo.mota.engine.catalog;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;

import java.util.Map;

/**
 * 玩家开局时的样子（{@code player.json}）。
 *
 * @param maxHealth 生命上限；缺省（null）时以 {@code health} 作为初始上限
 * @param direction 开局朝向；缺省为朝下
 */
public record PlayerDefinition(String id, String name, GameNumber health, GameNumber maxHealth,
                               GameNumber attack, GameNumber defense, Direction direction) {

    public PlayerDefinition {
        maxHealth = maxHealth == null ? health : maxHealth;
        direction = direction == null ? Direction.DOWN : direction;
    }

    /** 开局的基础属性 */
    public Map<StateType, GameNumber> initialStats() {
        return Map.of(
                StateType.HP, health,
                StateType.MAX_HP, maxHealth,
                StateType.ATK, attack,
                StateType.DEF, defense);
    }
}
