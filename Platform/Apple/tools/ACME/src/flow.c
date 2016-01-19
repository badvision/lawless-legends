//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Flow control stuff (loops, conditional assembly etc.)
//
// Macros, conditional assembly, loops and sourcefile-includes are all based on
// parsing blocks of code. When defining macros or using loops or conditional
// assembly, the block starts with "{" and ends with "}". In the case of
// "!source", the given file is treated like a block - the outermost assembler
// routine uses the same technique to parse the top level file.

#include <string.h>
#include "acme.h"
#include "alu.h"
#include "config.h"
#include "dynabuf.h"
#include "global.h"
#include "input.h"
#include "label.h"
#include "macro.h"
#include "mnemo.h"
#include "tree.h"


// type definitions
enum cond_key_t {
	ID_UNTIL,	// Handles to store instead of
	ID_WHILE,	// the UNTIL and WHILE keywords
};
typedef struct {
	enum cond_key_t	type;	// either ID_UNTIL or ID_WHILE
	int		line;	// original line number
	char*		body;	// pointer to actual expression
} loopcond_t;


// Variables

// predefined stuff
static node_t*	condkey_tree	= NULL;// tree to hold UNTIL and WHILE
static node_t	condkeys[]	= {
	PREDEFNODE("until",	ID_UNTIL),
	PREDEFLAST("while",	ID_WHILE),
	//    ^^^^ this marks the last element
};


// Helper functions for "!for" and "!do"

// Parse a loop body (FIXME - or a macro?)
static void parse_ram_block(int line_number, char* body) {
	Input_now->line_number = line_number;// set line number to loop start
	Input_now->src.ram_ptr = body;// set RAM read pointer to loop
	// Parse loop body
	Parse_until_eob_or_eof();
	if(GotByte != CHAR_EOB)
		Bug_found("IllegalBlockTerminator", GotByte);
}

// Try to read a condition into DynaBuf and store copy pointer in
// given loopcond_t structure.
// If no condition given, NULL is written to structure.
// Call with GotByte = first interesting character
static void store_condition(loopcond_t* condition, char terminator) {
	void*	node_body;

	// write line number
	condition->line = Input_now->line_number;
	// Check for empty condition
	if(GotByte == terminator) {
		// Write NULL condition, then return
		condition->body = NULL;
		return;
	}
	// Seems as if there really *is* a condition.
	// Read UNTIL/WHILE keyword
	if(Input_read_and_lower_keyword()) {
		// Search for new tree item
		if(!Tree_easy_scan(condkey_tree, &node_body, GlobalDynaBuf)) {
			Throw_error(exception_syntax);
			condition->body = NULL;
			return;
		}
		condition->type = (enum cond_key_t) node_body;
		// Write given condition into buffer
		SKIPSPACE();
		DYNABUF_CLEAR(GlobalDynaBuf);
		Input_until_terminator(terminator);
		DynaBuf_append(GlobalDynaBuf, CHAR_EOS);// ensure terminator
		condition->body = DynaBuf_get_copy(GlobalDynaBuf);
	}
	return;
}

// Check a condition expression
static bool check_condition(loopcond_t* condition) {
	result_t	expression;

	// First, check whether there actually *is* a condition
	if(condition->body == NULL)
		return(TRUE);	// non-existant conditions are always true
	// set up input for expression evaluation
	Input_now->line_number = condition->line;
	Input_now->src.ram_ptr = condition->body;
	GetByte();	// proceed with next char
	ALU_parse_expr_strict(&expression);
	if(GotByte)
		Throw_serious_error(exception_syntax);
	if(condition->type == ID_UNTIL)
		return(!expression.value);
	return(expression.value != FALSE);
}

// Looping assembly ("!do"). Has to be re-entrant.
static enum eos_t PO_do(void) {	// Now GotByte = illegal char
	loopcond_t	condition1,
			condition2;
	input_t		loop_input,
			*outer_input;
	char*		loop_body;
	bool		go_on;
	int		loop_start;// line number of loop pseudo opcode

