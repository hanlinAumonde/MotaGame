package com.demo.mota.engine.factory.monster;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.factory.AbstractFactory;
import com.demo.mota.engine.state.monster.Monster;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.MonsterConfigConstants.MONSTER_LIST_FILE;

public class MonsterFactory extends AbstractFactory<Monster, MonsterFactory.MonsterData, MonsterCreator> {
    private static class Holder {
        private static final MonsterFactory INSTANCE = new MonsterFactory();
    }

    public static MonsterFactory getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected String getConfigFileName() {
        return MONSTER_LIST_FILE;
    }

    @Override
    protected void parseData(ObjectMapper mapper, InputStream inputStream) throws IOException {
        mapper.readValue(inputStream, new TypeReference<List<MonsterData>>(){})
                .forEach(monsterData -> {
                    dataRegistry.put(monsterData.monsterId, monsterData);
                });
    }

    @Override
    protected MonsterCreator generateCreator(String id) {
        MonsterData monsterData = dataRegistry.get(id);
        return (monsterId, monsterName, monsterHealth, monsterAttack,
                monsterDefense, monsterGoldReward, monsterExperienceReward) -> {
            Map<StateType, Object> stateMap = Map.of(
                    StateType.HP, monsterHealth,
                    StateType.ATK, monsterAttack,
                    StateType.DEF, monsterDefense
            );
            return new Monster(monsterId, monsterName, stateMap,
                    Direction.DOWN, GameEngine.getGameEngine().getPlayerStateManager(),
                    monsterGoldReward, monsterExperienceReward);
        };
    }

    @Override
    protected Monster createProduct(MonsterCreator creator, MonsterData data) {
        return creator.createMonster(
                data.monsterId, data.monsterName,
                data.monsterHealth, data.monsterAttack,
                data.monsterDefense,
                data.monsterGoldReward, data.monsterExperienceReward
        );
    }

    public record MonsterData(String monsterId, String monsterName,
                              BigInteger monsterHealth, BigInteger monsterAttack, BigInteger monsterDefense,
                              long monsterGoldReward,
                              BigInteger monsterExperienceReward) implements Serializable {
    }
}
