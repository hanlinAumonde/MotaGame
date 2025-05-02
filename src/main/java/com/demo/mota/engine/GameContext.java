package com.demo.mota.engine;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.Map;

public class GameContext {
    private static volatile GameContext instance;

    private PlayerStateManager playerStateManager;

    private GameContext() {
        // Private constructor to prevent instantiation
        // Initialize the player state manager

        //TODO: should be initialized from an config file, here we just hardcode it first
        Map<StateType, Object> stateMap = Map.of(
                StateType.HP, 100,
                StateType.ATK, 10,
                StateType.DEF, 5
        );
        this.playerStateManager = new PlayerStateManager("player1", "Player 1", stateMap, Direction.DOWN);
    }

    public static GameContext getGameContext() {
        if (instance == null) {
            synchronized (GameContext.class) {
                if (instance == null) {
                    instance = new GameContext();
                }
            }
        }
        return instance;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }
}
