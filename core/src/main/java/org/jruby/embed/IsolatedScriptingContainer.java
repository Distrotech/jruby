package org.jruby.embed;

import java.net.URL;

/**
 * the IsolatedScriptingContainer detects the whether it is used with
 * a Thread.currentThread.contextClassLoader (J2EE) or with the classloader
 * which loaded IsolatedScriptingContainer.class (OSGi case)
 * 
 * the setup of LOAD_PATH and GEM_PATH and JRUBY_HOME uses ONLY uri: or uri:classloader:
 * protocol paths. i.e. everything lives within one or more classloaders - no jars added from
 * jave.class.path or similar "magics"
 *
 * the root of the "main" classloader is add to LOAD_PATH and GEM_PATH.
 *
 * in the OSGi case there are helper methods to add ClassLoaders to the LOAD_PATH or GEM_PATH
 */
public class IsolatedScriptingContainer extends ScriptingContainer {

    private static final String JRUBYDIR = "/.jrubydir";
    private static final String JRUBY_HOME = "/META-INF/jruby.home";
    private static final String JRUBY_HOME_DIR = JRUBY_HOME + JRUBYDIR;
    
    private boolean isContextClassLoader;
    
    public IsolatedScriptingContainer()
    {
        this(LocalContextScope.SINGLETON);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior )
    {
        this(scope, behavior, true);
    }

    public IsolatedScriptingContainer( LocalContextScope scope )
    {
        this(scope, LocalVariableBehavior.TRANSIENT);
    }

    public IsolatedScriptingContainer( LocalVariableBehavior behavior )
    {
        this(LocalContextScope.SINGLETON, behavior);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior,
                                       boolean lazy )
    {
        super( scope, behavior, lazy );
        isContextClassLoader = true;
        URL home = Thread.currentThread().getContextClassLoader().getResource( JRUBY_HOME_DIR.substring( 1 ) );
        if ( home == null ) {
            isContextClassLoader = false;
            home = this.getClass().getClassLoader().getResource( JRUBY_HOME_DIR );
            if ( home == null ) {
                throw new RuntimeException( "BUG can not find " + JRUBY_HOME_DIR );
            }
            setClassLoader( this.getClass().getClassLoader() );
            setHomeDirectory( "uri:" + home.toString().replaceFirst( JRUBYDIR + "$", "" ) );
        }
        else {
            setHomeDirectory( "uri:classloader:" + JRUBY_HOME );
        }

        // clean up LOAD_PATH
        runScriptlet( "$LOAD_PATH.delete_if{|p| p =~ /jar$/ }" );
        
        if ( isContextClassLoader ) {
            runScriptlet( "Gem::Specification.reset;"
                        + "Gem::Specification.add_dir 'uri:classloader:" + JRUBY_HOME + "/lib/ruby/gems/shared';"
                        + "Gem::Specification.add_dir 'uri:classloader:/';"
                        + "$LOAD_PATH << 'uri:classloader:/'; $LOAD_PATH.inspect" );
        }
        else {
            runScriptlet( "Gem::Specification.reset;"
                        + "Gem::Specification.add_dir '" + getHomeDirectory() + "/lib/ruby/gems/shared'" );
            addLoadPath( getClassLoader(), JRUBY_HOME_DIR );
            addGemPath( getClassLoader(), JRUBY_HOME_DIR );
        }
    }
    
    public void addLoadPath( ClassLoader cl ) {
        addLoadPath( cl, JRUBYDIR );
    }

    public void addLoadPath( ClassLoader cl, String ref ) {
        if ( isContextClassLoader ) {
            throw new RuntimeException( "not add load path on context classloader" );
        }
        URL url = cl.getResource( ref );
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }

        runScriptlet( "$LOAD_PATH << 'uri:" + url.toString().replaceFirst( ref + "$", "" ) + "'" );
    }

    public void addGemPath( ClassLoader cl ) {
        addGemPath( cl, "/specifications" + JRUBYDIR );
    }

    public void addGemPath( ClassLoader cl, String ref ) {
        if ( isContextClassLoader ) {
            throw new RuntimeException( "not add load path on context classloader" );
        }
        URL url = cl.getResource( ref );
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }
        runScriptlet( "Gem::Specification.add_dir 'uri:" + url.toString().replaceFirst( ref + "$", "" ) + "'" );
    }

}