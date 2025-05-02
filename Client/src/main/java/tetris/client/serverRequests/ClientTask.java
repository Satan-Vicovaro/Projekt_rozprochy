package tetris.client.serverRequests;

import tetris.client.game.PlayerData;
import tetris.client.game.Tile;

import java.util.ArrayList;

public class ClientTask {
    public MessageType message;
    private Object data;
    private PlayerData playerData;

    public ClientTask(MessageType message) {
        this.message = message;
        this.data = null;
    }

    public ClientTask(MessageType message, Object  data) {
        this.message = message;
        this.data = data;
    }

    public Object  getData() {
        return this.data;
    }
    public PlayerData getPlayerData() {
        return playerData;
    }
}
