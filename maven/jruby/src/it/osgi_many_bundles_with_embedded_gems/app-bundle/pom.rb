#-*- mode: ruby -*-

id 'org.jruby.osgi:app-bundle', '1.0'

packaging 'bundle'

properties( 'tesla.dump.pom' => 'pom.xml',
            'jruby.version' => '@project.version@' )

pom 'org.jruby:jruby', '${jruby.version}'

plugin( 'org.apache.felix:maven-bundle-plugin', '2.4.0',
        #:excludeDependencies => 'bundle',
        :instructions => {
          # org.junit is needed for the test phase to run unit tests
          'Export-Package' => 'org.jruby.*,org.junit.*',
          # this is needed to find javax.* packages
          'DynamicImport-Package' => '*',
          'Include-Resource' => '{maven-resources}',
          'Import-Package' => '!org.jruby.*,*;resolution:=optional, org.jruby.osgi.gems, org.jruby.osgi.scripts',
          'Embed-Dependency' => '*;type=jar;scope=compile|runtime;inline=true',
          'Embed-Transitive' => true
        } ) do
  # TODO fix DSL
  @current.extensions = true
end

bundle 'org.jruby.osgi:gems-bundle', '1.0'
bundle 'org.jruby.osgi:scripts-bundle', '1.0'
