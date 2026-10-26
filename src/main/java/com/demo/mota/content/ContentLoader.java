package com.demo.mota.content;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.catalog.ItemCatalog;
import com.demo.mota.engine.catalog.ItemDefinition;
import com.demo.mota.engine.catalog.LevelTable;
import com.demo.mota.engine.catalog.MonsterCatalog;
import com.demo.mota.engine.catalog.MonsterDefinition;
import com.demo.mota.engine.catalog.PlayerDefinition;
import com.demo.mota.engine.catalog.SkillCatalog;
import com.demo.mota.engine.catalog.SkillDefinition;
import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.map.region.RegionCatalog;
import com.demo.mota.engine.map.region.RegionInfo;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.skill.cost.SkillCostRegistry;
import com.demo.mota.engine.skill.effect.SkillEffectRegistry;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 把一座塔的全部数据读成 {@link TowerContent}：规则、区域、技能 / 道具 / 怪物表、等级表、玩家开局状态，
 * 楼层则交给按需读取的 {@link JsonFloorSource}。
 *
 * <p>读取拆成一张<b>有序、可观测</b>的任务表（{@link #tasks()}），由宿主在后台线程按序执行、把进度画到加载界面上，
 * 全部完成后取 {@link #result()}。顺序即依赖顺序：装备附带的技能、怪物的技能都要先有技能表。
 * 任何一项缺失或格式错误都直接抛异常（打包错误，没有可降级的余地）。
 */
public final class ContentLoader {

    public static final String RULES = "/assets/json/rules.json";
    public static final String REGIONS = "/assets/json/regions.json";
    public static final String SKILLS = "/assets/json/skills.json";
    public static final String ITEMS = "/assets/json/items.json";
    public static final String MONSTERS = "/assets/json/monsters.json";
    public static final String LEVELS = "/assets/json/levels.json";
    public static final String PLAYER = "/assets/json/player.json";

    private final ContentSource source;

    private GameRules rules;
    private RegionCatalog regions;
    private SkillCatalog skills;
    private ItemCatalog items;
    private MonsterCatalog monsters;
    private LevelTable levels;
    private PlayerDefinition player;

    public ContentLoader(ContentSource source) {
        this.source = source;
    }

    /** 一次性读完整座塔（测试、工具用，不报进度；游戏里由内容服务经启动进度执行 {@link #tasks()}） */
    public static TowerContent load(ContentSource source) {
        ContentLoader loader = new ContentLoader(source);
        loader.tasks().forEach(task -> task.action().run());
        return loader.result();
    }

    public List<LoadingTask> tasks() {
        List<LoadingTask> tasks = new ArrayList<>();
        tasks.add(new LoadingTask("载入规则配置", 3, this::loadRules));
        tasks.add(new LoadingTask("载入区域表", 2, this::loadRegions));
        tasks.add(new LoadingTask("注册技能效果", 5, () -> {
            var _ = SkillEffectRegistry.getInstance();
            var _ = SkillCostRegistry.getInstance();
        }));
        tasks.add(new LoadingTask("载入技能表", 10, this::loadSkills));
        tasks.add(new LoadingTask("载入道具表", 10, this::loadItems));
        tasks.add(new LoadingTask("载入怪物表", 20, this::loadMonsters));
        tasks.add(new LoadingTask("载入等级表", 10, this::loadLevels));
        tasks.add(new LoadingTask("载入角色", 2, this::loadPlayer));
        return tasks;
    }

    /** 任务表全部执行完之后的结果 */
    public TowerContent result() {
        return new TowerContent(
                Objects.requireNonNull(rules, "规则尚未载入"),
                Objects.requireNonNull(regions, "区域表尚未载入"),
                Objects.requireNonNull(skills, "技能表尚未载入"),
                Objects.requireNonNull(items, "道具表尚未载入"),
                Objects.requireNonNull(monsters, "怪物表尚未载入"),
                Objects.requireNonNull(levels, "等级表尚未载入"),
                Objects.requireNonNull(player, "角色尚未载入"),
                new JsonFloorSource(source));
    }

    private void loadRules() {
        GameRules loaded = source.loadJsonResource(RULES, new TypeReference<>() {});
        rules = loaded == null ? GameRules.DEFAULT : loaded;
    }

    private record RegionManifest(List<RegionInfo> regions) {
    }

    private void loadRegions() {
        RegionManifest manifest = source.loadJsonResource(REGIONS, new TypeReference<>() {});
        regions = new RegionCatalog(manifest == null ? null : manifest.regions());
    }

    private void loadSkills() {
        List<SkillDefinition> definitions = source.loadJsonResource(SKILLS, new TypeReference<>() {});
        skills = new SkillCatalog(definitions);
    }

    /** 道具表按大类分组：{@code [ { "Equipment": [ ... ] }, { "Key": [ ... ] }, ... ]} */
    private void loadItems() {
        List<Map<String, List<ItemDefinition>>> groups = source.loadJsonResource(ITEMS, new TypeReference<>() {});
        List<ItemDefinition> definitions = new ArrayList<>();
        groups.forEach(group -> group.values().forEach(definitions::addAll));
        items = new ItemCatalog(definitions, requireSkills());
    }

    private void loadMonsters() {
        List<MonsterDefinition> definitions = source.loadJsonResource(MONSTERS, new TypeReference<>() {});
        monsters = new MonsterCatalog(definitions, requireSkills());
    }

    private void loadLevels() {
        levels = source.loadJsonResource(LEVELS, new TypeReference<>() {});
    }

    /**
     * {@code player.json}：{@code playerId / playerName / health / maxHealth（可选）/ attack / defense /
     * playerInitialPosition}（开局朝向）
     */
    private void loadPlayer() {
        Map<String, Object> data = source.loadJsonResource(PLAYER, new TypeReference<>() {});
        Object direction = data.get("playerInitialPosition");
        player = new PlayerDefinition(
                (String) data.get("playerId"),
                (String) data.get("playerName"),
                number(data, "health"),
                data.containsKey("maxHealth") ? number(data, "maxHealth") : null,
                number(data, "attack"),
                number(data, "defense"),
                direction == null ? null : Direction.fromString((String) direction));
    }

    private SkillCatalog requireSkills() {
        return Objects.requireNonNull(skills, "技能表要先于道具表 / 怪物表载入");
    }

    private static GameNumber number(Map<String, Object> data, String key) {
        if (!(data.get(key) instanceof Number value)) {
            throw new IllegalStateException(PLAYER + " 缺少数值字段 " + key);
        }
        return GameNumber.of(value.longValue());
    }
}
