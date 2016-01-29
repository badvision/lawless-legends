//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Label stuff

#include <stdio.h>
#include "acme.h"
#include "alu.h"
#include "cpu.h"
#include "dynabuf.h"
#include "global.h"
#include "input.h"
#include "label.h"
#include "mnemo.h"
#include "platform.h"
#include "section.h"
#include "tree.h"


// Variables
node_ra_t*	Label_forest[256];// ... (because of 8-bit hash)


// Dump label value and flags to dump file
static void dump_one_label(node_ra_t* node, FILE* fd) {
	label_t*	label	= node->body;

	// output name
	fprintf(fd, "%s", node->id_string);
	switch(label->flags & MVALUE_FORCEBITS) {

		case MVALUE_FORCE16:
		fprintf(fd, "+2=");
		break;

		case MVALUE_FORCE16 | MVALUE_FORCE24:
		/*FALLTHROUGH*/

		case MVALUE_FORCE24:
		fprintf(fd, "+3=");
		break;

		default:
		fprintf(fd, "  =");

	}
	if(label->flags & MVALUE_DEFINED)
		fprintf(fd, "$%x", (unsigned int) label->value);
	else
		fprintf(fd, " ?");
	if(label->flags & MVALUE_UNSURE)
		fprintf(fd, "; ?");
	if(label->usage == 0)
		fprintf(fd, "; unused");
	fprintf(fd, "\n");
}

// Search for label. Create if nonexistant. If created, give it flags "Flags".
// The label name must be held in GlobalDynaBuf.
label_t* Label_find(zone_t zone, int flags) {
	node_ra_t*	node;
	label_t*	label;
	bool		node_created;
	int		force_bits	= flags & MVALUE_FORCEBITS;

	node_created = Tree_hard_scan(&node, Label_forest, zone, TRUE);
	// if node has just been created, create label as well
	if(node_created) {
		// Create new label structure
		label = safe_malloc(sizeof(label_t));
		// Finish empty label item
		label->flags	= flags;
		label->value	= 0;
		label->usage	= 0;	// usage count
		label->pass	= pass_count;
		node->body = label;
	} else
		label = node->body;
	// make sure the force bits don't clash
	if((node_created == FALSE) && force_bits)
		if((label->flags & MVALUE_FORCEBITS) != force_bits)
			Throw_error("Too late for postfix.");
	return(label);
}

// Assign value to label. The routine acts upon the label's flag bits and
// produces an error if needed.
void Label_set_value(label_t* label, result_t* new, bool change) {
	int	flags	= label->flags;

	// value stuff
	if((flags & MVALUE_DEFINED) && (change == FALSE)) {
		// Label is already defined, so compare new and old values
		if(new->value != label->value)
			Throw_error("Label already defined.");
	} else
		// Label is not defined yet OR redefinitions are allowed
		label->value = new->value;
	// flags stuff
	// Ensure that "unsure" labels without "isByte" state don't get that
	if((flags & (MVALUE_UNSURE | MVALUE_ISBYTE)) == MVALUE_UNSURE)
		new->flags &= ~MVALUE_ISBYTE;
	if(change)
		flags = (flags & MVALUE_UNSURE) | new->flags;
	else {
		if((flags & MVALUE_FORCEBITS) == 0)
			if((flags & (MVALUE_UNSURE | MVALUE_DEFINED)) == 0)
				flags |= new->flags & MVALUE_FORCEBITS;
		flags |= new->flags & ~MVALUE_FORCEBITS;
	}
	label->flags = flags;
}

// (Re)set label
static enum eos_t PO_set(void) {// Now GotByte = illegal char
	result_t	result;
	int		force_bit;
	label_t*	label;
	zone_t		zone;

	if(Input_read_zone_and_keyword(&zone) == 0)	// skips spaces before
		// Now GotByte = illegal char
		return(SKIP_REMAINDER);
	force_bit = Mnemo_get_force_bit();// skips spaces after
	label = Label_find(zone, force_bit);
	if(GotByte != '=') {
		Throw_error(exception_syntax);
		return(SKIP_REMAINDER);
	}
	// label = parsed value
	GetByte();// proceed with next char
	ALU_parse_expr_medium(&result);
	// clear label's force bits and set new ones
	label->flags &= ~(MVALUE_FORCEBITS | MVALUE_ISBYTE);
	if(force_bit) {
		label->flags |= force_bit;
		result.flags &= ~(MVALUE_FORCEBITS | MVALUE_ISBYTE);
	}
	Label_set_value(label, &result, TRUE);
	return(ENSURE_EOS);
}

