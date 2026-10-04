package com.demo.mota.engine.state;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.Item.*;
import com.demo.mota.engine.Item.GenericItem.GenericItem;
import com.demo.mota.engine.factory.item.ItemFactory;
import com.demo.mota.engine.factory.skill.SkillFactory;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.rules.EquipSlotRule;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillCast;
import com.demo.mota.engine.skill.book.OwnedSkill;
import com.demo.mota.engine.skill.book.SkillBook;
import com.demo.mota.engine.skill.book.SkillSource;
import com.demo.mota.engine.skill.preset.PresetEditResult;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.level.LevelBonus;
import com.demo.mota.engine.state.level.LevelManager;
import com.demo.mota.engine.state.level.LevelUpResult;

import java.util.*;
import java.util.stream.Stream;

import static com.demo.mota.engine.configs.ItemConfigConstants.INITIAL_KEY_SET;

public class PlayerStateManager extends AbstractCharacterState {
    private final LevelManager levelManager;

    private long currentGoldAmount;

    /** 拿到过的全部装备（含已穿上的），按拾取顺序 */
    private final List<Equipment> equipmentsOwned;
    /** 槽位 → 装备，下标即槽位号，空槽为 null；长度由规则配置决定 */
    private final Equipment[] equipmentsEquipped;
    private EquipSlotRule equipSlotRule = EquipSlotRule.ALLOW_ALL;

    /** 玩家的技能书（来源 + 开关）；怪物不用它，仍由基类直接持有技能列表 */
    private final SkillBook skillBook;
    /** 主动技能预设 + 当前就绪的那一套 */
    private final SkillPresetBook presetBook;
    private final boolean presetEditable;

    // Keys
    private Key yellow_Key;
    private Key red_Key;
    private Key blue_Key;
    private List<Key> ancientKeys;

    private final List<GenericItem> genericItemsOwned;
    /** 辅助类道具（怪物手册……），按拾取顺序 */
    private final List<AuxiliaryItem> auxiliaryItemsOwned;

    public PlayerStateManager(String characterId, String characterName, Map<StateType, GameNumber> stateMap, Direction currentDirection){
        this(characterId, characterName, stateMap, currentDirection, GameRules.get());
    }

    public PlayerStateManager(String characterId, String characterName, Map<StateType, GameNumber> stateMap,
                              Direction currentDirection, GameRules rules){
        super(characterId, characterName, stateMap, currentDirection);
        this.levelManager = new LevelManager();
        this.currentGoldAmount = 0;
        this.equipmentsOwned = new ArrayList<>();
        this.equipmentsEquipped = new Equipment[rules.equipment().slotCount()];
        this.skillBook = new SkillBook();

        GameRules.SkillRules skillRules = rules.skill();
        this.presetBook = new SkillPresetBook(skillRules.presetCount(), skillRules.slotCount());
        this.presetBook.load(skillRules.fixedPresets().stream()
                .map(data -> SkillPreset.of(data.name(), skillRules.slotCount(), data.slots()))
                .toList());
        this.presetEditable = skillRules.presetEditable();

        setCurrentKeySet(INITIAL_KEY_SET);
        this.genericItemsOwned = new ArrayList<>();
        this.auxiliaryItemsOwned = new ArrayList<>();
    }

    public void setCurrentKeySet(String currentKeySet) {
        addAncientKey();
        this.yellow_Key = (Key) ItemFactory.getInstance().createById(currentKeySet + KeyColor.YELLOW.getValue());
        this.red_Key = (Key) ItemFactory.getInstance().createById(currentKeySet + KeyColor.RED.getValue());
        this.blue_Key = (Key) ItemFactory.getInstance().createById(currentKeySet + KeyColor.BLUE.getValue());
    }

    private void addAncientKey() {
        if (this.ancientKeys == null) {
            this.ancientKeys = List.of();
        }
        if(this.yellow_Key != null) this.ancientKeys.add(this.yellow_Key);
        if(this.red_Key != null) this.ancientKeys.add(this.red_Key);
        if(this.blue_Key != null) this.ancientKeys.add(this.blue_Key);
    }

