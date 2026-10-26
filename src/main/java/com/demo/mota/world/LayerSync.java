package com.demo.mota.world;

import com.demo.mota.engine.map.Position;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * 地图一层（地砖 / 地物 / 道具 / 怪物）的显示同步：把「位置 → 模型对象」的当前状态与已显示的视图逐格对照，
 * 只增删变了的格子。
 *
 * <p>默认按<b>对象身份</b>比较（{@link #LayerSync(BiFunction, BiConsumer)}）：引擎开门 / 揭暗墙时整块替换 Tile、
 * 击败怪物 / 拾取道具时直接移除，不会原地改一个对象让它「看起来」变样，因此身份不同即需要换视图。
 * 模型是值（如地砖层的精灵 id）时用 {@link #byValue} 按 {@code equals} 比较。
 * 本类不认识 FXGL，视图类型由泛型 {@code V} 决定（实体、或实体 + 组件）。
 *
 * <p>销毁回调带上格子坐标：地图显示据此查这一格有没有对应的领域事件（开门、击败、拾取），
 * 有就播一段消失动画再移除，没有（撤销、读档、将来的脚本）就立即移除。
 *
 * @param <T> 模型类型
 * @param <V> 视图类型
 */
public final class LayerSync<T, V> {

    private record Shown<T, V>(T source, V view) {}

    private final Map<Position, Shown<T, V>> shown = new HashMap<>();
    private final BiFunction<Position, T, V> spawn;
    private final BiConsumer<Position, V> despawn;
    /** 已显示的模型与当前模型是否算「同一个」 */
    private final BiPredicate<T, T> same;

    /**
     * 按对象身份比较。
     *
     * @param spawn   为某一格的模型对象创建视图
     * @param despawn 销毁某一格的视图
     */
    public LayerSync(BiFunction<Position, T, V> spawn, BiConsumer<Position, V> despawn) {
        this(spawn, despawn, (shown, current) -> shown == current);
    }

    private LayerSync(BiFunction<Position, T, V> spawn, BiConsumer<Position, V> despawn, BiPredicate<T, T> same) {
        this.spawn = spawn;
        this.despawn = despawn;
        this.same = same;
    }

    /** 按 {@code equals} 比较：模型是值而不是有身份的对象时用（如地砖层的精灵 id） */
    public static <T, V> LayerSync<T, V> byValue(BiFunction<Position, T, V> spawn, BiConsumer<Position, V> despawn) {
        return new LayerSync<>(spawn, despawn, Objects::equals);
    }

    /** 对照当前状态：消失或被换掉的格子销毁视图，新出现或被换上的格子创建视图 */
    public void sync(Map<Position, ? extends T> current) {
        Iterator<Map.Entry<Position, Shown<T, V>>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Position, Shown<T, V>> entry = it.next();
            T now = current.get(entry.getKey());
            if (now == null || !same.test(entry.getValue().source(), now)) {
                despawn.accept(entry.getKey(), entry.getValue().view());
                it.remove();
            }
        }
        current.forEach((pos, source) -> {
            if (source != null && !shown.containsKey(pos)) {
                shown.put(pos, new Shown<>(source, spawn.apply(pos, source)));
            }
        });
    }

    /** 销毁全部视图（整张地图换掉时） */
    public void clear() {
        shown.forEach((pos, s) -> despawn.accept(pos, s.view()));
        shown.clear();
    }

    /** 当前显示中的全部视图 */
    public Collection<V> views() {
        return shown.values().stream().map(Shown::view).toList();
    }

    public int size() {
        return shown.size();
    }
}
