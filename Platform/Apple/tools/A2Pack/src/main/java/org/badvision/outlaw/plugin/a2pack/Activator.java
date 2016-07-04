package org.badvision.outlaw.plugin.a2pack;

import org.badvision.outlaweditor.api.ApplicationState;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This is the activator for the A2Pack plugin, in charge of starting and stopping.
 * 
 * @author mhaye
 */
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext bc) throws Exception {
        System.out.println("Hello, Apple II packer!");
        checkReferences();
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        System.out.println("Goodbye, Apple II packer!");
    }

    private void checkReferences() {
        // Note that our activator is not a component, so we can't use the @Reference
        // annotation to inject app automatically.  ApplicationState has a convenience
        // method to get around this in just such events, but it's a hack.
        // Ultimately it's not a good idea to rely on this too much as it follows
        // some bad practices behind the scene that leave unclosed references, etc.
        // I'll have to come up with a safer way to inject dependencies without
        // causing housekeeping issues for OSGi.
        ApplicationState app = ApplicationState.getInstance();
        if (app == null) {
            System.out.println("App is null?!?!");
        } else if (app.getCurrentPlatform() == null) {
            System.out.println("Current platform is null?");
        } else {
            System.out.println("Current platform is " + app.getCurrentPlatform());
        }        
    }
}