    /** 某项属性的实际值：基础值 + 已穿戴装备的加成（支持 ATK / DEF / MAX_HP） */
    public GameNumber getEffectiveAttr(StateType stateType){
        GameNumber playerBaseState = switch (stateType) {
            case ATK, DEF, MAX_HP -> this.getStateValue(stateType);
            default -> throw new IllegalArgumentException("Invalid state type: " + stateType);
        };
        GameNumber equipBonus = Arrays.stream(equipmentsEquipped)
                .filter(Objects::nonNull)
                .filter(equipment -> equipment.getStateEffectMap().containsKey(stateType))
                .map(equipment -> equipment.getStateEffectMap().get(stateType))
                .reduce(GameNumber.ZERO, GameNumber::plus);
        return playerBaseState.plus(equipBonus);
    }

    public GameNumber getEffectiveATK(){ return getEffectiveAttr(StateType.ATK); }
    public GameNumber getEffectiveDEF(){ return getEffectiveAttr(StateType.DEF); }

    /** 生命上限：基础上限 + 装备加成 */
    public GameNumber getMaxHP(){ return getEffectiveAttr(StateType.MAX_HP); }

    public GameNumber getCurrentHP(){ return this.getStateValue(StateType.HP); }

    /**
     * 是否已阵亡。生命值归零即判死，由 UI 层在每次玩家行动之后统一检查，
     * 这样战斗、以及将来的岩浆 / 毒 / 陷阱等掉血途径都汇到同一个判定点。
     */
    public boolean isDead() {
        return getCurrentHP().isNonPositive();
    }

    /**
     * 恢复生命值，结果不超过生命上限。
     */
    public void heal(GameNumber amount) {
        if (amount == null || amount.isNonPositive()) return;
        GameNumber maxHP = getMaxHP();
        GameNumber healed = getCurrentHP().plus(amount);
        this.updateState(StateType.HP, healed.compareTo(maxHP) > 0 ? maxHP : healed);
    }

    /**
     * 提升生命上限，并同时回复等量生命值（拾取生命上限宝石 / 升级时的上限成长）。
     */
    public void increaseMaxHP(GameNumber amount) {
        if (amount == null || amount.isNonPositive()) return;
        this.updateState(StateType.MAX_HP, this.getStateValue(StateType.MAX_HP).plus(amount));
        heal(amount);
    }

    /** 将生命值回满至当前生命上限 */
    public void restoreFullHP() {
        this.updateState(StateType.HP, getMaxHP());
    }

    public int getLevelNumber() { return this.levelManager.getLevelNumber(); }
    public GameNumber getCurrentExp() { return this.levelManager.getCurrentExperience(); }
    public void updateLevel(GameNumber expGained) {
        LevelUpResult result = this.levelManager.cumulateExperience(expGained);
        if (result.didLevelUp()) {
            for (LevelBonus bonus : result.bonuses()) {
                GameNumber currentBase = this.getStateValue(bonus.stat());
                GameNumber newValue = bonus.apply(currentBase);
                switch (bonus.stat()) {
                    // 上限成长：抬高上限并回复等量生命值
                    case MAX_HP -> increaseMaxHP(newValue.minus(currentBase));
                    // 纯恢复型加成：受上限约束
                    case HP -> heal(newValue.minus(currentBase));
                    default -> this.updateState(bonus.stat(), newValue);
                }
            }
            // 配置了 fullHeal 的等级：在全部加成生效后回满，因此填满的是升级后的新上限
            if (result.fullHeal()) {
                restoreFullHP();
            }
            SkillFactory.getInstance().createByIds(result.learnedSkillIds()).forEach(this::learnSkill);
        }
    }

    // ==================== 技能 ====================

    /** 展示用：全部已拥有的技能（习得 + 装备附带），按获得顺序 */
    @Override
    public List<Skill> getSkills() {
        return skillBook.skills();
    }

