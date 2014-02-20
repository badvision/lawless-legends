;
; Code
;
; (c) 2013, Brutal Deluxe Software
;

* 		= 	$1b00		; so it ends just before $2000 

		!source	"equates.i"

;---------------------------
;
; STR: draws a Pascal string
; CSTR: draws a C string (null terminated)
; CHAR: draws a single char
;

;---------------------------

		jmp	printSTR	; print a Pascal string
		jmp	printSTACK	; print a Pascal string by the stack
		jmp	printCSTR	; print a C string
		jmp	printCSTACK	; print a C string by the stack
printCHAR	jmp	drawCHAR	; pointer to 1/2 HGR printing
		jmp	tabXY		; sets the X/Y coordinates
		jmp	setFONT		; sets the font pointer
		jmp	displayMODE	; sets the display mode
		jmp	drawMODE	; sets inverse/normal writing mode
scrollWIN	jmp	scrollWINDOW	; scrolls the 'text' window

;---------------------------

; printSTR
;  Outputs a Pascal string
; Input:
;  X: high pointer to Pascal string
;  Y: low pointer to Pascal string

printSTR 	!zone

		sty	dpSTR
		stx	dpSTR+1

printSSTR:				; Entry point for stack

printSTR1	jsr	getCHAR		; Get length of string
		bne	printSTR2
		rts
printSTR2	sta	strLENGTH	; Save it for later

.lp		jsr	getCHAR		; Get a character
		jsr	printCHAR	; Print it out

		lda	strLENGTH	; Next character
		bne	.lp		; Until end of string
		rts

;---------------------------

; printCSTR
;  Outputs a C string (null terminated)
; Input:
;  X: high pointer to C string
;  Y: low pointer to C string

printCSTR 	!zone

		sty	dpSTR
		stx	dpSTR+1

printSCSTR:				; Entry point for stack

.lp		jsr	getCHAR		; Get a character

		cmp	#charNULL
		beq	printCSTR1	; end of string...

		jsr	printCHAR	; Print it out

		clc			; force BRanch Always
		bcc	.lp		; if 65c02, put a BRA instead

printCSTR1	rts

;---------------------------

; printSTACK
;  Displays a Pascal string through the stack

printSTACK 	!zone

		pla			; get the stack pointer
		sta	dpSTR
		pla
		sta	dpSTR+1

		jsr	getCHAR		; Move the pointer

		jsr	printSSTR	; call entry point for Pascal string

printSTACK1:				; Share the same return code

		dec	dpSTR		; Decrement string pointer
		lda	dpSTR
		cmp	#$ff
		bne	printSTACK2
		dec	dpSTR+1

printSTACK2	lda	dpSTR+1		; Push it to the stack
		pha
		lda	dpSTR
		pha
		rts			; return

;---------------------------

; printCSTACK
;  Displays a C string through the stack

printCSTACK: 	!zone

		pla		; get the stack pointer
		sta	dpSTR
		pla
		sta	dpSTR+1
	
		jsr	getCHAR	; Move the pointer
	
		jsr	printSCSTR	; call entry point for C string
	
		jmp	printSTACK1	; return code

;---------------------------
; Sub-routines
;---------------------------

; WNDLFT = $20 ; left edge of the window
; WNDWDTH = WNDLFT+1 ; width of text window
; WNDTOP = WNDWDTH+1 ; top of text window
; WNDBTM = WNDTOP+1 ; bottom+1 of text window

; calcNEXT
;  Calculates the next display position

calcNEXT: 	!zone			; Calculates the next char position

		inc	CH		; next column

		lda	CH		; did we reach the rightmost one?
		cmp	WNDWDTH
		bcc	calcNEXT9
		beq	calcNEXT9

nextLINE:

		lda	WNDLFT		; yes, next line
		sta	CH

;---

		inc	CV

		lda	CV		; did we reach the end of the area?
		cmp	WNDBTM
		bcc	calcNEXT9
		beq	calcNEXT9

		dec	CV

		jsr	scrollWIN	; yes, scroll

calcNEXT9	rts

;---------------------------

; scrollWINDOW
;  Scroll one HGR window

scrollWINDOW: !zone

; WNDLFT = $20 ; left edge of the window
; WNDWDTH = WNDLFT+1 ; width of text window
; WNDTOP = WNDWDTH+1 ; top of text window
; WNDBTM = WNDTOP+1 ; bottom+1 of text window

		lda	WNDBTM		; 23 => 24 * 8 = 192
		clc
		adc	#1
		asl
		asl
		asl			; *8
		sta	maxLINE		; the max line
	
		lda	#8		; loop

scrollIT1:

		pha
	
		lda	WNDTOP
		asl
		asl
		asl			; *8
		tax

scrollIT5	lda	tblHGRl,x
		sta	dpTO
		lda	tblHGRh,x
		ora	HPAG
		sta	dpTO+1
	
		inx
		lda	tblHGRl,x
		sta	dpFROM
		lda	tblHGRh,x
		ora	HPAG
		sta	dpFROM+1
	
		ldy	WNDLFT		; Copy a line
