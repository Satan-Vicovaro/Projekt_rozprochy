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
import tetris.client.GameController;
import tetris.client.game.PlayerData;
import tetris.client.game.Tile;

import java.util.ArrayList;
import java.util.List;
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
    int playerNum;

    GridPane enemiesGrid;
    ArrayList<Rectangle[][]> enemiesBoardsRects;
    ArrayList<Tile[][]> enemiesBoardsRaw;

    AtomicReference<Character> symbol = new AtomicReference<>((char) -1);

    TextFlow scoreText;
    PlayerData outPlayerData;
    final List<PlayerData> playerData;
    TextFlow leaderBoard;

    Rectangle selectedPlayerMark;

    public UiManager(AnchorPane root, Stage stage, FXMLLoader fxmlLoader, int sizeX, int sizeY, int playerNum,
                     ArrayList<Tile[][]> enemiesBoardsRaw, List<PlayerData> playerData) {
        this.root = root;
        this.stage = stage;
        this.stage.setResizable(false);
        GameController controller = fxmlLoader.getController();//new GameController();
        this.label = controller.getWelcomeText();
        this.mainBoard = controller.getMainGrid();
        this.scalableGroup = controller.getScalableGroup();
        this.enemiesGrid = controller.getEnemiesGrid();

        this.scoreText = controller.getScoreText();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.rectArr = new Rectangle[sizeY][sizeX];

        this.enemiesBoardsRects = new ArrayList<>();
        this.enemiesBoardsRaw = enemiesBoardsRaw; // reference to obj in ServerListener
        this.playerNum = playerNum;
        this.leaderBoard = controller.getLeaderBoard();

        this.playerData = playerData;

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
                case J -> {
                    symbol.set('J');
                    System.out.println("Pressed ⬅");
                }
                case K -> {
                    symbol.set('K');
                    System.out.println("Pressed ➡");
                }
                case F -> {
                    symbol.set('F');
                    System.out.println("Pressed F");
                }
            }
        });

    }

    public void init() {

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
                    rectArr[y][x].setFill(board[y][x].getColor());
                }else {
                    rectArr[y][x].setFill(Color.WHITE);
                }
            }
        }
    }

    public void updateEnemiesBoards() {
        for(int i = 0; i <this.enemiesBoardsRaw.size(); i++ ) {
            Tile[][] tileBoards = this.enemiesBoardsRaw.get(i);
            Rectangle[][]enemyVisualBoard = this.enemiesBoardsRects.get(i);

            for (int y = 0; y<sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    if (tileBoards[y][x].color != ' ') {
                        enemyVisualBoard[y][x].setFill(tileBoards[y][x].getColor());
                        continue;
                    }
                        enemyVisualBoard[y][x].setFill(Color.WHITE);
                }
            }
        }
    }

    public void initSelectedBoardMark() {
        this.selectedPlayerMark = new Rectangle(30,30,Color.DARKRED);
        GridPane.setRowIndex(selectedPlayerMark,0);
        GridPane.setColumnIndex(selectedPlayerMark,0);
        this.enemiesGrid.getChildren().add(selectedPlayerMark);
    }

    public void markSelectedBoard(int boardIndex) {
        if(boardIndex<0) {
            return;
        }
        if(boardIndex>enemiesBoardsRaw.size()) {
            return;
        }

        int x = boardIndex%4; // grid board is 4x4
        int y = boardIndex/4;
        GridPane.setRowIndex(selectedPlayerMark,y);
        GridPane.setColumnIndex(selectedPlayerMark,x);
    }

    public void loadEnemiesGrids() {
        double width = this.enemiesGrid.getWidth();
        double height = this.enemiesGrid.getHeight();

        enemiesGrid.setGridLinesVisible(true);
        // 80% of one grid cell size, 4 columns, gird cells should be squared-like;
        double singleEnemyGridWidth = (width/4)*0.95;

        int squaresInRow = 10;
        for (int i = 0; i < this.enemiesBoardsRaw.size(); i++) {
            GridPane singleEnemyGrid = new GridPane();
            singleEnemyGrid.setGridLinesVisible(true);
            singleEnemyGrid.setMaxWidth(singleEnemyGridWidth/2);
            singleEnemyGrid.setMaxHeight(singleEnemyGridWidth);

            // Column constraints
            for(int j = 0; j < squaresInRow; j++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(singleEnemyGridWidth / squaresInRow); // 10 columns;
                singleEnemyGrid.getColumnConstraints().add(column);
            }
            // Row constraints
            for (int j = 0; j < 20 ; j++) {
                RowConstraints row = new RowConstraints();
                row.setPrefHeight(singleEnemyGridWidth / squaresInRow);// rows should have the same height as width
                singleEnemyGrid.getRowConstraints().add(row);
            }

            double rectHeight = (singleEnemyGridWidth/20)*0.95;
            double rectWidth = rectHeight;
            // filling up the grid
            for(int y = 0; y < 20; y++) {
                for(int x = 0; x<10; x++) {
                    Rectangle rectangle = new Rectangle(rectWidth,rectHeight);
                    //rectangle.setStroke(Color.BLACK);
                    if (x%2 == 0)
                        rectangle.setFill(Color.RED);
                    else
                        rectangle.setFill(Color.WHITE);

                    this.enemiesBoardsRects.get(i)[y][x] = rectangle;
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
        this.outPlayerData = player;
    }

    public void initEnemyBoards() {
        for(int i = 0; i<this.enemiesBoardsRaw.size();i++) {
            this.enemiesBoardsRects.add(new Rectangle[20][10]);
        }
    }


    public char getUserInput() {
        // we reset the current symbol from buffer and pass the copy
        char copy = symbol.get();
        symbol.set((char) 0);
        return copy;
    }

    public void updateOurPlayerScore(int linesToSend) {
        String text = "Score: " + outPlayerData.score
                    + "\nLines cleared: " + outPlayerData.linesCleared
                    + "\nSpeed: " + (2*(int)(outPlayerData.gameStage)
                    + "\n Lines to send: " + linesToSend);

        Text showText = new Text(text);
        showText.setFont(new Font(16));
        this.scoreText.getChildren().set(0,showText);
    }

    public void updateScoreBoard() {
        leaderBoard.getChildren().clear();
        synchronized (playerData) {
            for (PlayerData data : playerData) {
                Text text = new Text(data.toString());
                text.setFont(new Font(14));

                leaderBoard.getChildren().add(text);
            }
        }
    }
    public Stage getStage(){
        return stage;
    }

    public void closeProgram() {
        Platform.exit();
    }
}
