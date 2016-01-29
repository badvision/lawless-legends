//
// ACME - a crossassembler for producing 6502/65c02/65816 code.
// Copyright (C) 1998-2006 Marco Baye
// Have a look at "acme.c" for further info
//
// Arithmetic/logic unit

#include <stdlib.h>
#include "platform.h"	// done first in case "inline" is redefined
#include "alu.h"
#include "cpu.h"
#include "dynabuf.h"
#include "encoding.h"
#include "global.h"
#include "input.h"
#include "label.h"
#include "section.h"
#include "tree.h"


// Constants

#define HALF_INITIAL_STACK_SIZE	8
static const char	exception_div_by_zero[]	= "Division by zero.";
static const char	exception_need_value[]	= "Value not yet defined.";
static const char	exception_paren_open[]	= "Too many '('.";

// Operator handles (FIXME - use function pointers instead? or too slow?)
enum op_handle_t {
//	special values (pseudo operators)
	OPHANDLE_END,		//		"reached end of expression"
	OPHANDLE_RETURN,	//		"return value to caller"
//	monadic operators
	OPHANDLE_OPENING,	//	(v	'(', starts subexpression
	OPHANDLE_NOT,		//	!v	NOT v	bit-wise NOT
	OPHANDLE_NEGATE,	//	-v		Negate
	OPHANDLE_LOWBYTEOF,	//	<v		Lowbyte of
	OPHANDLE_HIGHBYTEOF,	//	>v		Highbyte of
	OPHANDLE_BANKBYTEOF,	//	^v		Bankbyte of
//	dyadic operators
	OPHANDLE_CLOSING,	//	v)	')', ends subexpression
	OPHANDLE_POWEROF,	//	v^w
	OPHANDLE_MULTIPLY,	//	v*w
	OPHANDLE_DIVIDE,	//	v/w		Integer-Division
	OPHANDLE_MODULO,	//	v%w	v MOD w	Remainder
	OPHANDLE_SL,		//	v<<w	v ASL w	v LSL w	Shift left
	OPHANDLE_ASR,		//	v>>w	v ASR w	Arithmetic shift right
	OPHANDLE_LSR,		//	v>>>w	v LSR w	Logical shift right
	OPHANDLE_ADD,		//	v+w
	OPHANDLE_SUBTRACT,	//	v-w
	OPHANDLE_EQUALS,	//	v=w
	OPHANDLE_LE,		//	v<=w
	OPHANDLE_LESSTHAN,	//	v< w
	OPHANDLE_GE,		//	v>=w
	OPHANDLE_GREATERTHAN,	//	v> w
	OPHANDLE_NOTEQUAL,	//	v!=w	v<>w	v><w
	OPHANDLE_AND,		//	v&w		v AND w
	OPHANDLE_OR,		//	v|w		v OR w
	OPHANDLE_XOR,		//	v EOR w		v XOR w
};
struct operator_t {
	enum op_handle_t	handle;
	int			priority;
};
typedef struct operator_t op_t;

// operator structs (only hold handle and priority value)
static op_t OPSTRCT_END		= {OPHANDLE_END,	 0};	// special
static op_t OPSTRCT_RETURN	= {OPHANDLE_RETURN,	 1};	// special
static op_t OPSTRCT_CLOSING	= {OPHANDLE_CLOSING,	 2};	// dyadic
static op_t OPSTRCT_OPENING	= {OPHANDLE_OPENING,	 3};	// monadic
static op_t OPSTRCT_OR		= {OPHANDLE_OR,		 4};	// dyadic
static op_t OPSTRCT_XOR		= {OPHANDLE_XOR,	 5};	// dyadic
static op_t OPSTRCT_AND		= {OPHANDLE_AND,	 6};	// dyadic
static op_t OPSTRCT_EQUALS	= {OPHANDLE_EQUALS,	 7};	// dyadic
static op_t OPSTRCT_NOTEQUAL	= {OPHANDLE_NOTEQUAL,	 8};	// dyadic
	// same priority for all comparison operators
static op_t OPSTRCT_LE		= {OPHANDLE_LE,		 9};	// dyadic
static op_t OPSTRCT_LESSTHAN	= {OPHANDLE_LESSTHAN,	 9};	// dyadic
static op_t OPSTRCT_GE		= {OPHANDLE_GE,		 9};	// dyadic
static op_t OPSTRCT_GREATERTHAN	= {OPHANDLE_GREATERTHAN, 9};	// dyadic
	// same priority for all byte extraction operators
static op_t OPSTRCT_LOWBYTEOF	= {OPHANDLE_LOWBYTEOF,	10};	// monadic
static op_t OPSTRCT_HIGHBYTEOF	= {OPHANDLE_HIGHBYTEOF,	10};	// monadic
static op_t OPSTRCT_BANKBYTEOF	= {OPHANDLE_BANKBYTEOF,	10};	// monadic
	// same priority for all shift operators
