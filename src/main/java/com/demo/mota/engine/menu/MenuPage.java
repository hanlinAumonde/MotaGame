package com.demo.mota.engine.menu;

/**
 * 菜单的内容层级。确认键逐层深入，返回键逐层退出：
 * {@link #ROOT} → {@link #MONSTER_LIST} → {@link #MONSTER_SKILL}。
 */
public enum MenuPage {
    /** 焦点在左侧菜单项上，右侧为对应内容的预览 */
    ROOT,
    /** 焦点在右侧敌物列表上 */
    MONSTER_LIST,
    /** 焦点在选中怪物的技能列表上 */
    MONSTER_SKILL
}
