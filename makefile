JC = javac
J = java

default: Player.class VideoStream.class

Player.class: Player.java
	$(JC) $(JFLAGS) Player.java 
VideoStream.class: VideoStream.java
	$(JC) $(JFLAGS) VideoStream.java  
clean:
	rm -f *.class
