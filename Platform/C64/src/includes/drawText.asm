drawText
	
	stx textPointer+1
	sty textPointer+2

	lda #$FF
	sta textCursor

drawTextLoop
	inc textCursor
	ldy textCursor
textPointer
	lda $1234,y
	cmp #$FF
	beq drawTextDone
	sta letter+1

	ldy textCursor

	lda textPortTable_LO,y
	sta textPortPointer+1
	lda textPortTable_HI,y
	sta textPortPointer+2
	
	lda textPortColorTable_LO,y
	sta textPortColor+1
	lda textPortColorTable_HI,y
	sta textPortColor+2

;Draw a letter
letter
	ldy #0
	lda fontsTable_LO,y
	sta fontPointer+1
	lda fontsTable_HI,y
	sta fontPointer+2

	ldy #8
-

;Switch colour on for this letter
lda #1
textPortColor
	sta $1234

fontPointer
	lda $1234,y
textPortPointer
	sta $1234,y
	dey
	bne -

jmp drawTextLoop

drawTextDone

rts


