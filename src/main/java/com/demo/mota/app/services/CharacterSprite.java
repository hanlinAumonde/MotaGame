package com.demo.mota.app.services;

import com.demo.mota.engine.enums.Direction;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一个角色切好的帧（已放大）：每个朝向一张静止帧 + 一组行走帧（按 {@code sprites.json} 里 {@code walk} 的顺序）。
 *
 * <p>谁来播、怎么播不归这里管：现在由地图的玩家组件按步数取帧，之后换成 FXGL 的 {@code AnimatedTexture}
 * 时直接用这些帧建动画通道。
 */
public final class CharacterSprite {

    private final Map<Direction, Image> idle;
    private final Map<Direction, List<Image>> walk;

    CharacterSprite(Map<Direction, Image> idle, Map<Direction, List<Image>> walk) {
        this.idle = new EnumMap<>(idle);
        this.walk = new EnumMap<>(Direction.class);
        walk.forEach((direction, frames) -> this.walk.put(direction, List.copyOf(frames)));
    }

    /** 该朝向的静止帧；该朝向没有切出任何帧时为 {@code null}（绘制走纯色 fallback） */
    public Image idle(Direction direction) {
        return idle.get(direction);
    }

    /** 行走循环里有几帧；不足两帧的角色没有走路动画 */
    public int walkLength(Direction direction) {
        List<Image> frames = walk.get(direction);
        return frames == null ? 0 : frames.size();
    }

    /** 行走循环的第 {@code index} 帧，下标按帧数取模（可以直接喂只增不减的计数器）；没有行走帧时退回静止帧 */
    public Image walkFrame(Direction direction, int index) {
        List<Image> frames = walk.get(direction);
        if (frames == null || frames.isEmpty()) {
            return idle(direction);
        }
        return frames.get(Math.floorMod(index, frames.size()));
    }
}