	// Read head condition to buffer
	SKIPSPACE();
	store_condition(&condition1, CHAR_SOB);
	if(GotByte != CHAR_SOB)
		Throw_serious_error(exception_no_left_brace);
	// Remember line number of loop body,
	// then read block and get copy
	loop_start = Input_now->line_number;
	loop_body = Input_skip_or_store_block(TRUE);	// changes line number!
	// now GotByte = '}'
	NEXTANDSKIPSPACE();// Now GotByte = first non-blank char after block
	// Read tail condition to buffer
	store_condition(&condition2, CHAR_EOS);
	// now GotByte = CHAR_EOS
	// set up new input
	loop_input = *Input_now;// copy current input structure into new
	loop_input.source_is_ram = TRUE;	// set new byte source
	// remember old input
	outer_input = Input_now;
	// activate new input (not useable yet, as pointer and
	// line number are not yet set up)
	Input_now = &loop_input;
	do {
		// Check head condition
		go_on = check_condition(&condition1);
		if(go_on) {
			parse_ram_block(loop_start, loop_body);
			// Check tail condition
			go_on = check_condition(&condition2);
		}
	} while(go_on);
	// Free memory
	free(condition1.body);
	free(loop_body);
	free(condition2.body);
	// restore previous input:
	Input_now = outer_input;
	GotByte = CHAR_EOS;	// CAUTION! Very ugly kluge.
	// But by switching input, we lost the outer input's GotByte. We know
	// it was CHAR_EOS. We could just call GetByte() to get real input, but
	// then the main loop could choke on unexpected bytes. So we pretend
	// that we got the outer input's GotByte value magically back.
	return(AT_EOS_ANYWAY);
}

// Looping assembly ("!for"). Has to be re-entrant.
static enum eos_t PO_for(void) {// Now GotByte = illegal char
	input_t		loop_input,
			*outer_input;
	result_t	loop_counter;
	value_t		maximum;
	char*		loop_body;// pointer to loop's body block
	label_t*	label;
	zone_t		zone;
	int		force_bit,
			loop_start;// line number of "!for" pseudo opcode

	if(Input_read_zone_and_keyword(&zone) == 0)	// skips spaces before
		return(SKIP_REMAINDER);
	// Now GotByte = illegal char
	force_bit = Mnemo_get_force_bit();	// skips spaces after
	label = Label_find(zone, 0);	// FIXME - is it really useful that !for can set/clear force bits at will?
	if(force_bit) {
		label->flags &= ~MVALUE_FORCEBITS;// clear force bits
		label->flags |= force_bit;// set desired force bit
	}
	if(Input_accept_comma() == FALSE) {
		Throw_error(exception_syntax);
		return(SKIP_REMAINDER);
	}
	ALU_parse_expr_strict(&loop_counter);
// prepare flags, size of *end* value doesn't matter for *all* loops
	loop_counter.flags &= ~MVALUE_FORCEBITS;	// clear force bits
	if(loop_counter.value < 0)
		Throw_serious_error("Loop count is negative.");
	if(GotByte != CHAR_SOB)
		Throw_serious_error(exception_no_left_brace);
	// remember line number of loop pseudo opcode
	loop_start = Input_now->line_number;
	// read loop body into DynaBuf and get copy
	loop_body = Input_skip_or_store_block(TRUE);	// changes line number!
	// switching input makes us lose GotByte. But we know it's '}' anyway!
	// set up new input
	loop_input = *Input_now;// copy current input structure into new
	loop_input.source_is_ram = TRUE;	// set new byte source
	// remember old input
	outer_input = Input_now;
	// activate new input
	// (not yet useable; pointer and line number are still missing)
	Input_now = &loop_input;
	if(loop_counter.value) {// skip loop if counter == 0
		maximum = loop_counter.value;// remember maximum value
		loop_counter.value = 1;// start counting
		do {
			// set counter
			Label_set_value(label, &loop_counter, TRUE);
			parse_ram_block(loop_start, loop_body);
			// increase loop counter
			loop_counter.value++;
		} while(loop_counter.value <= maximum);
	} else
		Label_set_value(label, &loop_counter, TRUE); // set counter
	// Free memory
	free(loop_body);
	// restore previous input:
	Input_now = outer_input;
	// GotByte of OuterInput would be '}' (if it would still exist)
	GetByte();	// fetch next byte
	return(ENSURE_EOS);
}

// Helper functions for "!if" and "!ifdef"

// Parse or skip a block. Returns whether block's '}' terminator was missing.
// Afterwards: GotByte = '}'
static bool skip_or_parse_block(bool parse) {
	if(!parse) {
		Input_skip_or_store_block(FALSE);
		return(FALSE);
	}
	// if block was correctly terminated, return FALSE
	Parse_until_eob_or_eof();
	// if block isn't correctly terminated, complain and exit
	if(GotByte != CHAR_EOB)
		Throw_serious_error(exception_no_right_brace);
	return(FALSE);
}

// Parse {block} [else {block}]
static void parse_block_else_block(bool parse_first) {
	// Parse first block.
	// If it's not correctly terminated, return immediately (because
	// in that case, there's no use in checking for an "else" part).
	if(skip_or_parse_block(parse_first))
		return;
	// now GotByte = '}'. Check for "else" part.
	// If end of statement, return immediately.
	NEXTANDSKIPSPACE();
	if(GotByte == CHAR_EOS)
		return;
	// read keyword and check whether really "else"
	if(Input_read_and_lower_keyword()) {
		if(strcmp(GlobalDynaBuf->buffer, "else"))
			Throw_error(exception_syntax);
		else {
			SKIPSPACE();
			if(GotByte != CHAR_SOB)
				Throw_serious_error(exception_no_left_brace);
			skip_or_parse_block(!parse_first);
			// now GotByte = '}'
			GetByte();
		}
	}
	Input_ensure_EOS();
}