static op_t OPSTRCT_SL		= {OPHANDLE_SL,		11};	// dyadic
static op_t OPSTRCT_ASR		= {OPHANDLE_ASR,	11};	// dyadic
static op_t OPSTRCT_LSR		= {OPHANDLE_LSR,	11};	// dyadic
	// same priority for "+" and "-"
static op_t OPSTRCT_ADD		= {OPHANDLE_ADD,	12};	// dyadic
static op_t OPSTRCT_SUBTRACT	= {OPHANDLE_SUBTRACT,	12};	// dyadic
	// same priority for "*", "/" and "%"
static op_t OPSTRCT_MULTIPLY	= {OPHANDLE_MULTIPLY,	13};	// dyadic
static op_t OPSTRCT_DIVIDE	= {OPHANDLE_DIVIDE,	13};	// dyadic
static op_t OPSTRCT_MODULO	= {OPHANDLE_MODULO,	13};	// dyadic
	// highest priorities
static op_t OPSTRCT_NEGATE	= {OPHANDLE_NEGATE,	14};	// monadic
static op_t OPSTRCT_POWEROF	= {OPHANDLE_POWEROF,	15};	// dyadic
static op_t OPSTRCT_NOT		= {OPHANDLE_NOT,	16};	// monadic


// Variables

static op_t**		operator_stack		= NULL;
static int		operator_stk_size	= HALF_INITIAL_STACK_SIZE;
static int		operator_sp;		// operator stack pointer
static struct result_t*	operand_stack		= NULL;	// value and flags
static int		operand_stk_size	= HALF_INITIAL_STACK_SIZE;
static int		operand_sp;		// value stack pointer
static int		indirect_flag;	// Flag for indirect addressing
					// (indicated by useless parentheses)
					// Contains either 0 or MVALUE_INDIRECT
enum alu_state_t {
	STATE_EXPECT_OPERAND_OR_MONADIC_OPERATOR,
	STATE_EXPECT_DYADIC_OPERATOR,
	STATE_TRY_TO_REDUCE_STACKS,
	STATE_MAX_GO_ON,	// "border value" to find the stoppers:
	STATE_ERROR,		// error has occured
	STATE_END,		// standard end
};
static enum alu_state_t	alu_state;	// deterministic finite automaton
// predefined stuff
static node_t*	operator_tree	= NULL;// tree to hold operators
static node_t	operator_list[]	= {
	PREDEFNODE("ASR",	&OPSTRCT_ASR),
	PREDEFNODE("LSR",	&OPSTRCT_LSR),
	PREDEFNODE("ASL",	&OPSTRCT_SL),
	PREDEFNODE("LSL",	&OPSTRCT_SL),
	PREDEFNODE("DIV",	&OPSTRCT_DIVIDE),
	PREDEFNODE("MOD",	&OPSTRCT_MODULO),
	PREDEFNODE("AND",	&OPSTRCT_AND),
	PREDEFNODE("OR",	&OPSTRCT_OR),
	PREDEFNODE("EOR",	&OPSTRCT_XOR),
	PREDEFLAST("XOR",	&OPSTRCT_XOR),
	//    ^^^^ this marks the last element
};

#define LEFTVALUE	(operand_stack[operand_sp-2].value)
#define RIGHTVALUE	(operand_stack[operand_sp-1].value)
#define LEFTFLAGSG	(operand_stack[operand_sp-2].flags)
#define RIGHTFLAGSG	(operand_stack[operand_sp-1].flags)

#define PUSH_OPERATOR(x)	operator_stack[operator_sp++] = (x)

#define PUSH_OPERAND(x, y)	do {\
	operand_stack[operand_sp].value = (x);\
	operand_stack[operand_sp++].flags = (y);\
} while(0)


// Functions

// Enlarge operator stack
static void enlarge_operator_stack(void) {
	operator_stk_size *= 2;
	operator_stack =
		realloc(operator_stack, operator_stk_size * sizeof(op_t*));
	if(operator_stack == NULL)
		Throw_serious_error(exception_no_memory_left);
}

// Enlarge operand stack
static void enlarge_operand_stack(void) {
	operand_stk_size *= 2;
	operand_stack =
		realloc(operand_stack, operand_stk_size * sizeof(struct result_t));
	if(operand_stack == NULL)
		Throw_serious_error(exception_no_memory_left);
}

// Init (add stuff to tree, create operator/operand stacks)
void ALU_init(void) {
	Tree_add_table(&operator_tree, operator_list);
	enlarge_operator_stack();
	enlarge_operand_stack();
}

