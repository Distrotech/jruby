package org.jruby.osgi.gems;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class GemsBundleActivator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
	System.out.println("Hello World!");
    }

    public void stop(BundleContext context) throws Exception {
	System.out.println("Bye World!");
    }
}
