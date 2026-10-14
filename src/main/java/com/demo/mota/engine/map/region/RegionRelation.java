package com.demo.mota.engine.map.region;

/** 目标楼层相对当前区域的位置（按区域清单的顺序比较） */
public enum RegionRelation {
    /** 同一区域 */
    SAME,
    /** 后面的区域：需先通过本区域的检查 */
    FORWARD,
    /** 已通过的区域：区域单向推进，不可返回 */
    BACKWARD
}
