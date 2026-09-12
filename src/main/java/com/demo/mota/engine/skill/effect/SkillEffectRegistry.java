package com.demo.mota.engine.skill.effect;

import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.effect.builtin.BuiltinSkillEffects;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * effectId → {@link SkillEffectProvider} 的注册表，技能系统的扩展点。
 *
 * <p>技能配置里只写 effectId 与参数，具体由哪段代码实现由本表决定：
 * <ul>
 *   <li>内置机制在初始化时由 {@link BuiltinSkillEffects} 注册；</li>
 *   <li>新机制调用 {@link #register(String, SkillEffectProvider)} 追加即可，
 *       战斗层（{@code BattleSimulator}）与展示层（怪物手册）都不必改动；</li>
 *   <li>将来支持外部脚本时，脚本加载器只需把脚本包装成 {@link SkillEffectProvider}
 *       再注册进来——这也是本表用 {@link ConcurrentHashMap} 而非静态 switch 的原因。</li>
 * </ul>
 *
 * <p>未注册的 effectId 不会抛异常：技能照常在手册里展示，只是不产生战斗效果，
 * 这样一条配错的技能不会让整局游戏打不开。
 */
public final class SkillEffectRegistry {

    private static class Holder {
        private static final SkillEffectRegistry INSTANCE = new SkillEffectRegistry();
    }

    public static SkillEffectRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, SkillEffectProvider> providers = new ConcurrentHashMap<>();

    private SkillEffectRegistry() {
        BuiltinSkillEffects.registerAll(this::register);
    }

    /**
     * 注册（或覆盖）一个效果机制。
     *
     * @param effectId 技能配置中 {@code effectId} 字段的取值
     * @param provider 该机制的效果构造器
     */
    public void register(String effectId, SkillEffectProvider provider) {
        if (effectId == null || effectId.isBlank() || provider == null) {
            throw new IllegalArgumentException("Invalid skill effect registration: " + effectId);
        }
        providers.put(effectId.trim(), provider);
    }

    public void unregister(String effectId) {
        providers.remove(effectId);
    }

    public boolean isRegistered(String effectId) {
        return effectId != null && providers.containsKey(effectId.trim());
    }

    public Set<String> registeredEffectIds() {
        return Set.copyOf(providers.keySet());
    }

    /**
     * 按上下文构造技能对应的战斗效果。
     *
     * @return 技能未配效果、effectId 未注册、或该上下文下不产生效果时返回 {@code null}
     */
    public BattleEffect create(SkillEffectContext context) {
        Skill skill = context.skill();
        if (skill == null || !skill.hasEffect()) {
            return null;
        }
        SkillEffectProvider provider = providers.get(skill.effectId().trim());
        return provider == null ? null : provider.create(context);
    }
}
