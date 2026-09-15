package com.demo.mota.engine.configs;

/**
 * 图片资源的目录约定与拆分配置入口。
 *
 * <p>图片统一放在资源根下的 {@code /Graphics/}，按用途分子目录。
 * 该路径同样适用于资源外置目录（{@code FileSystemResourceProvider} 镜像资源根目录），
 * 因此外置时把 {@code Graphics/} 按同样结构摆过去即可。
 *
 * <p>配置表（itemList / monsterList / skillList）里的 {@code resourceId} 只写<b>裸文件名</b>，
 * 由具体的注册方法拼上所属类别的目录。
 */
public class GraphicsConfigConstants {

    public static final String GRAPHICS_ROOT = "/Graphics/";

    public static final String ITEM_IMAGE_DIR = GRAPHICS_ROOT + "items/";
    public static final String MONSTER_IMAGE_DIR = GRAPHICS_ROOT + "monsters/";
    public static final String SKILL_IMAGE_DIR = GRAPHICS_ROOT + "skills/";

    /**
     * 精灵图拆分配置清单。需要切分的大图（地图元件图、角色行走图等）各有一个配置文件，
     * 路径登记在这里；新增一张图只改 JSON，不动 Java。
     */
    public static final String SPRITE_SHEET_MANIFEST = "/data/graphics/spritesheets.json";

    /** 主角在角色行走图配置里的 characterId */
    public static final String PLAYER_CHARACTER_ID = "player";
}
