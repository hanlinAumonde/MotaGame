package com.demo.mota.engine.skill.cost;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillCast;
import com.demo.mota.engine.state.AbstractCharacterState;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * costId → {@link SkillCostHandler} 的注册表，与 {@code SkillEffectRegistry} 同构。
 *
 * <p>当前只内置 {@code none}（不消耗）。技能释放要付出什么——魔法、道具还是别的——
 * 由将来注册进来的 handler 决定，技能配置里写对应的 {@code costId} 即可。
 *
 * <p>未注册的 costId 视为<b>付不起</b>：宁可让技能放不出来，也不要让一条配错的消耗白白生效。
 */
public final class SkillCostRegistry {

    private static class Holder {
        private static final SkillCostRegistry INSTANCE = new SkillCostRegistry();
    }

    public static SkillCostRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private static final SkillCostHandler FREE = new SkillCostHandler() {
        @Override
        public boolean canAfford(SkillCostContext context) {
            return true;
        }

        @Override
        public void pay(SkillCostContext context) {}
    };

    private final Map<String, SkillCostHandler> handlers = new ConcurrentHashMap<>();

    private SkillCostRegistry() {
        register(SkillCostSpec.NONE_ID, FREE);
    }

    public void register(String costId, SkillCostHandler handler) {
        if (costId == null || costId.isBlank() || handler == null) {
            throw new IllegalArgumentException("Invalid skill cost registration: " + costId);
        }
        handlers.put(costId.trim(), handler);
    }

    public void unregister(String costId) {
        if (!SkillCostSpec.NONE_ID.equals(costId)) {
            handlers.remove(costId);
        }
    }

    public Set<String> registeredCostIds() {
        return Set.copyOf(handlers.keySet());
    }

    /** 被动技能（{@code castRound < 0}）不走消耗，恒为 true */
    public boolean canAfford(SkillCast cast, AbstractCharacterState owner) {
        if (cast.isPassive()) {
            return true;
        }
        SkillCostHandler handler = handlers.get(cast.skill().cost().costId());
        return handler != null && handler.canAfford(contextOf(cast, owner));
    }

    public void pay(SkillCast cast, AbstractCharacterState owner) {
        if (cast.isPassive()) {
            return;
        }
        SkillCostHandler handler = handlers.get(cast.skill().cost().costId());
        if (handler != null) {
            handler.pay(contextOf(cast, owner));
        }
    }

    private static SkillCostContext contextOf(SkillCast cast, AbstractCharacterState owner) {
        Skill skill = cast.skill();
        return new SkillCostContext(skill, skill.cost().params(), owner, cast.castRound());
    }
}
