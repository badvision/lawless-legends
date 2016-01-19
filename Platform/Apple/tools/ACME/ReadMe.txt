

                                 ACME

         ...the ACME Crossassembler for Multiple Environments

           --- Linux/Unix/platform independent  version ---


This file only describes stuff that is specific to the Linux / Unix
/ platform independent version of ACME. So if you are looking for more
general things, you should take a look at "docs/Help.txt".




***   Compiling and installing the executable:

Change into the directory "acme091/src" that was created when
unpacking the archive and simply type "make". This will compile the
sources and produce an executable file.

If you have root access:
Change into superuser mode using "su" and type "make install" to move
the executable to the appropriate directory (system-wide install).

If you don't have root access:
Type "make userinstall" to move the executable to your "~/bin"
directory (user-specific install).

Feel free to adjust the Makefile to your specific needs.




***   Installing the library:

The directory "ACME_Lib" contains a bunch of files that may be useful.
Okay, there's hardly anything in it at the moment, but it will
hopefully grow over time.
Copy the ACME_Lib directory to a place you think is appropriate. You
will have to set up an environment variable called "ACME" to allow the
main program to find this directory tree.

In bash:

    set ACME="/my/path/to/ACME_Lib" ; export ACME

You don't *have* to install the library just to use ACME, but it
doesn't hurt to install it - one of the supplied example programs uses
the library as well.




***   Miscellaneous:

ACME was tested and prepared on Redhat 5.2 but it should work on any
Linux distribution and on other Unices aswell (if not, then let me
know and i'll fix it ASAP).

Krzysztof Dabrowski, current Linux port maintainer
mailto:brush@pol.pl




***   Changes:

Release 0.91: Merged Linux/Unix version and source-only version.

Release 0.04 beta: First Linux version

