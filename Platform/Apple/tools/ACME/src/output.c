//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Output stuff

#include <stdlib.h>
#include <stdio.h>
#include <string.h>	// for memset()
#include "acme.h"
#include "alu.h"
#include "config.h"
#include "cpu.h"
#include "dynabuf.h"
#include "global.h"
#include "input.h"
#include "output.h"
#include "platform.h"
#include "tree.h"


// Constants
#define USERMSG_DYNABUF_INITIALSIZE	80

// Variables
static bool	memory_initialised	= FALSE;
value_t	Mem_current_pc;		// Current program counter (real memory address)
value_t	Mem_lowest_pc;	// Start address of program (first PC given)
value_t	Mem_highest_pc;	// End address of program plus one
static char*		output_buffer	= NULL;	// to hold assembled code
static dynabuf_t*	user_message;	// dynamic buffer (!warn/error/serious)
// Chosen output file format
static enum out_format_t	output_format	= OUTPUT_FORMAT_UNSPECIFIED;
// predefined stuff
static node_t*	file_format_tree	= NULL;// tree to hold output formats
// Possible output file formats
static node_t	file_formats[]	= {
	PREDEFNODE(s_cbm,	OUTPUT_FORMAT_CBM),
//	PREDEFNODE("o65",	OUTPUT_FORMAT_O65),	FIXME - add!
	PREDEFLAST("plain",	OUTPUT_FORMAT_PLAIN),
	//    ^^^^ this marks the last element
};
static const char	s_08[]	= "08";
#define s_8	(&s_08[1])	// Yes, I know I'm sick


// Functions

// Send low byte to output file, automatically increasing program counter
void Output_byte(value_t byte) {
	int	offset;

	CPU_ensure_defined_pc();
	offset = Mem_current_pc + pc_inc;
	if(offset == segment_max + 1) {
		if(offset == OUTBUFFERSIZE)
			Throw_serious_error("Produced too much code.");
		Throw_warning("Segment reached another one, overwriting it.");
		if(pass_flags & PASS_ISFIRST)
			CPU_find_segment_max(offset + 1);
	}
	output_buffer[offset] = byte & 255;
	pc_inc++;
}

// Output 8-bit value with range check
void Output_8b(value_t value) {
	if((value <= 255) && (value >= -128))
		Output_byte(value);
	else
		Throw_error(exception_number_out_of_range);
}

// Output 16-bit values with range check
void Output_16b(value_t value) {
	if((value <= 65535) && (value >= -32768)) {
		Output_byte(value);
		Output_byte(value >> 8);
	} else
		Throw_error(exception_number_out_of_range);
}

// Output 24-bit values with range check
void Output_24b(value_t value) {
	if((value <= 0xffffff) && (value >= -0x800000)) {
		Output_byte(value);
		Output_byte(value >> 8);
		Output_byte(value >> 16);
	} else
		Throw_error(exception_number_out_of_range);
}

// Output 32-bit values (without range check)
void Output_32b(value_t value) {
//  if((Value <= 0x7fffffff) && (Value >= -0x80000000)) {
	Output_byte(value);
	Output_byte(value >> 8);
	Output_byte(value >> 16);
	Output_byte(value >> 24);
//  } else
//	Throw_error(exception_number_out_of_range);
}

// Helper function for !8, !16, !24 and !32 pseudo opcodes
static enum eos_t output_objects(void (*fn)(value_t)) {
	result_t	result;

	do {
		ALU_parse_expr_medium(&result);
		fn(result.value);
	} while(Input_accept_comma());
	return(ENSURE_EOS);
}

// Insert 8-bit values ("!08" / "!8" / "!by" / "!byte" pseudo opcode)
static enum eos_t PO_08(void) {
	return(output_objects(Output_8b));
}

// Insert 16-bit values ("!16" / "!wo" / "!word" pseudo opcode)
static enum eos_t PO_16(void) {
	return(output_objects(Output_16b));
}

// Insert 24-bit values ("!24" pseudo opcode)
static enum eos_t PO_24(void) {
	return(output_objects(Output_24b));
}

// Insert 32-bit values ("!32" pseudo opcode)
static enum eos_t PO_32(void) {
	return(output_objects(Output_32b));
}

// Helper function for PO_binary()
static FILE* open_bin_and_skip(char* filename, int skip) {
	FILE*	fd	= fopen(filename, FILE_READBINARY);

	if(fd)
		fseek(fd, skip, SEEK_SET);	// skip "Skip" bytes
	else
		Throw_error(exception_cannot_open_input_file);// on error, complain
	return(fd);
}

// Include binary file
static enum eos_t PO_binary(void) {
	result_t	result;
	FILE*		fd;
	char*		filename;
	int		byte,
			size_given	= 0;
	value_t		size,
			skip	= 0;

