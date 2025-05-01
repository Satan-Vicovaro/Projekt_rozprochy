package tetris.client.game;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class Tetromino implements Iterable<Vector2d>{
    Vector2d[] tiles;
    Vector2d centralPosition;
    float sizeX; // for border check
    float sizeY;
    TetrominoType type;

    Vector2d velocity = new Vector2d(0,0);

    public Tetromino(float sizeX,float sizeY) {
        centralPosition = new Vector2d(0, 0);
        TetrominoType type = TetrominoType.getRandom();
        this.tiles = getType(type);
    }

    public Tetromino(Vector2d point, float sizeX,float sizeY) {
        centralPosition = new Vector2d(point);
        TetrominoType type = TetrominoType.getRandom();
        this.tiles = getType(type);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }
    public Tetromino(Vector2d point, TetrominoType type, float sizeX,float sizeY) {
        centralPosition = point;
        this.tiles = getType(type);
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    private Vector2d[] getType(TetrominoType type) {
        Vector2d[] tiles = new Vector2d[4];
        switch (type) {
            case TYPE_I -> {
                tiles = new Vector2d[]{
                        new Vector2d(-1, 0),
                        new Vector2d(0, 0),
                        new Vector2d(1, 0),
                        new Vector2d(2, 0)
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
                        new Vector2d(-1, 0),
                        new Vector2d(1, 0),
                        new Vector2d(0, 1)
                };
            }
            case TYPE_J -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(-1, 0),
                        new Vector2d(1, 0),
                        new Vector2d(1, 1)
                };
            }
            case TYPE_L -> {
                tiles = new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(-1, 0),
                        new Vector2d(1, 0),
                        new Vector2d(-1, 1)
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
                        new Vector2d(-1, 0),
                        new Vector2d(0, 1),
                        new Vector2d(1, 1)
                };
            }
        }
        this.type = type;
        return tiles;
    }

    public void shiftBy(Vector2d vector2d) {
        this.centralPosition.x += vector2d.x;
        this.centralPosition.y += vector2d.y;
    }
    public void rotateLeft() {
        if (type==TetrominoType.TYPE_O) {
            return;
        }
        for (Vector2d piece:tiles) {
            float x = piece.x;
            float y = piece.y;
            piece.y =-x;
            piece.x = y;
        }
    }
    public void rotateRight() {
        if (type==TetrominoType.TYPE_O) {
            return;
        }
        for (Vector2d piece:tiles) {
            float x = piece.x;
            float y = piece.y;
            piece.y = x;
            piece.x =-y;
        }
    }
    private boolean isPointValid(Vector2d point, Vector2d correction) {
        if(point.x < 0 ) {
            correction.x = abs(point.x);
            return false;
        }
        if(point.x >= this.sizeX) {
            correction.x = this.sizeX - point.x - 1;
            return false;
        }

        if(point.y < 0 ) {
            correction.y = abs(point.y);
            return false;
        }
        if(point.y >= this.sizeY) {
            correction.y = this.sizeY - point.y - 1;
            return false;
        }
        return true;
    }

    // if move is invalid, we shift it until it becomes valid
    public void makeMoveBorderValid() {
        Vector2d shift = new Vector2d(0,0);
        Vector2d possibleCorrection = new Vector2d(0,0);
        for (Vector2d tetrominoTile : this) { // iterate over ourselves
            if (!isPointValid(tetrominoTile,possibleCorrection)) {
                if (possibleCorrection.x < 0 || possibleCorrection.y < 0) {
                    shift.shiftyBy(new Vector2d(min(shift.x,possibleCorrection.x),min(shift.y, possibleCorrection.y)));
                }else {
                    shift.shiftyBy(new Vector2d(max(shift.x,possibleCorrection.x),max(shift.y, possibleCorrection.y)));
                }
            }
        }
        this.centralPosition.shiftyBy(possibleCorrection);
    }

    public boolean applyGravity(double deltaTime) {
        int posBeforeX = (int) centralPosition.x;
        int posBeforeY = (int) centralPosition.y;
        centralPosition.y += (float) (velocity.y * deltaTime);
        centralPosition.x += (float) (velocity.x * deltaTime);

        if ((int) centralPosition.x != posBeforeX || (int) centralPosition.y != posBeforeY)
            return  true; // tile position hase changed
        return false;
    }

    public Vector2d[] getLowestPoints() {
        Vector2d[] currentPoints = new Vector2d[4];
        int i = 0;
        for (Vector2d tilePos : this) {
            currentPoints[i] = tilePos;
            i++;
        }

        Collection<Vector2d> lowestPointsMap = Arrays.stream(currentPoints).collect(Collectors.toMap(
                p -> p.x, // key
                p -> p, // value
                (p1,p2) -> p2.y < p1.y ? p1 : p2 // key collision
        )).values();
        return lowestPointsMap.toArray(new Vector2d[0]);
    }
    public void setVelocity(Vector2d velocity) {
        this.velocity = velocity;
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
