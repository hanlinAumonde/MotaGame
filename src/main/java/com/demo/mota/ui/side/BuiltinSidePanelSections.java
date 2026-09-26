package com.demo.mota.ui.side;

import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillCast;
import com.demo.mota.engine.skill.book.OwnedSkill;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 右侧栏的内置分区：
 * <table border="1">
 *   <caption>内置分区</caption>
 *   <tr><th>id</th><th>内容</th></tr>
 *   <tr><td>{@code skillCombo}</td><td>就绪预设里实际会释放的主动技能（角标为释放回合），没有就绪预设时显示「未就绪」</td></tr>
 *   <tr><td>{@code passiveSkills}</td><td>全部被动技能，关闭的画暗</td></tr>
 *   <tr><td>{@code equipment}</td><td>装备槽，空槽画虚线框</td></tr>
 * </table>
 */
public final class BuiltinSidePanelSections {

    public static final String SKILL_COMBO = "skillCombo";
    public static final String PASSIVE_SKILLS = "passiveSkills";
    public static final String EQUIPMENT = "equipment";

    private static final int COLUMNS = 3;
    private static final double ICON_SIZE = 64;
    private static final double ICON_GAP = 10;
    /** 标题行（含下划线）高度 */
    private static final double TITLE_HEIGHT = 46;
    /** 副标题 / 空状态文字行高度 */
    private static final double LINE_HEIGHT = 30;
    private static final Color TITLE_COLOR = Color.web("#e8f0ff");
    private static final Color ARMED_COLOR = Color.web("#7cfc7c");

    private BuiltinSidePanelSections() {}

    public static void registerAll(BiConsumer<String, SidePanelSection> registrar) {
        registrar.accept(SKILL_COMBO, new SkillComboSection());
        registrar.accept(PASSIVE_SKILLS, new PassiveSkillsSection());
        registrar.accept(EQUIPMENT, new EquipmentSection());
    }

    // ==================== 技能组合 ====================

    private static final class SkillComboSection implements SidePanelSection {

        /** 只取主动技能：与战斗实际结算的是同一批（已剔除失效 / 超出次数的格子） */
        private static List<SkillCast> activeCasts(PlayerStateManager player) {
            return player.getBattleSkillCasts().stream().filter(cast -> !cast.isPassive()).toList();
        }

        @Override
        public double height(SidePanelContext ctx, double width) {
            if (ctx.player().getPresetBook().getArmed() == null) {
                return TITLE_HEIGHT + LINE_HEIGHT;
            }
            List<SkillCast> casts = activeCasts(ctx.player());
            return TITLE_HEIGHT + LINE_HEIGHT + (casts.isEmpty() ? LINE_HEIGHT : gridHeight(casts.size()));
        }

        @Override
        public void render(SidePanelContext ctx, double x, double y, double width) {
            GraphicsContext gc = ctx.gc();
            drawTitle(ctx, "技能组合", x, y, width);
            SkillPreset armed = ctx.player().getPresetBook().getArmed();
            double lineY = y + TITLE_HEIGHT + 22;
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
            if (armed == null) {
                ctx.painter().drawCenteredShadowText(gc, "未激活", x + width / 2, lineY, PanelStyle.DISABLED_COLOR);
                return;
            }
            ctx.painter().drawCenteredShadowText(gc, "● " + armed.getName(), x + width / 2, lineY, ARMED_COLOR);

            List<SkillCast> casts = activeCasts(ctx.player());
            double gridTop = y + TITLE_HEIGHT + LINE_HEIGHT;
            if (casts.isEmpty()) {
                gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
                ctx.painter().drawCenteredShadowText(gc, "全程普攻", x + width / 2, gridTop + 22, PanelStyle.DISABLED_COLOR);
                return;
            }
            for (int i = 0; i < casts.size(); i++) {
                SkillCast cast = casts.get(i);
                double[] cell = cell(x, gridTop, width, i);
                ctx.icons().drawSkillIcon(gc, cast.skill(), cell[0], cell[1], ICON_SIZE, ICON_SIZE);
                drawBadge(gc, ctx.painter(), cast.castRound() == 0 ? "前" : String.valueOf(cast.castRound()),
                        cell[0], cell[1]);
            }
        }
    }

    // ==================== 被动技能 ====================

    private static final class PassiveSkillsSection implements SidePanelSection {

