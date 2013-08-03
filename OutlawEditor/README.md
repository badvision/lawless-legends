Outlaw Editor
=============

This is a multi-platform RPG game editor.  It is written in (and requires) Java 7 and JavaFX 2.2 to run.  There are no external dependencies, but it is important that you install JavaFX 2.2 as per Oracle's instructions so that the java runtime can find the JavaFX libraries automatically.

Building the program
--------------------

First time building: Open the pom.xml file and double-check that you have the javafx jars in the same location indicated for the javafx dependency.  Adjust the jar file location as needed (if needed at all -- A compile error is a sure bet you have to do something about it.

If you have an IDE that understands Apache Maven projects, then open this project in your IDE and build from there.  I cannot stress how much easier this is than doing things manually.  But if you insist on doing it the hard way, then "mvn install" should be sufficient to build the program if you have already installed Maven 2 and it is in your bin path and properly configured.

Before you build the first time, you will see all kinds of compile errors all over the source tree.  That's because part of the program gets generated during the build process.  Try doing a clean build and see if that resolves the issues before jumping into panic.

Running the program
-------------------

There are no dependencies outside of JavaFX (which will be part of Java in the 8.0 release) so you can start the program by executing the jar file that is built in the target folder "OutlawEditor-1.0-SNAPSHOT.jar"   Depending on your platform you might be able to double-click it.  Or you might have to drop to the commandline and issue this command after going to that directory, like so:

> java -jar OutlawEditor-1.0-SNAPSHOT.jar

For more information about using Outlaw Editor, refer to the wiki.  Don't forget to check the Example Content folder for some starter content that you can use to get a better feel of what you can do!

Getting daily builds
--------------------

I am currently working to set up a build server and create a home for daily builds.  More news will be posted as it becomes available.



