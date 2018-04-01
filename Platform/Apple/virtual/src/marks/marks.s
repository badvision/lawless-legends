;****************************************************************************************
; Copyright (C) 2018 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
; ANY KIND, either express or implied. See the License for the specific language
; governing permissions and limitations under the License.
;****************************************************************************************

* = $DB00

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

tmp	= $4	; len 2
pTmp	= $6	; len 2

	jmp saveMarks

;Input:	A-reg = row stride
;       X-reg = width
;       Y-reg = height
;       TOS-2 = pointer to map data, lo byte...
;       TOS-1 = ...and hi byte
;       TOS   = map number (with hi bit set if 3D)
saveMarks: !zone
	sta .adStrd+1
	stx .cmpWd+1
	sty lineCt
	pla
	clc
	adc #1
	sta .ret+1
	pla
	adc #0
	sta .ret+2
	txa
	adc #7	; carry already clear from above
	lsr
	lsr
	lsr
	sta .addBSz+1
	lda #0
-	clc
.addBSz	adc #11		; self-modified above
	dey		; multiply height * size-of-encoded-row
	bne .addBSz	; Y=0 at end of loop (used below)
	tax		; X is now the total # of encoded bytes
	inx
	inx		; plus header size

	lda #<bufStart
	sta pTmp
	lda #>bufStart
	sta pTmp+1

.scan	pla
	pha
	cmp (pTmp),y
	beq .found
	lda (pTmp),y
	beq .notfnd
	iny
	lda (pTmp),y
	dey
	clc
	adc pTmp
	sta pTmp
	bcc .scan
	inc pTmp+1
	bne .scan	; always taken

.notfnd	pla
	sta (pTmp),y
	iny
	txa
	sta (pTmp),y
	tay		; save offset for end-of-buffer marking
	clc
	adc pTmp
	bcc +
	lda pTmp+1
	cmp #>(bufEnd-256)
	bcs .full
+	lda #0		; end-of-buffer mark, and also mask
	sta (pTmp),y	; mark new end of buffer
	beq .go

.found	pla
	lda #$FF
.go	sta .mask1+1
	sta .mask2+1
	ldy #2

	pla		; source data addr hi...
	sta .get+2
	pla		; ...and lo
	sta .get+1

	lda #$80
	sta tmp
.nxtrow	ldx #0
.get	lda $1111,x	; self-modified above
	asl
	asl		; shift $40 bit (automap) into carry
	ror tmp		; save the bit
	bcc +
	lda (pTmp),y
.mask1	and #11		; self-modified above
	ora tmp
	sta (pTmp),y
	iny
	lda #$80	; restore sentinel
	sta tmp
+	inx
.cmpWd	cpx #11		; check width (self-modified above)
	bne .get	; loop until row width is complete
	lda tmp
	bmi +		; skip final store if no bits
-	lsr tmp		; low-align last set of bits in row
	bcc -
	lda (pTmp),y
.mask2	and #11		; self-modified above
	ora tmp
	sta (pTmp),y
	iny
+	lda .get+1
	clc
.adStrd	adc #11		; add stride (self-modified above)
	sta .get+1
	bcc +
	inc .get+2
+	dec lineCt
	bne .nxtrow
.ret	jmp $1111	; self-modified above
.full	brk		; TODO

lineCt	!byte 0

bufStart = *
	!byte 0
bufEnd	= $E000