        private static List<OwnedSkill> passives(PlayerStateManager player) {
            return player.getSkillBook().entries().stream().filter(owned -> owned.skill().isPassive()).toList();
        }

        @Override
        public double height(SidePanelContext ctx, double width) {
            List<OwnedSkill> passives = passives(ctx.player());
            return TITLE_HEIGHT + (passives.isEmpty() ? LINE_HEIGHT : gridHeight(passives.size()));
        }

        @Override
        public void render(SidePanelContext ctx, double x, double y, double width) {
            drawTitle(ctx, "被动技能", x, y, width);
            List<OwnedSkill> passives = passives(ctx.player());
            double gridTop = y + TITLE_HEIGHT;
            if (passives.isEmpty()) {
                ctx.gc().setFont(Font.font("SimHei", FontWeight.BOLD, 20));
                ctx.painter().drawCenteredShadowText(ctx.gc(), "无", x + width / 2, gridTop + 22, PanelStyle.DISABLED_COLOR);
                return;
            }
            for (int i = 0; i < passives.size(); i++) {
                double[] cell = cell(x, gridTop, width, i);
                OwnedSkill owned = passives.get(i);
                Skill skill = owned.skill();
                ctx.icons().drawSkillIcon(ctx.gc(), skill, cell[0], cell[1], ICON_SIZE, ICON_SIZE, !owned.isEnabled());
            }
        }
    }

    // ==================== 当前装备 ====================

    private static final class EquipmentSection implements SidePanelSection {

        @Override
        public double height(SidePanelContext ctx, double width) {
            return TITLE_HEIGHT + gridHeight(ctx.player().getEquipSlotCount());
        }

        @Override
        public void render(SidePanelContext ctx, double x, double y, double width) {
            drawTitle(ctx, "当前装备", x, y, width);
            PlayerStateManager player = ctx.player();
            GraphicsContext gc = ctx.gc();
            double gridTop = y + TITLE_HEIGHT;
            for (int i = 0; i < player.getEquipSlotCount(); i++) {
                double[] cell = cell(x, gridTop, width, i);
                Equipment equipment = player.getEquipped(i);
                if (equipment != null) {
                    ctx.icons().drawEquipmentIcon(gc, equipment, cell[0], cell[1], ICON_SIZE, ICON_SIZE);
                } else {
                    gc.setStroke(Color.rgb(170, 182, 255, 0.55));
                    gc.setLineWidth(1.5);
                    gc.setLineDashes(5);
                    gc.strokeRect(cell[0] + 0.5, cell[1] + 0.5, ICON_SIZE - 1, ICON_SIZE - 1);
                    gc.setLineDashes(null);
                }
            }
        }
    }

    // ==================== 工具 ====================

    /** 加粗标题 + 一条细下划线 */
    private static void drawTitle(SidePanelContext ctx, String title, double x, double y, double width) {
        GraphicsContext gc = ctx.gc();
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        ctx.painter().drawCenteredShadowText(gc, title, x + width / 2, y + 28, TITLE_COLOR);
        gc.setStroke(Color.rgb(170, 182, 255, 0.35));
        gc.setLineWidth(1);
        gc.strokeLine(x + 24, y + 38.5, x + width - 24, y + 38.5);
    }

    /** 第 index 个图标的左上角坐标（每行 {@link #COLUMNS} 个，整体水平居中） */
    private static double[] cell(double x, double top, double width, int index) {
        double rowWidth = COLUMNS * ICON_SIZE + (COLUMNS - 1) * ICON_GAP;
        double left = x + (width - rowWidth) / 2;
        return new double[]{
                left + (index % COLUMNS) * (ICON_SIZE + ICON_GAP),
                top + (index / COLUMNS) * (ICON_SIZE + ICON_GAP)
        };
    }

    private static double gridHeight(int count) {
        int rows = Math.max(1, (count + COLUMNS - 1) / COLUMNS);
        return rows * ICON_SIZE + (rows - 1) * ICON_GAP;
    }

    /** 图标左下角的回合角标 */
    private static void drawBadge(GraphicsContext gc, TextPainter painter, String text, double x, double y) {
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRect(x, y + ICON_SIZE - 22, 26, 22);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
        painter.drawCenteredShadowText(gc, text, x + 13, y + ICON_SIZE - 5, Color.web("#ffd95a"));
    }
}
