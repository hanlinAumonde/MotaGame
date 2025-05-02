package com.demo.mota.engine.factory.monster;

import com.demo.mota.engine.GameContext;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.monster.Monster;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.demo.mota.engine.configs.MonsterConfigConstants.MONSTER_LIST_FILE;

public class MonsterObjectFactory {
    private static final Function<String,MonsterCreator> monsterCreator;
    private static final Map<String, MonsterData> monsterDataMap = new HashMap<>();

    private record MonsterData(String monsterId, String monsterName,
            BigInteger monsterHealth, BigInteger monsterAttack, BigInteger monsterDefense,
            long monsterGoldReward,
            BigInteger monsterExperienceReward) implements Serializable {
    }

    static {
        loadData();
        monsterCreator = (monsterId) ->
                generateCreator(monsterDataMap.get(monsterId));
    }

    private static void loadData(){
        InputStream inputStream = MonsterObjectFactory.class.getClassLoader().getResourceAsStream(MONSTER_LIST_FILE);
        if(inputStream == null){
            throw new RuntimeException("Monster list file not found: " + MONSTER_LIST_FILE);
        }
        ObjectMapper mapper = new ObjectMapper();
        try{
            mapper.readValue(inputStream, new TypeReference<List<MonsterData>>(){})
                    .forEach(monsterData -> {
                        monsterDataMap.put(monsterData.monsterId, monsterData);
                    });
        }catch (IOException | IllegalArgumentException e){
            throw new RuntimeException("Failed to load monster data", e);
        }
    }

    private static MonsterCreator generateCreator(MonsterData monsterData) {
        return (monsterId, monsterName, monsterHealth, monsterAttack,
                monsterDefense, monsterGoldReward, monsterExperienceReward) -> {
            Map<StateType, Object> stateMap = Map.of(
                    StateType.HP, monsterHealth,
                    StateType.ATK, monsterAttack,
                    StateType.DEF, monsterDefense
            );
            return new Monster(monsterId, monsterName, stateMap,
                    Direction.DOWN, GameContext.getGameContext().getPlayerStateManager(),
                    monsterGoldReward, monsterExperienceReward);
        };
    }

    public static Monster createById(String Id) {
        MonsterData monsterData = monsterDataMap.get(Id);
        return MonsterObjectFactory.monsterCreator.apply(Id).createMonster(
                monsterData.monsterId, monsterData.monsterName,
                monsterData.monsterHealth, monsterData.monsterAttack,
                monsterData.monsterDefense,
                monsterData.monsterGoldReward, monsterData.monsterExperienceReward
        );
    }
}
