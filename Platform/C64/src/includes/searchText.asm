
searchText

	;Clear old text
	ldy #15
	lda #0
-
	.for step=0, step<=15, step=step+1
		sta TEXTPORT_SCREEN_OFFSET+SCREEN_COLOR_RAM + step*40 - 1,y
	.next
	dey
	bne -
	
	;Search for new one
	clc
	lda mapPosX
	adc posX
	sta locationX
	
	lda mapPosX+1
	adc #0
	sta locationX+1

	clc
	lda mapPosY
	adc posY
	sta locationY
	
	lda mapPosY+1
	adc #0
	sta locationY+1

	;Get how many text places are available on map in general so we can iterate through them
	ldx TEXT_HEADER

	lda #<TEXT_POINTERS
	sta textPointerData_LO
	lda #>TEXT_POINTERS
	sta textPointerData_HI

-
	ldy #0
	lda (textPointerData_LO),y
	cmp locationX
	bne +
	
	ldy #1
	lda (textPointerData_LO),y
	cmp locationX+1
	bne +
	
	ldy #2
	lda (textPointerData_LO),y
	cmp locationY
	bne +
	
	ldy #3
	lda (textPointerData_LO),y
	cmp locationY+1
	bne +
	
	ldy #4
	lda (textPointerData_LO),y
	tax
	
	ldy #5
	lda (textPointerData_LO),y
	tay

	jsr drawText
	rts

+
	;Text not match location, search for the next one
	clc
	lda textPointerData_LO
	adc #6
	sta textPointerData_LO
	
	lda textPointerData_HI
	adc #0
	sta textPointerData_HI

	dex
	bne -

rts
