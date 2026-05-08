package com.demo.mota.engine.event;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.state.GameNumber;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;

public class BattleHandler {

    public boolean executeBattle(PlayerStateManager player, GameMap map, Position monsterPos) {
        Monster monster = map.getMonsterAt(monsterPos);
        if (monster == null) {
            return true;
        }

        DamageRange damageRange = monster.getCurrentDamageRange();
        if (damageRange == DamageRange.DEATH || damageRange == DamageRange.OVER_KILL) {
            return false;
        }

        GameNumber damage = monster.getCurrentDamage();
        GameNumber newHealth = player.getStateValue(StateType.HP).minus(damage);
        player.updateState(StateType.HP, newHealth);

        player.updateGoldAmount(monster.getGoldReward());
        player.updateLevel(monster.getExperienceReward());

        map.removeMonsterAt(monsterPos);

        return true;
    }

    public void recalculateAllDamage(PlayerStateManager player, GameMap map) {
        for (Monster monster : map.getMonsters().values()) {
            monster.updateCurrentDamage(player);
        }
    }
}
