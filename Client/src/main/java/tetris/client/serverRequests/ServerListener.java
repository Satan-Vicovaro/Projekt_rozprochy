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

    private boolean playerConnectedToLobby;
    private boolean playerReady;
    public ServerListener(String host, int portNumber) {
        try {
            this.socket = new Socket(host, portNumber);
            this.inStream =  socket.getInputStream();
            this.outStream = socket.getOutputStream();
            this.playerReady = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectToLobby() {
        while(true) {
            try {
                String message = "connect to lobby";
                outStream.write(message.getBytes());

                byte[] inputMessage  =  inStream.readNBytes(1);
//                StringBuilder inMessage = new StringBuilder();
//                for(byte sign : inputMessage) {
//                    inMessage.append((char) sign);
//                }
                char player =(char) inputMessage[0];
                System.out.println(player);
                break;
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    @Override
    public void run() {
        try {
            connectToLobby();
            while(true) {
               int a = 0;
               a++;
            }
        }catch (Exception e) {

        }
    }

    public boolean isPlayerReady() {
        return playerReady;
    }

    public void sendPlayerIsReady() {
        try {
            System.out.println("sending: player is ready");
            String message = "ready\0";
            this.outStream.write(message.getBytes());
            byte[] answer = inStream.readNBytes(1);
            if(answer[0]==1) {
                this.playerReady = true;
            }
        } catch (IOException e) {
            System.out.println();
        }
    }

    public void sendPlayerIsNotReady() {
        System.out.println("sending: player is not ready");
        try {
            String message = "not ready\0";
            this.outStream.write(message.getBytes());

            byte[] answer = inStream.readNBytes(1);
            if(answer[0]==1) {
                this.playerReady = false;
            }
        } catch (IOException e) {
            System.out.println();
        }
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
