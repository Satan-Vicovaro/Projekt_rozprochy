package tetris.client.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tetris.client.HelloController;
import tetris.client.game.Tile;

import java.util.concurrent.atomic.AtomicReference;

public class UiManager {
    AnchorPane root;
    Stage stage;
    Label label;
    GridPane mainBoard;
    AnchorPane scalableGroup;
    Rectangle[][] rectArr;
    int sizeY;
    int sizeX;

    AtomicReference<Character> symbol = new AtomicReference<>((char) -1);
    public UiManager(AnchorPane root, Stage stage, FXMLLoader fxmlLoader, int sizeX, int sizeY) {
        this.root = root;
        this.stage = stage;

        HelloController controller = fxmlLoader.getController();
        this.label = controller.getWelcomeText();
        this.mainBoard = controller.getMainGrid();
        this.scalableGroup = controller.getScalableGroup();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        rectArr = new Rectangle[sizeY][sizeX];

        root.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case W -> {
                    symbol.set('W');
                    System.out.println("Pressed W");
                }
                case A -> {
                    symbol.set('A');
                    System.out.println("Pressed A");
                }
                case S -> {
                    symbol.set('S');
                    System.out.println("Pressed S");
                }
                case D -> {
                    symbol.set('D');
                    System.out.println("Pressed D");
                }
                case Q -> {
                    symbol.set('Q');
                    System.out.println("Pressed Q");
                }
                case E -> {
                    symbol.set('E');
                    System.out.println("Pressed E");
                }
            }
        });

    }

    public void init() {
        Scene scene = new Scene(root);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        double rectSize = mainBoard.getWidth()/mainBoard.getColumnCount() * 0.95;
        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                Rectangle rect = new Rectangle(rectSize,rectSize, Color.RED);
                rectArr[y][x] = rect;
                mainBoard.add(rect,x,y);
            }
        }

        double originalWidth = 1400;
        double originalHeight = 700;

        ChangeListener<Number> scaleListener = (obs, oldVal, newVal) -> {
            double scaleX = root.getWidth() / originalWidth;
            double scaleY = root.getHeight() / originalHeight;
            double scale = Math.min(scaleX, scaleY); // maintain aspect ratio

            scalableGroup.setScaleX(scale);
            scalableGroup.setScaleY(scale);
        };

        root.widthProperty().addListener(scaleListener);
        root.heightProperty().addListener(scaleListener);
    }

    public void run() {
        label.setText("meow meow meow");
    }

    public void updateBoard(Tile[][] board) {
        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                if(board[y][x].color != ' ') {
                    rectArr[y][x].setFill(Color.BLUE);
                }else {
                    rectArr[y][x].setFill(Color.WHITE);
                }
            }
        }
    }
    public char getUserInput() {
        // we reset the current symbol from buffer and pass the copy
        char copy = symbol.get();
        symbol.set((char) 0);
        return copy;
    }

    public void closeProgram() {
        Platform.exit();
    }
}