// This routine handles problems that may be solved by performing further
// passes: "NeedValue" type errors.
// If the current pass has the corresponding flagbit set, this routine will
// not only count, but also show these errors to the user.
// FIXME - use a function pointer instead of checking flag bit?
static void result_is_undefined(void) {
	pass_undefined_count++;
	if(pass_flags & PASS_ISERROR)
		Throw_error(exception_need_value);
}

// calculate "to the power of" without having to resort to math.h's pow()
// function.
// my_pow(whatever,0) returns 1. my_pow(0,whatever_but_zero) returns 0.
static inline value_t my_pow(value_t mantissa, value_t exponent) {
	value_t	result	= 1;

	while(exponent) {
		// handle exponent's lowmost bit
		if(exponent & 1)
			result *= mantissa;
		// square the mantissa, halve the exponent
		mantissa *= mantissa;
		exponent >>= 1;
	}
	return(result);
}

// arithmetic shift right (works even if C compiler does not support it)
static inline value_t my_asr(value_t left, value_t right) {
	value_t	result	= left >>= right;

	if((left >= 0) || (result < 0))
		return(result);
	return(result | (-1L << (8 * sizeof(value_t) - right)));
}

// Lookup (and create, if necessary) label tree item and return its value.
// DynaBuf holds the label's name and "zone" its zone.
//
// This routine is not allowed to change DynaBuf because that's where the
// label name is stored!
static void get_label_value(zone_t zone) {
	label_t*	label;
	int		flags;

	// If the label gets created now, mark it as unsure
	label = Label_find(zone, MVALUE_UNSURE);
	flags = label->flags;
	// in first pass, count usage
	if(pass_flags & PASS_ISFIRST)
		label->usage++;
	PUSH_OPERAND(label->value, flags | MVALUE_EXISTS);
}

// Routine for parsing a quoted character. The character will be converted
// using the current encoding.
static inline void parse_quoted_character(char closing_quote) {
	value_t	value;

	// read character to parse - make sure not at end of statement
	if(GetQuotedByte() == CHAR_EOS)
		return;
	// on empty string, complain
	if(GotByte == closing_quote) {
		Throw_error(exception_missing_string);
		Input_skip_remainder();
		return;
	}
	// parse character
	value = (value_t) Encoding_encode_char(GotByte);
	// Read closing quote (hopefully)
	if(GetQuotedByte() == closing_quote)
		GetByte();// If length == 1, proceed with next byte
	else
		if(GotByte) {
			// If longer than one character
			Throw_error("There's more than one character.");
			Input_skip_remainder();
		}
	PUSH_OPERAND(value, MVALUE_GIVEN | MVALUE_ISBYTE);
	// Now GotByte = char following closing quote (or CHAR_EOS on error)
}

// Routine for parsing a hexadecimal value. It accepts "0" to "9", "a" to "f"
// and "A" to "F". Capital letters will be converted to lowercase letters using
// the flagtable. The current value is stored as soon as a character is read
// that is none of those given above.
static void parse_hexadecimal_value(void) {// Now GotByte = "$" or "x"
	char	byte;
	bool	go_on;		// continue loop flag
	int	digits	= -1,	// digit counter
		flags	= MVALUE_GIVEN;
	value_t	value	= 0;

	do {
		digits++;
		go_on = FALSE;
		byte = GetByte();
		//	first, convert "A-F" to "a-f"
		byte |= (BYTEFLAGS(byte) & BYTEIS_UPCASE);
		// if digit, add digit value
		if((byte >= '0') && (byte <= '9')) {
			value = (value << 4) + (byte - '0');
			go_on = TRUE;// keep going
		}
		// if legal ("a-f") character, add character value
		if((byte >= 'a') && (byte <= 'f')) {
			value = (value << 4) + (byte - 'a') + 10;
			go_on = TRUE;// keep going
		}
	} while(go_on);
	// set force bits
	if(digits > 2) {
		if(digits > 4) {
			if(value < 65536)
				flags |= MVALUE_FORCE24;
		} else {
			if(value < 256)
				flags |= MVALUE_FORCE16;
		}
	}
	PUSH_OPERAND(value, flags);
	// Now GotByte = non-hexadecimal char
}

// Routine for parsing a decimal value. It accepts "0" to "9". The current
// value is stored as soon as a character is read that is none of those given
// above. Unlike the others, the "decimal" routine expects the first digit to
// be read already, because decimal values don't use any prefixes.
// If the first two digits are "0x", this routine branches to the one for
// parsing hexadecimal values.
static inline void parse_decimal_value(void) {// Now GotByte = first digit
	value_t	value	= (GotByte & 15);	// this works. it's ASCII.

	GetByte();
	if((value == 0) && (GotByte == 'x'))
		parse_hexadecimal_value();
	else {
		while((GotByte >= '0') && (GotByte <= '9')) {
			value *= 10;
			value += (GotByte & 15);// this works. it's ASCII.
			GetByte();
		}
		PUSH_OPERAND(value, MVALUE_GIVEN);
	}
	// Now GotByte = non-decimal char
}

