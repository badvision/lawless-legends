package org.badvision.outlaw.plugin.example;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author blurry
 */
public class Activator implements BundleActivator {

    public void start(BundleContext bc) throws Exception {
        System.out.println("Hello, world!");
    }

    public void stop(BundleContext bc) throws Exception {
    }
    
}
