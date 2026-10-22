package com.demo.mota.ui.screen.game.map;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.CharacterSprite;
import com.demo.mota.engine.item.AbilityGem;
import com.demo.mota.engine.item.AuxiliaryItem;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.item.generic.GenericItem;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.item.Key;
import com.demo.mota.engine.item.Portion;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.DamagePalette;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.ValueFormatter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 地图单格绘制：精灵图优先 + 纯色 fallback，外加怪物的伤害数字。
 *
 * <p>一律画在<b>单格大小的局部坐标</b>里（左上角为原点）——每个地图元素各自一块画布，
 * 由 {@link EntityMapView} 挂到对应的实体上，因此这里不关心它在地图的哪一格。
 */
public final class MapCellPainter {
    /**
     * 伤害数字字号（相对格宽）。收缩记法已把长度硬约束在 6 个字符内，
     * 故直接取能整格容纳该上限的最大字号并全图统一，避免同层怪物因位数不同而字号参差。
     */
    private static final double DAMAGE_FONT_RATIO = 0.26;
    /** 兜底字号下限：仅在超出末级单位等极端情况下才会触发收缩 */
    private static final double DAMAGE_FONT_MIN_RATIO = 0.17;

    private final AssetService assets;
    private final double cellSize;
    /** 文本测量（伤害数字的自适应字号与右对齐） */
    private final TextPainter painter = new TextPainter();

    public MapCellPainter(AssetService assets, double cellSize) {
        this.assets = assets;
        this.cellSize = cellSize;
    }

    public double cellSize() {
        return cellSize;
    }

    // ==================== 地块 ====================

    public void paintTile(GraphicsContext gc, Tile tile) {
        Image bgImage = assets.sprite(tile.getBgResourceId());
        if (bgImage != null) {
            gc.drawImage(bgImage, 0, 0, cellSize, cellSize);
        } else {
            gc.setFill(Color.web("#f5f5dc"));
            gc.fillRect(0, 0, cellSize, cellSize);
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
                fgImage = assets.sprite(wallTile.getWallResourceId());
                fallbackColor = switch (wallTile.getWallType()) {
                    case NORMAL, DARK -> Color.web("#4a4a4a");
                    case MAGMA -> Color.web("#ff4500");
                };
            }
            case DoorTile doorTile -> {
                fgImage = assets.sprite(doorTile.getDoorResourceId());
                fallbackColor = keyColor(doorTile.getKeyColor());
            }
            case FloorSwitcherTile fsTile -> {
                fgImage = assets.sprite(fsTile.getSwitcherResourceId());
                fallbackColor = Color.web("#32cd32");
            }
            case TrickyTile trickyTile -> {
                fgImage = assets.sprite(trickyTile.getTrickyResourceId());
                fallbackColor = Color.web("#4a4a4a");
            }
            case RegionGateTile gateTile -> {
                fgImage = assets.sprite(gateTile.getGateResourceId());
                fallbackColor = Color.web("#8a2be2");
            }
        }

