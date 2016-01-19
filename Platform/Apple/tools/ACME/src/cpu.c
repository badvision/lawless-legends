//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// CPU stuff

#include "alu.h"
#include "config.h"
#include "cpu.h"
#include "dynabuf.h"
#include "global.h"
#include "input.h"
#include "mnemo.h"
#include "output.h"
#include "tree.h"


// Structure for linked list of segment data
typedef struct segment_t segment_t;
struct segment_t {
	segment_t*	next;
	value_t		start;
	value_t		length;
};


// Constants
static cpu_t	CPU_6502	= {keyword_is_6502mnemo, CPU_FLAG_INDJMP_BUGGY};
static cpu_t	CPU_6510	= {keyword_is_6510mnemo, CPU_FLAG_INDJMP_BUGGY};
static cpu_t	CPU_65c02	= {keyword_is_65c02mnemo, 0};
//static cpu_t	CPU_Rockwell65c02	= {keyword_is_Rockwell65c02mnemo, 0};
//static cpu_t	CPU_WDC65c02	= {keyword_is_WDC65c02mnemo, 0};
static cpu_t	CPU_65816	= {
	keyword_is_65816mnemo,
	CPU_FLAG_LONG_REGS | CPU_FLAG_IMM16
};


// Variables
cpu_t		*CPU_now;	// Struct of current CPU type (default 6502)
bool		CPU65816_long_a;// Flag for long accumulator (default off)
bool		CPU65816_long_r;// Flag for long index registers (default off)
value_t		CPU_pc;	// (Pseudo) program counter at start of statement
static segment_t*	segment_list;	// linked list of segment structures
// predefined stuff
static node_t*	CPU_tree	= NULL;// tree to hold CPU types
static node_t	CPUs[]	= {
//	PREDEFNODE("z80",		&CPU_Z80),
	PREDEFNODE("6502",		&CPU_6502),
	PREDEFNODE("6510",		&CPU_6510),
	PREDEFNODE("65c02",		&CPU_65c02),
//	PREDEFNODE("Rockwell65c02",	&CPU_Rockwell65c02),
//	PREDEFNODE("WDC65c02",		&CPU_WDC65c02),
	PREDEFLAST("65816",		&CPU_65816),
	//    ^^^^ this marks the last element
};


// init lowest and highest address
static void init_borders(value_t first_pc) {
	Mem_lowest_pc = first_pc;
	Mem_highest_pc = first_pc;
}

// set new program counter value
static void set_new_pc(value_t new_pc) {
	segment_start = new_pc;
	CPU_pc = new_pc;
	Mem_current_pc = new_pc;
}

// Link segment data into segment chain
static void link_segment(value_t start, value_t length) {
	segment_t	*new_segment;

	// may be faster if list is ordered!
	new_segment = ALLOC_PASS(sizeof(segment_t));
	new_segment->start = start;
	new_segment->length = length;
	new_segment->next = segment_list;
	segment_list = new_segment;
}

// Show start and end of current segment
void CPU_end_segment(void) {
	if(Mem_current_pc != CPU_pc)
		Throw_warning("Offset assembly still active at end of segment. Switched it off.");
	if(pass_flags & PASS_ISFIRST) {
		link_segment(segment_start, Mem_current_pc - segment_start);
		if(Process_verbosity > 1)
			printf("Segment size is %ld ($%lx) bytes ($%lx - $%lx exclusive).\n", Mem_current_pc - segment_start, Mem_current_pc - segment_start, segment_start, Mem_current_pc);
	}
}

// Set up new segment_max value according to the given program counter. Just
// find the next segment start and subtract 1.
void CPU_find_segment_max(value_t new_pc) {
	segment_t*	test_segment;

	// may be faster if list is ordered!
	segment_max = OUTBUFFERSIZE;// will be decremented later!
	test_segment = segment_list;
	while(test_segment) {
		if((test_segment->start > new_pc)
		&& (test_segment->start < segment_max))
			segment_max = test_segment->start;
		test_segment = test_segment->next;
	}
	segment_max--;// last free address available
}

// Check whether given PC is inside segment.
static void check_segment(value_t new_pc) {
	segment_t*	test_segment;

	test_segment = segment_list;
	while(test_segment) {
		if((new_pc >= test_segment->start)
		&& (new_pc < (test_segment->start) + (test_segment->length)))
			Throw_warning("Segment starts inside another one, overwriting it.");
		test_segment = test_segment->next;
	}
}

// Parse (re-)definitions of program counter
void CPU_set_pc(void) {// Now GotByte = "*"
	result_t	new_pc;

	NEXTANDSKIPSPACE();	// proceed with next char
	// re-definitions of program counter change segment
	if(GotByte == '=') {
		GetByte();// proceed with next char
		ALU_parse_expr_strict(&new_pc);
		if(CPU_pc != PC_UNDEFINED) {
			// It's a redefinition. Check some things:
			// check whether new low
			if(new_pc.value < Mem_lowest_pc)
				Mem_lowest_pc = new_pc.value;
			// show status of previous segment
			CPU_end_segment();
			// in first pass, maybe issue warning
			if(pass_flags & PASS_ISFIRST) {
				check_segment(new_pc.value);
				CPU_find_segment_max(new_pc.value);
			}
		} else	// it's the first pc definition
			init_borders(new_pc.value);
		set_new_pc(new_pc.value);
		Input_ensure_EOS();
	} else {
		Throw_error(exception_syntax);
		Input_skip_remainder();
	}
}

// make sure PC is defined. If not, complain and set to dummy value
// FIXME - get rid of this function as it slows down Output_byte
inline void CPU_ensure_defined_pc(void) {
	if(CPU_pc == PC_UNDEFINED) {
		Throw_error("Program counter is unset.");
		CPU_pc = PC_DUMMY;
	}
}

