package tetris.client.game;

public class Tile {
    public char color;

    public Tile(Vector2d position) {
        this.color = ' ';
    }

    public static Byte[] tileArrayToBytes(Tile[] array) {
        Byte[] result = new Byte[array.length];
        int index = 0;
        for(Tile tile: array) {
            result[index] = (byte) tile.color;
            index++;
        }
        return result;
    }
}
