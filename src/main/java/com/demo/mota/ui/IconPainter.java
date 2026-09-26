package com.demo.mota.ui;

import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.skill.Skill;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 技能 / 装备图标的绘制：有图画图，缺图时画<b>带首字的色块</b>占位——
 * 敌物资料、技能设置、装备界面与右侧栏都用这一份，占位风格保持一致。
 */
public class IconPainter {

    private final ResourceManager resourceManager;
    private final TextPainter painter;

    public IconPainter(ResourceManager resourceManager, TextPainter painter) {
        this.resourceManager = resourceManager;
        this.painter = painter;
    }

    public void drawSkillIcon(GraphicsContext gc, Skill skill, double x, double y, double w, double h) {
        drawSkillIcon(gc, skill, x, y, w, h, false);
    }

    /** @param dimmed 是否画成暗色（未生效的技能） */
    public void drawSkillIcon(GraphicsContext gc, Skill skill, double x, double y, double w, double h, boolean dimmed) {
        Image image = resourceManager.getSkillImage(skill.skillId());
        Color top = skill.isActive() ? Color.web("#6a2a2a") : Color.web("#6a4b2a");
        Color bottom = skill.isActive() ? Color.web("#2d1010") : Color.web("#2d1e10");
        drawIcon(gc, image, skill.skillName(), top, bottom, Color.web("#ffcf8a"), x, y, w, h);
        if (dimmed) {
            gc.setFill(Color.rgb(0, 0, 0, 0.55));
            gc.fillRect(x, y, w, h);
        }
    }

    public void drawEquipmentIcon(GraphicsContext gc, Equipment equipment, double x, double y, double w, double h) {
        Image image = resourceManager.getItemImage(equipment.getItemId());
        drawIcon(gc, image, equipment.getItemName(), Color.web("#5b6572"), Color.web("#262b33"),
                Color.web("#e6edf5"), x, y, w, h);
    }

    private void drawIcon(GraphicsContext gc, Image image, String name, Color top, Color bottom, Color textColor,
                          double x, double y, double w, double h) {
        if (image != null) {
            gc.drawImage(image, x, y, w, h);
            return;
        }
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, top), new Stop(1, bottom)));
        gc.fillRect(x, y, w, h);
        gc.setStroke(PanelStyle.PANEL_BORDER_DIM);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, w, h);
        if (name == null || name.isEmpty()) return;
        double size = Math.min(w, h) * 0.5;
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, size));
        painter.drawCenteredShadowText(gc, name.substring(0, 1), x + w / 2, y + h / 2 + size * 0.36, textColor);
    }
}
