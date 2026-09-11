package com.demo.mota.engine.state.monster;

import com.demo.mota.engine.battle.BattleResult;
import com.demo.mota.engine.battle.BattleSimulator;
import com.demo.mota.engine.battle.BattleSnapshot;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.AbstractCharacterState;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.List;
import java.util.Map;

public class Monster extends AbstractCharacterState {
    private long goldReward;
    private GameNumber experienceReward;

    private GameNumber currentDamage;
    private DamageRange currentDamageRange;
    /** 战斗预计算出的击杀所需回合数；无法击败时为 0 */
    private int currentRounds;

    public Monster(String characterId, String characterName, Map<StateType, GameNumber> stateMap, Direction currentDirection,
                   PlayerStateManager playerStateManager, long goldReward, GameNumber experienceReward,
                   List<Skill> skills) {
        // 技能由基类统一持有：怪物在构造时从配置带入，玩家可在技能系统落地后逐步习得
        super(characterId, characterName, stateMap, currentDirection, skills);
        this.goldReward = goldReward;
        this.experienceReward = experienceReward;
        this.updateCurrentDamage(playerStateManager);
    }

    public long getGoldReward() {
        return goldReward;
    }

    public void setGoldReward(long goldReward) {
        this.goldReward = goldReward;
    }

    public GameNumber getExperienceReward() {
        return experienceReward;
    }

    public void setExperienceReward(GameNumber experienceReward) {
        this.experienceReward = experienceReward;
    }

    public GameNumber getCurrentDamage() {
        return currentDamage;
    }

    public DamageRange getCurrentDamageRange() {
        return currentDamageRange;
    }

    public int getCurrentRounds() {
        return currentRounds;
    }

    /**
     * 以玩家<b>当前</b>生命值判断这只怪此刻会不会把玩家打死，
     * 与 {@code BattleHandler} 的胜负判定保持同一条件。
     *
     * <p>伤害分级（{@link DamageRange}）以生命上限为基准、不随掉血漂移，
     * 因此「此刻会不会死」需要单独判断，渲染层据此把伤害数字标红。
     */
    public boolean isLethalTo(PlayerStateManager player) {
        if (currentDamage == null || currentDamageRange == DamageRange.OVER_KILL) {
            return false;
        }
        return currentDamage.compareTo(player.getCurrentHP()) >= 0;
    }

    public void setCurrentDamage(GameNumber currentDamage) {
        this.currentDamage = currentDamage;
    }

    public void setCurrentDamageRange(DamageRange currentDamageRange) {
        this.currentDamageRange = currentDamageRange;
    }

    public void updateCurrentDamage(PlayerStateManager playerStateManager) {
        BattleSnapshot playerSnapshot = new BattleSnapshot(
                playerStateManager.getCurrentHP(),
                playerStateManager.getMaxHP(),
                playerStateManager.getEffectiveATK(),
                playerStateManager.getEffectiveDEF()
        );
        BattleSnapshot monsterSnapshot = new BattleSnapshot(
                this.getStateValue(StateType.HP),
                this.getStateValue(StateType.ATK),
                this.getStateValue(StateType.DEF)
        );
        BattleResult result = BattleSimulator.simulate(playerSnapshot, monsterSnapshot);
        this.currentDamage = result.totalDamage();
        this.currentDamageRange = result.damageRange();
        this.currentRounds = result.rounds();
    }

}
