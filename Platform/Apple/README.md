Building for the Apple II Platform
==================================

1. Build the A2Copy tool, which copies directories in/out of image files: `cd tools/A2Copy`
   and then `ant`, and finally `cd ../..`

2. Build the PackMap tool, which converts Outlaw XML to a packed map. `cd tools/PackMap`
   and then `ant`, and finally `cd ../..`. I think you need Groovy support in your
   NetBeans installation for this to work.

3. Pack your game data. `java -jar tools/PackMap/dist/PackMap.jar yourXMLFile.xml virtual/data/maps/map.pack/bin`

4. Copy the frame image (in Apple II format) to virtual/data/images/frame.bin

5. Set the location of the "cc65" tool set. Make a copy of `virtual/src/include/sample.build.props`
   and call it `build.props` (in that same directory). Edit the `CC65_BIN_DIR` path inside that file 
   to point at your cc65 installation. There should be "ca65", "cc65", "ld65" etc. in the directory
   you point to.

6. Now build a complete disk image: `cd virtual` and then `ant`

7. Boot up the resulting disk image `game.2mg` on your Apple II or emulator.

8. To run the render demo, type `-RENDER` at the ProDOS Basic prompt.
