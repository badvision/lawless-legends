*
* How to use the code
*
* (c) 2013, Brutal Deluxe Software
*

	org	$1000
	lst	off

	mx	%11

	use	LL.Equates

*---------------------------
*
* STR: draws a Pascal string
* CSTR: draws a C string (null terminated)
* CHAR: draws a single char
*

*---------------------------

stringCODE	=	$6000
printSTR	=	stringCODE
printSSTR	=	printSTR+3
printCSTR	=	printSSTR+3
printSCSTR	=	printCSTR+3
printCHAR	=	printSCSTR+3
tabXY	=	printCHAR+3
setFONT	=	tabXY+3
displayMODE	=	setFONT+3
drawMODE	=	displayMODE+3
scrollWINDOW	=	drawMODE+3

fontDATA	=	$7000

*---------------------------
* We assume the following:
*
* Code at $6000
* Font at $7000

	sta	$c050
	sta	$c052
	sta	$c054
	sta	$C057

	ldx	#>fontDATA	; set font data
	ldy	#fontDATA
	jsr	setFONT

*--- Set our frame

	lda	#0	; (0,0) TO (39,23)
	sta	WNDLFT
	sta	CH
	lda	#39
	sta	WNDWDTH
	lda	#0
	sta	WNDTOP
	sta	CV
	lda	#23
	sta	WNDBTM

*---

	lda	#pHGR1	; HGR1 page active
	jsr	displayMODE

	ldx	#2
	ldy	#1
	jsr	tabXY

	lda	#pNORMAL
	jsr	drawMODE

	ldx	#>string1
	ldy	#string1
	jsr	printSTR

*---

	lda	#2	; (2,4) TO (19,20)
	sta	WNDLFT
	sta	CH
	lda	#19
	sta	WNDWDTH
	lda	#4
	sta	WNDTOP
	sta	CV
	lda	#20
	sta	WNDBTM

	ldx	#2
	ldy	#10
	jsr	tabXY

	lda	#pINVERSE
	jsr	drawMODE

	jsr	printSSTR
	str	'Please press a key...'


*---

	jsr	$fd0c

	lda	#22	; (28,4) TO (38,20)
	sta	WNDLFT
	sta	CH
	lda	#37
	sta	WNDWDTH
	lda	#6
	sta	WNDTOP
	sta	CV
	lda	#16
	sta	WNDBTM

	lda	#pNORMAL
	jsr	drawMODE

	lda	#pHGR3	; Both pages active
	jsr	displayMODE

	ldx	#>string3
	ldy	#string3
	jsr	printCSTR

*-

	jsr	$fd0c
	sta	$c055
	jsr	$fd0c

	sta	$c054
	sta	$c056
	sta	$c051
	rts

*----------------------------

string1	str	'What time is it?'

string3	asc	'This is printed on both pages.'0d0d
	asc	'A very long text in both '83ff'inverse'8300
	asc	' and normal fontface.'0d
	asc	'This text will be long and will let the window scroll...'0d
	asc	'Yes, I promise it will scroll a lot... and a lot..'0d
	asc	83FF'Antoine'830000
