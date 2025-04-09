package tetris.client.game;

import java.util.Iterator;

public class Tetromino implements Iterable<Vector2d>{
    Vector2d[] tiles;
    Vector2d centralPosition;

    public Tetromino() {
        centralPosition = new Vector2d(0, 0);
        TetrominoType type = TetrominoType.getRandom();
        this.tiles = getType(type);
    }

    public Tetromino(Vector2d point) {
        centralPosition = new Vector2d(point);
        TetrominoType type = TetrominoType.getRandom();
        this.tiles = getType(type);
    }
    public Tetromino(Vector2d point, TetrominoType type) {
        centralPosition = point;
        this.tiles = getType(type);
    }

    private Vector2d[] getType(TetrominoType type) {
        Vector2d[] tiles = new Vector2d[4];
        switch (type) {
            case TYPE_I -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(1, 0),
                        new Vector2d(2, 0),
                        new Vector2d(3, 0)
                };
            }
            case TYPE_O -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(1, 0),
                        new Vector2d(1, 1),
                        new Vector2d(0, 1)
                };
            }
            case TYPE_T -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(1, 0),
                        new Vector2d(2, 0),
                        new Vector2d(1, 1)
                };
            }
            case TYPE_J -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 1),
                        new Vector2d(1, 1),
                        new Vector2d(2, 1),
                        new Vector2d(0, 2)
                };
            }
            case TYPE_L -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(0, 1),
                        new Vector2d(0, 2),
                        new Vector2d(1, 2)
                };
            }
            case TYPE_S -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 1),
                        new Vector2d(1, 0),
                        new Vector2d(1, 1),
                        new Vector2d(2, 0)
                };
            }
            case TYPE_Z -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(1, 0),
                        new Vector2d(1, 1),
                        new Vector2d(2, 1)
                };
            }
        }
        return tiles;
    }


    @Override
    public Iterator<Vector2d> iterator() {
        return new TerminoIterator();
    }
    private class TerminoIterator implements Iterator<Vector2d> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < tiles.length;
        }

        @Override
        public Vector2d next() {
            Vector2d returnVal = new Vector2d(centralPosition);
            returnVal.shiftyBy(tiles[index++]);
            return  returnVal;
        }
    }
}
