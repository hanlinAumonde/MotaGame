package com.demo.mota.engine.state.monster;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.AbstractCharacterState;
import com.demo.mota.engine.state.PlayerStateManager;

import java.math.BigInteger;
import java.util.Map;

public class Monster extends AbstractCharacterState {
    private long goldReward;
    private BigInteger experienceReward;

    private BigInteger currentDamage;
    private DamageRange currentDamageRange;

    public Monster(String characterId, String characterName, Map<StateType, Object> stateMap, Direction currentDirection,
                   PlayerStateManager playerStateManager, long goldReward, BigInteger experienceReward) {
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

    public BigInteger getExperienceReward() {
        return experienceReward;
    }

    public void setExperienceReward(BigInteger experienceReward) {
        this.experienceReward = experienceReward;
    }

    public BigInteger getCurrentDamage() {
        return currentDamage;
    }

    public DamageRange getCurrentDamageRange() {
        return currentDamageRange;
    }

    public void setCurrentDamage(BigInteger currentDamage) {
        this.currentDamage = currentDamage;
    }

    public void setCurrentDamageRange(DamageRange currentDamageRange) {
        this.currentDamageRange = currentDamageRange;
    }

    private void updateCurrentDamageRange(BigInteger playerHealth, boolean isOverKill) {
        //update the current damage range based on the calculated current damage
        if(isOverKill) {
            setCurrentDamageRange(DamageRange.OVER_KILL);
            return;
        }
        float rateOfDamage = this.currentDamage.divide(playerHealth).floatValue();
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
        BigInteger playerAttack = playerStateManager.getEffectiveATK();
        BigInteger damageToMonsterPerRound = playerAttack.subtract(this.getEffectiveDEF());
        boolean isOverKill = false;
        if(damageToMonsterPerRound.compareTo(BigInteger.ZERO) <= 0) {
            setCurrentDamage(BigInteger.ZERO);
            isOverKill = true;
        } else {
            BigInteger playerDefense = this.getEffectiveDEF();
            BigInteger damageToPlayerPerRound = playerDefense.subtract(this.getEffectiveATK());
            if(damageToPlayerPerRound.compareTo(BigInteger.ZERO) <= 0) {
                setCurrentDamage(BigInteger.ZERO);
            } else {
                BigInteger roundsForDefeatMonster = this.getCharacterHealth().divide(damageToMonsterPerRound);
                if(willOverflow(damageToPlayerPerRound.longValue(), roundsForDefeatMonster.longValue(), 2*playerStateManager.getCharacterHealth().longValue())) {
                    setCurrentDamage(BigInteger.ZERO);
                    isOverKill = true;
                } else {
                    setCurrentDamage(damageToPlayerPerRound.multiply(roundsForDefeatMonster));
                }
            }
        }
        updateCurrentDamageRange(playerStateManager.getCharacterHealth(), isOverKill);
    }

    private static boolean willOverflow(long a, long b, long c) {
        if (a == 0 || b == 0) return false;
        return b > c / a;
    }

}
