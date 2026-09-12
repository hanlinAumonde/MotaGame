package com.demo.mota.engine.skill.effect;

/**
 * 技能配置中的作用对象，以<b>技能持有者</b>为参照。
 *
 * <p>写成相对视角（{@code SELF} / {@code OPPONENT}）的技能可以原样挂到玩家或怪物身上，
 * 这正是技能模型不绑定持有者的意义所在；确实只对某一方生效的效果则直接写
 * {@code PLAYER} / {@code MONSTER}。
 */
public enum EffectTarget {
    SELF,
    OPPONENT,
    PLAYER,
    MONSTER;

    /** 结合持有者所在阵营，折算成绝对阵营 */
    public BattleSide resolve(BattleSide ownerSide) {
        return switch (this) {
            case SELF -> ownerSide;
            case OPPONENT -> ownerSide.opposite();
            case PLAYER -> BattleSide.PLAYER;
            case MONSTER -> BattleSide.MONSTER;
        };
    }
}
