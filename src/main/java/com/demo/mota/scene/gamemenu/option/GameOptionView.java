package com.demo.mota.scene.gamemenu.option;

import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 「游戏选项」相关的两块显示：右侧的说明预览，以及弹出的次级小窗（参照 img_5）。
 * 位置由游戏菜单给出，本类只管摆。
 */
public class GameOptionView {

    private static final double POPUP_WIDTH = 220;
    private static final double POPUP_ITEM_HEIGHT = 46;

    /** 左侧停在「游戏选项」上时，右侧列出各次级项的说明 */
    public void drawPreview(Sketch s, GameOptionPopup popup, double x, double y, double w) {
        List<GameOption> options = popup.getOptions();
        double h = 90 + options.size() * 44;
        s.panel(x, y, w, h, false);
        s.text("游戏选项", Fonts.bold(26), Palette.TITLE, x + 28, y + 46);
        for (int i = 0; i < options.size(); i++) {
            double lineY = y + 92 + i * 44;
            s.text(options.get(i).getDisplayName(), Fonts.bold(22), Palette.SECTION, x + 40, lineY);
            s.text(description(options.get(i)), Fonts.regular(19), Palette.HINT, x + 180, lineY);
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
    public void drawPopup(Sketch s, GameOptionPopup popup, double x, double y) {
        List<GameOption> options = popup.getOptions();
        double h = 16 + options.size() * POPUP_ITEM_HEIGHT;

        // 弹窗压在右侧内容之上，先铺一层不透明底，免得下面的文字透出来
        s.fill(x, y, POPUP_WIDTH, h, Color.web("#1d2147"));
        s.panel(x, y, POPUP_WIDTH, h, true);
        for (int i = 0; i < options.size(); i++) {
            double itemY = y + 8 + i * POPUP_ITEM_HEIGHT;
            if (i == popup.getSelectedIndex()) {
                s.selection(x + 8, itemY, POPUP_WIDTH - 16, POPUP_ITEM_HEIGHT - 4, true);
            }
            s.text(options.get(i).getDisplayName(), Fonts.bold(24), Color.WHITE, x + 22, itemY + POPUP_ITEM_HEIGHT * 0.66);
        }
    }
}