	// if file name is missing, don't bother continuing
	if((filename = Input_read_filename(TRUE)) == NULL) // uses copy
		return(SKIP_REMAINDER);
	// read arguments, if any
	if(Input_accept_comma()) {
		ALU_parse_expr_empty_strict(&result);// read size argument
		size = result.value;
		size_given = result.flags & MVALUE_EXISTS;
		// read skip argument, if given
		if(Input_accept_comma()) {
			ALU_parse_expr_empty_strict(&result);
			if(result.flags & MVALUE_EXISTS)
				skip = result.value;
		}
	}
	if(size_given) {
		// explicit size info, so truncate or pad file
		if(pass_undefined_count || pass_real_errors)
			// don't insert file if it's just a waste of time
			pc_inc += size;
		else {
			// really try to open file and insert it
			fd = open_bin_and_skip(filename, skip);
			free(filename);	// file name no longer needed
			if(fd == NULL)
				return(SKIP_REMAINDER);

			// copy "Size" bytes from file to output
			while(size--) {
				byte = getc(fd);
				if(byte == EOF)
					byte = 0;
				Output_8b(byte);
			}
			fclose(fd);
		}
	} else {
		// no explicit size info, so read file until EOF
		fd = open_bin_and_skip(filename, skip);
		free(filename);	// file name no longer needed
		if(fd == NULL)
			return(SKIP_REMAINDER);
		// copy bytes from file to output until EOF
		while((byte = getc(fd)) != EOF)
			Output_8b(byte);
		fclose(fd);
	}
	// if verbose, produce some output
	if((pass_flags & PASS_ISFIRST) && (Process_verbosity > 1))
		printf("Loaded %ld ($%lx) bytes from file offset %ld ($%lx).\n",
		pc_inc, pc_inc, skip, skip);
	return(ENSURE_EOS);
}

// Reserve space by sending bytes of given value ("!fi" / "!fill" pseudo opcode)
static enum eos_t PO_fill(void) {
	result_t	size;
	result_t	fill;

	fill.value = FILLVALUE_FILL;
	ALU_parse_expr_strict(&size);
	if(Input_accept_comma())
		ALU_parse_expr_medium(&fill);
	while(size.value--)
		Output_8b(fill.value);
	return(ENSURE_EOS);
}

// Fill output buffer with given byte value
static void fill_completely(char value) {
	memset(output_buffer, value, OUTBUFFERSIZE);
}

// Define default value for empty memory ("!initmem" pseudo opcode)
static enum eos_t PO_initmem(void) {
	result_t	result;

	if((pass_flags & PASS_ISFIRST) == 0)
		return(SKIP_REMAINDER);
	// if MemInit flag is already set, complain
	if(memory_initialised) {
		Throw_warning("Memory already initialised.");
		return(SKIP_REMAINDER);
	}
	// set MemInit flag
	memory_initialised = TRUE;
	// get value and init memory
	ALU_parse_expr_strict(&result);
	if((result.value > 255) || (result.value < -128))
		Throw_error(exception_number_out_of_range);
	// init memory
	fill_completely(result.value & 0xff);
	// tricky bit
	// enforce another pass
	if((pass_undefined_count == 0)
	&& (Mem_lowest_pc < Mem_highest_pc))
		pass_undefined_count = 1;
	return(ENSURE_EOS);
}

// show user-defined message
static enum eos_t throw_string(const char prefix[], void (*fn)(const char*)) {
	result_t	result;

	DYNABUF_CLEAR(user_message);
	DynaBuf_add_string(user_message, prefix);
	do {
		if(GotByte == '"') {
			// parse string
			GetQuotedByte();	// read initial character
			// send characters until closing quote is reached
			while(GotByte && (GotByte != '"')) {
				DYNABUF_APPEND(user_message, GotByte);
				GetQuotedByte();
			}
			if(GotByte == CHAR_EOS)
				return(AT_EOS_ANYWAY);
			// after closing quote, proceed with next char
			GetByte();
		} else {
			// parse value
			ALU_parse_expr_medium(&result);
			if(result.flags & MVALUE_DEFINED)
				DynaBuf_add_value(user_message, result.value);
			else
				DynaBuf_add_string(user_message, "<UNDEFINED>");
		}
	} while(Input_accept_comma());
	DynaBuf_append(user_message, '\0');
	fn(user_message->buffer);
	return(ENSURE_EOS);
}

////
//static enum eos_t PO_print(void) {
//	return(throw_string(FIXME));
//}

// throw warning as given in source code
static enum eos_t PO_warn(void) {
	return(throw_string("!warn: ", Throw_warning));
}

// throw error as given in source code
static enum eos_t PO_error(void) {
	return(throw_string("!error: ", Throw_error));
}

