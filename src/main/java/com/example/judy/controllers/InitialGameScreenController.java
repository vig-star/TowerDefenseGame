package com.example.judy.controllers;

import com.example.judy.TowerDefenseApplication;
import com.example.judy.modules.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;

public class InitialGameScreenController {
    @FXML
    private GridPane gridPane;
    @FXML
    private VBox gameData;
    @FXML
    private Label moneyLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private ImageView enemyImage;
    @FXML
    private VBox monumentData;
    @FXML
    private Label healthLabel;
    @FXML
    private ImageView monumentImage;

    private Image image;


    private Scene nextScene;

    private Game game;
    private Player player;
    private Enemy enemy;
    private Monument monument;


    @FXML
    private void initialize(){

        game = GameDataHolder.getGame();
        //player = GameDataHolder.game.getPlayer();
        //enemy = GameDataHolder.game.getEnemy();
        //monument = GameDataHolder.game.getMonument();

        for (int i = 0; i < 20; i++) {
            ColumnConstraints column = new ColumnConstraints(140);
            gridPane.getColumnConstraints().add(column);
        }
        for (int i = 0; i < 20; i++) {
            RowConstraints row = new RowConstraints(40);
            gridPane.getRowConstraints().add(row);
        }

        GridPane.setRowIndex(gameData, 1);
        GridPane.setColumnIndex(gameData, 0);
        gameData.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        //moneyLabel.setText("MONEY: " + player.getMoney());
        moneyLabel.setText("MONEY: " + 50);
        moneyLabel.setTextFill(Color.web("#FFFFFF"));
        moneyLabel.setFont(Font.font("Courier New", 20));

        //scoreLabel.setText("SCORE: " + player.getScore());
        scoreLabel.setText("SCORE: " + 0);
        scoreLabel.setTextFill(Color.web("#FFFFFF"));
        scoreLabel.setFont(Font.font("Courier New", 20));

        //levelLabel.setText("LEVEL: " + game.getLevel());
        levelLabel.setText("LEVEL: " + 0);
        levelLabel.setTextFill(Color.web("#FFFFFF"));
        levelLabel.setFont(Font.font("Courier New", 20));

        try {
            URL url = TowerDefenseApplication.class.getResource("assets/icons/enemy.png");
            image = new Image(String.valueOf(url));
        } catch (IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error in accessing assets");
            alert.show();
        }
        enemyImage.setImage(image);
        enemyImage.setFitWidth(100);
        //Setting the image view parameters
        GridPane.setRowIndex(enemyImage, 9);
        GridPane.setColumnIndex(enemyImage, 0);
        enemyImage.setPreserveRatio(true);
        enemyImage.toFront();


        //healthLabel.setText("HEALTH: " + monument.getHealth());
        healthLabel.setText("HEALTH: " + 50);
        healthLabel.setTextFill(Color.web("#FFFFFF"));
        healthLabel.setStyle("-fx-background-color: green;");
        healthLabel.setFont(Font.font("Courier New", 14));
        try {
            URL url = TowerDefenseApplication.class.getResource("assets/icons/gate.png");
            image = new Image(String.valueOf(url));
        } catch (IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error in accessing assets");
            alert.show();
        }
        monumentImage.setImage(image);
        monumentImage.setFitWidth(90);
        //Setting the image view parameters
        GridPane.setRowIndex(monumentData, 12);
        GridPane.setColumnIndex(monumentData, 8);
        monumentImage.setPreserveRatio(true);
        monumentImage.toFront();


    }

    /**
     *
     * Method to set next scene
     *
     * @param scene nextScene
     */
    public void setNextScene(Scene scene) {
        nextScene = scene;
    }

    /**
     *
     * Method to switch screens
     *
     * @param actionEvent actionEvent to trigger screen switch
     */
    public void openNextScene(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(nextScene);
    }


}