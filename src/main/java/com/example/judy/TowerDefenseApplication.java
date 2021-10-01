package com.example.judy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TowerDefenseApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                TowerDefenseApplication.class.getResource("screens/welcome-screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        scene.getRoot().setStyle("-fx-font-family: 'serif'");
        stage.setTitle("Tower Defense Game");
        stage.setScene(scene);
        stage.show();
    }

    /**
     *
     * Main method of the application
     *
     * @param args args
     */
    public static void main(String[] args) {
        launch();
    }
}