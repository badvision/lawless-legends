//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// CLI argument stuff

#include <stdlib.h>
#include <stdio.h>
#include "config.h"
#include "cliargs.h"


// Constants
const char	cliargs_error[]	= "Error in CLI arguments: ";


// Variables
static int		arguments_left;		// number of CLI arguments left
static const char**	next_argument;		// next argument pointer


// Exported functions

// Return pointer to next command line argument (NULL if no more)
const char* cliargs_get_next(void) {
	if(arguments_left == 0)
		return(NULL);
	arguments_left--;
	return(*next_argument++);
}

// parse command line switches
void cliargs_handle_options(char (*fn_short)(const char*), const char* (*fn_long)(const char*)) {
	const char	*problem_string,
			*argument;
	char		problem_char;

	do {
		// if there are no more arguments, return immediately
		if(arguments_left == 0)
			return;
		// if next argument is not an option, return immediately
		if((**next_argument) != '-')
			return;
		// officially fetch argument. We already know the
		// first character is a '-', so advance pointer.
		argument = cliargs_get_next() + 1;
		// Check for "--"
		if(*argument == '-') {
			// long argument
			if(argument[1] == '\0')
				return;	// when encountering "--", return
			else {
				problem_string = fn_long(argument + 1);
				if(problem_string) {
					fprintf(stderr, "%sUnknown option (--%s).\n", cliargs_error, problem_string);
					exit(EXIT_FAILURE);
				}
			}
		} else {
			problem_char = fn_short(argument);
			if(problem_char) {
				fprintf(stderr, "%sUnknown switch (-%c).\n", cliargs_error, problem_char);
				exit(EXIT_FAILURE);
			}
		}
	} while(TRUE);
}

// Return next arg. If there is none, complain and exit
const char* cliargs_get_string(const char name[]) {
	const char*	string;

	string = cliargs_get_next();
	if(string)
		return(string);
	fprintf(stderr, "%sMissing %s.\n", cliargs_error, name);
	exit(EXIT_FAILURE);
}

// Return signed long representation of next arg.
// If there is none, complain and exit.
// Copes with hexadecimal if prefixed with "$", "0x" or "0X".
// Copes with octal if prefixed with "&".
// Copes with binary if prefixed with "%".
// Assumes decimal otherwise.
signed long cliargs_get_long(const char name[]) {
	signed long	result;
	const char	*start;
	char		*end;
	int		base	= 10;

	start = cliargs_get_string(name);
	if(*start == '%') {
		base = 2;
		start++;
	} else if(*start == '&') {
		base = 8;
		start++;
	} else if(*start == '$') {
		base = 16;
		start++;
	} else if((*start == '0') && ((start[1] == 'x') || (start[1] == 'X'))) {
		base = 16;
		start += 2;
	}
	result = strtol(start, &end, base);
	if(*end == '\0')
		return(result);
	fprintf(stderr, "%sCould not parse '%s'.\n", cliargs_error, end);
	exit(EXIT_FAILURE);
}

// init command line handling stuff
const char* cliargs_init(int argc, const char *argv[]) {
	arguments_left = argc;
	next_argument = argv;
	return(cliargs_get_next());
}

// return unhandled (non-option) arguments. Complains if none.
void cliargs_get_rest(int *argc, const char***argv, const char error[]) {
	*argc = arguments_left;
	*argv = next_argument;

	if(error && (arguments_left == 0)) {
		fprintf(stderr, "%s%s.\n", cliargs_error, error);
		exit(EXIT_FAILURE);
	}
}
