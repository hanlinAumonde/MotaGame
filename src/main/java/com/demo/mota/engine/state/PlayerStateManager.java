package com.demo.mota.engine.state;

import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.Item.GenericItem.GenericItem;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.Item.ItemFactory;
import com.demo.mota.engine.Item.Key;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.EquipSlot;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.ItemConfigConstants.INITIAL_KEY_SET;

public class PlayerStateManager extends AbstractCharacterState {
    private final LevelManager levelManager;

    private long currentGoldAmount;

    private List<Equipment> equipmentsOwned;
    private Map<EquipSlot, Equipment> equipmentsEquipped;

    // Keys
    private Key yellow_Key;
    private Key red_Key;
    private Key blue_Key;
    private List<Key> ancientKeys;

    private List<GenericItem> genericItemsOwned;

    PlayerStateManager(String characterId, String characterName, Map<StateType, Object> stateMap, Direction currentDirection){
        super(characterId, characterName, stateMap, currentDirection);
        this.levelManager = new LevelManager();
        this.currentGoldAmount = 0;
        this.equipmentsOwned = List.of();
        this.equipmentsEquipped = Map.of();
        setCurrentKeySet(INITIAL_KEY_SET);
        this.genericItemsOwned = List.of();
    }

    public void setCurrentKeySet(String currentKeySet) {
        addAncientKey();
        this.yellow_Key = (Key) ItemFactory.createByItemID(currentKeySet + KeyColor.YELLOW.getValue());
        this.red_Key = (Key) ItemFactory.createByItemID(currentKeySet + KeyColor.RED.getValue());
        this.blue_Key = (Key) ItemFactory.createByItemID(currentKeySet + KeyColor.BLUE.getValue());
    }

    private void addAncientKey() {
        if (this.ancientKeys == null) {
            this.ancientKeys = List.of();
        }
        if(this.yellow_Key != null) this.ancientKeys.add(this.yellow_Key);
        if(this.red_Key != null) this.ancientKeys.add(this.red_Key);
        if(this.blue_Key != null) this.ancientKeys.add(this.blue_Key);
    }

    public BigInteger getEffectiveATK(){
        return equipmentsEquipped
                .values().stream()
                .filter(equipment -> equipment.getStateEffectMap().containsKey(StateType.ATK))
                .map(equipment -> (BigInteger) equipment.getStateEffectMap().get(StateType.ATK))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public BigInteger getEffectiveDEF(){
        return equipmentsEquipped
                .values().stream()
                .filter(equipment -> equipment.getStateEffectMap().containsKey(StateType.DEF))
                .map(equipment -> (BigInteger) equipment.getStateEffectMap().get(StateType.DEF))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public void updateLevel(BigInteger expGained){
        this.levelManager.cumulateExperience(expGained);
    }

    public void updateGoldAmount(long goldAmount){
        this.currentGoldAmount += goldAmount;
    }

    public void gainItem(Item item){
        if(item instanceof GenericItem genericItem){
            this.genericItemsOwned.add(genericItem);
        } else if(item instanceof Equipment equipment){
            this.equipmentsOwned.add(equipment);
        } else if(item instanceof Key key){
            switch (key.getKeyColor()){
                case YELLOW -> this.yellow_Key.updateItemCount(1);
                case RED -> this.red_Key.updateItemCount(1);
                case BLUE -> this.blue_Key.updateItemCount(1);
                default -> throw new IllegalArgumentException("Invalid key color: " + key.getKeyColor());
            }
        }
    }
}
