package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class MotaController {
    @FXML private Canvas mapCanvas;
    @FXML private Canvas statusCanvas;

    private GameEngine engine;
    private ResourceManager resourceManager;
    private double cellSize;
    private String currentMessage = "";

    @FXML
    public void initialize() {
        engine = GameEngine.getGameEngine();
        resourceManager = ResourceManager.getInstance();
        engine.startGame(1);
        cellSize = mapCanvas.getWidth() / MAP_SIDE_LENGTH;
        renderAll();
    }

    public void handleKeyPress(KeyEvent event) {
        Direction direction = switch (event.getCode()) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };

        if (direction == null) return;

        MoveResult result = engine.handlePlayerMove(direction);
        handleMoveResult(result);
        renderAll();
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
        drawFloorInfo(gc, map, w, h);
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
        drawShadowText(gc, String.valueOf(player.getLevelNumber()), iconX + iconSize + 18, topY + 38);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 28));
        drawShadowText(gc, "级", panelWidth - 42, topY + 38);
    }

    private void drawStats(GraphicsContext gc, PlayerStateManager player) {
        double statsY = 115;
        double statsX = 14;
        double lineHeight = 38;

        String[] labels = {"生命", "攻击", "防御", "金币", "经验"};
        String[] values = {
                player.getStateValue(StateType.HP).toString(),
                player.getEffectiveATK().toString(),
                player.getEffectiveDEF().toString(),
                String.valueOf(player.getCurrentGoldAmount()),
                player.getCurrentExp().toString()
        };

        for (int i = 0; i < labels.length; i++) {
            double ly = statsY + i * lineHeight;

            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            drawShadowText(gc, labels[i], statsX, ly);

            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
            drawShadowText(gc, values[i], statsX + 60, ly);
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
            drawShadowText(gc, countStr, keysX + keyIconSize + 14, ky + keyIconSize * 0.7);

            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            drawShadowText(gc, "个", keysX + keyIconSize + 64, ky + keyIconSize * 0.7);
        }
    }

    private void drawFloorInfo(GraphicsContext gc, GameMap map, double w, double h) {
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        String floorText = "第 " + map.getFloorNumber() + " 层";
        drawShadowText(gc, floorText, 14, h - 25);
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

    private void drawShadowText(GraphicsContext gc, String text, double x, double y) {
        gc.setFill(Color.BLACK);
        gc.fillText(text, x + 2, y + 2);
        gc.setFill(Color.WHITE);
        gc.fillText(text, x, y);
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
        } else {
            gc.setFill(Color.web("#ffa500"));
            double margin = cellSize * 0.3;
            gc.fillRoundRect(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin, 4, 4);
        }
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
        Color dmgColor = switch (range) {
            case NONE -> Color.LIMEGREEN;
            case LOW -> Color.YELLOW;
            case MEDIUM -> Color.ORANGE;
            case HIGH -> Color.RED;
            case DEATH, OVER_KILL -> Color.DARKRED;
        };

        String dmgText = monster.getCurrentDamage() != null ? monster.getCurrentDamage().toString() : "?";
        if (range == DamageRange.OVER_KILL || range == DamageRange.DEATH) {
            dmgText = "???";
        }

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, cellSize * 0.3));
        gc.setFill(Color.BLACK);
        gc.fillText(dmgText, px + cellSize * 0.42 + 1, py + cellSize * 0.95 + 1);
        gc.setFill(dmgColor);
        gc.fillText(dmgText, px + cellSize * 0.42, py + cellSize * 0.95);
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
