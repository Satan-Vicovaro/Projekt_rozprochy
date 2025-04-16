package tetris.client.game;

import javafx.animation.AnimationTimer;
import tetris.client.ui.UiManager;

import java.util.ArrayList;

public class TetrisGame {
    GameBoard board;
    int sizeX;
    int sizeY;

    UiManager manager;

    float speedState;
    Vector2d fallingSpeed;

    int score;
    int totalLinesCleared;

    PlayerData playerData;

    final int MAX_SPEED_STATE = 3;
    AnimationTimer gameLoop = new AnimationTimer() {
        // Move piece down, check collision, update score, etc.
        boolean gameOver = false;
        Tetromino currentShape = null;
        long lastUpdate = 0;

        public void updateGame(double deltaTime) {
            // get random Tetromino if currently no one is used by player
            if (currentShape == null) {
                currentShape = new Tetromino(new Vector2d((float)(sizeX/2), 0),sizeX,sizeY);
                currentShape.setVelocity(fallingSpeed);
            }


            // Player keyboard input
            char input =  manager.getUserInput();

            // if player moved a piece
            if (input != 0) {
                if (input =='A') {
                    // move left
                    currentShape.shiftBy(Vector2d.ones(Direction.LEFT));;
                } else if (input =='D') {
                    // move right
                    currentShape.shiftBy(Vector2d.ones(Direction.RIGHT));
                }else if (input == 'W') {
                    while (!board.checkPlaceShape(currentShape)){
                        currentShape.shiftBy(Vector2d.ones(Direction.DOWN));
                    }
                }else if (input == 'S') {
                    currentShape.shiftBy(new Vector2d(0,1));
                } else if (input == 'Q') {
                    currentShape.rotateLeft();
                } else if(input == 'E') {
                    currentShape.rotateRight();
                }
            }
            currentShape.applyGravity(deltaTime);
            currentShape.makeMoveBorderValid();

            if (board.checkPlaceShape(currentShape)) { // placing shape

                board.addToBoard(currentShape);
                manager.updateBoard(board.getTiles());
                currentShape = null;
                int clearedLines = board.handleLines();
                if (clearedLines == 0) {
                    if (board.gameOver()) {
                        gameOver = true;
                    }
                    return;
                }
                totalLinesCleared += clearedLines;

                switch (clearedLines) {
                    case 1:
                        score +=40;
                        break;
                    case 2:
                        score += 100;
                        break;
                    case 3:
                        score += 300;
                        break;
                    case 4:
                        score += 1200;
                        break;
                    default:
                        break;
                }

                if (totalLinesCleared > 10 * speedState && speedState < MAX_SPEED_STATE) {
                    speedState += 0.5F;
                    fallingSpeed.mulBy(speedState); // adjusting fall speed
                }
                return;
            }

            board.addToBoard(currentShape);
            manager.updateBoard(board.getTiles());
            ArrayList<Tile[][]> enemiesBoards = new ArrayList<>();
            for(int i =0 ; i<11; i++) {
                enemiesBoards.add(board.getTiles().clone());
            }
            manager.updateEnemiesBoards(enemiesBoards);
            manager.updateScoreBoard();
            playerData.updateData(score,totalLinesCleared,speedState);
            //manager.updateScore(score,totalLinesCleared,speedState);
            // send message to other players;
            //
            // wait for server answer

            board.removeFromBoard(currentShape);
        }

        @Override
        public void handle(long now) {

            if (lastUpdate > 0) {
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // seconds
                updateGame(deltaTime);
            }
            lastUpdate = now;
            if (gameOver) {
                this.stop();
                manager.closeProgram();
            }
        }
    };

    public TetrisGame(int sizeX, int sizeY, UiManager manager) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        board = new GameBoard(sizeX,sizeY);
        this.manager = manager;
        this.speedState = 1;
        this.fallingSpeed = new Vector2d(0,1F);
        this.totalLinesCleared = 0;
        this.score = 0;
        this.playerData = new PlayerData('X',score,totalLinesCleared,speedState);
        this.manager.addPlayerData(playerData);
    }

    public void init() {
        manager.init();
        manager.initEnemyBoards(11);
        manager.loadEnemiesGrids();
        manager.updateBoard(board.getTiles());
        manager.run();
    }

    public void start() {
        gameLoop.start();
    }

}
