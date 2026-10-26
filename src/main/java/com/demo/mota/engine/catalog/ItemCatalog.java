package com.demo.mota.engine.catalog;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.item.AbilityGem;
import com.demo.mota.engine.item.AuxiliaryItem;
import com.demo.mota.engine.item.AuxiliaryType;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.item.Key;
import com.demo.mota.engine.item.Portion;
import com.demo.mota.engine.item.generic.FloorJumper;
import com.demo.mota.engine.item.generic.GenericItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 道具目录：按 id 造出新的道具实例（道具带数量，可变，所以每次都是新对象）。
 * 装备附带的技能经 {@link SkillCatalog} 解析，因此技能目录要先建好。
 */
public final class ItemCatalog extends Catalog<ItemDefinition> {

    // itemType
    private static final String EQUIPMENT = "Equipment";
    private static final String KEY = "Key";
    private static final String PORTION = "Portion";
    private static final String ABILITY_GEM = "AbilityGem";
    private static final String AUXILIARY_ITEM = "AuxiliaryItem";
    private static final String GENERIC_ITEM = "GenericItem";

    // parameters 里的字段
    private static final String EQUIPMENT_STATS = "stats";
    private static final String EQUIPMENT_SKILLS = "skills";
    private static final String EQUIPMENT_SLOT_TYPE = "slotType";
    private static final String KEY_COLOR = "keyColor";
    private static final String HEALING_AMOUNT = "healingAmount";
    private static final String ABILITY_TYPE = "abilityType";
    private static final String ABILITY_VALUE = "abilityValue";
    private static final String AUXILIARY_TYPE = "auxiliaryType";
    private static final String GENERIC_ITEM_CLASS_NAME = "className";
    private static final String FLOOR_NUMBER_SELECTED = "floorNumberSelected";

    private final SkillCatalog skills;

    public ItemCatalog(Collection<ItemDefinition> definitions, SkillCatalog skills) {
        super("道具", definitions, ItemDefinition::itemId);
        this.skills = skills;
    }

    /**
     * 造一个新的道具实例（数量为 1）。
     *
     * @throws IllegalArgumentException 道具表里没有这个 id
     * @throws IllegalStateException    道具大类或通用道具的类名无法识别（数据错误）
     */
    public Item create(String id) {
        ItemDefinition d = require(id);
        Map<String, Object> p = d.parameters() == null ? Map.of() : d.parameters();
        return switch (d.itemType()) {
            case EQUIPMENT -> new Equipment(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), stats(p), skills.createAll(skillIds(p)),
                    (String) p.get(EQUIPMENT_SLOT_TYPE));
            case KEY -> new Key(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), KeyColor.fromString((String) p.get(KEY_COLOR)));
            case PORTION -> new Portion(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), GameNumber.of(number(p, HEALING_AMOUNT)));
            case ABILITY_GEM -> new AbilityGem(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), StateType.fromString((String) p.get(ABILITY_TYPE)),
                    GameNumber.of(number(p, ABILITY_VALUE)));
            case AUXILIARY_ITEM -> new AuxiliaryItem(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), AuxiliaryType.fromString((String) p.get(AUXILIARY_TYPE)));
            case GENERIC_ITEM -> genericItem(d, p);
            default -> throw new IllegalStateException("无法识别的道具大类 " + d.itemType() + "：" + d.itemId());
        };
    }

    /** 造出来恰好是 {@code type} 类型时返回，id 不存在或类型不符时为 {@code null}（快照恢复用） */
    public <T extends Item> T createOrNull(String id, Class<T> type) {
        if (!contains(id)) {
            return null;
        }
        Item item = create(id);
        return type.isInstance(item) ? type.cast(item) : null;
    }

    @Override
    protected String spriteOf(ItemDefinition definition) {
        return definition.sprite();
    }

    private static GenericItem genericItem(ItemDefinition d, Map<String, Object> p) {
        String className = String.valueOf(p.get(GENERIC_ITEM_CLASS_NAME));
        return switch (className.toUpperCase(Locale.ROOT)) {
            case "FLOORJUMPER" -> new FloorJumper(d.itemId(), d.itemName(), d.itemDescription(), d.itemPrice(), 1,
                    d.isStorable(), d.isConsumable(), (int) number(p, FLOOR_NUMBER_SELECTED));
            default -> throw new IllegalStateException("无法识别的通用道具 " + className + "：" + d.itemId());
        };
    }

    /** parameters.stats：{ "attack": 10, ... }，键为 {@link StateType} 的字符串值 */
    private static Map<StateType, GameNumber> stats(Map<String, Object> p) {
        Map<StateType, GameNumber> stats = new HashMap<>();
        if (p.get(EQUIPMENT_STATS) instanceof Map<?, ?> raw) {
            raw.forEach((stat, value) ->
                    stats.put(StateType.fromString(String.valueOf(stat)), GameNumber.of(((Number) value).longValue())));
        }
        return stats;
    }

    private static List<String> skillIds(Map<String, Object> p) {
        return p.get(EQUIPMENT_SKILLS) instanceof List<?> ids ? ids.stream().map(String::valueOf).toList() : List.of();
    }

    private static long number(Map<String, Object> p, String key) {
        if (!(p.get(key) instanceof Number value)) {
            throw new IllegalStateException("道具参数 " + key + " 缺失或不是数字");
        }
        return value.longValue();
    }
}
