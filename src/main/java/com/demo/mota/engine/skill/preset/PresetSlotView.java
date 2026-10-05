package com.demo.mota.engine.skill.preset;

import com.demo.mota.engine.skill.Skill;

/**
 * 激活预设里的一格（展示用）：排的是什么技能、哪一回合、这场战斗里是否真的会放。
 *
 * <p>{@code effective == false} 的格子在战斗中按普攻处理：技能暂时不在手上（装备卸下了），
 * 或超出了该技能每场的释放次数。界面据此画灰而不是直接隐藏，让玩家看得出预设本身还在。
 */
public record PresetSlotView(Skill skill, int round, boolean effective) {}
