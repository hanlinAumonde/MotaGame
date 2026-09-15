package com.demo.mota.engine.resource.sprite.builtin;

import com.demo.mota.engine.resource.sprite.SheetSlicer;

import java.util.function.BiConsumer;

/**
 * 内置切法的登记处，由 {@code SheetSlicerRegistry} 在初始化时调用。
 *
 * <p>新增一种切法时在 {@link #registerAll} 里加一行；
 * 游戏之外（如 mod 加载器）则直接调注册表的 {@code register}，不必改动本类。
 */
public final class BuiltinSheetSlicers {

    public static final String TYPE_TILE = "tile";
    public static final String TYPE_CHARACTER = "character";

    private BuiltinSheetSlicers() {
    }

    public static void registerAll(BiConsumer<String, SheetSlicer> registrar) {
        registrar.accept(TYPE_TILE, new TileSheetSlicer());
        registrar.accept(TYPE_CHARACTER, new CharacterSheetSlicer());
    }
}
