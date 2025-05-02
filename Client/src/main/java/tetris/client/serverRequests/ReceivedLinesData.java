package tetris.client.serverRequests;

public class ReceivedLinesData {
    public char senderMark;
    public int numberOfLines;

    public ReceivedLinesData(char senderMark, int numberOfLines) {
        this.senderMark = senderMark;
        this.numberOfLines = numberOfLines;
    }
}
