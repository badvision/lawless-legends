//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Dynamic buffer stuff

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "acme.h"
#include "global.h"
#include "dynabuf.h"
#include "input.h"


// Constants and macros

// macro to grow dynabuf (CAUTION - fails if a < 1)
#define MAKE_LARGER_THAN(a)		(2*(a))
// if someone requests a dynabuf smaller than this, use this size instead
#define DYNABUF_MINIMUM_INITIALSIZE	128	// should be >0 (see above)
// initial size for global dynabuf
// (as it holds macros, loop bodies, etc., make it large to begin with)
#define GLOBALDYNABUF_INITIALSIZE	1024	// should be >0 (see above)


// Variables
dynabuf_t*	GlobalDynaBuf;	// global dynamic buffer


// Functions

// get new buffer of given size
static void resize(dynabuf_t* db, size_t new_size) {
	char*	new_buf;

	new_buf = realloc(db->buffer, new_size);
	if(new_buf == NULL)
		Throw_serious_error(exception_no_memory_left);
	db->reserved = new_size;
	db->buffer = new_buf;
}


// Exported functions

// Create and init a dynamic buffer and return pointer
dynabuf_t* DynaBuf_create(int initial_size) {
	dynabuf_t*	db;

	if(initial_size < DYNABUF_MINIMUM_INITIALSIZE)
		initial_size = DYNABUF_MINIMUM_INITIALSIZE;
	if((db = malloc(sizeof(dynabuf_t)))) {
		db->size = 0;
		db->reserved = initial_size;
		db->buffer = malloc(initial_size);
		if(db->buffer)
			return(db);// if both pointers are != NULL, no error
	}
	// otherwise, complain
	fputs("Error: No memory for dynamic buffer.\n", stderr);
	exit(EXIT_FAILURE);
}

// Enlarge buffer
void DynaBuf_enlarge(dynabuf_t* db) {
	resize(db, MAKE_LARGER_THAN(db->reserved));
}

// Claim enough memory to hold a copy of the current buffer contents,
// make that copy and return it.
// The copy must be released by calling free().
char* DynaBuf_get_copy(dynabuf_t* db) {
	char	*copy;

	copy = safe_malloc(db->size);
	memcpy(copy, db->buffer, db->size);
	return(copy);
}

// add char to buffer
void DynaBuf_append(dynabuf_t* db, char byte) {
	DYNABUF_APPEND(db, byte);
}

// Append string to buffer (without terminator)
void DynaBuf_add_string(dynabuf_t* db, const char* string) {
	char	byte;

	while((byte = *string++))
		DYNABUF_APPEND(db, byte);
}

// add string version of value to buffer (without terminator)
void DynaBuf_add_value(dynabuf_t* db, value_t value) {
	value_t	header	= value / 10,
		last	= value % 10;

	// cater for negative values. "value = -value" would fail for -2^31
	if(value < 0) {
		DynaBuf_append(db, '-');
		header = -header;
		last = -last;
	}
	if(last < 0) {
		last += 10;
		header--;
	}
	if(header)
		DynaBuf_add_value(db, header);
	DynaBuf_append(db, "0123456789"[last]);
}

// Convert buffer contents to lower case (target and source may be identical)
void DynaBuf_to_lower(dynabuf_t* target, dynabuf_t* source) {
	char	*read,
		*write;

	// make sure target can take it
	if(source->size > target->reserved)
		resize(target, source->size);
	// convert to lower case
	read = source->buffer;// CAUTION - ptr may change when buf grows!
	write = target->buffer;// CAUTION - ptr may change when buf grows!
	while(*read)
		*write++ = (*read++) | 32;
	// Okay, so this method of converting to lowercase is lousy.
	// But actually it doesn't matter, because only pre-defined
	// keywords are converted, and all of those are plain
	// old-fashioned 7-bit ASCII anyway. So I guess it'll do.
	*write = '\0';	// terminate
}

// Initialisation - allocate global dynamic buffer
void DynaBuf_init(void) {
	GlobalDynaBuf = DynaBuf_create(GLOBALDYNABUF_INITIALSIZE);
}
