package com.demo.mota.engine.factory.item;

import com.demo.mota.engine.Item.*;
import com.demo.mota.engine.Item.GenericItem.FloorJumper;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.factory.AbstractFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.demo.mota.engine.configs.ItemConfigConstants.*;

public class ItemFactory extends AbstractFactory<Item, ItemFactory.ItemData, ItemCreator> {
    private static class Holder {
        private static final ItemFactory INSTANCE = new ItemFactory();
    }

    public static ItemFactory getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected String getConfigFileName() {
        return ITEM_LIST_FILE;
    }

    @Override
    protected void parseData(ObjectMapper mapper, InputStream inputStream) throws IOException {
        JsonNode rootNode = mapper.readTree(inputStream);
        for(JsonNode itemNode: rootNode){
            mapper.treeToValue(itemNode, new TypeReference<Map<String, ItemData>>(){})
                    .values().forEach(itemData -> {
                        dataRegistry.put(itemData.itemId(), itemData);
                    });
        }
    }

    @Override
    protected ItemCreator generateCreator(String id) {
        ItemData itemData = dataRegistry.get(id);
        return switch(itemData.itemType){
            case EQUIPMENT ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                     isStorable, isConsumable, parameters) ->
                    {
                        Map<StateType, Object> stateEffectMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                            stateEffectMap.put(StateType.fromString(entry.getKey()),
                                    entry.getValue());
                        }
                        return new Equipment(itemId, itemName, itemDescription,
                                itemPrice, itemCount,
                                isStorable, isConsumable,
                                stateEffectMap);
                    };
            case KEY ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                     isStorable, isConsumable, parameters) ->
                            new Key(itemId, itemName, itemDescription,
                                    itemPrice, itemCount,
                                    isStorable, isConsumable,
                                    KeyColor.fromString((String) parameters.get(KEY_COLOR)));
            case PORTION ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                     isStorable, isConsumable, parameters) ->
                            new Portion(itemId, itemName, itemDescription,
                                    itemPrice, itemCount,
                                    isStorable, isConsumable,
                                    BigInteger.valueOf((long) parameters.get(HEALING_AMOUNT)));
            case ABILITY_GEM ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                     isStorable, isConsumable, parameters) ->
                            new AbilityGem(itemId, itemName, itemDescription,
                                    itemPrice, itemCount,
                                    isStorable, isConsumable,
                                    StateType.fromString((String) parameters.get(ABILITY_TYPE)),
                                    parameters.get(ABILITY_VALUE));
            case GENERIC_ITEM ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                     isStorable, isConsumable, parameters) ->
                    {
                        String className = (String) parameters.get(GENERIC_ITEM_CLASS_NAME);
                        String completClassPath = GENERIC_ITEM_CLASS_PATH + '.' + className;
                        try{
                            return switch (className.toUpperCase()){
                                case FLOOR_JUMPER ->
                                        new FloorJumper(itemId, itemName, itemDescription,
                                                itemPrice, itemCount,
                                                isStorable, isConsumable,
                                                (short) parameters.get(FLOOR_NUMBER_SELECTED));
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + className.toUpperCase());
                            };
                        }catch (Exception e){
                            throw new RuntimeException("Failed to create generic item", e);
                        }
                    };
            default -> throw new IllegalStateException("Unexpected item-type: " + itemData.itemType);
        };
    }

    @Override
    protected Item createProduct(ItemCreator creator, ItemData data) {
        return creator.createItem(
                data.itemId,
                data.itemName,
                data.itemDescription,
                data.itemPrice,
                1,
                data.isStorable,
                data.isConsumable,
                data.parameters
        );
    }

    public record ItemData(String itemId, String itemType, String itemName, String itemDescription,
                         long itemPrice, boolean isStorable, boolean isConsumable,
                         Map<String, Object> parameters) implements Serializable {}
}
