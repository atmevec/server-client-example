all: client server

client:
	javac Client.java
	jar cvfm Client.jar clManifest.txt *.class
	rm *.class

server:
	javac Server.java
	jar cvfm Server.jar srManifest.txt *.class
	rm *.class