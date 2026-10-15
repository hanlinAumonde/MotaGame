package com.demo.mota.engine;

import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.event.MoveHandler;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.snapshot.GameSnapshot;
import com.demo.mota.engine.snapshot.UndoHistory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.demo.mota.engine.configs.GameContextConfigConstants.*;

/**
 * 引擎总控（单例）。
 *
 * <p>单例本身的生命周期与进程一致，但<b>一局游戏</b>的生命周期不是——
 * 从标题界面反复开局要求玩家状态、地图与移动分发都能整套重建，
 * 因此这三者不再是 final，统一由 {@link #startNewGame(int)} 装配。
 * 各界面的状态属于 UI 层，由对应 Screen 在「新开一局」事件（{@code GameFlow.startNewGame}）里自行重建，不归引擎管。
 * 无状态的 {@code ResourceManager} 与各工厂不参与重建。
 */
public class GameEngine {
    private static volatile GameEngine instance;

    /** 最外层的阶段状态机，贯穿整个进程，不随开局重建 */
    private final GameFlow gameFlow = new GameFlow();

    private PlayerStateManager playerStateManager;
    private MapManager mapManager;
    private MoveHandler moveHandler;
    /** 撤销历史：新开一局时按规则重建（同时也就清空了）；撤销本身不动它 */
    private UndoHistory undoHistory = new UndoHistory(0);

    /** 会产生撤销点的行动 */
    private static final Set<MoveResult.Type> UNDO_POINTS = EnumSet.of(
            MoveResult.Type.DOOR_OPENED, MoveResult.Type.BATTLE_WON,
            MoveResult.Type.BATTLE_LOST, MoveResult.Type.ITEM_PICKED);

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

    /**
     * 玩家朝某方向行动一步。开门 / 战斗（含致死）/ 拾取之前的状态记为一个撤销点；
     * 进入新区域时清空撤销历史（区域单向，已卸载的区域不再回来）。
     */
    public MoveResult handlePlayerMove(Direction direction) {
        GameSnapshot before = undoHistory.isEnabled() ? captureSnapshot() : null;
        MoveResult result = moveHandler.handleMove(direction);
        if (result.getType() == MoveResult.Type.REGION_ENTERED) {
            undoHistory.clear();
        } else if (before != null && UNDO_POINTS.contains(result.getType())) {
            undoHistory.push(before, undoLabel(result));
        }
        return result;
    }

    private static String undoLabel(MoveResult result) {
        return switch (result.getType()) {
            case DOOR_OPENED -> "打开" + result.getDetail();
            case BATTLE_WON, BATTLE_LOST -> "与" + result.getDetail() + "的战斗";
            case ITEM_PICKED -> "拾取" + result.getDetail();
            default -> result.getType().name();
        };
    }

    /** 是否还有可撤销的行动 */
    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public UndoHistory getUndoHistory() {
        return undoHistory;
    }

    /**
     * 撤销最近一次有意义的行动：恢复到它之前的快照（经 {@link #restoreSnapshot}，阶段回到 {@code PLAYING}）。
     *
     * @return 被撤销的行动描述，没有可撤销的行动时为 null
     */
    public String undoLastAction() {
        UndoHistory.Entry entry = undoHistory.pop();
        if (entry == null) {
            return null;
        }
        restoreSnapshot(entry.snapshot());
        return entry.label();
    }

    public void handleDirectionChange() { this.playerStateManager.playerDirectionChange(); }

    /**
     * 玩家在菜单里改了会影响战斗的东西（穿脱装备、切换就绪预设、改预设内容）之后调用，
     * 让当前层的伤害数字与之同步。
     */
    public void recalculateCurrentFloorDamage() {
        moveHandler.getBattleHandler().recalculateAllDamage(playerStateManager, mapManager.getCurrentMap());
    }

    public GameFlow getGameFlow() {
        return gameFlow;
    }

    /** 是否已有一局在进行（标题界面下为 false） */
    public boolean hasActiveGame() { return playerStateManager != null; }

    /**
     * 开始全新的一局：整套重建玩家状态 / 地图 / 移动分发，随后载入初始楼层并预计算伤害，
     * 最后经 {@code GameFlow.startNewGame} 广播「新开一局」并把阶段推进到 {@code PLAYING}。
     * 地图缓存随 {@code MapManager} 一起丢弃，上一局开过的门不会带到新局。
     */
    public void startNewGame(int initialFloor) {
        installSession(loadInitialPlayerState(), maps -> maps.loadFloor(initialFloor));
        this.undoHistory = new UndoHistory(GameRules.get().undo().maxSteps());
        gameFlow.startNewGame();
    }

    /** 把当前这局拍成快照（撤销与存档共用） */
    public GameSnapshot captureSnapshot() {
        return new GameSnapshot(playerStateManager.toSnapshot(), mapManager.toSnapshot());
    }

    /**
     * 按快照整套重建一局（撤销 / 读档），随后经 {@code GameFlow.resumeSession} 让各界面重建本局状态并回到 {@code PLAYING}。
     * 玩家先按初始状态构造、再施加快照；地图重新解析快照所在区域后施加各层变化。
     *
     * <p>重建中途失败（快照引用的区域已不存在等）时恢复到调用前的这一局并把异常抛给调用方。
     */
    public void restoreSnapshot(GameSnapshot snapshot) {
        PlayerStateManager player = loadInitialPlayerState();
        player.applySnapshot(snapshot.player());
        installSession(player, maps -> maps.restore(snapshot.world()));
        gameFlow.resumeSession();
    }

    /**
     * 装配一局：玩家 → 地图 → 移动分发 → 载入地图 → 预计算伤害。
     *
     * <p>装配顺序不能调：{@code MonsterFactory} 在创建怪物时会回头取
     * {@code GameEngine.getPlayerStateManager()} 做首次战斗预计算，
     * 因此玩家状态必须先于载入地图就位。中途抛异常时退回原来的三者。
     */
    private void installSession(PlayerStateManager player, Consumer<MapManager> mapLoader) {
        PlayerStateManager previousPlayer = this.playerStateManager;
        MapManager previousMaps = this.mapManager;
        MoveHandler previousMoveHandler = this.moveHandler;
        try {
            this.playerStateManager = player;
            this.mapManager = new MapManager();
            this.moveHandler = new MoveHandler(mapManager, playerStateManager);
            mapLoader.accept(mapManager);
            moveHandler.getBattleHandler().recalculateAllDamage(playerStateManager, mapManager.getCurrentMap());
        } catch (RuntimeException e) {
            this.playerStateManager = previousPlayer;
            this.mapManager = previousMaps;
            this.moveHandler = previousMoveHandler;
            throw e;
        }
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
