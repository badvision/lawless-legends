//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// CLI argument stuff
#ifndef cliargs_H
#define cliargs_H


// Constants
extern const char	cliargs_error[];


// Prototypes

// Handle options. Call fn_short for short options, fn_long for long ones.
extern void	cliargs_handle_options(char (*fn_short)(const char*), const char* (*fn_long)(const char*));
// Return next argument.
extern const char*	cliargs_get_next(void);
// Return next argument. If none left, complain with given name.
extern const char*	cliargs_get_string(const char name[]);
// Return next argument as signed long. If no arguments left, complain with
// given name. On parse error, exit with error message.
extern signed long	cliargs_get_long(const char name[]);
// Initialise argument handler. Returns program name (argv[0]).
extern const char*	cliargs_init(int argc, const char *argv[]);
// Get unhandled args. If none left, complain with given error message.
extern void	cliargs_get_rest(int *argc, const char***argv, const char error[]);


#endif
