package tetris.client.game;

// point and vector are the same thing actually
public class Vector2d {
    public float x;
    public float y;

    public Vector2d(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d(Vector2d vector) {
        this.x = vector.x;
        this.y = vector.y;
    }

    public void shiftyBy(Vector2d vector) {
        this.x += vector.x;
        this.y += vector.y;
    }

    public static Vector2d ones(Direction direction) {
        switch (direction){
            case UP -> {
                return new Vector2d(0,1);
            }
            case DOWN -> {
                return new Vector2d(0,-1);
            }
            case LEFT -> {
                return new Vector2d(-1,0);
            }
            case RIGHT -> {
                return new Vector2d(1,0);
            }
        }
        return new Vector2d(0,0);
    }
}
