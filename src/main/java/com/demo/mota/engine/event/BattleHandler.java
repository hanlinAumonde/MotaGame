package com.demo.mota.engine.event;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;

import java.math.BigInteger;

/**
 * 战斗事件处理器。
 * 战斗没有过程，结果完全取决于预计算的伤害值。
 */
public class BattleHandler {

    /**
     * 执行战斗：判断胜负，胜利则扣血、移除怪物、给予奖励
     *
     * @return true 表示战斗胜利，false 表示战斗失败（游戏结束）
     */
    public boolean executeBattle(PlayerStateManager player, GameMap map, Position monsterPos) {
        Monster monster = map.getMonsterAt(monsterPos);
        if (monster == null) {
            return true;
        }

        // 判断是否能击败怪物
        DamageRange damageRange = monster.getCurrentDamageRange();
        if (damageRange == DamageRange.DEATH || damageRange == DamageRange.OVER_KILL) {
            return false;
        }

        // 扣除玩家生命值
        BigInteger damage = monster.getCurrentDamage();
        BigInteger newHealth = ((BigInteger) player.getStateValue(StateType.HP)).subtract(damage);
        player.updateState(StateType.HP, newHealth);

        // 给予金币和经验奖励
        player.updateGoldAmount(monster.getGoldReward());
        player.updateLevel(monster.getExperienceReward());

        // 从地图移除怪物
        map.removeMonsterAt(monsterPos);

        return true;
    }

    /**
     * 对当前地图所有怪物重新预计算战斗伤害（地图加载后 / 玩家属性变化时调用）
     */
    public void recalculateAllDamage(PlayerStateManager player, GameMap map) {
        for (Monster monster : map.getMonsters().values()) {
            monster.updateCurrentDamage(player);
        }
    }
}
