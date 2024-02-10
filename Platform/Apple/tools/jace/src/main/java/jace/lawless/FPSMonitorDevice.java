package jace.lawless;

import jace.LawlessLegends;
import jace.apple2e.SoftSwitches;
import jace.core.Device;
import jace.core.Motherboard;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Simple device that displays speed and fps stats
 */
public class FPSMonitorDevice extends Device {
    public static final long UPDATE_CHECK_FREQUENCY = 1000;
    Label cpuSpeedIcon;
    Label fpsIcon;

    long checkCounter = 0;
    long tickCounter = 0;
    int frameCounter = 0;
    long lastUpdate = 0;
    long UPDATE_INTERVAL = 1000/2;
    boolean lastVBLState = false;
    
    public FPSMonitorDevice() {
    }
    
    @Override
    protected String getDeviceName() {
        return "FPS Monitor";
    }

    @Override
    public void tick() {
        tickCounter += Math.min(Motherboard.cpuPerClock, 1);
        boolean vblState = SoftSwitches.VBL.getSwitch().getState();
        if (!vblState && lastVBLState) {
            frameCounter++;
        }
        lastVBLState = vblState;
        if (--checkCounter <= UPDATE_CHECK_FREQUENCY) {
            updateIcon();
            checkCounter = UPDATE_CHECK_FREQUENCY;
        }
    }
    
    Label initLabel(Label l) {
         l.setTextFill(Color.WHITE);
         l.setEffect(new DropShadow(2.0, Color.BLACK));
         l.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.8), new CornerRadii(5.0), new Insets(-5.0))));
         l.setMinWidth(64.0);
         l.setMaxWidth(Region.USE_PREF_SIZE);
         return l;   
    }
    
    void updateIcon() {
        long now = System.currentTimeMillis();
        long ellapsed = now - lastUpdate;
        if (ellapsed < UPDATE_INTERVAL) {
            return;
        }
        
        if (cpuSpeedIcon == null) {
            cpuSpeedIcon = initLabel(new Label());
            fpsIcon = initLabel(new Label());
        }
        LawlessLegends.getApplication().controller.addIndicator(cpuSpeedIcon,1000);
        LawlessLegends.getApplication().controller.addIndicator(fpsIcon,1000);                    
        double secondsEllapsed = ((double) ellapsed) / 1000.0;
        double speed = ((double) tickCounter) / secondsEllapsed / 1000000.0;
        double fps = ((double) frameCounter)/secondsEllapsed;
        String mhzStr = String.format("%1.1fmhz", speed);
        String fpsStr = String.format("%1.1ffps", fps);
//        System.out.println(mhzStr+";"+fpsStr);
        Platform.runLater(()->{
            cpuSpeedIcon.setText(mhzStr);
            fpsIcon.setText(fpsStr);
        });
        // Reset counters
        lastUpdate = now;
        tickCounter = 0;
        frameCounter = 0;
    }

    @Override
    public String getShortName() {
        return "fps";
    }

    @Override
    public void reconfigure() {        
        tickCounter = 0;        
        frameCounter = 0;
    }

    @Override
    public void detach() {
        if (cpuSpeedIcon != null) {
            LawlessLegends.getApplication().controller.removeIndicator(cpuSpeedIcon);
            LawlessLegends.getApplication().controller.removeIndicator(fpsIcon);
        }        
    }
}
