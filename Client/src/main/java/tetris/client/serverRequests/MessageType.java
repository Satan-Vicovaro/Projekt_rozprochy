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
    MESSAGE_TIMEOUT;

    public static char intoChar(MessageType type) {
        switch (type){
            case NOT_OK -> {return (char)0;}
            case OK -> {return  (char)1;}
            case PLAYER_READY -> {return  (char)2;}
            case PLAYER_NOT_READY -> {return  (char)3;}
            case GET_OTHER_PLAYERS -> {return  (char)4;}
            case UPDATE_BOARD -> {return  (char)5;}
            case UPDATE_SCORE -> {return  (char)6;}
            case SEND_LINES_TO_ENEMY -> {return  (char)7;}
            case START_GAME -> {return  (char)8;}
            case NOT_A_MESSAGE -> {return  (char)9;}
            case MESSAGE_TIMEOUT -> {return  (char)10;}
            case null, default -> {return (char)9;}
        }
    }
    public static byte intoByte(MessageType type) {
        switch (type){
            case NOT_OK -> {return (byte)0;}
            case OK -> {return  (byte)1;}
            case PLAYER_READY -> {return  (byte)2;}
            case PLAYER_NOT_READY -> {return  (byte)3;}
            case GET_OTHER_PLAYERS -> {return  (byte)4;}
            case UPDATE_BOARD -> {return  (byte)5;}
            case UPDATE_SCORE -> {return  (byte)6;}
            case SEND_LINES_TO_ENEMY -> {return  (byte)7;}
            case START_GAME -> {return  (byte)8;}
            case NOT_A_MESSAGE -> {return  (byte)9;}
            case MESSAGE_TIMEOUT -> {return  (byte)10;}
            case null, default -> {return (byte)9;}
        }
    }
}
