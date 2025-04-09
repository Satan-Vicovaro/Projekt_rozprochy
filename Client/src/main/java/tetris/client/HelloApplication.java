package tetris.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import tetris.client.game.TetrisGame;
import tetris.client.ui.UiManager;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        AnchorPane root = fxmlLoader.load();

        UiManager manager = new UiManager(root, stage, fxmlLoader,10,20);
        TetrisGame game = new TetrisGame(10,20,manager);

        game.start();
    }

    public static void main(String[] args) {
        launch();
    }
}