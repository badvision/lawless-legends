package org.badvision.outlaw.plugin.a2pack;

import java.io.File;
import javafx.event.ActionEvent;
import javax.xml.bind.JAXB;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.api.MenuAction;
import org.badvision.outlaweditor.ui.UIAction;
import org.osgi.framework.BundleContext;


/**
 * This registers a plugin which creates Apple II disk images.
 * @author mhaye
 */
@Component(immediate = true)
@Service(MenuAction.class)
public class A2PackPlugin implements MenuAction {
    
    // Note: Because ApplicationState is already a defined service, this will automatically be bound.
    // Hence, it is not necessary to worry about passing it it.
    @Reference
    ApplicationState app;
    
    // This is called when our plugin is starting
    @Activate
    public void activate() throws Exception {
        System.out.println("Hello, menu!");
        checkReferences();
    }

    // This is called when our plugin is stopping
    @Deactivate
    public void stop(BundleContext bc) throws Exception {
        System.out.println("Goodbye, menu!");
    }

    // This identifies the menu item label
    @Override
    public String getName() {
        return "Build Apple II disk";
    }

    // This method is called when the user selects the menu item
    @Override
    public void handle(ActionEvent event) 
    {
        File currentSaveFile = UIAction.getCurrentSaveFile();
        if (currentSaveFile == null || !currentSaveFile.exists()) {
            UIAction.alert("You must open a world file\nbefore building a disk.");
            return;
        }
        System.out.println("Clicked X!");
        JAXB.marshal(ApplicationState.getInstance().getGameData(), System.out);
        checkReferences();
        UIAction.alert("A2_4evr!");
    }

    private void checkReferences() {
//        app = ApplicationState.getInstance();
        if (app == null) {
            System.out.println("App is null?!?!");
        } else {
            if (app.getCurrentPlatform() == null) {
                System.out.println("Current platform is null?");
            } else {
                System.out.println("Current platform is "+app.getCurrentPlatform());            
            }
        }        
    }
}
