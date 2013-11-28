TARGET = maven/jruby-dist/target/jruby-dist-1.7.8-bin.tar.gz
PREFIX = /usr
LIBDIR = lib

all: $(TARGET)

$(TARGET):
	mvn
	mvn -Pbootstrap
	mvn -Pdist

install: all
	install -d $(DESTDIR)$(PREFIX)/$(LIBDIR)/jvm
	tar -C $(DESTDIR)$(PREFIX)/$(LIBDIR)/jvm -xf $(TARGET)

clean:
	mvn clean
