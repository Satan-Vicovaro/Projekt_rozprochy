package tetris.client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class GameApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        LobbyController lobbyController = new LobbyController();
        lobbyController.loadLobby();
    }
    public static void main(String[] args) {
        launch();
    }
}