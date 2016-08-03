;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

* = $6000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

start:

; This code is written bottom-up. That is, simple routines first,
; then routines that call those to build complexity. The main
; code is at the very end.

; Here are the entry points for PLASMA code. Identical API for 2D and 3D.
	jmp pl_initMap 		; params: mapNum, pMapData, x, y, dir
	jmp pl_flipToPage1	; params: none; return: nothing
	jmp pl_getPos		; params: @x, @y; return: nothing
	jmp pl_setPos		; params: x (0-255), y (0-255); return: nothing
	jmp pl_getDir		; params: none; return: dir (0-15)
	jmp pl_setDir		; params: dir (0-15); return: nothing
	jmp pl_advance		; params: none; return: 0 if same, 1 if new map tile, 2 if new and scripted
	jmp pl_setColor		; params: slot (0=sky/1=ground), color (0-15); return: nothing
	jmp pl_render		; params: none
	jmp pl_texControl	; params: 0=unload textures, 1=load textures
	jmp pl_getScripts	; params: none
	jmp pl_setAvatar	; params: A=tile number

; Conditional assembly flags
DEBUG		= 0		; 1=some logging, 2=lots of logging
DEBUG_COLUMN	= -1

; Shared constants, zero page, buffer locations, etc.
!source "render.i"
; Memory manager
!source "../include/mem.i"
; Font engine
!source "../include/fontEngine.i"
; PLASMA
!source "../include/plasma.i"

; Local constants
MAX_SPRITES	= 64		; max # sprites visible at once
NUM_COLS	= 63
SPRITE_DIST_LIMIT = 8
SPRITE_CT_LIMIT = 4

; Starting position and dir. Eventually this will come from the map
PLAYER_START_X = $280		; 1.5
PLAYER_START_Y = $380		; 3.5
PLAYER_START_DIR = 4

;PLAYER_START_X = $53E		; special pos for debugging
;PLAYER_START_Y = $67A
;PLAYER_START_DIR = $A

; Useful constants
W_LOG_256	= $0800
W_LOG_65536	= $1000
W_LOG_VIEW_DIST = $0E3F

; Variables
mapHeader: 	!word 0		; map with header first
mapBase:   	!word 0		; first byte after the header
mapRayOrigin:	!word 0
mapNum:    	!byte 1
nMapSprites:	!byte 0		; number of sprite entries on map to fix up
nextLink:	!byte 0		; next link to allocate
plasmaStk:      !byte 0
nTextures:	!byte 0
scripts:	!word 0		; pointer to loaded scripts module
expanderRelocd:	!byte 0		; flag so we only reloc expander once
shadow_pTex:	!word 0		; backup of pTmp space on aux (because it gets overwritten by expander)

skyColorEven:   !byte $20
skyColorOdd:    !byte $22
gndColorEven:   !byte $20
gndColorOdd:    !byte $28

;-------------------------------------------------------------------------------
; Multiply two bytes, quickly but somewhat inaccurately, using logarithms.
; Utilizes tbl_log2_b_b and tbl_pow2_b_b, which translate to and from 3+5 bit
; fixed precision base 2 logarithms.
;
; Input : unsigned bytes in X and Y
; Output: unsigned byte in A of the *high* byte of the result only
;
umul_bb_b: !zone
	cpx #4
	bcc .x_lt_4
	cpy #4
	bcc .y_lt_4
	lda tbl_log2_b_b,x	; log2(x)
	clc
	adc tbl_log2_b_b,y	; plus log2(y)
	tax
	lda tbl_pow2_b_b,x	; 2 ^ (log2(x) + log2(y))  =  x * y
	rts
; handle cases 0..3 directly. This halved the size of the tables
; and made them more accurate.
.x_lt_4:
	lda #0
	cpx #2
	bcc .done		; x=0 or x=1: the high byte of result will be zero
	beq .two
.three:	cpy #86			; x=3: 3*(0..85) results in hi=0
	bcc .done
	lda #1
	cpy #171		; 3*(86..170) results in hi=1
	bcc .done
	lda #2			; 3*(171..255) results in hi=2
	rts
.two:	cpy #$80		; x=2: high byte is 1 iff input >= 0x80
	bcc .done
	lda #1
.done:	rts
.y_lt_4:
	stx tmp			; switch X and Y
	tya
	tax
	ldy tmp
	jmp .x_lt_4		; then re-use code

;-------------------------------------------------------------------------------
; Calculate log2 of a 16-bit number.
; Input: 16-bit unsigned int in A(lo)/X(hi)
; Output: fixed point 8+8 bit log2 in A(lo)/X(hi)
;
log2_w_w: !zone
	cpx #0
	beq log2_b_w	; hi-byte zero? only consider low byte
	stx tmp
	ldx #8		; start with exponent=8
	lsr tmp		; shift down
	beq .gotMant	; until high byte is exactly 1
.hiLup:	ror		; save the bit we shifted out
	inx		; bump the exponent
	lsr tmp		; shift next bit
	bne .hiLup	; loop again
.gotMant:		; mantissa now in A, exponent in X. Translate mantissa to log using table, and we're done
	tay
	lda tbl_log2_w_w,y
	rts

; Same as above but with with 8-bit input instead of 16. Same output though.
log2_b_w: !zone
	tax		; special case: log(0) we call zero.
	beq .zero
.low:			; we know high byte is zero
	ldx #7		; start with exponent=7
	asl		; shift up
	bcs .gotMant	; until high byte would be exactly 1
.loLup:	dex		; bump exponent down
	asl		; shift next bit
	bcc .loLup	; loop again
.gotMant:		; mantissa now in A, exponent in X. Translate mantissa to log using table, and we're done
	tay
	lda tbl_log2_w_w,y
.zero:	rts


;-------------------------------------------------------------------------------
; Calculate 2^n for a fixed-point n
; Input:  8.8 fixed precision number in Y(lo)/X(hi)
; Output: 16 bit unsigned int in A(lo)/X(hi)
;
pow2_w_w: !zone
	txa			; check for negative input
	bmi .zero		; if negative, just return zero - cheesy but works
	lda tbl_pow2_w_w,y	; table gives us log(2) -> mantissa in A
	cpx #8			; check the exponent
	bcc .lo			; less than 8? high byte will be zero.
	beq .mid		; equal to 8? high byte will be one.
.hi:	; greater than 8: need to compute high byte
	ldy #1			; start with one
	sty tmp
.hiLup:	asl			; shift up
	rol tmp			; including high byte
	dex			; count down exponent
	cpx #8			; until we reach 8
	bne .hiLup
	ldx tmp			; load computed high byte; proper low byte is already in A.
	rts
.lo:	sec			; so we shift a 1 into the high bit of the low byte
.loLup:	ror			; shift down
	inx			; count up exponent...
	cpx #8			; ...until we hit 8
	bcc .loLup		; handy because we need carry to be clear the next go-round
	ldx #0
	rts
.mid:	; exponent exactly 8 when we get here
	ldx #1			; that means high byte should be 1
	rts
.zero	lda #0
	tax
	rts

;-------------------------------------------------------------------------------
; Cast a ray  [ref BigBlue3_60]
; Input: pRayData, plus Y reg: precalculated ray data (4 bytes)
;        playerX, playerY (integral and fractional bytes of course)
;        pMap: pointer to current row on the map (mapBase + playerY{>}*height)
;        screenCol: column on the screen for buffer store
; Output: (where x is screenCol)
;         heightBuf,x - height to draw in double-lines
;         depthBuf,x  - depth (log of height basically), for compositing order
;         txNumBuf,x  - texture number to draw
;         txColBuf,x  - column in the texture to draw
castRay: !zone
	; First, grab the precalculated ray data from the table.
	ldx #1			; default X step: forward one column of the map
	lda (pRayData),y	; rayDirX
	bpl +			; if positive, don't negate
	eor #$FF		; negate
	ldx #$FF		; x step: back one column of the map
+	asl			; mul by 2 now that we've stripped the sign
	sta rayDirX		; store x direction
	stx stepX		; and x increment

	iny
	ldx #1			; default y step: forward one row of the map
	lda (pRayData),y	; rayDirY
	bpl +			; if positive, don't negate
	eor #$FF		; negate
	ldx #$FF		; y step: back one row of map
+	asl			; mul by 2 now that we've stripped the sign
	sta rayDirY		; store y direction
	stx stepY		; and y increment

	iny
	lda (pRayData),y	; distance moved in X direction
	sta deltaDistX		; for each step

	iny
	lda (pRayData),y	; distance moved in Y direction
	sta deltaDistY		; for each step

!if DEBUG >= 2 {
	+prStr : !text "cast: ddX=",0
	+prByte deltaDistX
	+prStr : !text "rdX=",0
	+prByte rayDirX
	+prStr : !text "stx=",0
	+prByte stepX
	+crout
	+prStr : !text "      ddY=",0
	+prByte deltaDistY
	+prStr : !text "rdY=",0
	+prByte rayDirY
	+prStr : !text "sty=",0
	+prByte stepY
	+crout
} ; end DEBUG

	; Next we need to calculate the initial distance on each side
	; Start with the X side
	lda playerX		; fractional byte of player distance
	bit stepX		; if stepping forward...
	bmi +
	eor #$FF		; invert initial dist
+	tax
	ldy deltaDistX		; calculate fraction of delta
	jsr umul_bb_b
	sta sideDistX		; to form initial side dist
	; Now the Y side
	lda playerY		; fractional byte of player distance
	bit stepY		; if stepping forward...
	bmi +
	eor #$FF		; invert initial dist
+	tax
	ldy deltaDistY		; calculate fraction of delta
	jsr umul_bb_b
	sta sideDistY		; to form initial side dist

!if DEBUG >= 2 {
	+prStr : !text "  initial sdx=",0
	+prByte sideDistX
	+prStr : !text " sdy=",0
	+prByte sideDistY
	+crout
} ; end DEBUG

	; Start at the player's position, and init Y reg for stepping in the X dir
	ldy playerX+1
	sty mapX
	ldx playerY+1
	stx mapY

	; the DDA algorithm
.DDA_step:
	lda sideDistX
	cmp sideDistY		; decide whether it's closer to step in X dir or Y dir
	bcs .takeStepY
	; taking a step in the X direction
.takeStepX:
	lda stepX		; advance mapX in the correct direction
	bmi .negX
	inc mapX
	iny			; also the Y reg which indexes the map
	jmp .checkX
.negX:	dec mapX
	dey
.checkX:
	!if DEBUG >= 2 { +prStr : !text "  sideX",0 :  jsr .debugSideData }
	lda sideDistY		; adjust side dist in Y dir
	sec
	sbc sideDistX
	sta sideDistY
	lda deltaDistX		; re-init X distance
	sta sideDistX
	lda (pMap),y		; check map at current X/Y position
	and #$DF		; mask off script flag
	beq .DDA_step		; nothing there? do another step.
	bpl .hitX
	jmp .hitSprite
	; We hit something!
.hitX:	!if DEBUG >= 2 { +prStr : !text "  Hit.",0 }
	sta txNum		; store the texture number we hit
	lda #0
	sec
	sbc playerX		; inverse of low byte of player coord
	sta dist		; is fractional byte of dist.
	lda mapX		; map X is the integer byte
	sbc playerX+1
	tax
	bit stepX
	bpl +
	inx			; if stepping backward, add one to dist
+	stx dist+1
	ldx rayDirX		; parameters for wall calculation
	ldy rayDirY
	lda stepY
	jsr .wallCalc		; centralized code for wall calculation
	; adjust wall X
	lda playerY		; fractional player pos
	clc
	adc txColumn
	bit stepX		; if stepping forward in X...
	bmi +
	eor #$FF		; ...invert the texture coord
+	sta txColBuf,x		; and save the final coordinate
	!if DEBUG >= 2 { jsr .debugFinal }
	rts
	; taking a step in the Y direction
.takeStepY:
	lda pMap		; get ready to switch map row
	bit stepY		; advance mapY in the correct direction
	bmi .negY
	inc mapY
	clc
	adc mapWidth
	bcc .chkY
	inc pMap+1
	bne .chkY		; always taken
.negY:	dec mapY
	sec
	sbc mapWidth
	bcs .chkY
	dec pMap+1
.chkY:	sta pMap
	!if DEBUG >= 2 { +prStr : !text "  sideY",0 :  jsr .debugSideData }
	lda sideDistX		; adjust side dist in Y dir
	sec
	sbc sideDistY
	sta sideDistX
	lda deltaDistY		; re-init Y distance
	sta sideDistY
	lda (pMap),y		; check map at current X/Y position
	and #$DF		; mask off script flag
	bmi .hitSprite
	bne .hitY		; nothing there? do another step.
	jmp .DDA_step
.hitY:	; We hit something!
	!if DEBUG >= 2 { +prStr : !text "  Hit.",0 }
	sta txNum		; store the texture number we hit
	lda #0
	sec
	sbc playerY		; inverse of low byte of player coord
	sta dist		; is fractional byte of dist.
	lda mapY		; map X is the integer byte
	sbc playerY+1
	tax
	bit stepY
	bpl +
	inx			; if stepping backward, add one to dist
+	stx dist+1
	ldx rayDirY		; parameters for wall calculation
	ldy rayDirX
	lda stepX
	jsr .wallCalc		; centralized code for wall calculation
	; adjust wall X
	lda playerX		; fractional player pos
	clc
	adc txColumn
	bit stepY		; if stepping backward in Y
	bpl +
	eor #$FF		; ...invert the texture coord
+	sta txColBuf,x		; and save the final coord
	!if DEBUG >= 2 { jsr .debugFinal }
	rts
.hitSprite:
	cmp #$DF		; check for special mark at edges of map (was $FF but the $20 bit was masked off)
	beq .hitEdge
	; We found a sprite cell on the map. We only want to process this sprite once,
	; so check if we've already done it.
	tax
	and #$40
	beq .notDone		; already done, don't do again
	txa
	and #$1F
	tax
	jsr getTileFlags
	and #4			; blocker sprite?
	bne .hitEdge		; if yes, stop tracing here
	jmp .spriteDone		; if not, keep tracing
.notDone:
	; Haven't seen this one yet. Mark it, and also record the address of the flag
	; so we can clear it later after tracing all rays.
	lda (pMap),y		; get back the original byte
	ora #$40		; add special flag
	sta (pMap),y		; and store it back
	and #$1F		; get just the texture number
	sta txNum		; and save it
	ldx nMapSprites		; get ready to store the address so we can fix the flag later
	cpx #MAX_SPRITES	; check for table overflow
	bne +
	lda #'S'
	sta $7F7		; quickly note it on the text screen
	bne .spriteDone		; and skip this sprite (always taken)
+	tya			; Y reg indexes the map
	clc
	adc pMap		; add to map pointer
	sta mapSpriteL,x	; save lo byte
	lda #0
	adc pMap+1		; calculate hi byte of map pointer
	sta mapSpriteH,x	; and save that too
	inc nMapSprites		; advance to next table entry
	!if DEBUG >= 2 { jsr .debugSprite }
	lda #$80		; put sprite in middle of map square
	sta spriteX
	sta spriteY
	lda mapX		; x coord of sprite
	sta spriteX+1
	lda mapY
	sta spriteY+1		; y coord of sprite
	tya			; save Y reg to avoid losing our place on the map
	pha
	jsr spriteCalc		; do all the magic math to calculate the sprite's position
	bcc +			; if sprite is off-screen, don't draw it
	jsr drawSprite		; put it on screen
