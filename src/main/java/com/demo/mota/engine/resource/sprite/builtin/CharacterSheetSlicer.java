package com.demo.mota.engine.resource.sprite.builtin;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.resource.sprite.CharacterSprites;
import com.demo.mota.engine.resource.sprite.SheetSlicer;
import com.demo.mota.engine.resource.sprite.SpriteSheet;
import com.demo.mota.engine.resource.sprite.SpriteSheetConfig;
import com.demo.mota.engine.resource.sprite.SpriteStore;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * {@code type: "character"} —— 角色行走图。
 *
 * <p>网格的含义是「一行是一个朝向，一行里的各列是该朝向的行走帧」：
 *
 * <pre>{@code
 * "characterId":   "player",
 * "directionRows": { "DOWN": 0, "LEFT": 1, "RIGHT": 2, "UP": 3 },
 * "walkColumns":   [0, 1, 2, 3],
 * "idleFrame":     0
 * }</pre>
 *
 * <p>朝向到行号的映射<b>显式写在配置里</b>，不再依赖 {@code Direction} 枚举的声明顺序，
 * 因此换一张行列排布不同的素材只改 JSON 即可。
 *
 * <p>{@code walkColumns} 声明的所有帧都会被切出来存进 {@link CharacterSprites}，
 * 尽管当前游戏没有动画系统、渲染只取 {@code idleFrame} 那一帧——
 * 这就是留给动画系统的入口：素材侧已经就位，届时只需新增「按时间选帧」的逻辑。
 */
public final class CharacterSheetSlicer implements SheetSlicer {

    private static final String KEY_CHARACTER_ID = "characterId";
    private static final String KEY_DIRECTION_ROWS = "directionRows";
    private static final String KEY_WALK_COLUMNS = "walkColumns";
    private static final String KEY_IDLE_FRAME = "idleFrame";

    @Override
    public void slice(SpriteSheet sheet, SpriteSheetConfig config, SpriteStore store) {
        String characterId = config.params().getString(KEY_CHARACTER_ID, null);
        if (characterId == null) {
            System.err.println("[SpriteSheet] 角色图未声明 characterId，已跳过：" + config.sourcePath());
            return;
        }

        Map<String, Integer> directionRows = config.params().getIntMap(KEY_DIRECTION_ROWS);
        if (directionRows.isEmpty()) {
            System.err.println("[SpriteSheet] 角色图未声明 directionRows，已跳过：" + config.sourcePath());
            return;
        }

        List<Integer> walkColumns = config.params().getIntList(KEY_WALK_COLUMNS, List.of(0));
        int idleFrame = config.params().getInt(KEY_IDLE_FRAME, 0);

        Map<Direction, List<Image>> frames = new EnumMap<>(Direction.class);
        for (Map.Entry<String, Integer> entry : directionRows.entrySet()) {
            Direction direction = parseDirection(entry.getKey(), config);
            if (direction == null) {
                continue;
            }
            int row = entry.getValue();
            List<Image> rowFrames = new ArrayList<>(walkColumns.size());
            for (int col : walkColumns) {
                Image frame = sheet.sub(col, row);
                if (frame == null) {
                    System.err.println("[SpriteSheet] 角色 " + characterId + " 的帧 (col=" + col + ", row=" + row
                            + ") 越界（图共 " + sheet.getColumns() + "x" + sheet.getRows()
                            + " 格），已跳过：" + config.sourcePath());
                    continue;
                }
                rowFrames.add(frame);
            }
            if (!rowFrames.isEmpty()) {
                frames.put(direction, rowFrames);
            }
        }

        if (frames.isEmpty()) {
            System.err.println("[SpriteSheet] 角色 " + characterId + " 没有切出任何帧，已跳过："
                    + config.sourcePath());
            return;
        }
        store.putCharacter(characterId, new CharacterSprites(frames, idleFrame));
    }

    private Direction parseDirection(String name, SpriteSheetConfig config) {
        try {
            return Direction.fromString(name);
        } catch (IllegalArgumentException e) {
            System.err.println("[SpriteSheet] directionRows 中的 \"" + name + "\" 不是合法朝向，已跳过："
                    + config.sourcePath());
            return null;
        }
    }
}
