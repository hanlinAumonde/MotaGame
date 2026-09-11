package com.demo.mota.engine;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.event.MoveHandler;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.menu.GameMenu;
import com.demo.mota.engine.resource.ResourceManager;
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
    private final GameMenu gameMenu;

    private GameEngine() {
        ResourceManager.getInstance();
        this.playerStateManager = loadInitialPlayerState();
        this.mapManager = new MapManager();
        this.moveHandler = new MoveHandler(mapManager, playerStateManager);
        this.gameMenu = new GameMenu(mapManager);
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

    public GameMenu getGameMenu() {
        return gameMenu;
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
        int health = (int) playerData.get(StateType.HP.getValue());
        // 未配置 maxHealth 时，以初始生命值作为初始上限
        Object maxHealth = playerData.getOrDefault(StateType.MAX_HP.getValue(), health);
        return new PlayerStateManager(
                (String) playerData.get(PLAYER_ID),
                (String) playerData.get("playerName"),
                Map.of(
                        StateType.HP, GameNumber.of(health),
                        StateType.MAX_HP, GameNumber.of((int) maxHealth),
                        StateType.ATK, GameNumber.of((int) playerData.get(StateType.ATK.getValue())),
                        StateType.DEF, GameNumber.of((int) playerData.get(StateType.DEF.getValue()))
                ),
                Direction.DOWN
        );
    }
}
