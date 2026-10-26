package com.demo.mota.scene.game;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.SubScene;
import com.demo.mota.app.input.GameplayInput;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.equipset.EquipSetResult;
import com.demo.mota.hud.HudModel;
import com.demo.mota.hud.StatusPanel;
import com.demo.mota.hud.side.SidePanelView;
import com.demo.mota.scene.equipset.EquipmentSetState;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.world.MapPresenter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;


/**
 * 对局画面（FXGL 的游戏场景）：左侧状态栏（{@link StatusPanel}）+ 中间地图 + 右侧技能装备栏（{@link SidePanelView}）。
 *
 * <p>本对象贯穿整个进程，每局开始时（FXGL 的 {@code initUI}，见 {@link #onNewGame()}）重新搭两侧栏、挂进游戏场景、
 * 新建地图显示（{@link MapPresenter}）——{@code startNewGame()} 会清空游戏场景的世界、UI 节点与世界变量。
 * 状态栏绑定世界变量（{@link HudModel}），每次 {@link #render()} 只写变量、变了的文字自己更新。按键由 {@link GameplayInput}
 * 注册的 FXGL 动作翻译成这里的命令（{@link GameplayInput.Commands}）：走一步（或撞上怪物 / 门 / 楼梯……）、转身、撤销、
 * 叠上装备 / 套装 / 技能设置界面、切换技能组、换上套装。X（游戏菜单）由 FXGL 的菜单键处理。
 *
 * <p>主角走一格有平滑移动。没走完时方向键先进 {@link StepBuffer}，
 * 走完（{@link MapPresenter#setOnStepFinished}）再走下一步；按住方向键即连续行走。
 * 换层的黑幕是叠上来的子场景，期间按键不会到这里。
 * 主角没有走动的行动（撞墙、开门、打怪）之后没有「走完一步」来接续，方向键按住超过 {@link #HOLD_REPEAT_SECONDS}
 * 时每帧再试一次，与以前系统按键重复的效果一致（FXGL 的动作不会因按键重复再次触发）。
 *
 * <p>每次回到对局（{@link #returnToGame()}）都重算一次当前层伤害：其他界面里穿脱装备、改预设都可能影响战斗推演，
 * 统一在这里补算，那些界面就不必各自汇报「改没改」。当前层怪物只有几十只，开销可以忽略。
 *
 * <p>本界面同时负责<b>死亡检测</b>：每次玩家行动之后检查一次 {@code isDead()}，
 * 一旦归零就叠上游戏结束横幅。判死点放在这里而不是战斗结算里，
 * 是为了让将来的岩浆、毒、陷阱等一切掉血途径都走同一个口子。
 */
public class GameScreen implements GameplayInput.Commands {

    /** 对局中经快捷键或游戏逻辑叠上的子场景，由组装处提供 */
    public interface Menus {
        SubScene equipment();

        SubScene equipmentSets();

        SubScene skillSetup();

        SubScene gameOver();
    }

    /** 方向键按住多久之后，主角空闲时每帧再试着行动一次（与系统按键重复的默认延迟相当） */
    static final double HOLD_REPEAT_SECONDS = 0.5;

    private final SessionService sessions;
    private final AssetService assets;
    private final Navigator navigator;
    private final Menus menus;

    private final List<String> sidePanelSections;
    /** 以下每局新建：startNewGame() 会清空世界里的实体、UI 节点与世界变量 */
    private SidePanelView sidePanel;
    private MapPresenter mapPresenter;

    /** 动画期间缓下来的方向键，见类注释 */
    private final StepBuffer steps = new StepBuffer();
    /** 本局对局场景活动的累计秒数（被子场景盖住时不走），用来判断方向键按住了多久 */
    private double clock;

    private String currentMessage = "";

    public GameScreen(SessionService sessions, AssetService assets, Navigator navigator, Menus menus) {
        this.sessions = sessions;
        this.assets = assets;
        this.navigator = navigator;
        this.menus = menus;
        this.sidePanelSections = sessions.content().rules().sidePanel().sections();
    }

