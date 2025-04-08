package tetris.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    private GridPane mainGrid;

    @FXML
    public void initialize(){

    }

    public GridPane getMainGrid() {
        return mainGrid;
    }

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
    public Label getWelcomeText(){
        return  welcomeText;
    }
}