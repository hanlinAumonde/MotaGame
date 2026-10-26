package com.demo.mota.app.services;

import com.almasb.fxgl.texture.AnimationChannel;
import com.almasb.fxgl.texture.AnimationChannelData;
import com.demo.mota.engine.enums.Direction;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一个角色切好的帧（已放大）：
 * <ul>
 *   <li>每个朝向一张<b>静止帧</b>图片——状态栏头像、装备 / 技能界面的立绘用；</li>
 *   <li>每个朝向一对 FXGL <b>动画通道</b>（{@link AnimationChannel}）——静止、行走，地图上的主角用
 *       {@code AnimatedTexture} 播放。</li>
 * </ul>
 * 行走通道按 {@code sprites.json} 的 {@code walk} 帧序构造，每帧 = 半步：XP 式的 {@code [1, 2, 3, 0]}
 * 循环播放时恰好是「每步两帧、左右脚交替、每步落回站立帧」。
 */
public final class CharacterSprite {

    private final Map<Direction, Image> idleImages;
    private final Map<Direction, AnimationChannel> idleChannels;
    private final Map<Direction, AnimationChannel> walkChannels;
    private final double stepSeconds;
    private final int frameWidth;
    private final int frameHeight;

    CharacterSprite(Map<Direction, Image> idleImages, Map<Direction, AnimationChannel> idleChannels,
                    Map<Direction, AnimationChannel> walkChannels, double stepSeconds, int frameWidth, int frameHeight) {
        this.idleImages = new EnumMap<>(idleImages);
        this.idleChannels = new EnumMap<>(idleChannels);
        this.walkChannels = new EnumMap<>(walkChannels);
        this.stepSeconds = stepSeconds;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    /** 该朝向的静止帧；该朝向没有切出任何帧时为 {@code null}（绘制走纯色 fallback） */
    public Image idle(Direction direction) {
        return idleImages.get(direction);
    }

    /** 该朝向的静止通道（一帧）；没有时为 {@code null} */
    public AnimationChannel idleChannel(Direction direction) {
        return idleChannels.get(direction);
    }

    /** 该朝向的行走通道；行走帧不足两帧时没有走路动画，退回静止通道 */
    public AnimationChannel walkChannel(Direction direction) {
        return walkChannels.getOrDefault(direction, idleChannels.get(direction));
    }

    /** 走一格的秒数（精灵目录的 {@code stepSeconds}） */
    public double stepSeconds() {
        return stepSeconds;
    }

    /** 单帧尺寸（已放大），如 64×66 */
    public int frameWidth() {
        return frameWidth;
    }

    public int frameHeight() {
        return frameHeight;
    }

    /**
     * 把一行里依次用到的列（如 {@code [1, 2, 3, 0]}）换成 FXGL 的帧区间：连续的列合成一段，
     * 即 {@code (1..3) + (0..0)}。帧号 = 行 × 每行帧数 + 列，与 {@link AnimationChannel} 按整张图集编号的规则一致。
     * 用 {@link AnimationChannelData} 而不是 FXGL 主构造函数的 {@code kotlin.Pair}，对 Java 更友好。
     */
    static List<AnimationChannelData> frameRuns(List<Integer> columns, int row, int framesPerRow,
                                                int frameWidth, int frameHeight) {
        List<AnimationChannelData> runs = new ArrayList<>();
        int i = 0;
        while (i < columns.size()) {
            int start = columns.get(i);
            int end = start;
            while (i + 1 < columns.size() && columns.get(i + 1) == end + 1) {
                end++;
                i++;
            }
            int base = row * framesPerRow;
            runs.add(new AnimationChannelData(base + start, base + end, frameWidth, frameHeight));
            i++;
        }
        return runs;
    }
}
