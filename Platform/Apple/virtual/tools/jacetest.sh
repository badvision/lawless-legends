#!/usr/bin/env bash

mkdir -p jacecaps
while [[ 1 ]]; do
    DATETIME=`date '+%Y%m%d-%H%M%S'`
    echo $DATETIME
    #"/Users/mhaye/AppleII/Projects/LawlessLegends/Lawless Legends-osx/Lawless Legends.app/Contents/MacOS/LawlessLegends" -computer.showBootAnimation false &
    open "/Users/mhaye/AppleII/Projects/LawlessLegends/Lawless Legends-osx/Lawless Legends.app"
    sleep 25
    OUTFILE="jacecaps/$DATETIME.png"
    screencapture -R440,180,559,406 -t png -x $OUTFILE
    file $OUTFILE
    pkill LawlessLegends
    sleep 1
done
