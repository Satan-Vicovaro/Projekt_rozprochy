package tetris.client.game;

import javafx.animation.AnimationTimer;
import tetris.client.ui.UiManager;

public class TetrisGame {
    GameBoard board;
    int sizeX;
    int sizeY;

    UiManager manager;

    AnimationTimer gameLoop = new AnimationTimer() {
        // Move piece down, check collision, update score, etc.
        boolean gameOver = false;
        Tetromino currentShape = null;
        long lastUpdate = 0;

        public void updateGame(double deltaTime) {
            // get random Tetromino if currently no one is used by player
            if (currentShape == null) {
                currentShape = new Tetromino(new Vector2d((float)(sizeX/2), 0),sizeX,sizeY);
                currentShape.setVelocity(new Vector2d(0,1F));
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
                board.handleLines();


                if (board.gameOver()) {
                    gameOver = true;
                }

                return;
            }

            board.addToBoard(currentShape);
            manager.updateBoard(board.getTiles());
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
    }

    public void init() {
        manager.init();
        manager.updateBoard(board.getTiles());
        manager.run();
    }

    public void start() {
        gameLoop.start();
    }


}
