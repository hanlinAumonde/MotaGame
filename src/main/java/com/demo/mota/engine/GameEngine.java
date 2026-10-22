package com.demo.mota.engine;

import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.save.SaveException;
import com.demo.mota.engine.save.SaveFile;
import com.demo.mota.engine.save.SaveMigrations;
import com.demo.mota.engine.save.SaveRepository;
import com.demo.mota.engine.save.SaveSlotInfo;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.engine.snapshot.GameSnapshot;
import com.demo.mota.engine.snapshot.UndoHistory;
import com.demo.mota.engine.state.PlayerStateManager;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 引擎总控（单例，过渡用）：持有本塔的内容（{@link TowerContent}）、存档库与当前这一局（{@link GameSession}），
 * 并把界面的调用转给当前会话。
 *
 * <p>一局的状态全在 {@code GameSession} 里，这里只负责「换哪一局」：新开、撤销、读档都是造一个新会话再换上去，
 * 造失败时原来那一局原样保留。FXGL 迁移的阶段 5 会由表现层的 {@code SessionService} 接替本类。
 */
public class GameEngine {
    private static final System.Logger log = System.getLogger(GameEngine.class.getName());

    private static volatile GameEngine instance;

    /** 最外层的阶段状态机，贯穿整个进程，不随开局重建 */
    private final GameFlow gameFlow = new GameFlow();

    private TowerContent content;
    private SaveRepository saveRepository;
    private GameSession session;

    private GameEngine() {
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

    /**
     * 装上加载好的塔（启动时调用一次），并按本塔规则建好存档库（档位索引另由加载期任务扫描）。
     */
    public void install(TowerContent content) {
        this.content = content;
        this.saveRepository = new SaveRepository(SaveRepository.defaultDirectory(),
                content.rules().save().slotCount(), SaveMigrations.standard());
    }

    public TowerContent getContent() {
        return content;
    }

    public GameFlow getGameFlow() {
        return gameFlow;
    }

    /** 当前这一局；标题界面下（还没开过局）为 null */
    public GameSession getSession() {
        return session;
    }

    /** 是否已有一局在进行（标题界面下为 false） */
    public boolean hasActiveGame() {
        return session != null;
    }

    public PlayerStateManager getPlayerStateManager() {
        return session == null ? null : session.player();
    }

    public MapManager getMapManager() {
        return session == null ? null : session.maps();
    }

    // ==================== 对局命令（转给当前会话）====================

    public MoveResult handlePlayerMove(Direction direction) {
        return session.move(direction);
    }

    public void handleDirectionChange() {
        session.turn();
    }

    /**
     * 玩家在菜单里改了会影响战斗的东西（穿脱装备、切换就绪预设、改预设内容）之后调用，
     * 让当前层的伤害数字与之同步。
     */
    public void recalculateCurrentFloorDamage() {
        session.recalculateDamage();
    }

    /** 是否还有可撤销的行动 */
    public boolean canUndo() {
        return session != null && session.canUndo();
    }

    public UndoHistory getUndoHistory() {
        return session.undoHistory();
    }

    // ==================== 换一局 ====================

    /**
     * 开始全新的一局，随后经 {@code GameFlow.startNewGame} 广播「新开一局」并把阶段推进到 {@code PLAYING}。
     */
    public void startNewGame() {
        session = GameSession.newGame(content);
        gameFlow.startNewGame();
    }

    /**
     * 撤销最近一次有意义的行动：按它之前的快照重建一局（沿用同一份撤销历史），阶段回到 {@code PLAYING}。
     *
     * @return 被撤销的行动描述，没有可撤销的行动时为 null
     */
    public String undoLastAction() {
        UndoHistory history = session.undoHistory();
        UndoHistory.Entry entry = history.pop();
        if (entry == null) {
            return null;
        }
        session = GameSession.restore(content, entry.snapshot(), history);
        gameFlow.resumeSession();
        return entry.label();
    }

    /** 把当前这局拍成快照（撤销与存档共用） */
    public GameSnapshot captureSnapshot() {
        return session.capture();
    }

    // ==================== 存档 ====================

    public SaveRepository getSaveRepository() {
        return saveRepository;
    }

    /** 把当前这局写入指定档位（覆盖原有） */
    public void saveGame(String slotId) throws IOException {
        saveRepository.write(slotId, session.describe(LocalDateTime.now()), session.capture());
    }

    /** 写自动存档（切换楼层 / 进入新区域之后）；失败只记录原因并返回 false，不打断游戏 */
    public boolean autoSave() {
        try {
            saveGame(SaveRepository.AUTO_SLOT);
            return true;
        } catch (IOException | RuntimeException e) {
            log.log(System.Logger.Level.WARNING, "自动存档失败：" + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 读档：按存档里的快照重建一局（撤销历史为空），阶段迁到 {@code PLAYING}。
     *
     * @throws SaveException 档位为空 / 损坏 / 版本不兼容，或快照与当前塔的数据对不上（区域已不存在等）；
     *                       此时当前这一局（若有）原样保留
     */
    public void loadGame(String slotId) throws SaveException {
        SaveFile save = saveRepository.read(slotId);
        GameSession restored;
        try {
            restored = GameSession.restore(content, save.snapshot(), GameSession.emptyUndoHistory(content));
        } catch (RuntimeException e) {
            throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档与当前塔的数据对不上：" + e.getMessage(), e);
        }
        session = restored;
        gameFlow.resumeSession();
    }
}
