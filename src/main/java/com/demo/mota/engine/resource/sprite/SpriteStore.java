package com.demo.mota.engine.resource.sprite;

import javafx.scene.image.Image;

/**
 * 切分产物的写入口：{@link SheetSlicer} 只管切，切完往这里放，不关心缓存长什么样。
 *
 * <p>由 {@code ResourceManager} 实现，把产物落到自己的缓存里。
 * 方法按<b>产物种类</b>划分而非按切法划分——因此新增一种切法（比如九宫格自动拼接墙体、
 * 从另一种排布的图集里切元件）只要复用已有的 put 方法即可，无需改动本接口与 ResourceManager。
 */
public interface SpriteStore {

    /** 存入一个地图元件子图，key 为地图 JSON 里引用的 resourceId */
    void putTile(String resourceId, Image image);

    /** 存入一个角色的全部行走帧 */
    void putCharacter(String characterId, CharacterSprites sprites);
}
