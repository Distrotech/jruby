#-*- mode: ruby -*-

gemfile

id 'org.jruby.osgi:gems-bundle', '1.0'

packaging 'bundle'

jar 'org.osgi:org.osgi.core', '5.0.0', :scope => :provided

jruby_plugin! :gem, :includeRubygemsInResources => true

require 'fileutils'
execute 'jrubydir', 'process-resources' do |ctx|
  def process( dir, root = false )
    File.open( dir + '/.jrubydir', 'w' ) do |f|
      f.puts ".." unless root
      f.puts "."
      Dir[ dir + '/*'].entries.each do |e|
        f.print File.basename( e )
        if File.directory?( e )
          process( e )
        end
        f.puts
      end
    end
  end
  process( ctx.project.build.directory.to_pathname + '/rubygems', true )
end

plugin( 'org.apache.felix:maven-bundle-plugin', '2.4.0',
        :instructions => {
          'Export-Package' => 'org.jruby.osgi.gems',
          'Include-Resource' => '{maven-resources}'
        } ) do
  # TODO fix DSL
  @current.extensions = true
end
