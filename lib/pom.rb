# the versions are declared in ../pom.xml
default_gems = { 
  'jruby-openssl' => 'jopenssl.version',
  'rake' => 'rake.version',
  'rdoc' => 'rdoc.version',
  'json' => 'json.version',
  'krypt' => 'krypt.version',
  'krypt-core' => 'krypt.version',
  'krypt-provider-jdk' => 'krypt.version',
  'bouncy-castle-java' => 'bc.version'
}

only_specs = [ 'rdoc', 'json', 'jruby-openssl' ]

project 'JRuby Lib Setup' do

  version = '9000.dev' #File.read( File.join( basedir, '..', 'VERSION' ) )

  model_version '4.0.0'
  id "org.jruby:jruby-lib:#{version}"
  inherit "org.jruby:jruby-parent:#{version}"
  packaging 'pom'

  properties( 'tesla.dump.pom' => 'pom.xml',
              'tesla.dump.readonly' => true,
              'tesla.version' => '0.0.9',
              'jruby.home' => '${basedir}/..' )

  # just depends on jruby-core so we are sure the jruby.jar is in place
  jar "org.jruby:jruby-core:#{version}"

  repository( 'http://rubygems-proxy.torquebox.org/releases',
              :id => 'rubygems-releases' )

  # tell maven to download the respective gem artifacts
  default_gems.each do |n,k|
    gem n, "${#{k}}"
  end

  # this is not an artifact for maven central
  plugin :deploy, :skip => true 

  phase :package do
    plugin :dependency do
      items = default_gems.collect do |n,k|
        { 'groupId' =>  'rubygems',
          'artifactId' =>  n,
          'version' =>  "${#{k}}",
          'type' =>  'gem',
          'overWrite' =>  'false',
          'outputDirectory' =>  '${project.build.directory}' }
      end
      execute_goals( 'copy',
                     :id => 'copy gems',
                     'artifactItems' => items )
    end
  end

  execute :install_gems, :package do |ctx|
    require 'fileutils'

    puts "using jruby #{JRUBY_VERSION}"

    target = ctx.project.build.directory.to_s
    gem_home = File.join( target, 'rubygems' )
    gems = File.join( gem_home, 'gems' )
    specs = File.join( gem_home, 'specifications' )
    default_specs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                               'specifications', 'default' )
    bin_stubs = File.join( ctx.project.basedir.to_s, 'ruby', 'gems', 'shared', 
                           'gems' )
    shared = File.join( ctx.project.basedir.to_s, 'ruby', 'shared' )
    openssl_dir = File.join( target, 'lib' )
    openssl = File.join( openssl_dir, 'openssl.rb' )

    FileUtils.mkdir_p( default_specs )
    FileUtils.mkdir_p( openssl_dir )
    File.open( openssl, 'w' )

    # in case we run some jruby-complete jar for that script
    $LOAD_PATH.unshift openssl_dir

    # now we can require the rubygems staff
    require 'rubygems/installer'
    
    default_gems.each do |name, key|
      version = ctx.project.properties.get( key )
      
      if Dir[ File.join( specs, "#{name}-#{version}*.gemspec" ) ].empty?
        installer = Gem::Installer.new( File.join( ctx.project.build.directory.to_s, 
                                                   "#{name}-#{version}.gem" ),
                                        :ignore_dependencies => true,
                                        :install_dir => gem_home )
        installer.install 
        
        unless only_specs.include? name
          puts "setup gem #{name}-#{version}"
          Dir[ File.join( gems, "#{name}-#{version}*", 'lib', '*' ) ].each do |f|
            FileUtils.cp_r( f, shared )
          end
        end
        
        bin = File.join( gems, "#{name}-#{version}", 'bin' )
        if File.exists? bin
          Dir[ File.join( bin, '*' ) ].each do |f|
            puts "copy bin file #{File.basename( f )}"
            target = File.join( bin_stubs, f.sub( /#{gems}/, '' ) )
            FileUtils.mkdir_p( File.dirname( target ) )
            FileUtils.cp_r( f, target )
          end
        end
        
        spec = Dir[ File.join( specs, "#{name}-#{version}*.gemspec" ) ].first
        puts "copy specification #{File.basename( spec )}"
        FileUtils.cp( spec, default_specs )
      end
    end
    
    # crude HACK to maybe fix the bouncy-castle loading problems
    File.open( File.join( shared, 'bouncy-castle-java.rb' ), 'w' ) do |f|
      f.puts "require File.expand_path('bcpkix-jdk15on-147.jar', File.dirname(__FILE__))"
      f.puts "require File.expand_path('bcprov-jdk15on-147.jar', File.dirname(__FILE__))"
    end

  end
end
