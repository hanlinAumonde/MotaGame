package com.demo.mota.ui.screen;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.Item.AbilityGem;
import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.Item.GenericItem.GenericItem;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.Item.Key;
import com.demo.mota.engine.Item.Portion;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.DamagePalette;
import com.demo.mota.ui.EquipmentRenderer;
import com.demo.mota.ui.MenuRenderer;
import com.demo.mota.ui.SkillSetupRenderer;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.ValueFormatter;
import com.demo.mota.ui.overlay.EquipmentOverlay;
import com.demo.mota.ui.overlay.GameMenuOverlay;
import com.demo.mota.ui.overlay.Overlay;
import com.demo.mota.ui.overlay.SkillSetupOverlay;
import com.demo.mota.ui.side.SidePanelRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

/**
 * 对局界面：左侧状态面板 + 右侧地图，以及覆盖其上的游戏内菜单。
 *
 * <p>由 {@code MotaController} 里搬出来的原样逻辑——移动分发、状态面板绘制、
 * 地图绘制（精灵图优先 + 纯色 fallback）、伤害数字。Controller 从此只做阶段路由。
 *
 * <p>本界面同时负责<b>死亡检测</b>：每次玩家行动之后检查一次 {@code isDead()}，
 * 一旦归零就迁到 {@code GAME_OVER}。判死点放在这里而不是战斗结算里，
 * 是为了让将来的岩浆、毒、陷阱等一切掉血途径都走同一个口子。
 */
public class GameScreen implements Screen {
    /**
     * 伤害数字字号（相对格宽）。收缩记法已把长度硬约束在 6 个字符内，
     * 故直接取能整格容纳该上限的最大字号并全图统一，避免同层怪物因位数不同而字号参差。
     */
    private static final double DAMAGE_FONT_RATIO = 0.26;
    /** 兜底字号下限：仅在超出末级单位等极端情况下才会触发收缩 */
    private static final double DAMAGE_FONT_MIN_RATIO = 0.17;

    private final GameEngine engine;
    private final ResourceManager resourceManager;
    private final GameFlow flow;

    private final Canvas statusCanvas;
    private final Canvas mapCanvas;
    private final Canvas sideCanvas;
    private final Canvas menuCanvas;
    private final SidePanelRenderer sidePanelRenderer;

    /** 可呼出的覆盖层（X 游戏菜单 / D 技能设置 / Q 装备），同一时刻至多一个打开 */
    private final List<Overlay> overlays;
    private Overlay activeOverlay;

    /** 切换就绪预设的按键 → 预设下标，取自塔规则 {@code skill.hotkeys} */
    private final Map<KeyCode, Integer> presetHotkeys;

    /** 文本测量与绘制工具（阴影描边、自适应字号、折行） */
    private final TextPainter painter = new TextPainter();

    private final double cellSize;
    private String currentMessage = "";

    public GameScreen(GameEngine engine, ResourceManager resourceManager, GameFlow flow,
                      Canvas statusCanvas, Canvas mapCanvas, Canvas sideCanvas, Canvas menuCanvas) {
        this.engine = engine;
        this.resourceManager = resourceManager;
        this.flow = flow;
        this.statusCanvas = statusCanvas;
        this.mapCanvas = mapCanvas;
        this.sideCanvas = sideCanvas;
        this.menuCanvas = menuCanvas;
        this.sidePanelRenderer = new SidePanelRenderer(resourceManager);
        this.overlays = List.of(
                new GameMenuOverlay(engine, new MenuRenderer(resourceManager)),
                new SkillSetupOverlay(engine, new SkillSetupRenderer(resourceManager)),
                new EquipmentOverlay(engine, new EquipmentRenderer(resourceManager)));
        this.presetHotkeys = parseHotkeys(GameRules.get().skill().hotkeys());
        this.cellSize = mapCanvas.getWidth() / MAP_SIDE_LENGTH;
    }

    /** 规则里写的是 JavaFX {@code KeyCode} 名；写错的忽略，不影响其余按键 */
    private static Map<KeyCode, Integer> parseHotkeys(List<String> names) {
        Map<KeyCode, Integer> keys = new EnumMap<>(KeyCode.class);
        for (int i = 0; i < names.size(); i++) {
            try {
                keys.put(KeyCode.valueOf(names.get(i).trim().toUpperCase()), i);
            } catch (IllegalArgumentException e) {
                System.err.println("[GameScreen] 无法识别的预设按键: " + names.get(i));
            }
        }
        return keys;
    }

