package com.demo.mota.app.services;

import com.almasb.fxgl.core.EngineService;
import com.demo.mota.content.ContentLoader;
import com.demo.mota.content.ContentSource;
import com.demo.mota.content.LoadingTask;
import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.save.SaveMigrations;
import com.demo.mota.engine.save.SaveRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 内容服务（FXGL {@link EngineService}）：启动期一次性读完塔的全部内容（{@link TowerContent}），并建好存档库、扫描档位。
 *
 * <p>{@link #onInit()} 在 FXGL 的服务初始化阶段（后台线程、启动场景显示期间）执行 {@link ContentLoader} 的任务表，
 * 进度汇入 {@link StartupProgress}。排在 {@link AssetService} 之前注册：后者要按内容表预载图片。
 * 标题菜单、游戏菜单在这之前就已构造（源码：{@code GameApplicationService.initAppScenes}），它们只能在进入时取内容。
 */
public final class ContentService extends EngineService {

    private final ContentLoader loader = new ContentLoader(ContentSource.getInstance());
    private final List<LoadingTask> tasks = new ArrayList<>();

    private volatile TowerContent content;
    private volatile SaveRepository saves;

    public ContentService() {
        tasks.addAll(loader.tasks());
        tasks.add(new LoadingTask("扫描存档", 3, this::openSaves));
        StartupProgress.get().reserve(tasks);
    }

    @Override
    public void onInit() {
        StartupProgress.get().run(tasks);
    }

    /** 按本塔规则的档位数建存档库并扫描已有档位 */
    private void openSaves() {
        content = loader.result();
        saves = new SaveRepository(SaveRepository.defaultDirectory(),
                content.rules().save().slotCount(), SaveMigrations.standard());
        saves.refresh();
    }

    /** 本塔的内容（启动期之后恒非 null） */
    public TowerContent content() {
        return Objects.requireNonNull(content, "塔的内容尚未载入");
    }

    /** 存档库（启动期之后恒非 null） */
    public SaveRepository saves() {
        return Objects.requireNonNull(saves, "存档库尚未建立");
    }
}
