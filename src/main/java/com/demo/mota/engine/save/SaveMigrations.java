package com.demo.mota.engine.save;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 存档格式的迁移链：每一步把 {@code vN} 的 JSON 树改写成 {@code vN+1}，读档时从存档版本逐步升到当前版本再绑定。
 *
 * <p>只有快照结构有不兼容的调整时才需要升版本、登记一步迁移（在 {@link #standard()} 的表里追加）；
 * 增删字段由读取时忽略未知字段、缺省字段取默认值兜住，不必动这里。
 */
public final class SaveMigrations {

    /** 当前程序写出的存档版本 */
    public static final int CURRENT_VERSION = 1;

    private final int currentVersion;
    /** 键为来源版本，值为把它升到下一版本的改写 */
    private final Map<Integer, UnaryOperator<ObjectNode>> steps;

    public SaveMigrations(int currentVersion, Map<Integer, UnaryOperator<ObjectNode>> steps) {
        this.currentVersion = currentVersion;
        this.steps = Map.copyOf(steps);
    }

    /** 本程序使用的迁移链（目前没有任何迁移） */
    public static SaveMigrations standard() {
        return new SaveMigrations(CURRENT_VERSION, Map.of());
    }

    public int currentVersion() {
        return currentVersion;
    }

    /**
     * 把存档树升到当前版本（原地改写并返回同一棵树，{@code version} 字段随之更新）。
     *
     * @throws SaveException 版本高于当前（{@code TOO_NEW}）或缺少某一步迁移（{@code UNSUPPORTED}）
     */
    public ObjectNode migrate(ObjectNode root) throws SaveException {
        int version = root.path("version").asInt(0);
        if (version > currentVersion) {
            throw new SaveException(SaveSlotInfo.Status.TOO_NEW,
                    "存档版本 " + version + " 高于当前程序支持的 " + currentVersion);
        }
        ObjectNode node = root;
        while (version < currentVersion) {
            UnaryOperator<ObjectNode> step = steps.get(version);
            if (step == null) {
                throw new SaveException(SaveSlotInfo.Status.UNSUPPORTED,
                        "缺少从存档版本 " + version + " 升级的迁移");
            }
            node = step.apply(node);
            version++;
            node.put("version", version);
        }
        return node;
    }
}
