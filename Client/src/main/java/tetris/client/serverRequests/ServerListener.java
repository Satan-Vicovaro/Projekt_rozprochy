package tetris.client.serverRequests;

import tetris.client.game.GameBoard;
import tetris.client.game.PlayerData;
import tetris.client.game.PlayerDataComparator.PlayerDataComparator;
import tetris.client.game.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

public class ServerListener extends Thread {
    private final Socket socket;
    private final InputStream inStream;
    private final OutputStream outStream;

    private final ConcurrentLinkedQueue<ClientTask> messagesFromGameClient;
    private final List<PlayerData> otherPlayersData;
    private ArrayList<Tile[][]> enemiesBoards;
    private int currentPlayerNumber;


    private boolean playerConnectedToLobby;
    private boolean playerReady;
    private boolean startGame;
    private char playerMark;

    public ServerListener(String host, int portNumber) throws IOException{

        this.socket = new Socket(host, portNumber);
        this.socket.setSoTimeout(250);
        this.inStream =  socket.getInputStream();
        this.outStream = socket.getOutputStream();
        this.playerReady = false;
        this.startGame = false;
        this.messagesFromGameClient = new ConcurrentLinkedQueue<>();
        this.otherPlayersData = Collections.synchronizedList(new ArrayList<>());
        this.currentPlayerNumber = 0;
        this.enemiesBoards = new ArrayList<>();
    }

    public ConcurrentLinkedQueue<ClientTask> getMessagesFromGameClient() {
        return messagesFromGameClient;
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
                MessageType message = messagesFromGameClient.remove().message;
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
            while(!messagesFromGameClient.isEmpty()) {
                ClientTask messageType = messagesFromGameClient.remove();
                switch (messageType.message){
                    case UPDATE_BOARD -> sendPlayerBoard((Tile[][]) messageType.getData());
                    case UPDATE_SCORE -> sendScore((PlayerData) messageType.getData());
                }
            }

            //receive messages from server
            MessageType message = listenForMessage();
            switch (message){
                case UPDATE_BOARD -> receiveUpdatedBoard();
                case UPDATE_SCORE -> receiveUpdatedScore();
            }
        }
    }

    @Override
    public void run() {
        try {
            connectToLobby();
            lobbyLoop();
            gameLoop();
            //listenForMessage();
        }catch (Exception e) {
            System.out.println(e);
        }
    }


    private void sendPlayerIsReady() {
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

    private void sendPlayerIsNotReady() {
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

    private void sendToGetOtherLobbyPlayers() {
        try {
           //receiving all players currently connected to server
            this.outStream.write(4); // 4 <-> get_other_players

           byte[] answerInfo = inStream.readNBytes(5);
           int messageLength = 0;
           for(int i = 0; i < 4;i++) {
               messageLength += (messageLength<<8) + answerInfo[i] & 0xFF;
           }
           int playerNumber = answerInfo[4];

           // setting up empty boards
           while (currentPlayerNumber < playerNumber) {
               enemiesBoards.add(Tile.initBoard(10,20));
               currentPlayerNumber++;
           }
           this.currentPlayerNumber = playerNumber;

           byte[] data = inStream.readNBytes(messageLength - 5);

           List<PlayerData> newData = PlayerData.fromBytes(data, playerNumber);
           synchronized (otherPlayersData) {
               otherPlayersData.clear();
               otherPlayersData.addAll(newData);
           }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean isPlayerReady() {
        return playerReady;
    }
    public synchronized void sendMessage(ClientTask task) {
        this.messagesFromGameClient.add(task);
    }
    public synchronized List<PlayerData> getOtherLobbyPlayersData() {
        return this.otherPlayersData;
    }
    public char getPlayerMark() {
        return this.playerMark;
    }
    public boolean getStartGame() {
        return this.startGame;
    }
    public int getCurrentPlayerNumber() {
        return  this.currentPlayerNumber;
    }

    public ArrayList<Tile[][]> getEnemiesBoards() {
        return this.enemiesBoards;
    }

    private void sendPlayerLost() {

    }

    private void sendPlayerBoard(Tile[][] boardTile) {

        Vector<Byte>byteVector = new Vector<>();
        for(Tile[] row: boardTile) {
            byteVector.addAll(List.of(Tile.tileArrayToBytes(row)));
        }

        byte[] byteArray = new byte[byteVector.size() + 1];
        for (int i = 1; i < byteVector.size()+1; i++) {
            byteArray[i] = byteVector.get(i-1);
        }
        byteArray[0] = MessageType.intoByte(MessageType.UPDATE_BOARD);
        try {
            System.out.println("Sending updated position");
            outStream.write(byteArray);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void sendScore(PlayerData data) {
        byte[] message = data.toBytes();
        message[0] = MessageType.intoByte(MessageType.UPDATE_SCORE);
        try {
            System.out.println("sending updated score");
            outStream.write(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveUpdatedBoard() {
        // message format:
        // (char) player_mark (char) board[20][10]
        try {
            int bufferLen = 20*10 + 1;
            byte[] buffer = inStream.readNBytes(bufferLen);

            char playerMark = (char) buffer[0];
            System.out.println("Received position: " + playerMark);

            int playerId = playerMark - 'A';
            Tile[][] board = this.enemiesBoards.get(playerId);

            int x = 0;
            int y = 0;
            for(int i = 0 ; i < bufferLen - 1; i++) {
                y = i/10;
                x = i%10;
                board[y][x].color = (char) buffer[i + 1];
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void receiveUpdatedScore() {
        //message format:
        //(char)player_mark (int)currentScore (float)gameStage (int)linesCleared
        try {
            int bufferLen = 1+4+4+4;
            ByteBuffer buffer =ByteBuffer.wrap(inStream.readNBytes(bufferLen)).order(ByteOrder.LITTLE_ENDIAN);
            char playerMark = (char) buffer.get();
            int currentScore = buffer.getInt();
            float gameStage = buffer.getFloat();
            int linesCleared = buffer.getInt();

            int playerIndex = playerMark - 'A';
            synchronized (otherPlayersData) {
                otherPlayersData.get(playerIndex).updateData(currentScore,linesCleared,gameStage);
                otherPlayersData.sort(new PlayerDataComparator());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
