package com.demo.mota.engine.state.monster;

import com.demo.mota.engine.battle.BattleResult;
import com.demo.mota.engine.battle.BattleSimulator;
import com.demo.mota.engine.battle.BattleSnapshot;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.AbstractCharacterState;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.Map;

public class Monster extends AbstractCharacterState {
    private long goldReward;
    private GameNumber experienceReward;

    private GameNumber currentDamage;
    private DamageRange currentDamageRange;

    public Monster(String characterId, String characterName, Map<StateType, GameNumber> stateMap, Direction currentDirection,
                   PlayerStateManager playerStateManager, long goldReward, GameNumber experienceReward) {
        super(characterId, characterName, stateMap, currentDirection);
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

    public void setCurrentDamage(GameNumber currentDamage) {
        this.currentDamage = currentDamage;
    }

    public void setCurrentDamageRange(DamageRange currentDamageRange) {
        this.currentDamageRange = currentDamageRange;
    }

    public void updateCurrentDamage(PlayerStateManager playerStateManager) {
        BattleSnapshot playerSnapshot = new BattleSnapshot(
                playerStateManager.getStateValue(StateType.HP),
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
    }

}
