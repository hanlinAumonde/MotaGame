package com.demo.mota.engine.resource.sprite.builtin;

import com.demo.mota.engine.resource.sprite.SheetParams;
import com.demo.mota.engine.resource.sprite.SheetSlicer;
import com.demo.mota.engine.resource.sprite.SpriteSheet;
import com.demo.mota.engine.resource.sprite.SpriteSheetConfig;
import com.demo.mota.engine.resource.sprite.SpriteStore;
import javafx.scene.image.Image;

import java.util.Map;

/**
 * {@code type: "tile"} —— 命名元件图。
 *
 * <p>网格的含义是「每一格是一个独立的、有名字的元件」，配置里逐个列出
 * {@code resourceId → (col, row)}，切出来的子图按 resourceId 存入 store，
 * 供地图 JSON 里的 {@code bgResourceId} / {@code wallResourceId} 等字段引用。
 *
 * <pre>{@code
 * "tiles": { "bg_default": { "col": 3, "row": 1 }, ... }
 * }</pre>
 */
public final class TileSheetSlicer implements SheetSlicer {

    private static final String KEY_TILES = "tiles";

    @Override
    public void slice(SpriteSheet sheet, SpriteSheetConfig config, SpriteStore store) {
        Map<String, SheetParams> tiles = config.params().getObjectMap(KEY_TILES);
        if (tiles.isEmpty()) {
            System.err.println("[SpriteSheet] 配置未声明任何 tiles，已跳过：" + config.sourcePath());
            return;
        }
        for (Map.Entry<String, SheetParams> entry : tiles.entrySet()) {
            String resourceId = entry.getKey();
            int col = entry.getValue().getInt("col", -1);
            int row = entry.getValue().getInt("row", -1);
            Image sub = sheet.sub(col, row);
            if (sub == null) {
                System.err.println("[SpriteSheet] 元件 " + resourceId + " 的 (col=" + col + ", row=" + row
                        + ") 越界（图共 " + sheet.getColumns() + "x" + sheet.getRows()
                        + " 格），已跳过：" + config.sourcePath());
                continue;
            }
            store.putTile(resourceId, sub);
        }
    }
}