// Select dump file
static enum eos_t PO_sl(void) {
	// only process this pseudo opcode in the first pass
	if((pass_flags & PASS_ISFIRST) == 0)
		return(SKIP_REMAINDER);
	// if label dump file already chosen, complain and exit
	if(labeldump_filename) {
		Throw_warning("Label dump file already chosen.");
		return(SKIP_REMAINDER);
	}
	// read filename (returns a malloc'd copy)
	// if no file name given, exit (complaining will have been done)
	if((labeldump_filename = Input_read_filename(FALSE)) == NULL)
		return(SKIP_REMAINDER);
	// ensure there's no garbage at end of line
	return(ENSURE_EOS);
}

// predefined stuff
static node_t	pseudo_opcodes[]	= {
	PREDEFNODE("set",	PO_set),
	PREDEFLAST("sl",	PO_sl),
	//    ^^^^ this marks the last element
};

// Parse implicit label definition (can be either global or local).
// GlobalDynaBuf holds the label name.
void Label_implicit_definition(zone_t zone, int stat_flags, int force_bit, bool change) {
	result_t	result;
	label_t*	label;

	label = Label_find(zone, force_bit);
	// implicit label definition (label)
	if((stat_flags & SF_FOUND_BLANK)
	&& (pass_flags & PASS_ISFIRST)
	&& (1 == 1))	// FIXME - make configurable via CLI switch
		Throw_warning("Implicit label definition not in leftmost column.");
	CPU_ensure_defined_pc();
	result.value = CPU_pc;
	result.flags = MVALUE_DEFINED;
	Label_set_value(label, &result, change);
}

// Parse label definition (can be either global or local).
// GlobalDynaBuf holds the label name.
void Label_parse_definition(zone_t zone, int stat_flags) {
	result_t	result;
	label_t*	label;
	int		force_bit	= Mnemo_get_force_bit();// skips spaces after

	if(GotByte == '=') {
		// explicit label definition (label = <something>)
		label = Label_find(zone, force_bit);
		// label = parsed value
		GetByte();	// skip '='
		ALU_parse_expr_medium(&result);
		Label_set_value(label, &result, FALSE);
		Input_ensure_EOS();
	} else
		Label_implicit_definition(zone, stat_flags, force_bit, FALSE);
}

// Dump global labels into file
void Label_dump_all(FILE* fd) {
	Tree_dump_forest(Label_forest, ZONE_GLOBAL, dump_one_label, fd);
	PLATFORM_SETFILETYPE_TEXT(labeldump_filename);
}

// Init (add to tree and clear pointer table)
void Label_init(void) {
	node_ra_t**	ptr;
	int		i;

	Tree_add_table(&pseudo_opcode_tree, pseudo_opcodes);
	// Cut down all the trees (clear pointer table)
	ptr = Label_forest;
	// Clear pointertable
	for(i = 255; i >= 0; i--)
		*ptr++ = NULL;
}

// Fix name of anonymous forward label (held in DynaBuf, NOT TERMINATED!) so it
// references the *next* anonymous forward label definition. The tricky bit is,
// each name length would need its own counter. But hey, ACME's real quick in
// finding labels, so I'll just abuse the label system to store those counters.
label_t* Label_fix_forward_name(void) {
	label_t*	counter_label;
	unsigned long	number;

	// terminate name, find "counter" label and read value
	DynaBuf_append(GlobalDynaBuf, '\0');
	counter_label = Label_find(Section_now->zone, 0);
	// make sure it gets reset to zero in each new pass
	if(counter_label->pass != pass_count) {
		counter_label->pass = pass_count;
		counter_label->value = 0;
	}
	number = (unsigned long) counter_label->value;
	// now append to the name to make it unique
	GlobalDynaBuf->size--;	// forget terminator, we want to append
	do {
		DYNABUF_APPEND(GlobalDynaBuf, "0123456789abcdef"[number & 15]);
		number >>= 4;
	} while(number);
	DynaBuf_append(GlobalDynaBuf, '\0');
	return(counter_label);
}
