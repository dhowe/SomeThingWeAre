#!/bin/sh

JAVAC=/usr/bin/javac

CLASS_PATH=".:lib/core.jar:lib/jsoup-1.7.3.jar:lib/rita-1.0.67.jar:lib/imgscalr-lib-4.2.jar:lib/gson-2.3.jar:lib/netty-buffer-4.0.25.Final.jar:lib/netty-codec-4.0.25.Final.jar:lib/netty-codec-http-4.0.25.Final.jar:lib/netty-common-4.0.25.Final.jar:lib/netty-handler-4.0.25.Final.jar:lib/netty-socketio-1.7.6.jar:lib/netty-transport-4.0.25.Final.jar:lib/netty-transport-native-epoll-4.0.25.Final.jar:lib/slf4j-api-1.7.7.jar:lib/jl1.0.jar:lib/jsminim.jar:lib/minim.jar:lib/mp3spi1.9.4.jar:lib/tritonus_aos.jar:lib/tritonus_share.jar:lib/jackson-annotations-2.4.0.jar:lib/jackson-core-2.4.3.jar:lib/jackson-databind-2.4.3.jar:lib/slf4j-nop-1.7.12.jar"

$JAVAC -d bin -cp $CLASS_PATH -sourcepath src src/stwa/SomeThingWeAre.java
