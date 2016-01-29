//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Platform specific stuff (in this case, for DOS, OS/2 and Windows)
#ifndef platform_H
#define platform_H

#include "config.h"


// Symbolic constants and macros

// Called once on program startup (could register exit handler, if needed)
#define PLATFORM_INIT		DOS_entry()

// Convert UNIX-style pathname to DOS-style pathname
#define PLATFORM_CONVERTPATHCHAR(a)	DOS_convert_path_char(a)

// String containing the prefix for accessing files from the library tree
#define PLATFORM_LIBPREFIX	DOS_lib_prefix

// Setting file types of created files
#define PLATFORM_SETFILETYPE_CBM(a)
#define PLATFORM_SETFILETYPE_PLAIN(a)
#define PLATFORM_SETFILETYPE_TEXT(a)

// Platform specific message output
#define PLATFORM_WARNING(a)
#define PLATFORM_ERROR(a)
#define PLATFORM_SERIOUS(a)

// Integer-to-character conversion routine
#define PLATFORM_INT2BYTE(x)	do {\
	x ^= x >> 16;\
	x ^= x >>  8;\
	x &= 255;\
} while(0)

// Output of platform-specific command line switches
#define PLATFORM_OPTION_HELP

// Processing of platform-specific command line switches
#define PLATFORM_SHORTOPTION_CODE
#define PLATFORM_LONGOPTION_CODE


// Variables
extern char	*DOS_lib_prefix;	// header string of library tree


// Prototypes
extern void	DOS_entry(void);
extern char	DOS_convert_path_char(char);


#endif
