package tetris.client.game;

import java.util.concurrent.atomic.AtomicInteger;

public class PlayerData {
    int id;
    char color;
    int score;
    int linesCleared;
    float gameStage;
    private static final AtomicInteger nextId = new AtomicInteger(0);
    public PlayerData(char color, int score, int linesCleared, float gameStage) {
        this.color = color;
        this.score = score;
        this.linesCleared = linesCleared;
        this.gameStage = gameStage;
        this.id = nextId.getAndIncrement();
    }

//    public PlayerData(Byte[] bytes) {
//
//    }
    public void updateData(int score, int linesCleared, float gameStage) {
        this.score = score;
        this.linesCleared = linesCleared;
        this.gameStage = gameStage;
    }

    @Override
    public String toString() {
        return String.format("Player: %c, score: %d, linesCleared: %d, gameStage: %.1f \n", color, score, linesCleared, gameStage);
    }
}
