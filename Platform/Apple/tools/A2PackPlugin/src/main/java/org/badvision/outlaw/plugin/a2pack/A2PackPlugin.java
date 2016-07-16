package org.badvision.outlaw.plugin.a2pack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.w3c.dom.Element;

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
        //System.out.println("Hello, menu!");
        System.out.println(Element.ATTRIBUTE_NODE);
    }

    // This is called when our plugin is stopping
    @Deactivate
    public void stop(BundleContext bc) throws Exception {
        //System.out.println("Goodbye, menu!");
    }

    // This identifies the menu item label
    @Override
    public String getName() {
        return "Build Apple II disk";
    }

    public void error(String msg, String context)
    {
        System.out.println("Error: msg=" + msg + ", context=" + context);
    }

    public void warnings(int nWarnings, String str)
    {
        System.out.println("Warnings: nWarnings=" + nWarnings + ", str=" + str);
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

        try {
            System.out.println("Creating A2PackPartitions instance.");
            Class<?> clazz = Class.forName("org.badvision.A2PackPartitions");
            System.out.println("Calling packer.");
            Method m = clazz.getMethod("packWorld", String.class, Object.class);
            m.invoke(null, currentSaveFile.toString(), (Object) this); 
        } catch (Exception ex) {
            System.out.println("...failed: " + ex.toString());
            throw new RuntimeException(ex);
        }
        UIAction.alert("A2_4evr!");
    }
}
