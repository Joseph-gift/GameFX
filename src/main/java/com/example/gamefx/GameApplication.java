package com.example.gamefx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                GameApplication.class.getResource("introduction-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 550);
        stage.setTitle("Mission Désamorçage");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }
}
