package tetris.client.game;

import javafx.animation.AnimationTimer;
import tetris.client.serverRequests.ClientTask;
import tetris.client.serverRequests.MessageType;
import tetris.client.serverRequests.ReceivedLinesData;
import tetris.client.serverRequests.ServerListener;
import tetris.client.ui.UiManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TetrisGame {
    GameBoard board;
    int sizeX;
    int sizeY;

    UiManager manager;

    float speedState;
    Vector2d fallingSpeed;

    int score;
    int totalLinesCleared;

    int linesToSend;
    int selectedEnemyIndex;

    PlayerData playerData;

    ServerListener listener;

    boolean gameOver = false;
    boolean positionChanged = false;
    boolean scoreChanged = false;

    private final ConcurrentLinkedQueue<ReceivedLinesData> receivedLines;

    final int MAX_SPEED_STATE = 3;
    AnimationTimer gameLoop = new AnimationTimer() {
        // Move piece down, check collision, update score, etc.
        Tetromino currentShape = null;
        long lastUpdate = 0;

        public void updateGame(double deltaTime) {
            if (gameOver) {
                //only update the board
                return;
            }

            currentShape = getNewShape(currentShape);
            handlePlayersInput(currentShape);

            if(currentShape.applyGravity(deltaTime)){
                positionChanged = true;
            }
            currentShape.makeMoveBorderValid();

            if (board.checkPlaceShape(currentShape)) { // placing shape
                currentShape = handlePlacingBlock(currentShape);
                while(!receivedLines.isEmpty()){
                    ReceivedLinesData data = receivedLines.remove();
                    board.addLinesToBottomOfBoard(data.numberOfLines, data.senderMark);
                }
                return; // creating a new one in the next loop
            }

            board.addToBoard(currentShape);
            manager.updateBoard(board.getTiles());

            handleSendingData();

            manager.updateEnemiesBoards();
            manager.updateScoreBoard();
            playerData.updateData(score,totalLinesCleared,speedState);

            manager.updateOurPlayerScore(linesToSend);

            board.removeFromBoard(currentShape);
        }

        @Override
        public void handle(long now) {

            if (lastUpdate > 0) {
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // seconds
                updateGame(deltaTime);
            }
             lastUpdate = now;
            if (gameOver) {
                this.stop();
                //manager.closeProgram();
            }
        }
    };

    public void handleSendingData() {
        if(positionChanged) {

            Tile[][] board =this.board.board;

            Tile[][] copyBoard = new Tile[sizeY][sizeX];
            for(int y = 0; y<sizeY;y++) {
                for(int x = 0; x<sizeX;x++) {
                    copyBoard[y][x] = new Tile(board[y][x].color);
                }
            }
            System.out.println("Tetris: sending board");
            listener.sendMessage(new ClientTask(MessageType.UPDATE_BOARD, copyBoard));
            positionChanged = false;
        }
        if(scoreChanged) {
            listener.sendMessage(new ClientTask(MessageType.UPDATE_SCORE, this.playerData));
            scoreChanged = false;
        }
    }

    public Tetromino getNewShape(Tetromino currentShape) {
        // get random Tetromino if currently no one is used by player
        if (currentShape == null) {
            currentShape = new Tetromino(new Vector2d((float)(sizeX/2), 0),sizeX,sizeY);
            currentShape.setVelocity(fallingSpeed);
            positionChanged = true;
        }
        return currentShape;
    }

    public void handlePlayersInput(Tetromino currentShape) {
       // Player keyboard input
       char input =  manager.getUserInput();

       // if player moved a piece
       if (input != 0) {
           positionChanged = true;
           if (input =='A') {
               // move left
               currentShape.shiftBy(Vector2d.ones(Direction.LEFT));;
           } else if (input =='D') {
               // move right
               currentShape.shiftBy(Vector2d.ones(Direction.RIGHT));
           }else if (input == 'W') {
               while (!board.checkPlaceShape(currentShape)){
                   currentShape.shiftBy(Vector2d.ones(Direction.DOWN));
               }
           }else if (input == 'S') {
               currentShape.shiftBy(new Vector2d(0,1));
           } else if (input == 'Q') {
               currentShape.rotateLeft();
           } else if(input == 'E') {
               currentShape.rotateRight();
           } else if (input == 'K') {
               positionChanged = false;
               if(selectedEnemyIndex < listener.getCurrentPlayerNumber() - 1)
                   this.selectedEnemyIndex++;
               manager.markSelectedBoard(selectedEnemyIndex);
           }else if (input == 'J') {
               positionChanged = false;
               if(selectedEnemyIndex>0)
                   this.selectedEnemyIndex--;
               manager.markSelectedBoard(selectedEnemyIndex);
           } else if (input == 'F') {
               // selectedEnemyIndex != listener.getMyIndex()
               if (linesToSend > 0) {
                   listener.sendMessage(new ClientTask(MessageType.SEND_LINES_TO_ENEMY, linesToSend));
               }
               linesToSend = 0;
           }
       }
    }
   public Tetromino handlePlacingBlock(Tetromino currentShape) {

       board.addToBoard(currentShape);
       manager.updateBoard(board.getTiles());
       currentShape = null;
       int clearedLines = board.handleLines();
       if (clearedLines == 0) {
           if (board.gameOver()) {
               gameOver = true;
           }
           return currentShape;
       }
       totalLinesCleared += clearedLines;
       scoreChanged = true;
       switch (clearedLines) {
           case 1:
               score +=40;
               break;
           case 2:
               score += 100;
               this.linesToSend +=1;
               break;
           case 3:
               score += 300;

               this.linesToSend +=2;
               break;
           case 4:
               score += 1200;

               this.linesToSend +=3;
               break;
           default:
               break;
       }

       if (totalLinesCleared > 10 * speedState && speedState < MAX_SPEED_STATE) {
           speedState += 0.5F;
           fallingSpeed.mulBy(speedState); // adjusting fall speed
       }
       return currentShape;
   }
    public TetrisGame(int sizeX, int sizeY, UiManager manager, ServerListener listener) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        board = new GameBoard(sizeX,sizeY, listener.getPlayerMark());
        this.manager = manager;
        this.speedState = 1;
        this.fallingSpeed = new Vector2d(0,1F);
        this.totalLinesCleared = 0;
        this.score = 0;
        this.linesToSend = 0;
        this.playerData = new PlayerData(listener.getPlayerMark(), score,totalLinesCleared,speedState);
        this.manager.addPlayerData(playerData);
        this.listener = listener;
        this.receivedLines = listener.getReceivedLines();
    }

    public void init() {
        manager.init();
        manager.initEnemyBoards();
        manager.loadEnemiesGrids();
        manager.updateBoard(board.getTiles());
        manager.initSelectedBoardMark();
        manager.run();
    }

    public void start() {
        gameLoop.start();
    }
}
