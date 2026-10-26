package com.demo.mota.world.component;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.pathfinding.CellMoveComponent;
import com.almasb.fxgl.texture.AnimatedTexture;
import com.demo.mota.app.services.CharacterSprite;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.map.Position;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.function.Function;

import static com.demo.mota.world.WorldGrid.CELL;

/**
 * 地图上的主角：FXGL 的 {@link CellMoveComponent} 负责格间平滑移动，{@link AnimatedTexture} 播静止 / 行走帧。
 *
 * <ul>
 *   <li>{@link #step}：走到相邻格。行走通道用 {@code loopNoOverride} 播放——连续行走时同一通道不重启，
 *       XP 式 {@code [1, 2, 3, 0]} 帧序循环下去恰好是「每步两帧、左右脚交替」；</li>
 *   <li>到达后回调「走完一步」（{@link #setOnStepFinished}）。回调里若又开始了下一步，就不切回静止帧，免得闪一下；</li>
 *   <li>{@link #place}：直接落位（换图、跳转、撤销），打断正在走的一步且不回调；</li>
 *   <li>{@link #face}：只改朝向（转身、撞墙、打怪、开门），正在走的那一步照常走完。</li>
 * </ul>
 * 行走图缺失时退回占位形状（蓝色圆 + 方向三角），一样能移动，只是没有走路帧。
 */
public final class PlayerComponent extends Component {

    private final CharacterSprite sprite;
    private final Function<Direction, Node> placeholder;

    private CellMoveComponent move;
    private AnimatedTexture texture;
    private final Group fallback = new Group();

    private Direction facing = Direction.DOWN;
    private boolean walking;
    private Runnable onStepFinished = () -> { };

    /**
     * @param sprite      主角的行走图（可为 {@code null}）
     * @param placeholder 没有行走图时，按朝向给出占位形状
     */
    public PlayerComponent(CharacterSprite sprite, Function<Direction, Node> placeholder) {
        this.sprite = sprite;
        this.placeholder = placeholder;
    }

    @Override
    public void onAdded() {
        move = entity.getComponent(CellMoveComponent.class);
        if (sprite != null && sprite.idleChannel(facing) != null) {
            texture = new AnimatedTexture(sprite.idleChannel(facing));
            texture.setSmooth(false);
            // 帧比格子高（64×66）：上移让脚踩在格子底边
            texture.setTranslateY(CELL - sprite.frameHeight());
            entity.getViewComponent().addChild(texture);
        } else {
            entity.getViewComponent().addChild(fallback);
        }
        showIdle();
    }

    public void setOnStepFinished(Runnable listener) {
        this.onStepFinished = listener == null ? () -> { } : listener;
    }

    public boolean isWalking() {
        return walking;
    }

    /** 直接落到某格并面向 {@code direction}，打断正在走的一步（不回调） */
    public void place(Position pos, Direction direction) {
        move.setPositionToCell(pos.x(), pos.y());
        walking = false;
        facing = direction;
        showIdle();
    }

    /** 从当前所在（可能还在上一步的半路）走向相邻的 {@code to} */
    public void step(Position to, Direction direction) {
        facing = direction;
        walking = true;
        move.moveToCell(to.x(), to.y());
        showWalk();
    }

    /** 只改朝向 */
    public void face(Direction direction) {
        if (direction == facing) {
            return;
        }
        facing = direction;
        if (walking) {
            showWalk();
        } else {
            showIdle();
        }
    }

    @Override
    public void onUpdate(double tpf) {
        // CellMoveComponent 先加入、先更新：这一帧它刚到达时，这里就能看到
        if (walking && move.isAtDestination()) {
            walking = false;
            onStepFinished.run();
            if (!walking) {
                showIdle();
            }
        }
    }

    private void showIdle() {
        if (texture != null) {
            // 行走图里没有这个朝向的行就保持原样
            if (sprite.idleChannel(facing) != null) {
                texture.playAnimationChannel(sprite.idleChannel(facing));
            }
        } else {
            fallback.getChildren().setAll(placeholder.apply(facing));
        }
    }

    private void showWalk() {
        if (texture != null) {
            if (sprite.walkChannel(facing) != null) {
                texture.loopNoOverride(sprite.walkChannel(facing));
            }
        } else {
            fallback.getChildren().setAll(placeholder.apply(facing));
        }
    }

    @Override
    public boolean isComponentInjectionRequired() {
        return false;
    }
}
