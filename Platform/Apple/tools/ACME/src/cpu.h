//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// CPU stuff
#ifndef cpu_H
#define cpu_H

#include "config.h"


// CPU type structure definition
struct cpu_t {
	// This routine is not allowed to change GlobalDynaBuf
	// because that's where the mnemonic is stored!
	bool	(*keyword_is_mnemonic)(int);
	int	flags;
};
#define	CPU_FLAG_LONG_REGS	(1u << 0)
#define	CPU_FLAG_IMM16		(1u << 1)
#define	CPU_FLAG_INDJMP_BUGGY	(1u << 2)
typedef struct cpu_t cpu_t;


// Variables
extern cpu_t	*CPU_now;	// Struct of current CPU type (default 6502)
extern bool	CPU65816_long_a;	// Flags for long accumulator and long
extern bool	CPU65816_long_r;	//  index registers (both default off)
	// FIXME - move the 65816 stuff out of general CPU stuff?
extern value_t	CPU_pc;	// Current program counter (pseudo value)
#define PC_UNDEFINED	-1	// value of PC when undefined
#define PC_DUMMY	512	// dummy value to use when looking for errors


// Prototypes
extern void	CPUtype_init(void);
extern void	CPU_init(void);
extern void	CPU_passinit(cpu_t* cpu_type, signed long starting_pc);
extern void	CPU_end_segment(void);
extern void	CPU_find_segment_max(value_t);
extern void	CPU_set_pc(void);
extern inline void	CPU_ensure_defined_pc(void);
extern bool	CPU_find_cpu_struct(cpu_t** target);


#endif
