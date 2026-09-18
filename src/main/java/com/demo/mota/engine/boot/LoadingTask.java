package com.demo.mota.engine.boot;

/**
 * 启动加载流程中的一项任务。
 *
 * @param name   展示在加载界面上的任务名
 * @param weight 相对耗时权重，用于折算进度条；无需精确，量级对就行
 * @param action 任务本体
 */
public record LoadingTask(String name, int weight, Runnable action) {

    public LoadingTask {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Loading task name must not be blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Loading task weight must be positive: " + name);
        }
        if (action == null) {
            throw new IllegalArgumentException("Loading task action must not be null: " + name);
        }
    }
}