.lp1		lda	(dpFROM),y
		sta	(dpTO),y
		iny
		cpy	WNDWDTH
		bcc	.lp1
		beq	.lp1
	
		cpx	maxLINE
		bne	scrollIT5
	
		ldy	WNDLFT		; Put a blank line
		lda	#0
.lp2		sta	(dpTO),y
		iny
		cpy	WNDWDTH
		bcc	.lp2
		beq	.lp2
	
		pla			; next loop
		sec
		sbc	#1
		bne	scrollIT1
		rts

;---------------------------

; scroll2WINDOW
;  Scroll two HGR window

scroll2WINDOW: !zone

; WNDLFT = $20 ; left edge of the window
; WNDWDTH = WNDLFT+1 ; width of text window
; WNDTOP = WNDWDTH+1 ; top of text window
; WNDBTM = WNDTOP+1 ; bottom+1 of text window

		lda	WNDBTM		; 23 => 24 * 8 = 192
		clc
		adc	#1
		asl
		asl
		asl			; *8
		sta	maxLINE		; the max line
	
		lda	#8		; loop

scroll2IT1	=	*

		pha
	
		lda	WNDTOP
		asl
		asl
		asl			; *8
		tax

scroll2IT5	lda	tblHGRl,x
		sta	dpTO
		sta	dpTO2
		lda	tblHGRh,x
		ora	HPAG
		sta	dpTO+1
		clc
		adc	#pHGR1
		sta	dpTO2+1
	
		inx
		lda	tblHGRl,x
		sta	dpFROM
		lda	tblHGRh,x
		ora	HPAG
		sta	dpFROM+1
	
		ldy	WNDLFT		; Copy a line
.lp1		lda	(dpFROM),y
		sta	(dpTO),y
		sta	(dpTO2),y
		iny
		cpy	WNDWDTH
		bcc	.lp1
		beq	.lp1
	
		cpx	maxLINE
		bne	scroll2IT5
	
		ldy	WNDLFT		; Put a blank line
		lda	#0
.lp2		sta	(dpTO),y
		sta	(dpTO2),y
		iny
		cpy	WNDWDTH
		bcc	.lp2
		beq	.lp2
	
		pla			; next loop
		sec
		sbc	#1
		bne	scroll2IT1
		rts

;---------------------------

; getCHAR
;  Gets the character
; Input:
;  dpSTR: pointer to the string
; Output:
;  A: character

getCHAR:	!zone			; Gets the character

		ldy	#0
		lda	(dpSTR),y
	
		dec	strLENGTH
	
		inc	dpSTR
		bne	getCHAR1
		inc	dpSTR+1
getCHAR1	rts

;---------------------------

; calcPOS
;  Calculates the HGR coordinates
; Input:
;  Uses CH and CV
; Output:
;  X: first HGR line of char
;  Y: last HGR line of char

calcPOS:	!zone	; Calculates the X/Y coordinate of a char

		lda	CV	; from a text row
		asl
		asl
		asl		; *8
		tax		; to a HGR one
	
		clc
		adc	#8	; height of a character
		tay		; max line
		sty	theY
		rts

;----------
; Calculate the HGR coordinates

calcPOS1	!zone

		lda	tblHGRl,x
		sta	BASL		; HGR
		sta	GBASL		; double HGR
		lda	tblHGRh,x
		ora	HPAG
		sta	BASH
		clc
		adc	#pHGR1		; for printing
		sta	GBASH		; on both HGR page
		rts

;---------------------------

; tabXY
;  Sets the X/Y coordinates
; Input:
;  X: X coordinate
;  Y: Y coordinate

tabXY		stx	CH
		sty	CV
		rts

;---------------------------

; setFONT
;  Sets the font pointer
; Input:
;  X: high pointer of font data
;  Y: low pointer of font data

setFONT		!zone			; Sets the font pointer

		sty	fontPTR
		stx	fontPTR+1
		rts

;---------------------------

; displayMODE
;  Sets writing to 1 or 2 HGR page(s)
; Input:
;  A: #$20 for HGR1, #$40 for HGR2, #$60 for both pages

displayMODE	!zone			; Sets writing to 1 or 2 HGR page(s)

; Prepare scroll window routine

		ldx	#>scrollWINDOW
		stx	scrollWIN+2
		ldy	#<scrollWINDOW
		sty	scrollWIN+1
	
		ldx	#>drawCHAR
		ldy	#<drawCHAR
	
		cmp	#pHGR3		; Do we ask for HGR1+HGR2 printing?
		bne	dispMODE1

; We want to display/scroll on both pages

		ldx	#>scroll2WINDOW
		stx	scrollWIN+2
		ldy	#<scroll2WINDOW
		sty	scrollWIN+1
	
		lda	#pHGR1
		ldx	#>draw2CHAR
		ldy	#<draw2CHAR

dispMODE1	sta	HPAG		; Page to draw a char
		sty	printCHAR+1
		stx	printCHAR+2
		rts

;---------------------------

