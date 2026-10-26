package com.demo.mota.engine.snapshot;

/**
 * 一局游戏在某一时刻的完整运行时状态：玩家 + 地图。
 *
 * <p>撤销（内存里的快照栈）与存档（写到磁盘的快照）共用这一份结构。
 * 快照只存<b>纯数据</b>、按 id 引用配置表里的道具 / 怪物 / 技能，不持有任何运行时对象，
 * 因此拍下之后游戏继续进行也不会改到它；恢复时由 {@code GameSession.restore} 按快照整套重建一局。
 */
public record GameSnapshot(PlayerSnapshot player, WorldSnapshot world) {
}
