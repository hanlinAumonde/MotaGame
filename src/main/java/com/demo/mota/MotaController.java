package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.event.MoveResult;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Map;

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
    private double cellSize;

    @FXML
    public void initialize() {
        engine = GameEngine.getGameEngine();
        engine.startGame(1);
        cellSize = mapCanvas.getWidth() / MAP_SIDE_LENGTH;
        renderAll();
    }

    /**
     * 由 MotaApplication 调用，绑定键盘事件
     */
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

    /**
     * 重新渲染地图和状态面板
     */
    private void renderAll() {
        renderMap();
        updateStatusPanel();
    }

    private void renderMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        GameMap map = engine.getMapManager().getCurrentMap();
        Position playerPos = engine.getMapManager().getPlayerPosition();

        // 清空画布
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        for (int y = 0; y < MAP_SIDE_LENGTH; y++) {
            for (int x = 0; x < MAP_SIDE_LENGTH; x++) {
                double px = x * cellSize;
                double py = y * cellSize;
                Position pos = new Position((short) x, (short) y);
                Tile tile = map.getTileAt(pos);

                // 渲染 Tile 底层
                renderTile(gc, tile, px, py);

                // 渲染道具
                if (map.getItemAt(pos) != null) {
                    renderItem(gc, px, py);
                }

                // 渲染怪物
                Monster monster = map.getMonsterAt(pos);
                if (monster != null) {
                    renderMonster(gc, monster, px, py);
                }
            }
        }

        // 渲染玩家
        if (playerPos != null) {
            double ppx = playerPos.getX_index() * cellSize;
            double ppy = playerPos.getY_index() * cellSize;
            gc.setFill(Color.DODGERBLUE);
            double margin = cellSize * 0.15;
            gc.fillOval(ppx + margin, ppy + margin, cellSize - 2 * margin, cellSize - 2 * margin);

            // 玩家朝向指示（小三角）
            gc.setFill(Color.WHITE);
            double cx = ppx + cellSize / 2;
            double cy = ppy + cellSize / 2;
            double sz = cellSize * 0.2;
            Direction dir = engine.getPlayerStateManager().getCurrentDirection();
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

    private void renderTile(GraphicsContext gc, Tile tile, double px, double py) {
        if (tile instanceof BackGroundTile) {
            gc.setFill(Color.web("#f5f5dc")); // 米色地板
        } else if (tile instanceof WallTile wallTile) {
            gc.setFill(switch (wallTile.getWallType()) {
                case NORMAL -> Color.web("#4a4a4a");
                case MAGMA -> Color.web("#ff4500");
                case DARK -> Color.web("#4a4a4a"); // 暗墙外观和普通墙相同
            });
        } else if (tile instanceof DoorTile doorTile) {
            gc.setFill(switch (doorTile.getKeyColor()) {
                case YELLOW -> Color.web("#ffd700");
                case BLUE -> Color.web("#4169e1");
                case RED -> Color.web("#dc143c");
                case GREEN -> Color.web("#2e8b57");
            });
        } else if (tile instanceof FloorSwitcherTile) {
            gc.setFill(Color.web("#32cd32")); // 楼梯 - 绿色
        } else if (tile instanceof TrickyTile) {
            gc.setFill(Color.web("#4a4a4a")); // 机关墙外观和普通墙相同
        } else {
            gc.setFill(Color.BLACK);
        }

        gc.fillRect(px + 0.5, py + 0.5, cellSize - 1, cellSize - 1);

        // 门上画锁孔标记
        if (tile instanceof DoorTile) {
            gc.setFill(Color.WHITE);
            double lockSize = cellSize * 0.2;
            gc.fillOval(px + cellSize / 2 - lockSize / 2, py + cellSize / 2 - lockSize / 2, lockSize, lockSize);
        }

        // 楼梯画箭头
        if (tile instanceof FloorSwitcherTile) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(cellSize * 0.6));
            gc.fillText("↑", px + cellSize * 0.25, py + cellSize * 0.75);
        }
    }

    private void renderItem(GraphicsContext gc, double px, double py) {
        gc.setFill(Color.web("#ffa500"));
        double margin = cellSize * 0.3;
        gc.fillRoundRect(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin, 4, 4);
    }

    private void renderMonster(GraphicsContext gc, Monster monster, double px, double py) {
        // 怪物用红色圆表示
        gc.setFill(Color.web("#cc0000"));
        double margin = cellSize * 0.1;
        gc.fillOval(px + margin, py + margin, cellSize - 2 * margin, cellSize - 2 * margin);

        // 右下角显示伤害值
        DamageRange range = monster.getCurrentDamageRange();
        gc.setFill(switch (range) {
            case NONE -> Color.LIMEGREEN;
            case LOW -> Color.YELLOW;
            case MEDIUM -> Color.ORANGE;
            case HIGH -> Color.RED;
            case DEATH, OVER_KILL -> Color.DARKRED;
        });
        gc.setFont(Font.font(cellSize * 0.35));
        String dmgText = monster.getCurrentDamage() != null ? monster.getCurrentDamage().toString() : "?";
        if (range == DamageRange.OVER_KILL || range == DamageRange.DEATH) {
            dmgText = "X";
        }
        gc.fillText(dmgText, px + cellSize * 0.45, py + cellSize * 0.95);
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
