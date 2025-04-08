module com.example.client_java {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.almasb.fxgl.all;

    opens com.example.client_java to javafx.fxml;
    exports com.example.client_java;
}