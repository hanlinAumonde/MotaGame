package com.demo.mota.content;

import java.util.List;

/**
 * 按序执行一张加载任务表，每项之前回调进度。
 *
 * <p><b>线程</b>：设计为在后台线程执行，进度回调也发生在后台线程，UI 侧需要自行切回 FX 线程。
 * <b>失败策略</b>：任何一项抛异常都直接向上抛——资源与配置表缺失属于打包错误，没有可降级的余地。
 */
public final class LoadingRunner {

    /** 进度回调：{@code progress} ∈ [0,1]，{@code taskName} 为即将执行（或刚完成）的任务名 */
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(double progress, String taskName);
    }

    private LoadingRunner() {
    }

    /** @param listener 进度回调，可为 {@code null} */
    public static void run(List<LoadingTask> tasks, ProgressListener listener) {
        int total = Math.max(1, tasks.stream().mapToInt(LoadingTask::weight).sum());
        int done = 0;
        for (LoadingTask task : tasks) {
            notify(listener, (double) done / total, task.name());
            task.action().run();
            done += task.weight();
        }
        notify(listener, 1.0, "加载完成");
    }

    private static void notify(ProgressListener listener, double progress, String taskName) {
        if (listener != null) {
            listener.onProgress(progress, taskName);
        }
    }
}
