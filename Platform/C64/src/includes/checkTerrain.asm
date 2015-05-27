
checkTerrainLeft
	lda playerPosition_LO
	sec
	sbc #1
	sta mapPointer_LO
	lda playerPosition_HI
	sbc #0
	sta mapPointer_HI
	jmp checkTerrain

checkTerrainRight
	lda playerPosition_LO
	clc
	adc #1
	sta mapPointer_LO
	lda playerPosition_HI
	adc #0
	sta mapPointer_HI
	jmp checkTerrain

checkTerrainUp
	lda playerPosition_LO
	sec
	sbc mapSizeXAdc
	sta mapPointer_LO
	lda playerPosition_HI
	sbc mapSizeXAdc+1
	sta mapPointer_HI
	jmp checkTerrain

checkTerrainDown
	lda playerPosition_LO
	clc
	adc mapSizeXAdc
	sta mapPointer_LO
	lda playerPosition_HI
	adc mapSizeXAdc+1
	sta mapPointer_HI
	jmp checkTerrain

checkTerrain

	ldy #0
	lda (mapPointer_LO),y
	cmp #1
	beq cannotAccess

	;Can access
	lda #1
rts

cannotAccess
	;Cannot access
	lda #0
rts
