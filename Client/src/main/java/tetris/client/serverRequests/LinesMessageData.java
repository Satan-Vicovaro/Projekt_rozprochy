package tetris.client.serverRequests;

public class LinesMessageData {
    public char senderMark;
    public int numberOfLines;

    public LinesMessageData(char senderMark, int numberOfLines) {
        this.senderMark = senderMark;
        this.numberOfLines = numberOfLines;
    }
}
