package com.demo.mota.engine.state;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCharacterState {
    private final String characterId;
    private final String characterName;

    private final Map<StateType, Object> stateMap;
    private Direction currentDirection;

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, Object> stateMap, Direction currentDirection) {
        this.characterId = characterId;
        this.characterName = characterName;
        this.stateMap = new HashMap<>(stateMap);
        this.currentDirection = currentDirection;
    }

    public Object getStateValue(StateType stateType){
        try{
            return switch(stateType){
                case HP, ATK, DEF -> (BigInteger) this.stateMap.get(stateType);
            };
        }catch(ClassCastException e){
            throw new IllegalStateException("Invalid state value for state type " + stateType);
        }
    }

    public BigInteger calculateDamagePerRound(AbstractCharacterState target) {
        BigInteger damage = ((BigInteger) this.getStateValue(StateType.ATK)).subtract((BigInteger) target.getStateValue(StateType.DEF));
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

    public void setCurrentDirection(Direction direction) {
        this.currentDirection = direction;
    }

    public void updateState(StateType stateType, Object value) {
        this.stateMap.put(stateType, value);
    }
}
