package org.badvision.outlaw.plugin.example;

import javafx.event.ActionEvent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.badvision.outlaweditor.api.ApplicationState;
import org.badvision.outlaweditor.api.MenuAction;
import org.osgi.framework.BundleContext;


/**
 * This 
 * @author blurry
 */
@Component(immediate = true)
@Service(MenuAction.class)
public class ExamplePlugin implements MenuAction {
    
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
        return "Example action";
    }

    // This method is called when the user selects the menu item
    @Override
    public void handle(ActionEvent event) {
        System.out.println("Clicked!");
        checkReferences();
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
