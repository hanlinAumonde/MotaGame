package com.demo.mota.ui.screen.game.map;

/**
 * 行走时该显示哪一帧。
 *
 * <p>每走一格播两帧：前半程是「迈步帧」、后半程是它的下一帧；步数每加一，起点往后挪两帧。
 * 对 RPG Maker XP 式的 4 帧行走图（站立 / 迈左脚 / 站立 / 迈右脚，静止帧为第 0 帧）而言：
 * 第 0 步播 1、2，第 1 步播 3、0，第 2 步又是 1、2——左右脚交替，每步末尾都落回一帧站立姿势。
 * 帧数不是 4 的行走图同样按「起点 + 2 × 步数」循环取帧，只是不再有左右脚的含义。
 */
public final class WalkCycle {

    private WalkCycle() {
    }

    /**
     * @param stepIndex  这是连续行走的第几步（只增不减的计数器）
     * @param progress   这一步走了多少，{@code [0, 1)}
     * @param idleFrame  静止帧下标
     * @param frameCount 该朝向的帧数；不足两帧时始终取静止帧
     * @return 帧下标，范围 {@code [0, frameCount)}
     */
    public static int frameAt(int stepIndex, double progress, int idleFrame, int frameCount) {
        if (frameCount < 2) {
            return idleFrame;
        }
        int offset = 1 + 2 * stepIndex + (progress < 0.5 ? 0 : 1);
        return Math.floorMod(idleFrame + offset, frameCount);
    }
}
