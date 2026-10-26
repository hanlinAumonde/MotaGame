package com.demo.mota.scene.gamemenu;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.scene.gamemenu.option.GameOptionView;
import com.demo.mota.ui.DamagePalette;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.ValueFormatter;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import com.demo.mota.ui.kit.SkillCards;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 游戏内菜单：把 {@link GameMenuState} 的状态摆成节点，铺满窗口。
 *
 * <p>只读取菜单状态与游戏数据，不修改任何状态。左侧菜单项 + 右侧敌物列表共用一屏
 * （{@link MenuPage#ROOT} 与 {@link MenuPage#MONSTER_LIST} 的区别只在焦点落于哪一侧），
 * 技能详情（{@link MenuPage#MONSTER_SKILL}）则整屏替换。
 */
public class GameMenuView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 236;
    private static final double LEFT_ITEM_HEIGHT = 46;
    private static final double RIGHT_X = PADDING + LEFT_WIDTH + PADDING;

    /** 敌物列表每行高度与行距，以及一屏可见的行数 */
    private static final double ROW_HEIGHT = 140;
    private static final double ROW_GAP = 10;
    private static final int VISIBLE_ROWS = 5;

    private static final Color GOLD_COLOR = Color.web("#ffd95a");
    private static final Color EXP_COLOR = Color.web("#b9ff7a");
    private static final Color PLAIN_MONSTER_COLOR = Color.web("#9aa5b1");

    private final AssetService assets;
    private final SkillCards cards;
    private final GameOptionView optionView = new GameOptionView();

    public GameMenuView(AssetService assets) {
        this.assets = assets;
        this.cards = new SkillCards(new Icons(assets));
    }

    public void draw(Sketch s, GameMenuState menu, PlayerStateManager player) {
        s.screen(WIDTH, HEIGHT);
        switch (menu.getPage()) {
            case MONSTER_SKILL -> drawSkillPage(s, menu);
            case ROOT, MONSTER_LIST, OPTIONS -> {
                drawLeftPanel(s, menu);
                switch (menu.getSelectedEntry()) {
                    case MONSTER_BOOK -> {
                        if (menu.isMonsterBookUnlocked()) {
                            drawMonsterList(s, menu, player);
                        } else {
                            drawMessagePanel(s, "尚未获得怪物手册", "拾取「怪物手册」后才能在这里查看本层敌物的情报");
                        }
                    }
                    case GAME_OPTIONS -> optionView.drawPreview(s, menu.getOptionPopup(),
                            RIGHT_X, PADDING, WIDTH - RIGHT_X - PADDING);
                    case SAVE_GAME -> drawMessagePanel(s, "存档",
                            "把当前进度写入一个档位；切换楼层时还会自动写入「自动存档」");
                    case LOAD_GAME -> drawMessagePanel(s, "读档", "从档位读取进度，当前这一局未保存的进度会丢失");
                }
                if (menu.getPage() == MenuPage.OPTIONS) {
                    // 弹窗贴在左侧「游戏选项」一项的右下方（参照 img_5）
                    double entryY = PADDING + 10 + menu.getSelectedEntryIndex() * LEFT_ITEM_HEIGHT;
                    optionView.drawPopup(s, menu.getOptionPopup(),
                            PADDING + LEFT_WIDTH - 40, entryY + LEFT_ITEM_HEIGHT - 6);
                }
            }
        }
    }

    // ==================== 左侧菜单项 ====================

    private void drawLeftPanel(Sketch s, GameMenuState menu) {
        List<MenuEntry> entries = menu.getEntries();
        double boxH = 20 + entries.size() * LEFT_ITEM_HEIGHT;
        boolean focused = menu.getPage() == MenuPage.ROOT;
        s.panel(PADDING, PADDING, LEFT_WIDTH, boxH, focused);

        for (int i = 0; i < entries.size(); i++) {
            double itemY = PADDING + 10 + i * LEFT_ITEM_HEIGHT;
            if (i == menu.getSelectedEntryIndex()) {
                // 焦点移到右侧内容后，左侧选中项保留一个暗一些的高亮，提示所处分类
                s.selection(PADDING + 8, itemY, LEFT_WIDTH - 16, LEFT_ITEM_HEIGHT - 4, focused);
            }
            // 未解锁的项（如没拿到怪物手册时的「敌物资料」）照常可选，但画灰
            Color color = menu.isEntryEnabled(entries.get(i)) ? Color.WHITE : Palette.DISABLED;
            s.text(entries.get(i).getDisplayName(), Fonts.bold(26), color,
                    PADDING + 22, itemY + LEFT_ITEM_HEIGHT * 0.68);
        }

        double infoY = PADDING + boxH + 14;
        s.panel(PADDING, infoY, LEFT_WIDTH, 92, false);
        s.text("当前楼层", Fonts.bold(22), Color.WHITE, PADDING + 18, infoY + 34);
        s.text("第 " + menu.getFloorNumber() + " 层", Fonts.mono(28), Color.WHITE, PADDING + 18, infoY + 74);

        double hintY = infoY + 92 + 14;
        s.panel(PADDING, hintY, LEFT_WIDTH, 150, false);
        String[] hints = {"↑ ↓ / W S   选择", "Enter / Space   确认", "Esc   返回上一层", "X   关闭菜单"};
        for (int i = 0; i < hints.length; i++) {
            s.text(hints[i], Fonts.regular(17), Palette.HINT, PADDING + 16, hintY + 34 + i * 30);
        }

        drawNotice(s, menu, PADDING, hintY + 150 + 14, LEFT_WIDTH);
    }

    /** 最近一次操作的提示（如「尚未获得怪物手册」），没有时不显示 */
    private void drawNotice(Sketch s, GameMenuState menu, double x, double y, double w) {
        String notice = menu.getNotice();
        if (notice == null || notice.isEmpty()) return;
        s.panel(x, y, w, 56, false);
        s.fitted(notice, Fonts::bold, 19, 12, w - 28, Palette.NOTICE, x + 14, y + 36);
    }

    /** 右侧单块提示面板（内容不可用时的占位） */
    private void drawMessagePanel(Sketch s, String title, String detail) {
        double w = WIDTH - RIGHT_X - PADDING;
        s.panel(RIGHT_X, PADDING, w, 130, false);
        s.text(title, Fonts.bold(26), PLAIN_MONSTER_COLOR, RIGHT_X + 28, PADDING + 52);
        s.text(detail, Fonts.regular(19), Palette.HINT, RIGHT_X + 28, PADDING + 96);
    }

    // ==================== 右侧敌物列表 ====================

    private void drawMonsterList(Sketch s, GameMenuState menu, PlayerStateManager player) {
        double listX = RIGHT_X;
        double listW = WIDTH - RIGHT_X - PADDING;
        List<Monster> monsters = menu.getMonsters();

        if (monsters.isEmpty()) {
            s.panel(listX, PADDING, listW, 110, false);
            s.text("本层没有敌物", Fonts.bold(26), PLAIN_MONSTER_COLOR, listX + 28, PADDING + 66);
            return;
        }

        boolean listFocused = menu.getPage() == MenuPage.MONSTER_LIST;
        int selected = menu.getSelectedMonsterIndex();
        // 选中项越过可视区底部时才滚动，保证选中行始终可见
        int first = Math.max(0, Math.min(selected - VISIBLE_ROWS + 1, monsters.size() - VISIBLE_ROWS));
        int last = Math.min(monsters.size(), first + VISIBLE_ROWS);

        for (int i = first; i < last; i++) {
            double rowY = PADDING + 8 + (i - first) * (ROW_HEIGHT + ROW_GAP);
            drawMonsterRow(s, monsters.get(i), player, listX, rowY, listW, listFocused && i == selected);
        }
        if (first > 0) {
            s.centered("▲", Fonts.bold(20), Palette.HINT, listX + listW / 2, PADDING + 4);
        }
        if (last < monsters.size()) {
            s.centered("▼", Fonts.bold(20), Palette.HINT, listX + listW / 2, HEIGHT - 10);
        }
    }

    private void drawMonsterRow(Sketch s, Monster monster, PlayerStateManager player,
                                double x, double y, double w, boolean selected) {
        s.panel(x, y, w, ROW_HEIGHT, selected);
        if (selected) {
            s.fill(x, y, w, ROW_HEIGHT, Color.rgb(255, 255, 255, 0.20));
        }

        // --- 头像与回合数 ---
        double iconSize = 60;
        double iconCenterX = x + 18 + iconSize / 2;
        Image icon = assets.monsterImage(monster.getCharacterId());
        if (icon != null) {
            s.image(icon, x + 18, y + 14, iconSize, iconSize);
        } else {
            s.add(new Circle(iconCenterX, y + 14 + iconSize / 2, iconSize / 2, Color.web("#cc0000")));
        }
        s.centered("回合数", Fonts.regular(15), Palette.HINT, iconCenterX, y + 96);
        s.centered(roundsText(monster), Fonts.mono(22), Palette.VALUE, iconCenterX, y + 124);

        // --- 名称与技能标签 ---
        double nameCenterX = x + 190;
        // 名称栏宽度有限（右侧紧接属性网格），过长的名字按可用宽度缩字号
        Font nameFont = s.metrics().fit(monster.getCharacterName(), Fonts::bold, 26, 16, 220);
        s.centered(monster.getCharacterName(), nameFont, Color.WHITE, nameCenterX, y + 52);
        drawSkillTags(s, monster, nameCenterX, y + 84);

        // --- 属性网格：两列三行 ---
        GameNumber maxHp = player.getMaxHP();
        double col1Label = x + 320;
        double col1Value = x + 470;
        double col2Label = x + 520;
        double col2Value = x + w - 26;
        double[] lineY = {y + 46, y + 88, y + 128};

        drawStat(s, "生命", ValueFormatter.formatScaled(monster.getStateValue(StateType.HP), maxHp),
                col1Label, col1Value, lineY[0], Palette.VALUE);
        drawStat(s, "攻击", ValueFormatter.formatScaled(monster.getStateValue(StateType.ATK), maxHp),
                col2Label, col2Value, lineY[0], Palette.VALUE);
        drawStat(s, "防御", ValueFormatter.formatScaled(monster.getStateValue(StateType.DEF), maxHp),
                col1Label, col1Value, lineY[1], Palette.VALUE);
        drawStat(s, "伤害", damageText(monster, maxHp), col2Label, col2Value, lineY[1],
                DamagePalette.colorOnDark(monster.getCurrentDamageRange(), monster.isLethalTo(player)));
        drawStat(s, "金币", String.valueOf(monster.getGoldReward()), col1Label, col1Value, lineY[2], GOLD_COLOR);
        drawStat(s, "经验", ValueFormatter.formatScaled(monster.getExperienceReward(), maxHp),
                col2Label, col2Value, lineY[2], EXP_COLOR);
    }

    /** 一格「绿色标签 + 右对齐数值」，数值过长时自动缩字号 */
    private static void drawStat(Sketch s, String label, String value,
                                 double labelX, double valueRightX, double baselineY, Color valueColor) {
        s.text(label, Fonts.bold(22), Palette.LABEL, labelX, baselineY);
        double maxWidth = valueRightX - (labelX + 70);
        s.right(value, s.metrics().fit(value, Fonts::mono, 22, 14, maxWidth), valueColor, valueRightX, baselineY);
    }

    /** 技能名标签；没有技能的怪物标注「普通」，与原版手册保持一致 */
    private static void drawSkillTags(Sketch s, Monster monster, double centerX, double baselineY) {
        if (!monster.hasSkills()) {
            s.centered("普通", Fonts.bold(16), PLAIN_MONSTER_COLOR, centerX, baselineY);
            return;
        }
        List<Skill> skills = monster.getSkills();
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            s.centered(skill.skillName(), Fonts.bold(16), skillColor(skill), centerX, baselineY + i * 22);
        }
    }

    private static Color skillColor(Skill skill) {
        return switch (skill.skillType()) {
            case PASSIVE -> Palette.PASSIVE_SKILL;
            case ACTIVE -> Palette.ACTIVE_SKILL;
        };
    }

    private static String roundsText(Monster monster) {
        return monster.getCurrentDamageRange() == DamageRange.OVER_KILL
                ? "-" : String.valueOf(monster.getCurrentRounds());
    }

    private static String damageText(Monster monster, GameNumber maxHp) {
        if (monster.getCurrentDamageRange() == DamageRange.OVER_KILL) {
            return "???";
        }
        if (monster.getCurrentDamage() == null) {
            return "?";
        }
        return ValueFormatter.formatScaled(monster.getCurrentDamage(), maxHp);
    }

    // ==================== 技能详情页 ====================

    private void drawSkillPage(Sketch s, GameMenuState menu) {
        Monster monster = menu.getSelectedMonster();
        if (monster == null) return;

        // 标题：怪物名居中，右侧跟一个小头像
        Font titleFont = Fonts.bold(36);
        double nameWidth = s.width(monster.getCharacterName(), titleFont);
        s.centered(monster.getCharacterName(), titleFont, Palette.TITLE, WIDTH / 2, 66);
        Image icon = assets.monsterImage(monster.getCharacterId());
        if (icon != null) {
            s.image(icon, WIDTH / 2 + nameWidth / 2 + 16, 30, 44, 44);
        }
        s.text("技能列表：", Fonts.bold(22), Palette.SECTION, 40, 120);

        double cardX = 40;
        double cardW = WIDTH - 80;
        double cardY = 140;
        List<Skill> skills = monster.getSkills();
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            List<String> lines = cards.layout(skill, cardW);
            double cardH = cards.height(lines);
            if (cardY + cardH > HEIGHT - 60) break;
            cards.card(s, skill, lines, cardX, cardY, cardW, cardH, i == menu.getSelectedSkillIndex(), null, false);
            cardY += cardH + 12;
        }

        s.text("↑ ↓ 切换技能    Esc 返回敌物列表    X 关闭菜单", Fonts.regular(17), Palette.HINT, 40, HEIGHT - 28);
    }
}
