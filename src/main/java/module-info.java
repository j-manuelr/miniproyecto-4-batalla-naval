module com.example.navalbattle {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.navalbattle to javafx.fxml;
    exports com.example.navalbattle;
}