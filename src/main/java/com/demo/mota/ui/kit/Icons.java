package com.demo.mota.ui.kit;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.ui.Fonts;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * 技能 / 装备 / 道具图标（原 {@code IconPainter}）：有图摆图，缺图时摆<b>带首字的色块</b>占位——
 * 敌物资料、技能设置、装备、物品栏与右侧栏都用这一份，占位风格保持一致。
 */
public final class Icons {

    private final AssetService assets;

    public Icons(AssetService assets) {
        this.assets = assets;
    }

    public void skill(Sketch sketch, Skill skill, double x, double y, double w, double h) {
        skill(sketch, skill, x, y, w, h, false);
    }

    /** @param dimmed 是否盖一层暗色（未生效的技能） */
    public void skill(Sketch sketch, Skill skill, double x, double y, double w, double h, boolean dimmed) {
        Color top = skill.isActive() ? Color.web("#6a2a2a") : Color.web("#6a4b2a");
        Color bottom = skill.isActive() ? Color.web("#2d1010") : Color.web("#2d1e10");
        icon(sketch, assets.sprite(skill.sprite()), skill.skillName(), top, bottom, Color.web("#ffcf8a"), x, y, w, h);
        if (dimmed) {
            sketch.fill(x, y, w, h, Color.rgb(0, 0, 0, 0.55));
        }
    }

    public void equipment(Sketch sketch, Equipment equipment, double x, double y, double w, double h) {
        icon(sketch, assets.itemImage(equipment.getItemId()), equipment.getItemName(),
                Color.web("#5b6572"), Color.web("#262b33"), Color.web("#e6edf5"), x, y, w, h);
    }

    /** 物品栏里的道具图标；缺图时同样摆带首字的色块 */
    public void item(Sketch sketch, Item item, double x, double y, double w, double h) {
        icon(sketch, assets.itemImage(item.getItemId()), item.getItemName(),
                Color.web("#4b3f7a"), Color.web("#1f1a38"), Color.web("#e8dcff"), x, y, w, h);
    }

    private static void icon(Sketch sketch, Image image, String name, Color top, Color bottom, Color textColor,
                             double x, double y, double w, double h) {
        if (image != null) {
            sketch.image(image, x, y, w, h);
            return;
        }
        sketch.fill(x, y, w, h, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        sketch.outline(x, y, w, h, Palette.BORDER_DIM, 1);
        if (name == null || name.isEmpty()) {
            return;
        }
        double size = Math.min(w, h) * 0.5;
        sketch.centered(name.substring(0, 1), Fonts.bold(size), textColor, x + w / 2, y + h / 2 + size * 0.36);
    }
}
