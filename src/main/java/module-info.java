module org.example.tunesfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.lwjgl.openal;
    requires java.datatransfer;
    requires java.desktop;
    requires javafx.graphics;

    opens org.example.tunesfx to javafx.fxml, javafx.graphics;
    exports org.example.tunesfx;
    exports org.example.tunesfx.utils;
    opens org.example.tunesfx.utils to javafx.fxml, javafx.graphics;
    exports org.example.tunesfx.audio;
    opens org.example.tunesfx.audio to javafx.fxml, javafx.graphics;
    opens fonts to javafx.graphics;
    exports org.example.tunesfx.controller;
    opens org.example.tunesfx.controller to javafx.fxml, javafx.graphics;
    exports org.example.tunesfx.synth;
    opens org.example.tunesfx.synth to javafx.fxml, javafx.graphics;

}