package org.badvision.outlaw.plugin.example;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.badvision.outlaweditor.api.ApplicationState;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author blurry
 */
public class Activator implements BundleActivator {

    
    public void start(BundleContext bc) throws Exception {
        System.out.println("Hello, world!");
        ApplicationState app = bc.getService(bc.getServiceReference(ApplicationState.class));
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

    public void stop(BundleContext bc) throws Exception {
    }
    
}
