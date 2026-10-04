package com.demo.mota.ui.screen.gamemenu;

import com.demo.mota.engine.Item.AuxiliaryType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.screen.gamemenu.option.GameOptionPopup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏内菜单的状态机（按 X 呼出）。
 *
 * <p>只负责「当前停在哪一层、选中了第几项」，不涉及任何绘制；
 * 渲染由 UI 层按这些状态读取。层级关系见 {@link MenuPage}：
 * <pre>
 *   ROOT ─┬─(敌物资料)─▶ MONSTER_LIST ──确认（该怪物有技能时）──▶ MONSTER_SKILL
 *         └─(游戏选项)─▶ OPTIONS ──(物品栏 / 角色装备 / 角色技能)──▶ 关闭菜单并请求改开对应界面（{@link #takeHandoff}）
 * </pre>
 *
 * <p>敌物列表在每次打开菜单时按当前楼层重新采样：同一种怪物只保留一条，
 * 顺序按其在地图上的位置（先上后下、先左后右）排定，保证展示稳定。
 * 「敌物资料」需持有怪物手册（{@link AuxiliaryType#MONSTER_BOOK}）才能进入。
 */
public class GameMenuState {
    private final MapManager mapManager;
    private final PlayerStateManager player;

    private boolean open;
    private MenuPage page = MenuPage.ROOT;

    private int entryIndex;
    private int monsterIndex;
    private int skillIndex;

    /** 「游戏选项」弹出小窗的选择状态 */
    private final GameOptionPopup optionPopup = new GameOptionPopup();

    /** 当前楼层去重后的怪物列表快照，打开菜单时刷新 */
    private List<Monster> monsters = List.of();

    /** 最近一次操作留给玩家的提示（如「尚未获得怪物手册」），下一次操作即清除 */
    private String notice;

    /** 选中某个游戏选项后要迁往的阶段，由 {@link GameMenuScreen} 取走 */
    private GamePhase pendingHandoff;

    public GameMenuState(MapManager mapManager, PlayerStateManager player) {
        this.mapManager = mapManager;
        this.player = player;
    }

    // ==================== 开关 ====================

    public void open() {
        this.open = true;
        this.page = MenuPage.ROOT;
        this.entryIndex = 0;
        this.monsterIndex = 0;
        this.skillIndex = 0;
        this.optionPopup.reset();
        this.notice = null;
        this.pendingHandoff = null;
        refreshMonsters();
    }

    public void close() {
        this.open = false;
        this.monsters = List.of();
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * 取走并清除待迁往的阶段。菜单因选中了某个游戏选项而关闭时非空；
     * 其余情况下关闭（Esc 退到底）为 {@code null}，即回到游戏。
     */
    public GamePhase takeHandoff() {
        GamePhase handoff = pendingHandoff;
        pendingHandoff = null;
        return handoff;
    }

    // ==================== 输入处理 ====================

    /**
     * 处理一次菜单操作。
     *
     * @return 是否消费了该操作（菜单未打开时恒为 false，由调用方继续按游戏内逻辑处理）
     */
    public boolean handle(MenuCommand command) {
        if (!open || command == null) {
            return false;
        }
        notice = null;
        switch (command) {
            case CLOSE -> close();
            case BACK -> back();
            case CONFIRM -> confirm();
            case UP -> move(-1);
            case DOWN -> move(1);
            default -> { /* 本菜单不响应左右与清除 */ }
        }
        return true;
    }

    private void back() {
        switch (page) {
            case ROOT -> close();
            case MONSTER_LIST, OPTIONS -> page = MenuPage.ROOT;
            case MONSTER_SKILL -> page = MenuPage.MONSTER_LIST;
        }
    }

    private void confirm() {
        switch (page) {
            case ROOT -> confirmEntry();
            // 该怪物有技能时再深入一层，否则停留在列表
            case MONSTER_LIST -> {
                Monster monster = getSelectedMonster();
                if (monster != null && monster.hasSkills()) {
                    page = MenuPage.MONSTER_SKILL;
                    skillIndex = 0;
                }
            }
            case MONSTER_SKILL -> { /* 技能详情已是最内层 */ }
            case OPTIONS -> {
                // 每个游戏选项都是独立界面：关掉本菜单，由 GameMenuScreen 迁往对应阶段
                pendingHandoff = optionPopup.getSelected().getHandoff();
                close();
            }
        }
    }

    private void confirmEntry() {
        switch (getSelectedEntry()) {
            // 进入右侧内容，并默认选中第一项
            case MONSTER_BOOK -> {
                if (!isMonsterBookUnlocked()) {
                    notice = "尚未获得怪物手册";
                } else if (!monsters.isEmpty()) {
                    page = MenuPage.MONSTER_LIST;
                    monsterIndex = 0;
                }
            }
            case GAME_OPTIONS -> {
                page = MenuPage.OPTIONS;
                optionPopup.reset();
            }
        }
    }

    private void move(int delta) {
        switch (page) {
            case ROOT -> entryIndex = wrap(entryIndex + delta, MenuEntry.values().length);
            case MONSTER_LIST -> monsterIndex = wrap(monsterIndex + delta, monsters.size());
            case MONSTER_SKILL -> {
                Monster monster = getSelectedMonster();
                int size = monster == null ? 0 : monster.getSkills().size();
                skillIndex = wrap(skillIndex + delta, size);
            }
            case OPTIONS -> optionPopup.move(delta);
        }
    }

    /** 循环选择：越过首尾时绕回另一端；列表为空时固定停在 0 */
    private static int wrap(int index, int size) {
        if (size <= 0) return 0;
        return ((index % size) + size) % size;
    }

    // ==================== 状态查询（供渲染层读取） ====================

    public MenuPage getPage() {
        return page;
    }

    public String getNotice() {
        return notice;
    }

    public List<MenuEntry> getEntries() {
        return List.of(MenuEntry.values());
    }

    public int getSelectedEntryIndex() {
        return entryIndex;
    }

    public MenuEntry getSelectedEntry() {
        return MenuEntry.values()[entryIndex];
    }

    /** 某个菜单项此刻是否可进入（未解锁的画灰） */
    public boolean isEntryEnabled(MenuEntry entry) {
        return entry != MenuEntry.MONSTER_BOOK || isMonsterBookUnlocked();
    }

    /** 是否持有怪物手册 */
    public boolean isMonsterBookUnlocked() {
        return player.hasAuxiliary(AuxiliaryType.MONSTER_BOOK);
    }

    public List<Monster> getMonsters() {
        return monsters;
    }

    public int getSelectedMonsterIndex() {
        return monsterIndex;
    }

    public Monster getSelectedMonster() {
        if (monsterIndex < 0 || monsterIndex >= monsters.size()) {
            return null;
        }
        return monsters.get(monsterIndex);
    }

    public int getSelectedSkillIndex() {
        return skillIndex;
    }

    public GameOptionPopup getOptionPopup() {
        return optionPopup;
    }

    public int getFloorNumber() {
        GameMap map = mapManager.getCurrentMap();
        return map == null ? 0 : map.getFloorNumber();
    }

    // ==================== 内部 ====================

    /**
     * 采样当前楼层的怪物：按位置（y 优先、x 次之）排序后按 characterId 去重，
     * 使同种怪物只出现一次且次序不受 HashMap 迭代顺序影响。
     */
    private void refreshMonsters() {
        GameMap map = mapManager.getCurrentMap();
        if (map == null) {
            this.monsters = List.of();
            return;
        }
        List<Map.Entry<Position, Monster>> entries = new ArrayList<>(map.getMonsters().entrySet());
        entries.sort(Comparator
                .comparingInt((Map.Entry<Position, Monster> e) -> e.getKey().getY_index())
                .thenComparingInt(e -> e.getKey().getX_index()));

        Map<String, Monster> distinct = new LinkedHashMap<>();
        for (Map.Entry<Position, Monster> entry : entries) {
            distinct.putIfAbsent(entry.getValue().getCharacterId(), entry.getValue());
        }
        this.monsters = List.copyOf(distinct.values());
    }
}
