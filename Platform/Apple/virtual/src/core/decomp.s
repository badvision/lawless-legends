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

tmp	= $2		; len 2
pTmp	= $4		; len 2
bits	= $6		; len 1

pSrc	= $C		; len 2
pDst	= $E		; len 2
pEnd	= $10		; len 2

; Decompress from pSrc to pDst. They can overlap, as long as the source block
; ends (at least) 2 bytes beyond the end of the dest block, e.g.
;    DDDDDDDDDDDDDDD
;        SSSSSSSSSSSss  <-- 2 bytes beyond dest
; This guarantees that the decompression won't overwrite any source material
; before it gets used.
decomp	ldy #0		; invariant: Y=0 unless we're mid-copy
	beq .lits2	; always taken

.lits	lsr bits
	bne +
.lits2	jsr getBits
+	bcc .seq
	jsr rdGamma
	tax
	cpx #255	; special case: long literal marked by len=255; chk and save to carry
-	lda (pSrc),y
	sta (pDst),y
	inc pSrc
	bne +
	inc pSrc+1
+	inc pDst
	bne +
	inc pDst+1
+	dex
	bne -
	bcs .lits	; (see special case above)
.endchk	lda pDst+1
	cmp pEnd+1	; check for done at end of each literal string
	bcc .seq
	lda pDst
	cmp pEnd
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
	tya
	clc
	adc pDst
	sta pDst
	ldy #0		; back to 0 as expected by lits section
	bcc .lits
	inc pDst+1
	bcs .lits	; always taken

; Read an Elias Gamma value into A. Destroys X. Sets carry.
rdGamma	lda #1
-	asl bits
	bne +
	jsr getBits
+	bcs .ret
	asl bits
	bne +
	jsr getBits
+	rol
	bcc -		; always taken except if overflow error, in which case WTF.
.ret	rts

; Get another 8 bits into our bit buffer. Destroys X. Preserves A. Requires Y=0.
getBits	tax
	lda (pSrc),y
	inc pSrc
	bne +
	inc pSrc+1
+	sec
	rol
	sta bits
	txa
	rts
