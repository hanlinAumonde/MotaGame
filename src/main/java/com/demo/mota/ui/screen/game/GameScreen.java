package com.demo.mota.ui.screen.game;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.region.RegionInfo;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.ValueFormatter;
import com.demo.mota.engine.state.equipset.EquipSetResult;
import com.demo.mota.ui.screen.ChordKey;
import com.demo.mota.ui.screen.PresetHotkeys;
import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.screen.equipset.EquipmentSetState;
import com.demo.mota.ui.screen.game.map.MapView;
import com.demo.mota.ui.screen.game.side.SidePanelRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;


/**
 * 对局界面（{@link GamePhase#PLAYING}）：左侧状态面板 + 中间地图 + 右侧技能装备栏（{@code side} 子包）。
 *
 * <p>负责移动分发、状态面板绘制；地图（含伤害数字）交给注入的 {@link MapView}（{@code map} 子包），每次重绘时同步一次；
 * 按 X / D / Q / A 迁往游戏菜单 / 技能设置 / 装备 / 套装界面，那些界面各自是独立的 Screen。
 * 组合快捷键：按住 D 再按数字键切换技能组、按住 W 再按数字键换上套装（{@link ChordKey}）。
 * Backspace 撤销最近一次开门 / 战斗 / 拾取（{@link #undoLastAction()}，游戏结束框里同样可用）。
 *
 * <p>地图有动画（走一格的平滑移动、换层的黑幕）。动画没播完时方向键先进 {@link StepBuffer}，
 * 播完（{@link MapView#setOnAnimationsFinished}）再走下一步；按住方向键即连续行走。
 * 其他按键（菜单、撤销、组合键）不等动画，照常立即处理。
 *
 * <p>每次回到本界面都重算一次当前层伤害：其他界面里穿脱装备、改预设都可能影响战斗推演，
 * 统一在这里补算，那些界面就不必各自汇报「改没改」。当前层怪物只有几十只，开销可以忽略。
 *
 * <p>本界面同时负责<b>死亡检测</b>：每次玩家行动之后检查一次 {@code isDead()}，
 * 一旦归零就迁到 {@code GAME_OVER}。判死点放在这里而不是战斗结算里，
 * 是为了让将来的岩浆、毒、陷阱等一切掉血途径都走同一个口子。
 */
public class GameScreen implements Screen {
    /**
     * 对局中呼出其他界面的快捷键（物品栏暂无快捷键，只能经游戏菜单进入）。
     * 技能设置（D）不在这里：D 同时是技能组快捷键的修饰键，只有「单按」松开时才打开，见 {@link #handleKeyRelease}
     */
    private static final Map<KeyCode, GamePhase> SCREEN_KEYS = Map.of(
            KeyCode.X, GamePhase.GAME_MENU,
            KeyCode.Q, GamePhase.EQUIPMENT,
            KeyCode.A, GamePhase.EQUIPMENT_SETS);

    private final GameEngine engine;
    private final ResourceManager resourceManager;
    private final GameFlow flow;

    private final Canvas statusCanvas;
    private final MapView mapView;
    private final Canvas sideCanvas;
    private final SidePanelRenderer sidePanelRenderer;

    /** 按住 D 再按数字键：激活 / 停用技能组；单按 D：打开技能设置 */
    private final ChordKey presetChord = new ChordKey(PresetHotkeys.SKILL_CHORD);
    /** 按住 W 再按数字键：换上对应套装 */
    private final ChordKey setChord = new ChordKey(PresetHotkeys.EQUIPMENT_SET_CHORD);
    /** 数字键 → 预设下标，取自塔规则 {@code skill.hotkeys} */
    private final Map<KeyCode, Integer> presetHotkeys = PresetHotkeys.fromRules();
    /** 数字键 → 套装下标，取自塔规则 {@code equipment.setHotkeys} */
    private final Map<KeyCode, Integer> setHotkeys = PresetHotkeys.equipmentSetsFromRules();

    /** 文本测量与绘制工具（阴影描边、自适应字号、折行） */
    private final TextPainter painter = new TextPainter();

    /** 动画期间缓下来的方向键，见类注释 */
    private final StepBuffer steps = new StepBuffer();

    private String currentMessage = "";

