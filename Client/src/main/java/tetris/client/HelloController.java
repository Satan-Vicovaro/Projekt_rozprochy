package tetris.client;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

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
    public void initialize() {

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
}