//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Mnemonic definitions
#ifndef mnemo_H
#define mnemo_H


// Prototypes
extern void	Mnemo_init(void);
extern int	Mnemo_get_force_bit(void);
extern bool	keyword_is_6502mnemo(int length);
extern bool	keyword_is_6510mnemo(int length);
extern bool	keyword_is_65c02mnemo(int length);
//extern bool	keyword_is_Rockwell65c02mnemo(int length);
//extern bool	keyword_is_WDC65c02mnemo(int length);
extern bool	keyword_is_65816mnemo(int length);


#endif
