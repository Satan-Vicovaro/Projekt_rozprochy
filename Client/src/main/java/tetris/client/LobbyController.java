package tetris.client;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import tetris.client.game.PlayerData;
import tetris.client.game.TetrisGame;
import tetris.client.serverRequests.ClientTask;
import tetris.client.serverRequests.MessageType;
import tetris.client.serverRequests.ServerListener;
import tetris.client.ui.UiManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private  Timeline leaderBoardRefresh;
    @FXML
    private TextFlow lobbyPlayerText;

    @FXML
    private TextField serverAddresTextField;

    @FXML
    private Button connectToServerButton;

    @FXML
    private void initialize() {

    }

    public LobbyController() {
        //this.listener = new ServerListener("local host",8080);
        this.playerIsReady = false;

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            pullForPlayerData();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        Timeline leaderBoardRefresh = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            lobbyBoardRefresh();
        }));

        leaderBoardRefresh.setCycleCount(Animation.INDEFINITE);
        leaderBoardRefresh.play();

        this.leaderBoardRefresh = leaderBoardRefresh;
        this.timeline = timeline;
        this.connectedToServer = false;
    }

    private void pullForPlayerData() {
        if(connectedToServer) {
            listener.sendMessage(new ClientTask(MessageType.GET_OTHER_PLAYERS));
            List<PlayerData> lobbyList = listener.getOtherLobbyPlayersData();
            updateLobbyList(lobbyList);
        }
    }
    private void lobbyBoardRefresh() {
        if(connectedToServer) {
            List<PlayerData> lobbyList = listener.getOtherLobbyPlayersData();
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

    public void updateLobbyList(List<PlayerData> lobbyList) {
        synchronized (lobbyList) {
            this.lobbyPlayerText.getChildren().clear(); // resetting lobby text
            for(PlayerData player : lobbyList) {
                Text text = new Text(player.toStringLobby());
                text.setFont(new Font(18));
                this.lobbyPlayerText.getChildren().add(text);
            }
        }
        this.lobbyPlayerText.requestLayout();
    }

    //button function
    public void playerIsReady(ActionEvent event) {
        if (!listener.isPlayerReady()) {
            listener.sendMessage(new ClientTask(MessageType.PLAYER_READY));
        }else {
            listener.sendMessage(new ClientTask(MessageType.PLAYER_NOT_READY));
        }
    }
    //button function
    public void connectToServer() {
        if(connectedToServer) {
            return;
        }
        String hostAddress = this.serverAddresTextField.getText();
        try {
            this.listener = new ServerListener(hostAddress,8080);// "127.0.0.1"
            this.listener.start();
            this.connectToServerButton.setText("Connected");
            this.connectedToServer = true;
            this.listener.sendMessage(new ClientTask(MessageType.GET_OTHER_PLAYERS));
        } catch (Exception e) {
                this.connectToServerButton.setText("Couldn't connect");
                this.connectToServerButton.setFont(new Font(12));
        }
    }

    //button function
    public void switchToGame(ActionEvent event) throws IOException {

        if (!listener.getStartGame()) {
            return;
        }
        this.fxmlLoader = new FXMLLoader(getClass().getResource("game-view.fxml"));

        Parent root = fxmlLoader.load();

        stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        initGame(root);
    }

    public void initGame(Parent root) throws IOException {
        pullForPlayerData();
        timeline.stop();
        leaderBoardRefresh.stop();

        UiManager manager = new UiManager((AnchorPane) root,
                stage, fxmlLoader,
                10,20,
                listener.getCurrentPlayerNumber(),
                listener.getEnemiesBoards(),
                listener.getOtherLobbyPlayersData());
        TetrisGame game = new TetrisGame(10,20,manager, listener);

        game.init();
        game.start();
    }
}