package tetris.client.game;

public class GameBoard {

    Tile[][] board;
    int sizeX;
    int sizeY;

    public GameBoard(int sizeX, int sizeY){
        this.board = new Tile[sizeY][sizeX];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                board[y][x] = new Tile(new Vector2d(x, y));
            }
        }
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
        Tetromino shape = new Tetromino(sizeX,sizeY);
        addToBoard(shape);
    }

    public void addRandomTetromino(Vector2d point) {
        Tetromino shape = new Tetromino(point, sizeX, sizeY);
        addToBoard(shape);
    }
    public void addTetromino(Vector2d point, TetrominoType type) {
        Tetromino shape = new Tetromino(point,type,sizeX,sizeY);
        addToBoard(shape);
    }

    public Tile[][] getTiles(){
       return board;
    }

    public void addToBoard(Tetromino shape) {
        for (Vector2d pos: shape) {
            this.board[(int) pos.y][(int) pos.x].color = 'X';
        }
    }
    public void removeFromBoard(Tetromino shape) {
        for (Vector2d pos: shape) {
            this.board[(int) pos.y][(int) pos.x].color = ' ';
        }
    }

}
