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

; Decompress from pSrc to pDst, stop at pEnd. The source and dest can overlap, as long as the 
; source block ends (at least) 2 bytes beyond the end of the dest block, e.g.
;    DDDDDDDDDDDDDDD
;            SSSSSSSss  <-- 2 bytes beyond dest
; This guarantees that the decompression won't overwrite any source material
; before it gets used.
decomp	!zone {
	ldy #0		; In lit loop Y must be zero
	beq .lits2	; always taken

.lits	asl bits	; get bit that tells us whether there's a literal string
	bne +		; ran out of bits in bit buffer?
.lits2	jsr .getbts	; get more bits
+	bcc .seq	; if bit was zero, no literals: go straight to sequence
	jsr .gamma	; Yes we have literals. Get the count.
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
.endchk	lda pDst	; check for done at end of each literal string
	cmp pEnd
	lda pDst+1
	sbc pEnd+1
	bcs .ret

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
	jsr .gamma
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
	jsr .gamma
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
.gamma	lda #1
-	asl bits
	bne +
	jsr .getbts
+	bcs .ret
	asl bits
	bne +
	jsr .getbts
+	rol
	bcc -		; always taken except if overflow error, in which case whatevs.

; Get another 8 bits into our bit buffer. Destroys X. Preserves A. Requires Y=0.
.getbts	tax
	lda (pSrc),y
	inc pSrc
	bne +
	inc pSrc+1
+	sec
	rol
	sta bits
	txa
.ret	rts
} ; end of zone
