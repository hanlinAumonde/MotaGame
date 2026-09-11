package com.demo.mota.engine.menu;

/**
 * 菜单内的抽象操作。UI 层负责把具体按键映射成这里的语义，
 * 菜单状态机只认语义、不认按键。
 */
public enum MenuCommand {
    /** 上移选择 */
    UP,
    /** 下移选择 */
    DOWN,
    /** 确认（回车 / 空格）：进入下一层内容 */
    CONFIRM,
    /** 返回上一层内容，已在最外层时关闭菜单 */
    BACK,
    /** 直接关闭菜单 */
    CLOSE
}
