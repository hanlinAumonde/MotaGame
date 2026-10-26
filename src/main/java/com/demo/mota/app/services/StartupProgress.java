package com.demo.mota.app.services;

import com.demo.mota.content.LoadingTask;
import javafx.application.Platform;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 启动期（FXGL 服务初始化）的总进度：内容服务、资产服务各自的任务表汇成一根进度条，画在启动场景上。
 *
 * <p>FXGL 先构造全部引擎服务、再按注册顺序逐个 {@code onInit}（后台线程，源码：{@code Engine.initServices}），
 * 所以各服务在<b>构造时</b>{@link #reserve 登记}自己的总权重，进度条从一开始就知道总量、不会倒退；
 * {@code onInit} 里再 {@link #run 执行}任务表。
 *
 * <p>启动场景早于一切 FXGL 服务构造（源码：{@code FXGLApplication.start}），拿不到服务实例，
 * 因此本类是进程内唯一的静态实例。进度经 {@link Platform#runLater} 交给 FX 线程上的监听者。
 */
public final class StartupProgress {

    private static final StartupProgress INSTANCE = new StartupProgress();

    private int total;
    private int done;
    private volatile BiConsumer<Double, String> listener = (progress, task) -> { };

    private StartupProgress() {
    }

    public static StartupProgress get() {
        return INSTANCE;
    }

    /** 由启动场景设置（FX 线程）；回调同样在 FX 线程：{@code progress} ∈ [0,1]，{@code task} 为当前任务名 */
    public void setListener(BiConsumer<Double, String> listener) {
        this.listener = listener;
    }

    /** 服务构造时登记即将执行的任务总权重 */
    public synchronized void reserve(List<LoadingTask> tasks) {
        total += tasks.stream().mapToInt(LoadingTask::weight).sum();
    }

    /**
     * 按序执行任务表并推进总进度。任一项抛异常时先把原因显示在启动场景上，再原样抛出——
     * FXGL 随后弹出致命错误框并退出（资源 / 配置缺失属于打包错误，没有可降级的余地）。
     */
    public void run(List<LoadingTask> tasks) {
        for (LoadingTask task : tasks) {
            report(task.name());
            try {
                task.action().run();
            } catch (RuntimeException e) {
                publish(fraction(), "加载失败：" + e.getMessage());
                throw e;
            }
            synchronized (this) {
                done += task.weight();
            }
        }
        report("加载完成");
    }

    private void report(String taskName) {
        publish(fraction(), taskName);
    }

    private synchronized double fraction() {
        return total == 0 ? 1 : Math.min(1, (double) done / total);
    }

    private void publish(double progress, String taskName) {
        BiConsumer<Double, String> target = listener;
        Platform.runLater(() -> target.accept(progress, taskName));
    }
}