// Insert byte until PC fits conditions
static enum eos_t PO_align(void) {
	result_t	and,
			equal,
			fill;
	value_t		test	= CPU_pc;

	CPU_ensure_defined_pc();
	ALU_parse_expr_strict(&and);
	fill.value = FILLVALUE_ALIGN;
	if(!Input_accept_comma())
		Throw_error(exception_syntax);
	ALU_parse_expr_strict(&equal);
	if(Input_accept_comma())
		ALU_parse_expr_medium(&fill);
	while((test++ & and.value) != equal.value)
		Output_8b(fill.value);
	return(ENSURE_EOS);
}

// Try to find CPU type held in DynaBuf. Returns whether succeeded.
bool CPU_find_cpu_struct(cpu_t** target) {
	void*	node_body;

	if(!Tree_easy_scan(CPU_tree, &node_body, GlobalDynaBuf))
		return(FALSE);
	*target = node_body;
	return(TRUE);
}

// Select CPU ("!cpu" pseudo opcode)
static enum eos_t PO_cpu(void) {
	cpu_t*	cpu_buffer	= CPU_now;	// remember current cpu

	if(Input_read_and_lower_keyword())
		if(!CPU_find_cpu_struct(&CPU_now))
			Throw_error("Unknown processor.");
	// If there's a block, parse that and then restore old value!
	if(Parse_optional_block())
		CPU_now = cpu_buffer;
	return(ENSURE_EOS);
}

static const char	Warning_old_offset_assembly[]	=
	"\"!pseudopc/!realpc\" is deprecated; use \"!pseudopc {}\" instead.";

// Start offset assembly
static enum eos_t PO_pseudopc(void) {
	result_t	result;
	value_t		offset	= CPU_pc - Mem_current_pc;

	// set new
	ALU_parse_expr_strict(&result);
	CPU_pc = result.value;
	// If there's a block, parse that and then restore old value!
	if(Parse_optional_block())
		CPU_pc = (Mem_current_pc + offset) & 0xffff;	// restore old
	else if(pass_flags & PASS_ISFIRST)
		Throw_warning(Warning_old_offset_assembly);
	return(ENSURE_EOS);
}

// End offset assembly
static enum eos_t PO_realpc(void) {
	if(pass_flags & PASS_ISFIRST)
		Throw_warning(Warning_old_offset_assembly);
	CPU_pc = Mem_current_pc;// deactivate offset assembly
	return(ENSURE_EOS);
}

// If cpu type and value match, set register length variable to value.
// If cpu type and value don't match, complain instead.
static void check_and_set_reg_length(int *var, bool long_reg) {
	if(long_reg && ((CPU_now->flags & CPU_FLAG_LONG_REGS) == 0))
		Throw_error("Chosen CPU does not support long registers.");
	else
		*var = long_reg;
}

// Set register length, block-wise if needed.
static enum eos_t set_register_length(int* var, bool long_reg) {
	int	buffer	= *var;

	// Set new register length (or complain - whichever is more fitting)
	check_and_set_reg_length(var, long_reg);
	// If there's a block, parse that and then restore old value!
	if(Parse_optional_block())
		check_and_set_reg_length(var, buffer);// restore old length
	return(ENSURE_EOS);
}

// Switch to long accu ("!al" pseudo opcode)
static enum eos_t PO_al(void) {
	return(set_register_length(&CPU65816_long_a, TRUE));
}

// Switch to short accu ("!as" pseudo opcode)
static enum eos_t PO_as(void) {
	return(set_register_length(&CPU65816_long_a, FALSE));
}

// Switch to long index registers ("!rl" pseudo opcode)
static enum eos_t PO_rl(void) {
	return(set_register_length(&CPU65816_long_r, TRUE));
}

// Switch to short index registers ("!rs" pseudo opcode)
static enum eos_t PO_rs(void) {
	return(set_register_length(&CPU65816_long_r, FALSE));
}

// pseudo opcode table
static node_t	pseudo_opcodes[]	= {
	PREDEFNODE("align",	PO_align),
	PREDEFNODE("cpu",	PO_cpu),
	PREDEFNODE("pseudopc",	PO_pseudopc),
	PREDEFNODE("realpc",	PO_realpc),
	PREDEFNODE("al",	PO_al),
	PREDEFNODE("as",	PO_as),
	PREDEFNODE("rl",	PO_rl),
	PREDEFLAST("rs",	PO_rs),
	//    ^^^^ this marks the last element
};

// Set default values for pass
void CPU_passinit(cpu_t* cpu_type, signed long starting_pc) {
	// handle cpu type (default is 6502)
	CPU_now		= cpu_type ? cpu_type : &CPU_6502;
	CPU65816_long_a	= FALSE;	// short accumulator
	CPU65816_long_r	= FALSE;	// short index registers
	// handle program counter
	if(starting_pc == PC_UNDEFINED) {
		init_borders(0);	// set to _something_ (for !initmem)
		segment_start	= 0;
		CPU_pc = PC_UNDEFINED;
		Mem_current_pc = 0;
	} else {
		init_borders(starting_pc);
		set_new_pc(starting_pc);
	}
	// other stuff
	segment_list	= NULL;
	segment_max	= OUTBUFFERSIZE-1;
}

// init cpu type tree (is done early)
void CPUtype_init(void) {
	Tree_add_table(&CPU_tree, CPUs);
}

// init other stuff (done later)
void CPU_init(void) {
	Tree_add_table(&pseudo_opcode_tree, pseudo_opcodes);
}
