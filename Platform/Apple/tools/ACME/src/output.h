//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Output stuff
#ifndef output_H
#define output_H

#include <stdio.h>
#include "config.h"


// Constants
#define OUTBUFFERSIZE		65536
#define MEMINIT_USE_DEFAULT	256
enum out_format_t {
	OUTPUT_FORMAT_UNSPECIFIED,	// default value. (results in
	OUTPUT_FORMAT_PLAIN,		//	"plain" being used)
	OUTPUT_FORMAT_CBM,	// default for "!to" pseudo opcode
};


// Variables
extern value_t	Mem_current_pc;	// Current memory pointer
extern value_t	Mem_lowest_pc;	// Start address of used memory
extern value_t	Mem_highest_pc;	// End address of used memory plus one


// Prototypes
extern void	Outputfile_init(void);
extern void	Output_init(signed long fill_value);
extern void	Output_8b(value_t);
extern void	Output_16b(value_t);
extern void	Output_24b(value_t);
extern void	Output_32b(value_t);
extern void	Output_byte(value_t);
extern bool	Output_set_output_format(void);
extern void	Output_save_file(FILE* fd, value_t, value_t);


#endif
