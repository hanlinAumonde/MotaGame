module com.demo.mota {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires annotations;
    requires com.fasterxml.jackson.databind;

    opens com.demo.mota to javafx.fxml;
    opens com.demo.mota.engine to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.state to com.fasterxml.jackson.databind;
    opens com.demo.mota.engine.map to com.fasterxml.jackson.databind;

    exports com.demo.mota;
    exports com.demo.mota.engine;
    exports com.demo.mota.engine.enums;
    exports com.demo.mota.engine.event;
    exports com.demo.mota.engine.map;
    exports com.demo.mota.engine.map.tile;
    exports com.demo.mota.engine.state;
    exports com.demo.mota.engine.state.monster;
}
