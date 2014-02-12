;
; Equates
;

pHGR1	        =	$20
pHGR2	        =	$40
pHGR3	        =	$60

pNORMAL	        =	$00
pINVERSE	=	$FF

charNULL	=	$00
charRET		=	$0d
charSPACE	=	$20
charCMD		=	$80

;
; Zero page usage
;

WNDLFT		=	$20		; left edge of the window
WNDWDTH		=	WNDLFT+1	; width of text window
WNDTOP		=	WNDWDTH+1	; top of text window
WNDBTM		=	WNDTOP+1	; bottom+1 of text window

CH		=	WNDBTM+1	; Cursor H-pos 0-39
CV		=	CH+1		; Cursor V-pos 0-23 => 0-191

BASL		=	$28		; Text base address
BASH		=	BASL+1
GBASL		=	BASH+1		; Second text base address
GBASH		=	GBASL+1

dpFROM		=	BASL
dpTO		=	dpFROM+2
dpTO2		=	dpTO+2

INVFLG		=	$32	; Inverse flag (FF: normal, 7F: flash, 3F: inverse)

dpSTR		=	$fe	; Pointer to source string

CSWL		=	$36	; Char output hook
CSWH		=	CSWL+1

HPAG		=	$E6	; $20 for HGR1, $40 for HGR2

;
; Monitor routines
;

HGR2		=	$f3d8
HGR		=	$f3e2
