module tetris.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires annotations;

    opens tetris.client to javafx.fxml;
    exports tetris.client;
}