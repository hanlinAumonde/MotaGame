package com.demo.mota.ui.screen.game.map;

/**
 * 行走时该显示行走循环里的哪一帧。
 *
 * <p>每走一格播两帧：前半程一帧、后半程下一帧；步数每加一，起点往后挪两帧。
 * 行走循环的帧序由精灵目录给出（{@code sprites.json} 的 {@code walk}），RPG Maker XP 式的 4 帧行走图写成
 * {@code [1, 2, 3, 0]}（迈一只脚 / 站立 / 迈另一只脚 / 站立）：第 0 步播 1、2，第 1 步播 3、0，第 2 步又是 1、2——
 * 左右脚交替，每步末尾都落回一帧站立姿势。
 */
public final class WalkCycle {

    /** 显示静止帧，而不是行走循环里的某一帧 */
    public static final int IDLE = -1;

    private WalkCycle() {
    }

    /**
     * @param stepIndex   这是连续行走的第几步（只增不减的计数器）
     * @param progress    这一步走了多少，{@code [0, 1)}
     * @param cycleLength 行走循环的帧数；不足两帧时没有走路动画，始终取静止帧
     * @return 行走循环里的下标 {@code [0, cycleLength)}，或 {@link #IDLE}
     */
    public static int frameAt(int stepIndex, double progress, int cycleLength) {
        if (cycleLength < 2) {
            return IDLE;
        }
        return Math.floorMod(2 * stepIndex + (progress < 0.5 ? 0 : 1), cycleLength);
    }
}
