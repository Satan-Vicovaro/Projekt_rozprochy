package tetris.client;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    private GridPane mainGrid;

    @FXML
    private StackPane rootStack;

    @FXML
    private AnchorPane scalableGroup;

    @FXML
    private GridPane enemiesGrid;

    @FXML
    private TextFlow controlsInfo;

    @FXML
    private TextFlow scoreText;

    @FXML
    private TextFlow leaderBoard;

    @FXML
    public void initialize() {
        Text t1 = new Text("Controls:\nw");
        Text t2 = new Text("Movement: W,A,S,D.\n");
        Text t3 = new Text("Send lines: F.\n");
        Text t4 = new Text("Change attack player: ⬅ ➡.");
        t1.setFont(new Font(16));
        t2.setFont(new Font(16));
        t3.setFont(new Font(16));
        t4.setFont(new Font(16));
        controlsInfo.getChildren().addAll(t1,t2,t3,t4);

        Text t5 = new Text("Score: 0");
        t5.setFont(new Font(16));
        scoreText.getChildren().add(t5);
    }

    public GridPane getMainGrid() {
        return mainGrid;
    }
    public AnchorPane getScalableGroup() {
        return scalableGroup;
    }

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    public Label getWelcomeText(){
        return  welcomeText;
    }

    public GridPane getEnemiesGrid() {
        return enemiesGrid;
    }

    public TextFlow getScoreText() {
        return scoreText;
    }

    public TextFlow getLeaderBoard() {
        return  leaderBoard;
    }
}