package com.demo.mota.ui.screen.gamemenu.option;

import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 「游戏选项」相关的两块绘制：右侧的说明预览，以及弹出的次级小窗（参照 img_5）。
 * 位置由游戏菜单的绘制器给出，本类只管画。
 */
public class GameOptionRenderer {

    private static final double POPUP_WIDTH = 220;
    private static final double POPUP_ITEM_HEIGHT = 46;

    private final TextPainter painter;

    public GameOptionRenderer(TextPainter painter) {
        this.painter = painter;
    }

    /** 左侧停在「游戏选项」上时，右侧列出各次级项的说明 */
    public void drawPreview(GraphicsContext gc, GameOptionPopup popup, double x, double y, double w) {
        List<GameOption> options = popup.getOptions();
        double h = 90 + options.size() * 44;
        PanelStyle.drawPanel(gc, x, y, w, h, false);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        painter.drawShadowText(gc, "游戏选项", x + 28, y + 46, PanelStyle.TITLE_COLOR);
        for (int i = 0; i < options.size(); i++) {
            double lineY = y + 92 + i * 44;
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            painter.drawShadowText(gc, options.get(i).getDisplayName(), x + 40, lineY, PanelStyle.SECTION_COLOR);
            gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 19));
            painter.drawShadowText(gc, description(options.get(i)), x + 180, lineY, PanelStyle.HINT_COLOR);
        }
    }

    private static String description(GameOption option) {
        return switch (option) {
            case ITEMS -> "查看持有的钥匙、辅助道具与通用道具";
            case EQUIPMENT -> "打开装备界面（快捷键 Q）";
            case SKILLS -> "打开技能设置界面（快捷键 D）";
            case EQUIPMENT_SETS -> "一键切换已保存的装备套装（快捷键 A）";
        };
    }

    /** 弹出的次级小窗，左上角落在 (x, y) */
    public void drawPopup(GraphicsContext gc, GameOptionPopup popup, double x, double y) {
        List<GameOption> options = popup.getOptions();
        double h = 16 + options.size() * POPUP_ITEM_HEIGHT;

        // 弹窗压在右侧内容之上，先铺一层不透明底，免得下面的文字透出来
        gc.setFill(Color.web("#1d2147"));
        gc.fillRect(x, y, POPUP_WIDTH, h);
        PanelStyle.drawPanel(gc, x, y, POPUP_WIDTH, h, true);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
        for (int i = 0; i < options.size(); i++) {
            double itemY = y + 8 + i * POPUP_ITEM_HEIGHT;
            if (i == popup.getSelectedIndex()) {
                PanelStyle.drawSelection(gc, x + 8, itemY, POPUP_WIDTH - 16, POPUP_ITEM_HEIGHT - 4, true);
            }
            painter.drawShadowText(gc, options.get(i).getDisplayName(), x + 22, itemY + POPUP_ITEM_HEIGHT * 0.66);
        }
    }
}
