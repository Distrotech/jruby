package org.jruby.osgi.scripts;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ScriptsBundleActivator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
	System.out.println("Hello World!");
    }

    public void stop(BundleContext context) throws Exception {
	System.out.println("Bye World!");
    }
}
