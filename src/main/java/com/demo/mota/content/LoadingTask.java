package com.demo.mota.content;

/**
 * 加载期的一项任务。
 *
 * @param name   展示在加载界面上的任务名
 * @param weight 相对耗时，折算进度条
 * @param action 任务本体
 */
public record LoadingTask(String name, int weight, Runnable action) {
}