    public GameScreen(GameEngine engine, ResourceManager resourceManager, GameFlow flow,
                      Canvas statusCanvas, MapView mapView, Canvas sideCanvas) {
        this.engine = engine;
        this.resourceManager = resourceManager;
        this.flow = flow;
        this.statusCanvas = statusCanvas;
        this.mapView = mapView;
        this.sideCanvas = sideCanvas;
        this.sidePanelRenderer = new SidePanelRenderer(resourceManager);
        mapView.setOnAnimationsFinished(this::continueWalking);
    }

    /** 新的一局：清掉上一局残留的消息 */
    @Override
    public void onNewGame() {
        this.currentMessage = "";
    }

    /** 其他界面带回来的一句提示（如「已切换到第1号套装」），显示在状态栏消息处 */
    public void showMessage(String message) {
        this.currentMessage = message == null ? "" : message;
    }

    /** 回到对局（新开一局或从其他界面返回）：补算当前层伤害，见类注释 */
    @Override
    public void onEnter() {
        // 松键可能落在了别的界面里，回来时不能让「按住」状态残留
        presetChord.reset();
        setChord.reset();
        steps.clear();
        engine.recalculateCurrentFloorDamage();
    }

    /** 窗口失焦：之后的松键事件收不到，按住的方向键 / 修饰键都当作已松开，免得回来时自己一直走 */
    @Override
    public void onFocusLost() {
        presetChord.reset();
        setChord.reset();
        steps.clear();
    }

    // ==================== 输入 ====================

    @Override
    public void handleKey(KeyCode code) {
        if (code == presetChord.key()) {
            presetChord.press(code);
            return;
        }
        if (code == setChord.key()) {
            setChord.press(code);
            return;
        }
        // 按住 D 期间按了任何别的键（数字、方向……），松开 D 时都不再算「单按」，不会再弹出技能设置
        presetChord.markUsed();
        setChord.markUsed();
        if (handleChordDigit(code)) {
            return;
        }

        GamePhase screen = SCREEN_KEYS.get(code);
        if (screen != null) {
            flow.to(screen);
            return;
        }

        switch (code) {
            case BACK_SPACE -> undoLastAction();
            case Z -> {
                this.engine.handleDirectionChange();
                // 只有朝向变了，同步时只会重画玩家实体
                mapView.sync();
            }
            default -> {
                Direction direction = toDirection(code);
                if (direction == null) break;

                boolean fresh = steps.press(direction);
                if (mapView.isAnimating()) {
                    // 上一步还没走完：新按下的记下来，等动画播完再走；自动重复不记（按着就会接着走）
                    if (fresh) {
                        steps.queue(direction);
                    }
                } else {
                    move(direction);
                }
            }
        }
    }

    /** 走一步（或撞上怪物 / 门 / 楼梯……），之后统一判死 */
    private void move(Direction direction) {
        MoveResult result = engine.handlePlayerMove(direction);
        handleMoveResult(result);
        autoSaveAfter(result);
        playFloorTransitionAfter(result);
        render();

        // 行动之后统一判死：战斗、以及将来的岩浆 / 毒 / 陷阱都汇到这一处
        if (engine.getPlayerStateManager().isDead()) {
            flow.to(GamePhase.GAME_OVER);
        }
    }

    /** 地图动画播完：接着走缓下来的一步，或沿仍按着的方向继续走 */
    private void continueWalking() {
        if (flow.getPhase() != GamePhase.PLAYING) {
            // 动画播的途中切去了别的界面：回来时不该自己接着走
            steps.clear();
            return;
        }
        Direction next = steps.next();
        if (next != null) {
            move(next);
        }
    }

    /** 换层 / 进入新区域：地图换图时播一段黑幕过渡，黑屏上写出到了哪 */
    private void playFloorTransitionAfter(MoveResult result) {
        switch (result.getType()) {
            case FLOOR_SWITCHED, REGION_ENTERED -> {
                RegionInfo region = engine.getMapManager().getCurrentRegion();
                String floor = "第 " + engine.getMapManager().getCurrentMap().getFloorNumber() + " 层";
                // 区域名本身常带「·」（如「试炼之塔·下层」），与楼层分两行写，免得连成一串
                mapView.playFloorTransition(region == null ? floor : region.name() + "\n" + floor);
            }
            default -> { }
        }
    }

