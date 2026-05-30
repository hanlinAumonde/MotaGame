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
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class MotaController {
    @FXML private Canvas mapCanvas;
    @FXML private Label floorLabel;
    @FXML private Label hpLabel;
    @FXML private Label atkLabel;
    @FXML private Label defLabel;
    @FXML private Label goldLabel;
    @FXML private Label yellowKeyLabel;
    @FXML private Label blueKeyLabel;
    @FXML private Label redKeyLabel;
    @FXML private Label messageLabel;

    private GameEngine engine;
    private ResourceManager resourceManager;
    private double cellSize;

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
        switch (result) {
            case BATTLE_LOST -> messageLabel.setText("战斗失败！游戏结束");
            case BATTLE_WON -> messageLabel.setText("战斗胜利！");
            case DOOR_OPENED -> messageLabel.setText("门已打开");
            case DARK_WALL_REVEALED -> messageLabel.setText("发现了暗墙！");
            case FLOOR_SWITCHED -> messageLabel.setText("切换楼层");
            case ITEM_PICKED -> messageLabel.setText("获得道具");
            case BLOCKED, NO_MOVE -> messageLabel.setText("");
            case MOVED -> messageLabel.setText("");
        }
    }

    private void renderAll() {
        renderMap();
        updateStatusPanel();
    }

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

    private void updateStatusPanel() {
        PlayerStateManager player = engine.getPlayerStateManager();
        GameMap map = engine.getMapManager().getCurrentMap();

        floorLabel.setText("楼层: " + map.getFloorNumber() + "F");
        hpLabel.setText("生命: " + player.getStateValue(StateType.HP));
        atkLabel.setText("攻击: " + player.getEffectiveATK());
        defLabel.setText("防御: " + player.getEffectiveDEF());
        goldLabel.setText("金币: " + player.getCurrentGoldAmount());

        yellowKeyLabel.setText("黄:" + player.getKeyCount(KeyColor.YELLOW));
        blueKeyLabel.setText("蓝:" + player.getKeyCount(KeyColor.BLUE));
        redKeyLabel.setText("红:" + player.getKeyCount(KeyColor.RED));
    }
}