// Routine for parsing an octal value. It accepts "0" to "7". The current
// value is stored as soon as a character is read that is none of those given
// above.
static inline void parse_octal_value(void) {// Now GotByte = "&"
	value_t	value	= 0;
	int	flags	= MVALUE_GIVEN,
		digits	= 0;	// digit counter

	GetByte();
	while((GotByte >= '0') && (GotByte <= '7')) {
		value = (value << 3) + (GotByte & 7);// this works. it's ASCII.
		digits++;
		GetByte();
	}
	// set force bits
	if(digits > 3) {
		if(digits > 6) {
			if(value < 65536)
				flags |= MVALUE_FORCE24;
		} else {
			if(value < 256)
				flags |= MVALUE_FORCE16;
		}
	}
	PUSH_OPERAND(value, flags);
	// Now GotByte = non-octal char
}

// Routine for parsing a binary value. Apart from "0" and "1", it also accepts
// characters "." and "#", this is much more readable. The current value is
// stored as soon as a character is read that is none of those given above.
static inline void parse_binary_value(void) {// Now GotByte = "%"
	value_t	value	= 0;
	bool	go_on	= TRUE;	// continue loop flag
	int	flags	= MVALUE_GIVEN,
		digits	= -1;	// digit counter

	do {
		digits++;
		switch(GetByte()) {

			case '0':
			case '.':
			value <<= 1;
			break;

			case '1':
			case '#':
			value = (value << 1) | 1;
			break;

			default:
			go_on = FALSE;
		}
	} while(go_on);
	// set force bits
	if(digits > 8) {
		if(digits > 16) {
			if(value < 65536)
				flags |= MVALUE_FORCE24;
		} else {
			if(value < 256)
				flags |= MVALUE_FORCE16;
		}
	}
	PUSH_OPERAND(value, flags);
	// Now GotByte = non-binary char
}

// Expect operand or monadic operator (hopefully inlined)
static inline void expect_operand_or_monadic_operator(void) {
	op_t*	operator;
	bool	perform_negation;

	SKIPSPACE();
	switch(GotByte) {

		case '+':// anonymous forward label
		// count plus signs to build name of anonymous label
		DYNABUF_CLEAR(GlobalDynaBuf);
		do
			DYNABUF_APPEND(GlobalDynaBuf, '+');
		while(GetByte() == '+');
		Label_fix_forward_name();
		get_label_value(Section_now->zone);
		goto now_expect_dyadic;

		case '-':// NEGATION operator or anonymous backward label
		// count minus signs in case it's an anonymous backward label
		perform_negation = FALSE;
		DYNABUF_CLEAR(GlobalDynaBuf);
		do {
			DYNABUF_APPEND(GlobalDynaBuf, '-');
			perform_negation = !perform_negation;
		} while(GetByte() == '-');
		SKIPSPACE();
		if(BYTEFLAGS(GotByte) & FOLLOWS_ANON) {
			DynaBuf_append(GlobalDynaBuf, '\0');
			get_label_value(Section_now->zone);
			goto now_expect_dyadic;
		} // goto means we don't need an "else {" here
		if(perform_negation)
			PUSH_OPERATOR(&OPSTRCT_NEGATE);
		// State doesn't change
		break;

// Real monadic operators (state doesn't change, still ExpectMonadic)

		case '!':// NOT operator
		operator = &OPSTRCT_NOT;
		goto get_byte_and_push_monadic;

		case '<':// LOWBYTE operator
		operator = &OPSTRCT_LOWBYTEOF;
		goto get_byte_and_push_monadic;

		case '>':// HIGHBYTE operator
		operator = &OPSTRCT_HIGHBYTEOF;
		goto get_byte_and_push_monadic;

		case '^':// BANKBYTE operator
		operator = &OPSTRCT_BANKBYTEOF;
		goto get_byte_and_push_monadic;

// Faked monadic operators

		case '(':// left parenthesis
		operator = &OPSTRCT_OPENING;
		goto get_byte_and_push_monadic;

		case ')':// right parenthesis
		// this makes "()" also throw a syntax error
		Throw_error(exception_syntax);
		alu_state = STATE_ERROR;
		break;

// Operands (values, state changes to ExpectDyadic)

		// Quoted character
		case '"':
		case '\'':
		// Character will be converted using current encoding
		parse_quoted_character(GotByte);
		// Now GotByte = char following closing quote
		goto now_expect_dyadic;

		// Binary value
		case '%':
		parse_binary_value();// Now GotByte = non-binary char
		goto now_expect_dyadic;

		// Octal value
		case '&':
		parse_octal_value();// Now GotByte = non-octal char
		goto now_expect_dyadic;

		// Hexadecimal value
		case '$':
		parse_hexadecimal_value();
		// Now GotByte = non-hexadecimal char
		goto now_expect_dyadic;

		// Program counter
		case '*':
		GetByte();// proceed with next char
		CPU_ensure_defined_pc();
		PUSH_OPERAND(CPU_pc, MVALUE_GIVEN);
		// Now GotByte = char after closing quote
		goto now_expect_dyadic;

		// Local label
		case '.':
		operand_stack[operand_sp].value = 0;
		GetByte();// start after '.'
		if(Input_read_keyword()) {
			// Now GotByte = illegal char
			get_label_value(Section_now->zone);
			goto now_expect_dyadic;
		}
		alu_state = STATE_ERROR;
		break;

		// Decimal values and global labels
		default:// (all other characters)
		if((GotByte >= '0') && (GotByte <= '9')) {
			parse_decimal_value();
			// Now GotByte = non-decimal char
			goto now_expect_dyadic;
		} // goto means we don't need an "else {" here
		if(BYTEFLAGS(GotByte) & STARTS_KEYWORD) {
			register int	length;
			// Read global label (or "NOT")
			length = Input_read_keyword();
			// Now GotByte = illegal char
			// Check for NOT. Okay, it's hardcoded,
			// but so what? Sue me...
			if((length == 3)
			&& (GlobalDynaBuf->buffer[0] == 'N')
			&& (GlobalDynaBuf->buffer[1] == 'O')
			&& (GlobalDynaBuf->buffer[2] == 'T')) {
				PUSH_OPERATOR(&OPSTRCT_NOT);
				// state doesn't change
			} else {
				get_label_value(ZONE_GLOBAL);
				alu_state = STATE_EXPECT_DYADIC_OPERATOR;
			}
		} else {
			// illegal character read - so don't go on
			PUSH_OPERAND(0, 0);
			// push pseudo value, EXISTS flag is clear
			if(operator_stack[operator_sp-1] == &OPSTRCT_RETURN) {
				PUSH_OPERATOR(&OPSTRCT_END);
				alu_state = STATE_TRY_TO_REDUCE_STACKS;
			} else {
				Throw_error(exception_syntax);
				alu_state = STATE_ERROR;
			}
		}
		break;

// no other possibilities, so here are the shared endings

get_byte_and_push_monadic:
		GetByte();
		PUSH_OPERATOR(operator);
		// State doesn't change
		break;

now_expect_dyadic:
		alu_state = STATE_EXPECT_DYADIC_OPERATOR;
		break;

	}
}

