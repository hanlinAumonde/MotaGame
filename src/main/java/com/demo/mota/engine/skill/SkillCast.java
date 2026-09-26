package com.demo.mota.engine.skill;

/**
 * 一次战斗中的一次技能生效：技能本体 + 释放回合。
 *
 * <p>被动技能整场生效，{@code castRound = }{@link #PASSIVE_ROUND}；
 * 主动技能由预设排在具体回合上，{@code 0} 表示战前（进入第一回合之前），{@code 1..N} 为对应回合。
 * 同一个主动技能排在多个回合，就是多条 {@code SkillCast}。
 */
public record SkillCast(Skill skill, int castRound) {

    public static final int PASSIVE_ROUND = -1;

    public static SkillCast passive(Skill skill) {
        return new SkillCast(skill, PASSIVE_ROUND);
    }

    public boolean isPassive() {
        return castRound < 0;
    }
}