        // 走到这里的五种格子都已给出兜底色，无需再判空
        if (fgImage != null) {
            gc.drawImage(fgImage, 0, 0, cellSize, cellSize);
        } else {
            gc.setFill(fallbackColor);
            gc.fillRect(0.5, 0.5, cellSize - 1, cellSize - 1);
        }
    }

    // ==================== 道具 ====================

    public void paintItem(GraphicsContext gc, Item item) {
        Image img = assets.itemImage(item.getItemId());
        if (img != null) {
            gc.drawImage(img, 0, 0, cellSize, cellSize);
            return;
        }

        // 缺少图片资源时的占位：宝石画成菱形，其余画成圆角方块，颜色按道具语义取。
        // Item 是封闭类型，新增一个道具大类会让这里编译失败，不会静默落到某个兜底色
        switch (item) {
            case AbilityGem gem -> drawGemPlaceholder(gc, abilityGemColor(gem));
            case Key key -> drawItemBlock(gc, keyColor(key.getKeyColor()));
            case Portion _ -> drawItemBlock(gc, Color.web("#ff5a5a"));
            case Equipment _ -> drawItemBlock(gc, Color.web("#9aa5b1"));
            case AuxiliaryItem _ -> drawItemBlock(gc, Color.web("#b07cff"));
            case GenericItem _ -> drawItemBlock(gc, Color.web("#ffa500"));
        }
    }

    /** 能力宝石占位色：攻击红 / 防御蓝 / 生命上限黄（黄色宝石美术资源到位前的替代） */
    private static Color abilityGemColor(AbilityGem gem) {
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
    private void drawItemBlock(GraphicsContext gc, Color color) {
        gc.setFill(color);
        double margin = cellSize * 0.3;
        gc.fillRoundRect(margin, margin, cellSize - 2 * margin, cellSize - 2 * margin, 4, 4);
    }

    /** 用色块画一颗菱形宝石（带描边与高光，便于和普通道具方块区分） */
    private void drawGemPlaceholder(GraphicsContext gc, Color color) {
        double cx = cellSize / 2;
        double cy = cellSize / 2;
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

    // ==================== 怪物 ====================

    /** 怪物本体 + 右下角的伤害数字（伤害由 {@link #damageLabel} 给出，调用方据此判断要不要重画） */
    public void paintMonster(GraphicsContext gc, Monster monster, DamageLabel label) {
        Image img = assets.monsterImage(monster.getCharacterId());
        if (img != null) {
            gc.drawImage(img, 0, 0, cellSize, cellSize);
        } else {
            gc.setFill(Color.web("#cc0000"));
            double margin = cellSize * 0.1;
            gc.fillOval(margin, margin, cellSize - 2 * margin, cellSize - 2 * margin);
        }

        // 右下角对齐 + 全图统一字号（fitFont 仅在极端长度下兜底收缩），保证数字完整落格且大小一致
        double padding = cellSize * 0.05;
        double maxWidth = cellSize - 2 * padding;
        Font dmgFont = painter.fitFont(label.text(), Fonts::mono,
                cellSize * DAMAGE_FONT_RATIO, cellSize * DAMAGE_FONT_MIN_RATIO, maxWidth);
        double textX = cellSize - padding - painter.measureWidth(label.text(), dmgFont);
        double textY = cellSize - padding;

        gc.setFont(dmgFont);
        gc.setFill(Color.BLACK);
        gc.fillText(label.text(), textX + 1, textY + 1);
        gc.setFill(label.color());
        gc.fillText(label.text(), textX, textY);
    }

    /** 怪物此刻应显示的伤害数字：文字 + 颜色（打不动显示 "???"，会被打死时强制标红） */
    public DamageLabel damageLabel(Monster monster, PlayerStateManager player) {
        DamageRange range = monster.getCurrentDamageRange();
        Color color = DamagePalette.colorOf(range, monster.isLethalTo(player));

        String text;
        if (range == DamageRange.OVER_KILL) {
            text = "???";
        } else if (monster.getCurrentDamage() == null) {
            text = "?";
        } else {
            text = ValueFormatter.formatScaled(monster.getCurrentDamage(), player.getMaxHP());
        }
        return new DamageLabel(text, color);
    }

    /** 一只怪物显示的伤害数字；值相等即不必重画 */
    public record DamageLabel(String text, Color color) {}

    // ==================== 玩家 ====================

    /** 主角的行走图；没有切出来时为 {@code null}（绘制走纯色 fallback，也就没有走路帧可播） */
    public CharacterSprite playerSprite() {
        return assets.character(AssetService.PLAYER);
    }

    /**
     * 主角某朝向的一帧：{@code walkIndex} 是行走循环里的下标（按帧数取模，见 {@link CharacterSprite#walkFrame}），
     * {@link WalkCycle#IDLE} 表示静止帧。
     */
    public void paintPlayer(GraphicsContext gc, Direction dir, int walkIndex) {
        CharacterSprite sprite = playerSprite();
        Image playerImg = sprite == null ? null
                : walkIndex == WalkCycle.IDLE ? sprite.idle(dir) : sprite.walkFrame(dir, walkIndex);
        if (playerImg != null) {
            gc.drawImage(playerImg, 0, 0, cellSize, cellSize);
            return;
        }

        gc.setFill(Color.DODGERBLUE);
        double margin = cellSize * 0.15;
        gc.fillOval(margin, margin, cellSize - 2 * margin, cellSize - 2 * margin);

        gc.setFill(Color.WHITE);
        double cx = cellSize / 2;
        double cy = cellSize / 2;
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
