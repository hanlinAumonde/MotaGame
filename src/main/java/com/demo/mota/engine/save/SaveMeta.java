package com.demo.mota.engine.save;

/**
 * 存档摘要：档位列表里直接展示，不必为了列个表把整份快照恢复出来。
 *
 * @param savedAt    保存时间（本地时间，{@code yyyy-MM-dd HH:mm}）
 * @param regionName 所在区域名
 * @param hp         当前生命值（十进制字符串，与快照里的数值口径一致）
 */
public record SaveMeta(String savedAt, String regionName, int floor, int level, String hp) {
}
