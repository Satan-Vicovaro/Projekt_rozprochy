package tetris.client.game;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import tetris.client.ResultViewController;
import tetris.client.serverRequests.*;
import tetris.client.ui.UiManager;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    boolean runOnce = false;

    private final ConcurrentLinkedQueue<LinesMessageData> receivedLines;

    final int MAX_SPEED_STATE = 3;
    AnimationTimer gameLoop = new AnimationTimer() {
        // Move piece down, check collision, update score, etc.
        Tetromino currentShape = null;
        long lastUpdate = 0;

        public void updateGame(double deltaTime) {
            if (gameOver) {
                //only update the board
                manager.updateBoard(board.getTiles());
                manager.updateEnemiesBoards();
                manager.updateScoreBoard();
                if(!runOnce) {
                    System.out.println("Last update");
                    listener.sendMessage(new ClientTask(MessageType.UPDATE_BOARD,board.getTiles()));
                    listener.sendMessage(new ClientTask(MessageType.PLAYER_STATUS, PlayerStatus.LOST));
                    runOnce = true;
                }
                return;
            }

            currentShape = getNewShape(currentShape);
            handlePlayersInput(currentShape);

            // checks if after applying gravity tile moved to next square
            if(currentShape.applyGravity(deltaTime)){
                positionChanged = true;
            }
            currentShape.makeMoveBorderValid();

            if (board.checkPlaceShape(currentShape)) { // placing shape
                currentShape = handlePlacingBlock(currentShape);
                while(!receivedLines.isEmpty()){
                    LinesMessageData data = receivedLines.remove();
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
            if(listener.globalEndOfGame()) {
                gameLoop.stop();
                switchToResultView();
            }
        }
    };

    public void switchToResultView() {
        ResultViewController resultView = new ResultViewController(manager.getStage(),listener.getOtherLobbyPlayersData());
        resultView.show();
    }

    public void handleSendingData() {
        if(positionChanged) {

            Tile[][] board =this.board.board;

            Tile[][] copyBoard = new Tile[sizeY][sizeX];
            for(int y = 0; y<sizeY;y++) {
                for(int x = 0; x<sizeX;x++) {
                    copyBoard[y][x] = new Tile(board[y][x].color);
                }
            }
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
                   listener.sendMessage(new ClientTask(MessageType.SEND_LINES_TO_ENEMY, new LinesMessageData((char)('A'+selectedEnemyIndex),linesToSend)));
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

       // polynomial for scaling values
       PolynomialFunction p = new PolynomialFunction(new double[]{0.0666667,-0.4, 0.883333,0.40});
       double coefficient  =  p.value(speedState);
       switch (clearedLines) {
           case 1:
               score +=(int) 40 * coefficient;
               break;
           case 2:
               score += 100 * coefficient;
               this.linesToSend +=1;
               break;
           case 3:
               score += 300 * coefficient;
               this.linesToSend +=2;
               break;
           case 4:
               score += 1200 * coefficient;
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
        this.linesToSend = 2;
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
    }

    public void start() {
        gameLoop.start();
    }
}