    /** 习得技能（来源记为 {@link SkillSource.Learned}）；已拥有时只追加来源 */
    @Override
    public void learnSkill(Skill skill) {
        skillBook.grant(skill, SkillSource.Learned.INSTANCE);
    }

    /** 遗忘<b>习得</b>的技能；同一技能若还由装备提供，则仍然保留 */
    @Override
    public void forgetSkill(String skillId) {
        skillBook.revoke(skillId, SkillSource.Learned.INSTANCE);
    }

    /**
     * 玩家的战斗技能 = 开启的被动技能（整场生效）+ 就绪预设里逐回合排定的主动技能。
     *
     * <p>预设里引用的技能若当前不再拥有（例如装备卸下了），或者已不是主动技能，就跳过、该回合按普攻；
     * 预设本身不动，重新获得后自动恢复。
     */
    @Override
    public List<SkillCast> getBattleSkillCasts() {
        List<SkillCast> casts = new ArrayList<>();
        for (OwnedSkill owned : skillBook.entries()) {
            if (owned.skill().isPassive() && owned.isEnabled()) {
                casts.add(SkillCast.passive(owned.skill()));
            }
        }
        SkillPreset armed = presetBook.getArmed();
        if (armed != null) {
            // 按回合先后计数，超出技能每场可释放次数的格子按普攻处理（规则里的固定预设可能超排）
            Map<String, Integer> scheduled = new HashMap<>();
            for (int round = 0; round < armed.slotCount(); round++) {
                OwnedSkill owned = skillBook.get(armed.getSlot(round));
                if (owned == null || !owned.skill().isActive()) continue;
                int used = scheduled.getOrDefault(owned.skill().skillId(), 0);
                if (!owned.skill().canCastAgain(used)) continue;
                scheduled.put(owned.skill().skillId(), used + 1);
                casts.add(new SkillCast(owned.skill(), round));
            }
        }
        return casts;
    }

    public SkillBook getSkillBook() {
        return skillBook;
    }

    public SkillPresetBook getPresetBook() {
        return presetBook;
    }

    /** 塔规则是否允许玩家编辑预设；为 false 时预设只能切换就绪，不能修改 */
    public boolean isPresetEditable() {
        return presetEditable;
    }

    /**
     * 游戏中按数字键：激活对应预设；对<b>已激活</b>的那套再按一次则停用（回到全程普攻）。
     * 没编排过技能的预设激活后同样是全程普攻。调用方随后需要重算伤害。
     *
     * <p>停用必须保留：所有预设都编排了技能时，「不放技能」只能靠它回去。
     *
     * @return 调用后激活的预设下标，{@link SkillPresetBook#NONE} 表示没有激活任何预设（越界时不变）
     */
    public int togglePreset(int presetIndex) {
        return presetBook.toggleArmed(presetIndex);
    }

    /**
     * 激活某套预设（已激活时不变，不会像 {@link #togglePreset} 那样停用）。
     * 技能设置界面用它：界面是用来编辑的，切到哪套就激活哪套。
     *
     * @return 下标越界时返回 false
     */
    public boolean activatePreset(int presetIndex) {
        if (presetIndex < 0 || presetIndex >= presetBook.presetCount()) {
            return false;
        }
        presetBook.arm(presetIndex);
        return true;
    }

    /**
     * 把一个主动技能排进预设的某个回合。<b>放进栏位即就绪</b>：成功后该预设直接成为就绪预设，
     * 玩家不必再额外按键确认（魔塔里切技能很频繁，多一步确认太繁琐）。
     *
     * <p>技能在本预设中已排满每场可释放次数时拒绝（覆盖同一格不算新增一次）。
     */
    public PresetEditResult assignPresetSlot(int presetIndex, int round, String skillId) {
        if (!presetEditable) {
            return PresetEditResult.NOT_EDITABLE;
        }
        if (presetIndex < 0 || presetIndex >= presetBook.presetCount()
                || round < 0 || round >= presetBook.slotCount()) {
            return PresetEditResult.OUT_OF_RANGE;
        }
        OwnedSkill owned = skillBook.get(skillId);
        if (owned == null) {
            return PresetEditResult.NOT_OWNED;
        }
        if (!owned.skill().isActive()) {
            return PresetEditResult.NOT_ACTIVE;
        }
        if (!owned.skill().canCastAgain(presetBook.get(presetIndex).countOf(skillId, round))) {
            return PresetEditResult.CAST_LIMIT_REACHED;
        }
        presetBook.assign(presetIndex, round, skillId);
        presetBook.arm(presetIndex);
        return PresetEditResult.OK;
    }

