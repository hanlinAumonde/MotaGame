package com.demo.mota.engine.skill.effect;

import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.state.AbstractCharacterState;

/**
 * 构造技能效果时可用的全部上下文。
 *
 * <p>{@link com.demo.mota.engine.battle.BattleEffect} 只看得见战斗内部的数值，
 * 而「地图上还有几只亡灵」「持有者身上有什么标签」这类信息属于战斗之外；
 * 因此把它们集中在这里，由 {@link SkillEffectProvider} 在<b>构造效果时</b>读取并折算成
 * 具体数值，效果本身依旧只做纯粹的战斗运算。
 *
 * @param skill     技能本体（效果实现一般只需要 {@code params}，保留它便于日志与调试）
 * @param params    技能配置中的 {@code params}，永不为 null
 * @param ownerSide 持有者在本场战斗中的阵营
 * @param owner     技能持有者
 * @param opponent  战斗的另一方
 * @param map       战斗发生时的当前地图；怪物构造阶段尚无地图，此时为 null，
 *                  依赖地图的效果需自行判空（随后的 {@code recalculateAllDamage} 会带着地图重算）
 */
public record SkillEffectContext(Skill skill, SkillParams params, BattleSide ownerSide,
                                 AbstractCharacterState owner, AbstractCharacterState opponent,
                                 GameMap map) {
}
