package com.demo.mota.engine.menu;

import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.state.monster.Monster;

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
 *   ROOT ──确认──▶ MONSTER_LIST ──确认（该怪物有技能时）──▶ MONSTER_SKILL
 *        ◀─返回──              ◀─返回──
 * </pre>
 *
 * <p>敌物列表在每次打开菜单时按当前楼层重新采样：同一种怪物只保留一条，
 * 顺序按其在地图上的位置（先上后下、先左后右）排定，保证展示稳定。
 */
public class GameMenu {
    private final MapManager mapManager;

    private boolean open;
    private MenuPage page = MenuPage.ROOT;

    private int entryIndex;
    private int monsterIndex;
    private int skillIndex;

    /** 当前楼层去重后的怪物列表快照，打开菜单时刷新 */
    private List<Monster> monsters = List.of();

    public GameMenu(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    // ==================== 开关 ====================

    public void open() {
        this.open = true;
        this.page = MenuPage.ROOT;
        this.entryIndex = 0;
        this.monsterIndex = 0;
        this.skillIndex = 0;
        refreshMonsters();
    }

    public void close() {
        this.open = false;
        this.monsters = List.of();
    }

    public boolean isOpen() {
        return open;
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
        switch (command) {
            case CLOSE -> close();
            case BACK -> back();
            case CONFIRM -> confirm();
            case UP -> move(-1);
            case DOWN -> move(1);
        }
        return true;
    }

    private void back() {
        switch (page) {
            case ROOT -> close();
            case MONSTER_LIST -> page = MenuPage.ROOT;
            case MONSTER_SKILL -> page = MenuPage.MONSTER_LIST;
        }
    }

    private void confirm() {
        switch (page) {
            // 进入右侧内容，并默认选中第一项
            case ROOT -> {
                if (getSelectedEntry() == MenuEntry.MONSTER_BOOK && !monsters.isEmpty()) {
                    page = MenuPage.MONSTER_LIST;
                    monsterIndex = 0;
                }
            }
            // 该怪物有技能时再深入一层，否则停留在列表
            case MONSTER_LIST -> {
                Monster monster = getSelectedMonster();
                if (monster != null && monster.hasSkills()) {
                    page = MenuPage.MONSTER_SKILL;
                    skillIndex = 0;
                }
            }
            case MONSTER_SKILL -> { /* 技能详情已是最内层 */ }
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

    public List<MenuEntry> getEntries() {
        return List.of(MenuEntry.values());
    }

    public int getSelectedEntryIndex() {
        return entryIndex;
    }

    public MenuEntry getSelectedEntry() {
        return MenuEntry.values()[entryIndex];
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
