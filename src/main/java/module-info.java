module com.zatribune.devtools {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;

    opens com.zatribune.devtools to javafx.fxml;
    exports com.zatribune.devtools;
}