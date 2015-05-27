
Top_Frame_IRQ
	pha        ;store register A in stack
	txa
	pha        ;store register X in stack
	tya
	pha
	
	inc $d019

	lda Raster_Indicator
	beq +
		lda #2
		sta $d020
+

	lda movementBuffer
	beq calculatePlayerPosition
		dec movementBuffer
		jmp moveFinished

calculatePlayerPosition ;calculate position of player for further operations like checkTerrain procedure
	clc
	lda mapOffsetPointer_LO
	adc posX
	sta playerPosition_LO
	lda mapOffsetPointer_HI
	adc #0
	sta playerPosition_HI

	ldy posY
	beq +
-
	clc
	lda playerPosition_LO
	adc mapSizeXAdc
	sta playerPosition_LO

	lda playerPosition_HI
	adc mapSizeXAdc+1
	sta playerPosition_HI

	dey
	bne -
+
		
checkIsUp
	lda #%00000001 ;when joystick up
	bit $dc00
	beq +
	lda #%11111101 ;when key "W" hit
	sta $dc00
	lda $dc01
	cmp #%11111101
	bne checkIsLeft

+
	;Joystick Up
	jsr checkTerrainUp
	bne +
		jmp moveFinished
+
	lda MOVEMENT_DELAY
	sta movementBuffer
	
	lda mapPosY
	cmp #1
	bcc ++
	
	lda posY
	cmp #5
	bcs ++
	
	dec mapPosY
	lda mapPosY
	cmp #$FF
	bne +
		dec mapPosY+1
+
	jsr drawMap
	jsr setPlayer
	jsr searchText
	jmp moveFinished
+
	lda posY
	beq +
	dec posY
	jsr setPlayer
	jsr searchText
+
	jmp moveFinished

checkIsLeft
	lda #%00000100 ;when joystick left
	bit $dc00
	beq +
	lda #%11111101 ;when key "A" hit
	sta $dc00
	lda $dc01
	cmp #%11111011
	bne checkIsRight
+
	;Joystick Left
	jsr checkTerrainLeft
	bne +
		jmp moveFinished
+
	lda MOVEMENT_DELAY
	sta movementBuffer
	
	lda mapPosX
	cmp #1
	bcc ++

	lda posX
	cmp #5
	bcs ++
	
	dec mapPosX
	lda mapPosX
	cmp #$FF
	bne +
		dec mapPosX+1
+
	jsr drawMap
	jsr setPlayer
	jsr searchText
	jmp moveFinished
+
	lda posX
	beq +
	dec posX
	jsr setPlayer
	jsr searchText
+
	jmp moveFinished

checkIsRight
	lda #%00001000 ;when joystick right
	bit $dc00
	beq +
	lda #%11111011 ;when key "D" hit
	sta $dc00
	lda $dc01
	cmp #%11111011
	bne checkIsDown
+

	;Joystick Right
	jsr checkTerrainRight
	bne +
		jmp moveFinished
+
	lda MOVEMENT_DELAY
	sta movementBuffer

	lda mapPosX+1
	cmp mapPosXMax+1
	bcc +

	lda mapPosX
	cmp mapPosXMax
	bcs +++
	
+
	lda posX
	cmp #4
	bcc ++
	
	inc mapPosX
	bne +
		inc mapPosX+1
+
	jsr drawMap
	jsr setPlayer
	jsr searchText
	jmp moveFinished
+
	lda posX
	cmp #8
	beq +
	inc posX
	jsr setPlayer
	jsr searchText
+
	jmp moveFinished

checkIsDown
	lda #%00000010 ;when joystick down
	bit $dc00
	beq +
	lda #%11111101 ;when key "S" hit
	sta $dc00
	lda $dc01
	cmp #%11011111
	bne moveFinished
+
	;Joystick Down
	jsr checkTerrainDown
	bne +
		jmp moveFinished
+
	lda MOVEMENT_DELAY
	sta movementBuffer

	lda mapPosY+1
	cmp mapPosYMax+1
	bcc +
	
	lda mapPosY
	cmp mapPosYMax
	bcs +++

+
	lda posY
	cmp #4
	bcc ++

	inc mapPosY
	bne +
		inc mapPosY+1
+
	jsr drawMap
	jsr setPlayer
	jsr searchText
	jmp moveFinished
+
	lda posY
	cmp #8
	beq +
	inc posY
	jsr setPlayer
	jsr searchText
+
	jmp moveFinished

moveFinished

	lda #$FB ;when key "R" hit
	sta $dc00
	lda $dc01
	cmp #$FD
	bne Free_Raster_Indicator_Key_Pressed
		lda Raster_Indicator_Key_Pressed
		bne +
		lda #1
		sta Raster_Indicator_Key_Pressed

		;Display raster lines when "R" pressed
		lda Raster_Indicator
		eor #1
		sta Raster_Indicator
		
		
		lda Raster_Indicator
		bne +
		lda #0
		sta $d020
		
		jmp +
Free_Raster_Indicator_Key_Pressed
		lda #0
		sta Raster_Indicator_Key_Pressed

	+

	lda #127 ;Disable keyboard detection
	sta $dc00

	lda Raster_Indicator
	beq +
		lda #0
		sta $d020
	+
	
		pla
		tay
		pla
		tax
		pla
	rti
