package com.demo.mota.engine.event;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;

public class BattleHandler {

    public boolean executeBattle(PlayerStateManager player, GameMap map, Position monsterPos) {
        Monster monster = map.getMonsterAt(monsterPos);
        if (monster == null) {
            return true;
        }

        // OVER_KILL：无论血量多少都打不动
        if (monster.getCurrentDamageRange() == DamageRange.OVER_KILL) {
            return false;
        }

        // 是否致命由当前生命值判定（伤害等级只以生命上限分级，不参与死活判断）
        GameNumber damage = monster.getCurrentDamage();
        GameNumber currentHealth = player.getCurrentHP();
        if (damage.compareTo(currentHealth) >= 0) {
            return false;
        }

        player.updateState(StateType.HP, currentHealth.minus(damage));

        player.updateGoldAmount(monster.getGoldReward());
        player.updateLevel(monster.getExperienceReward());

        return true;
    }

    /**
     * 重算当前地图上所有怪物的预计伤害。
     *
     * <p>地图作为技能效果的上下文一并传入：像「亡灵协同」这种按同层同类数量加成的技能，
     * 每死掉一只同类，其余怪物的数值都会变，因此必须在<b>怪物增减 / 玩家属性变化</b>后
     * 整张地图一起重算，而不是只更新被打的那一只。
     */
    public void recalculateAllDamage(PlayerStateManager player, GameMap map) {
        for (Monster monster : map.getMonsters().values()) {
            monster.updateCurrentDamage(player, map);
        }
    }
}
