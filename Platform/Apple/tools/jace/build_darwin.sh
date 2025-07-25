#!/bin/sh
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-17-gluon-22.1.0.1/Contents/Home
cd ~/Documents/code/lawless-legends/Platform/Apple/tools/jace
mvn3.8 gluonfx:build
fileicon set target/gluonfx/x86_64-darwin/lawlesslegends src/main/resources/jace/data/game_icon.png
cp target/gluonfx/x86_64-darwin/lawlesslegends ~/Desktop
