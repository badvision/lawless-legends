drawText
	stx textPointer+1
	sty textPointer+2
	stx preTextPointer+1
	sty preTextPointer+2

	lda #$FF
	sta textCursor
	sta outputCursor
	
	ldx #0 ;x is for pre-drawing calculation if each word will fit in currently drawn line
	stx ignoreNextCharacterFlag
	
;Check if the next word fits in current line
preDrawTextInit
	stx wordCursor

preDrawTextLoop
	ldy textCursor

preDrawTextLoop2
	iny
	inx
	
	jsr preTextPointer

	cmp #$FF
	bne +
		jmp drawWord
+
	cmp #$20
	bne +
		jmp drawWord
+
	cmp #13 ;ignore this character as this is the part of breakline: chr(13) and chr(10)
	bne +
		dex
		jmp preDrawTextLoop2
+
	cmp #10
	bne +
		ldx #0
		jmp drawWord
+

	cpx #16
	bcc ++ ;skip if word fits in line
		ldx #0
		stx wordCursor
		;Check if next character is the space character so we don't need then to break the line
		iny
		jsr preTextPointer
		cmp #$20
		beq +
			jsr breakLine
			jmp drawWord
		+
		inc ignoreNextCharacterFlag
		jmp drawWord
+

jmp preDrawTextLoop2

drawWord
	ldx wordCursor

drawWordLoop
	inc outputCursor
drawWordLoop2
	inc textCursor
	inx
	ldy textCursor
textPointer
	lda $1234,y
	cmp #$FF ;end of text
	beq drawTextDone

	cmp #13 ;ignore this character as this is the part of breakline: chr(13) and chr(10)
	bne +
		jmp drawWordLoop2
+
	cmp #10 ;next line
	bne +
		jsr eatNextCharacter
		ldx #0
		jsr breakLine
		jmp preDrawTextInit
+
	cmp #$20
	bne +
		jsr eatNextCharacter
		jmp preDrawTextInit
+
	
	sta letter+1

	ldy outputCursor

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
jmp drawWordLoop

preTextPointer
	lda $1234,y
rts

breakLine
		clc
		lda outputCursor
		lsr
		lsr
		lsr
		lsr
		asl
		asl
		asl
		asl
		adc #15
		sta outputCursor
rts

eatNextCharacter
	lda ignoreNextCharacterFlag
	beq +
		dec outputCursor
		dec ignoreNextCharacterFlag
+
rts

drawTextDone
rts

