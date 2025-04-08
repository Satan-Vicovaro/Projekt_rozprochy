package tetris.client.game;

import java.util.Random;

public enum TetrominoType {
    TYPE_I,
    TYPE_O,
    TYPE_T,
    TYPE_J,
    TYPE_L,
    TYPE_S,
    TYPE_Z;

    public static final Random RANDOM = new Random();

    public static TetrominoType getRandom() {
        TetrominoType[] values = values();
        return values[RANDOM.nextInt(values.length)];
    }
}
