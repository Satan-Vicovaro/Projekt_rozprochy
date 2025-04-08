package tetris.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tetris.client.HelloController;
import tetris.client.game.Tile;
import tetris.client.game.Vector2d;

public class UiManager {
    AnchorPane root;
    Stage stage;
    Label label;
    GridPane mainBoard;
    int sizeY;
    int sizeX;
    public UiManager(AnchorPane root, Stage stage, FXMLLoader fxmlLoader, int sizeX, int sizeY) {
        this.root = root;
        this.stage = stage;

        HelloController controller = fxmlLoader.getController();
        this.label = controller.getWelcomeText();

        this.mainBoard = controller.getMainGrid();

        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    public void init() {
        Scene scene = new Scene(root);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public void run() {
        label.setText("meow meow meow");
        //Rectangle square = new Rectangle(49,50);
       // mainBoard.add(new Rectangle(49,50),4,5);
        //mainBoard.add(new Rectangle(49,50),5,5);
    }

    public void updateBoard(Tile[][] board) {
        double rectSize = mainBoard.getWidth()/mainBoard.getColumnCount();


        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                if(board[y][x].color != ' ') {
                    mainBoard.add(new Rectangle(rectSize,rectSize),x,y);
                    break;
                }
            }
        }
    }
}
