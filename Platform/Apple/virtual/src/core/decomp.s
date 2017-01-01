;****************************************************************************************
; Copyright (C) 2017 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

;@com.wudsn.ide.asm.hardware=APPLE2
; Memory manager
; ------------------
;
; See detailed description in mem.i

* = $2000			; PLASMA loader loads us initially at $2000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"

bits	= $B		; len 1
pSrc	= $C		; len 2
pDst	= $E		; len 2

; Decompress from pSrc to pDst. They can overlap, as long as the source block
; ends (at least) 2 bytes beyond the end of the dest block, e.g.
;    DDDDDDDDDDDDDDD
;        SSSSSSSSSSSss  <-- 2 bytes beyond dest
; This guarantees that the decompression won't overwrite any source material
; before it gets used.
decomp	ldy #0		; invariant: Y=0 unless we're mid-copy
	sty bits
	beq .lits

.lits	jsr rdGamma
	tax
	dex
	beq +
-	lda (pSrc),y
	sta (pDst),y
	iny
	dex
	bne -
	jsr advSrc
	jsr advDst
	cpy #254	; special case: long literal string
	beq .lits
	ldy #0		; back to invariant
.endchk	cmp pEnd
	lda pDst+1
	sbc pEnd+1
	bcc .seq
	rts

.seq	lda (pSrc),y
	iny
	asl
	php		; save high bit for later len check
	asl
	bcs .bigoff
	lsr
	lsr
	sta tmp
	ldx #0
	beq .gotoff	; always taken
.bigoff	sta tmp
	jsr rdGamma
	lsr
	rol tmp
	lsr
	rol tmp
	tax
.gotoff	lda pDst
	sec
	sbc tmp
	sta pTmp
	txa
	eor #$FF
	adc pDst+1
	sta pDst+1
.len	ldx #2
	plp
	bcc .gotlen
	jsr rdGamma	; A>=1 + sec + 1 => final len 3 or more
	adc #1
	tax
.gotlen	jsr advSrc
	ldy #0
-	lda (pTmp),y
	sta (pDst),y
	iny
	dex
	bne -
	jsr advDst
	ldy #0
	beq .lits	; always taken

rdGamma	lda #1
-	asl bits
	bne +
	jsr getBits
+	bcs .ret
	asl bits
	bne +
	jsr getBits
+	rol
	bcc -		; always taken except if overflow error
.ret	rts

getBits	pha
	lda (pSrc),y
	iny
	sec
	rol
	sta bits
	pla
	rts

advSrc	tya
	clc
	adc pSrc
	sta pSrc
	bcc +
	inc pSrc+1
+	rts

advDst	tya
	clc
	adc pDst
	sta pDst
	bcc +
	inc pDst+1
+	rts
