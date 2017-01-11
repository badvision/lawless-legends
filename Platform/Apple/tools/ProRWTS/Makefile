.SUFFIXES	=
PRORWTS = PRORWTS2\#4000
PROBOOT = PROBOOT\#800

all: $(PRORWTS) $(PROBOOT)

clean:
	-rm $(PRORWTS) $(PROBOOT)

$(PRORWTS): prorwts2.s
	acme -o $(PRORWTS) prorwts2.s

$(PROBOOT): proboot.s
	acme -o $(PROBOOT) proboot.s
