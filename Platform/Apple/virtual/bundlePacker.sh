#!/bin/bash

set -euxo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: bundlePacker.sh <versionNum>"
    exit 1
fi

./b
rm -f PackPartitions.exe
java -Djava.awt.headless=true -jar /Users/mhaye/plat/virtual/launch4j/launch4j.jar launch4j.cfg.xml 

rm -rf bundle-tmp
mkdir bundle-tmp
cd bundle-tmp
unzip ../packer-bundle.zip
mv packer-bundle-* packer-bundle
mv packer-bundle packer-bundle-$1
rm packer-bundle-$1/PackPartitions.exe
rm -f packer-bundle-$1/world.xml
mv ../PackPartitions.exe packer-bundle-$1/PackPartitions.exe
rm -rf packer-bundle-$1/jdk
cp -r ../win-jdk packer-bundle-$1/jdk
zip -r packer-bundle-$1.zip *
cp packer-bundle-$1.zip ../packer-bundle.zip
mv ~/Dropbox/Lawless\ Legends/packer-bundle-V*.zip ~/Dropbox/Lawless\ Legends/packer-bundle.zip
sleep 5  # allow time for Dropbox to pick up the rename
cp packer-bundle-$1.zip ~/Dropbox/Lawless\ Legends/packer-bundle.zip
sleep 5  # allow time for Dropbox up detect the diff
mv ~/Dropbox/Lawless\ Legends/packer-bundle.zip ~/Dropbox/Lawless\ Legends/packer-bundle-$1.zip
cd ..
rm -rf bundle-tmp
