package com.demo.mota.engine.state;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCharacterState {
    private final String characterId;
    private final String characterName;

    private final Map<StateType, GameNumber> stateMap;
    private Direction currentDirection;

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, GameNumber> stateMap, Direction currentDirection) {
        this.characterId = characterId;
        this.characterName = characterName;
        this.stateMap = new HashMap<>(stateMap);
        this.currentDirection = currentDirection;
    }

    public GameNumber getStateValue(StateType stateType) {
        return this.stateMap.get(stateType);
    }

    public GameNumber calculateDamagePerRound(AbstractCharacterState target) {
        GameNumber damage = getStateValue(StateType.ATK).minus(target.getStateValue(StateType.DEF));
        return damage.clampMin(GameNumber.ZERO);
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

    public void updateState(StateType stateType, GameNumber value) {
        this.stateMap.put(stateType, value);
    }
}
