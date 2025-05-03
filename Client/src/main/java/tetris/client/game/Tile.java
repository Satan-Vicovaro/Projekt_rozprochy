package tetris.client.game;

import javafx.scene.paint.Color;

public class Tile {
    public char color;

    public Tile(Vector2d position) {
        this.color = ' ';
    }
    public Tile (char color) {
        this.color = color;
    }
    public static Color getColorFromChar(char color) {
        return switch (color) {
            case 'A' -> Color.RED;
            case 'B' -> Color.BLUE;
            case 'C' -> Color.GREEN;
            case 'D' -> Color.DARKGOLDENROD;
            case 'E' -> Color.DARKMAGENTA;
            case 'F' -> Color.FORESTGREEN;
            case 'G' -> Color.DARKGREY;
            case 'H' -> Color.YELLOWGREEN;
            case 'I' -> Color.YELLOW;
            case 'J' -> Color.LIGHTCORAL;
            case 'K' -> Color.DARKRED;
            case 'L' -> Color.CADETBLUE;
            case 'M' -> Color.DARKGREEN;
            case 'N' -> Color.DARKGRAY;
            case 'O' -> Color.DARKSALMON;
            case 'P' -> Color.FUCHSIA;
            case 'R' -> Color.GOLDENROD;
            case ' ' -> Color.WHITE;
            default -> Color.ORANGE;
        };
    }

    public Color getColor() {
        switch (color) {
            case 'A':
                return Color.RED;
            case 'B':
                return Color.BLUE;
            case 'C':
                return Color.AQUA;
            case 'D':
                return Color.CYAN;
            case 'E':
                return Color.DARKMAGENTA;
            case 'F':
                return Color.FORESTGREEN;
            case 'G':
                return Color.KHAKI;
            case 'H':
                return Color.YELLOWGREEN;
            case 'I':
                return Color.YELLOW;
            case 'J':
                return Color.LIGHTCORAL;
            case 'K':
                return Color.DARKRED;
            case 'L':
                return Color.CADETBLUE;
            case 'M':
                return Color.DARKGREEN;
            case 'N':
                return Color.DARKGRAY;
            case 'O':
                return Color.DARKSALMON;
            case 'P':
                return Color.FUCHSIA;
            case 'R':
                return Color.GOLDENROD;
            case ' ':
                return Color.WHITE;
            default:
                return Color.ORANGE;
        }
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

    public static Tile[][] initBoard(int sizeX,int sizeY) {
        Tile[][] returnBoard = new Tile[sizeY][sizeX];
        for(int y = 0; y < sizeY; y++) {
            for(int x = 0; x < sizeX; x++) {
                returnBoard[y][x] = new Tile(' ');
            }
        }
        return returnBoard;
    }
}