// Expect dyadic operator (hopefully inlined)
static inline void expect_dyadic_operator(void) {
	void*	node_body;
	op_t*	operator;

	SKIPSPACE();
	switch(GotByte) {

// Single-character dyadic operators

		case '^':// "to the power of"
		operator = &OPSTRCT_POWEROF;
		goto get_byte_and_push_dyadic;

		case '+':// add
		operator = &OPSTRCT_ADD;
		goto get_byte_and_push_dyadic;

		case '-':// subtract
		operator = &OPSTRCT_SUBTRACT;
		goto get_byte_and_push_dyadic;

		case '*':// multiply
		operator = &OPSTRCT_MULTIPLY;
		goto get_byte_and_push_dyadic;

		case '/':// divide
		operator = &OPSTRCT_DIVIDE;
		goto get_byte_and_push_dyadic;

		case '%':// modulo
		operator = &OPSTRCT_MODULO;
		goto get_byte_and_push_dyadic;

		case '&':// bitwise AND
		operator = &OPSTRCT_AND;
		goto get_byte_and_push_dyadic;

		case '|':// bitwise OR
		operator = &OPSTRCT_OR;
		goto get_byte_and_push_dyadic;

// This part is commented out because there is no XOR character defined
//		case '':// bitwise exclusive OR
//		operator = &OPSTRCT_XOR;
//		goto get_byte_and_push_dyadic;

		case '=':// is equal
		operator = &OPSTRCT_EQUALS;
		goto get_byte_and_push_dyadic;

		case ')':// closing parenthesis
		operator = &OPSTRCT_CLOSING;
		goto get_byte_and_push_dyadic;

// Multi-character dyadic operators

		// "!="
		case '!':
		if(GetByte() == '=') {
			operator = &OPSTRCT_NOTEQUAL;
			goto get_byte_and_push_dyadic;
		} // goto means we don't need an "else {" here
		Throw_error(exception_syntax);
		alu_state = STATE_ERROR;
		break;

		// "<", "<=", "<<" and "<>"
		case '<':
		switch(GetByte()) {

			case '=':// "<=", less or equal
			operator = &OPSTRCT_LE;
			goto get_byte_and_push_dyadic;

			case '<':// "<<", shift left
			operator = &OPSTRCT_SL;
			goto get_byte_and_push_dyadic;

			case '>':// "<>", not equal
			operator = &OPSTRCT_NOTEQUAL;
			goto get_byte_and_push_dyadic;

			default:// "<", less than
			operator = &OPSTRCT_LESSTHAN;
			goto push_dyadic;
		}
		break;

		// ">", ">=", ">>", ">>>" and "><"
		case '>':
		switch(GetByte()) {

			case '=':// ">=", greater or equal
			operator = &OPSTRCT_GE;
			goto get_byte_and_push_dyadic;

			case '<':// "><", not equal
			operator = &OPSTRCT_NOTEQUAL;
			goto get_byte_and_push_dyadic;

			case '>':// ">>" or ">>>", shift right
			operator = &OPSTRCT_ASR;// arithmetic shift right
			if(GetByte() != '>')
				goto push_dyadic;
			operator = &OPSTRCT_LSR;// logical shift right
			goto get_byte_and_push_dyadic;

			default:// ">", greater than
			operator = &OPSTRCT_GREATERTHAN;
			goto push_dyadic;
		}
		break;

// end of expression or text version of dyadic operator
		default:
		// check string version of operators
		if(BYTEFLAGS(GotByte) & STARTS_KEYWORD) {
			Input_read_keyword();
			// Now GotByte = illegal char
			// search for tree item
			if(Tree_easy_scan(operator_tree, &node_body, GlobalDynaBuf)) {
				operator = node_body;
				goto push_dyadic;
			}
			// goto means we don't need an "else {" here
			Throw_error("Unknown operator.");
			alu_state = STATE_ERROR;
		} else {
			operator = &OPSTRCT_END;
			goto push_dyadic;
		}
		break;

// no other possibilities, so here are the shared endings

get_byte_and_push_dyadic:
		GetByte();
push_dyadic:
		PUSH_OPERATOR(operator);
		alu_state = STATE_TRY_TO_REDUCE_STACKS;
		break;

	}
}

