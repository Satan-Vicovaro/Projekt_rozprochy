package tetris.client.serverRequests;

import tetris.client.game.GameBoard;
import tetris.client.game.PlayerData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerListener extends Thread {
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;

    private boolean playerConnectedToLobby;
    private boolean playerReady;
    private char playerMark;
    private boolean startGame;
    private BlockingQueue<MessageType> messagesFromGameClient;
    private ArrayList<PlayerData> otherPlayersData;

    public ServerListener(String host, int portNumber) throws IOException{

        this.socket = new Socket(host, portNumber);
        this.socket.setSoTimeout(250);
        this.inStream =  socket.getInputStream();
        this.outStream = socket.getOutputStream();
        this.playerReady = false;
        this.startGame = false;
        this.messagesFromGameClient = new LinkedBlockingQueue<>();
        this.otherPlayersData = new ArrayList<>();
    }

    public void connectToLobby() {
        while(true) {
            try {
                String message = "connect to lobby";
                outStream.write(message.getBytes());

                byte[] inputMessage  =  inStream.readNBytes(1);

                this.playerMark =(char) inputMessage[0];
                System.out.println(playerMark);
                break;
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public MessageType listenForMessage() {
        try {
            byte[] inputMessage = inStream.readNBytes(1);
            System.out.println("Received " + inputMessage[0]);
            return switch (inputMessage[0]) {
                case 0 -> MessageType.NOT_OK;
                case 1 -> MessageType.OK;
                case 2 -> MessageType.PLAYER_READY;
                case 3 -> MessageType.PLAYER_NOT_READY;
                case 4 -> MessageType.GET_OTHER_PLAYERS;
                case 5 -> MessageType.UPDATE_BOARD;
                case 6 -> MessageType.UPDATE_SCORE;
                case 7 -> MessageType.SEND_LINES_TO_ENEMY;
                case 8 -> MessageType.START_GAME;
                default -> {
                    System.out.println("Received: Unknown message");
                    yield MessageType.NOT_OK;
                }
            };
        }catch (SocketTimeoutException e) {
            //System.out.println("listenForMessage: Waited to long for message");
            return MessageType.MESSAGE_TIMEOUT;
        }catch (IOException e) {
            System.out.println(e);
        }
        return MessageType.NOT_A_MESSAGE;
    }

    public void lobbyLoop() {
        while(!startGame) {
            //handling messages form Client
            while(!messagesFromGameClient.isEmpty()) {
                MessageType message = messagesFromGameClient.remove();
                switch (message) {
                    case PLAYER_READY -> sendPlayerIsReady();
                    case PLAYER_NOT_READY -> sendPlayerIsNotReady();
                    case GET_OTHER_PLAYERS -> sendToGetOtherLobbyPlayers();
                    default -> System.out.println("lobbyLoop: got wrong message");
                }
            }

            //waiting for messages from server
            MessageType serverMessage = listenForMessage();

            switch (serverMessage) {
                case START_GAME ->{
                    this.startGame = true;
                }
            }
        }
    }
    public void gameLoop() {
        while (true) {
            int a = 0;
            a++;
        }
    }

    @Override
    public void run() {
        try {
            connectToLobby();
            lobbyLoop();
            gameLoop();
            listenForMessage();
        }catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean isPlayerReady() {
        return playerReady;
    }

    public synchronized void sendMessage(MessageType task) {
        this.messagesFromGameClient.add(task);
    }
    
    public void sendPlayerIsReady() {
        try {
            System.out.println("sending: player is ready");
            byte[] message = {2}; // 2 <-> send: player_ready
            this.outStream.write(message);
            byte[] answer = inStream.readNBytes(1);
            if(answer[0] == 1) { // 1 = OK
                this.playerReady = true;
            }
        } catch (IOException e) {
            System.out.println();
        }
    }

    public void sendPlayerIsNotReady() {
        System.out.println("sending: player is not ready");
        try {
            byte[] message = {3}; // 3 <-> send: player_not_ready
            this.outStream.write(message);

            byte[] answer = inStream.readNBytes(1);
            if(answer[0]==1) {
                this.playerReady = false;
            }
        } catch (IOException e) {
            System.out.println();
        }
    }
    public synchronized ArrayList<PlayerData> getOtherLobbyPlayersData() {
            return this.otherPlayersData;
    }

    public void sendToGetOtherLobbyPlayers() {
        try {
           this.outStream.write(4); // 4 <-> get_other_players

           byte[] answerInfo = inStream.readNBytes(5);
           int messageLength = 0;
           for(int i = 0; i < 4;i++) {
               messageLength += (messageLength<<8) + answerInfo[i] & 0xFF;
           }
           int playerNumber = answerInfo[4];
           byte[] data = inStream.readNBytes(messageLength - 5);

            this.otherPlayersData = PlayerData.fromBytes(data, playerNumber);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public char getPlayerMark() {
        return this.playerMark;
    }
    public boolean getStartGame() {
        return this.startGame;
    }

    public void sendPlayerLost() {

    }

    public void sendPlayerBoard(GameBoard board) {
        Vector<Byte> arrayVector =board.getByteBoardArray();

        byte[] byteArray = new byte[arrayVector.size() + 1];
        for (int i = 1; i < arrayVector.size()+1; i++) {
            byteArray[i] = arrayVector.get(i-1);
        }
        byteArray[0] = 5; // 5 = update_board_m
        try {
            this.outStream.write(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendScore(PlayerData data) {
        System.out.println("TODO sendScore");
        return;
    }

    public void getLeaderBoard() {

    }

    public void getEnemiesLines() {

    }

    public void getEnemiesBoards() {

    }
}
