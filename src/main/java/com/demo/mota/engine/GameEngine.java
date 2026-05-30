package com.demo.mota.engine;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.event.MoveHandler;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.GameNumber;
import com.demo.mota.engine.state.PlayerStateManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

import static com.demo.mota.engine.configs.GameContextConfigConstants.INITIAL_PLAYER_STATE_PATH;
import static com.demo.mota.engine.configs.GameContextConfigConstants.PLAYER_ID;

public class GameEngine {
    private static volatile GameEngine instance;

    private final PlayerStateManager playerStateManager;
    private final MapManager mapManager;
    private final MoveHandler moveHandler;

    private GameEngine() {
        ResourceManager.getInstance();
        this.playerStateManager = loadInitialPlayerState();
        this.mapManager = new MapManager();
        this.moveHandler = new MoveHandler(mapManager, playerStateManager);
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

    public MapManager getMapManager() {
        return mapManager;
    }

    public MoveHandler getMoveHandler() {
        return moveHandler;
    }

    public MoveResult handlePlayerMove(Direction direction) {
        return moveHandler.handleMove(direction);
    }

    public void startGame(int initialFloor) {
        mapManager.loadFloor(initialFloor);
        moveHandler.getBattleHandler().recalculateAllDamage(playerStateManager, mapManager.getCurrentMap());
    }

    private PlayerStateManager loadInitialPlayerState() {
        Map<String, Object> playerData = ResourceManager.getInstance()
                .loadJsonResource(INITIAL_PLAYER_STATE_PATH, new TypeReference<>() {});
        return new PlayerStateManager(
                (String) playerData.get(PLAYER_ID),
                (String) playerData.get("playerName"),
                Map.of(
                        StateType.HP, GameNumber.of((int) playerData.get("health")),
                        StateType.ATK, GameNumber.of((int) playerData.get("attack")),
                        StateType.DEF, GameNumber.of((int) playerData.get("defense"))
                ),
                Direction.DOWN
        );
    }
}
