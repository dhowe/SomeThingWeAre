#!/bin/sh

PROJ_HOME=~/Documents/Workspaces/eclipse-workspace/SomeThingWeAre

cd $PROJ_HOME

echo copying lib jars
cp -r lib/* SomeThingWeAre.app/Contents/Java

echo creating app jar
cd bin
jar cf ../SomeThingWeAre.jar *
cd ..

echo copying app jar
cp SomeThingWeAre.jar SomeThingWeAre.app/Contents/Java

ls -l SomeThingWeAre.app/Contents/Java

echo done


