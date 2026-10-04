module com.demo.mota {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;

    opens com.demo.mota to javafx.fxml;
    opens com.demo.mota.engine to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.state to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.map to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.factory.item to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.factory.monster to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.factory.skill to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.resource to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.state.level to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.rules to com.fasterxml.jackson.databind;

    exports com.demo.mota;
    exports com.demo.mota.engine;
    exports com.demo.mota.engine.enums;
    exports com.demo.mota.engine.event;
    exports com.demo.mota.engine.map;
    exports com.demo.mota.engine.map.tile;
    exports com.demo.mota.engine.Item;
    exports com.demo.mota.engine.Item.GenericItem;
    exports com.demo.mota.engine.state;
    exports com.demo.mota.engine.state.monster;
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
    exports com.demo.mota.engine.resource.sprite;
    exports com.demo.mota.engine.resource.sprite.builtin;
    exports com.demo.mota.engine.state.level;
    exports com.demo.mota.engine.boot;
    exports com.demo.mota.engine.app;
    exports com.demo.mota.ui;
    exports com.demo.mota.ui.screen;
    exports com.demo.mota.ui.screen.loading;
    exports com.demo.mota.ui.screen.title;
    exports com.demo.mota.ui.screen.game;
    exports com.demo.mota.ui.screen.game.side;
    exports com.demo.mota.ui.screen.gameover;
    exports com.demo.mota.ui.screen.gamemenu;
    exports com.demo.mota.ui.screen.gamemenu.option;
    exports com.demo.mota.ui.screen.inventory;
    exports com.demo.mota.ui.screen.equipment;
    exports com.demo.mota.ui.screen.skill;
}
