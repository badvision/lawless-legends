mkdir -p /tmp/ll/Lawless\ Legends
cd /tmp/ll
cp /Users/brobert/Dropbox/Lawless\ Legends/Lawless\ Legends\ Manual\ v5\ 20210217.txt .
cp /Users/brobert/Documents/code/lawless-legends/Platform/Apple/tools/jace/target/*jar ./Lawless\ Legends
zip -rv /Users/brobert/Dropbox/Lawless\ Legends/Lawless\ Legends-win64.zip .
rm -rf ./Lawless\ Legends

mkdir -p /tmp/ll/Lawless\ Legends.app/Contents/Resources
cp /Users/brobert/Documents/code/lawless-legends/Platform/Apple/tools/jace/target/*jar ./Lawless\ Legends.app/Contents/Resources
zip -rv /Users/brobert/Dropbox/Lawless\ Legends/Lawless\ Legends-osx.zip .
cd ~
rm -rf /tmp/ll
