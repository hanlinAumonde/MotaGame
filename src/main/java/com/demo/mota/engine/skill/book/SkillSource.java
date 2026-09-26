package com.demo.mota.engine.skill.book;

import com.demo.mota.engine.Item.Equipment;

/**
 * 玩家持有某个技能的<b>来源</b>。同一个技能可以同时有多个来源，
 * 只有全部来源都撤销后技能才真正失去——自己学会的技能和装备附带的撞了同一个时，
 * 卸下装备不会把它一起带走。
 */
public sealed interface SkillSource {

    /** 习得（升级、以及将来的事件 / 技能书等途径） */
    record Learned() implements SkillSource {
        public static final Learned INSTANCE = new Learned();
    }

    /**
     * 由某件装备附带。按<b>装备实例</b>区分：两件同名装备是两个来源，
     * 卸下其中一件不影响另一件带来的同一技能。
     */
    record FromEquipment(Equipment equipment) implements SkillSource {
        @Override
        public boolean equals(Object o) {
            return o instanceof FromEquipment(Equipment equipment1) && equipment1 == this.equipment;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(equipment);
        }
    }
}
