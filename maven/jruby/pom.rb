project 'JRuby Main Maven Artifact' do

  model_version '4.0.0'
  id 'org.jruby:jruby:1.7.14.dev-SNAPSHOT'
  inherit 'org.jruby:jruby-artifacts:1.7.14.dev-SNAPSHOT'
  packaging 'jar'

  properties( 'jruby.basedir' => '${basedir}/../../',
              'main.basedir' => '${project.parent.parent.basedir}' )

  jar 'org.jruby:jruby-core:${project.version}'
  jar 'org.jruby:jruby-stdlib:${project.version}'

  plugin( :source,
          'skipSource' =>  'true' )
  plugin 'org.codehaus.mojo:build-helper-maven-plugin' do
    execute_goals( 'attach-artifact',
                   :id => 'attach-artifacts',
                   :phase => 'package',
                   'artifacts' => [ { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'sources' },
                                    { 'file' =>  '${basedir}/src/empty.jar',
                                      'classifier' =>  'javadoc' } ] )
  end

  execute 'setup other osgi frameworks', :phase => 'pre-integration-test' do |ctx|
    require 'fileutils'
    felix = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_all_inclusive_felix_4.4' )
    [ 'equinox-3.6', 'equinox-3.7', 'felix-3.2' ].each do |m|
      target = File.join( ctx.basedir.to_pathname, 'src', 'it', 'osgi_all_inclusive_' + m )
      FileUtils.cp_r( felix, target )
      File.open( File.join( target, 'invoker.properties' ), 'w' ) do |f|
        f.puts 'invoker.profiles = ' + m
      end
    end
  end

  plugin( :invoker,
          'projectsDirectory' =>  'src/it',
          'cloneProjectsTo' =>  '${project.build.directory}/it',
          'preBuildHookScript' =>  'setup.bsh',
          'postBuildHookScript' =>  'verify.bsh',
          'goals' => [ 'install' ] ) do
    execute_goals( 'install', 'run',
                   :id => 'integration-test',
                   'settingsFile' =>  '${basedir}/src/it/settings.xml',
                   'localRepositoryPath' =>  '${project.build.directory}/local-repo' )
  end

end
