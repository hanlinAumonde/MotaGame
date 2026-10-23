package com.demo.mota.world;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.item.AbilityGem;
import com.demo.mota.engine.item.AuxiliaryItem;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.item.Key;
import com.demo.mota.engine.item.Portion;
import com.demo.mota.engine.item.generic.GenericItem;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.DoorTile;
import com.demo.mota.engine.map.tile.FloorSwitcherTile;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.map.tile.WallTile;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import static com.demo.mota.world.WorldGrid.CELL;

/**
 * 缺图时的占位视图：按语义着色的形状节点（原 {@code MapCellPainter} 的纯色 fallback，画法不变）。
 *
 * <p>用节点而不是生成一张图：实体在加载场景的后台线程里生成，那里不能对画布截图，搭节点则是安全的。
 */
final class Placeholders {

    private Placeholders() {
    }

    // ==================== 地块 ====================

    static Node background() {
        return new Rectangle(CELL, CELL, Color.web("#f5f5dc"));
    }

    /** 前景占位（背景格没有前景层，返回 {@code null}） */
    static Node foreground(Tile tile) {
        Color color = switch (tile) {
            case BackGroundTile _ -> null;
            case WallTile wall -> switch (wall.getWallType()) {
                case NORMAL, DARK -> Color.web("#4a4a4a");
                case MAGMA -> Color.web("#ff4500");
            };
            case DoorTile door -> keyColor(door.getKeyColor());
            case FloorSwitcherTile _ -> Color.web("#32cd32");
            case TrickyTile _ -> Color.web("#4a4a4a");
            case RegionGateTile _ -> Color.web("#8a2be2");
        };
        if (color == null) {
            return null;
        }
        Rectangle block = new Rectangle(0.5, 0.5, CELL - 1, CELL - 1);
        block.setFill(color);
        return block;
    }

    // ==================== 道具 ====================

    /** 宝石画成菱形，其余画成圆角方块，颜色按道具语义取。Item 是封闭类型，新增大类会在这里编译失败 */
    static Node item(Item item) {
        return switch (item) {
            case AbilityGem gem -> gem(abilityGemColor(gem));
            case Key key -> block(keyColor(key.getKeyColor()));
            case Portion _ -> block(Color.web("#ff5a5a"));
            case Equipment _ -> block(Color.web("#9aa5b1"));
            case AuxiliaryItem _ -> block(Color.web("#b07cff"));
            case GenericItem _ -> block(Color.web("#ffa500"));
        };
    }

    /** 能力宝石占位色：攻击红 / 防御蓝 / 生命上限黄 / 生命绿 */
    private static Color abilityGemColor(AbilityGem gem) {
        return switch (gem.getEffectedAbilityType()) {
            case ATK -> Color.web("#e74c3c");
            case DEF -> Color.web("#3498db");
            case MAX_HP -> Color.web("#ffd700");
            case HP -> Color.web("#2ecc71");
        };
    }

    /** 钥匙与门共用同一套配色 */
    private static Color keyColor(KeyColor color) {
        return switch (color) {
            case YELLOW -> Color.web("#ffd700");
            case BLUE -> Color.web("#4169e1");
            case RED -> Color.web("#dc143c");
            case GREEN -> Color.web("#2e8b57");
        };
    }

    private static Node block(Color color) {
        double margin = CELL * 0.3;
        Rectangle block = new Rectangle(margin, margin, CELL - 2 * margin, CELL - 2 * margin);
        block.setArcWidth(4);
        block.setArcHeight(4);
        block.setFill(color);
        return block;
    }

    /** 一颗菱形宝石（带描边与高光，便于和普通道具方块区分） */
    private static Node gem(Color color) {
        double c = CELL / 2.0;
        double r = CELL * 0.3;
        Polygon body = new Polygon(c, c - r, c + r, c, c, c + r, c - r, c);
        body.setFill(color);
        body.setStroke(color.darker());
        body.setStrokeWidth(2);
        Polygon shine = new Polygon(c, c - r, c + r * 0.45, c - r * 0.2, c, c - r * 0.2);
        shine.setFill(color.brighter());
        return new Group(body, shine);
    }

    // ==================== 角色 ====================

    static Node monster() {
        double margin = CELL * 0.1;
        return new Circle(CELL / 2.0, CELL / 2.0, CELL / 2.0 - margin, Color.web("#cc0000"));
    }

    /** 主角：蓝色圆 + 指向朝向的白色三角 */
    static Node player(Direction direction) {
        double c = CELL / 2.0;
        double sz = CELL * 0.2;
        Circle body = new Circle(c, c, c - CELL * 0.15, Color.DODGERBLUE);
        Polygon arrow = switch (direction) {
            case UP -> new Polygon(c, c - sz, c - sz, c + sz * 0.5, c + sz, c + sz * 0.5);
            case DOWN -> new Polygon(c, c + sz, c - sz, c - sz * 0.5, c + sz, c - sz * 0.5);
            case LEFT -> new Polygon(c - sz, c, c + sz * 0.5, c - sz, c + sz * 0.5, c + sz);
            case RIGHT -> new Polygon(c + sz, c, c - sz * 0.5, c - sz, c - sz * 0.5, c + sz);
        };
        arrow.setFill(Color.WHITE);
        return new Group(body, arrow);
    }
}
