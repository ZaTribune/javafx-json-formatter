package com.zatribune.devtools;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("json-formatter.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        URL icon=getClass().getResource("icon-128.png");
        if (icon!=null)
            stage.getIcons().add(new Image(icon.toExternalForm()));

        stage.setTitle("DevTools");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}