    /** 地图位置的透明占位：只撑开尺寸，让两侧栏落在原位；地图是世界层里的实体，从这里透出来 */
    private static Region mapSlot() {
        Region slot = new Region();
        slot.setMinSize(MotaLayout.MAP_SIZE, MotaLayout.MAP_SIZE);
        slot.setPrefSize(MotaLayout.MAP_SIZE, MotaLayout.MAP_SIZE);
        slot.setMaxSize(MotaLayout.MAP_SIZE, MotaLayout.MAP_SIZE);
        return slot;
    }

    // ==================== 一局的开始与回到对局 ====================

    /**
     * 新的一局（含读档）：由 FXGL 的 {@code initUI} 调用——加载场景的后台线程，游戏场景尚未显示，
     * 在上面搭节点、生成实体是安全的。会话服务此前已在 {@code initGame} 里装上了这一局。
     */
    public void onNewGame() {
        resetHeld();
        clock = 0;
        currentMessage = sessions.takeStartMessage();
        mapPresenter = new MapPresenter(sessions, assets, navigator);
        mapPresenter.setOnStepFinished(this::continueWalking);
        StatusPanel statusPanel = new StatusPanel(assets, MotaLayout.STATUS_WIDTH, MotaLayout.MAP_SIZE);
        sidePanel = new SidePanelView(assets, sidePanelSections, MotaLayout.SIDE_WIDTH, MotaLayout.MAP_SIZE);
        FXGL.getGameScene().addUINode(new HBox(0, statusPanel.node(), mapSlot(), sidePanel.node()));
        render();
    }

    /** 从叠在上面的界面回到对局：补算当前层伤害，见类注释 */
    public void returnToGame() {
        // 松键可能落在了别的界面里，回来时不能让「按住」状态残留
        resetHeld();
        sessions.recalculateDamage();
        render();
    }

    /** 清掉按住的方向键与缓下来的一步：离开对局之前、窗口失焦时（之后的松键事件收不到）、回到对局时 */
    public void resetHeld() {
        steps.clear();
    }

    /** 其他界面带回来的一句提示（如「已切换到第1号套装」），显示在状态栏消息处 */
    public void showMessage(String message) {
        this.currentMessage = message == null ? "" : message;
    }

    // ==================== 对局命令（由 GameplayInput 的动作调用） ====================

    /** 方向键按下：主角空闲则立即走一步，否则记为动画播完后要走的一步 */
    @Override
    public void pressDirection(Direction direction) {
        steps.press(direction, clock);
        if (mapPresenter.isAnimating()) {
            steps.queue(direction);
        } else {
            move(direction);
        }
    }

    @Override
    public void releaseDirection(Direction direction) {
        steps.release(direction);
    }

    /** 每帧调用（只在对局场景活动时）：方向键按住够久、主角又空闲时接着行动，见类注释 */
    public void onFrame(double tpf) {
        if (mapPresenter == null) {
            return;
        }
        clock += tpf;
        Direction held = steps.repeatDue(clock, HOLD_REPEAT_SECONDS);
        if (held != null && !mapPresenter.isAnimating()) {
            move(held);
        }
    }

    @Override
    public void turn() {
        sessions.turn();
        // 只有朝向变了，同步时只会重画玩家实体
        mapPresenter.sync();
    }

    @Override
    public void openEquipment() {
        navigator.push(menus.equipment());
    }

    @Override
    public void openEquipmentSets() {
        navigator.push(menus.equipmentSets());
    }

    @Override
    public void openSkillSetup() {
        navigator.push(menus.skillSetup());
    }

    /** 走一步（或撞上怪物 / 门 / 楼梯……），之后统一判死 */
    private void move(Direction direction) {
        MoveResult result = sessions.move(direction);
        handleMoveResult(result);
        autoSaveAfter(result);
        // 换了层的话，地图显示据 FloorChanged 事件先叠上黑幕过渡、黑幕下再换图
        render();

        // 行动之后统一判死：战斗、以及将来的岩浆 / 毒 / 陷阱都汇到这一处
        if (sessions.session().player().isDead()) {
            navigator.push(menus.gameOver());
        }
    }

