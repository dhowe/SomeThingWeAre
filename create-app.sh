#!/bin/sh

PROJ_HOME=~/git/SomeThingWeAre

cd $PROJ_HOME

echo copying lib jars
cp -r lib/* SomeThingWeAre.app/Contents/Java

echo creating app jar
cd bin
jar cvf ../SomeThingWeAre.jar *
cd ..

echo copying app jar
mv SomeThingWeAre.jar SomeThingWeAre.app/Contents/Java
#cp SomeThingWeAre.jar SomeThingWeAre.app/Contents/Java

ls -l SomeThingWeAre.app/Contents/Java

echo done


