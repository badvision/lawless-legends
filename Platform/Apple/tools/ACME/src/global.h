//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Global stuff - things that are needed by several modules
#ifndef global_H
#define global_H

#include <stdio.h>
#include <stdlib.h>
#include "config.h"


// Constants

#define SF_FOUND_BLANK		(1u)	// statement had space or tab
#define SF_IMPLIED_LABEL	(2u)	// statement had implied label def
extern const char	s_cbm[];
// Error messages during assembly
extern const char	exception_cannot_open_input_file[];
extern const char	exception_missing_string[];
extern const char	exception_no_left_brace[];
extern const char	exception_no_memory_left[];
extern const char	exception_no_right_brace[];
//extern const char	exception_not_yet[];
extern const char	exception_syntax[];
extern const char	exception_number_out_of_range[];
// Byte flags table
extern const char	Byte_flags[];
#define BYTEFLAGS(c)	(Byte_flags[(unsigned char) c])
#define STARTS_KEYWORD	(1u << 7)	// Byte is allowed to start a keyword
#define CONTS_KEYWORD	(1u << 6)	// Byte is allowed in a keyword
#define BYTEIS_UPCASE	(1u << 5)	// Byte is upper case and can be
			// converted to lower case by OR-ing this bit(!)
#define BYTEIS_SYNTAX	(1u << 4)	// special character for input syntax
#define FOLLOWS_ANON	(1u << 3)	// preceding '-' are backward label
// bits 2, 1 and 0 are unused


// Variables

extern node_t*		pseudo_opcode_tree;// tree to hold pseudo opcodes
// structures
enum eos_t {
	SKIP_REMAINDER,		// skip remainder of line - (after errors)
	ENSURE_EOS,		// make sure there's nothing left in statement
	PARSE_REMAINDER,	// parse what's left
	AT_EOS_ANYWAY,		// actually, same as PARSE_REMAINDER
};
extern int	pass_flags;	// Pass flags and its bitfields
// Do stuff that only has to be done once (only in first pass)
#define PASS_ISFIRST	(1u << 0)
// Show errors when values are undefined (only in additional pass)
#define PASS_ISERROR	(1u << 1)
extern int	pass_count;
extern int	Process_verbosity;// Level of additional output
extern char	GotByte;// Last byte read (processed)
// Global counters
extern int	pass_undefined_count;// "NeedValue" type errors in current pass
extern int	pass_real_errors;	// Errors yet
extern value_t	pc_inc;	// Increase PC by this amount after statement
extern value_t	segment_start;		// Start of current segment
extern value_t	segment_max;		// Highest address segment may use
extern signed long	max_errors;		// errors before giving up

extern void*	autofree_list_pass;	// linked list, holds malloc blocks

// Macros for skipping a single space character
#define SKIPSPACE()		do {if(GotByte   == ' ') GetByte();} while(0)
#define NEXTANDSKIPSPACE()	do {if(GetByte() == ' ') GetByte();} while(0)


// Prototypes
extern inline void*	safe_malloc(size_t);
extern void*	autofree_malloc(size_t, void**);
// Macro for claiming auto-freed memory blocks
#define ALLOC_PASS(Size)	autofree_malloc(Size, &autofree_list_pass)
extern void	autofree_free(void**);
extern void	Parse_until_eob_or_eof(void);
extern int	Parse_optional_block(void);
extern void	Throw_warning(const char*);
extern void	Throw_error(const char*);
extern void	Throw_serious_error(const char*);
extern void	Bug_found(const char*, int);


#endif
