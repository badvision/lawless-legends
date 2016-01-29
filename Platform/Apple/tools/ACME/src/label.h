//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Label stuff
#ifndef label_H
#define label_H

#include <stdio.h>


// "label" structure type definition
struct label_t {
	value_t	value;	// Expression value
	int	flags;	// Expression flags
	// FIXME - integrate value and flags to result_t struct!
	int	usage;	// usage count
	int	pass;	// set to pass number on creation (for anon counters)
};


// Variables
extern node_ra_t*	Label_forest[];	// trees (because of 8-bit hash)


// Prototypes
extern void	Label_init(void);
extern void	Label_set_value(label_t*, result_t*, bool change);
extern void	Label_implicit_definition(zone_t zone, int stat_flags, int force_bit, bool change);
extern void	Label_parse_definition(zone_t zone, int stat_flags);
extern label_t*	Label_find(zone_t, int);
extern void	Label_dump_all(FILE* fd);
extern label_t*	Label_fix_forward_name(void);


#endif
