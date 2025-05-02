package tetris.client.game;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import static java.lang.Math.abs;

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
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    public Vector<Byte> getByteBoardArray() {

        Iterator<Tile[]> iterator = this.rowIterator();
        Vector<Byte> result = new Vector<>();
        while(iterator.hasNext()) {
            Tile[] line = iterator.next();
            Byte[] byteLine = Tile.tileArrayToBytes(line);

            result.addAll(Arrays.asList(byteLine));
        }
        return result;
    }

    public void printBoard() {
        for (int y = 0; y<sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                System.out.print(board[y][x].color);
            }
            System.out.println();
        }
    }

    public void setBoard(Tile[][] board) {
        this.board = board;
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

    // if there is end of track or collision with blocks below we set Shape position
    public boolean checkPlaceShape(Tetromino shape) {
        Vector2d[] lowestPoints = shape.getLowestPoints();

        for (Vector2d point: lowestPoints) {
            if (point.y > sizeY - 1 || point.y < 0) {
                return true; //set shape on the bottom of board
            }
            Tile pointBelow = board[(int)(point.y + 1)][(int)point.x];
            if (pointBelow.color != ' ') {
                return true;
            }
        }

        return false;
    }

    public int clearFullLines() { // number of lines Cleared after placing shape
        Iterator<Tile[]> iterator = this.rowIterator();
        int linesClearedNum = 0;
        int yIndex = 0;
        while (iterator.hasNext()) {
            Tile[] row = iterator.next();
            boolean lineCleared = true;
            for (Tile tile : row) {
                if (tile.color == ' ') {
                    lineCleared = false;
                }
            }
            if (lineCleared) {
                for (Tile tile : row) {
                    tile.color = ' ';
                }
                linesClearedNum++;
                this.shiftDownFrom(yIndex);
            }
            yIndex++;
        }
        return linesClearedNum;
    }

    public void shiftDownFrom(int lineIndex) {
        for(int y = lineIndex; y > 2; y--) {
            for(int x = 0; x<sizeX; x++) {
                if(board[y - 1][x].color !=' '){
                    Tile temp = board[y][x];
                    board[y][x] = board[y-1][x];
                    board[y-1][x] = temp;
                }
            }
        }
    }

    public int handleLines() {
        return clearFullLines();
    }

    public void addLinesToBottomOfBoard(int linesNum) {

        // shifting up tiles
        for(int y = 0; y > sizeY - 1 - linesNum; y++) {
            for(int x = 0; x<sizeX; x++) {
                if(board[y + 1][x].color !=' '){
                    Tile temp = board[y][x];
                    board[y][x] = board[y + 1][x];
                    board[y + 1][x] = temp;
                }
            }
        }
        Random random = new Random();
        int spaceAtIndex = abs(random.nextInt(sizeX));

        for(int y = sizeY - 1; y > sizeY - 1 - linesNum; y--){
            for(int x = 0; x<sizeX; x++) {
                if(x == spaceAtIndex) {
                    continue;
                }
                board[y][x].color = 'X';
           }
        }
    }

    public boolean gameOver() {
        Tile[] highestRow = board[0];
        for (Tile tile: highestRow) {
            if (tile.color != ' ') {
                return true;
            }
        }
        return false;
    }

    public Iterator<Tile[]> rowIterator(){
        Tile[][] board = this.board;
        return new Iterator<Tile[]>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < sizeY;
            }

            @Override
            public Tile[] next() {
                return board[index++];
            }

            public int getIndex() {
                return index;
            }
        };
    }
}
