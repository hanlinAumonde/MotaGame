package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.Item.AbilityGem;
import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.Item.Key;
import com.demo.mota.engine.Item.Portion;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.menu.GameMenu;
import com.demo.mota.engine.menu.MenuCommand;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.DamagePalette;
import com.demo.mota.ui.MenuRenderer;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.ValueFormatter;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class MotaController {
    /**
     * 伤害数字字号（相对格宽）。收缩记法已把长度硬约束在 6 个字符内，
     * 故直接取能整格容纳该上限的最大字号并全图统一，避免同层怪物因位数不同而字号参差。
     */
    private static final double DAMAGE_FONT_RATIO = 0.26;
    /** 兜底字号下限：仅在超出末级单位等极端情况下才会触发收缩 */
    private static final double DAMAGE_FONT_MIN_RATIO = 0.17;

    @FXML private Canvas mapCanvas;
    @FXML private Canvas statusCanvas;
    @FXML private Canvas menuCanvas;

    private GameEngine engine;
    private ResourceManager resourceManager;
    private GameMenu menu;
    private MenuRenderer menuRenderer;
    private double cellSize;
    private String currentMessage = "";

    /** 文本测量与绘制工具（阴影描边、自适应字号、折行） */
    private final TextPainter painter = new TextPainter();

    @FXML
    public void initialize() {
        engine = GameEngine.getGameEngine();
        resourceManager = ResourceManager.getInstance();
        menu = engine.getGameMenu();
        menuRenderer = new MenuRenderer(resourceManager);
        engine.startGame(1);
        cellSize = mapCanvas.getWidth() / MAP_SIDE_LENGTH;
        renderAll();
        renderMenu();
    }

    // ==================== 输入 ====================

    public void handleKeyPress(KeyEvent event) {
        // 菜单打开时独占输入：先把按键翻译成菜单语义，再交给菜单状态机
        if (menu.isOpen()) {
            menu.handle(toMenuCommand(event.getCode()));
            renderMenu();
            return;
        }

        if (event.getCode() == KeyCode.X) {
            menu.open();
            renderMenu();
            return;
        }

        Direction direction = toDirection(event.getCode());
        if (direction == null) return;

        MoveResult result = engine.handlePlayerMove(direction);
        handleMoveResult(result);
        renderAll();
    }

    private static Direction toDirection(KeyCode code) {
        return switch (code) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    /** 按键 → 菜单语义；返回 null 表示该键在菜单中无意义 */
    private static MenuCommand toMenuCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            case X -> MenuCommand.CLOSE;
            default -> null;
        };
    }

    private void handleMoveResult(MoveResult result) {
        String detail = result.getDetail();
        currentMessage = switch (result.getType()) {
            case BATTLE_WON -> "击败 " + detail + "！";
            case BATTLE_LOST -> "败给 " + detail + "！游戏结束";
            case ITEM_PICKED -> "获得 " + detail;
            case DOOR_OPENED -> detail + "已打开";
            case DARK_WALL_REVEALED -> "发现了暗墙！";
            case FLOOR_SWITCHED -> "前往" + detail;
            case BLOCKED, NO_MOVE, MOVED -> "";
        };
    }

    private void renderAll() {
        renderMap();
        renderStatusPanel();
    }

    // ==================== 菜单渲染 ====================

    /** 菜单画布平时隐藏，打开时整屏覆盖游戏画面 */
    private void renderMenu() {
        menuCanvas.setVisible(menu.isOpen());
        if (!menu.isOpen()) return;
        menuRenderer.render(menuCanvas.getGraphicsContext2D(), menu, engine.getPlayerStateManager(),
                menuCanvas.getWidth(), menuCanvas.getHeight());
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

        Image playerSprite = resourceManager.getPlayerSprite(Direction.DOWN.ordinal());
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

        if (tile instanceof BackGroundTile) {
            return;
        }

        Image fgImage = null;
        Color fallbackColor = null;

        if (tile instanceof WallTile wallTile) {
            fgImage = resourceManager.getTileImage(wallTile.getWallResourceId());
            fallbackColor = switch (wallTile.getWallType()) {
                case NORMAL, DARK -> Color.web("#4a4a4a");
                case MAGMA -> Color.web("#ff4500");
            };
        } else if (tile instanceof DoorTile doorTile) {
            fgImage = resourceManager.getTileImage(doorTile.getDoorResourceId());
            fallbackColor = switch (doorTile.getKeyColor()) {
                case YELLOW -> Color.web("#ffd700");
                case BLUE -> Color.web("#4169e1");
                case RED -> Color.web("#dc143c");
                case GREEN -> Color.web("#2e8b57");
            };
        } else if (tile instanceof FloorSwitcherTile fsTile) {
            fgImage = resourceManager.getTileImage(fsTile.getSwitcherResourceId());
            fallbackColor = Color.web("#32cd32");
        } else if (tile instanceof TrickyTile trickyTile) {
            fgImage = resourceManager.getTileImage(trickyTile.getTrickyResourceId());
            fallbackColor = Color.web("#4a4a4a");
        }

        if (fgImage != null) {
            gc.drawImage(fgImage, px, py, cellSize, cellSize);
        } else if (fallbackColor != null) {
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

        // 缺少图片资源时的占位：宝石画成菱形，其余画成圆角方块，颜色按道具语义取
        if (item instanceof AbilityGem gem) {
            drawGemPlaceholder(gc, px, py, abilityGemColor(gem));
        } else {
            gc.setFill(itemFallbackColor(item));
            double margin = cellSize * 0.3;
            gc.fillRoundRect(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin, 4, 4);
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

    private Color itemFallbackColor(Item item) {
        if (item instanceof Key key) {
            return switch (key.getKeyColor()) {
                case YELLOW -> Color.web("#ffd700");
                case BLUE -> Color.web("#4169e1");
                case RED -> Color.web("#dc143c");
                case GREEN -> Color.web("#2e8b57");
            };
        }
        if (item instanceof Portion) return Color.web("#ff5a5a");
        if (item instanceof Equipment) return Color.web("#9aa5b1");
        return Color.web("#ffa500");
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
        Image playerImg = resourceManager.getPlayerSprite(dir.ordinal());

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
}
