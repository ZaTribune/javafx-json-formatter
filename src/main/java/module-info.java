module com.tribune.devtools {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;
    requires java.logging;

    opens com.tribune.devtools to javafx.fxml;
    exports com.tribune.devtools;
}