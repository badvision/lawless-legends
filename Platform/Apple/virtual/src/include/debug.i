; Debug macros
!macro prStr {
	jsr _writeStr
}

!macro prByte addr {
	jsr _prByte
	!word addr
}

!macro prSpace {
	jsr _prSpace
}

!macro prChr chr {
	jsr _writeStr
	!byte chr, 0
}

!macro prA {
	jsr _prA
	jsr _prSpace
}

!macro prX {
	jsr _prX
	jsr _prSpace
}

!macro prY {
	jsr _prY
	jsr _prSpace
}

!macro prXA {
	jsr _prX
	jsr _prA
	jsr _prSpace
}

!macro prAX {
	jsr _prA
	jsr _prX
	jsr _prSpace
}

!macro prXY {
	jsr _prX
	jsr _prY
	jsr _prSpace
}

!macro prYX {
	jsr _prY
	jsr _prX
	jsr _prSpace
}

!macro prWord addr {
	jsr _prWord
	!word addr
}

!macro crout {
	jsr _crout
}

!macro waitKey {
	jsr _waitKey
}

_getStackByte !zone {
	inc $101,x
	bne +
	inc $102,x
+	lda $101,x
	sta .ld+1
	lda $102,x
	sta .ld+2
.ld:   	lda $2000
	rts
}

; Support to print a string following the JSR, in high or low bit ASCII, 
; terminated by zero. If the string has a period "." it will be followed 
; automatically by a carriage return. Preserves all registers.
_writeStr: !zone {
	jsr iosave
	tsx
.loop:	jsr _getStackByte
	beq .done
	jsr cout
	cmp #$AE	; "."
	bne .loop
	jsr crout
	jmp .loop
.done:	jmp iorest
}

_prByte: !zone {
	jsr iosave
	ldy #0
	; fall through to _prShared...
}

_prShared: !zone {
	tsx
	jsr _getStackByte
	sta .ld+1
	jsr _getStackByte
	sta .ld+2
.ld:	lda $2000,y
	jsr prbyte
	dey
	bpl .ld
	+prSpace
	jmp iorest
}

_prSpace: !zone {
	php
	pha
	lda #$A0
	jsr cout
	pla
	plp
	rts
}

_prWord: !zone {
	jsr iosave
	ldy #1
	bne _prShared	; always taken
}

_prA: !zone {
	php
	pha
	jsr prbyte
	pla
	plp
	rts
}
	
_prX: !zone {
	php
	pha
	txa
	jsr prbyte
	pla
	plp
	rts
}

_prY: !zone {
	php
	pha
	tya
	jsr prbyte
	pla
	plp
	rts
}

_crout: !zone {
	php
	pha
	jsr crout
	pla
	plp
	rts
}

_waitKey: !zone {
	jsr iosave
	jsr rdkey
	jmp iorest
}