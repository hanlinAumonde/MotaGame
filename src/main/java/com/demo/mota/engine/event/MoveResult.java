package com.demo.mota.engine.event;

/**
 * 玩家移动操作的所有可能结果
 */
public enum MoveResult {
    MOVED,              // 成功移动到空地
    ITEM_PICKED,        // 移动并拾取了道具
    BATTLE_WON,         // 触发战斗并胜利（玩家位置不变）
    BATTLE_LOST,        // 触发战斗但失败（游戏结束）
    DOOR_OPENED,        // 成功开门（玩家位置不变）
    DARK_WALL_REVEALED, // 暗墙被揭示（玩家位置不变）
    FLOOR_SWITCHED,     // 触发楼层切换
    BLOCKED,            // 被墙/机关墙/钥匙不足等阻挡
    NO_MOVE             // 在地图边界，无法移动
}
