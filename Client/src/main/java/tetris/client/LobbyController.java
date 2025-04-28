package tetris.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import tetris.client.game.TetrisGame;
import tetris.client.serverRequests.ServerListener;
import tetris.client.ui.UiManager;

import java.io.IOException;

public class LobbyController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private FXMLLoader fxmlLoader;
    private ServerListener listener;
    private boolean playerIsReady;
    public LobbyController() {
        //this.listener = new ServerListener("local host",8080);
        this.playerIsReady = false;
    }
    public void loadLobby() throws IOException {
        // creating lobby scene
        this.fxmlLoader = new FXMLLoader(GameApplication.class.getResource("lobby-view.fxml"));
        root = fxmlLoader.load();
        scene = new Scene(root);
        stage = new Stage();
        stage.setTitle("Lobby");
        stage.setScene(scene);
        stage.show();
    }

    public void playerIsReady(ActionEvent event) {
        if (listener.isPlayerReady()) {
            listener.sendPlayerIsNotReady();
        }else {
            listener.sendPlayerIsReady();
            listener.getOtherLobbyPlayers();
        }
    }

    public void connectToServer() {
        this.listener = new ServerListener("127.0.0.1",8080);
        this.listener.start();
    }

    public void switchToGame(ActionEvent event) throws IOException {

//        if (!listener.getStartGame()) {
//            return;
//        }
        this.fxmlLoader = new FXMLLoader(getClass().getResource("game-view.fxml"));
        //Parent root = FXMLLoader.load(getClass().getResource("game-view.fxml"));
        Parent root = fxmlLoader.load();

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        initGame(root);
    }

    public void initGame(Parent root) throws IOException {
        UiManager manager = new UiManager((AnchorPane) root, stage, fxmlLoader,10,20);
        TetrisGame game = new TetrisGame(10,20,manager);

        game.init();
        game.start();
    }
}
