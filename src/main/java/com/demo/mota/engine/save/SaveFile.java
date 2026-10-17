package com.demo.mota.engine.save;

import com.demo.mota.engine.snapshot.GameSnapshot;

/**
 * 一个存档文件的内容：格式版本 + 档位列表里展示的摘要 + 快照本体。
 *
 * <p>{@code version} 只在快照结构有<b>不兼容</b>的调整（字段改名 / 挪位置）时才升，
 * 并在 {@link SaveMigrations} 里登记对应的一步迁移；单纯增删字段靠读取时的缺省值兼容，不必升版本。
 */
public record SaveFile(int version, SaveMeta meta, GameSnapshot snapshot) {
}
