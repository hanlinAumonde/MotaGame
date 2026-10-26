package com.demo.mota.world.fx;

import com.almasb.fxgl.entity.Entity;
import com.demo.mota.world.EntityType;
import com.demo.mota.world.RemovalHints.Exit;
import com.demo.mota.world.component.MonsterComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.animationBuilder;
import static com.demo.mota.world.WorldGrid.CELL;

/**
 * 地图元素的消失动画（无素材版：只用补间，不需要额外的图）。动画播完再把实体移出世界。
 *
 * <table>
 *   <caption>消失方式</caption>
 *   <tr><th>方式</th><th>表现</th></tr>
 *   <tr><td>开门</td><td>门（地物实体）以上沿为原点纵向收拢并淡出；底下的地砖本来就是独立实体，原样不动</td></tr>
 *   <tr><td>暗墙 / 机关墙 / 检测格</td><td>盖一层白色闪一下后淡出</td></tr>
 *   <tr><td>击败怪物</td><td>伤害数字立即消失，本体缩小并淡出</td></tr>
 *   <tr><td>拾取道具</td><td>上浮并淡出</td></tr>
 * </table>
 *
 * 怪物与道具保持原来的层次（主角走上道具格时，上浮的道具在主角身后）。
 * 动画用 {@code animationBuilder()}，绑在游戏场景的更新上：有子场景盖住时随之暂停。
 * 特效不阻塞操作——实体已经不属于任何一层，新的同步不会碰它。
 * 将来有门开启的帧动画素材时，在这里换成播精灵目录里的动画通道。
 */
public final class MapEffects {

    private static final Duration DOOR = Duration.seconds(0.25);
    private static final Duration WALL = Duration.seconds(0.3);
    private static final Duration DEFEAT = Duration.seconds(0.3);
    private static final Duration PICK = Duration.seconds(0.3);

    private MapEffects() {
    }

    /**
     * 让 {@code entity} 按 {@code exit} 的方式退场。
     *
     * @param onDone 动画播完时调用（调用方在这里把实体移出世界）
     */
    public static void play(Exit exit, Entity entity, Runnable onDone) {
        entity.setType(EntityType.EFFECT);
        switch (exit) {
            case DOOR_OPENED -> doorOpened(entity, onDone);
            case WALL_OPENED -> wallOpened(entity, onDone);
            case MONSTER_DEFEATED -> monsterDefeated(entity, onDone);
            case ITEM_PICKED -> itemPicked(entity, onDone);
        }
    }

    private static void doorOpened(Entity door, Runnable onDone) {
        animationBuilder().duration(DOOR).scale(door)
                .origin(new Point2D(CELL / 2.0, 0))
                .to(new Point2D(1, 0.05))
                .buildAndPlay();
        animationBuilder().duration(DOOR).onFinished(onDone).fadeOut(door).buildAndPlay();
    }

    private static void wallOpened(Entity wall, Runnable onDone) {
        Rectangle flash = new Rectangle(CELL, CELL, Color.WHITE);
        flash.setOpacity(0.85);
        wall.getViewComponent().addChild(flash);
        animationBuilder().duration(WALL).fadeOut(flash).buildAndPlay();
        animationBuilder().duration(WALL).onFinished(onDone).fadeOut(wall).buildAndPlay();
    }

    private static void monsterDefeated(Entity monster, Runnable onDone) {
        if (monster.hasComponent(MonsterComponent.class)) {
            monster.getComponent(MonsterComponent.class).damageText().setVisible(false);
        }
        animationBuilder().duration(DEFEAT).scale(monster)
                .origin(new Point2D(CELL / 2.0, CELL / 2.0))
                .to(new Point2D(0.3, 0.3))
                .buildAndPlay();
        animationBuilder().duration(DEFEAT).onFinished(onDone).fadeOut(monster).buildAndPlay();
    }

    private static void itemPicked(Entity item, Runnable onDone) {
        animationBuilder().duration(PICK).translate(item)
                .to(item.getPosition().add(0, -CELL * 0.4))
                .buildAndPlay();
        animationBuilder().duration(PICK).onFinished(onDone).fadeOut(item).buildAndPlay();
    }
}
