package com.demo.mota.engine.save;

import com.demo.mota.engine.snapshot.GameSnapshot;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 存档的读写与档位索引。每个档位一个 JSON 文件：{@code auto.json}（自动存档）+ {@code slot_01.json} ……
 *
 * <p>目录默认是运行目录下的 {@code saves/}（开发期即项目根目录），可用系统属性 {@code -Dmota.save.dir=...} 覆盖；
 * 手动档位数取塔规则 {@code save.slotCount}。
 *
 * <p><b>版本兼容</b>分三层：读取时忽略未知字段、缺省字段取默认值（增删字段天然兼容）；
 * 结构调整经 {@link SaveMigrations} 逐步升级；快照里引用的内容在配置表里已不存在时由恢复逻辑跳过。
 * 只有 JSON 本身损坏、版本过新、缺少迁移三种情况读不了，档位分别标出状态，不会把异常抛到界面外。
 *
 * <p>档位列表在启动时扫描一次（加载期任务「扫描存档」），之后每次写入同步更新，
 * 因此标题界面判断「有没有存档」不必再碰磁盘。
 */
public final class SaveRepository {

    public static final String AUTO_SLOT = "auto";
    public static final String SAVE_DIR_PROPERTY = "mota.save.dir";

    private static final String EXTENSION = ".json";

    private final Path directory;
    private final int slotCount;
    private final SaveMigrations migrations;
    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /** 档位索引缓存，顺序与 {@link #slotIds()} 一致；尚未扫描时为 null */
    private volatile List<SaveSlotInfo> slots;

    public SaveRepository(Path directory, int slotCount, SaveMigrations migrations) {
        this.directory = directory;
        this.slotCount = Math.max(1, slotCount);
        this.migrations = migrations;
    }

    /** 默认存档目录：系统属性 {@value #SAVE_DIR_PROPERTY}，没有时为运行目录下的 {@code saves/} */
    public static Path defaultDirectory() {
        String configured = System.getProperty(SAVE_DIR_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.dir"), "saves");
    }

    public Path getDirectory() {
        return directory;
    }

    /** 全部档位 id：自动存档在前，其后是手动档位 */
    public List<String> slotIds() {
        List<String> ids = new ArrayList<>();
        ids.add(AUTO_SLOT);
        for (int i = 1; i <= slotCount; i++) {
            ids.add(String.format("slot_%02d", i));
        }
        return ids;
    }

    public static String displayName(String slotId) {
        if (AUTO_SLOT.equals(slotId)) {
            return "自动存档";
        }
        return "存档 " + slotId.substring(slotId.indexOf('_') + 1);
    }

    // ==================== 索引 ====================

    /** 重新扫描全部档位 */
    public List<SaveSlotInfo> refresh() {
        List<SaveSlotInfo> scanned = new ArrayList<>();
        for (String slotId : slotIds()) {
            scanned.add(inspect(slotId));
        }
        this.slots = List.copyOf(scanned);
        return slots;
    }

    /** 档位索引（尚未扫描过时先扫一次） */
    public List<SaveSlotInfo> getSlots() {
        List<SaveSlotInfo> current = slots;
        return current != null ? current : refresh();
    }

    /** 是否至少有一个能读取的存档 */
    public boolean hasAnySave() {
        return getSlots().stream().anyMatch(SaveSlotInfo::isLoadable);
    }

    private SaveSlotInfo inspect(String slotId) {
        String name = displayName(slotId);
        if (!Files.exists(fileOf(slotId))) {
            return new SaveSlotInfo(slotId, name, SaveSlotInfo.Status.EMPTY, null);
        }
        try {
            return new SaveSlotInfo(slotId, name, SaveSlotInfo.Status.OK, read(slotId).meta());
        } catch (SaveException e) {
            return new SaveSlotInfo(slotId, name, e.getStatus(), null);
        }
    }

    // ==================== 读写 ====================

    /**
     * 写入一个档位（覆盖原有）。先写临时文件再整体替换，写到一半出错不会毁掉原来的存档。
     */
    public void write(String slotId, SaveMeta meta, GameSnapshot snapshot) throws IOException {
        Files.createDirectories(directory);
        Path target = fileOf(slotId);
        Path temp = directory.resolve(slotId + EXTENSION + ".tmp");
        mapper.writeValue(temp.toFile(), new SaveFile(migrations.currentVersion(), meta, snapshot));
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

        List<SaveSlotInfo> updated = new ArrayList<>(getSlots());
        int index = slotIds().indexOf(slotId);
        if (index >= 0) {
            updated.set(index, new SaveSlotInfo(slotId, displayName(slotId), SaveSlotInfo.Status.OK, meta));
            this.slots = List.copyOf(updated);
        }
    }

    /**
     * 读取一个档位：解析 → 按需迁移到当前版本 → 绑定 → 校验必需部分。
     *
     * @throws SaveException 档位为空 / 损坏 / 版本过新 / 缺少迁移
     */
    public SaveFile read(String slotId) throws SaveException {
        Path file = fileOf(slotId);
        if (!Files.exists(file)) {
            throw new SaveException(SaveSlotInfo.Status.EMPTY, displayName(slotId) + " 还没有存档");
        }
        try {
            JsonNode tree = mapper.readTree(file.toFile());
            if (!(tree instanceof ObjectNode root)) {
                throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档内容不是 JSON 对象");
            }
            SaveFile save = mapper.treeToValue(migrations.migrate(root), SaveFile.class);
            GameSnapshot snapshot = save.snapshot();
            if (snapshot == null || snapshot.player() == null || snapshot.world() == null
                    || snapshot.world().regionId() == null) {
                throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档缺少必需的内容");
            }
            return save;
        } catch (JacksonException e) {
            throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档无法解析：" + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new SaveException(SaveSlotInfo.Status.CORRUPTED, "存档读取失败：" + e.getMessage(), e);
        }
    }

    private Path fileOf(String slotId) {
        return directory.resolve(slotId + EXTENSION);
    }
}
