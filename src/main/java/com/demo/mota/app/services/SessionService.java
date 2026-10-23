package com.demo.mota.app.services;

import com.almasb.fxgl.core.EngineService;
import com.almasb.fxgl.logging.Logger;
import com.demo.mota.app.event.EngineEventBridge;
import com.demo.mota.app.event.MotaEvent;
import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.event.GameEvent;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.save.SaveException;
import com.demo.mota.engine.save.SaveFile;
import com.demo.mota.engine.save.SaveRepository;
import com.demo.mota.engine.save.SaveSlotInfo;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.engine.snapshot.UndoHistory;
import javafx.event.EventType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 对局会话服务（FXGL {@link EngineService}，取代原来的 {@code GameEngine} 单例）：持有当前这一局 {@link GameSession}，
 * 并把界面的命令转给它。
 *
 * <p><b>换一局统一走 FXGL 的 {@code startNewGame()}</b>：新开、读档都先在原界面里把这一局造好（会失败的步骤都在这里，
 * 失败就留在原界面提示），{@link #request} 交给本服务，再调 {@code getGameController().startNewGame()}——
 * FXGL 清空世界 / 计时器 / UI 节点后，在加载场景的 {@code initGame} 里 {@link #installPending()} 装上它。
 * 这样 {@code initGame} 只做不会失败的安装（它在后台线程执行，一抛异常 FXGL 就弹致命错误并退出）。
 *
 * <p>撤销是局内操作：按快照造一个新会话直接换上，不经加载场景；地图显示发现换了一张地图会整张重建。
 *
 * <p>命令产生的领域事件经 {@link EngineEventBridge} 发到 FXGL 的事件总线（{@link MotaEvent}），地图显示等据此播放表现；
 * 订阅经 {@link #subscribe}，换局时统一注销。
 */
public final class SessionService extends EngineService {

    private static final Logger log = Logger.get(SessionService.class);

    /** 由 FXGL 按字段类型注入 */
    private ContentService contentService;

    /** 下一次 startNewGame() 要装上的一局，以及进入对局后显示在状态栏的提示 */
    private GameSession pending;
    private String pendingMessage = "";

    private GameSession session;
    private String startMessage = "";

    private final EngineEventBridge events = new EngineEventBridge();

    // ==================== 换一局 ====================

    /** 全新的一局（标题「开始游戏」） */
    public GameSession prepareNewGame() {
        return GameSession.newGame(content());
    }

    /**
     * 读档：读出快照并按它造好一局（撤销历史为空），但不装上。
     *
     * @throws SaveException 档位为空 / 损坏 / 版本不兼容，或快照与当前塔的数据对不上（区域已不存在等）
     */
    public GameSession prepareLoad(String slotId) throws SaveException {
        SaveFile save = saves().read(slotId);
        try {
            return GameSession.restore(content(), save.snapshot(), GameSession.emptyUndoHistory(content()));
        } catch (RuntimeException e) {
            throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档与当前塔的数据对不上：" + e.getMessage(), e);
        }
    }

    /** 交代下一次 {@code startNewGame()} 要装上的一局，{@code message} 进入对局后显示在状态栏 */
    public void request(GameSession prepared, String message) {
        this.pending = Objects.requireNonNull(prepared);
        this.pendingMessage = message == null ? "" : message;
    }

    /**
     * 由 {@code initGame} 调用（加载场景的后台线程），只做不会失败的安装。
     * 没有准备好的一局时（开发模式的 Ctrl+R 重开也会调 startNewGame）按开局状态新开一局。
     */
    public GameSession installPending() {
        session = pending != null ? pending : prepareNewGame();
        startMessage = pending != null ? pendingMessage : "";
        pending = null;
        pendingMessage = "";
        return session;
    }

    /** 取走本局开始时要显示的提示（如「已读取存档 1」） */
    public String takeStartMessage() {
        String message = startMessage;
        startMessage = "";
        return message;
    }

    /** startNewGame() 清场时调用（源码：Engine.resetServices）：丢弃上一局，注销它的事件订阅 */
    @Override
    public void onGameReset() {
        session = null;
        events.clear();
    }

    /** 订阅这一局的某类领域事件（同步回调，在 FX 线程上）；换局时自动注销 */
    public void subscribe(EventType<MotaEvent> type, Consumer<GameEvent> handler) {
        events.subscribe(type, handler);
    }

    // ==================== 当前这一局 ====================

    /** 当前这一局；标题界面下（还没开过局）为 null */
    public GameSession session() {
        return session;
    }

    public TowerContent content() {
        return contentService.content();
    }

    public SaveRepository saves() {
        return contentService.saves();
    }

    /** 走一步；结果里的领域事件先广播出去，再返回给调用方 */
    public MoveResult move(Direction direction) {
        MoveResult result = session.move(direction);
        events.publish(result.events());
        return result;
    }

    public void turn() {
        events.publish(session.turn());
    }

    /**
     * 玩家在菜单里改了会影响战斗的东西（穿脱装备、切换就绪预设、改预设内容）之后调用，
     * 让当前层的伤害数字与之同步。
     */
    public void recalculateDamage() {
        if (session != null) {
            session.recalculateDamage();
        }
    }

    public boolean canUndo() {
        return session != null && session.canUndo();
    }

    /**
     * 撤销最近一次有意义的行动：按它之前的快照重建一局（沿用同一份撤销历史）并换上。
     *
     * @return 被撤销的行动描述，没有可撤销的行动时为 null
     */
    public String undoLastAction() {
        UndoHistory history = session.undoHistory();
        UndoHistory.Entry entry = history.pop();
        if (entry == null) {
            return null;
        }
        session = GameSession.restore(content(), entry.snapshot(), history);
        return entry.label();
    }

    // ==================== 存档 ====================

    /** 把当前这局写入指定档位（覆盖原有） */
    public void saveGame(String slotId) throws IOException {
        saves().write(slotId, session.describe(LocalDateTime.now()), session.capture());
    }

    /** 写自动存档（切换楼层 / 进入新区域之后）；失败只记录原因并返回 false，不打断游戏 */
    public boolean autoSave() {
        try {
            saveGame(SaveRepository.AUTO_SLOT);
            return true;
        } catch (IOException | RuntimeException e) {
            log.warning("自动存档失败：" + e.getMessage(), e);
            return false;
        }
    }
}
