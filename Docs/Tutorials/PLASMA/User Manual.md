# PLASMA Programming Manual
## ( Proto Language AsSeMbler for Apple)

## Introduction
PLASMA is a medium level programming language for the 8 bit 6502 processor. Historically, there were simple languages developed in the early history of computers that improved on the tedium of assembly language programming while still being low level enough for system coding. Languages like B, FORTH, and PLASMA fall into this category. The following will take you through the process of writing, building and running a PLASMA module.

### Obligatory 'Hello world'
To start things off, here is the standard introductory program:

```
    import STDLIB
        predef puts
    end
    
    byte hello[] = "Hello, world.\n"
    
    puts(@hello)
    done
```

Three tools are required to build and run this program: **plasm**, **acme**, and **plvm**.  The PLASMA compiler, **plasm**, will convert the PLASMA source code (usually with an extension on .pla) into an assembly language source file.  **acme**, the portable 6502 assembler will convert the assembly source into a binary ready for loading. To execute the module, the PLASMA portable VM, **plvm**, can load and interpret the bytecode. The same binary can be loaded onto the target platform and run there with the appropriate VM. On Linux/Unix from the lawless-legends/PLASMA/src, the steps would be entered as:

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

A couple of things to note: **plasm** only accepts input from stdin and output to stdout. To build acme compatible module source, tha '-AM' flags must be passed in. The **acme** assembler needs the --setpc 4096 to assemble the module at the proper address, and the -o option sets the output file.


