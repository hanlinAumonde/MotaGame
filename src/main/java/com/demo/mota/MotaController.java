package com.demo.mota;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MotaController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}