    /** 每次进入对局都是一局新游戏的开头，清掉上一局残留的消息 */
    @Override
    public void onEnter() {
        this.currentMessage = "";
        this.activeOverlay = null;
    }

    // ==================== 输入 ====================

    @Override
    public boolean handleKey(KeyCode code) {
        // 覆盖层打开时独占输入（技能设置界面里也允许用数字键切换就绪预设，方便边改边看）
        if (activeOverlay != null && activeOverlay.isOpen()) {
            boolean changed = false;
            if (code == Overlay.CLOSE_KEY) {
                // 所有游戏内界面统一用 X 返回游戏，各覆盖层不单独处理
                activeOverlay.close();
            } else if (presetHotkeys.containsKey(code) && activeOverlay instanceof SkillSetupOverlay) {
                // 技能设置界面里，数字键切换正在编辑（同时激活）的预设
                changed = engine.getSkillSetupMenu().selectPreset(presetHotkeys.get(code));
            } else {
                changed = activeOverlay.handleKey(code);
            }
            if (changed) {
                engine.recalculateCurrentFloorDamage();
            }
            if (!activeOverlay.isOpen()) {
                activeOverlay = null;
                renderAll();
            }
            renderOverlay();
            return true;
        }

        for (Overlay overlay : overlays) {
            if (overlay.toggleKey() == code) {
                overlay.open();
                activeOverlay = overlay;
                renderOverlay();
                return true;
            }
        }

        if (presetHotkeys.containsKey(code)) {
            if (togglePreset(presetHotkeys.get(code))) {
                engine.recalculateCurrentFloorDamage();
            }
            renderAll();
            return true;
        }

        return switch (code) {
            case Z -> {
                this.engine.handleDirectionChange();
                //当前玩家站立的tile必定为可通过地形
                renderTileAndPlayerAt(mapCanvas.getGraphicsContext2D(), this.engine.getMapManager().getPlayerPosition());
                yield true;
            }
            default -> {
                Direction direction = toDirection(code);
                if (direction == null) yield false;

                MoveResult result = engine.handlePlayerMove(direction);
                handleMoveResult(result);
                renderAll();

                // 行动之后统一判死：战斗、以及将来的岩浆 / 毒 / 陷阱都汇到这一处
                if (engine.getPlayerStateManager().isDead()) {
                    flow.toGameOver();
                }
                yield true;
            }
        };
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
     * 游戏中按数字键：激活对应预设；对已激活的那套再按一次则停用（全程普攻）。结果写进状态栏消息。
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
        renderAll();
        renderOverlay();
    }

