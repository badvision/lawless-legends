Building for the Apple II Platform
==================================

1. Build the A2Copy tool, which copies directories in/out of image files: `cd tools/A2Copy`
   and then `ant`, and finally `cd ../..`

2. Set the location of the "cc65" tool set. Make a copy of `virtual/src/include/sample.build.props`
   and call it `build.props` (in that same directory). Edit the `CC65_BIN_DIR` path inside that file 
   to point at your cc65 installation. There should be "ca65", "cc65", "ld65" etc. in the directory
   you point to.

3. Add game data. Grab sample game data (images, maps, etc.) and put them into the `data` directory.
   Image .bin files go in `data/images`, etc.

4. Now build a complete disk image: `cd virtual` and then `ant`

5. Boot up the resulting disk image `game.2mg` on your Apple II or emulator.

6. To run the render demo, type `-RENDER` at the ProDOS Basic prompt.
