.SUFFIXES	=
PRORWTS = PRORWTS2\#4000
PROBOOT = PROBOOT\#800
PROBOOTHD = PROBOOTHD\#800

all: $(PRORWTS) $(PROBOOT) $(PROBOOTHD)

clean:
	-rm $(PRORWTS) $(PROBOOT) $(PROBOOTHD)

$(PRORWTS): prorwts2.s
	acme -o $(PRORWTS) prorwts2.s

$(PROBOOT): proboot.s
	acme -o $(PROBOOT) proboot.s

$(PROBOOTHD): proboothd.s
	acme -o $(PROBOOTHD) proboothd.s