    /** 主角走完一步：接着走缓下来的一步，或沿仍按着的方向继续走 */
    private void continueWalking() {
        if (!navigator.isInGame()) {
            // 走的途中叠上了别的界面：回来时不该自己接着走
            steps.clear();
            return;
        }
        Direction next = steps.next();
        if (next != null) {
            move(next);
        }
    }

    /**
     * 撤销最近一次有意义的行动（Backspace；游戏结束横幅里也走这里，此时横幅已弹回对局）。
     * 会话按快照重建这一局，地图显示发现换了一张地图会整张重建。
     */
    @Override
    public void undoLastAction() {
        // 撤销整局重建，行走动画随之打断：缓下来的那一步不该落在撤销后的局面上
        steps.clear();
        String undone = sessions.undoLastAction();
        currentMessage = undone == null ? "没有可撤销的行动" : "已撤销：" + undone;
        render();
    }

    /** 切换楼层 / 进入新区域之后写自动存档；失败只在状态栏消息后补一句 */
    private void autoSaveAfter(MoveResult result) {
        switch (result.type()) {
            case FLOOR_SWITCHED, REGION_ENTERED -> {
                if (!sessions.autoSave()) {
                    currentMessage = currentMessage + "（自动存档失败）";
                }
            }
            default -> { }
        }
    }

    /** 对局中 W+数字键换上套装，结果写进状态栏消息；换成功则重算伤害 */
    @Override
    public void applyEquipmentSet(int setIndex) {
        PlayerStateManager player = sessions.session().player();
        EquipSetResult result = player.applyEquipmentSet(setIndex);
        currentMessage = EquipmentSetState.describe(player, setIndex, result);
        if (result.isOk()) {
            sessions.recalculateDamage();
        }
        render();
    }

    /**
     * 游戏中按 D+数字键：激活对应预设；对已激活的那套再按一次则停用（全程普攻）。结果写进状态栏消息。
     * 下标超出预设套数时不动。
     */
    @Override
    public void togglePreset(int presetIndex) {
        PlayerStateManager player = sessions.session().player();
        if (presetIndex >= player.getPresetBook().presetCount()) {
            render();
            return;
        }
        int armed = player.togglePreset(presetIndex);
        if (armed == SkillPresetBook.NONE) {
            currentMessage = "已停用技能组（全程普攻）";
        } else {
            SkillPreset preset = player.getPresetBook().get(armed);
            currentMessage = preset.getName() + (preset.isEmpty() ? " 已激活（未编排）" : " 已激活");
        }
        sessions.recalculateDamage();
        render();
    }

    // ==================== 渲染入口 ====================

    public void render() {
        mapPresenter.sync();
        HudModel.update(sessions.session(), currentMessage);
        sidePanel.refresh(sessions.session().player());
    }

    private void handleMoveResult(MoveResult result) {
        String detail = result.detail();
        currentMessage = switch (result.type()) {
            case BATTLE_WON -> "击败 " + detail + "！";
            case BATTLE_LOST -> "败给 " + detail + "！";
            case ITEM_PICKED -> "获得 " + detail;
            case DOOR_OPENED -> detail + "已打开";
            case DARK_WALL_REVEALED -> "发现了暗墙！";
            case FLOOR_SWITCHED -> "前往" + detail;
            case REGION_GATE_OPENED -> "区域检查通过，通路已开启";
            case REGION_GATE_LOCKED -> "区域检查未通过：" + detail;
            case REGION_EXIT_LOCKED -> "需先通过本区域的检查";
            case REGION_BACKWARD_BLOCKED -> "已通过的区域无法返回";
            case REGION_ENTERED -> "进入区域：" + detail;
            case BLOCKED, NO_MOVE, MOVED -> "";
        };
    }
}
