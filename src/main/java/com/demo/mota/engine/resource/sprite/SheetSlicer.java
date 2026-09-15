package com.demo.mota.engine.resource.sprite;

/**
 * 一种「如何解释精灵图网格」的切分策略。
 *
 * <p>切图这个动作本身由 {@link SpriteSheet#sub(int, int)} 统一完成，
 * slicer 只负责回答两件事：<b>哪些格子要切</b>、<b>切出来叫什么名字 / 属于谁</b>。
 * 地图元件图与角色行走图的区别完全落在这一层。
 *
 * <p>实现类通过 {@link SheetSlicerRegistry} 按配置里的 {@code type} 字段注册，
 * 内置实现见 {@code sprite.builtin} 包。
 */
@FunctionalInterface
public interface SheetSlicer {

    /**
     * 按配置切分整张图，并把产物写入 store。
     *
     * @param sheet  已按配置几何构造好的精灵图
     * @param config 本次拆分的完整配置（切法专属字段取 {@link SpriteSheetConfig#params()}）
     * @param store  产物写入口
     */
    void slice(SpriteSheet sheet, SpriteSheetConfig config, SpriteStore store);
}