    /**
     * 把预设的某个回合清回普攻，即把该技能从栏位上<b>卸下</b>。
     * 预设被清空后保持原有的激活状态——空预设本就等同全程普攻。
     */
    public PresetEditResult clearPresetSlot(int presetIndex, int round) {
        if (!presetEditable) {
            return PresetEditResult.NOT_EDITABLE;
        }
        if (presetIndex < 0 || presetIndex >= presetBook.presetCount()
                || round < 0 || round >= presetBook.slotCount()) {
            return PresetEditResult.OUT_OF_RANGE;
        }
        presetBook.clear(presetIndex, round);
        return PresetEditResult.OK;
    }

    // ==================== 装备 ====================

    /** 拿到过的全部装备（含已穿上的） */
    public List<Equipment> getEquipmentsOwned() {
        return Collections.unmodifiableList(equipmentsOwned);
    }

    public int getEquipSlotCount() {
        return equipmentsEquipped.length;
    }

    /** @return 该槽位上的装备，空槽或越界时为 null */
    public Equipment getEquipped(int slotIndex) {
        return slotIndex >= 0 && slotIndex < equipmentsEquipped.length ? equipmentsEquipped[slotIndex] : null;
    }

    /** @return 装备所在槽位，未穿上时为 -1 */
    public int slotOf(Equipment equipment) {
        for (int i = 0; i < equipmentsEquipped.length; i++) {
            if (equipmentsEquipped[i] == equipment) return i;
        }
        return -1;
    }

    public boolean isEquipped(Equipment equipment) {
        return slotOf(equipment) >= 0;
    }

    /** @return 第一个空槽，没有时为 -1 */
    public int firstEmptySlot() {
        for (int i = 0; i < equipmentsEquipped.length; i++) {
            if (equipmentsEquipped[i] == null) return i;
        }
        return -1;
    }

    public void setEquipSlotRule(EquipSlotRule rule) {
        this.equipSlotRule = rule == null ? EquipSlotRule.ALLOW_ALL : rule;
    }

    /**
     * 把背包里的装备穿到指定槽位。槽位上原有的装备先卸下；该装备若已穿在别的槽位，则挪过来。
     * 装备附带的技能随之授予。调用方随后需要重算伤害。
     *
     * @return 装备不在背包里、槽位越界或不满足 {@link EquipSlotRule} 时返回 false
     */
    public boolean equip(Equipment equipment, int slotIndex) {
        if (equipment == null || !equipmentsOwned.contains(equipment)
                || slotIndex < 0 || slotIndex >= equipmentsEquipped.length
                || !equipSlotRule.canEquip(equipment, slotIndex)) {
            return false;
        }
        int currentSlot = slotOf(equipment);
        if (currentSlot == slotIndex) {
            return true;
        }
        if (currentSlot >= 0) {
            // 同一件装备换槽：技能来源不变，只挪位置
            equipmentsEquipped[currentSlot] = null;
        }
        unequip(slotIndex);
        equipmentsEquipped[slotIndex] = equipment;
        if (currentSlot < 0) {
            SkillSource source = new SkillSource.FromEquipment(equipment);
            equipment.getSkills().forEach(skill -> skillBook.grant(skill, source));
        }
        return true;
    }

