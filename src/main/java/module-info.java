/**
 * open module：FXGL 按名字加载 assets/ 下的资源、给服务 / 组件注入依赖、调用 @Spawns 方法都要反射访问本模块，
 * Jackson 绑定配置与存档也一样——整个模块开放，不再逐包 opens（FXGL 迁移指南的写法）。
 */
open module com.demo.mota {
    requires javafx.controls;
    requires com.almasb.fxgl.all;

    requires com.fasterxml.jackson.databind;

    exports com.demo.mota;
    exports com.demo.mota.app.services;
    exports com.demo.mota.engine;
    exports com.demo.mota.engine.enums;
    exports com.demo.mota.engine.event;
    exports com.demo.mota.engine.map;
    exports com.demo.mota.engine.map.tile;
    exports com.demo.mota.engine.map.region;
    exports com.demo.mota.engine.Item;
    exports com.demo.mota.engine.Item.GenericItem;
    exports com.demo.mota.engine.state;
    exports com.demo.mota.engine.state.monster;
    exports com.demo.mota.engine.state.equipset;
    exports com.demo.mota.engine.skill;
    exports com.demo.mota.engine.skill.effect;
    exports com.demo.mota.engine.skill.effect.builtin;
    exports com.demo.mota.engine.skill.book;
    exports com.demo.mota.engine.skill.preset;
    exports com.demo.mota.engine.skill.cost;
    exports com.demo.mota.engine.rules;
    exports com.demo.mota.engine.battle;
    exports com.demo.mota.engine.resource;
    exports com.demo.mota.engine.resource.provider;
    exports com.demo.mota.engine.state.level;
    exports com.demo.mota.engine.snapshot;
    exports com.demo.mota.engine.save;
    exports com.demo.mota.engine.boot;
    exports com.demo.mota.engine.app;
    exports com.demo.mota.ui;
    exports com.demo.mota.ui.screen;
    exports com.demo.mota.ui.screen.loading;
    exports com.demo.mota.ui.screen.title;
    exports com.demo.mota.ui.screen.game;
    exports com.demo.mota.ui.screen.game.map;
    exports com.demo.mota.ui.screen.game.side;
    exports com.demo.mota.ui.screen.gameover;
    exports com.demo.mota.ui.screen.gamemenu;
    exports com.demo.mota.ui.screen.gamemenu.option;
    exports com.demo.mota.ui.screen.inventory;
    exports com.demo.mota.ui.screen.equipment;
    exports com.demo.mota.ui.screen.equipset;
    exports com.demo.mota.ui.screen.skill;
    exports com.demo.mota.ui.screen.saveload;
}
