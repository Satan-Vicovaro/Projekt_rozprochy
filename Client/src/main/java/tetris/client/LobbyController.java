package tetris.client;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import tetris.client.game.PlayerData;
import tetris.client.game.TetrisGame;
import tetris.client.serverRequests.ServerListener;
import tetris.client.ui.UiManager;

import java.io.IOException;
import java.util.Vector;

public class LobbyController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private FXMLLoader fxmlLoader;
    private ServerListener listener;

    private boolean connectedToServer;
    private boolean playerIsReady;
    private TextFlow playerTable;

    private Timeline timeline;
    @FXML
    private TextFlow lobbyPlayerText;

    @FXML
    private void initialize() {

    }

    public LobbyController() {
        //this.listener = new ServerListener("local host",8080);
        this.playerIsReady = false;

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            pullForPlayerData();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        this.timeline = timeline;
        this.connectedToServer = false;
    }

    private void pullForPlayerData() {
        if(connectedToServer) {
            Vector<PlayerData> lobbyList =  listener.getOtherLobbyPlayers();
            updateLobbyList(lobbyList);
        }
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

    public void updateLobbyList(Vector<PlayerData> lobbyList) {
        this.lobbyPlayerText.getChildren().clear(); // resetting lobby text
        for(PlayerData player : lobbyList) {
            Text text = new Text(player.toString());
            text.setFont(new Font(18));
            this.lobbyPlayerText.getChildren().add(text);
        }
        this.lobbyPlayerText.requestLayout();
    }

    public void playerIsReady(ActionEvent event) {
        if (listener.isPlayerReady()) {
            listener.sendPlayerIsNotReady();
        }else {
            listener.sendPlayerIsReady();
        }
        Vector<PlayerData> lobbyList =  listener.getOtherLobbyPlayers();
        updateLobbyList(lobbyList);
    }

    public void connectToServer() {
        this.listener = new ServerListener("127.0.0.1",8080);
        this.listener.start();
        this.connectedToServer = true;
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
        TetrisGame game = new TetrisGame(10,20,manager, listener);

        timeline.stop();

        game.init();
        game.start();
    }
}
