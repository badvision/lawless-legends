package jace;

import org.junit.BeforeClass;

import javafx.application.Platform;

public abstract class AbstractFXTest {
    @BeforeClass
    public static void initJfxRuntime() {
        Platform.startup(() -> {});
    }    
}
