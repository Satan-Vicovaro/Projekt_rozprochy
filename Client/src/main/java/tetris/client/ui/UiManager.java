package tetris.client.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import tetris.client.HelloController;
import tetris.client.game.PlayerData;
import tetris.client.game.Tile;

import java.util.ArrayList;
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

    GridPane enemiesGrid;
    ArrayList<Rectangle[][]> enemiesBoards;

    AtomicReference<Character> symbol = new AtomicReference<>((char) -1);

    TextFlow scoreText;

    ArrayList<PlayerData> playerData;
    TextFlow leaderBoard;

    public UiManager(AnchorPane root, Stage stage, FXMLLoader fxmlLoader, int sizeX, int sizeY) {
        this.root = root;
        this.stage = stage;
        this.stage.setResizable(false);
        HelloController controller = fxmlLoader.getController();
        this.label = controller.getWelcomeText();
        this.mainBoard = controller.getMainGrid();
        this.scalableGroup = controller.getScalableGroup();
        this.enemiesGrid = controller.getEnemiesGrid();

        this.scoreText = controller.getScoreText();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.rectArr = new Rectangle[sizeY][sizeX];

        this.enemiesBoards = new ArrayList<>();
        this.leaderBoard = controller.getLeaderBoard();

        this.playerData = new ArrayList<>();

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
        stage.setTitle("Tetris Battle Royal");
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

    public void updateEnemiesBoards(ArrayList<Tile[][]>enemiesBoards) {
        for(int i = 0; i <enemiesBoards.size(); i++ ) {
            Tile[][] tileBoards = enemiesBoards.get(i);
            Rectangle[][]enemyVisualBoard = this.enemiesBoards.get(i);

            for (int y = 0; y<sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    if (tileBoards[y][x].color == 'X') {
                        enemyVisualBoard[y][x].setFill(Color.RED);
                    }
                    else {
                        enemyVisualBoard[y][x].setFill(Color.WHITE);
                    }
                }
            }
        }
    }

    public void loadEnemiesGrids() {
        double width = this.enemiesGrid.getWidth();
        double height = this.enemiesGrid.getHeight();

        enemiesGrid.setGridLinesVisible(true);
        // 80% of one grid cell size, 4 columns, gird cells should be squared-like;
        double singleEnemyGridWidth = (width/4)*0.95;

        int squaresInRow = 10;
        for (int i = 0; i < 11; i++) {
            GridPane singleEnemyGrid = new GridPane();
            //singleEnemyGrid.setH
            singleEnemyGrid.setGridLinesVisible(true);
            singleEnemyGrid.setMaxWidth(singleEnemyGridWidth/2);
            singleEnemyGrid.setMaxHeight(singleEnemyGridWidth);
            for(int j = 0; j < squaresInRow; j++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(singleEnemyGridWidth / squaresInRow); // 10 columns;
                singleEnemyGrid.getColumnConstraints().add(column);
            }

            for (int j = 0; j < 20 ; j++) {
                RowConstraints row = new RowConstraints();
                row.setPrefHeight(singleEnemyGridWidth / squaresInRow);// rows should have the same height as width
                singleEnemyGrid.getRowConstraints().add(row);
            }

            double rectHeight = (singleEnemyGridWidth/20)*0.95;
            double rectWidth = rectHeight;

            for(int y = 0; y < 20; y++) {
                for(int x = 0; x<10; x++) {
                    Rectangle rectangle = new Rectangle(rectWidth,rectHeight);
                    //rectangle.setStroke(Color.BLACK);
                    if (x%2 == 0)
                        rectangle.setFill(Color.RED);
                    else
                        rectangle.setFill(Color.WHITE);

                    this.enemiesBoards.get(i)[y][x] = rectangle;
                    GridPane.setHalignment(rectangle,HPos.CENTER);
                    GridPane.setValignment(rectangle,VPos.CENTER);
                    singleEnemyGrid.add(rectangle,x,y);
                }
            }

            GridPane.setHalignment(singleEnemyGrid, HPos.CENTER);
            GridPane.setValignment(singleEnemyGrid, VPos.CENTER);
            int y = i/4;
            this.enemiesGrid.add(singleEnemyGrid,i%4,y);
        }
    }

    public void addPlayerData(PlayerData player) {
        this.playerData.add(player);
    }

    public void getEnemiesBoards() {
        // for tests
        this.enemiesBoards.add(this.rectArr.clone());
        this.enemiesBoards.add(this.rectArr.clone());
        this.enemiesBoards.add(this.rectArr.clone());
    }

    public void initEnemyBoards(int playerCount) {
        for(int i = 0; i<playerCount;i++) {
            this.enemiesBoards.add(new Rectangle[20][10]);
        }
    }

    public void showEnemiesBoards() {

    }

    public char getUserInput() {
        // we reset the current symbol from buffer and pass the copy
        char copy = symbol.get();
        symbol.set((char) 0);
        return copy;
    }

    public void updateScore(int score, int linesCleared, float speedState) {
        String text = "Score: " + Integer.toString(score) + "\nLines cleared: "
                    + Integer.toString(linesCleared)
                    + "\nSpeed: " + Integer.toString((int)(speedState));

        Text showText = new Text(text);
        showText.setFont(new Font(16));
        this.scoreText.getChildren().set(0,showText);
    }

    public void updateScoreBoard() {
        leaderBoard.getChildren().clear();
        for (PlayerData data : playerData) {
            Text text = new Text(data.toString());
            text.setFont(new Font(14));

            leaderBoard.getChildren().add(text);
        }
    }

    public void closeProgram() {
        Platform.exit();
    }
}
