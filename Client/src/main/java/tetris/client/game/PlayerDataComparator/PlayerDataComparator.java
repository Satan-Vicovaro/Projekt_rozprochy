package tetris.client.game.PlayerDataComparator;

import tetris.client.game.PlayerData;

import java.util.Comparator;

public class PlayerDataComparator implements Comparator<PlayerData> {
    @Override
    public int compare(PlayerData o1, PlayerData o2) {
        if(o1.score > o2.color) {
            return 1;
        } else if (o1.score < o2.score) {
            return -1;
        } else if (o1.color > o2.color) {
            return 1;
        } else if (o1.color < o2.color) {
            return -1;
        }else{
            return 0;
        }

    }
}
