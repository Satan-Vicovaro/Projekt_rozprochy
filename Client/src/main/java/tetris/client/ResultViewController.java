package tetris.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import tetris.client.game.PlayerData;

import java.io.IOException;
import java.util.EventObject;
import java.util.List;

public class ResultViewController {
    @FXML
    private TextFlow FinalLeaderBoard;

    @FXML
    private void initialize(){

    }

    private Stage stage;
    private Scene scene;
    private Parent root;
    private final List<PlayerData> otherPlayersData;
    public ResultViewController(Stage stage, List<PlayerData> otherPlayersData) {

        System.out.println(getClass().getResource("result-view.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("result-view.fxml"));
        fxmlLoader.setController(this);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        this.otherPlayersData = otherPlayersData;
        this.stage =stage;
        scene = new Scene(root);
        stage.setScene(scene);

    }
    public void show() {
        int place = 1;
        for(PlayerData data:otherPlayersData) {
            Text text = new Text(
                    "Place: " + place
                    + " ,Player: " + data.color
                    + "     ,score: " + data.score
                    + " ,lines cleared: " + data.linesCleared
                    + " ,final game stage: " + (int) (2*data.gameStage)
                    );
            text.setFont(new Font(16));
            this.FinalLeaderBoard.getChildren().add(text);
            place++;
        }
        stage.show();
    }
}
