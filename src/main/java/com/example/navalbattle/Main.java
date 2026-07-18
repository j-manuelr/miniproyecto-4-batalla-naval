package com.example.navalbattle;

import javafx.application.Application;
import javafx.stage.Stage;

import static javafx.application.Application.launch;

public class Main extends Application {

    @Override
    public  void start (Stage stage){
        stage.setTitle("Batalla Naval");
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
