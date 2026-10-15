package com.demo.mota.engine.snapshot;

import java.util.List;

/**
 * 地图状态快照：只存<b>当前区域</b>（已通过的区域早已卸载，不会再回去）。
 *
 * <p>各层只记「相对楼层文件的变化」（见 {@link FloorState}），恢复时重新解析该区域的楼层文件、
 * 再把变化逐条施加上去。这样塔的数据后续有改动（比如加了一只怪）时，旧存档依然能读，新内容照常出现。
 */
public record WorldSnapshot(String regionId, int currentFloor, int playerX, int playerY, List<FloorState> floors) {
}
