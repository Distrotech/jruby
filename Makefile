VER=1.7.9
TARGET = maven/jruby-dist/target/jruby-dist-$(VER)-bin.tar.gz
PREFIX = /usr
LIBDIR = lib

all: $(TARGET)

$(TARGET):
	mvn
	mvn -U -Pdist

install: all
	install -d $(DESTDIR)$(PREFIX)/$(LIBDIR)/jvm
	tar -C $(DESTDIR)$(PREFIX)/$(LIBDIR)/jvm -xf $(TARGET)

clean:
	mvn clean

distclean: clean
	rm -rf maven/jruby-dist/target/  maven/jruby-stdlib/target/ lib/jni/
	rm -f bin/install_doc.bat bin/ast.bat bin/jgem.bat bin/jrubyc.bat bin/rake.bat \
	      lib/jruby.jar bin/testrb.bat bin/jruby