+	pla
	tay			; restore map position index
	ldx txNum
	jsr getTileFlags
	and #4			; blocker sprite?
	bne .hitEdge		; if yes, stop tracing rays
.spriteDone:
	jmp .DDA_step		; trace this ray some more

	; special case: hit edge of map
.hitEdge:
	ldy #0			; height
	lda #1			; depth
	sty txNum		; texture number
	jsr saveLink		; allocate a link and save those
	lda #0			; column number
	sta txColBuf,x		; save that too
	rts			; all done

	; wall calculation: X=dir1, Y=dir2, A=dir2step
.wallCalc:
	pha		; save step
	tya
	pha		; save dir2
	txa
	jsr log2_b_w	; calc log2(dir1)
	sta .sub1+1	; save it for later subtraction
	stx .sub2+1
	lda dist	; calc abs(dist)
	ldx dist+1	; dist currently in A(lo)/X(hi)
	bpl .notNeg
	lda #0		; invert distance if negative to get absolute value
	sec
	sbc dist
	tay
	lda #0
	sbc dist+1
	tax		; get inverted dist into A(lo)/X(hi)
	tya
.notNeg:
	!if DEBUG >= 2 { +prStr : !text "  dist=",0 : +prXA }
	jsr log2_w_w	; calculate log2(abs(dist))
	sec
.sub1:	sbc #0		; subtract log2(dir1)
	sta diff
	txa
.sub2:	sbc #0
	sta diff+1
	!if DEBUG >= 2 { +prStr : !text "diff=",0 : +prWord diff }
	; Calculate texture coordinate
	pla		; get dir2 back
	jsr log2_b_w	; calculate log2(dir2)
	clc
	adc diff	; sum = diff + log2(dir2)
	tay
	txa
	adc diff+1
	tax
	!if DEBUG >= 2 { +prStr : !text "sum=",0 : +prXY : +crout }
	jsr pow2_w_w	; calculate 2 ^ sum
	; fractional part (A-reg) of result is texture coord
	tax
	pla		; retrieve the step direction
	bpl +		; if positive, don't flip the texture coord
	txa
	eor #$FF	; negative, flip the coord
	tax
