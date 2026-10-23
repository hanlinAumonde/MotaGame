package com.demo.mota.world;

/** 地图上的实体种类（FXGL 实体的 {@code type}），供按类型查询世界 */
public enum EntityType {
    /** 地砖：每格一个底图 */
    FLOOR,
    /** 地物：墙 / 门 / 楼梯 / 机关墙 / 检测格的前景（背景格没有） */
    FEATURE,
    ITEM,
    MONSTER,
    PLAYER,
    /** 正在播消失动画、已经不属于任何一层的实体 */
    EFFECT
}
