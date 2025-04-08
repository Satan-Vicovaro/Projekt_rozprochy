package tetris.client.game;

public class TetrisGame {
    Tile[][] board;
    int sizeX;
    int sizeY;

    public TetrisGame(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        board = new Tile[sizeY][sizeX];
        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                board[y][x] = new Tile(new Vector2d(x, y));
            }
        }
    }

    public void game() {

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

    public void addToBoard(Tetromino shape) {
        for (Vector2d pos: shape) {
            this.board[(int) pos.y][(int) pos.x].color = 'X';
        }
    }
}
