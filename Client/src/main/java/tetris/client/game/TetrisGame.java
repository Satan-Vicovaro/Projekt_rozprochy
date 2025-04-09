package tetris.client.game;

import tetris.client.ui.UiManager;

public class TetrisGame {
    Tile[][] board;
    int sizeX;
    int sizeY;

    UiManager manager;

    public TetrisGame(int sizeX, int sizeY, UiManager manager) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        board = new Tile[sizeY][sizeX];

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                board[y][x] = new Tile(new Vector2d(x, y));
            }
        }

        this.manager = manager;
    }

    public void start() {
        manager.init();
        addTetromino(new Vector2d(5,14),TetrominoType.TYPE_O);
        addTetromino(new Vector2d(5,17),TetrominoType.TYPE_T);
        addTetromino(new Vector2d(5,1),TetrominoType.TYPE_L);
        addTetromino(new Vector2d(4,1),TetrominoType.TYPE_I);

        manager.updateBoard(board);
        printBoard();
        manager.run();

    }

    public void printBoard() {
        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                System.out.print(board[y][x].color);
            }
            System.out.println();
        }
    }

    public void addRandomTetromino() {
        Tetromino shape = new Tetromino();
        addToBoard(shape);
    }

    public void addRandomTetromino(Vector2d point) {
        Tetromino shape = new Tetromino(point);
        addToBoard(shape);
    }
    public void addTetromino(Vector2d point, TetrominoType type) {
        Tetromino shape = new Tetromino(point,type);
        addToBoard(shape);
    }

    public void addToBoard(Tetromino shape) {
        for (Vector2d pos: shape) {
            this.board[(int) pos.y][(int) pos.x].color = 'X';
        }
    }
}
