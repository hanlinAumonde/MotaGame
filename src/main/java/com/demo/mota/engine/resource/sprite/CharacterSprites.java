package com.demo.mota.engine.resource.sprite;

import com.demo.mota.engine.enums.Direction;
import javafx.scene.image.Image;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一个角色从行走图里切出来的全部帧：朝向 → 该朝向的帧序列（按播放顺序）。
 *
 * <p><b>动画系统的扩展入口就在这里</b>。当前游戏没有动画，渲染只取
 * {@link #idle(Direction)}；但整行的行走帧照样切出来缓存着，因此日后加动画系统时
 * 只需要新增一个「按时间推进帧下标」的组件去调 {@link #frame(Direction, int)}，
 * 资源侧（配置格式、切分逻辑、缓存结构）一行都不用改。
 */
public final class CharacterSprites {

    private final Map<Direction, List<Image>> frames;
    private final int idleFrame;

    public CharacterSprites(Map<Direction, List<Image>> frames, int idleFrame) {
        Map<Direction, List<Image>> copy = new EnumMap<>(Direction.class);
        if (frames != null) {
            frames.forEach((direction, list) -> {
                if (direction != null && list != null && !list.isEmpty()) {
                    copy.put(direction, List.copyOf(list));
                }
            });
        }
        this.frames = Collections.unmodifiableMap(copy);
        this.idleFrame = Math.max(0, idleFrame);
    }

    /** 静止帧，当前渲染唯一用到的帧 */
    public Image idle(Direction direction) {
        return frame(direction, idleFrame);
    }

    /**
     * 取某朝向的第 index 帧，下标按帧数取模循环，
     * 因此动画侧可以直接喂一个只增不减的计数器。
     *
     * @return 该朝向没有任何帧时返回 {@code null}（渲染侧走 fallback）
     */
    public Image frame(Direction direction, int index) {
        List<Image> list = frames.get(direction);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(Math.floorMod(index, list.size()));
    }

    public int frameCount(Direction direction) {
        List<Image> list = frames.get(direction);
        return list == null ? 0 : list.size();
    }

    /** 是否有多于一帧可播；没有动画素材的角色（单帧站立图）在此返回 false */
    public boolean isAnimated() {
        return frames.values().stream().anyMatch(list -> list.size() > 1);
    }

    public int getIdleFrame() {
        return idleFrame;
    }
}
