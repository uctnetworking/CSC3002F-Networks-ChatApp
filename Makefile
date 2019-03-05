# Assignment 1 Networking Makefile
# 24 February 2018

JAVAC = /usr/bin/javac
.SUFFIXES: .java .class

SRCDIR=src
BINDIR=bin
DOCDIR=doc

$(BINDIR)/%.class:$(SRCDIR)/%.java
	$(JAVAC) -d $(BINDIR)/ -cp $(BINDIR):$(SRCDIR): $<

CLASSES=ProtocolResponses.class ProtocolRequests.class ChatProtocol.class ClientHandlerThread.class ChatServer.class ChatGUI.class
CLASS_FILES=$(CLASSES:%.class=$(BINDIR)/%.class)

SRC_FILES=$(SRC:%.java=$(SRCDIR)/%.java)

default: $(CLASS_FILES)

runServer:
	java -cp $(BINDIR) ChatServer 60000

runClient:
	java -cp $(BINDIR) ChatGUI

docs:
	javadoc -d $(DOCDIR) $(SRCDIR)/*.java

clean:
	rm $(BINDIR)/*.class
