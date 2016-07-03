package org.badvision.outlaw.plugin.example;

import org.badvision.outlaweditor.api.ApplicationState;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This is an example activator class, provided to demonstrate basic bundle lifecycle events.
 * Since we're using declarative servies (Felix SCR), all the messy bits of service registration
 * and reference passing are managed for us.  Otherwise we'd be doing that here.  Fortunately,
 * we don't have to do all that.
 * 
 * Still, this is a useful mechanism if you have some one-time setup or shutdown concerns that apply
 * to your whole bundle, such as reading configuration data from a file or whatever.
 * 
 * @author blurry
 */
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext bc) throws Exception {
        System.out.println("Hello, plugin!");
        checkReferences();
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        System.out.println("Goodbye, plugin!");
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
