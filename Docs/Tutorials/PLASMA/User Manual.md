# PLASMA Programming User Manual
## ( Proto Language AsSeMbler for Apple)

## Introduction
PLASMA is a medium level programming language targetting the 8 bit 6502 processor. Historically, there were simple languages developed in the early history of computers that improved on the tedium of assembly language programming while still being low level enough for system coding. Languages like B, FORTH, and PLASMA fall into this category. The following will take you through the process of writing, building and running a PLASMA module.

### Obligatory 'Hello World'
To start things off, here is the standard introductory program:

```
    import STDLIB
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
    import STDLIB
        const reshgr1 = $0004
        predef putc, puts, getc, gets, cls, gotoxy
    end

    import TESTLIB
        byte testdata, teststring
        word testarray
        predef puti
    end
```

The `predef` pre-defines functions that can be called throughout the module.  The data declarations, `byte` and `word` will refer to data in those modules. `const` can appear in an `import` block, although not required. It does keep values associated with the imported module in a well-contained block for readability. Case is not significant for either the module name nor the pre-defined function/data labels. They are all converted to uppercase with 16 characters significant when the loader resolves them.

#### Constant Declarations
Constants help with the readability of source code where hard-coded numbers might not be very descriptive.

```
    const MACHID  = $BF98
    const speaker = $C030
    const bufflen = 2048
```

These constants can be used in expressions just like a variable name.

#### Predefined Functions
Sometimes a function needs to be referenced before it is defined. The `predef` declaration reserves the label for a function. The 'import' declaration block also uses the `predef` declaration to reserve an external function. Outside of an `import` block, `predef` will only predefine a function that must be declared later in the source file, otherwise an error will occur.

```
    predef exec_file, mydef
```

#### Global Data & Variable Declarations
One of the most powerful features in PLASMA is the flexible data declarations. 

#### Native Functions
An advanced feature of PLASMA is the ability to write functions in native assembly language. This is a very advanced topic that is covered more in-depth in the Advanced Topics section.

#### Function Definitions
Function definitions **must** come after all other declarations. Once a function definition is written, no other globale declarations are allowed.

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

## Stacks
The basic architecture of PLASMA relies on different stack based data structures. The stacks aren't directly manipulated from PLASMA, but almost every PLASMA operation involves one or more of the stacks. A stack architecture is a very flexible and convenient way to manage an interpreted language, even if it isn't the highest performance.

### Call Stack
The call stack, where function return addresses are saved, is implemented using the hardware call stack of the CPU. This makes for a fast and efficient implementation of function call/return.

### Local Frame Stack
Any dunctiona definition that involves parameters or local variables builds a local frame to contain the variables. Often called automatic variables, they only persist during the lifetime of the function. They are a very powerful tool when implementing recursive algorithms. PLASMA puts a limitation of 254 bytes for the size of the frame, due to the nature of the 6502 CPU. With careful planning, this shouldn't be too constraining.

### Evaluation Stack
All temporary values are loaded and manipulated on the evaluation stack. This is a small (16 element) stack implemeted in high performance memory/registers of the host CPU. Parameters to functions are passed on the evaluation stack, then moved to local variables for named reference inside the funtion.

## Data Types
PLASMA only really defines two types: `byte`, `word`. All operations take place on word sized quantities, with the exception of loads and stores to byte sized addresses. The interpretation of a value can be an interger, or an address. There are a nuber of operators to identify how a value is to be interpreted.

### Decimal and Hexadecimal Numbers

### Character and String Literals

### Bytes
Bytes are unsigned, 8 bit values, stored at an address.  Bytes cannot be manipulated as bytes, but are promoted to words as soon as they are loaded ontp the evaluation stack. When stored to a byte addres, the low order byte of a word is used.

### Words

### Addresses

#### Arrays

#### Offsets

#### Pointers

## Function Definitions

### Expressions

### Control Flow

## Advanced Topics
