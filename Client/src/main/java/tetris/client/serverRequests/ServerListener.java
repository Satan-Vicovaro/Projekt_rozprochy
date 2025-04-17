package tetris.client.serverRequests;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ServerListener extends Thread {
    private Socket socket;
    private InputStream stream;
    public ServerListener(String host, int portNumber) {
        try {
            this.socket = new Socket(host, portNumber);
            this.stream =  socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = "";
                stream.read(message.getBytes());
            }
        }catch (Exception e) {

        }
    }

    public void sendPlayerIsReady() {
        System.out.println("sending: player is ready");
    }

    public void sendPlayerIsNotReady() {
        System.out.println("sending: player is not ready");
    }

    public boolean getStartGame() {
        return true;
    }

    public void sendPlayerLost() {

    }

    public void getLeaderBoard() {

    }

    public void getEnemiesLines() {

    }

    public void getEnemiesBoards() {

    }
}
