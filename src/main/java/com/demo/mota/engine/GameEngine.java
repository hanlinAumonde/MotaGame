package com.demo.mota.engine;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.PlayerStateManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.demo.mota.engine.configs.GameContextConfigConstants.INITIAL_PLAYER_STATE_PATH;
import static com.demo.mota.engine.configs.GameContextConfigConstants.PLAYER_ID;

public class GameEngine {
    private static volatile GameEngine instance;

    private PlayerStateManager playerStateManager;

    private GameEngine() {
        this.playerStateManager = loadInitialPlayerState();
    }

    public static GameEngine getGameEngine() {
        if (instance == null) {
            synchronized (GameEngine.class) {
                if (instance == null) {
                    instance = new GameEngine();
                }
            }
        }
        return instance;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    private PlayerStateManager loadInitialPlayerState() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(INITIAL_PLAYER_STATE_PATH);
        if(inputStream == null) {
            throw new RuntimeException("Initial player state file not found: " + INITIAL_PLAYER_STATE_PATH);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String,Object> playerData = mapper.readValue(inputStream, new TypeReference<Map<String,Object>>(){});
            return new PlayerStateManager(
                    (String) playerData.get(PLAYER_ID),
                    (String) playerData.get("playerName"),
                    Map.of(
                            StateType.HP, playerData.get("health"),
                            StateType.ATK, playerData.get("attack"),
                            StateType.DEF, playerData.get("defense")
                    ),
                    Direction.DOWN
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse initial player state", e);
        }
    }

    //private record PlayerInitialData(String playerId, String playerName, long health, long attack, long defense) implements Serializable { }
}
