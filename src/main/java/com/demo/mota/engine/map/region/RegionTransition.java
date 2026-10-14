package com.demo.mota.engine.map.region;

/**
 * 一次区域切换（{@code MapManager.enterRegion} 的返回值）。
 * 区域切换的动画 / 提示尚未实现，留作将来 UI 层的取数口。
 *
 * @param from 离开的区域（新开一局时为 null）
 * @param to   进入的区域
 */
public record RegionTransition(RegionInfo from, RegionInfo to) {}