; drawMODE
;  Sets drawing method
; Input:
;  A: #$00 for normal, #$FF for inverse

drawMODE	!zone			; Sets drawing method

		sta	INVFLG
		rts

;---------------------------

; handleCHAR
;  Handles command chars
; Output:
;  Y: low pointer to font data
;  X: high pointer to font data

handleCHAR	!zone			; Handles command chars

		cmp	#charCMD
		bcs	handleCHAR2
	
		cmp	#charRET	; any Return to handle?
		bne	handleCHAR1
	
		jsr	nextLINE	; Yes!
		sec
		rts

handleCHAR1	and	#%01111111
		sec			; subtract
		sbc	#charSPACE	; #$20 space

; Prepare a *8 multiplication

		sta	fontIDX
	
		lda	#0
		sta	fontIDX+1
	
		asl	fontIDX	; *2
		rol	fontIDX+1
		asl	fontIDX	; *4
		rol	fontIDX+1
		asl	fontIDX	; *8
		rol	fontIDX+1

; Now, calculate the char data address

		lda	fontIDX
		clc
		adc	fontPTR
		tay
	
		lda	fontIDX+1
		adc	fontPTR+1
		tax

; The old routine with a table (faster but more room)
; tax  ; calculate the pointer
; lda fontINDEXl,x
; clc  ; to the font data
; adc fontPTR
; tay
;
; lda fontINDEXh,x
; adc fontPTR+1
; tax

		clc			; character to print
		rts

;--- We have a command code
;
; #$80: tabXY
; #$81: setFONT
; #$82: displayMODE
; #$83: drawMODE
; #$84: call routine

handleCHAR2	!zone

		and	#%00000111	; Must change if more commands
		asl
		tax
		lda	tblCOMMANDS,x
		sta	handleCHAR3+1
		lda	tblCOMMANDS+1,x
		sta	handleCHAR3+2

handleCHAR3	jsr	$bdbd
		sec
		rts

tblCOMMANDS	!word	doCMD0,doCMD1,doCMD2,doCMD3
		!word	doCMD4,doCMD0,doCMD0,doCMD0

;--- tabXY

doCMD0		jsr	getCHAR
		tax
		jsr	getCHAR
		tay
		jsr	tabXY
		rts

;--- setFONT

doCMD1		jsr	getCHAR
		sta	fontPTR
		jsr	getCHAR
		sta	fontPTR+1
		rts

;--- displayMODE

doCMD2		jsr	getCHAR		; HGR page
		jsr	displayMODE
		rts

;--- drawMODE

doCMD3		jsr	getCHAR		; FF: inverse, 00:normal
		sta	INVFLG
		rts

;--- execute code (useful for protection)

doCMD4		jsr	getCHAR
		sta	doCMD45+1
		jsr	getCHAR
		sta	doCMD45+2
doCMD45		jmp	$bdbd

;---------------------------
; Graphic code
;---------------------------

; drawCHAR
;  Draws a character on 1 HGR page
; Input:
;  A: the character to draw

drawCHAR	!zone			; Draws a char on HGR

		jsr	handleCHAR	; Sets the font data pointer
		bcc	drawCHAR1
		rts

drawCHAR1	sty	drawCHAR2+1
		stx	drawCHAR2+2
	
		jsr	calcPOS		; calculate min/max of lines
	
		ldy	CH		; get the Y

.lp		jsr	calcPOS1	; calculate HGR coordinates

drawCHAR2	lda	$bdbd		; pointer to font data
		eor	INVFLG
		sta	(BASL),y	; output on HGR screen
	
		inc	drawCHAR2+1
		inx			; loop until 8 lines
		cpx	theY
		bne	.lp
	
		jsr	calcNEXT	; Prepare next position
	
		rts

;---------------------------

; draw2CHAR
;  Draws a character on 2 HGR pages
; Input:
;  A: the character to draw

draw2CHAR	!zone			; Draws one char on 2 pages

		jsr	handleCHAR	; Sets the font data pointer
		bcc	draw2CHAR1
		rts

draw2CHAR1	sty	draw2CHAR2+1
		stx	draw2CHAR2+2
	
		jsr	calcPOS		; calculate min/max of lines
	
		ldy	CH		; get the Y

.lp		jsr	calcPOS1	; calculate HGR coordinates

draw2CHAR2	lda	$bdbd		; pointer to font data
		eor	INVFLG
		sta	(BASL),y	; output on HGR screen
		sta	(GBASL),y	; ditto
	
		inc	draw2CHAR2+1
	
		inx			; loop until 8 lines
		cpx	theY
		bne	.lp
	
		jsr	calcNEXT	; Prepare next position
		rts

;---------------------------
; Some data
;---------------------------

theY		!fill	1		; save Y coordinate
maxLINE		!fill	1		; max line number for scrolling

fontIDX		!fill	2		; index to char data
fontPTR		!fill	2		; font data pointer

strLENGTH	!fill	1		; length of string

;---------------------------
; End of the code...
;---------------------------

		!source	"tables.i"
