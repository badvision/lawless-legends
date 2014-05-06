.SUFFIXES	=
AFLAGS	= -o $@
LFLAGS	= -C default.cfg
PLVM    = plvm
PLASM   = plasm
INCS    = tokens.h symbols.h lex.h parse.h codegen.h
OBJS	= plasm.c parse.o lex.o codegen.o
#
# Image filetypes for Virtual ][
#
PLATYPE	= .\$$ED
BINTYPE	= .BIN
SYSTYPE	= .SYS
TXTTYPE	= .TXT
#
# Image filetypes for CiderPress
#
#PLATYPE	= \#ed0000
#BINTYPE	= \#060000
#SYSTYPE	= \#ff0000
#TXTTYPE	= \#040000

all: $(PLASM) $(PLVM) TESTLIB

clean:
	-rm *.o *~ *.a *.BIN TESTLIB $(PLASM) $(PLVM)

$(PLASM): $(OBJS) $(INCS)
	cc $(OBJS) -o $(PLASM)

$(PLVM): plvm.c
	cc plvm.c -o $(PLVM)

TESTLIB: testlib.pla $(PLVM) $(PLASM)
	./$(PLASM) -AM < testlib.pla > testlib.a
	acme --setpc 4096 -o TESTLIB testlib.a

test: test.pla TESTLIB $(PLVM) $(PLASM)
	./$(PLASM) -AM < test.pla > test.a
	acme --setpc 4096 -o TEST.BIN test.a
	./$(PLVM) TEST.BIN MAIN

debug: test.pla TESTLIB $(PLVM) $(PLASM)
	./$(PLASM) -AM < test.pla > test.a
	acme --setpc 4096 -o TEST.BIN test.a
	./$(PLVM) -s TEST.BIN MAIN
