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

.lits	lsr bits
	bne +
	jsr getBits
+	bcc .seq
.lits2	jsr rdGamma
	tax
-	lda (pSrc),y
	sta (pDst),y
	iny
	dex
	bne -
	jsr advSrc
	jsr advDst
	iny		; special case: long literal string marked by len=255
	beq .lits2
	ldy #0		; back to invariant
.endchk	cmp pEnd	; check for done at end of each literal string
	bcc .seq
	lda pDst+1
	cmp pEnd+1
	bcc .seq
	rts

.seq	lda (pSrc),y
	inc pSrc
	bne +
	inc pSrc+1
+	asl
	php		; save high bit for later len check
	bmi .bigoff
	lsr
	sta tmp
	sty tmp+1	; zero
	bcc .gotoff	; always taken
.bigoff	asl
	sta tmp
	jsr rdGamma
	lsr
	rol tmp
	lsr
	rol tmp
	sta tmp+1
.gotoff	lda pDst
	clc		; effectively add 1 to offset.
	sbc tmp
	sta pTmp
	lda pDst+1
	sbc tmp+1
	sta pTmp+1
.len	ldx #2
	plp
	bcc .gotlen
	jsr rdGamma
	adc #1		; A>=1 + sec + 1 => final len 3 or more
	tax
.gotlen
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
	inc pSrc
	bne +
	inc pSrc+1
+	sec
	rol
	sta bits
	pla
	rts

advDst	inx
	inx
advSrc	tya
	clc
	adc pSrc,x
	sta pSrc,x
	bcc +
	inc pSrc+1,x
+	rts
