.SUFFIXES	=
AFLAGS	= -o $@
LFLAGS	= -C default.cfg
PLVM    = plvm
PLVM02	= PLVM02.SYSTEM.sys#2000
CMD     = CMD\#FF2000
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

all: $(PLASM) $(PLVM) $(PLVM02) $(CMD)

clean:
	-rm *.o *~ *.a *FE1000 $(PLVM02) $(CMD) $(PLASM) $(PLVM)

$(PLASM): $(OBJS) $(INCS)
	cc $(OBJS) -o $(PLASM)

$(PLVM): plvm.c
	cc plvm.c -o $(PLVM)

$(PLVM02): plvm02.s
	acme -o $(PLVM02) plvm02.s

$(CMD): cmd.pla cmdstub.s $(PLVM) $(PLASM)
	./$(PLASM) -A < cmd.pla > cmd.a
	acme --setpc 8192 -o $(CMD) cmdstub.s

TESTLIB\#FE1000: testlib.pla $(PLVM) $(PLASM)
	./$(PLASM) -AM < testlib.pla > testlib.a
	acme --setpc 4094 -o TESTLIB\#FE1000 testlib.a

test: test.pla TESTLIB\#FE1000 $(PLVM) $(PLASM)
	./$(PLASM) -AM < test.pla > test.a
	acme --setpc 4094 -o TEST\#FE1000 test.a
	./$(PLVM) TEST

debug: test.pla TESTLIB $(PLVM) $(PLASM)
	./$(PLASM) -AM < test.pla > test.a
	acme --setpc 4094 -o TEST\#FE1000 test.a
	./$(PLVM) -s TEST MAIN

hello: hello.pla $(PLVM) $(PLASM)
	./$(PLASM) -AM < hello.pla > hello.a
	acme --setpc 4094 -o HELLO\#FE1000 hello.a
	./$(PLVM) HELLO
