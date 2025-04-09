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
        @Override
        public void handle(long now) {
            // get random Tetromino if currently no one is used by player
            if (currentShape == null) {
                currentShape = new Tetromino(new Vector2d((float)(sizeX/2), 0));
            }
            // Player keyboard input
            char input =  manager.getUserInput();

            if (input != 0) {
                if (input =='A') {
                    // move left
                    currentShape.shiftBy(new Vector2d(-1,0));
                } else if (input =='D') {
                    // move right
                    currentShape.shiftBy(new Vector2d(1,0));
                }else if (input == 'W') {
                    currentShape.shiftBy(new Vector2d(0,-1));
                }else if (input == 'S') {
                    currentShape.shiftBy(new Vector2d(0,1));
                } else if (input == 'Q') {
                    currentShape.rotateLeft();
                } else if(input == 'E') {
                    currentShape.rotateRight();
                }
            }
            board.addToBoard(currentShape);
            manager.updateBoard(board.getTiles());
            board.removeFromBoard(currentShape);
            if (gameOver) {
                return;
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
        //board.addRandomTetromino();
        //board.addTetromino(new Vector2d(5,6),TetrominoType.TYPE_S);
        //board.addTetromino(new Vector2d(5,10),TetrominoType.TYPE_Z);
        //board.addTetromino(new Vector2d(5,15),TetrominoType.TYPE_J);
        manager.updateBoard(board.getTiles());
        manager.run();
    }

    public void start() {
        gameLoop.start();
    }


}