+	stx txColumn
	; Calculate line height
	; we need to subtract diff from log2(64) which is $0600
	lda #0
	sec
	sbc diff
	tay
	lda #6
	sbc diff+1
	tax
	sta tmp
	tya
	lsr tmp		; Depth is 4 bits of exponent + upper 4 bits of mantissa
	ror
	lsr tmp
	ror
	lsr tmp
	ror
	lsr tmp
	ror
	pha		; stash it on stack (we don't have X reg free yet for indexed store)
	jsr pow2_w_w	; calculate 2 ^ (log(64) - diff)  =~  64.0 / dist
	tay		; save the height in Y reg
	txa
	beq +
	ldy #$FF	; clamp large line heights to 255
+	pla		; get the depth back
	jmp saveLink	; save final column data to link buffer

!if DEBUG >= 2 {
.debugSideData:
	+prStr : !text ", mapX=",0
	+prByte mapX
	+prStr : !text "mapY=",0
	+prByte mapY
	+prStr : !text "sdx=",0
	+prByte sideDistX
	+prStr : !text "sdy=",0
	+prByte sideDistY
	+crout
	rts
.debugFinal:
	ldx screenCol
	+prStr : !text "  height=",0
	lda heightBuf,x
	+prA
	+prStr : !text "depth=",0
	lda depthBuf,x
	+prA
	+prStr : !text "txNum=",0
	lda txNumBuf,x
	+prA
	+prStr : !text "txCol=",0
	lda txColBuf,x
	+prA
	+crout
	jmp rdkey
}
!if DEBUG >= 2 {
.debugSprite:
	+crout
	+prStr : !text "Hit sprite, mapX=",0
	+prByte mapX
	+prStr : !text "mapY=",0
	+prByte mapY
	+prStr : !text "sprite=",0
	+prA
	+crout
	rts
}

;------------------------------------------------------------------------------
; Perform screen position calculations for a sprite.
; Input: spriteX, spriteY, playerX, playerY, playerDir
; Output: clc if sprite is off screen
;         sec if sprite is on screen, and sets the following variables:
;         lineCt (height), wSpriteLeft, txColumn, wTxColBump, depth
spriteCalc: !zone
	!if DEBUG >= 2 { jsr .debug0 }

	lda #0			; track sign bits
	sta bSgnSinT
	sta bSgnCosT
	sta bSgnDx
	sta bSgnDy
	sta bSgnRy

	; Look up sin of player direction, minus wLog256, and store it
	lda playerDir
	asl
	tay
	lda sinTbl,y
	sta wLogSinT		; store lo byte
	lda sinTbl+1,y
	bpl +			; sign bit clear?
	and #$7F		; sign bit was set - mask it off
	inc bSgnSinT		; and update sign flag
+	sec
	sbc #>W_LOG_256		; subtract wLog256
	sta wLogSinT+1		; store hi byte

	; Look up cos of player direction, minus wLog256, and store it
	lda playerDir
	sec
	sbc #4			; cos(a) = sin(a - 90 degrees)
	and #$F			; wrap around
	asl
	tay
	lda sinTbl,y
	sta wLogCosT		; store lo byte
	lda sinTbl+1,y
	bpl +			; sign bit clear?
	and #$7F		; sign bit was set - mask it off
	inc bSgnCosT		; and update sign byte
+	sec
	sbc #>W_LOG_256		; subtract wLog256
	sta wLogCosT+1		; store hi byte

	!if DEBUG >= 2 { jsr .debug1 }

	; Calculate wLogDx = log2_w_w(spriteX - playerX), as abs value and a sign bit
	lda spriteX		; calculate spriteX - playerX
	sec
	sbc playerX
	tay			; stash lo byte
	lda spriteX+1		; work on hi byte
	sbc playerX+1
	tax			; put hi byte in X where we need it
	bpl +			; if positive, no inversion necessary
	inc bSgnDx		; flip sign bit for output
	jsr .negYX		; negate to get absolute value
+	tya			; lo byte in A where log2 wants it
	cpx #SPRITE_DIST_LIMIT	; too far away?
	bmi +
	!if DEBUG >= 2 { +prStr : !text "Sprite is too far away (X).",0 }
	clc
	rts
+	jsr log2_w_w		; wants A=lo, X=Hi
	sta wLogDx
	stx wLogDx+1

	; Calculate wLogDy = log2_w_w(spriteY - playerY), as abs value and a sign bit
	lda spriteY		; calculate spriteY - playerY
	sec
	sbc playerY
	tay			; stash lo byte
	lda spriteY+1		; work on hi byte
	sbc playerY+1
	tax			; put hi byte in X where we need it
	bpl +			; if positive, no inversion necessary
	inc bSgnDy		; flip sign bit for output
	jsr .negYX		; negate to get absolute value
+	tya			; lo byte in A where log2 wants it
	cpx #SPRITE_DIST_LIMIT	; too far away?
	bmi +
	!if DEBUG >= 2 { +prStr : !text "Sprite is too far away (Y).",0 }
	clc
	rts
+	jsr log2_w_w		; wants A=lo, X=Hi
	sta wLogDy
	stx wLogDy+1

	!if DEBUG >= 2 { jsr .debug2 }

	; Calculate wRx = bSgnDx*bSgnCosT*pow2_w_w(wLogDx + wLogCosT) -
	;                 bSgnDy*bSgnSinT*pow2_w_w(wLogDy + wLogSinT)
	lda wLogDx		; start with lo byte
	clc
	adc wLogCosT
	tay			; put it in Y where pow2 wants it
	lda wLogDx+1		; now do hi byte
	adc wLogCosT+1
	tax			; in X where pow2 wants it
	jsr pow2_w_w		; transform from log space to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	tay			; set lo byte aside
	lda bSgnDx
	eor bSgnCosT		; multiply the two sign bits together
	beq +			; if result is clear, no negation
	jsr .negYX		; negate
+	sty wRx			; save partial result
	stx wRx+1
	lda wLogDy		; start with lo byte
	clc
	adc wLogSinT
	tay			; put it in Y where pow2 wants it
	lda wLogDy+1		; now do hi byte
	adc wLogSinT+1
	tax			; in X where pow2 wants it
	jsr pow2_w_w		; transform from log space to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	tay			; set lo byte aside
	lda bSgnDy
	eor bSgnSinT		; multiply the two sign bits together
	eor #1			; one extra inversion since we want to end up subtracting this
	beq +			; if result is clear, no negation
	jsr .negYX		; negate
+	tya
	clc
	adc wRx			; add to partial result
	sta wRx
	txa
	adc wRx+1		; also hi byte
	sta wRx+1

	!if DEBUG >= 2 { jsr .debug3 }

	; if wRx is negative, it means sprite is behind viewer... we get out of school early.
	bpl +
	!if DEBUG >= 2 { +prStr : !text "Sprite is behind viewer.",0 }
	clc
	rts


	; Calculate wRy = bSgnDx*bSgnSinT*pow2_w_w(wLogDx + wLogSinT) + 
	;                 bSgnDy*bSgnCosT*pow2_w_w(wLogDy + wLogCosT);

+	lda wLogDx		; start with lo byte
	clc
	adc wLogSinT
	tay			; put it in Y where pow2 wants it
	lda wLogDx+1		; now do hi byte
	adc wLogSinT+1
	tax			; in X where pow2 wants it
	jsr pow2_w_w		; transform from log space to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	tay			; set lo byte aside
	lda bSgnDx
	eor bSgnSinT		; multiply the two sign bits together
	beq +			; if result is clear, no negation
	jsr .negYX		; negate
+	sty wRy			; save partial result
	stx wRy+1
	lda wLogDy		; start with lo byte
	clc
	adc wLogCosT
	tay			; put it in Y where pow2 wants it
	lda wLogDy+1		; now do hi byte
	adc wLogCosT+1
	tax			; in X where pow2 wants it
	jsr pow2_w_w		; transform from log space to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	tay			; set lo byte aside
	lda bSgnDy
	eor bSgnCosT		; multiply the two sign bits together
	beq +			; if result is clear, no negation
	jsr .negYX		; negate
+	tya
	clc
	adc wRy			; add to partial result
	tay
	txa
	adc wRy+1		; also hi byte
	tax
	bpl +			; if already positive, skip negation
	jsr .negYX		; negate to get abs value
	inc bSgnRy		; and update sign bit
+	sty wRy			; save result (we may not actually need to do this, but it helps w/ debug)
	stx wRy+1
	tya			; get lo byte where it needs to be for log2
	jsr log2_w_w		; calculate the log of wRy
	sta wLogRy		; save it for later
	stx wLogRy+1

	; Calculate wLogSqRy = (log2_w_w(wRy) << 1) - wLog256;
	asl			; we already have it in register. Shift it up 1 bit
	sta wLogSqRy		; save lo byte
	txa			; get hi byte
	rol			; shift up 1 bit, with carry from lo byte
	sec
	sbc #>W_LOG_256		; subtract wLog256 = $800
	sta wLogSqRy+1

	; Calculate wLogSqRx = (log2_w_w(wRx) << 1) - wLog256
+	lda wRx
	ldx wRx+1
	jsr log2_w_w		; calculate log of wRx
	asl			; shift it up 1 bit
	sta wLogSqRx		; save lo byte
	tay			; save it also in Y for upcoming pow2
	txa			; get hi byte
	rol			; shift up 1 bit, with carry from lo byte
	sec
	sbc #>W_LOG_256		; subtract wlog256 = $800
	sta wLogSqRx+1

	; Calculate wSqDist = pow2_w_w(wLogSqRx) + pow2_w_w(wLogSqRy)
	tax			; get lo byte where we need for pow2 (hi byte already in Y)
	jsr pow2_w_w		; convert back to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	sta wSqDist		; save partial result
	stx wSqDist+1
	ldy wLogSqRy		; get wLogSqRy into the right regs
	ldx wLogSqRy+1
	jsr pow2_w_w		; convert it back to normal space also (in: Y=lo,X=hi, out: A=lo,X=hi)
	clc
	adc wSqDist		; add to previous partial result (lo byte)
	sta wSqDist		; save lo byte
	tay			; also stash aside
	txa
	adc wSqDist+1		; hi byte of partial
	sta wSqDist+1		; save hi byte

	; Calculate wLogDist = (log2_w_w(wSqDist) + wLog256) >> 1
	tax			; stash hi byte in X
	tya			; retrieve lo byte back to A
	jsr log2_w_w		; convert to log space
	tay			; set aside lo byte
	txa			; work on hi byte
	clc
	adc #>W_LOG_256		; add wLog256 = $800
	lsr			; shift right 1 bit -> carry
	sta wLogDist+1
	tya			; finish off lo byte
	ror			; shift right with carry from hi byte
	sta wLogDist

	; Calculate wSize = pow2_w_w(wLogViewDist - wLogDist)
	lda #<W_LOG_VIEW_DIST	; lo byte of constant
	sec
	sbc wLogDist		; minus log dist
	sta wLogSize
	tay			; lo byte where pow2 wants it
	lda #>W_LOG_VIEW_DIST	; hi byte of constant
	sbc wLogDist+1		; minus log dist
	sta wLogSize+1
	tax			; hi byte where pow2 wants it
	jsr pow2_w_w		; get back from log space to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	sta wSize
	stx wSize+1

	; Clamp wSize to form lineCt (height of final drawn sprite)
	tay			; stash lo byte of wSize
	txa			; work on hi byte
	beq +
	ldy #$FF
+	sty lineCt

	; Calculate wSpriteTop = 32 - (wSize >> 1);
	lsr			; shift right 1 bit
	tax			; save hi byte to X
	tya			; work on lo byte
	ror			; shift right including bit from hi byte
	tay			; lo byte to Y
	jsr .negYX		; invert it
	tya			; lo byte
	clc
	adc #32			; add 32
	sta wSpriteTop		; save sprite top
	bcc +			; if no carry, no bump
	inx			; bump hi byte
+	stx wSpriteTop+1	; save hi byte

	!if DEBUG >= 2 { jsr .debug4 }

	; Need X position on screen.
	; The constant below is cheesy and based on empirical observation rather than understanding.
	; Sorry :/
	; Calculate wX = bSgnRy * pow2_w_w(log2_w_w(wRy) - wLogDist + log2_w_w(252 / 8 / 0.44))
	; Note: log2_w_w(252 / 8 / 0.44) = $626
	lda wLogRy		; calc wRy minus wLogDist, lo byte
	sec
	sbc wLogDist
	tay			; stash lo byte temporarily
	lda wLogRy+1		; now work on hi byte
	sbc wLogDist+1
	tax			; stash hi byte
	tya			; back to lo byte
	clc
	adc #$26		; add lo byte of const log2_w_w(252 / 8 / 0.44)
	tay			; put it where pow2 wants it
	txa			; finish off hi byte
	adc #6			; hi byte of const
	tax			; put it where pow2 wants it
	jsr pow2_w_w		; back to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	tay			; save lo byte to Y
	lda bSgnRy		; check sign
	and #1			; only the lo bit counts
	beq +			; clear, no invert
	jsr .negYX
+	; don't need to actually store wX -- it's only needed for spriteLeft below
	!if DEBUG >= 2 { jsr .debug5 }

	; Calculate wSpriteLeft = wx + wSpriteTop
	tya
	clc			; lo byte already in A from code above
	adc wSpriteTop		; add to spriteTop (which if you think about it, is a function of dist just like spriteLeft)
	sta wSpriteLeft		; store lo byte of left coord
	tay			; also set aside for later
	txa			; hi byte in X from code above
	adc wSpriteTop+1	; hi byte of top
	sta wSpriteLeft+1	; save hi byte of left coord
	!if DEBUG >= 2 { jsr .debug6 }
	bmi .ckLeft		; if negative, check against left side

	; Left coord is positive, check against right side
	bne .offR		; if there is a hi byte, sprite is off screen to right
	cpy #NUM_COLS		; right side of screen
	bcs .offR		; if left >= 63, sprite is off right side.
	lda #0			; start with first column of texture
	sta txColumn		; save starting tex coord
	jmp .cBump		; sprite starts on screen, might run off to right but that's ok
.offR	!if DEBUG >= 2 { +prStr : !text "Sprite is off-screen to right.",0 }
	clc
	rts

.ckLeft	; Left coord is negative, check against left side
	cmp #$FF		; hi byte should be $FF for sprite to have a chance
	bcc .offL
	cpy #0-NUM_COLS		; now check lo byte, should be >= -63
	bpl .clipL
.offL	!if DEBUG >= 2 { +prStr : !text "Sprite is off-screen to left.",0 }
	clc
	rts
.clipL	; Sprite overlaps left edge of screen; calculate clipping.
	; Calculate txColumn = Math.min(255, pow2_w_w(log2_w_w(-wSpriteLeft) - wLogSize + wLog256))
	lda #0
	sec
	sbc wSpriteLeft		; Negate wSpriteLeft to get positive number
	ldx #0			; We know high byte of wSpriteLeft is $FF, so neg will be 0.
	jsr log2_w_w		; Get to log space (in: A=lo/X=hi; out: same)
	sec
	sbc wLogSize		; subtract lo byte of log size
	tay			; to Y reg where pow2 expects lo byte
	txa			; work on hi byte
	sbc wLogSize+1		; subtract hi byte of log size
	clc
	adc #>W_LOG_256		; add wLog256 = $800
	tax			; to X where pow2 wants the hi byte
	jsr pow2_w_w		; back to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	cpx #0			; in some anomalous cases, it comes out > 255
	beq +			; normal case, no clamping
	lda #$FF		; clamp to 255
+	sta txColumn

.cBump	; Calculate the texture bump per column. Result is really an 8.8 fix-point.
	; wTxColBump = pow2_w_w(wLog65536 - wLogSize)
	lda #<W_LOG_65536
	sec
	sbc wLogSize		; calc lo byte
	tay			; where pow2 wants it
	lda #>W_LOG_65536
	sbc wLogSize+1		; calc hi byte
	tax			; where pow2 wants it
	jsr pow2_w_w		; back to normal space (in: Y=lo,X=hi, out: A=lo,X=hi)
	sta wTxColBump
	stx wTxColBump+1

.cDepth	; Last thing to do is calculate the depth.
	; The constant below is cheesy and I'm not sure why it's needed. But it seems to
	; keep things at roughly the right depth.
	; Calculate depth = calcZ(wLogSize-75) = (wLogSize-75) >> 4
	lda wLogSize
	sec
	sbc #75
	sta depth
	lda wLogSize+1
	sbc #0
	lsr
	ror depth
	lsr
	ror depth
	lsr
	ror depth
	lsr
	ror depth

	!if DEBUG >= 2 { jsr .debug7 }

.draw	; Okay, I think we're all done with calculations for this sprite.
	sec	; flag to say draw it
	rts	; all done

.negYX:				; subroutine to negate value in Y=lo,X=hi.
	tya
	eor #$FF		; invert lo byte
	tay
	txa
	eor #$FF		; invert hi byte
	tax
	iny			; bump by 1 for true negation
	bne +
	inx
+	rts

	; Code for debugging sprite math
!if DEBUG >= 2 {
.debug0 +prStr : !text "playerX=",0
	+prWord playerX
	+prStr : !text "playerY=",0
	+prWord playerY
	+prStr : !text "playerDir=",0
	+prByte playerDir
	+crout
	rts
.debug1	+prStr : !text "bSgnSinT=",0
	+prByte bSgnSinT
	+prStr : !text "wLogSinT=",0
	+prWord wLogSinT
	+prStr : !text "bSgnCosT=",0
	+prByte bSgnCosT
	+prStr : !text "wLogCosT=",0
	+prWord wLogCosT
	+crout
	rts
.debug2	+prStr : !text "bSgnDx=",0
	+prByte bSgnDx
	+prStr : !text "wLogDx=",0
	+prWord wLogDx
	+prStr : !text "bSgnDy=",0
	+prByte bSgnDy
	+prStr : !text "wLogDy=",0
	+prWord wLogDy
	+crout
	rts
.debug3	+prStr : !text "wRx=",0
	+prWord wRx
	+crout
	rts
.debug4	+prStr : !text "wRy=",0
	+prWord wRy
	+prStr : !text "wLogSqRx=",0
	+prWord wLogSqRx
	+prStr : !text "wLogSqRy=",0
	+prWord wLogSqRy
	+crout
	+prStr : !text "wSqDist=",0
	+prWord wSqDist
	+prStr : !text "wLogDist=",0
	+prWord wLogDist
	+crout
	+prStr : !text "wLogSize=",0
	+prWord wLogSize
	+prStr : !text "wSize=",0
	+prWord wSize
	+crout
	+prStr : !text "wSpriteTop=",0
	+prWord wSpriteTop
	+crout
	rts
.debug5 +prStr : !text "wX=",0
	+prXY
	rts
.debug6 +prStr : !text "wSpriteLeft=",0
	+prWord wSpriteLeft
	+crout
	rts
.debug7	+prStr : !text "txColumn=",0
	+prByte txColumn
	+prStr : !text "wTxColBump=",0
	+prWord wTxColBump
	+prStr : !text "depth=",0
	+prByte depth
	+crout
	rts
}

;------------------------------------------------------------------------------
; Draw sprite on screen. Uses all the variables output by spriteCalc.
drawSprite: !zone
	lda screenCol		; save screen column that main raycaster is doing
	pha
	lda wSpriteLeft		; lo byte of left coord
	ldx wSpriteLeft+1	; hi byte
	bpl +			; if positive, ok
	lda #0			; if negative, clamp left coord to 0
+	sta screenCol
	lda #$80		; fractional byte of txColumn
	pha
.lup	ldx screenCol
	cpx #NUM_COLS
	bcs .done
	inc spriteCtBuf,x	; count sprites in this column
	lda spriteCtBuf,x	; and check it
	cmp #SPRITE_CT_LIMIT	; limit to 4 sprites per column
	bcs .skip
	ldy lineCt		; column height
	lda depth		; depth index
	jsr saveLink		; save height and depth, link in to column data
	lda txColumn		; also save the column number
	sta txColBuf,x
.skip	inc screenCol		; next column on screen
	pla			; fractional byte
	clc
	adc wTxColBump		; advance lo byte
	pha
	lda txColumn		; integer part
	adc wTxColBump+1	; advance integer part
	sta txColumn		; and save it
	bcc .lup		; back for more
.done	pla			; discard fractional byte
	pla			; get back ray caster's screen column
	sta screenCol
	rts

;------------------------------------------------------------------------------
; Save a link in the linked column data, sorted according to its depth.
;
; Note: 	Does *not* fill in txColBuf,x on the assumption that the column
;       	number needs further computation after this function returns.
;		Store it in txColBuf,x when it's ready.
;
; Input: 	screenCol: horizontal screen column position 
;		Y-reg: column height
;		A-reg: depth index
;		txNum: texture number
;
; Output:	X-reg: index into the link buffers at which data was saved
;		(can be used for further manipulation of the values there).
;
saveLink: !zone
	sta tmp			; keep depth for storing later
	sty tmp+1		; same with height
	ldx screenCol
	ldy firstLink,x
	bne .chk1
.store				; this is the first link for the column
	lda nextLink
	sta firstLink,x
.store2	tax			; switch to the new link's area now
	bne +
	+prChr 'L'
	brk			; ack! ran out of link space -- too much complexity on screen
+	tya
	sta linkBuf,x
	lda tmp
	sta depthBuf,x
	lda tmp+1
	sta heightBuf,x
	lda txNum
	sta txNumBuf,x
	inc nextLink
	!if DEBUG >= 2 { jsr .debugLink }
	rts	
.chk1				; does it need to be inserted before the existing first link?
	lda tmp
	cmp depthBuf,y
	bcc .store
	; advance to next link
.next	tya
	tax
	ldy linkBuf,x
	bne .chk2
	; put new link here
.insert	lda nextLink
	sta linkBuf,x
	bne .store2		; always taken; also note: Y contains next link (0 for end of chain)
.chk2				; do we need to insert before this (non-first) link?
	lda tmp
	cmp depthBuf,y
	bcc .insert		; found the right place
	bcs .next		; not the right place to insert, look at next link (always taken)
!if DEBUG >= 2 {
.debugLink:
	lda screenCol
	cmp #DEBUG_COLUMN
	beq +
	rts
+	txa
	pha
	+prStr : !text "Links for col ",0
	+prByte screenCol
	ldx screenCol
	ldy firstLink,x
.dlup	+prStr : !text "[ht=",0
	lda heightBuf,y
	+prA
	+prStr : !text "tx=",0
	lda txNumBuf,y
	+prA
	+prStr : !text "dp=",0
	lda depthBuf,y
	+prA
	+prStr : !text "] ",0
	lda linkBuf,y
	tay
	bne .dlup
	+crout
	pla
	tax
	rts
}

; Advance pLine to the next line on the hi-res screen
nextLine: !zone
	lda pLine+1	; Hi byte of line
	clc
	adc #4		; Next line is 1K up
	tax
	eor pLine+1
	and #$20	; Past end of screen?
	beq .done	; If not, we're done
	txa
	sec
	sbc #$20	; Back to start
	tax
	lda pLine	; Lo byte
	clc
	adc #$80	; Inner blks offset by 128 bytes
	sta pLine
	bcc .done
	inx		; Next page
	txa
	and #7
	cmp #4		; Still inside inner blk?
	bne .done	; If so we're done
	txa
	sec
	sbc #4		; Back to start of inner blk
	tax
	lda pLine
	clc
	adc #$28	; Outer blks offset by 40 bytes
	sta pLine
.done:	stx pLine+1
	rts

; Draw a ray that was traversed by castRay
drawRay: !zone
	ldy screenCol
	ldx firstLink,y
.lup:	lda linkBuf,x		; get link to next stacked data to draw
	pha			; save link for later
	lda heightBuf,x
	beq .skip
	sta lineCt
	lda txNumBuf,x
	sta txNum
	lda txColBuf,x
	sta txColumn
	; Make a pointer to the selected texture
	ldx txNum
	dex			; translate tex 1..4 to 0..3
	lda texAddrLo,x
	sta pTex
	lda texAddrHi,x
	sta pTex+1
	; jump to the unrolled expansion code for the selected height
	!if DEBUG >= 2 { +prStr : !text "Calling expansion code.",0 }
	ldx txColumn
	lda lineCt
	sei			; prevent interrupts while in aux mem
	sta setAuxZP
	asl
	bcc +
	lda #254		; clamp max height
+	sta expanderJmp+1	; set vector offset
	bit setLcRW+lcBank2	; part of expander split and relocated to LC bank 2
	txa
	jsr callExpander	; was copied from .callIt to $100 at init time
	sta clrAuxZP
	cli			; interrupts ok after we get back from aux
.skip	pla			; retrieve link to next in stack
	tax			; put in X for indexing
	bne .lup		; if non-zero, we have more to draw
	rts			; next link was zero - we're done with this ray

; Template for blitting code [ref BigBlue3_70]
blitTemplate: !zone	; comments show byte offset
	lda decodeTo57	;  0: pixel 3
	asl		;  3: save half of pix 3 in carry
	ora decodeTo01	;  4: pixel 0
	ora decodeTo23	;  7: pixel 1
	ora decodeTo45	; 10: pixel 2
	sta (0),y	; 13: even column
	iny		; 15: prep for odd
	lda decodeTo01b	; 16: pixel 4
	ora decodeTo23b	; 19: pixel 5
	rol		; 22: recover half of pix 3
	ora decodeTo56	; 23: pixel 6 - after rol to ensure right hi bit
	sta (0),y	; 26: odd column
	dey		; 28: prep for even
			; 29 bytes total

; Create the unrolled blit code
makeBlit: !zone
	lda #0		; Start with line zero
	sta lineCt
	lda #<TOP_LINE	; Begin with the first screen line
	sta pLine
	lda #>TOP_LINE
	sta pLine+1
	lda #<blitRoll	; Store to blit unroll code buf
	sta pDst
	lda #>blitRoll
	sta pDst+1
.lineLup:		; Copy the template
	ldy #29
.copy:	lda blitTemplate,y
	sta (pDst),y
	dey
	bpl .copy
; Set the line pointers
	ldy #14
	jsr .storeLine
	ldy #27
	jsr .storeLine
	; Get ready for odd line
	jsr .advance
; Loop until all lines are done
	lda lineCt
	cmp #NLINES
	bne .lineLup
	jmp storeRTS	; Finish with RTS for cleanliness
.storeLine:		; Subroutine to store pLine to pDst
	lda lineCt
	asl
	sta (pDst),y
	rts
.advance:		; Subroutine to go to next unroll
	lda #29
	jsr advPDst
	inc lineCt
	jmp nextLine

; Add A to pDst
advPDst: !zone
	clc
	adc pDst
	sta pDst
	bcc .rts
	inc pDst+1
.rts:	rts

; Store a RTS at pDst
storeRTS: !zone
	lda #$60
	ldy #0
	sta (pDst),y
	rts

; Create code to clear the blit
makeClrBlit: !zone
	lda #<blitRoll
	sta pDst
	lda #>blitRoll
	sta pDst+1
	ldx #0
	ldy #0
.lup:	lda .st
	sta clrBlitRollE,x
	sta clrBlitRollO,x
	inx
	lda pDst
	sta clrBlitRollE,x
	clc
	adc #29
	sta clrBlitRollO,x
	inx
	lda pDst+1
.st:	sta clrBlitRollE,x
	adc #0
	sta clrBlitRollO,x
	inx
	lda pDst
	clc
	adc #29*2
	sta pDst
	bcc +
	inc pDst+1
+	iny
	iny
	cpy #64
	bne .noSwitch
	lda .tya	; switch from sky color to ground color
	sta clrBlitRollE,x
	sta clrBlitRollO,x
	inx
.noSwitch:
	cpy #NLINES
	bne .lup
	lda .rts
	sta clrBlitRollE,x
	sta clrBlitRollO,x
.rts:	rts
.tya:	tya

; Clear the blit
clearBlit: !zone
	sta setAuxZP
	lda pTex		; save screen addr that gets overwritten by expander
	sta shadow_pTex
	lda pTex+1
	sta shadow_pTex+1
	sta clrAuxZP
	lda byteNum
	and #2
	bne .alt
	ldx #BLIT_OFF0
	jsr .clr1
	ldx #BLIT_OFF1
	jsr .clr2
	ldx #BLIT_OFF2
	jsr .clr1
	ldx #BLIT_OFF3
	jsr .clr2
	ldx #BLIT_OFF4
	jsr .clr1
	ldx #BLIT_OFF5
	jsr .clr2
	ldx #BLIT_OFF6
	jmp .clr1
.alt:	ldx #BLIT_OFF0
	jsr .clr2
	ldx #BLIT_OFF1
	jsr .clr1
	ldx #BLIT_OFF2
	jsr .clr2
	ldx #BLIT_OFF3
	jsr .clr1
	ldx #BLIT_OFF4
	jsr .clr2
	ldx #BLIT_OFF5
	jsr .clr1
	ldx #BLIT_OFF6
	jmp .clr2
.clr1:	ldy gndColorEven
	lda skyColorEven
	jsr clrBlitRollO
	ldy gndColorOdd
	lda skyColorOdd
	jmp clrBlitRollE
.clr2:	ldy gndColorEven
	lda skyColorEven
	jsr clrBlitRollE
	ldy gndColorOdd
	lda skyColorOdd
	jmp clrBlitRollO

; Construct the pixel decoding tables
makeDecodeTbls: !zone
	ldx #0
.shiftA:		; bit 5 controls the high bit (orange/blue vs green/purple)
	txa
	asl
	asl
	and #$80
	sta tmp+1
			; extract only bits 1 and 3 for the pixel data
	txa
	and #$0a	; bits 3 and 1
	lsr
	lsr		; bit 1 -> carry
	adc #0
.decodeTo01:
	ora tmp+1
	sta decodeTo01,x
.decodeTo01b:		; put hi bit in bit 6 instead of 7
	bpl +
	ora #$40
+	sta decodeTo01b,x
.decodeTo23:
	asl
	asl
	ora tmp+1
	sta decodeTo23,x
.decodeTo23b:		; put hi bit in bit 6 instead of 7
	bpl +
	ora #$40
+	sta decodeTo23b,x
.decodeTo45:
	asl
	asl
	ora tmp+1
	sta decodeTo45,x
.decodeTo56:
	asl
	ora tmp+1
	sta decodeTo56,x
.decodeTo57:
	asl
	asl
	php
	lsr
	plp
	ror
	sta decodeTo57,x
.next:	inx
	bne .shiftA
	rts

; Build table of screen line pointers
; on aux zero-page
makeLines: !zone
	lda #0
	sta lineCt
	lda #<TOP_LINE
	sta pLine
	lda #>TOP_LINE
	sta pLine+1
	sei
.lup:	lda lineCt
	asl
	tax
	lda pLine
	ldy pLine+1
	sta setAuxZP
	sta 0,x
	sty 1,x
	sta clrAuxZP
	jsr nextLine
	inc lineCt
	lda lineCt
	cmp #NLINES
	bne .lup
	cli
	rts

; Set screen lines to current back buf
setBackBuf: !zone
; calculate screen start
	lda backBuf
	asl
	asl
	asl
	asl
	asl
	clc
	adc #$20
	sei
	sta setAuxZP
	ldx #0
.lup:	eor 1,x
	and #$60	; only two bits control the screen buf
	eor 1,x
	sta 1,x
	inx
	inx
	bne .lup
	sta clrAuxZP
	cli
	rts

;-------------------------------------------------------------------------------
setExpansionCaller:
	; Copy the expansion caller to low stack.
	ldx #.callEnd - .callIt - 1
	sei
	sta setAuxZP
-	lda .callIt,x
	sta callExpander,x
	dex
	bpl -
	sta clrAuxZP
	cli
	rts
.callIt:
!pseudopc $100 {
callExpander:
	sta setAuxRd
	jsr expanderJmp
	sta clrAuxRd
	rts
expanderJmp:
	jmp (expandVec)
}
.callEnd:

;-------------------------------------------------------------------------------
getTileFlags: !zone
	dex			; because tile numbers start at 1 but list at 0
	lda $1111,x
	rts

;-------------------------------------------------------------------------------
; Parse map header, and load the textures into aux mem. Also, loads the script
; module, and if A-reg is nonzero, inits it.
loadTextures: !zone
	!if DEBUG { +prStr : !text "Loading textures.",0 }
	; Save parameter (non-zero: init scripts)
	pha
	; Scan the map header
	lda mapHeader
	sta .get+1
	lda mapHeader+1
	sta .get+2
	jsr .get	; get map width
	sta mapWidth	; and save it
	jsr .get	; get map height
	sta mapHeight	; and save it
	jsr .get	; get script module num
	tay		; and get ready to load it
	lda #QUEUE_LOAD
	ldx #RES_TYPE_MODULE
	jsr mainLoader	; queue script to load
	stx .scInit+1	; store its location so we call its init...
	sty .scInit+2	; ...after it loads of course.
	stx scripts
	sty scripts+1
	lda #0		; now comes the list of textures.
	sta txNum
.lup:	jsr .get	; get texture resource number
	tay		; to Y for mem manager
	beq .done	; zero = end of texture list
	lda #QUEUE_LOAD
	ldx #RES_TYPE_TEXTURE
	jsr auxLoader	; we want textures in aux mem
	txa		; addr lo to A for safekeeping
	ldx txNum	; get current texture num
	sta texAddrLo,x	; save address lo
	tya
	sta texAddrHi,x	; save address hi
	inx		; get ready for next texture
	cpx #MAX_TEXTURES
	bne +
	+prChr 'T'
	brk		; barf out if too many textures
+	stx txNum
	jmp .lup
.done:	; end of texture numbers is the list of tile flags
	lda txNum
	sta nTextures
	lda .get+1
	sta getTileFlags+2
	lda .get+2
	sta getTileFlags+3
-	jsr .get	; skip over the flags now
	tay
	bne -
	; end of the texture flags is the base of the map data - record it
	lda .get+1
	sta mapBase
	lda .get+2
	sta mapBase+1
	; finalize the load, and close the queue because textures are the last thing
	; to load.
	lda #FINISH_LOAD
	ldx #0
	jsr mainLoader
	; finally, init the scripts.
	pla
	beq .fin
!if DEBUG { +prStr : !text "Calling script init ",0 : +prWord .scInit+1 : +crout }
	ldx plasmaStk
.scInit	jsr $1111		; self-modified earlier
!if DEBUG { +prStr : !text "Back from script init. ",0 }
.fin    rts
.get:	lda $1111
	inc .get+1
	bne +
	inc .get+2
+	rts

;-------------------------------------------------------------------------------
; Plasma interface to texture control: 1 to load textures, 0 to unload
pl_texControl: !zone {
	tax
	beq .unload
	lda #START_LOAD
	ldx #2		; textures are on disk 2
	jsr mainLoader
	lda #0		; don't re-init scripts
	jmp loadTextures
.unload
-	txa
	pha
	ldy texAddrHi,x
	lda texAddrLo,x
	tax
	lda #FREE_MEMORY
	jsr auxLoader
	pla
	tax
	lda #0
	sta texAddrLo,x
	sta texAddrHi,x
	inx
	cpx nTextures
	bne -
	rts
}

;-------------------------------------------------------------------------------
; Set up front and back buffers, go to hires mode, and clear for first blit.
graphInit: !zone
	lda #0
	sta frontBuf
	lda #1
	sta backBuf
!if DEBUG >= 2 {
	+prStr : !text "Staying in text mode.",0
} else {
	bit clrText
	bit setHires
	bit clrMixed
}
	rts

;-------------------------------------------------------------------------------
; Using the current coordinates, calculate pointer on the map to the current row
; and put it in mapRayOrigin (and also A=lo, Y=hi)
calcMapOrigin: !zone

	lda mapBase		; start at row 0, col 0 of the map
	ldy mapBase+1
	ldx playerY+1		; integer part of player's Y coord
	beq .gotMapRow
	clc
.mapLup:			; advance forward one row
	adc mapWidth
	bcc +
	iny
	clc
+	dex			; until we reach players Y coord
	bne .mapLup
.gotMapRow:
	sta mapRayOrigin
	sty mapRayOrigin+1
	rts

;-------------------------------------------------------------------------------
; Advance in current direction if not blocked. 
; Params: none
; Return: 0 if blocked;
;	  1 if advanced but still within same map tile;
;         2 if pos is on a new map tile;
;         3 if that new tile is also scripted
pl_advance: !zone
	lda playerDir
	asl
	asl			; shift twice: each dir is 4 bytes in table
	tax

	; Advance the coordinates based on the direction.
	; Along the way, we save each one on the stack for later compare or restore
	lda playerX
	pha
	clc
	adc walkDirs,x
	sta playerX
	lda playerX+1
	pha
	adc walkDirs+1,x
	sta playerX+1

	lda playerY
	pha
	clc
	adc walkDirs+2,x
	sta playerY
	lda playerY+1
	pha
	adc walkDirs+3,x
	sta playerY+1

	; Check if the new position is blocked
	jsr calcMapOrigin
	sta pMap
	sty pMap+1
	ldy playerX+1
	lda (pMap),y
	and #$1F
	beq .ok			; empty tiles are never blocked
	tax
	jsr getTileFlags
	sta tmp+1
	and #2			; tile flag 2 is for obstructions
	beq .ok
	; Blocked! Restore old position.
	+prStr : !text "Blocked.", 0
	pla
	sta playerY+1
	pla
	sta playerY
	pla
	sta playerX+1
	pla
	sta playerX
	ldy #0
	beq .done
.ok	; Not blocked. See if we're in a new map tile.
	pla
	eor playerY+1
	sta tmp
	pla
	pla
	eor playerX+1
	ora tmp
	tay
	pla
	tya
	bne +
	iny			; not a new map tile, return 1
	bne .done		; always taken
+	; It is a new map tile. Is script hint set?
	ldy playerX+1
	lda (pMap),y
	ldy #2			; ret val 2 = new blk but no script
	and #$20		; map flag $20 is the script hint
	beq .done		; if not scripted, return one
	iny			; else return 3 = new blk and a script
.done	tya			; retrieve ret value
	ldy #0			; hi byte of ret is always 0
	rts			; all done

;-------------------------------------------------------------------------------
; Render at the current position and direction.
; Params: none
; Return: none
pl_render: !zone
	jsr setExpansionCaller	; $100 area is often overwritten
	lda $2000		; check if hgr2 was overwritten by mem mgr
	cmp $4000
	bne +
	lda $2001
	cmp $4001
	beq ++
+	jsr copyScreen		; if it was, restore by copying hgr1 to hgr2
	jsr makeLines
++	jmp renderFrame		; then go ahead and render

;-------------------------------------------------------------------------------
; Cast all the rays from the current player coord
castAllRays: !zone

	; Initialize pointer into precalculated ray table, based on player dir.
	; The table has 256 bytes per direction.
	ldx #0
	stx pRayData
	stx nMapSprites		; clear this while we've got zero in a register
	lda playerDir
	clc
	adc #>precast_0
	sta pRayData+1

	; start at column zero
	stx screenCol
	
	; clear the initial column links
	txa
	ldx #NUM_COLS-1
-	sta firstLink,x
	sta spriteCtBuf,x
	dex
	bpl -
	
	; start allocating column links at #1 (zero would be bad - used to indicate ends of lists)
	lda #1
	sta nextLink

	; Calculate pointer to the map row based on playerY
	jsr calcMapOrigin

	; Calculate the height, depth, texture number, and texture column for one ray
	; [ref BigBlue3_50]
	lda screenCol		; calculate ray offset...
.oneCol:
	asl			; as screen column * 4
	asl
	tay
	lda mapRayOrigin	; set initial map pointer for the ray
	sta pMap
	lda mapRayOrigin+1
	sta pMap+1

	jsr castRay		; cast the ray across the map

	inc screenCol		; advance to next column
	lda screenCol
	cmp #NUM_COLS		; stop after we do 63 columns = 126 bw pix
	bne .oneCol
	; now that we're done tracing rays, we need to reset the sprite flags.
.resetMapSprites:
	ldx #0			; index the table with X
	stx pMap
.rstLup	cpx nMapSprites		; are we done yet?
	bcs .done		; if so stop.
	ldy mapSpriteL,x	; grab lo byte of ptr, stick in Y to index the page
	lda mapSpriteH,x	; grab hi byte of ptr
	sta pMap+1
	lda (pMap),y		; get the sprite byte
	and #$BF		; mask off the already-done bit
	sta (pMap),y		; and save it back
	inx			; next table entry
	bne .rstLup		; always taken
.done	rts

;-------------------------------------------------------------------------------
; Reset sprite flags on the map

;-------------------------------------------------------------------------------
; Render one whole frame
renderFrame: !zone
	jsr setBackBuf

	jsr castAllRays

	lda #0
	sta pixNum
	sta byteNum
	sta screenCol

.oneCol:
	lda pixNum
	bne +
	jsr clearBlit		; clear blit on the first pixel
+	jsr drawRay		; and draw the ray
	!if DEBUG >= 2 { +prStr : !text "Done drawing ray ",0 : +prByte screenCol : +crout }
	inc screenCol		; next column
	inc pixNum		; do we need to flush the pixel buffer?
	lda pixNum
	cmp #7
	bne .nextCol		; not yet
.flush:	; flush the blit
	!if DEBUG >= 2 { +prStr : !text "Flushing.",0 }
	ldy byteNum
	iny			; move to right 2 bytes to preserve frame border
	iny
	sei
	sta setAuxZP
	lda shadow_pTex		; restore screen addr that gets overwritten by expander
	sta pTex		
	lda shadow_pTex+1
	sta pTex+1
	jsr blitRoll		; go do the blitting
	sta clrAuxZP
	cli
	lda #0
	sta pixNum
	inc byteNum
	inc byteNum
.nextCol:
	lda byteNum
	cmp #18
	bne .oneCol		; go back for another ray
	jmp flip		; flip it onto the screen

;-------------------------------------------------------------------------------
; Called by PLASMA code to ensure that hi res page 1 is showing. Usually in
; preparation for doing memory manager work (mem mgr uses hi res page 2)
pl_flipToPage1: !zone
	lda frontBuf
	bne +
	rts
+	jmp renderFrame		; so that page 1 has updated graphics on it.

;-------------------------------------------------------------------------------
; Flip back buffer onto the screen
flip: !zone
	ldy backBuf
	lda frontBuf
	sta backBuf
	sty frontBuf
	lda page1,y
	rts

;-------------------------------------------------------------------------------
copyScreen: !zone
	; Copy all screen data from page 1 to page 2
	ldy #0
	ldx #$20
.outer:	stx .inr1+2
	txa
	eor #$60	; page 1 -> page 2
	sta .inr2+2
.inr1:	lda $2000,y
.inr2:	sta $4000,y
	iny
	bne .inr1
	inx
	cpx #$40
	bne .outer
	rts

;-------------------------------------------------------------------------------
; Called by PLASMA code to get the position on the map.
; Parameters: @x, @y
; Returns: Nothing (but stores into the addressed variables)
pl_getPos: !zone {
	ldy playerY+1
	dey			; adjust for border guards
	tya
	jsr .sto
	inx
	ldy playerX+1
	dey			; adjust for border guards
	tya
	; Now fall thru, and exit with X incremented once (2 params - 1 return slot = 1)
.sto	ldy evalStkL,x
	sty pTmp
	ldy evalStkH,x
	sty pTmp+1
	ldy #0
	sta (pTmp),y
	tya
	iny
	sta (pTmp),y
	rts
}

;-------------------------------------------------------------------------------
; Called by PLASMA code to set the position on the map.
; Parameters: x, y
; Returns: Nothing
pl_setPos: !zone {
	lda evalStkL,x		; normally handled by asmplasm, but we're also called by pl_initMap
	clc
	adc #1			; adjust for border guards
	sta playerY+1
	lda evalStkL+1,x
	clc
	adc #1			; adjust for border guards
	sta playerX+1
	lda #$80
	sta playerY
	sta playerX
	rts
}

;-------------------------------------------------------------------------------
; Called by PLASMA code to get the player's direction
; Parameters: dir (0-15)
; Returns: Nothing
pl_getDir: !zone {
	lda playerDir
	ldy #0
	rts
}

;-------------------------------------------------------------------------------
; Called by PLASMA code to set the player's direction
; Parameters: dir (0-15)
; Returns: Nothing
pl_setDir: !zone {
	lda evalStkL,x		; normally handled by asmplasm, but we're also called by pl_initMap
	and #15
	sta playerDir
	rts
}

;-------------------------------------------------------------------------------
pl_setColor: !zone
	tay			; color number
	lda evalStkL+1,x
	and #1
	asl
	tax			; slot
	lda skyGndTbl1,y
	sta skyColorEven,x
	lda skyGndTbl2,y
	sta skyColorOdd,x
	rts

;-------------------------------------------------------------------------------
; Called by PLASMA code to get the currently loaded scripts module
; Parameters: None
; Returns: A pointer to the loaded script module
pl_getScripts: !zone {
	lda scripts
	ldy scripts+1
	rts
}

;-------------------------------------------------------------------------------
; Called by PLASMA code to set the avatar tile. Ignored by 3D engine.
; Parameters: A = tile number
; Returns: Nothing
pl_setAvatar: !zone {
	rts
}

;-------------------------------------------------------------------------------
; The real action
pl_initMap: !zone
	; Figure out PLASMA stack for calling script init
	txa
	clc
	adc #5			; 5 params
	sta plasmaStk		; save PLASMA's eval stack pos, without our params
	; Record the address of the map
	lda evalStkL+3,x
	sta mapHeader
	lda evalStkH+3,x
	sta mapHeader+1
	; Record player X, Y and dir
	jsr pl_setDir
	inx
	jsr pl_setPos
	; Reserve memory for all our tables.
	lda #SET_MEM_TARGET
	ldx #<tableStart
	ldy #>tableStart
	jsr mainLoader
	lda #REQUEST_MEMORY
	ldx #<(tableEnd-tableStart)
	ldy #>(tableEnd-tableStart)
	jsr mainLoader
	; If expander hasn't been split and relocated yet, do so now
	jsr splitExpander
	; Proceed with loading
	lda #1			; non-zero to init scripts also
	jsr loadTextures
	jsr copyScreen
	; Build all the unrolls and tables
	!if DEBUG { +prStr : !text "Making tables.",0 }
	jsr makeBlit
	jsr makeClrBlit
	jsr makeDecodeTbls
	jsr makeLines
	jsr setExpansionCaller
	jsr graphInit
	jmp renderFrame

splitExpander:
	lda expanderRelocd
	bne .done		; only relocate once
	jsr setExpansionCaller
	sei			; prevent interrupts while in aux mem
	sta setAuxZP
	sta setAuxWr
	lda setLcRW+lcBank2	; reloc to bank 2 in aux mem (unused by anything else)
	lda setLcRW+lcBank2	; second access to make it read/write
	jsr callExpander	; was copied from .callIt to $100 at init time
	sta clrAuxWr
	sta clrAuxZP
	cli			; interrupts ok after we get back from aux
	pha			; save new seg length
	txa
	pha
	; Now truncate the main segment of the expander, freeing up the
	; split space for textures and other resources.
	lda #FREE_MEMORY
	jsr .memexp
	lda #SET_MEM_TARGET
	jsr .memexp
	pla
	tay
	pla
	tax
	lda #REQUEST_MEMORY
	jsr auxLoader
	lda #LOCK_MEMORY
	jsr .memexp
	inc expanderRelocd
.done	rts
.memexp	ldx #<expandVec
	ldy #>expandVec
	jmp auxLoader

; Following are log/pow lookup tables. For speed, align them on a page boundary.
	!align 255,0

; Table to translate an unsigned byte to 3+5 bit fixed point log2 [ref BigBlue3_20]
tbl_log2_b_b:
	!byte $00,$00,$00,$00,$00,$07,$0C,$11,$15,$19,$1C,$1F,$22,$24,$27,$29
	!byte $2B,$2D,$2E,$30,$32,$33,$34,$36,$37,$38,$3A,$3B,$3C,$3D,$3E,$3F
	!byte $40,$41,$42,$43,$44,$44,$45,$46,$47,$48,$48,$49,$4A,$4A,$4B,$4C
	!byte $4C,$4D,$4E,$4E,$4F,$50,$50,$51,$51,$52,$52,$53,$53,$54,$54,$55
	!byte $55,$56,$56,$57,$57,$58,$58,$59,$59,$59,$5A,$5A,$5B,$5B,$5B,$5C
	!byte $5C,$5D,$5D,$5D,$5E,$5E,$5E,$5F,$5F,$5F,$60,$60,$61,$61,$61,$61
	!byte $62,$62,$62,$63,$63,$63,$64,$64,$64,$65,$65,$65,$65,$66,$66,$66
	!byte $67,$67,$67,$67,$68,$68,$68,$68,$69,$69,$69,$69,$6A,$6A,$6A,$6A
	!byte $6B,$6B,$6B,$6B,$6C,$6C,$6C,$6C,$6D,$6D,$6D,$6D,$6D,$6E,$6E,$6E
	!byte $6E,$6F,$6F,$6F,$6F,$6F,$70,$70,$70,$70,$70,$71,$71,$71,$71,$71
	!byte $72,$72,$72,$72,$72,$72,$73,$73,$73,$73,$73,$74,$74,$74,$74,$74
	!byte $74,$75,$75,$75,$75,$75,$75,$76,$76,$76,$76,$76,$76,$77,$77,$77
	!byte $77,$77,$77,$78,$78,$78,$78,$78,$78,$79,$79,$79,$79,$79,$79,$79
	!byte $7A,$7A,$7A,$7A,$7A,$7A,$7A,$7B,$7B,$7B,$7B,$7B,$7B,$7B,$7C,$7C
	!byte $7C,$7C,$7C,$7C,$7C,$7D,$7D,$7D,$7D,$7D,$7D,$7D,$7D,$7E,$7E,$7E
	!byte $7E,$7E,$7E,$7E,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F

; Table to translate 3+5 bit fixed point log2 back to an unsigned byte
tbl_pow2_b_b:
	!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
	!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
	!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
	!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
	!byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
	!byte $00,$00,$00,$00,$00,$00,$01,$01,$01,$01,$01,$01,$01,$01,$01,$01
	!byte $01,$01,$01,$01,$01,$01,$01,$01,$01,$01,$01,$02,$02,$02,$02,$02
	!byte $02,$02,$02,$02,$02,$02,$02,$02,$03,$03,$03,$03,$03,$03,$03,$03
	!byte $04,$04,$04,$04,$04,$04,$04,$05,$05,$05,$05,$05,$05,$06,$06,$06
	!byte $06,$06,$07,$07,$07,$07,$08,$08,$08,$09,$09,$09,$09,$0A,$0A,$0A
	!byte $0B,$0B,$0C,$0C,$0C,$0D,$0D,$0E,$0E,$0F,$0F,$10,$10,$11,$11,$12
	!byte $13,$13,$14,$14,$15,$16,$17,$17,$18,$19,$1A,$1B,$1C,$1D,$1D,$1E
	!byte $20,$21,$22,$23,$24,$25,$26,$28,$29,$2A,$2C,$2D,$2F,$30,$32,$34
	!byte $35,$37,$39,$3B,$3D,$3F,$41,$43,$45,$48,$4A,$4C,$4F,$52,$54,$57
	!byte $5A,$5D,$60,$63,$67,$6A,$6D,$71,$75,$79,$7D,$81,$85,$8A,$8E,$93
	!byte $98,$9D,$A2,$A7,$AD,$B3,$B8,$BF,$C5,$CB,$D2,$D9,$E0,$E8,$EF,$F7

; Table to translate 8-bit mantissa to fractional part of log2
tbl_log2_w_w:
	!byte $00,$01,$02,$04,$05,$07,$08,$09,$0B,$0C,$0E,$0F,$10,$12,$13,$15
	!byte $16,$17,$19,$1A,$1B,$1D,$1E,$1F,$21,$22,$23,$25,$26,$27,$28,$2A
	!byte $2B,$2C,$2E,$2F,$30,$31,$33,$34,$35,$36,$38,$39,$3A,$3B,$3D,$3E
	!byte $3F,$40,$41,$43,$44,$45,$46,$47,$49,$4A,$4B,$4C,$4D,$4E,$50,$51
	!byte $52,$53,$54,$55,$57,$58,$59,$5A,$5B,$5C,$5D,$5E,$60,$61,$62,$63
	!byte $64,$65,$66,$67,$68,$69,$6A,$6C,$6D,$6E,$6F,$70,$71,$72,$73,$74
	!byte $75,$76,$77,$78,$79,$7A,$7B,$7C,$7D,$7E,$7F,$80,$81,$83,$84,$85
	!byte $86,$87,$88,$89,$8A,$8B,$8C,$8C,$8D,$8E,$8F,$90,$91,$92,$93,$94
	!byte $95,$96,$97,$98,$99,$9A,$9B,$9C,$9D,$9E,$9F,$A0,$A1,$A2,$A2,$A3
	!byte $A4,$A5,$A6,$A7,$A8,$A9,$AA,$AB,$AC,$AD,$AD,$AE,$AF,$B0,$B1,$B2
	!byte $B3,$B4,$B5,$B5,$B6,$B7,$B8,$B9,$BA,$BB,$BC,$BC,$BD,$BE,$BF,$C0
	!byte $C1,$C2,$C2,$C3,$C4,$C5,$C6,$C7,$C8,$C8,$C9,$CA,$CB,$CC,$CD,$CD
	!byte $CE,$CF,$D0,$D1,$D1,$D2,$D3,$D4,$D5,$D6,$D6,$D7,$D8,$D9,$DA,$DA
	!byte $DB,$DC,$DD,$DE,$DE,$DF,$E0,$E1,$E1,$E2,$E3,$E4,$E5,$E5,$E6,$E7
	!byte $E8,$E8,$E9,$EA,$EB,$EB,$EC,$ED,$EE,$EF,$EF,$F0,$F1,$F2,$F2,$F3
	!byte $F4,$F5,$F5,$F6,$F7,$F7,$F8,$F9,$FA,$FA,$FB,$FC,$FD,$FD,$FE,$FF

; Table to translate fractional part of log2 back to 8-bit mantissa
tbl_pow2_w_w:
	!byte $00,$00,$01,$02,$02,$03,$04,$04,$05,$06,$07,$07,$08,$09,$09,$0A
	!byte $0B,$0C,$0C,$0D,$0E,$0E,$0F,$10,$11,$11,$12,$13,$14,$14,$15,$16
	!byte $17,$17,$18,$19,$1A,$1A,$1B,$1C,$1D,$1E,$1E,$1F,$20,$21,$21,$22
	!byte $23,$24,$25,$25,$26,$27,$28,$29,$29,$2A,$2B,$2C,$2D,$2D,$2E,$2F
	!byte $30,$31,$32,$32,$33,$34,$35,$36,$37,$37,$38,$39,$3A,$3B,$3C,$3D
	!byte $3D,$3E,$3F,$40,$41,$42,$43,$44,$44,$45,$46,$47,$48,$49,$4A,$4B
	!byte $4C,$4C,$4D,$4E,$4F,$50,$51,$52,$53,$54,$55,$56,$57,$57,$58,$59
	!byte $5A,$5B,$5C,$5D,$5E,$5F,$60,$61,$62,$63,$64,$65,$66,$67,$68,$69
	!byte $6A,$6B,$6C,$6D,$6E,$6F,$70,$71,$72,$73,$74,$75,$76,$77,$78,$79
	!byte $7A,$7B,$7C,$7D,$7E,$7F,$80,$81,$82,$83,$84,$85,$86,$87,$88,$89
	!byte $8A,$8C,$8D,$8E,$8F,$90,$91,$92,$93,$94,$95,$96,$97,$99,$9A,$9B
	!byte $9C,$9D,$9E,$9F,$A0,$A2,$A3,$A4,$A5,$A6,$A7,$A8,$AA,$AB,$AC,$AD
	!byte $AE,$AF,$B1,$B2,$B3,$B4,$B5,$B6,$B8,$B9,$BA,$BB,$BC,$BE,$BF,$C0
	!byte $C1,$C3,$C4,$C5,$C6,$C7,$C9,$CA,$CB,$CC,$CE,$CF,$D0,$D1,$D3,$D4
	!byte $D5,$D7,$D8,$D9,$DA,$DC,$DD,$DE,$E0,$E1,$E2,$E4,$E5,$E6,$E7,$E9
	!byte $EA,$EB,$ED,$EE,$EF,$F1,$F2,$F4,$F5,$F6,$F8,$F9,$FA,$FC,$FD,$FF

; Precalculated ray initialization parameters. One table for each of the 16 angles.
; Each angle has 63 rays, and each ray is provided with 4 parameters (one byte each param):
; dirX, dirY, deltaX, deltaY. [ref BigBlue3_30]
precast_0:
	!byte $72,$C7,$3E,$7C
	!byte $72,$C9,$3D,$7E
	!byte $72,$CB,$2C,$5E
	!byte $72,$CD,$39,$7E
	!byte $72,$CE,$30,$6E
	!byte $72,$D0,$35,$7E
	!byte $72,$D2,$33,$7E
	!byte $72,$D4,$31,$7E
	!byte $72,$D6,$2F,$7E
	!byte $72,$D7,$28,$70
	!byte $72,$D9,$2B,$7E
	!byte $72,$DB,$29,$7E
	!byte $72,$DD,$27,$7E
	!byte $72,$DF,$25,$7E
	!byte $72,$E0,$23,$7E
	!byte $72,$E2,$21,$7E
	!byte $72,$E4,$1F,$7E
	!byte $72,$E6,$1D,$7E
	!byte $72,$E8,$18,$70
	!byte $72,$E9,$19,$7E
	!byte $72,$EB,$17,$7E
	!byte $72,$ED,$14,$78
	!byte $72,$EF,$13,$7E
	!byte $72,$F1,$11,$7E
	!byte $72,$F2,$0F,$7E
	!byte $72,$F4,$0D,$7E
	!byte $72,$F6,$0B,$7E
	!byte $72,$F8,$09,$7E
	!byte $72,$FA,$07,$7E
	!byte $72,$FB,$05,$7E
	!byte $72,$FD,$03,$7E
	!byte $72,$FF,$01,$7E
	!byte $72,$01,$01,$7F
	!byte $72,$03,$03,$7E
	!byte $72,$05,$04,$65
	!byte $72,$06,$07,$7E
	!byte $72,$08,$09,$7F
	!byte $72,$0A,$0B,$7E
	!byte $72,$0C,$0A,$61
	!byte $72,$0E,$0F,$7E
	!byte $72,$0F,$0C,$59
	!byte $72,$11,$13,$7E
	!byte $72,$13,$15,$7F
	!byte $72,$15,$17,$7E
	!byte $72,$17,$18,$79
	!byte $72,$18,$18,$70
	!byte $72,$1A,$1A,$71
	!byte $72,$1C,$1F,$7E
	!byte $72,$1E,$1C,$6B
	!byte $72,$20,$23,$7E
	!byte $72,$21,$20,$6D
	!byte $72,$23,$27,$7E
	!byte $72,$25,$1B,$53
	!byte $72,$27,$2B,$7E
	!byte $72,$29,$28,$70
	!byte $72,$2A,$2F,$7E
	!byte $72,$2C,$23,$5A
	!byte $72,$2E,$33,$7E
	!byte $72,$30,$2D,$6B
	!byte $72,$32,$30,$6E
	!byte $72,$33,$34,$73
	!byte $72,$35,$2C,$5E
	!byte $72,$37,$3D,$7E
	!fill 4	; to bring it up to 256 bytes per angle
precast_1:
	!byte $7F,$F7,$09,$7F
	!byte $7E,$F9,$05,$56
	!byte $7E,$FA,$05,$6F
	!byte $7D,$FC,$04,$7D
	!byte $7C,$FE,$02,$6A
	!byte $7B,$FF,$00,$7F
	!byte $7B,$01,$01,$7C
	!byte $7A,$03,$02,$5C
	!byte $79,$04,$04,$71
	!byte $79,$06,$06,$79
	!byte $78,$08,$06,$5E
	!byte $77,$09,$09,$73
	!byte $77,$0B,$09,$61
	!byte $76,$0D,$0D,$79
	!byte $75,$0E,$0B,$5A
	!byte $75,$10,$0E,$66
	!byte $74,$12,$12,$76
	!byte $73,$13,$15,$7D
	!byte $72,$15,$14,$6D
	!byte $72,$17,$19,$7E
	!byte $71,$18,$0E,$41
	!byte $70,$1A,$19,$6C
	!byte $70,$1C,$1D,$75
	!byte $6F,$1D,$20,$79
	!byte $6E,$1F,$1B,$60
	!byte $6E,$21,$22,$72
	!byte $6D,$22,$23,$6F
	!byte $6C,$24,$2A,$7F
	!byte $6C,$26,$22,$61
	!byte $6B,$27,$2A,$72
	!byte $6A,$29,$2E,$77
	!byte $69,$2B,$33,$7E
	!byte $69,$2C,$24,$55
	!byte $68,$2E,$2E,$68
	!byte $67,$30,$35,$73
	!byte $67,$31,$32,$68
	!byte $66,$33,$3F,$7E
	!byte $65,$35,$40,$7B
	!byte $65,$36,$43,$7C
	!byte $64,$38,$3C,$6B
	!byte $63,$3A,$39,$62
	!byte $63,$3B,$2F,$4E
	!byte $62,$3D,$49,$75
	!byte $61,$3F,$47,$6E
	!byte $60,$40,$54,$7E
	!byte $60,$42,$50,$74
	!byte $5F,$44,$2F,$42
	!byte $5E,$45,$56,$75
	!byte $5E,$47,$5B,$78
	!byte $5D,$49,$56,$6E
	!byte $5C,$4A,$53,$67
	!byte $5C,$4C,$53,$64
	!byte $5B,$4E,$64,$75
	!byte $5A,$4F,$5F,$6C
	!byte $5A,$51,$56,$5F
	!byte $59,$53,$6C,$74
	!byte $58,$54,$70,$75
	!byte $58,$56,$7A,$7C
	!byte $57,$58,$5C,$5B
	!byte $56,$59,$6C,$68
	!byte $55,$5B,$71,$6A
	!byte $55,$5D,$68,$5F
	!byte $54,$5E,$64,$59
	!fill 4	; to bring it up to 256 bytes per angle
precast_2:
	!byte $79,$28,$2A,$7F
	!byte $77,$2A,$29,$76
	!byte $76,$2B,$2A,$74
	!byte $75,$2C,$1A,$45
	!byte $74,$2D,$28,$66
	!byte $72,$2F,$2A,$67
	!byte $71,$30,$24,$55
	!byte $70,$31,$33,$74
	!byte $6E,$32,$2A,$5C
	!byte $6D,$34,$35,$70
	!byte $6C,$35,$37,$70
	!byte $6B,$36,$39,$70
	!byte $69,$38,$3A,$6E
	!byte $68,$39,$42,$79
	!byte $67,$3A,$41,$73
	!byte $66,$3B,$45,$76
	!byte $64,$3D,$48,$77
	!byte $63,$3E,$4B,$78
	!byte $62,$3F,$42,$66
	!byte $60,$41,$54,$7E
	!byte $5F,$42,$2F,$44
	!byte $5E,$43,$57,$7A
	!byte $5D,$44,$4C,$67
	!byte $5B,$46,$60,$7E
	!byte $5A,$47,$55,$6C
	!byte $59,$48,$5B,$70
	!byte $57,$49,$5E,$70
	!byte $56,$4B,$6E,$7F
	!byte $55,$4C,$6E,$7B
	!byte $54,$4D,$6C,$75
	!byte $52,$4F,$66,$6B
	!byte $51,$50,$7D,$7F
	!byte $50,$51,$40,$3F
	!byte $4F,$52,$6C,$67
	!byte $4D,$54,$76,$6D
	!byte $4C,$55,$72,$66
	!byte $4B,$56,$71,$62
	!byte $49,$57,$51,$44
	!byte $48,$59,$7B,$64
	!byte $47,$5A,$7F,$64
	!byte $46,$5B,$7A,$5D
	!byte $44,$5D,$7E,$5D
	!byte $43,$5E,$7B,$58
	!byte $42,$5F,$51,$38
	!byte $41,$60,$7F,$55
	!byte $3F,$62,$77,$4D
	!byte $3E,$63,$7B,$4D
	!byte $3D,$64,$72,$45
	!byte $3B,$66,$5E,$37
	!byte $3A,$67,$7A,$45
	!byte $39,$68,$77,$41
	!byte $38,$69,$5B,$30
	!byte $36,$6B,$6E,$38
	!byte $35,$6C,$72,$38
	!byte $34,$6D,$74,$37
	!byte $32,$6E,$7F,$3A
	!byte $31,$70,$6D,$30
	!byte $30,$71,$5C,$27
	!byte $2F,$72,$4C,$1F
	!byte $2D,$74,$4F,$1F
	!byte $2C,$75,$72,$2B
	!byte $2B,$76,$7F,$2E
	!byte $2A,$77,$73,$28
	!fill 4	; to bring it up to 256 bytes per angle
precast_3:
	!byte $60,$53,$62,$71
	!byte $5E,$54,$61,$6D
	!byte $5D,$55,$74,$7F
	!byte $5B,$55,$69,$70
	!byte $59,$56,$67,$6B
	!byte $58,$57,$5A,$5B
	!byte $56,$58,$7D,$7B
	!byte $54,$58,$5E,$5A
	!byte $53,$59,$66,$5F
	!byte $51,$5A,$4A,$43
	!byte $4F,$5A,$7D,$6E
	!byte $4E,$5B,$7C,$6A
	!byte $4C,$5C,$6A,$58
	!byte $4A,$5C,$43,$36
	!byte $49,$5D,$4E,$3D
	!byte $47,$5E,$74,$58
	!byte $45,$5E,$66,$4B
	!byte $44,$5F,$49,$34
	!byte $42,$60,$64,$45
	!byte $40,$60,$7F,$55
	!byte $3F,$61,$4F,$33
	!byte $3D,$62,$7D,$4E
	!byte $3B,$63,$49,$2C
	!byte $3A,$63,$6E,$40
	!byte $38,$64,$7B,$45
	!byte $36,$65,$6F,$3C
	!byte $35,$65,$62,$33
	!byte $33,$66,$7F,$40
	!byte $31,$67,$4F,$26
	!byte $30,$67,$75,$36
	!byte $2E,$68,$71,$32
	!byte $2C,$69,$76,$32
	!byte $2B,$69,$79,$31
	!byte $29,$6A,$4B,$1D
	!byte $27,$6B,$7A,$2D
	!byte $26,$6C,$4D,$1B
	!byte $24,$6C,$7E,$2A
	!byte $22,$6D,$72,$24
	!byte $21,$6E,$7C,$25
	!byte $1F,$6E,$67,$1D
	!byte $1D,$6F,$66,$1B
	!byte $1C,$70,$79,$1E
	!byte $1A,$70,$5F,$16
	!byte $18,$71,$74,$19
	!byte $17,$72,$7D,$19
	!byte $15,$72,$62,$12
	!byte $13,$73,$77,$14
	!byte $12,$74,$48,$0B
	!byte $10,$75,$50,$0B
	!byte $0E,$75,$62,$0C
	!byte $0D,$76,$5D,$0A
	!byte $0B,$77,$56,$08
	!byte $09,$77,$66,$08
	!byte $08,$78,$7D,$08
	!byte $06,$79,$78,$06
	!byte $04,$79,$70,$04
	!byte $03,$7A,$5B,$02
	!byte $01,$7B,$7B,$01
	!byte $FF,$7B,$5B,$00
	!byte $FE,$7C,$69,$02
	!byte $FC,$7D,$7C,$04
	!byte $FA,$7E,$6E,$05
	!byte $F9,$7E,$67,$06
	!fill 4	; to bring it up to 256 bytes per angle
precast_4:
	!byte $39,$72,$7F,$40
	!byte $37,$72,$5F,$2E
	!byte $35,$72,$7E,$3B
	!byte $33,$72,$5F,$2B
	!byte $32,$72,$47,$1F
	!byte $30,$72,$72,$30
	!byte $2E,$72,$54,$22
	!byte $2C,$72,$71,$2C
	!byte $2A,$72,$43,$19
	!byte $29,$72,$7B,$2C
	!byte $27,$72,$7B,$2A
	!byte $25,$72,$56,$1C
	!byte $23,$72,$71,$23
	!byte $21,$72,$77,$23
	!byte $20,$72,$73,$20
	!byte $1E,$72,$67,$1B
	!byte $1C,$72,$41,$10
	!byte $1A,$72,$75,$1B
	!byte $18,$72,$79,$1A
	!byte $17,$72,$7D,$19
	!byte $15,$72,$73,$15
	!byte $13,$72,$7D,$15
	!byte $11,$72,$6A,$10
	!byte $0F,$72,$6F,$0F
	!byte $0E,$72,$6D,$0D
	!byte $0C,$72,$74,$0C
	!byte $0A,$72,$67,$09
	!byte $08,$72,$7D,$09
	!byte $06,$72,$7D,$07
	!byte $05,$72,$7D,$05
	!byte $03,$72,$7D,$03
	!byte $01,$72,$7D,$01
	!byte $FF,$72,$7E,$01
	!byte $FD,$72,$7E,$03
	!byte $FB,$72,$7E,$05
	!byte $FA,$72,$7E,$07
	!byte $F8,$72,$7E,$09
	!byte $F6,$72,$7E,$0B
	!byte $F4,$72,$7E,$0D
	!byte $F2,$72,$7E,$0F
	!byte $F1,$72,$7E,$11
	!byte $EF,$72,$7E,$13
	!byte $ED,$72,$7E,$15
	!byte $EB,$72,$7E,$17
	!byte $E9,$72,$7E,$19
	!byte $E8,$72,$7E,$1B
	!byte $E6,$72,$7E,$1D
	!byte $E4,$72,$7E,$1F
	!byte $E2,$72,$7E,$21
	!byte $E0,$72,$7E,$23
	!byte $DF,$72,$7E,$25
	!byte $DD,$72,$7E,$27
	!byte $DB,$72,$7E,$29
	!byte $D9,$72,$7E,$2B
	!byte $D7,$72,$7E,$2D
	!byte $D6,$72,$7E,$2F
	!byte $D4,$72,$7E,$31
	!byte $D2,$72,$7E,$33
	!byte $D0,$72,$7E,$35
	!byte $CE,$72,$47,$1F
	!byte $CD,$72,$7E,$39
	!byte $CB,$72,$7E,$3B
	!byte $C9,$72,$7E,$3D
	!fill 4	; to bring it up to 256 bytes per angle
precast_5:
	!byte $09,$7F,$7E,$09
	!byte $07,$7E,$67,$06
	!byte $06,$7E,$6E,$05
	!byte $04,$7D,$7C,$04
	!byte $02,$7C,$69,$02
	!byte $01,$7B,$5B,$00
	!byte $FF,$7B,$7B,$01
	!byte $FD,$7A,$5B,$02
	!byte $FC,$79,$70,$04
	!byte $FA,$79,$78,$06
	!byte $F8,$78,$7D,$08
	!byte $F7,$77,$66,$08
	!byte $F5,$77,$56,$08
	!byte $F3,$76,$5D,$0A
	!byte $F2,$75,$62,$0C
	!byte $F0,$75,$50,$0B
	!byte $EE,$74,$48,$0B
	!byte $ED,$73,$77,$14
	!byte $EB,$72,$62,$12
	!byte $E9,$72,$7D,$19
	!byte $E8,$71,$74,$19
	!byte $E6,$70,$5F,$16
	!byte $E4,$70,$79,$1E
	!byte $E3,$6F,$66,$1B
	!byte $E1,$6E,$67,$1D
	!byte $DF,$6E,$7C,$25
	!byte $DE,$6D,$72,$24
	!byte $DC,$6C,$7E,$2A
	!byte $DA,$6C,$4D,$1B
	!byte $D9,$6B,$7A,$2D
	!byte $D7,$6A,$4B,$1D
	!byte $D5,$69,$79,$31
	!byte $D4,$69,$76,$32
	!byte $D2,$68,$71,$32
	!byte $D0,$67,$75,$36
	!byte $CF,$67,$4F,$26
	!byte $CD,$66,$7F,$40
	!byte $CB,$65,$62,$33
	!byte $CA,$65,$6F,$3C
	!byte $C8,$64,$7B,$45
	!byte $C6,$63,$6E,$40
	!byte $C5,$63,$49,$2C
	!byte $C3,$62,$7D,$4E
	!byte $C1,$61,$4F,$33
	!byte $C0,$60,$7F,$55
	!byte $BE,$60,$64,$45
	!byte $BC,$5F,$49,$34
	!byte $BB,$5E,$66,$4B
	!byte $B9,$5E,$74,$58
	!byte $B7,$5D,$4E,$3D
	!byte $B6,$5C,$43,$36
	!byte $B4,$5C,$6A,$58
	!byte $B2,$5B,$7C,$6A
	!byte $B1,$5A,$7D,$6E
	!byte $AF,$5A,$4A,$43
	!byte $AD,$59,$66,$5F
	!byte $AC,$58,$5E,$5A
	!byte $AA,$58,$7D,$7B
	!byte $A8,$57,$5A,$5B
	!byte $A7,$56,$67,$6B
	!byte $A5,$55,$69,$70
	!byte $A3,$55,$74,$7F
	!byte $A2,$54,$61,$6D
	!fill 4	; to bring it up to 256 bytes per angle
precast_6:
	!byte $D8,$79,$7E,$2A
	!byte $D6,$77,$73,$28
	!byte $D5,$76,$7F,$2E
	!byte $D4,$75,$7A,$2E
	!byte $D3,$74,$4F,$1F
	!byte $D1,$72,$4C,$1F
	!byte $D0,$71,$76,$32
	!byte $CF,$70,$7D,$37
	!byte $CE,$6E,$7F,$3A
	!byte $CC,$6D,$72,$36
	!byte $CB,$6C,$72,$38
	!byte $CA,$6B,$6E,$38
	!byte $C8,$69,$6E,$3A
	!byte $C7,$68,$77,$41
	!byte $C6,$67,$73,$41
	!byte $C5,$66,$6A,$3E
	!byte $C3,$64,$72,$45
	!byte $C2,$63,$7B,$4D
	!byte $C1,$62,$77,$4D
	!byte $BF,$60,$7F,$55
	!byte $BE,$5F,$51,$38
	!byte $BD,$5E,$7E,$5A
	!byte $BC,$5D,$7E,$5D
	!byte $BA,$5B,$7A,$5D
	!byte $B9,$5A,$5E,$4A
	!byte $B8,$59,$7B,$64
	!byte $B7,$57,$51,$44
	!byte $B5,$56,$78,$68
	!byte $B4,$55,$72,$66
	!byte $B3,$54,$76,$6D
	!byte $B1,$52,$56,$52
	!byte $B0,$51,$7F,$7D
	!byte $AF,$50,$7D,$7F
	!byte $AE,$4F,$52,$56
	!byte $AC,$4D,$6C,$75
	!byte $AB,$4C,$66,$72
	!byte $AA,$4B,$68,$78
	!byte $A9,$49,$5E,$70
	!byte $A7,$48,$5B,$70
	!byte $A6,$47,$4A,$5E
	!byte $A5,$46,$60,$7E
	!byte $A3,$44,$4C,$67
	!byte $A2,$43,$5A,$7E
	!byte $A1,$42,$2F,$44
	!byte $A0,$41,$54,$7E
	!byte $9E,$3F,$4D,$77
	!byte $9D,$3E,$4B,$78
	!byte $9C,$3D,$48,$77
	!byte $9A,$3B,$3E,$6A
	!byte $99,$3A,$41,$73
	!byte $98,$39,$42,$79
	!byte $97,$38,$3A,$6E
	!byte $95,$36,$39,$70
	!byte $94,$35,$37,$70
	!byte $93,$34,$36,$72
	!byte $92,$32,$2A,$5C
	!byte $90,$31,$37,$7D
	!byte $8F,$30,$32,$76
	!byte $8E,$2F,$2A,$67
	!byte $8C,$2D,$28,$66
	!byte $8B,$2C,$2E,$7A
	!byte $8A,$2B,$2A,$74
	!byte $89,$2A,$29,$76
	!fill 4	; to bring it up to 256 bytes per angle
precast_7:
	!byte $AD,$60,$62,$55
	!byte $AC,$5E,$64,$59
	!byte $AB,$5D,$68,$5F
	!byte $AB,$5B,$71,$6A
	!byte $AA,$59,$6C,$68
	!byte $A9,$58,$5C,$5B
	!byte $A8,$56,$7A,$7C
	!byte $A8,$54,$70,$75
	!byte $A7,$53,$6C,$74
	!byte $A6,$51,$56,$5F
	!byte $A6,$4F,$5F,$6C
	!byte $A5,$4E,$64,$75
	!byte $A4,$4C,$53,$64
	!byte $A4,$4A,$53,$67
	!byte $A3,$49,$56,$6E
	!byte $A2,$47,$5B,$78
	!byte $A2,$45,$56,$75
	!byte $A1,$44,$2F,$42
	!byte $A0,$42,$50,$74
	!byte $A0,$40,$54,$7E
	!byte $9F,$3F,$47,$6E
	!byte $9E,$3D,$49,$75
	!byte $9D,$3B,$2F,$4E
	!byte $9D,$3A,$39,$62
	!byte $9C,$38,$3C,$6B
	!byte $9B,$36,$43,$7C
	!byte $9B,$35,$40,$7B
	!byte $9A,$33,$3F,$7E
	!byte $99,$31,$32,$68
	!byte $99,$30,$35,$73
	!byte $98,$2E,$2E,$68
	!byte $97,$2C,$24,$55
	!byte $97,$2B,$33,$7E
	!byte $96,$29,$2E,$77
	!byte $95,$27,$2A,$72
	!byte $94,$26,$22,$61
	!byte $94,$24,$2A,$7F
	!byte $93,$22,$23,$6F
	!byte $92,$21,$22,$72
	!byte $92,$1F,$1B,$60
	!byte $91,$1D,$20,$79
	!byte $90,$1C,$1D,$75
	!byte $90,$1A,$19,$6C
	!byte $8F,$18,$0E,$41
	!byte $8E,$17,$19,$7E
	!byte $8E,$15,$14,$6D
	!byte $8D,$13,$15,$7D
	!byte $8C,$12,$12,$76
	!byte $8B,$10,$0E,$66
	!byte $8B,$0E,$0B,$5A
	!byte $8A,$0D,$0D,$79
	!byte $89,$0B,$09,$61
	!byte $89,$09,$09,$73
	!byte $88,$08,$06,$5E
	!byte $87,$06,$06,$79
	!byte $87,$04,$04,$71
	!byte $86,$03,$02,$5C
	!byte $85,$01,$01,$7C
	!byte $85,$FF,$00,$7F
	!byte $84,$FE,$02,$6A
	!byte $83,$FC,$04,$7D
	!byte $82,$FA,$05,$6F
	!byte $82,$F9,$05,$56
	!fill 4	; to bring it up to 256 bytes per angle
precast_8:
	!byte $8E,$39,$3F,$7E
	!byte $8E,$37,$3D,$7E
	!byte $8E,$35,$3B,$7E
	!byte $8E,$33,$39,$7E
	!byte $8E,$32,$37,$7E
	!byte $8E,$30,$35,$7E
	!byte $8E,$2E,$33,$7E
	!byte $8E,$2C,$31,$7E
	!byte $8E,$2A,$2F,$7E
	!byte $8E,$29,$2D,$7E
	!byte $8E,$27,$2B,$7E
	!byte $8E,$25,$29,$7E
	!byte $8E,$23,$27,$7E
	!byte $8E,$21,$25,$7E
	!byte $8E,$20,$23,$7E
	!byte $8E,$1E,$21,$7E
	!byte $8E,$1C,$1F,$7E
	!byte $8E,$1A,$1D,$7E
	!byte $8E,$18,$1B,$7E
	!byte $8E,$17,$19,$7E
	!byte $8E,$15,$17,$7E
	!byte $8E,$13,$15,$7E
	!byte $8E,$11,$13,$7E
	!byte $8E,$0F,$11,$7E
	!byte $8E,$0E,$0F,$7E
	!byte $8E,$0C,$0D,$7E
	!byte $8E,$0A,$0B,$7E
	!byte $8E,$08,$09,$7E
	!byte $8E,$06,$07,$7E
	!byte $8E,$05,$05,$7E
	!byte $8E,$03,$03,$7E
	!byte $8E,$01,$01,$7E
	!byte $8E,$FF,$01,$7F
	!byte $8E,$FD,$03,$7F
	!byte $8E,$FB,$04,$65
	!byte $8E,$FA,$07,$7F
	!byte $8E,$F8,$09,$7F
	!byte $8E,$F6,$0A,$73
	!byte $8E,$F4,$0A,$61
	!byte $8E,$F2,$0C,$65
	!byte $8E,$F1,$0C,$59
	!byte $8E,$EF,$0B,$49
	!byte $8E,$ED,$15,$7F
	!byte $8E,$EB,$16,$79
	!byte $8E,$E9,$18,$79
	!byte $8E,$E8,$19,$75
	!byte $8E,$E6,$1A,$71
	!byte $8E,$E4,$1E,$7A
	!byte $8E,$E2,$1C,$6B
	!byte $8E,$E0,$21,$77
	!byte $8E,$DF,$20,$6D
	!byte $8E,$DD,$1E,$61
	!byte $8E,$DB,$1B,$53
	!byte $8E,$D9,$1D,$55
	!byte $8E,$D7,$29,$73
	!byte $8E,$D6,$2C,$76
	!byte $8E,$D4,$2F,$79
	!byte $8E,$D2,$24,$59
	!byte $8E,$D0,$2D,$6B
	!byte $8E,$CE,$30,$6E
	!byte $8E,$CD,$34,$73
	!byte $8E,$CB,$2C,$5E
	!byte $8E,$C9,$3C,$7C
	!fill 4	; to bring it up to 256 bytes per angle
precast_9:
	!byte $81,$09,$09,$7F
	!byte $82,$07,$05,$56
	!byte $82,$06,$05,$6F
	!byte $83,$04,$04,$7D
	!byte $84,$02,$02,$6A
	!byte $85,$01,$00,$7F
	!byte $85,$FF,$01,$7C
	!byte $86,$FD,$02,$5C
	!byte $87,$FC,$04,$71
	!byte $87,$FA,$06,$79
	!byte $88,$F8,$06,$5E
	!byte $89,$F7,$09,$73
	!byte $89,$F5,$09,$61
	!byte $8A,$F3,$0D,$79
	!byte $8B,$F2,$0B,$5A
	!byte $8B,$F0,$0E,$66
	!byte $8C,$EE,$12,$76
	!byte $8D,$ED,$15,$7D
	!byte $8E,$EB,$14,$6D
	!byte $8E,$E9,$19,$7E
	!byte $8F,$E8,$0E,$41
	!byte $90,$E6,$19,$6C
	!byte $90,$E4,$1D,$75
	!byte $91,$E3,$20,$79
	!byte $92,$E1,$1B,$60
	!byte $92,$DF,$22,$72
	!byte $93,$DE,$23,$6F
	!byte $94,$DC,$2A,$7F
	!byte $94,$DA,$22,$61
	!byte $95,$D9,$2A,$72
	!byte $96,$D7,$2E,$77
	!byte $97,$D5,$33,$7E
	!byte $97,$D4,$24,$55
	!byte $98,$D2,$2E,$68
	!byte $99,$D0,$35,$73
	!byte $99,$CF,$32,$68
	!byte $9A,$CD,$3F,$7E
	!byte $9B,$CB,$40,$7B
	!byte $9B,$CA,$43,$7C
	!byte $9C,$C8,$3C,$6B
	!byte $9D,$C6,$39,$62
	!byte $9D,$C5,$2F,$4E
	!byte $9E,$C3,$49,$75
	!byte $9F,$C1,$47,$6E
	!byte $A0,$C0,$54,$7E
	!byte $A0,$BE,$50,$74
	!byte $A1,$BC,$2F,$42
	!byte $A2,$BB,$56,$75
	!byte $A2,$B9,$5B,$78
	!byte $A3,$B7,$56,$6E
	!byte $A4,$B6,$53,$67
	!byte $A4,$B4,$53,$64
	!byte $A5,$B2,$64,$75
	!byte $A6,$B1,$5F,$6C
	!byte $A6,$AF,$56,$5F
	!byte $A7,$AD,$6C,$74
	!byte $A8,$AC,$70,$75
	!byte $A8,$AA,$7A,$7C
	!byte $A9,$A8,$5C,$5B
	!byte $AA,$A7,$6C,$68
	!byte $AB,$A5,$71,$6A
	!byte $AB,$A3,$68,$5F
	!byte $AC,$A2,$64,$59
	!fill 4	; to bring it up to 256 bytes per angle
precast_10:
	!byte $87,$D8,$2A,$7F
	!byte $89,$D6,$29,$76
	!byte $8A,$D5,$2A,$74
	!byte $8B,$D4,$1A,$45
	!byte $8C,$D3,$28,$66
	!byte $8E,$D1,$2A,$67
	!byte $8F,$D0,$24,$55
	!byte $90,$CF,$33,$74
	!byte $92,$CE,$2A,$5C
	!byte $93,$CC,$35,$70
	!byte $94,$CB,$37,$70
	!byte $95,$CA,$39,$70
	!byte $97,$C8,$27,$4A
	!byte $98,$C7,$42,$79
	!byte $99,$C6,$3D,$6C
	!byte $9A,$C5,$45,$76
	!byte $9C,$C3,$48,$77
	!byte $9D,$C2,$4B,$78
	!byte $9E,$C1,$4B,$74
	!byte $A0,$BF,$54,$7E
	!byte $A1,$BE,$2F,$44
	!byte $A2,$BD,$57,$7A
	!byte $A3,$BC,$4C,$67
	!byte $A5,$BA,$60,$7E
	!byte $A6,$B9,$55,$6C
	!byte $A7,$B8,$5B,$70
	!byte $A9,$B7,$5E,$70
	!byte $AA,$B5,$6E,$7F
	!byte $AB,$B4,$6E,$7B
	!byte $AC,$B3,$6C,$75
	!byte $AE,$B1,$66,$6B
	!byte $AF,$B0,$7C,$7E
	!byte $B0,$AF,$40,$3F
	!byte $B1,$AE,$6C,$67
	!byte $B3,$AC,$76,$6D
	!byte $B4,$AB,$7C,$6F
	!byte $B5,$AA,$71,$62
	!byte $B7,$A9,$51,$44
	!byte $B8,$A7,$7B,$64
	!byte $B9,$A6,$7F,$64
	!byte $BA,$A5,$7A,$5D
	!byte $BC,$A3,$7E,$5D
	!byte $BD,$A2,$7B,$58
	!byte $BE,$A1,$51,$38
	!byte $BF,$A0,$7F,$55
	!byte $C1,$9E,$7A,$4F
	!byte $C2,$9D,$7B,$4D
	!byte $C3,$9C,$72,$45
	!byte $C5,$9A,$5E,$37
	!byte $C6,$99,$7A,$45
	!byte $C7,$98,$77,$41
	!byte $C8,$97,$5B,$30
	!byte $CA,$95,$6E,$38
	!byte $CB,$94,$72,$38
	!byte $CC,$93,$74,$37
	!byte $CE,$92,$7F,$3A
	!byte $CF,$90,$6D,$30
	!byte $D0,$8F,$5C,$27
	!byte $D1,$8E,$4C,$1F
	!byte $D3,$8C,$4F,$1F
	!byte $D4,$8B,$72,$2B
	!byte $D5,$8A,$7F,$2E
	!byte $D6,$89,$73,$28
	!fill 4	; to bring it up to 256 bytes per angle
precast_11:
	!byte $A0,$AD,$62,$71
	!byte $A2,$AC,$61,$6D
	!byte $A3,$AB,$74,$7F
	!byte $A5,$AB,$69,$70
	!byte $A7,$AA,$67,$6B
	!byte $A8,$A9,$5A,$5B
	!byte $AA,$A8,$7D,$7B
	!byte $AC,$A8,$5E,$5A
	!byte $AD,$A7,$66,$5F
	!byte $AF,$A6,$4A,$43
	!byte $B1,$A6,$7D,$6E
	!byte $B2,$A5,$7C,$6A
	!byte $B4,$A4,$6A,$58
	!byte $B6,$A4,$43,$36
	!byte $B7,$A3,$4E,$3D
	!byte $B9,$A2,$74,$58
	!byte $BB,$A2,$66,$4B
	!byte $BC,$A1,$49,$34
	!byte $BE,$A0,$64,$45
	!byte $C0,$A0,$7F,$55
	!byte $C1,$9F,$4F,$33
	!byte $C3,$9E,$7D,$4E
	!byte $C5,$9D,$49,$2C
	!byte $C6,$9D,$6E,$40
	!byte $C8,$9C,$7B,$45
	!byte $CA,$9B,$6F,$3C
	!byte $CB,$9B,$62,$33
	!byte $CD,$9A,$7F,$40
	!byte $CF,$99,$4F,$26
	!byte $D0,$99,$75,$36
	!byte $D2,$98,$71,$32
	!byte $D4,$97,$76,$32
	!byte $D5,$97,$79,$31
	!byte $D7,$96,$4B,$1D
	!byte $D9,$95,$7A,$2D
	!byte $DA,$94,$4D,$1B
	!byte $DC,$94,$7E,$2A
	!byte $DE,$93,$72,$24
	!byte $DF,$92,$7C,$25
	!byte $E1,$92,$67,$1D
	!byte $E3,$91,$66,$1B
	!byte $E4,$90,$79,$1E
	!byte $E6,$90,$5F,$16
	!byte $E8,$8F,$74,$19
	!byte $E9,$8E,$7D,$19
	!byte $EB,$8E,$62,$12
	!byte $ED,$8D,$77,$14
	!byte $EE,$8C,$48,$0B
	!byte $F0,$8B,$50,$0B
	!byte $F2,$8B,$62,$0C
	!byte $F3,$8A,$5D,$0A
	!byte $F5,$89,$56,$08
	!byte $F7,$89,$66,$08
	!byte $F8,$88,$7D,$08
	!byte $FA,$87,$78,$06
	!byte $FC,$87,$70,$04
	!byte $FD,$86,$5B,$02
	!byte $FF,$85,$7B,$01
	!byte $01,$85,$5B,$00
	!byte $02,$84,$69,$02
	!byte $04,$83,$7C,$04
	!byte $06,$82,$6E,$05
	!byte $07,$82,$67,$06
	!fill 4	; to bring it up to 256 bytes per angle
precast_12:
	!byte $C7,$8E,$7F,$40
	!byte $C9,$8E,$5F,$2E
	!byte $CB,$8E,$4F,$25
	!byte $CD,$8E,$5F,$2B
	!byte $CE,$8E,$47,$1F
	!byte $D0,$8E,$72,$30
	!byte $D2,$8E,$79,$31
	!byte $D4,$8E,$71,$2C
	!byte $D6,$8E,$43,$19
	!byte $D7,$8E,$7B,$2C
	!byte $D9,$8E,$7B,$2A
	!byte $DB,$8E,$56,$1C
	!byte $DD,$8E,$71,$23
	!byte $DF,$8E,$77,$23
	!byte $E0,$8E,$73,$20
	!byte $E2,$8E,$67,$1B
	!byte $E4,$8E,$41,$10
	!byte $E6,$8E,$75,$1B
	!byte $E8,$8E,$79,$1A
	!byte $E9,$8E,$7D,$19
	!byte $EB,$8E,$73,$15
	!byte $ED,$8E,$7D,$15
	!byte $EF,$8E,$6A,$10
	!byte $F1,$8E,$6F,$0F
	!byte $F2,$8E,$6D,$0D
	!byte $F4,$8E,$74,$0C
	!byte $F6,$8E,$67,$09
	!byte $F8,$8E,$7D,$09
	!byte $FA,$8E,$7D,$07
	!byte $FB,$8E,$7D,$05
	!byte $FD,$8E,$7D,$03
	!byte $FF,$8E,$7D,$01
	!byte $01,$8E,$7E,$01
	!byte $03,$8E,$7E,$03
	!byte $05,$8E,$7E,$05
	!byte $06,$8E,$7E,$07
	!byte $08,$8E,$7E,$09
	!byte $0A,$8E,$7E,$0B
	!byte $0C,$8E,$7E,$0D
	!byte $0E,$8E,$7E,$0F
	!byte $0F,$8E,$7E,$11
	!byte $11,$8E,$7E,$13
	!byte $13,$8E,$7E,$15
	!byte $15,$8E,$7E,$17
	!byte $17,$8E,$7E,$19
	!byte $18,$8E,$7E,$1B
	!byte $1A,$8E,$7E,$1D
	!byte $1C,$8E,$7E,$1F
	!byte $1E,$8E,$7E,$21
	!byte $20,$8E,$7E,$23
	!byte $21,$8E,$7E,$25
	!byte $23,$8E,$7E,$27
	!byte $25,$8E,$7E,$29
	!byte $27,$8E,$7E,$2B
	!byte $29,$8E,$7E,$2D
	!byte $2A,$8E,$7E,$2F
	!byte $2C,$8E,$7E,$31
	!byte $2E,$8E,$7E,$33
	!byte $30,$8E,$7E,$35
	!byte $32,$8E,$7E,$37
	!byte $33,$8E,$7E,$39
	!byte $35,$8E,$7E,$3B
	!byte $37,$8E,$7E,$3D
	!fill 4	; to bring it up to 256 bytes per angle
precast_13:
	!byte $F7,$81,$7E,$09
	!byte $F9,$82,$67,$06
	!byte $FA,$82,$6E,$05
	!byte $FC,$83,$7C,$04
	!byte $FE,$84,$69,$02
	!byte $FF,$85,$5B,$00
	!byte $01,$85,$7B,$01
	!byte $03,$86,$5B,$02
	!byte $04,$87,$70,$04
	!byte $06,$87,$78,$06
	!byte $08,$88,$7D,$08
	!byte $09,$89,$66,$08
	!byte $0B,$89,$56,$08
	!byte $0D,$8A,$5D,$0A
	!byte $0E,$8B,$62,$0C
	!byte $10,$8B,$50,$0B
	!byte $12,$8C,$48,$0B
	!byte $13,$8D,$77,$14
	!byte $15,$8E,$62,$12
	!byte $17,$8E,$7D,$19
	!byte $18,$8F,$74,$19
	!byte $1A,$90,$5F,$16
	!byte $1C,$90,$79,$1E
	!byte $1D,$91,$66,$1B
	!byte $1F,$92,$67,$1D
	!byte $21,$92,$7C,$25
	!byte $22,$93,$72,$24
	!byte $24,$94,$7E,$2A
	!byte $26,$94,$4D,$1B
	!byte $27,$95,$7A,$2D
	!byte $29,$96,$4B,$1D
	!byte $2B,$97,$79,$31
	!byte $2C,$97,$76,$32
	!byte $2E,$98,$71,$32
	!byte $30,$99,$75,$36
	!byte $31,$99,$4F,$26
	!byte $33,$9A,$7F,$40
	!byte $35,$9B,$62,$33
	!byte $36,$9B,$6F,$3C
	!byte $38,$9C,$7B,$45
	!byte $3A,$9D,$6E,$40
	!byte $3B,$9D,$49,$2C
	!byte $3D,$9E,$7D,$4E
	!byte $3F,$9F,$4F,$33
	!byte $40,$A0,$7F,$55
	!byte $42,$A0,$64,$45
	!byte $44,$A1,$49,$34
	!byte $45,$A2,$66,$4B
	!byte $47,$A2,$74,$58
	!byte $49,$A3,$4E,$3D
	!byte $4A,$A4,$43,$36
	!byte $4C,$A4,$6A,$58
	!byte $4E,$A5,$7C,$6A
	!byte $4F,$A6,$7D,$6E
	!byte $51,$A6,$4A,$43
	!byte $53,$A7,$66,$5F
	!byte $54,$A8,$5E,$5A
	!byte $56,$A8,$7D,$7B
	!byte $58,$A9,$5A,$5B
	!byte $59,$AA,$67,$6B
	!byte $5B,$AB,$69,$70
	!byte $5D,$AB,$74,$7F
	!byte $5E,$AC,$61,$6D
	!fill 4	; to bring it up to 256 bytes per angle
precast_14:
	!byte $28,$87,$7E,$2A
	!byte $2A,$89,$73,$28
	!byte $2B,$8A,$7F,$2E
	!byte $2C,$8B,$7A,$2E
	!byte $2D,$8C,$4F,$1F
	!byte $2F,$8E,$4C,$1F
	!byte $30,$8F,$76,$32
	!byte $31,$90,$7D,$37
	!byte $32,$92,$7F,$3A
	!byte $34,$93,$72,$36
	!byte $35,$94,$72,$38
	!byte $36,$95,$6E,$38
	!byte $38,$97,$6E,$3A
	!byte $39,$98,$77,$41
	!byte $3A,$99,$73,$41
	!byte $3B,$9A,$6A,$3E
	!byte $3D,$9C,$72,$45
	!byte $3E,$9D,$7B,$4D
	!byte $3F,$9E,$77,$4D
	!byte $41,$A0,$7F,$55
	!byte $42,$A1,$51,$38
	!byte $43,$A2,$7E,$5A
	!byte $44,$A3,$7E,$5D
	!byte $46,$A5,$7A,$5D
	!byte $47,$A6,$5E,$4A
	!byte $48,$A7,$7B,$64
	!byte $49,$A9,$51,$44
	!byte $4B,$AA,$78,$68
	!byte $4C,$AB,$72,$66
	!byte $4D,$AC,$76,$6D
	!byte $4F,$AE,$56,$52
	!byte $50,$AF,$7F,$7D
	!byte $51,$B0,$7D,$7F
	!byte $52,$B1,$52,$56
	!byte $54,$B3,$6C,$75
	!byte $55,$B4,$66,$72
	!byte $56,$B5,$68,$78
	!byte $57,$B7,$5E,$70
	!byte $59,$B8,$5B,$70
	!byte $5A,$B9,$4A,$5E
	!byte $5B,$BA,$60,$7E
	!byte $5D,$BC,$4C,$67
	!byte $5E,$BD,$5A,$7E
	!byte $5F,$BE,$2F,$44
	!byte $60,$BF,$54,$7E
	!byte $62,$C1,$4D,$77
	!byte $63,$C2,$4B,$78
	!byte $64,$C3,$48,$77
	!byte $66,$C5,$3E,$6A
	!byte $67,$C6,$41,$73
	!byte $68,$C7,$42,$79
	!byte $69,$C8,$3A,$6E
	!byte $6B,$CA,$39,$70
	!byte $6C,$CB,$37,$70
	!byte $6D,$CC,$36,$72
	!byte $6E,$CE,$2A,$5C
	!byte $70,$CF,$37,$7D
	!byte $71,$D0,$32,$76
	!byte $72,$D1,$2A,$67
	!byte $74,$D3,$28,$66
	!byte $75,$D4,$2E,$7A
	!byte $76,$D5,$2A,$74
	!byte $77,$D6,$29,$76
	!fill 4	; to bring it up to 256 bytes per angle
precast_15:
	!byte $53,$A0,$62,$55
	!byte $54,$A2,$64,$59
	!byte $55,$A3,$68,$5F
	!byte $55,$A5,$71,$6A
	!byte $56,$A7,$6C,$68
	!byte $57,$A8,$5C,$5B
	!byte $58,$AA,$7A,$7C
	!byte $58,$AC,$70,$75
	!byte $59,$AD,$6C,$74
	!byte $5A,$AF,$56,$5F
	!byte $5A,$B1,$5F,$6C
	!byte $5B,$B2,$64,$75
	!byte $5C,$B4,$53,$64
	!byte $5C,$B6,$53,$67
	!byte $5D,$B7,$56,$6E
	!byte $5E,$B9,$5B,$78
	!byte $5E,$BB,$56,$75
	!byte $5F,$BC,$2F,$42
	!byte $60,$BE,$50,$74
	!byte $60,$C0,$54,$7E
	!byte $61,$C1,$47,$6E
	!byte $62,$C3,$49,$75
	!byte $63,$C5,$2F,$4E
	!byte $63,$C6,$39,$62
	!byte $64,$C8,$3C,$6B
	!byte $65,$CA,$43,$7C
	!byte $65,$CB,$40,$7B
	!byte $66,$CD,$3F,$7E
	!byte $67,$CF,$32,$68
	!byte $67,$D0,$35,$73
	!byte $68,$D2,$2E,$68
	!byte $69,$D4,$24,$55
	!byte $69,$D5,$33,$7E
	!byte $6A,$D7,$2E,$77
	!byte $6B,$D9,$2A,$72
	!byte $6C,$DA,$22,$61
	!byte $6C,$DC,$2A,$7F
	!byte $6D,$DE,$23,$6F
	!byte $6E,$DF,$22,$72
	!byte $6E,$E1,$1B,$60
	!byte $6F,$E3,$20,$79
	!byte $70,$E4,$1D,$75
	!byte $70,$E6,$19,$6C
	!byte $71,$E8,$0E,$41
	!byte $72,$E9,$19,$7E
	!byte $72,$EB,$14,$6D
	!byte $73,$ED,$15,$7D
	!byte $74,$EE,$12,$76
	!byte $75,$F0,$0E,$66
	!byte $75,$F2,$0B,$5A
	!byte $76,$F3,$0D,$79
	!byte $77,$F5,$09,$61
	!byte $77,$F7,$09,$73
	!byte $78,$F8,$06,$5E
	!byte $79,$FA,$06,$79
	!byte $79,$FC,$04,$71
	!byte $7A,$FD,$02,$5C
	!byte $7B,$FF,$01,$7C
	!byte $7B,$01,$00,$7F
	!byte $7C,$02,$02,$6A
	!byte $7D,$04,$04,$7D
	!byte $7E,$06,$05,$6F
	!byte $7E,$07,$05,$56
	!fill 4	; to bring it up to 256 bytes per angle

; Column compositing linked buffers
txNumBuf:	!fill 256
txColBuf:	!fill 256
heightBuf:	!fill 256
depthBuf:	!fill 256
linkBuf:	!fill 256
spriteCtBuf:	!fill 256
firstLink:	!fill NUM_COLS

; Active sprite restore addresses
mapSpriteL	!fill MAX_SPRITES
mapSpriteH	!fill MAX_SPRITES

; Sin of each angle, in log8.8 format plus the high bit being the sign (0x8000 = negative)
sinTbl	!word $0000, $8699, $877F, $87E1, $8800, $87E1, $877F, $8699
	!word $8195, $0699, $077F, $07E1, $0800, $07E1, $077F, $0699

; Dithering patterns for sky and ground, encoded specially for this engine. 18 different combinations.
skyGndTbl1:
	!byte $00 ; lo-bit black
	!byte $00 ; lo-bit black
	!byte $00 ; lo-bit black
	!byte $02 ; violet
	!byte $08 ; green
	!byte $0A ; lo-bit white
	!byte $0A ; lo-bit white
	!byte $0A ; lo-bit white
	!byte $20 ; hi-bit black
	!byte $20 ; hi-bit black
	!byte $20 ; hi-bit black
	!byte $22 ; blue
	!byte $28 ; orange
	!byte $2A ; hi-bit white
	!byte $2A ; hi-bit white
	!byte $2A ; hi-bit white
	!byte $00 ; lo-bit black
	!byte $20 ; hi-bit black
skyGndTbl2:
	!byte $00 ; lo-bit black
	!byte $02 ; violet
	!byte $08 ; green
	!byte $02 ; violet
	!byte $08 ; green
	!byte $02 ; violet
	!byte $08 ; green
	!byte $0A ; lo-bit white
	!byte $20 ; hi-bit black
	!byte $22 ; blue
	!byte $28 ; orange
	!byte $22 ; blue
	!byte $28 ; orange
	!byte $22 ; blue
	!byte $28 ; orange
	!byte $2A ; hi-bit white
	!byte $0A ; lo-bit white
	!byte $2A ; hi-bit white

; Movement amounts when walking at each angle
; Each entry consists of an X bump and a Y bump, in 8.8 fixed point
walkDirs !word $0040, $0000
	 !word $003B, $0018
	 !word $002D, $002D
	 !word $0018, $003B
	 !word $0000, $0040
	 !word $FFE8, $003B
	 !word $FFD3, $002D
	 !word $FFC5, $0018
	 !word $FFC0, $0000
	 !word $FFC5, $FFE8
	 !word $FFD3, $FFD3
	 !word $FFE8, $FFC5
	 !word $0000, $FFC0
	 !word $0018, $FFC5
	 !word $002D, $FFD3
	 !word $003B, $FFE8

