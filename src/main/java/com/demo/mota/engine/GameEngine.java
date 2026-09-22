package com.demo.mota.engine;

import com.demo.mota.engine.app.GameFlow;
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

import static com.demo.mota.engine.configs.GameContextConfigConstants.*;

/**
 * 引擎总控（单例）。
 *
 * <p>单例本身的生命周期与进程一致，但<b>一局游戏</b>的生命周期不是——
 * 从标题界面反复开局要求玩家状态、地图、移动分发与菜单都能整套重建，
 * 因此这四者不再是 final，统一由 {@link #startNewGame(int)} 装配。
 * 无状态的 {@code ResourceManager} 与各工厂不参与重建。
 */
public class GameEngine {
    private static volatile GameEngine instance;

    /** 最外层的阶段状态机，贯穿整个进程，不随开局重建 */
    private final GameFlow gameFlow = new GameFlow();

    private PlayerStateManager playerStateManager;
    private MapManager mapManager;
    private MoveHandler moveHandler;
    private GameMenu gameMenu;

    private GameEngine() {
        // 只保证 provider 链就位；精灵图与各配置表由 GameBootstrap 在加载阶段统一加载
        ResourceManager.getInstance();
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

    public void handleDirectionChange() { this.playerStateManager.playerDirectionChange(); }

    public GameFlow getGameFlow() {
        return gameFlow;
    }

    /** 是否已有一局在进行（标题界面下为 false） */
    public boolean hasActiveGame() { return playerStateManager != null; }

    /**
     * 开始全新的一局：整套重建玩家状态 / 地图 / 移动分发 / 菜单，随后载入初始楼层并预计算伤害，
     * 最后把阶段推进到 {@code PLAYING}。
     *
     * <p>装配顺序不能调：{@code MonsterFactory} 在创建怪物时会回头取
     * {@code GameEngine.getPlayerStateManager()} 做首次战斗预计算，
     * 因此玩家状态必须先于 {@code loadFloor} 就位。
     * 地图缓存随 {@code MapManager} 一起丢弃，上一局开过的门不会带到新局。
     */
    public void startNewGame(int initialFloor) {
        this.playerStateManager = loadInitialPlayerState();
        this.mapManager = new MapManager();
        this.moveHandler = new MoveHandler(mapManager, playerStateManager);
        this.gameMenu = new GameMenu(mapManager);

        mapManager.loadFloor(initialFloor);
        moveHandler.getBattleHandler().recalculateAllDamage(playerStateManager, mapManager.getCurrentMap());

        gameFlow.toPlaying();
    }

    private PlayerStateManager loadInitialPlayerState() {
        Map<String, Object> playerData = ResourceManager.getInstance()
                .loadJsonResource(INITIAL_PLAYER_STATE_PATH, new TypeReference<>() {});
        int health = (int) playerData.get(StateType.HP.getValue());
        // 未配置 maxHealth 时，以初始生命值作为初始上限
        Object maxHealth = playerData.getOrDefault(StateType.MAX_HP.getValue(), health);
        String dir = (String) playerData.get(PLAYER_INIT_POSITION);
        Direction initialDirection = dir != null ? Direction.fromString(dir) : Direction.DOWN;
        return new PlayerStateManager(
                (String) playerData.get(PLAYER_ID),
                (String) playerData.get(PLAYER_NAME),
                Map.of(
                        StateType.HP, GameNumber.of(health),
                        StateType.MAX_HP, GameNumber.of((int) maxHealth),
                        StateType.ATK, GameNumber.of((int) playerData.get(StateType.ATK.getValue())),
                        StateType.DEF, GameNumber.of((int) playerData.get(StateType.DEF.getValue()))
                ),
                initialDirection
        );
    }
}
