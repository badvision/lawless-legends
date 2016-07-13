package org.badvision.outlaw.plugin.a2pack;

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
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        System.out.println("Goodbye, Apple II packer!");
    }
}
