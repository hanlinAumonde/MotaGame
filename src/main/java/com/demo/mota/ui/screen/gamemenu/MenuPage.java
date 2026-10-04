package com.demo.mota.ui.screen.gamemenu;

/**
 * 菜单的内容层级。确认键逐层深入，返回键逐层退出：
 * <pre>
 *   ROOT ─┬─(敌物资料)─▶ MONSTER_LIST ─▶ MONSTER_SKILL
 *         └─(游戏选项)─▶ OPTIONS ─(选中任一项)─▶ 关闭本菜单，改开对应的独立界面
 * </pre>
 */
public enum MenuPage {
    /** 焦点在左侧菜单项上，右侧为对应内容的预览 */
    ROOT,
    /** 焦点在右侧敌物列表上 */
    MONSTER_LIST,
    /** 焦点在选中怪物的技能列表上 */
    MONSTER_SKILL,
    /** 焦点在「游戏选项」弹出的次级菜单上 */
    OPTIONS
}
