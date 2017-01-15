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
; Lx47 Decompressor
; ------------------

* = $DF00

tmp	= $2		; len 2
pTmp	= $6		; len 2		; not at $4, to preserve memmgr's pTmp
bits	= $8		; len 1

pDst	= $C		; len 2		; opposite order from memmgr,
pSrc	= $E		; len 2		;	so it can avoid swapping
pEnd	= $10		; len 2

cout	= $FDED
prbyte	= $FDDA
crout	= $FD8E
rdkey	= $FD0C

DEBUG	= 0

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
+	bcc .endchk	; if bit was zero, no literals: go straight to sequence (after end check)
	jsr .gamma	; Yes we have literals. Get the count.
	!if DEBUG { jsr .dbg1 }
	tax
-	lda (pSrc),y
	sta (pDst),y
	iny
	dex
	bne -
	tya
	clc
	adc pSrc
	sta pSrc
	bcc +
	inc pSrc+1
+	tya
	clc
	adc pDst
	sta pDst
	bcc +
	inc pDst+1
+	iny		; special case: long literal marked by len=255
	beq .lits
	ldy #0
.endchk	lda pDst	; check for done at end of each literal string
	cmp pEnd
	lda pDst+1
	sbc pEnd+1
	;bcs .ret
	bcs .chk

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
	ror tmp
	lsr
	ror tmp
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
	!if DEBUG { jsr .dbg2 }
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
	bcc +
	inc pDst+1
+	jmp .lits

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

.chk	ora pDst
	eor pEnd
	beq .ret
	brk

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

!if DEBUG {
.dbg1	pha
	lda #'L'|$80
	jsr cout
	pla
	pha
.dbgEnd	jsr prbyte
	jsr crout
;-	bit $C000
;	bpl -
;	bit $C010
	pla
	rts

.dbg2	pha
	lda #'S'|$80
	jsr cout
	txa
	jsr prbyte
	lda #' '|$80
	jsr cout
	lda tmp
	clc
	adc #1
	pha
	lda tmp+1
	adc #0
	jsr prbyte
	pla
	jmp .dbgEnd
}

} ; end of zone
