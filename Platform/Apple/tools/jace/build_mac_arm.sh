#!/bin/sh
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-gluon-22.1.0.1/Contents/Home
cd ~/Documents/code/lawless-legends/Platform/Apple/tools/jace
mvn gluonfx:build
fileicon set target/gluonfx/aarch64-darwin/lawlesslegends src/main/resources/jace/data/game_icon.png
cp target/gluonfx/aarch64-darwin/lawlesslegends ~/Desktop
