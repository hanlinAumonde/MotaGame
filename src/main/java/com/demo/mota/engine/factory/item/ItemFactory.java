package com.demo.mota.engine.factory.item;

import com.demo.mota.engine.Item.*;
import com.demo.mota.engine.Item.GenericItem.FloorJumper;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.demo.mota.engine.configs.ItemConfigConstants.*;

public class ItemFactory {
    private static final Function<String, ItemCreator> itemCreator;
    private static final Map<String, ItemData> itemDataRegistry = new HashMap<>();

    static {
        loadData();
        itemCreator = (itemId) -> generateCreator(itemDataRegistry.get(itemId));
    }

    private static void loadData() {
        InputStream inputStream = ItemFactory.class.getClassLoader().getResourceAsStream(ITEM_LIST_FILE);
        if (inputStream == null) {
            throw new RuntimeException("Item list file not found: " + ITEM_LIST_FILE);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(inputStream);
            for(JsonNode itemNode: rootNode){
                mapper.treeToValue(itemNode, new TypeReference<Map<String, ItemData>>(){})
                        .values().forEach(itemData -> {
                            itemDataRegistry.put(itemData.itemId(), itemData);
                        });
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to load item data", e);
        }
    }

    //private static Item instantiateGenericItem(){}
    private static ItemCreator generateCreator(ItemData itemData){
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
                    {
                        return new Key(itemId, itemName, itemDescription,
                                itemPrice, itemCount,
                                isStorable, isConsumable,
                                KeyColor.fromString((String) parameters.get(KEY_COLOR)));
                    };
            case PORTION ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                    isStorable, isConsumable, parameters) ->
                    {
                        return new Portion(itemId, itemName, itemDescription,
                                itemPrice, itemCount,
                                isStorable, isConsumable,
                                BigInteger.valueOf((long) parameters.get(HEALING_AMOUNT)));
                    };
            case ABILITY_GEM ->
                    (itemId, itemName, itemDescription, itemPrice, itemCount,
                    isStorable, isConsumable, parameters) ->
                    {
                        return new AbilityGem(itemId, itemName, itemDescription,
                                itemPrice, itemCount,
                                isStorable, isConsumable,
                                StateType.fromString((String) parameters.get(ABILITY_TYPE)),
                                parameters.get(ABILITY_VALUE));
                    };
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
                                //TODO: Add other generic items
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

    private record ItemData(String itemId, String itemType, String itemName, String itemDescription,
                         long itemPrice, boolean isStorable, boolean isConsumable,
                         Map<String, Object> parameters) implements Serializable {}

    public static Item createByID(String Id){
        ItemData itemData = itemDataRegistry.get(Id);
        return ItemFactory.itemCreator.apply(Id).createItem(
                itemData.itemId,
                itemData.itemName,
                itemData.itemDescription,
                itemData.itemPrice,
                1,
                itemData.isStorable,
                itemData.isConsumable,
                itemData.parameters
        );
    }
}
