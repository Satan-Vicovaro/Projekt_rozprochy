package tetris.client.serverRequests;

import tetris.client.game.PlayerData;
import tetris.client.game.Tile;

public class ClientTask {
    public MessageType message;
    private Object data;

    public ClientTask(MessageType message) {
        this.message = message;
        data = null;
    }

    public ClientTask(MessageType message, Object data) {
        this.message = message;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getType() {
        if (data instanceof PlayerData) return "PlayerData";
        if (data instanceof Tile[][]) return  "Tile[][]";
        return "Unknown";
    }
}
