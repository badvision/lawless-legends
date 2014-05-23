# PLASMA Programming User Manual
## ( Proto Language AsSeMbler for Apple)

## Introduction
PLASMA is a medium level programming language targetting the 8 bit 6502 processor. Historically, there were simple languages developed in the early history of computers that improved on the tedium of assembly language programming while still being low level enough for system coding. Languages like B, FORTH, and PLASMA fall into this category. The following will take you through the process of writing, building and running a PLASMA module.

### PLASMA Modules
To keep development compartmentalized and easily managed, PLASMA uses relatively small, dynamically loaded and linked modules. The module format extends the .REL filetype originally defined by the EDASM assembler from the DOS/ProDOS Toolkit from Apple Computer, Inc. PLASMA extends the file format through a backwards compatible extension that the PLASMA loader recognizes to locate the PLASMA bytecode and provide for advanced dynamic loading of module dependencies.

### Obligatory 'Hello World'
To start things off, here is the standard introductory program:

```
import stdlib
    predef puts
end
    
byte hello[] = "Hello, world.\n"
    
puts(@hello)
done
```

Three tools are required to build and run this program: **plasm**, **acme**, and **plvm**. The PLASMA compiler, **plasm**, will convert the PLASMA source code (usually with an extension of .pla) into an assembly language source file. **acme**, the portable 6502 assembler, will convert the assembly source into a binary ready for loading. To execute the module, the PLASMA portable VM, **plvm**, can load and interpret the bytecode. The same binary can be loaded onto the target platform and run there with the appropriate VM. On Linux/Unix from lawless-legends/PLASMA/src, the steps would be entered as:

```
./plasm -AM < hello.pla > hello.a
acme --setpc 4096 -o HELLO.REL hello.a
./plvm HELLO.REL
```

The computer will respond with:

```
Load module HELLO.REL
Hello, world.
```

A couple of things to note: **plasm** only accepts input from stdin and output to stdout. To build acme compatible module source, tha '-AM' flags must be passed in. The **acme** assembler needs the --setpc 4096 to assemble the module at the proper address, and the -o option sets the output file. The makefile in the lawless-legends/PLASMA/src directory has automated this process. Enter:

```
make hello
```

for the make program to automate this.

## Organization of a PLASMA Source File
### Comments
Comments are allowed throughout a PLASMA source file. The format follows that of an assembler: they begin with a `;` and comment out the rest of the line:

```
; This is a comment, the rest of this line is ignored
```

### Declarations
The beginning of the source file is the best place for certain declarations. This will help when reading others' code as well as returning to your own after a time.

#### Module Dependencies
Module dependencies will direct the loader to make sure these modules are loaded first, thus resolving any outstanding references.  A module dependency is declared with the `import` statement block with predefined function and data definitions. The `import` block is completed with an `end`. An example:

```
import stdlib
    const reshgr1 = $0004
    predef putc, puts, getc, gets, cls, gotoxy
end

import testlib
    predef puti
    byte testdata, teststring
    word testarray
end
```

The `predef` pre-defines functions that can be called throughout the module.  The data declarations, `byte` and `word` will refer to data in those modules. `const` can appear in an `import` block, although not required. It does keep values associated with the imported module in a well-contained block for readability and useful with pre-processor file inclusion. Case is not significant for either the module name nor the pre-defined function/data labels. They are all converted to uppercase with 16 characters significant when the loader resolves them.

#### Constant Declarations
Constants help with the readability of source code where hard-coded numbers might not be very descriptive.

```
const MACHID  = $BF98
const speaker = $C030
const bufflen = 2048
```

These constants can be used in expressions just like a variable name.

#### Predefined Functions
Sometimes a function needs to be referenced before it is defined. The `predef` declaration reserves the label for a function. The `import` declaration block also uses the `predef` declaration to reserve an external function. Outside of an `import` block, `predef` will only predefine a function that must be declared later in the source file, otherwise an error will occur.

```
predef exec_file, mydef
```

#### Global Data & Variable Declarations
One of the most powerful features in PLASMA is the flexible data declarations. 

#### Native Functions
An advanced feature of PLASMA is the ability to write functions in native assembly language. This is a very advanced topic that is covered more in-depth in the Advanced Topics section.

#### Function Definitions
Function definitions **must** come after all other declarations. Once a function definition is written, no other global declarations are allowed.

#### Module Initialization Function
After all the function definitions are complete, an optional module initiialization routine follows. This is an un-named defintion an is written in-line without a definition declaration. As such, it doesn't have parameters or local variables. Function definitions can be called from within the initialization code.

#### Exported Declarations
Data and function labels can be exported so other modules may access this modules data and code. By prepending `export` to the data or functions declaration, the label will become available to the loader for inter-module resolution.

```
export def plot(x, y)
    romcall(y, 0, x, 0, $F800)
end
```

#### Module Done
The final declaration of a module source file is the `done` statement. This declares the end of the source file. Anything following this statement is ignored.

### m4 Pre-Processor
The m4 pre-processor can be very helpful when managing module imports and macro facilities. The easiest way to use the pre-processor is to write a module import header for each library module. Any module that depends on a given library can `include()` the shared header file. See the GNU m4 documentation for more information: https://www.gnu.org/software/m4/manual/

