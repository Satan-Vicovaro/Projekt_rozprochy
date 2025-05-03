package tetris.client.serverRequests;

public enum PlayerStatus {
    NOT_READY,
    READY,
    JOINING_LOBBY,
    PLAYING,
    LOST,
    ERROR;

    public static byte intoByte(PlayerStatus status) {
        switch (status) {
            case NOT_READY -> {
                return 0;
            }
            case READY -> {return 1;}
            case JOINING_LOBBY -> {
                return 2;
            }
            case PLAYING -> {return 3;}
            case LOST -> {return 4;}
            case ERROR -> {return 5;}
            case null, default -> {return 5;}
        }
    }
}