    private void renderAll() {
        renderMap();
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
            case BLOCKED, NO_MOVE, MOVED -> "";
        };
    }

    // ==================== 菜单渲染 ====================

    /** 菜单画布平时隐藏，有覆盖层打开时整屏盖住游戏画面 */
    private void renderOverlay() {
        boolean open = activeOverlay != null && activeOverlay.isOpen();
        menuCanvas.setVisible(open);
        if (!open) return;
        activeOverlay.render(menuCanvas.getGraphicsContext2D(), menuCanvas.getWidth(), menuCanvas.getHeight());
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

    // ==================== 地图渲染 ====================

    private void renderMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        GameMap map = engine.getMapManager().getCurrentMap();
        Position playerPos = engine.getMapManager().getPlayerPosition();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        for (int y = 0; y < MAP_SIDE_LENGTH; y++) {
            for (int x = 0; x < MAP_SIDE_LENGTH; x++) {
                double px = x * cellSize;
                double py = y * cellSize;
                Position pos = new Position((short) x, (short) y);
                Tile tile = map.getTileAt(pos);

                renderTile(gc, tile, px, py);

                Item item = map.getItemAt(pos);
                if (item != null) {
                    renderItem(gc, item, px, py);
                }

                Monster monster = map.getMonsterAt(pos);
                if (monster != null) {
                    renderMonster(gc, monster, px, py);
                }
            }
        }

        renderPlayer(gc, playerPos);
    }

    private void renderTile(GraphicsContext gc, Tile tile, double px, double py) {
        Image bgImage = resourceManager.getTileImage(tile.getBgResourceId());
        if (bgImage != null) {
            gc.drawImage(bgImage, px, py, cellSize, cellSize);
        } else {
            gc.setFill(Color.web("#f5f5dc"));
            gc.fillRect(px, py, cellSize, cellSize);
        }

        // 背景格没有前景层，上面那层底图就是全部；其余格子各取前景图，缺图时用纯色兜底。
        // Tile 同为封闭类型，这里也是穷尽匹配
        Image fgImage;
        Color fallbackColor;
        switch (tile) {
            case BackGroundTile _ -> {
                return;
            }
            case WallTile wallTile -> {
                fgImage = resourceManager.getTileImage(wallTile.getWallResourceId());
                fallbackColor = switch (wallTile.getWallType()) {
                    case NORMAL, DARK -> Color.web("#4a4a4a");
                    case MAGMA -> Color.web("#ff4500");
                };
            }
            case DoorTile doorTile -> {
                fgImage = resourceManager.getTileImage(doorTile.getDoorResourceId());
                fallbackColor = keyColor(doorTile.getKeyColor());
            }
            case FloorSwitcherTile fsTile -> {
                fgImage = resourceManager.getTileImage(fsTile.getSwitcherResourceId());
                fallbackColor = Color.web("#32cd32");
            }
            case TrickyTile trickyTile -> {
                fgImage = resourceManager.getTileImage(trickyTile.getTrickyResourceId());
                fallbackColor = Color.web("#4a4a4a");
            }
        }

        // 走到这里的四种格子都已给出兜底色，无需再判空
        if (fgImage != null) {
            gc.drawImage(fgImage, px, py, cellSize, cellSize);
        } else {
            gc.setFill(fallbackColor);
            gc.fillRect(px + 0.5, py + 0.5, cellSize - 1, cellSize - 1);
        }
    }

    private void renderItem(GraphicsContext gc, Item item, double px, double py) {
        Image img = resourceManager.getItemImage(item.getItemId());
        if (img != null) {
            gc.drawImage(img, px, py, cellSize, cellSize);
            return;
        }

        // 缺少图片资源时的占位：宝石画成菱形，其余画成圆角方块，颜色按道具语义取。
        // Item 是封闭类型，新增一个道具大类会让这里编译失败，不会静默落到某个兜底色
        switch (item) {
            case AbilityGem gem -> drawGemPlaceholder(gc, px, py, abilityGemColor(gem));
            case Key key -> drawItemBlock(gc, px, py, keyColor(key.getKeyColor()));
            case Portion _ -> drawItemBlock(gc, px, py, Color.web("#ff5a5a"));
            case Equipment _ -> drawItemBlock(gc, px, py, Color.web("#9aa5b1"));
            case GenericItem _ -> drawItemBlock(gc, px, py, Color.web("#ffa500"));
        }
    }

    /** 能力宝石占位色：攻击红 / 防御蓝 / 生命上限黄（黄色宝石美术资源到位前的替代） */
    private Color abilityGemColor(AbilityGem gem) {
        return switch (gem.getEffectedAbilityType()) {
            case ATK -> Color.web("#e74c3c");
            case DEF -> Color.web("#3498db");
            case MAX_HP -> Color.web("#ffd700");
            case HP -> Color.web("#2ecc71");
        };
    }

    /** 钥匙与门共用同一套配色——分开写两份，改了一处忘另一处就会对不上 */
    private static Color keyColor(KeyColor color) {
        return switch (color) {
            case YELLOW -> Color.web("#ffd700");
            case BLUE -> Color.web("#4169e1");
            case RED -> Color.web("#dc143c");
            case GREEN -> Color.web("#2e8b57");
        };
    }

    /** 道具缺图时的占位方块 */
    private void drawItemBlock(GraphicsContext gc, double px, double py, Color color) {
        gc.setFill(color);
        double margin = cellSize * 0.3;
        gc.fillRoundRect(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin, 4, 4);
    }

    /** 用色块画一颗菱形宝石（带描边与高光，便于和普通道具方块区分） */
    private void drawGemPlaceholder(GraphicsContext gc, double px, double py, Color color) {
        double cx = px + cellSize / 2;
        double cy = py + cellSize / 2;
        double r = cellSize * 0.3;
        double[] xs = {cx, cx + r, cx, cx - r};
        double[] ys = {cy - r, cy, cy + r, cy};

        gc.setFill(color);
        gc.fillPolygon(xs, ys, 4);
        gc.setStroke(color.darker());
        gc.setLineWidth(2);
        gc.strokePolygon(xs, ys, 4);

        gc.setFill(color.brighter());
        gc.fillPolygon(
                new double[]{cx, cx + r * 0.45, cx},
                new double[]{cy - r, cy - r * 0.2, cy - r * 0.2}, 3);
    }

    private void renderMonster(GraphicsContext gc, Monster monster, double px, double py) {
        Image img = resourceManager.getMonsterImage(monster.getCharacterId());
        if (img != null) {
            gc.drawImage(img, px, py, cellSize, cellSize);
        } else {
            gc.setFill(Color.web("#cc0000"));
            double margin = cellSize * 0.1;
            gc.fillOval(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin);
        }

        DamageRange range = monster.getCurrentDamageRange();
        Color dmgColor = DamagePalette.colorOf(range, monster.isLethalTo(engine.getPlayerStateManager()));

        String dmgText;
        if (range == DamageRange.OVER_KILL) {
            dmgText = "???";
        } else if (monster.getCurrentDamage() == null) {
            dmgText = "?";
        } else {
            dmgText = ValueFormatter.formatScaled(monster.getCurrentDamage(),
                    engine.getPlayerStateManager().getMaxHP());
        }

        // 右下角对齐 + 全图统一字号（fitFont 仅在极端长度下兜底收缩），保证数字完整落格且大小一致
        double padding = cellSize * 0.05;
        double maxWidth = cellSize - 2 * padding;
        Font dmgFont = painter.fitFont(dmgText, "Consolas",
                cellSize * DAMAGE_FONT_RATIO, cellSize * DAMAGE_FONT_MIN_RATIO, maxWidth);
        double textX = px + cellSize - padding - painter.measureWidth(dmgText, dmgFont);
        double textY = py + cellSize - padding;

        gc.setFont(dmgFont);
        gc.setFill(Color.BLACK);
        gc.fillText(dmgText, textX + 1, textY + 1);
        gc.setFill(dmgColor);
        gc.fillText(dmgText, textX, textY);
    }

    private void renderPlayer(GraphicsContext gc, Position playerPos) {
        if (playerPos == null) return;
        double ppx = playerPos.getX_index() * cellSize;
        double ppy = playerPos.getY_index() * cellSize;

        Direction dir = engine.getPlayerStateManager().getCurrentDirection();
        Image playerImg = resourceManager.getPlayerSprite(dir);

        if (playerImg != null) {
            gc.drawImage(playerImg, ppx, ppy, cellSize, cellSize);
        } else {
            gc.setFill(Color.DODGERBLUE);
            double margin = cellSize * 0.15;
            gc.fillOval(ppx + margin, ppy + margin, cellSize - 2 * margin, cellSize - 2 * margin);

            gc.setFill(Color.WHITE);
            double cx = ppx + cellSize / 2;
            double cy = ppy + cellSize / 2;
            double sz = cellSize * 0.2;
            switch (dir) {
                case UP -> gc.fillPolygon(
                        new double[]{cx, cx - sz, cx + sz},
                        new double[]{cy - sz, cy + sz * 0.5, cy + sz * 0.5}, 3);
                case DOWN -> gc.fillPolygon(
                        new double[]{cx, cx - sz, cx + sz},
                        new double[]{cy + sz, cy - sz * 0.5, cy - sz * 0.5}, 3);
                case LEFT -> gc.fillPolygon(
                        new double[]{cx - sz, cx + sz * 0.5, cx + sz * 0.5},
                        new double[]{cy, cy - sz, cy + sz}, 3);
                case RIGHT -> gc.fillPolygon(
                        new double[]{cx + sz, cx - sz * 0.5, cx - sz * 0.5},
                        new double[]{cy, cy - sz, cy + sz}, 3);
            }
        }
    }

    private void renderTileAndPlayerAt(GraphicsContext gc, Position playerPos) {
        Tile tile = this.engine.getMapManager().getCurrentMap().getTileAt(playerPos);
        renderTile(gc, tile, playerPos.getX_index() * cellSize, playerPos.getY_index() * cellSize);
        renderPlayer(gc, playerPos);
    }
}
