module org.example.tunesfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.lwjgl.openal;
    requires java.datatransfer;

    opens org.example.tunesfx to javafx.fxml, javafx.graphics;
    exports org.example.tunesfx;
}