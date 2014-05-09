.SUFFIXES	=
AFLAGS	= -o $@
LFLAGS	= -C default.cfg
PLVM    = plvm
PLVM02	= PLVM02.SYS
CMD     = CMD.SYS
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

all: $(PLASM) $(PLVM) $(PLVM02) $(CMD) TESTLIB

clean:
	-rm *.o *~ *.a *.SYM *.SYS *.BIN TESTLIB $(PLASM) $(PLVM)

$(PLASM): $(OBJS) $(INCS)
	cc $(OBJS) -o $(PLASM)

$(PLVM): plvm.c
	cc plvm.c -o $(PLVM)

cmdexec.a: cmdexec.pla $(PLASM)
	./$(PLASM) -A < cmdexec.pla > cmdexec.a

$(PLVM02): plvm02.s cmdexec.a
	acme -o $(PLVM02) -l PLVM02.SYM plvm02.s

$(CMD): cmd.pla $(PLVM) $(PLASM)
	./$(PLASM) -A < cmd.pla > cmd.a
	acme --setpc 8192 -o $(CMD) cmd.a

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
