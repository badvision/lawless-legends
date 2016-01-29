//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Input stuff
#ifndef input_H
#define input_H

#include <stdio.h>


// type definitions

// values for input_t component "Src.State"
enum inputstate_t {
	INPUTSTATE_NORMAL,	// everything's fine
	INPUTSTATE_AGAIN,	// re-process last byte
	INPUTSTATE_SKIPBLANKS,	// shrink multiple spaces
	INPUTSTATE_LF,		// send start-of-line after end-of-statement
	INPUTSTATE_CR,		// same, but also remember to skip LF
	INPUTSTATE_SKIPLF,	// skip LF if that's next
	INPUTSTATE_COMMENT,	// skip characters until newline or EOF
	INPUTSTATE_EOB,		// send end-of-block after end-of-statement
	INPUTSTATE_EOF,		// send end-of-file after end-of-statement
};
typedef struct {
	const char*	original_filename;// during RAM reads, too
	int		line_number;	// in file (on RAM reads, too)
	bool		source_is_ram;	// TRUE if RAM, FALSE if file
	enum inputstate_t	state;	// state of input
	union {
		FILE*	fd;	// file descriptor
		char*	ram_ptr;	// RAM read ptr (loop or macro block)
	} src;
} input_t;


// Constants
extern const char	FILE_READBINARY[];
// Special characters
// The program *heavily* relies on CHAR_EOS (end of statement) being 0x00!
#define CHAR_EOS	(0)	// end of statement	(in high-level format)
#define CHAR_SOB	'{'	// start of block
#define CHAR_EOB	'}'	// end of block
#define CHAR_SOL	(10)	// start of line	(in high-level format)
#define CHAR_EOF	(13)	// end of file		(in high-level format)
// If the characters above are changed, don't forget to adjust Byte_flags[]!


// Variables
extern input_t*	Input_now;	// current input structure


// Prototypes
extern void	Input_init(void);
extern void	Input_new_file(const char* filename, FILE* fd);
extern char	GetByte(void);
extern char	GetQuotedByte(void);
extern void	Input_skip_remainder(void);
extern void	Input_ensure_EOS(void);
extern char*	Input_skip_or_store_block(bool store);
extern void	Input_until_terminator(char terminator);
extern int	Input_append_keyword_to_global_dynabuf(void);// returns length
extern int	Input_read_zone_and_keyword(zone_t*);
extern int	Input_read_keyword(void);// returns length
extern int	Input_read_and_lower_keyword(void);// returns length
extern char*	Input_read_filename(bool library_allowed);
extern bool	Input_accept_comma(void);// skips spaces before and after


#endif
