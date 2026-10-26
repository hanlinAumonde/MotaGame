package com.demo.mota.engine.catalog;

import java.util.Map;

/**
 * 道具表（{@code items.json}）的一条。
 *
 * @param itemType   大类：{@code Equipment / Key / Portion / AbilityGem / AuxiliaryItem / GenericItem}
 * @param parameters 随大类而定的参数（钥匙颜色、恢复量、装备的 stats / skills / slotType ……）
 * @param sprite     精灵目录 id，或 {@code assets/textures/} 下的图片路径；null 为没有图
 */
public record ItemDefinition(String itemId, String itemType, String itemName, String itemDescription,
                             long itemPrice, boolean isStorable, boolean isConsumable,
                             Map<String, Object> parameters, String sprite) {
}
