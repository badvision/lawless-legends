;Draw tile
drawTile

	sta tile
	stx x
	sty y

	;Get a tile
	tay
	lda tilesBitmapTable_LO,y
	sta tileBitmapPointer_LO
	lda tilesBitmapTable_HI,y
	sta tileBitmapPointer_HI
	
	clc
	lda y
	tay
	lda multiplyBy9,y ;Multiply y by 9
	adc x
	tay


	lda mapBitmapTable_LO,y
	sta mapBitmapPointer_LO
	lda mapBitmapTable_HI,y
	sta mapBitmapPointer_HI

	lda mapBitmapTable2_LO,y
	sta mapBitmapPointer2_LO
	lda mapBitmapTable2_HI,y
	sta mapBitmapPointer2_HI

	lda screenVideoRamTable_LO,y
	sta screenVideoRamPointer_LO
	lda screenVideoRamTable_HI,y
	sta screenVideoRamPointer_HI

	lda colorRamTable_LO,y
	sta colorRamPointer_LO
	lda colorRamTable_HI,y
	sta colorRamPointer_HI
	
	ldy #16
-
	lda (tileBitmapPointer_LO),y
	sta (mapBitmapPointer_LO),y

	dey
	bne -

	ldx #16
	ldy #32
-
	lda (tileBitmapPointer_LO),y
	sta (mapBitmapPointer2_LO),y

	dey
	dex
	bne -

	;Draw Video RAM
	lda tile
	asl
	asl
	tax
	
	ldy #0

	lda TILES_VIDEO_RAM,x
	sta (screenVideoRamPointer_LO),y

	inx
	ldy #1
	
	lda TILES_VIDEO_RAM,x
	sta (screenVideoRamPointer_LO),y
	
	inx
	ldy #40
	
	lda TILES_VIDEO_RAM,x
	sta (screenVideoRamPointer_LO),y
	
	inx
	ldy #41

	lda TILES_VIDEO_RAM,x
	sta (screenVideoRamPointer_LO),y

	;Draw Color RAM
	dex
	dex
	dex
	
	ldy #0
	
	lda TILES_COLOR_RAM,x
	sta (colorRamPointer_LO),y

	inx
	ldy #1
	
	lda TILES_COLOR_RAM,x
	sta (colorRamPointer_LO),y
	
	inx
	ldy #40
	
	lda TILES_COLOR_RAM,x
	sta (colorRamPointer_LO),y
	
	inx
	ldy #41
	
	lda TILES_COLOR_RAM,x
	sta (colorRamPointer_LO),y
rts