    /**
     * 撤销最近一次有意义的行动（Backspace）。引擎按快照重建这一局后各界面随之重建（状态栏消息也被清掉），
     * 因此提示在恢复之后再写、再重绘。游戏结束框里按 Backspace 也走这里：恢复时阶段已迁回 {@code PLAYING}。
     */
    public void undoLastAction() {
        // 撤销整局重建，行走动画随之打断：缓下来的那一步不该落在撤销后的局面上
        steps.clear();
        String undone = engine.undoLastAction();
        currentMessage = undone == null ? "没有可撤销的行动" : "已撤销：" + undone;
        render();
    }

    /** 切换楼层 / 进入新区域之后写自动存档；失败只在状态栏消息后补一句 */
    private void autoSaveAfter(MoveResult result) {
        switch (result.getType()) {
            case FLOOR_SWITCHED, REGION_ENTERED -> {
                if (!engine.autoSave()) {
                    currentMessage = currentMessage + "（自动存档失败）";
                }
            }
            default -> { }
        }
    }

    /** 显示一句状态栏提示并立即重绘（读档后由存读档界面调用：此时阶段已迁到对局、界面已画过一遍） */
    public void showMessageNow(String message) {
        showMessage(message);
        render();
    }

    /** 单按 D（按住期间没有组合数字键）松开时才打开技能设置 */
    @Override
    public void handleKeyRelease(KeyCode code) {
        Direction direction = toDirection(code);
        if (direction != null) {
            steps.release(direction);
        }
        if (presetChord.release(code)) {
            flow.to(GamePhase.SKILL_SETUP);
        }
        setChord.release(code);
    }

    /**
     * 数字键：按住 D 时切换技能组，按住 W 时换上套装，都没按住时不做任何事。
     *
     * @return 是否是数字键（已消费）
     */
    private boolean handleChordDigit(KeyCode code) {
        Integer preset = presetHotkeys.get(code);
        Integer set = setHotkeys.get(code);
        if (preset == null && set == null) {
            return false;
        }
        boolean changed = false;
        if (presetChord.isHeld() && preset != null) {
            changed = togglePreset(preset);
        } else if (setChord.isHeld() && set != null) {
            changed = applyEquipmentSet(set);
        }
        if (changed) {
            engine.recalculateCurrentFloorDamage();
        }
        render();
        return true;
    }

    /** 对局中 W+数字键换上套装，结果写进状态栏消息 */
    private boolean applyEquipmentSet(int setIndex) {
        PlayerStateManager player = engine.getPlayerStateManager();
        EquipSetResult result = player.applyEquipmentSet(setIndex);
        currentMessage = EquipmentSetState.describe(player, setIndex, result);
        return result.isOk();
    }

    private static Direction toDirection(KeyCode code) {
        return switch (code) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case LEFT -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }

    /**
     * 游戏中按 D+数字键：激活对应预设；对已激活的那套再按一次则停用（全程普攻）。结果写进状态栏消息。
     *
     * @return 是否真的切换了（下标超出预设套数时不动）
     */
    private boolean togglePreset(int presetIndex) {
        PlayerStateManager player = engine.getPlayerStateManager();
        if (presetIndex >= player.getPresetBook().presetCount()) {
            return false;
        }
        int armed = player.togglePreset(presetIndex);
        if (armed == SkillPresetBook.NONE) {
            currentMessage = "已停用技能组（全程普攻）";
        } else {
            SkillPreset preset = player.getPresetBook().get(armed);
            currentMessage = preset.getName() + (preset.isEmpty() ? " 已激活（未编排）" : " 已激活");
        }
        return true;
    }

    // ==================== 渲染入口 ====================

    @Override
    public void render() {
        mapView.sync();
        renderStatusPanel();
        sidePanelRenderer.render(sideCanvas, engine.getPlayerStateManager());
    }

