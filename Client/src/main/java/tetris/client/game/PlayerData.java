package tetris.client.game;

import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerData {
    //int id;
    char color;
    int score;
    int linesCleared;
    float gameStage;
    boolean ready;
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public PlayerData(char color, int score, int linesCleared, float gameStage) {
        this.color = color;
        this.score = score;
        this.linesCleared = linesCleared;
        this.gameStage = gameStage;
        this.ready = true;
    }

    public PlayerData(char color, int score, int linesCleared, float gameStage, int ready) {
        this.color = color;
        this.score = score;
        this.linesCleared = linesCleared;
        this.gameStage = gameStage;

        if(ready == 0) {
            this.ready = false;
        }
        else {
            this.ready = true;
        }
       // this.id = nextId.getAndIncrement();
    }
//    public PlayerData(Byte[] bytes) {
//
//    }
    public void updateData(int score, int linesCleared, float gameStage) {
        this.score = score;
        this.linesCleared = linesCleared;
        this.gameStage = gameStage;
    }

    public static ArrayList<PlayerData> fromBytes(byte[] array, int playerNum) {
        ArrayList<PlayerData> result = new ArrayList<>();
//        data format from C:
//        (char)player_mark (int)current_score (float)game_state (int)lines_scored (int)ready

        ByteBuffer buffer = ByteBuffer.wrap(array);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for(int i =0; i<playerNum;i++) {
            byte playerMark = buffer.get();
            int currentScore = buffer.getInt();
            float gameState = buffer.getFloat();
            int linesScored = buffer.getInt();
            int ready =  buffer.getInt();
            result.add(new PlayerData((char) playerMark,currentScore,linesScored,gameState,ready));
        }

        return result;
    }

    public String toStringLobby() {
        return String.format("Player: %c, ready: %b \n",color,ready);
    }
    @Override
    public String toString() {
        return String.format("Player: %c, score: %d, linesCleared: %d, gameStage: %.1f \n", color, score, linesCleared, gameStage);
    }
}
