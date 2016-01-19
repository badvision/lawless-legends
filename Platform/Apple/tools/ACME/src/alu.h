//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// ALU stuff (the expression parser)
#ifndef alu_H
#define alu_H

#include "config.h"


// result structure type definition
struct result_t {
	value_t	value;	// Expression value
	int	flags;	// Expression flags
};


// Constants

// Meaning of bits in "Flags" of result_t structure
// Needless parentheses indicate use of indirect addressing
#define MVALUE_INDIRECT	(1u << 7)
// There was *something* to parse
#define MVALUE_EXISTS	(1u << 6)
// Value once was related to undefined expression
#define MVALUE_UNSURE	(1u << 5)
// Value is defined (if this is cleared, the value will be zero)
#define MVALUE_DEFINED	(1u << 4)
// Value is guaranteed to fit in one byte
#define MVALUE_ISBYTE	(1u << 3)
// Value usage forces 24-bit usage
#define MVALUE_FORCE24	(1u << 2)
// Value usage forces 16-bit usage
#define MVALUE_FORCE16	(1u << 1)
// Value usage forces 8-bit usage
#define MVALUE_FORCE08	(1u << 0)
// Bit mask for force bits
#define MVALUE_FORCEBITS	(MVALUE_FORCE08| MVALUE_FORCE16| MVALUE_FORCE24)
// Bit mask for fixed values (defined and existing)
#define MVALUE_GIVEN	(MVALUE_DEFINED | MVALUE_EXISTS)


// Prototypes
extern void	ALU_init(void);
extern void	ALU_parse_expr_strict(result_t*);
extern void	ALU_parse_expr_empty_strict(result_t*);
extern void	ALU_parse_expr_medium(result_t*);
extern int	ALU_parse_expr_liberal(result_t*);


#endif
