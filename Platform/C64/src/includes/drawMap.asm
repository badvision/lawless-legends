drawMap

	lda Raster_Indicator
	beq +
		lda #2
		sta $d020
	+
	
	clc
	lda #<MAP
	adc mapPosX
	sta mapPointer_LO
	lda #>MAP
	adc mapPosX+1
	sta mapPointer_HI

	;Increment map pointer by a multiplication of row length if map Y position more than 0
	lda mapPosY+1
	bne setCalculateDiffer
	lda mapPosY
	beq +

setCalculateDiffer
	;Set differ
	lda mapPosY
	sta calculateDiffer_LO
	lda mapPosY+1
	sta calculateDiffer_HI

calculateY

	;Calculate row on the map (length of rows vary for a particular map so it cannot be pre-calculated in table)
	clc
	lda mapPointer_LO
	adc mapSizeXAdc
	sta mapPointer_LO

	lda mapPointer_HI
	adc mapSizeXAdc+1
	sta mapPointer_HI

	lda calculateDiffer_LO
	sec
	sbc #1
	sta calculateDiffer_LO
	lda calculateDiffer_HI
	sbc #0
	sta calculateDiffer_HI

	;Is it everything?
calculateDiffer
	lda calculateDiffer_LO
	bne calculateY
	lda calculateDiffer_HI
	bne calculateY

	
+

	;Remember offset of a map, used for checking terrain type
	clc
	lda mapPointer_LO
	sta mapOffsetPointer_LO
	lda mapPointer_HI
	sta mapOffsetPointer_HI

	.for row=0,row<=8,row=row+1
		.for tileN=0,tileN<=8,tileN=tileN+1
			ldy #0 + tileN
			lda (mapPointer_LO),y
			ldx #0 + tileN
			ldy #0 + row
			jsr drawTile
		.next

		clc
		lda mapPointer_LO
		adc mapSizeXAdc
		sta mapPointer_LO
		
		lda mapPointer_HI
		adc mapSizeXAdc+1
		sta mapPointer_HI

	.next

	lda Raster_Indicator
	beq +
		lda #0
		sta $d020
	+
	
rts

