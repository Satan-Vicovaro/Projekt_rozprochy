package tetris.client.serverRequests;

public enum MessageType {
    NOT_OK,
    OK,
    PLAYER_READY,
    PLAYER_NOT_READY,
    GET_OTHER_PLAYERS,
    UPDATE_BOARD,
    UPDATE_SCORE,
    SEND_LINES_TO_ENEMY,
    START_GAME,
    NOT_A_MESSAGE,
    MESSAGE_TIMEOUT
}
