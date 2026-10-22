package com.demo.mota.ui.screen.game.map;

import com.demo.mota.engine.map.Position;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 地图一层（地块 / 道具 / 怪物）的显示同步：把「位置 → 模型对象」的当前状态与已显示的视图逐格对照，
 * 只增删变了的格子。
 *
 * <p>按<b>对象身份</b>比较：引擎开门 / 揭暗墙时整块替换 Tile、击败怪物 / 拾取道具时直接移除，
 * 不会原地改一个对象让它「看起来」变样，因此身份不同即需要换视图。
 * 本类不认识 FXGL，视图类型由泛型 {@code V} 决定（实体、或实体 + 画布的组合）。
 *
 * @param <T> 模型类型
 * @param <V> 视图类型
 */
final class LayerSync<T, V> {

    private record Shown<T, V>(T source, V view) {}

    private final Map<Position, Shown<T, V>> shown = new HashMap<>();
    private final BiFunction<Position, T, V> spawn;
    private final Consumer<V> despawn;

    /**
     * @param spawn   为某一格的模型对象创建视图
     * @param despawn 销毁一个视图
     */
    LayerSync(BiFunction<Position, T, V> spawn, Consumer<V> despawn) {
        this.spawn = spawn;
        this.despawn = despawn;
    }

    /** 对照当前状态：消失或被换掉的格子销毁视图，新出现或被换上的格子创建视图 */
    void sync(Map<Position, ? extends T> current) {
        Iterator<Map.Entry<Position, Shown<T, V>>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Position, Shown<T, V>> entry = it.next();
            if (current.get(entry.getKey()) != entry.getValue().source()) {
                despawn.accept(entry.getValue().view());
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
    void clear() {
        shown.values().forEach(s -> despawn.accept(s.view()));
        shown.clear();
    }

    /** 当前显示中的全部视图 */
    Collection<V> views() {
        return shown.values().stream().map(Shown::view).toList();
    }

    int size() {
        return shown.size();
    }
}
