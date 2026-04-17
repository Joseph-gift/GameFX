module com.example.gamefx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;


    opens com.example.gamefx to javafx.fxml;
    exports com.example.gamefx;
}
