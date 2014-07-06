;
; Font Engine
;
; (c) 2013, Brutal Deluxe Software
;

; Equates
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
wndleft		=	$70		; left edge of the window
wndwdth		=	wndleft+1	; width of text window
wndtop		=	wndwdth+1	; top of text window
wndbtm		=	wndtop+1	; bottom+1 of text window

cursh		=	wndbtm+1	; Cursor H-pos 0-39
cursv		=	cursh+1		; Cursor V-pos 0-23 => 0-191


;---------------------------
;
; STR: draws a Pascal string
; CSTR: draws a C string (null terminated)
; CHAR: draws a single char
;

;---------------------------

fontEngine	=	$BA00
printSTR	=	fontEngine
printSSTR	=	printSTR+3
printCSTR	=	printSSTR+3
printSCSTR	=	printCSTR+3
printCHAR	=	printSCSTR+3
tabXY		=	printCHAR+3
setFONT		=	tabXY+3
displayMODE	=	setFONT+3
drawMODE	=	displayMODE+3
scrollWINDOW	=	drawMODE+3
clearWINDOW	=	scrollWINDOW+3