// Conditional assembly ("!if"). Has to be re-entrant.
static enum eos_t PO_if(void) {// Now GotByte = illegal char
	result_t	result;

	ALU_parse_expr_strict(&result);
	if(GotByte != CHAR_SOB)
		Throw_serious_error(exception_no_left_brace);
	parse_block_else_block(!!(result.value));
	return(ENSURE_EOS);
}

// Conditional assembly ("!ifdef"). Has to be re-entrant.
static enum eos_t PO_ifdef(void) {// Now GotByte = illegal char
	node_ra_t*	node;
	label_t*	label;
	zone_t		zone;
	bool		defined	= FALSE;

	if(Input_read_zone_and_keyword(&zone) == 0)	// skips spaces before
		return(SKIP_REMAINDER);
	Tree_hard_scan(&node, Label_forest, zone, FALSE);
	if(node) {
		label = (label_t*) node->body;
		// in first pass, count usage
		if(pass_flags & PASS_ISFIRST)
			label->usage++;
		if(label->flags & MVALUE_DEFINED)
			defined = TRUE;
	}
	SKIPSPACE();
	if(GotByte == CHAR_SOB)
		parse_block_else_block(defined);
	else {
		if(defined)
			return(PARSE_REMAINDER);
		return(SKIP_REMAINDER);
	}
	return(ENSURE_EOS);
}

// Macro definition ("!macro").
static enum eos_t PO_macro(void) {// Now GotByte = illegal char
	// In first pass, parse. In all other passes, skip.
	if(pass_flags & PASS_ISFIRST)
		Macro_parse_definition();	// now GotByte = '}'
	else {
		// skip until CHAR_SOB ('{') is found.
		// no need to check for end-of-statement, because such an
		// error would already have been detected in first pass.
		// for the same reason, there is no need to check for quotes.
		while(GotByte != CHAR_SOB)
			GetByte();
		Input_skip_or_store_block(FALSE);	// now GotByte = '}'
	}
	GetByte();	// Proceed with next character
	return(ENSURE_EOS);
}

// Parse a whole source code file
void Parse_and_close_file(FILE* fd, const char* filename) {
	// be verbose
	if(Process_verbosity > 2)
		printf("Parsing source file '%s'\n", filename);
	// set up new input
	Input_new_file(filename, fd);
	// Parse block and check end reason
	Parse_until_eob_or_eof();
	if(GotByte != CHAR_EOF)
		Throw_error("Found '}' instead of end-of-file.");
	// close sublevel src
	fclose(Input_now->src.fd);
}

// Include source file ("!source" or "!src"). Has to be re-entrant.
static enum eos_t PO_source(void) {// Now GotByte = illegal char
	FILE*	fd;
	char	local_gotbyte,
		*filename;
	input_t	new_input,
		*outer_input;

	// Enter new nesting level.
	// Quit program if recursion too deep.
	if(--source_recursions_left < 0)
		Throw_serious_error("Too deeply nested. Recursive \"!source\"?");
	// Read file name. Quit function on error.
	if((filename = Input_read_filename(TRUE)) == NULL) // uses copy
		return(SKIP_REMAINDER);
	// If file could be opened, parse it. Otherwise, complain.
	if((fd = fopen(filename, FILE_READBINARY))) {
		outer_input = Input_now;// remember old input
		local_gotbyte = GotByte;// CAUTION - ugly kluge
		Input_now = &new_input;// activate new input
		Parse_and_close_file(fd, filename);
		Input_now = outer_input;// restore previous input
		GotByte = local_gotbyte;// CAUTION - ugly kluge
	} else
		Throw_error(exception_cannot_open_input_file);
	// Release dynamically allocated file name
	free(filename);
	// Leave nesting level
	source_recursions_left++;
	return(ENSURE_EOS);
}

// pseudo opcode table
static node_t	pseudo_opcodes[]	= {
	PREDEFNODE("do",	PO_do),
	PREDEFNODE("for",	PO_for),
	PREDEFNODE("if",	PO_if),
	PREDEFNODE("ifdef",	PO_ifdef),
	PREDEFNODE("macro",	PO_macro),
	PREDEFNODE("source",	PO_source),
	PREDEFLAST("src",	PO_source),
	//    ^^^^ this marks the last element
};

// Init (add stuff to tree)
void Flow_init(void) {
	Tree_add_table(&condkey_tree, condkeys);
	Tree_add_table(&pseudo_opcode_tree, pseudo_opcodes);
}