## Stacks
The basic architecture of PLASMA relies on different stack based FIFO data structures. The stacks aren't directly manipulated from PLASMA, but almost every PLASMA operation involves one or more of the stacks. A stack architecture is a very flexible and convenient way to manage an interpreted language, even if it isn't the highest performance.

### Call Stack
The call stack, where function return addresses are saved, is implemented using the hardware call stack of the CPU. This makes for a fast and efficient implementation of function call/return.

### Local Frame Stack
Any function definition that involves parameters or local variables builds a local frame to contain the variables. Often called automatic variables, they only persist during the lifetime of the function. They are a very powerful tool when implementing recursive algorithms. PLASMA puts a limitation of 254 bytes for the size of the frame, due to the nature of the 6502 CPU. With careful planning, this shouldn't be too constraining.

### Evaluation Stack
All temporary values are loaded and manipulated on the PLASMA evaluation stack. This is a small (16 element) stack implemeted in high performance memory/registers of the host CPU. Parameters to functions are passed on the evaluation stack, then moved to local variables for named reference inside the funtion.

## Data Types
PLASMA only really defines two data types: `byte`, `word`. All operations take place on word sized quantities, with the exception of loads and stores to byte sized addresses. The interpretation of a value can be an interger, an address, or anything that fits in 16 bits. There are a number of address operators to identify how an address value is to be interpreted.

### Decimal and Hexadecimal Numbers
Numbers can be represented in either decimal (base 10), or hexadecimal (base 16). Values beginning with a `$` will be parsed as hexadecimal, in keeping with 6502 assembler syntax.

### Character and String Literals
A character literal, represented by a single character or an escaped character enclosed in single quotes `'`, can be used wherever a number is used. String literals, a character sequence enclosed in double quotes `"`, can only appear in a data definition. A length byte will be calculated and prepended to the character data. This is the Pascal style of string definition used throughout PLASMA and ProDOS. When referencing the string, it's address is used:
```
char mystring[] = "This is my string; I am very proud of \n"
    
puts(@mystring)
```
Excaped characters, like the `\n` above are replaces with the Carriage Return character.  The list of escaped characters is:

| Escaped Char | ASCII Value
|:------------:|------------
|   \n         |    13
|   \t         |     8
|   \r         |    13
|   \\\\       |    \
|   \\0        |    0

### Words
Words, 16 bit signed values, are the native sized quanta of PLASMA. All calculations, parameters, and return values are words.

### Bytes
Bytes are unsigned, 8 bit values, stored at an address.  Bytes cannot be manipulated as bytes, but are promoted to words as soon as they are read onto the evaluation stack. When written to a byte addres, the low order byte of a word is used.

### Addresses
Words can represent many things in PLASMA, including addresses. PLASMA uses a 16 bit address space for data and function entrypoints. There are many operators in PLASMA to help with address calculation and access. Due to the signed implementation of word in PLASMA, the Standard Library has some unsigned comparison functions to help with address comparisons.

#### Arrays
Arrays are the most useful data structure in PLASMA. Using an index into a list of values is indispensible. PLASMA has a flexible array operator. Arrays can be defined in many ways, usually as:

[`export`] <`byte`, `word`> [label] [= < number, character, string, address, ... >]

For example:
```
predef myfunc
    
byte smallarray[4]
byte initbarray[] = 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
byte string[64] = "Initialized string"
word wlabel[]
word = 1000, 2000, 3000, 4000 ; Anonymous array
word funclist = @myfunc, $0000
```
Arrays can be uninitialized and reserve a size, as in `smallarray` above.  Initilized arrays without a size specifier in the definition will take up as much data as is present, as in `initbarray` above. Strings are special arrays that include a hidden length byte in the beginning (Pascal strings). When specified with a size, a minimum size is reserved for the string value. Labels can be defined as arrays without size or initializers; this can be useful when overlapping labels with other arrays or defining the actual array data as anonymous arrays in following lines as in `wlabel` and following lines. Addresses of other data (must be defined previously) or function definitions (pre-defined with predef), including imported references, can be initializers.

##### Type Overrides
Arrays are usually identified by the data type specifier, `byte` or `word` when the array is defined. However, this can be overridden with the type override specifiers: `:` and `.`. `:` overrides the type to be `word`, `.` overrides the type to be `byte`. An example of accessing a `word` array as `bytes`:
```
word myarray[] = $AABB, $CCDD, $EEFF
    
def prarray
    byte i
    for i = 0 to 5
        puti(myarray.[i])
    next
end
```
The override operator becomes more useful when multi-dimenstional arrays are used.

