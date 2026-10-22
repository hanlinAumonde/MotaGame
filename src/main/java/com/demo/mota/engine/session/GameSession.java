package com.demo.mota.engine.session;

import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.event.GameEvent;
import com.demo.mota.engine.event.MoveHandler;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.save.SaveMeta;
import com.demo.mota.engine.snapshot.GameSnapshot;
import com.demo.mota.engine.snapshot.UndoHistory;
import com.demo.mota.engine.state.PlayerStateManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 一局游戏：玩家、地图、移动分发与撤销历史，全部在构造时装好、之后不再替换。
 *
 * <p>新开一局与按快照恢复（撤销 / 读档）都是<b>造一个新的会话</b>：
 * 造的过程中出了错（快照对不上当前的塔），抛出的异常不会波及原来那一局，调用方继续用旧的即可——
 * 不再需要「逐个字段装上、失败时再逐个退回」。
 *
 * <p>改游戏状态的入口是这里的命令方法（{@link #move}、{@link #turn}）；菜单界面改装备、预设之类
 * 仍直接调玩家对象的公开方法，回到对局时由调用方 {@link #recalculateDamage()} 统一重算伤害。
 */
public final class GameSession {

    /** 会产生撤销点的行动 */
    private static final Set<MoveResult.Type> UNDO_POINTS = EnumSet.of(
            MoveResult.Type.DOOR_OPENED, MoveResult.Type.BATTLE_WON,
            MoveResult.Type.BATTLE_LOST, MoveResult.Type.ITEM_PICKED);

    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TowerContent content;
    private final PlayerStateManager player;
    private final MapManager maps;
    private final MoveHandler moveHandler;
    private final UndoHistory undoHistory;

    private GameSession(TowerContent content, PlayerStateManager player, MapManager maps, UndoHistory undoHistory) {
        this.content = content;
        this.player = player;
        this.maps = maps;
        this.undoHistory = undoHistory;
        this.moveHandler = new MoveHandler(maps, player);
        recalculateDamage();
    }

    /** 全新的一局：开局状态的玩家落在开局楼层的出生点，撤销历史为空 */
    public static GameSession newGame(TowerContent content) {
        PlayerStateManager player = new PlayerStateManager(content.player(), content);
        MapManager maps = newMapManager(content);
        maps.loadFloor(content.startFloor());
        return new GameSession(content, player, maps, new UndoHistory(content.rules().undo().maxSteps()));
    }

    /**
     * 按快照重建一局（撤销 / 读档）：玩家先按开局状态造出来再施加快照；地图重新载入快照所在区域、施加各层变化。
     *
     * @param undoHistory 新会话沿用的撤销历史——撤销时传入原来那一份（已弹出被撤销的一项），读档时传入空的
     * @throws RuntimeException 快照与当前塔的数据对不上（区域已不存在等）
     */
    public static GameSession restore(TowerContent content, GameSnapshot snapshot, UndoHistory undoHistory) {
        PlayerStateManager player = new PlayerStateManager(content.player(), content);
        player.applySnapshot(snapshot.player());
        MapManager maps = newMapManager(content);
        maps.restore(snapshot.world());
        return new GameSession(content, player, maps, undoHistory);
    }

    /** 读档用：按本塔规则建一份空的撤销历史 */
    public static UndoHistory emptyUndoHistory(TowerContent content) {
        return new UndoHistory(content.rules().undo().maxSteps());
    }

    private static MapManager newMapManager(TowerContent content) {
        return new MapManager(content.regions(), content.floors(), content.mapFactory());
    }

    // ==================== 命令 ====================

    /**
     * 玩家朝某方向行动一步。开门 / 战斗（含致死）/ 拾取之前的状态记为一个撤销点；
     * 进入新区域时清空撤销历史（区域单向，已卸载的区域不再回来）。生命归零时在事件末尾追加 {@code PlayerDied}。
     */
    public MoveResult move(Direction direction) {
        GameSnapshot before = undoHistory.isEnabled() ? capture() : null;
        MoveResult result = moveHandler.handleMove(direction);
        if (result.type() == MoveResult.Type.REGION_ENTERED) {
            undoHistory.clear();
        } else if (before != null && UNDO_POINTS.contains(result.type())) {
            undoHistory.push(before, undoLabel(result));
        }
        if (player.isDead()) {
            result = result.withEvent(new GameEvent.PlayerDied(maps.getPlayerPosition()));
        }
        return result;
    }

    /** 原地顺时针转身 */
    public List<GameEvent> turn() {
        player.playerDirectionChange();
        return List.of(new GameEvent.PlayerTurned(player.getCurrentDirection()));
    }

    /**
     * 玩家在菜单里改了会影响战斗的东西（穿脱装备、切换就绪预设、改预设内容）之后调用，
     * 让当前层的伤害数字与之同步。
     */
    public void recalculateDamage() {
        moveHandler.getBattleHandler().recalculateAllDamage(player, maps.getCurrentMap());
    }

    // ==================== 快照 / 存档 ====================

    /** 把这一局拍成快照（撤销与存档共用） */
    public GameSnapshot capture() {
        return new GameSnapshot(player.toSnapshot(), maps.toSnapshot());
    }

    /** 存档档位列表上显示的摘要 */
    public SaveMeta describe(LocalDateTime now) {
        return new SaveMeta(now.format(SAVE_TIME_FORMAT),
                maps.getCurrentRegion().name(),
                maps.getCurrentMap().getFloorNumber(),
                player.getLevelNumber(),
                player.getCurrentHP().toString());
    }

    private static String undoLabel(MoveResult result) {
        return switch (result.type()) {
            case DOOR_OPENED -> "打开" + result.detail();
            case BATTLE_WON, BATTLE_LOST -> "与" + result.detail() + "的战斗";
            case ITEM_PICKED -> "拾取" + result.detail();
            default -> result.type().name();
        };
    }

    // ==================== 取数 ====================

    public TowerContent content() {
        return content;
    }

    public PlayerStateManager player() {
        return player;
    }

    public MapManager maps() {
        return maps;
    }

    public UndoHistory undoHistory() {
        return undoHistory;
    }

    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }
}
