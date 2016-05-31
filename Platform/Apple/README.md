Building a game disk image for the Apple II Platform
====================================================

1. Install dependencies

  The platform build for the Apple II requires only two dependencies. You will need to install these and have them in your path before you try to build.

  - Java 8 (or higher). You can use either OpenJDK 1.8+ or Sun JDK 1.8+
  - Apache ant 1.9 (or higher)

  You can check if you already have them this way:
  - `java -version`  # should show "1.8.xxx"
  - `ant -version`   # should show "1.9.x"

2. Build the tools

  - `cd Platform/Apple`
  - `ant`

3. Put scenario files in place

  You will need acquire and place three scenario files into the appropriate subdirectories of `Platform/Apple/virtual/` as follows:
  - `Platform/Apple/virtual/data/world/world.xml`
  - `Platform/Apple/virtual/data/world/enemies.tsv`
  - `Platform/Apple/virtual/data/fonts/font.bin`

4. Build a game disk

  - `cd Platform/Apple/virtual`
  - `ant`
  - The resulting disk image will be `Platform/Apple/Virtual/game.2mg`. Just boot it in an emulator or copy to a real Apple II, and have fun.

5. Rinse and repeat

  - Change any of the code files in virtual/src, or update the world.xml file using Outlaw
  - Go to step 4. The system uses incremental building for speedy turnaround.