    private void handleMoveResult(MoveResult result) {
        String detail = result.getDetail();
        currentMessage = switch (result.getType()) {
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

    // ==================== 状态面板渲染 ====================

    private void renderStatusPanel() {
        GraphicsContext gc = statusCanvas.getGraphicsContext2D();
        double w = statusCanvas.getWidth();
        double h = statusCanvas.getHeight();
        PlayerStateManager player = engine.getPlayerStateManager();
        GameMap map = engine.getMapManager().getCurrentMap();

        drawStatusBackground(gc, w, h);
        drawPlayerAvatar(gc, player, w);
        drawStats(gc, player);
        drawKeys(gc, player);
        drawFloorInfo(gc, map, h);
        drawMessage(gc, w);
    }

    private void drawStatusBackground(GraphicsContext gc, double w, double h) {
        Image bgTile = resourceManager.getTileImage("wall_normal");
        if (bgTile != null) {
            for (double ty = 0; ty < h; ty += 32) {
                for (double tx = 0; tx < w; tx += 32) {
                    gc.drawImage(bgTile, tx, ty, 32, 32);
                }
            }
        } else {
            gc.setFill(Color.web("#3a3a3a"));
            gc.fillRect(0, 0, w, h);
        }
    }

    private void drawPlayerAvatar(GraphicsContext gc, PlayerStateManager player, double panelWidth) {
        double topY = 18;
        double iconSize = 52;
        double iconX = 14;

        Image playerSprite = resourceManager.getPlayerSprite(Direction.DOWN);
        if (playerSprite != null) {
            gc.drawImage(playerSprite, iconX, topY, iconSize, iconSize);
        }

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 34));
        painter.drawShadowText(gc, String.valueOf(player.getLevelNumber()), iconX + iconSize + 18, topY + 38);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 28));
        painter.drawShadowText(gc, "级", panelWidth - 42, topY + 38);
    }

    private void drawStats(GraphicsContext gc, PlayerStateManager player) {
        double statsY = 115;
        double statsX = 14;
        double lineHeight = 38;
        double valueX = statsX + 60;
        double valueMaxWidth = statusCanvas.getWidth() - valueX - 8;

        GameNumber maxHp = player.getMaxHP();
        String[] labels = {"生命", "攻击", "防御", "金币", "经验"};
        String[] values = {
                ValueFormatter.formatScaled(player.getCurrentHP(), maxHp)
                        + "/" + ValueFormatter.formatScaled(maxHp, maxHp),
                ValueFormatter.formatScaled(player.getEffectiveATK(), maxHp),
                ValueFormatter.formatScaled(player.getEffectiveDEF(), maxHp),
                String.valueOf(player.getCurrentGoldAmount()),
                ValueFormatter.formatScaled(player.getCurrentExp(), maxHp)
        };

        for (int i = 0; i < labels.length; i++) {
            double ly = statsY + i * lineHeight;

            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            painter.drawShadowText(gc, labels[i], statsX, ly);

            painter.drawFittedShadowText(gc, values[i], "Consolas", valueX, ly, valueMaxWidth, 22, 12);
        }
    }

    private void drawKeys(GraphicsContext gc, PlayerStateManager player) {
        double keysY = 340;
        double keysX = 14;
        double keyIconSize = 40;
        double keyLineHeight = 58;

        KeyColor[] keyColors = {KeyColor.YELLOW, KeyColor.BLUE, KeyColor.RED};
        String[] keyItemIds = {"key-001-yellow", "key-001-blue", "key-001-red"};

        for (int i = 0; i < 3; i++) {
            double ky = keysY + i * keyLineHeight;

            Image keyImg = resourceManager.getItemImage(keyItemIds[i]);
            if (keyImg != null) {
                gc.drawImage(keyImg, keysX, ky, keyIconSize, keyIconSize);
            }

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
            String countStr = String.format("%02d", player.getKeyCount(keyColors[i]));
            painter.drawShadowText(gc, countStr, keysX + keyIconSize + 14, ky + keyIconSize * 0.7);

            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            painter.drawShadowText(gc, "个", keysX + keyIconSize + 64, ky + keyIconSize * 0.7);
        }
    }

    private void drawFloorInfo(GraphicsContext gc, GameMap map, double h) {
        RegionInfo region = engine.getMapManager().getCurrentRegion();
        if (region != null) {
            gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 16));
            painter.drawShadowText(gc, region.name(), 14, h - 58);
        }
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        String floorText = "第 " + map.getFloorNumber() + " 层";
        painter.drawShadowText(gc, floorText, 14, h - 25);
    }

    private void drawMessage(GraphicsContext gc, double w) {
        if (currentMessage == null || currentMessage.isEmpty()) return;

        double msgY = 530;
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRect(6, msgY, w - 12, 28);

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 15));
        gc.setFill(Color.web("#ff6b6b"));
        gc.fillText(currentMessage, 12, msgY + 20);
    }
}
