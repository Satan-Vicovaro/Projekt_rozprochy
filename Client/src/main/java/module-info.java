module tetris.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;

    opens tetris.client to javafx.fxml;
    exports tetris.client;
}