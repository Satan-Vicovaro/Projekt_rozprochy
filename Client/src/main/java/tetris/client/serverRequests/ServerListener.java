package tetris.client.serverRequests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ServerListener extends Thread {
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    public ServerListener(String host, int portNumber) {
        try {
            this.socket = new Socket(host, portNumber);
            this.inStream =  socket.getInputStream();
            this.outStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = "";
                Scanner scanner = new Scanner(System.in);
                message = scanner.nextLine();
                outStream.write(message.getBytes());
                //inStream.mark();

                byte[] inputMessage  =  inStream.readNBytes(10);
                StringBuilder inMessage = new StringBuilder();
                for(byte sign : inputMessage) {
                    inMessage.append((char) sign);
                }
                System.out.println(inMessage);
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
