package com.demo.mota.engine.state.monster;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.AbstractCharacterState;
import com.demo.mota.engine.state.GameNumber;
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

    private void updateCurrentDamageRange(GameNumber playerHealth, boolean isOverKill) {
        if(isOverKill) {
            setCurrentDamageRange(DamageRange.OVER_KILL);
            return;
        }
        float rateOfDamage = this.currentDamage.dividedBy(playerHealth).toFloat();
        if(rateOfDamage <= 0){
            setCurrentDamageRange(DamageRange.NONE);
        } else if(rateOfDamage > 0 && rateOfDamage <= 0.3f) {
            setCurrentDamageRange(DamageRange.LOW);
        } else if(rateOfDamage > 0.3f && rateOfDamage <= 0.6f) {
            setCurrentDamageRange(DamageRange.MEDIUM);
        } else if(rateOfDamage > 0.6f && rateOfDamage < 1.0f) {
            setCurrentDamageRange(DamageRange.HIGH);
        } else if(rateOfDamage >= 1.0f && rateOfDamage <= 2.0f) {
            setCurrentDamageRange(DamageRange.DEATH);
        }
    }

    public void updateCurrentDamage(PlayerStateManager playerStateManager) {
        GameNumber playerAttack = playerStateManager.getEffectiveATK();
        GameNumber damageToMonsterPerRound = playerAttack.minus(this.getStateValue(StateType.DEF));
        boolean isOverKill = false;
        if(damageToMonsterPerRound.isNonPositive()) {
            setCurrentDamage(GameNumber.ZERO);
            isOverKill = true;
        } else {
            GameNumber playerDefense = playerStateManager.getStateValue(StateType.DEF);
            GameNumber damageToPlayerPerRound = this.getStateValue(StateType.ATK).minus(playerDefense);
            if(damageToPlayerPerRound.isNonPositive()) {
                setCurrentDamage(GameNumber.ZERO);
            } else {
                GameNumber roundsForDefeatMonster = this.getStateValue(StateType.HP).dividedBy(damageToMonsterPerRound);
                if(willOverflow(
                        damageToPlayerPerRound.toLong(),
                        roundsForDefeatMonster.toLong(),
                        playerStateManager.getStateValue(StateType.HP).toLong() * 2)) {
                    setCurrentDamage(GameNumber.ZERO);
                    isOverKill = true;
                } else {
                    setCurrentDamage(damageToPlayerPerRound.times(roundsForDefeatMonster));
                }
            }
        }
        updateCurrentDamageRange(playerStateManager.getStateValue(StateType.HP), isOverKill);
    }

    private static boolean willOverflow(long a, long b, long c) {
        if (a == 0 || b == 0) return false;
        return b > c / a;
    }

}
