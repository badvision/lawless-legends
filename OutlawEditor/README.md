Outlaw Editor
=============

This is a multi-platform RPG game editor.  It is written in (and requires) Java 8 to run.  There are no external dependencies.

Download
--------
The most recent copy of Outlaw Editor (aka the Daily build) can be found here: https://www.dropbox.com/s/zrystud66myzlwa/OutlawEditor-jfx.jar

Running the program
-------------------

You can start the program by executing the jar file that is built in the target/jfx/app folder "OutlawEditor-jfx.jar"   Depending on your platform you might be able to double-click it.  Or you might have to drop to the commandline and issue this command after going to that directory, like so:

> java -jar OutlawEditor-jfx.jar

For more information about using Outlaw Editor, refer to the wiki.  Don't forget to check the Example Content folder for some starter content that you can use to get a better feel of what you can do!

Building the program
--------------------

First time building: Open the pom.xml file and double-check that you have the javafx jars in the same location indicated for the javafx dependency.  Adjust the jar file location as needed (if needed at all -- A compile error is a sure bet you have to do something about it.

If you have an IDE that understands Apache Maven projects, then open this project in your IDE and build from there.  I cannot stress how much easier this is than doing things manually.  But if you insist on doing it the hard way, then "mvn install" should be sufficient to build the program if you have already installed Maven 3 and it is in your bin path and properly configured.

With Netbeans, you should be able to open this project directly.  For Eclipse you should be fine as long as you have Indigo (3.7) or later and the m2eclipse plugin, with which you import this project into your workspace and go from there.

Before you build the first time, you will see all kinds of compile errors all over the source tree.  That's because part of the program gets generated during the build process.  Try doing a clean build and see if that resolves the issues before jumping into panic.
