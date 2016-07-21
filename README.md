Lawless Legends
===============

This project contains all open-source (Apache-licensed) code and assets related to Lawless Legends and its supporting tools.  This project is currently in its infancy, so don't be surprised if some things move around or seem to appear out of nowhere in the code base.  We're still trying on some ideas of how to manage developer workflow, and feedback is greatly appreciated!

The Apache license covers everything in here, including any sample game content that might filter in from time to time.  The primary goal for this open-source project is to build out game creation tools and hopefully the majority of a usable game engine that can be used in conjunction with the tools to build a playable RPG game.  By being open-source, we are saying you are free to build your own games with these tools and distribute those games as you see fit.  You are not obligated to distribute your games as open-source or freeware, but we do ask that you consider contributing bug fixes back up to this main branch so that the community can benefit, and at the very least uphold the terms of the Apache license that apply to this code.

Ancient Legends
--------------
Our first example adventure is freely available under the CC-NC License.

You are Quinn of Thane, the westernmost island in Merkania. Since the last of great wars your family has lived in peaceful seclusion for generations. Raised on ancient legends, you have often dreamed of exploring what lies beyond the island. You surmise that dark times must have befallen the mainland, as dark creatures now sully the sanctuary of your island home as well. 

Now you are the last of your line and have decided to venture to the mainland and seek your fortune! You pack your things, take your family's heirloom amulet for luck and enter the forest for adventure...

Go here for more info: https://github.com/badvision/lawless-legends/tree/master/Example%20Content/Ancient%20Legends

Download Links
--------------

- The most recent copy of Outlaw Editor (aka the Daily build) can be found here: https://www.dropbox.com/s/zrystud66myzlwa/OutlawEditor-jfx.jar?dl=0
- Live builds of the Apple platform code (AKA the Apple // series game port) will soon be available
- Download the 2014 KFest playable demo disk here: https://www.dropbox.com/s/j7pwpgweu65i4md/Lawless%20Legends%20-%20KFest%20playable%20demo%20-%20v0_50.dsk

If you are curious how the Ray Caster works, Martin Haye has made a tutorial with versions in both text and video: https://github.com/badvision/lawless-legends/blob/master/Docs/Tutorials/BigBlue/TaleOfBigBlue.md

How to get involved
-------------------

Grab a daily build and play with it!  If you are a QA tester or a developer you can get a pre-built copy of the development version of the game and boot it in your favorite apple or apple emulator.  If you want to change things around, go for it!  Just remember that you have to check in your code later on (see notes to 6502 coders) before you can use the next daily build!  If you are helping us with testing, you can discard the daily build once you're finished with it -- nobody likes old bugs!

You (yes, you!) can get involved even if you have no programming experience and just want to dabble with retro gaming.  We will need testers who are not afraid to press untold combinations of buttons for the sake of breaking things and telling the programmers how misbehaved our code is.  You are welcome to test our build process for yourself and let us know if there's a way you think we can make it easier -- because ultimately we are striving to collaborate with modern technology while building a game that runs on old pre-internet 8-bit technology: There are bound to be pitfalls and some amount of inconvenience is almost assured!  But despite this, we think we can achieve the following:

- Common game creation tools (Outlaw Editor) which run in all modern OS platforms using Java 8 / JavaFX
- Native game development on-platform, meaning you code the Apple engine on the Apple (via the ACME assembler)
- Native program code is managed within the virtual computer's disk image and synchronized out via provided scripts (see Notes to 6502 Coders below)
- Much game logic is/will be written in PLASMA, a moderately high level language that runs on the Apple II.
- Native game development and game previewing is possible on real hardware (using VSDrive/ADT Pro or CDDB), or emulation (using _any_ apple //e or //gs emulator)
- New tricks to teach our old dogs.  Many surprises await!

Notes to 6502 Coders
--------------------

Time to ROL up your sleeves and accumulate some good documentation on Prodos memory layouts and Prodos device drivers!  We're going to make this game so that you can run (and exit back to) Prodos on any 128kb (or more) machine.  That means the //c and //gs will be natively compatible as well.  We have some cool routines written, but are still in our infancy:
- Fast tile drawing using text page 1 ($400-$7ff) as scratchpad
- Crazy awesome Hi-res 3D raycaster engine
- Module-based memory management which can utilize extra ram whenever possible
- PLASMA interpreter

We need a lot more, and ideas are greatly appreciated for the following:
- 2D tile drawing engine
- Mockingboard playback routines
- Ensoniq DOC playback routines
- Image depack routines, with support for animation frames and transparent overlays.

Daily build process
-------------------
1. Core tools are checked out from the repository (e.g. Outlaw Editor) and built.
2. For each platform, tools are checked out and each platform's main build script is run.
3. All built assets are uploaded to the daily build download locations

The apple platform build works as follows: 
   + Start with a clean base image from platforms/apple/base
   + Run the sync script in platforms/apple/tools to update source files in the image
   + Run the export script in platforms/apple/tools to export common game data from Example Content in to data folders of the image
   ++ The export script is used by creative authors to update things like tiles, images and maps used by the game
   + Run the build script in platforms/apple/tools to run the build process and compile all source code, this renders executables in the daily build image
   ++ Note: Please familiarize yourself with the build script, you might have to add your source files to its list of known sources!

How to build
------------
- Each platform has its own process, refer to each platform's readme for more details.  
- The Outlaw Editor has more information about how to build it locally using Apache Maven and is described in the OutlawEditor readme.