##### Multi-Dimensional Arrays
Multi-dimensional arrays are implemented as arrays of arrays, not as a single block of memory. This allows constructs such as:
```
;
; Hi-Res scanline addresses
;
word hgrscan[] = $2000,$2400,$2800,$2C00,$3000,$3400,$3800,$3C00
word           = $2080,$2480,$2880,$2C80,$3080,$3480,$3880,$3C80
```
...
```
def hgrfill(val)
	byte yscan, xscan

	for yscan = 0 to 191
		for xscan = 0 to 19
			hgrscan:[yscan][xscan] = val
		next
	next
end
```
Every array dimension except the last is a pointer to another array of pointers, thus the type is word. The last dimension is either `word` or `byte`, but cannot be specified with an array declaration, so the type override is used to identify the type of the final element. In the above example, the memory would be accessed as bytes with the following:
```
def hgrfill(val)
	byte yscan, xscan

	for yscan = 0 to 191
		for xscan = 0 to 39
			hgrscan.[yscan][xscan] = val
		next
	next
end
```
Notice how xscan goes to 39 instead of 19 in the byte accessed version.

#### Offsets (Structure Elements)
Structures are another fundamental construct when accessing in-common data. Using fixed element offsets from a given address means you only have to pass one address around to access the entire record. Offsets are specified with a constant expression following the type override specifier.
```
predef puti ; print an integer
byte myrec[]
word = 2
byte = "PLASMA"
    
puti(myrec:0) ; ID = 2
puti(myrec.2) ; Name length = 6 (Pascal string puts length byte first)
```
This contrived example shows how one can access offsets from a variable as either `byte`s or `word`s regardless of how they were defined. This operator becomes more powerful when combined with pointers, defined next.

#### Pointers
Pointers are values that represent addresses. In order to get the value pointed to by the address, one must 'dereference' the pointer. All data and code memory has a unique address, all 65536 of them (16 bits). In the Apple II, many addresses are actually connected to hardware instead of memory. Accessing these addresses can make thing happen in the Apple II, or read external inputs like the keyboard and joystick.

##### Pointer Dereferencing
Just as there are type override for arrays and offsets, there is a `byte` and `word` type override for pointers. Prepending a value with `^` dereferences a `byte`. Prepending a value with `*` dereferences a `word`. These are unary operators, so they won't be confused with the binary operators using the same symbol.

##### Addresses of Data/Code
Along with dereferencing a pointer, there is the question of getting the address of a variable. The `@` operator prepended to a variable name or a function definition name, will return the address of the variable/definition.

##### Function Pointers
One very powerful combination of operations is the function pointer. This involves getting the address of a function and saving it in a `word` variable. Then, the function can be called be dereferencing the variable as a function call invocation. PLASMA is smart enough to know what you mean when your code looks like this:
```
word funcptr
    
def addvals(a, b)
    return a + b
end
def subvals(a, b)
    return a - b
end
    
funcptr = @addvals
puti(funcptr(5, 2)) ; Outputs 7 
funcptr = @subvals
puti(funcptr(5, 2)) ; Outputs 3
```
These concepts can be combined with the structure offsets to create a function table that can be easily changed on the fly. Virtual functions in object oriented languages are implemented this way.
```
predef myinit, mynew, mydelete
    
export word myobject_class = @myinit, @mynew, @mydelete
; Rest of class data/code follows...
```
And an external module can call into this library (class) like:
```
import myclass
    const init   = 0
    const new    = 2
    const delete = 4
    word myobject_class
end
    
word an_obj ; an object pointer
    
myobject_class:init()
an_obj = myobject_class:new()
myobject_class:delete(an_obj)
```

## Function Definitions
Function definitions in PLASMA is what really seperates PLASMA from a low level language like assembly, or even a language like FORTH. 

### Expressions

### Control Flow
PLASMA implements most of the control flow that most higher level languages provide. It may do it in a slightly different way, though. One thing you won't find in PLASMA is GOTO - there are other ways around it.

#### RETURN

#### IF/ELSIF/ELSE/FIN

#### WHEN/IS/OTHERWISE/WEND

#### FOR/NEXT

#### WHILE/LOOP

#### REPEAT/UNTIL

## Dynamic Heap Memory Allocation
Memory allocation isn't technically part of the PLASMA specification, but it plays such an integral part of the PLASMA environment that is is covered here.

## Advanced Topics
There are some things about PLASMA that aren't necessary to know, but can add to it's effectiveness in a tight situation. Usually you can just code along, and the system will do a pretty reasonable job of carrying out your task. However, a little knowledge in the way to implement small assembly language routines or some coding practices just might be the ticket.

### Native Assembly Functions
Assembly code in PLASMA is implemented strictly as a pass-through to the assembler. No syntax checking, or checking at all, is made. All assembly routines *must* come after all data has been declared, and before any PLASMA function definitions.

### Code Optimizations
#### Return Values
PLASMA always returns a value from a function, even if you don't supply one. Probably the easiest optimization to make in PLASMA is to cascade a return value if you don't care about the value you return. This only works if the last thing you do before returning from your routine is calling another definition. You would go from:
```
def mydef
    ; do some stuff
    calldef(10) ; call some other def
end
```
PLASMA will effectively add a RETURN 0 to the end of your function, as well as add code to ignore the result of `calldef(10)`. As long as you don't care about the return value from `mydef`, you can save some code bytes with:
```
def mydef
    ; do some stuff
    return calldef(10) ; call some other def
end
```
