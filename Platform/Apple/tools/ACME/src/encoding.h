//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Character encoding stuff
#ifndef encoding_H
#define encoding_H


// Variables
extern char	(*Encoding_encode_char)(char);	// conversion function pointer


// Prototypes
extern void	Encoding_init(void);
extern void	Encoding_passinit(void);


#endif
