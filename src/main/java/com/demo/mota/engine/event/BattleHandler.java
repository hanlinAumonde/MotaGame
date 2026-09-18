package com.demo.mota.engine.event;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;

public class BattleHandler {

    /**
     * 结算一场战斗。
     *
     * <p><b>不再拒绝致命战斗</b>：撞上去就是打，血照扣，扣光了就是死。
     * 地图上的红色伤害数字因此从「这只怪不能碰」变成了「碰了会死」的警告，
     * 要不要冒险交给玩家。判死不在这里做——扣完血由调用链上层统一检查
     * {@code PlayerStateManager.isDead()}，好让岩浆、毒这些将来的掉血途径共用一个判定点。
     *
     * @return 是否击败了怪物；false 表示玩家在这一战里倒下了
     */
    public boolean executeBattle(PlayerStateManager player, GameMap map, Position monsterPos) {
        Monster monster = map.getMonsterAt(monsterPos);
        if (monster == null) {
            return true;
        }

        GameNumber currentHealth = player.getCurrentHP();

        // OVER_KILL：打不动（伤害为 0 或回合超限），耗到最后倒下的只会是玩家
        if (monster.getCurrentDamageRange() == DamageRange.OVER_KILL) {
            player.updateState(StateType.HP, GameNumber.ZERO);
            return false;
        }

        // 预计算伤害即打完这一战要挨的总伤害；够不够扛得住只看当前生命值
        GameNumber damage = monster.getCurrentDamage();
        if (damage.compareTo(currentHealth) >= 0) {
            player.updateState(StateType.HP, GameNumber.ZERO);
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
