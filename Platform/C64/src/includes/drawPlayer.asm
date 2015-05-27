
setPlayer
	ldy posX
	lda playerPosTableX,y
	sta $d000
	sta $d002

	ldy posY
	lda playerPosTableY,y
	sta $d001
	sta $d003

rts