// throw serious error as given in source code
static enum eos_t PO_serious(void) {
	return(throw_string("!serious: ", Throw_serious_error));
}

// Try to set output format in DynaBuf. Returns whether succeeded.
bool Output_set_output_format(void) {
	void*	node_body;

	if(!Tree_easy_scan(file_format_tree, &node_body, GlobalDynaBuf))
		return(FALSE);
	output_format = (enum out_format_t) node_body;
	return(TRUE);
}

// Select output file and format ("!to" pseudo opcode)
static enum eos_t PO_to(void) {
	// only act upon this pseudo opcode in first pass
	if((pass_flags & PASS_ISFIRST) == 0)
		return(SKIP_REMAINDER);
	// if output file already chosen, complain
	if(output_filename) {
		Throw_warning("Output file already chosen.");
		return(SKIP_REMAINDER);
	}
	// on file name error, give up
	if((output_filename = Input_read_filename(FALSE)) == NULL) // uses copy
		return(SKIP_REMAINDER);
	// select output format
	// if no comma found, use default file format
	if(Input_accept_comma() == FALSE) {
		if(output_format == OUTPUT_FORMAT_UNSPECIFIED) {
			output_format = OUTPUT_FORMAT_CBM;
			// output deprecation warning
			if(pass_flags & PASS_ISFIRST)
				Throw_warning("Used \"!to\" without file format indicator. Defaulting to \"cbm\".");
			}
		return(ENSURE_EOS);
	}
	// parse output format name
	// if no keyword given, give up
	if(Input_read_and_lower_keyword() == 0)
		return(SKIP_REMAINDER);
	if(Output_set_output_format())
		return(ENSURE_EOS);	// success
	// error occurred
	Throw_error("Unknown output format.");
	return(SKIP_REMAINDER);
}

// pseudo ocpode table
static node_t	pseudo_opcodes[]	= {
	PREDEFNODE(s_08,	PO_08),
	PREDEFNODE(s_8,		PO_08),
	PREDEFNODE("by",	PO_08),
	PREDEFNODE("byte",	PO_08),
	PREDEFNODE("16",	PO_16),
	PREDEFNODE("wo",	PO_16),
	PREDEFNODE("word",	PO_16),
	PREDEFNODE("24",	PO_24),
	PREDEFNODE("32",	PO_32),
	PREDEFNODE("bin",	PO_binary),
	PREDEFNODE("binary",	PO_binary),
	PREDEFNODE("fi",	PO_fill),
	PREDEFNODE("fill",	PO_fill),
	PREDEFNODE("initmem",	PO_initmem),
//	PREDEFNODE("print",	PO_print),
	PREDEFNODE("warn",	PO_warn),
	PREDEFNODE("error",	PO_error),
	PREDEFNODE("serious",	PO_serious),
	PREDEFLAST("to",	PO_to),
	//    ^^^^ this marks the last element
};

// Init file format tree (is done early)
void Outputfile_init(void) {
	Tree_add_table(&file_format_tree, file_formats);
}

// Init other stuff (done later)
void Output_init(signed long fill_value) {
	// FIXME - call safe_malloc instead (after having it fixed so it can
	// be called before a section is set up)
	output_buffer = malloc(OUTBUFFERSIZE);
	if(output_buffer == NULL) {
		fputs("Error: No memory for output buffer.\n", stderr);
		exit(EXIT_FAILURE);
	}
	if(fill_value == MEMINIT_USE_DEFAULT)
		fill_value = FILLVALUE_INITIAL;
	else
		memory_initialised = TRUE;
	user_message = DynaBuf_create(USERMSG_DYNABUF_INITIALSIZE);
	Tree_add_table(&pseudo_opcode_tree, pseudo_opcodes);
	// init output buffer (fill memory with initial value)
	fill_completely(fill_value & 0xff);
}

// Dump memory buffer into output file
void Output_save_file(FILE* fd, value_t start, value_t end) {
	if(Process_verbosity)
		printf("Saving %ld ($%lx) bytes ($%lx - $%lx exclusive).\n",
			end - start, end - start, start, end);

	// Output file header according to file format
	switch(output_format) {

		case OUTPUT_FORMAT_UNSPECIFIED:
		case OUTPUT_FORMAT_PLAIN:
		PLATFORM_SETFILETYPE_PLAIN(output_filename);
		break;

		case OUTPUT_FORMAT_CBM:
		PLATFORM_SETFILETYPE_CBM(output_filename);
		// output 16-bit load address in little-endian byte order
		putc(start & 255, fd);
		putc(start >>  8, fd);
		break;

	}
	// Dump output buffer to file
	fwrite(output_buffer + start, sizeof(char), end - start, fd);
}
