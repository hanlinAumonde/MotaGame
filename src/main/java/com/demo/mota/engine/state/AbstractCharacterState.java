package com.demo.mota.engine.state;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;

import java.math.BigInteger;
import java.util.Map;

public abstract class AbstractCharacterState {
    private final String characterId;
    private final String characterName;

    private Map<StateType, Object> stateMap;
    private Direction currentDirection;

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, Object> stateMap, Direction currentDirection) {
        this.characterId = characterId;
        this.characterName = characterName;
        this.stateMap = stateMap;
        this.currentDirection = currentDirection;
    }

    public BigInteger getEffectiveATK() {
        return (BigInteger) stateMap.get(StateType.ATK);
    }

    public BigInteger getEffectiveDEF() {
        return (BigInteger) stateMap.get(StateType.DEF);
    }

    public BigInteger getCharacterHealth() {
        return (BigInteger) stateMap.get(StateType.HP);
    }

    public BigInteger calculateDamagePerRound(AbstractCharacterState target) {
        BigInteger damage = this.getEffectiveATK().subtract(target.getEffectiveDEF());
        return damage.compareTo(BigInteger.ZERO) > 0 ? damage : BigInteger.ZERO;
    }

    public String getCharacterId() {
        return characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }
}