    /**
     * 卸下指定槽位的装备，撤销它附带的技能。生命上限因此降低时，把当前生命值钳到新上限。
     *
     * @return 被卸下的装备，空槽时为 null
     */
    public Equipment unequip(int slotIndex) {
        Equipment removed = getEquipped(slotIndex);
        if (removed == null) {
            return null;
        }
        equipmentsEquipped[slotIndex] = null;
        skillBook.revoke(new SkillSource.FromEquipment(removed));
        GameNumber maxHP = getMaxHP();
        if (getCurrentHP().compareTo(maxHP) > 0) {
            this.updateState(StateType.HP, maxHP);
        }
        return removed;
    }

    public long getCurrentGoldAmount() {
        return currentGoldAmount;
    }

    public void updateGoldAmount(long goldAmount){
        this.currentGoldAmount += goldAmount;
    }

    /**
     * 拾取道具：按道具大类分派。
     *
     * <p>{@link Item} 是封闭类型，这里的 switch 因而是穷尽的——将来新增一个大类，
     * 编译器会直接指着这里报错，不会出现「捡起来却什么都没发生」的静默漏处理。
     */
    public void gainItem(Item item){
        switch (item) {
            case GenericItem genericItem -> this.genericItemsOwned.add(genericItem);
            case AuxiliaryItem auxiliaryItem -> this.auxiliaryItemsOwned.add(auxiliaryItem);
            case Equipment equipment -> this.equipmentsOwned.add(equipment);
            case Key key -> {
                switch (key.getKeyColor()) {
                    case YELLOW -> this.yellow_Key.updateItemCount(1);
                    case RED -> this.red_Key.updateItemCount(1);
                    case BLUE -> this.blue_Key.updateItemCount(1);
                    default -> throw new IllegalArgumentException("Invalid key color: " + key.getKeyColor());
                }
            }
            case Portion portion -> heal(portion.getReplyAmount());
            case AbilityGem abilityGem -> {
                StateType effectedAbilityType = abilityGem.getEffectedAbilityType();
                GameNumber effectValue = abilityGem.getEffectValue();
                switch (effectedAbilityType) {
                    // 生命上限宝石：抬高上限并回复等量生命值
                    case MAX_HP -> increaseMaxHP(effectValue);
                    case HP -> heal(effectValue);
                    default -> this.updateState(effectedAbilityType,
                            this.getStateValue(effectedAbilityType).plus(effectValue));
                }
            }
        }
    }

    // ==================== 物品栏（供菜单读取） ====================

    /** 当前钥匙组的三把钥匙（黄 / 蓝 / 红），数量见各自的 {@code getItemCount} */
    public List<Key> getCurrentKeys() {
        return Stream.of(yellow_Key, blue_Key, red_Key).filter(Objects::nonNull).toList();
    }

    public List<GenericItem> getGenericItemsOwned() {
        return Collections.unmodifiableList(genericItemsOwned);
    }

    public List<AuxiliaryItem> getAuxiliaryItemsOwned() {
        return Collections.unmodifiableList(auxiliaryItemsOwned);
    }

    /** 是否持有解锁某项功能的辅助道具（例如怪物手册） */
    public boolean hasAuxiliary(AuxiliaryType type) {
        return auxiliaryItemsOwned.stream().anyMatch(item -> item.getAuxiliaryType() == type);
    }

    /**
     * 获取指定颜色钥匙的数量
     */
    public int getKeyCount(KeyColor color) {
        return switch (color) {
            case YELLOW -> yellow_Key != null ? yellow_Key.getItemCount() : 0;
            case RED -> red_Key != null ? red_Key.getItemCount() : 0;
            case BLUE -> blue_Key != null ? blue_Key.getItemCount() : 0;
            default -> 0;
        };
    }

    /**
     * 消耗一把指定颜色的钥匙，返回是否成功
     */
    public boolean consumeKey(KeyColor color) {
        int count = getKeyCount(color);
        if (count <= 0) {
            return false;
        }
        switch (color) {
            case YELLOW -> yellow_Key.updateItemCount(-1);
            case RED -> red_Key.updateItemCount(-1);
            case BLUE -> blue_Key.updateItemCount(-1);
        }
        return true;
    }

    public void playerDirectionChange() {
        this.setCurrentDirection(this.getCurrentDirection().getNextDirection());
    }
}
