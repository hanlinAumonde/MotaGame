package com.demo.mota.engine.boot;

import com.demo.mota.engine.factory.item.ItemFactory;
import com.demo.mota.engine.factory.monster.MonsterFactory;
import com.demo.mota.engine.factory.skill.SkillFactory;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.resource.sprite.SheetSlicerRegistry;
import com.demo.mota.engine.skill.effect.SkillEffectRegistry;
import com.demo.mota.engine.state.level.LevelManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动引导：把全局资源与各注册表 / 工厂的初始化集中成一张<b>有序、可观测</b>的任务表。
 *
 * <p>改造前这些初始化是「谁先用到谁触发」——{@code ResourceManager} 在 {@code GameEngine}
 * 构造里被叫起、三个工厂是第一次 {@code createById} 才读表、{@code SkillEffectRegistry}
 * 第一次解析技能才注册内置效果。功能上没问题，但没有任何地方能回答「现在加载到哪一步了」，
 * 也就做不出加载界面。
 *
 * <p>本类不改变这些单例的惰性语义（仍可被任何调用方触发），只是提前、按序地把它们叫起来，
 * 并在每一步之前回调进度。将来的存档索引扫描、外部脚本加载器都往这张表里加一项即可。
 *
 * <p><b>线程</b>：{@link #run} 设计为在后台线程执行（见 {@code MotaApplication}），
 * 因此进度回调也发生在后台线程，UI 侧需要自行切回 FX 线程。
 */
public final class GameBootstrap {

    /** 进度回调：{@code progress} ∈ [0,1]，{@code taskName} 为即将执行（或刚完成）的任务名 */
    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(double progress, String taskName);
    }

    private final List<LoadingTask> tasks;

    public GameBootstrap() {
        this(defaultTasks());
    }

    public GameBootstrap(List<LoadingTask> tasks) {
        this.tasks = List.copyOf(tasks);
    }

    /**
     * 默认任务表，顺序即依赖顺序：
     * 精灵图依赖已搭好的 provider 链，三个工厂在读表时会回头调 {@code registerXxxImage}，
     * 因此都排在资源之后。
     */
    public static List<LoadingTask> defaultTasks() {
        List<LoadingTask> tasks = new ArrayList<>();
        tasks.add(new LoadingTask("加载图像资源", 40,
                () -> ResourceManager.getInstance().loadSpriteSheets()));
        tasks.add(new LoadingTask("注册切图规则", 5,
                SheetSlicerRegistry::getInstance));
        tasks.add(new LoadingTask("注册技能效果", 5,
                SkillEffectRegistry::getInstance));
        tasks.add(new LoadingTask("载入技能表", 10,
                SkillFactory::getInstance));
        tasks.add(new LoadingTask("载入道具表", 10,
                ItemFactory::getInstance));
        tasks.add(new LoadingTask("载入怪物表", 20,
                MonsterFactory::getInstance));
        tasks.add(new LoadingTask("载入等级表", 10,
                LevelManager::preload));
        return tasks;
    }

    /** 任务数量，供测试与调试查看 */
    public int taskCount() {
        return tasks.size();
    }

    /**
     * 按序执行全部任务。任何一项抛出异常都直接向上抛——
     * 资源与配置表缺失属于打包错误，没有可降级的余地，早失败早发现。
     *
     * @param listener 进度回调，可为 {@code null}
     */
    public void run(ProgressListener listener) {
        int total = tasks.stream().mapToInt(LoadingTask::weight).sum();
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