// Try to reduce stacks by performing high-priority operations
static inline void try_to_reduce_stacks(int* open_parentheses) {
	if(operator_sp < 2) {
		alu_state = STATE_EXPECT_OPERAND_OR_MONADIC_OPERATOR;
		return;
	}
	if(operator_stack[operator_sp-2]->priority < operator_stack[operator_sp-1]->priority) {
		alu_state = STATE_EXPECT_OPERAND_OR_MONADIC_OPERATOR;
		return;
	}
	switch(operator_stack[operator_sp-2]->handle) {

// special (pseudo) operators

		case OPHANDLE_RETURN:
		// don't touch indirect_flag; needed for INDIRECT flag
		operator_sp--;// decrement operator stack pointer
		alu_state = STATE_END;
		break;

		case OPHANDLE_OPENING:
		indirect_flag = MVALUE_INDIRECT;// parentheses found
		switch(operator_stack[operator_sp-1]->handle) {

			case OPHANDLE_CLOSING:// matching parentheses
			operator_sp -= 2;// remove both of them
			alu_state = STATE_EXPECT_DYADIC_OPERATOR;
			break;

			case OPHANDLE_END:// unmatched parenthesis
			(*open_parentheses)++;// count
			goto RNTLObutDontTouchIndirectFlag;

			default:
			Bug_found("StrangeParenthesis", operator_stack[operator_sp-1]->handle);
		}
		break;

		case OPHANDLE_CLOSING:
		Throw_error("Too many ')'.");
		goto remove_next_to_last_operator;

// monadic operators

		case OPHANDLE_NOT:
		RIGHTVALUE = ~(RIGHTVALUE);
		RIGHTFLAGSG &= ~MVALUE_ISBYTE;
		goto remove_next_to_last_operator;

		case OPHANDLE_NEGATE:
		RIGHTVALUE = -(RIGHTVALUE);
		RIGHTFLAGSG &= ~MVALUE_ISBYTE;
		goto remove_next_to_last_operator;

		case OPHANDLE_LOWBYTEOF:
		RIGHTVALUE = (RIGHTVALUE) & 255;
		RIGHTFLAGSG |= MVALUE_ISBYTE;
		RIGHTFLAGSG &= ~MVALUE_FORCEBITS;
		goto remove_next_to_last_operator;

		case OPHANDLE_HIGHBYTEOF:
		RIGHTVALUE = ((RIGHTVALUE) >> 8) & 255;
		RIGHTFLAGSG |= MVALUE_ISBYTE;
		RIGHTFLAGSG &= ~MVALUE_FORCEBITS;
		goto remove_next_to_last_operator;

		case OPHANDLE_BANKBYTEOF:
		RIGHTVALUE = ((RIGHTVALUE) >> 16) & 255;
		RIGHTFLAGSG |= MVALUE_ISBYTE;
		RIGHTFLAGSG &= ~MVALUE_FORCEBITS;
		goto remove_next_to_last_operator;

// dyadic operators

		case OPHANDLE_POWEROF:
		if(RIGHTVALUE >= 0)
			LEFTVALUE = my_pow(LEFTVALUE, RIGHTVALUE);
		else {
			if(RIGHTFLAGSG & MVALUE_DEFINED)
				Throw_error("Exponent is negative.");
			LEFTVALUE = 0;
		}
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_MULTIPLY:
		LEFTVALUE *= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_DIVIDE:
		if(RIGHTVALUE) LEFTVALUE /= RIGHTVALUE;
		else {
			if(RIGHTFLAGSG & MVALUE_DEFINED)
				Throw_error(exception_div_by_zero);
			LEFTVALUE = 0;
		}
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_MODULO:
		if(RIGHTVALUE) LEFTVALUE %= RIGHTVALUE;
		else {
			if(RIGHTFLAGSG & MVALUE_DEFINED)
				Throw_error(exception_div_by_zero);
			LEFTVALUE = 0;
		}
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_ADD:
		LEFTVALUE += RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_SUBTRACT:
		LEFTVALUE -= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_SL:
		LEFTVALUE <<= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_ASR:
		LEFTVALUE = my_asr(LEFTVALUE, RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_LSR:
		LEFTVALUE = ((uvalue_t) LEFTVALUE) >> RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_LE:
		LEFTVALUE = (LEFTVALUE <= RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_LESSTHAN:
		LEFTVALUE = (LEFTVALUE < RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_GE:
		LEFTVALUE = (LEFTVALUE >= RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_GREATERTHAN:
		LEFTVALUE = (LEFTVALUE > RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_NOTEQUAL:
		LEFTVALUE = (LEFTVALUE != RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_EQUALS:
		LEFTVALUE = (LEFTVALUE == RIGHTVALUE);
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_AND:
		LEFTVALUE &= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_XOR:
		LEFTVALUE ^= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		case OPHANDLE_OR:
		LEFTVALUE |= RIGHTVALUE;
		goto handle_flags_and_dec_stacks;

		default:
		Bug_found("IllegalOperatorHandle", operator_stack[operator_sp-2]->handle);
		break;

// no other possibilities, so here are the shared endings

// entry point for dyadic operators
handle_flags_and_dec_stacks:
		// Handle flags and decrement value stack pointer
		// "OR" EXISTS, UNSURE and FORCEBIT flags
		LEFTFLAGSG |= RIGHTFLAGSG &
			(MVALUE_EXISTS|MVALUE_UNSURE|MVALUE_FORCEBITS);
		// "AND" DEFINED flag
		LEFTFLAGSG &= (RIGHTFLAGSG | ~MVALUE_DEFINED);
		LEFTFLAGSG &= ~MVALUE_ISBYTE;// clear ISBYTE flag
		operand_sp--;
		/*FALLTHROUGH*/

// entry point for monadic operators
remove_next_to_last_operator:
		// toplevel operation was something other than parentheses
		indirect_flag = 0;
		/*FALLTHROUGH*/

// entry point for '(' operator (has set indirect_flag, so don't clear now)
RNTLObutDontTouchIndirectFlag:
		// Remove operator and shift down next one
		operator_stack[operator_sp-2] = operator_stack[operator_sp-1];
		operator_sp--;// decrement operator stack pointer
		break;
	}
}

// The core of it. Returns number of parentheses left open.
// FIXME - make state machine using function pointers?
static int parse_expression(result_t* result) {
	int	open_parentheses = 0;

	operator_sp = 0;	// operator stack pointer
	operand_sp = 0;	// value stack pointer
	// begin by reading value (or monadic operator)
	alu_state = STATE_EXPECT_OPERAND_OR_MONADIC_OPERATOR;
	indirect_flag = 0;	// Contains either 0 or MVALUE_INDIRECT
	PUSH_OPERATOR(&OPSTRCT_RETURN);
	do {
		// check stack sizes. enlarge if needed
		if(operator_sp >= operator_stk_size)
			enlarge_operator_stack();
		if(operand_sp >= operand_stk_size)
			enlarge_operand_stack();
		switch(alu_state) {

			case STATE_EXPECT_OPERAND_OR_MONADIC_OPERATOR:
			expect_operand_or_monadic_operator();
			break;

			case STATE_EXPECT_DYADIC_OPERATOR:
			expect_dyadic_operator();
			break;// no fallthrough; state might
			// have been changed to END or ERROR

			case STATE_TRY_TO_REDUCE_STACKS:
			try_to_reduce_stacks(&open_parentheses);
			break;

			case STATE_MAX_GO_ON:	// suppress
			case STATE_ERROR:	// compiler
			case STATE_END:		// warnings
			break;
		}
	} while(alu_state < STATE_MAX_GO_ON);
	// done. check state.
	if(alu_state == STATE_END) {
		// check for bugs
		if(operand_sp != 1)
			Bug_found("OperandStackNotEmpty", operand_sp);
		if(operator_sp != 1)
			Bug_found("OperatorStackNotEmpty", operator_sp);
		// okay, no errors
		*result = operand_stack[0];	// copy struct
		result->flags |= indirect_flag;	// and OR indirect flag
		// only allow *one* force bit
		if(result->flags & MVALUE_FORCE24)
			result->flags &= ~(MVALUE_FORCE16 | MVALUE_FORCE08);
		if(result->flags & MVALUE_FORCE16)
			result->flags &= ~MVALUE_FORCE08;
		// if value is sure, check to set ISBYTE
		if((result->flags & MVALUE_UNSURE) == 0)
			if((result->value <= 255) && (result->value >= -128))
				result->flags |= MVALUE_ISBYTE;
		// if there was nothing to parse, mark as undefined
		// (so ALU_parse_expr_strict() can react)
		if((result->flags & MVALUE_EXISTS) == 0)
			result->flags &= ~MVALUE_DEFINED;
		// if undefined, return zero
		if((result->flags & MVALUE_DEFINED) == 0)
			result->value = 0;// if undefined, return 0.
	} else {
		// State is STATE_ERROR. But actually, nobody cares.
		// ...errors have already been reported anyway. :)
	}
	// return number of open (unmatched) parentheses
	return(open_parentheses);
}

// These routines handle numerical expressions. There are operators for
// arithmetic, logic, shift and comparison operations.
// There are four different ways to call the core routine:
//	void ALU_parse_expr_strict		(result_t*)
//	void ALU_parse_expr_empty_strict	(result_t*)
//	void ALU_parse_expr_medium		(result_t*)
//	int  ALU_parse_expr_liberal		(result_t*)
// After calling one of these functions, the given result_t structure holds
// some information:
//	value	holds the numerical result.
//	flags	holds some additional informational flags about the result:
//	    Unsure	A label was referenced that had its "unsure"-flag set
//	    Defined	The result is known (no "undefined"-label referenced)
//	    ForceBits	To enforce oversized addressing modes
//	    IsByte	If the value fits in 8 bits
//	    Exists	Expression wasn't empty
//	    Indirect	Needless parentheses indicate indirect addressing
// "Unsure" is needed for producing the same addresses in all passes; because
//	in the first pass there will almost for sure be labels that are
//	undefined, you can't simply get the addressing mode from looking at the
//	parameter's value.
// "Defined" shows that the value really could be computed - so if an undefined
//	label was referenced, this flag will be cleared.
// "Indirect" is needed to recognize unnecessary parentheses (which imply use
//	of indirect adressing modes).
// The return value of "ALU_parse_expr_liberal(result_t*)" is the number of
// parentheses still open.

// This routine needs a defined value. If the result's "defined" flag is
// clear, it throws a serious error and therefore stops assembly.
void ALU_parse_expr_strict(result_t* result) {
	if(parse_expression(result))
		Throw_error(exception_paren_open);
	if((result->flags & MVALUE_DEFINED) == 0)
		Throw_serious_error(exception_need_value);
}

// This routine needs either a defined value or no expression at all. So
// empty expressions are accepted, but undefined ones are not.
// If the result's "defined" flag is clear and the "exists" flag is set, it
// throws a serious error and therefore stops assembly.
void ALU_parse_expr_empty_strict(result_t* result) {
	if(parse_expression(result))
		Throw_error(exception_paren_open);
	if((result->flags & MVALUE_GIVEN) == MVALUE_EXISTS)
		Throw_serious_error(exception_need_value);
}

// The 'normal' call, it parses an expression and returns the result.
// It the result's "exists" flag is clear (=empty expression), it throws an
// error.
// If the result's "defined" flag is clear, result_is_undefined() is called.
void ALU_parse_expr_medium(result_t* result) {
	if(parse_expression(result))
		Throw_error(exception_paren_open);
	if((result->flags & MVALUE_EXISTS) == 0)
		Throw_error(exception_need_value);
	else
		if((result->flags & MVALUE_DEFINED) == 0)
			result_is_undefined();
}

// This routine allows for one "(" too many. Needed when parsing indirect
// addressing modes where internal indices have to be possible. Returns number
// of parentheses still open (either 0 or 1).
int ALU_parse_expr_liberal(result_t* result) {
	int	parentheses_still_open;

	parentheses_still_open = parse_expression(result);
	if(parentheses_still_open > 1) {
		parentheses_still_open = 0;
		Throw_error(exception_paren_open);
	}
	if(result->flags & MVALUE_EXISTS)
		if((result->flags & MVALUE_DEFINED) == 0)
			result_is_undefined();
	return(parentheses_still_open);
}
