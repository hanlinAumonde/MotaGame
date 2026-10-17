package com.demo.mota.engine.save;

/**
 * 一个档位的现状，供存读档界面列表展示。
 *
 * @param slotId 档位 id（文件名去掉扩展名）：{@link SaveRepository#AUTO_SLOT} 或 {@code slot_01} …
 * @param name   展示名：「自动存档」/「存档 01」…
 * @param meta   摘要，只有 {@link Status#OK} 时非空
 */
public record SaveSlotInfo(String slotId, String name, Status status, SaveMeta meta) {

    public enum Status {
        /** 还没有存档 */
        EMPTY("—— 空 ——"),
        OK(""),
        /** 文件无法解析 / 内容不完整 */
        CORRUPTED("存档损坏"),
        /** 存档的版本比当前程序还新 */
        TOO_NEW("存档版本过新"),
        /** 旧版本存档，但缺少升级到当前版本的迁移 */
        UNSUPPORTED("存档版本无法升级");

        private final String displayText;

        Status(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    public boolean isLoadable() {
        return status == Status.OK;
    }

    public boolean isEmpty() {
        return status == Status.EMPTY;
    }

    public boolean isAuto() {
        return SaveRepository.AUTO_SLOT.equals(slotId);
    }
}
