package com.redx.modsync;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // This tells JavaFX where to find the layout file we made earlier
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientsView.fxml"));
        Parent root = loader.load();
        
        // This creates the actual window with a size of 800x600
        Scene scene = new Scene(root, 800, 600);
        
        // Set the title of the window and display it
        primaryStage.setTitle("Mod Sync Admin Panel");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // This is the trigger that starts the whole JavaFX lifecycle
        launch(args);
    }
}