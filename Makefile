# Assignment 1 Networking Makefile
# 24 February 2018

JAVAC = /usr/bin/javac
.SUFFIXES: .java .class

SRCDIR=src
BINDIR=bin
DOCDIR=doc

$(BINDIR)/%.class:$(SRCDIR)/%.java
	$(JAVAC) -d $(BINDIR)/ -cp $(BINDIR) $<

CLASSES=ChatProtocol.class ClientHandlerThread.class ChatServer.class ChatClient.class
CLASS_FILES=$(CLASSES:%.class=$(BINDIR)/%.class)

default: $(CLASS_FILES)

runServer:
	java -cp $(BINDIR) ChatServer 60000

#runClient:
#	java -cp $(BINDIR) Client

docs:
	javadoc -d $(DOCDIR) $(SRCDIR)/*.java

clean:
	rm $(BINDIR)/*.class
