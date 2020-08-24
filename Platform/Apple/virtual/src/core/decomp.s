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

DEBUG	= 0

; Decompress from pSrc to pDst, stop at pEnd. The source and dest can overlap, as long as the 
; source block ends (at least) 2 bytes beyond the end of the dest block, e.g.
;    DDDDDDDDDDDDDDD
;            SSSSSSSss  <-- 2 bytes beyond dest
; This guarantees that the decompression won't overwrite any source material
; before it gets used.
decomp	!zone {
	jsr .chkdst
	ldy #0		; In lit loop Y must be zero
.fill1A	jsr .getbt2
	jmp .fill1B

.incdst	inc pDst+1
.chkdst	ldx pDst+1
	cpx pEnd+1
	ldx #$B0	; bcs
	bcc +
	clc
	ldx #$90	; bcc
+	stx .ifend
	rts

.endchk	lda pDst
	cmp pEnd	; check for done at end of each literal string
	bcc .seq
	bne .bad
	rts
.bad	sta $C002	; clrAuxRd
	brk

.src1A	inc pSrc+1
	bne .src1B	; always taken

.src2Ay	iny		; now Y=1
.src2A	inc pSrc+1
	clc
	bcc .src2B	; always taken

.lits	asl bits	; get bit that tells us whether there's a literal string
	beq .fill1A	; if we ran out of bits, get more
.fill1B	bcc .ifend	; if bit was zero, no literals: go straight to sequence (after end check)
	; Yes we have literals. Is the count exactly 1?
	asl bits
	bcc .not1
	bne .yes1	; if we didn't run out of bits, we can conclude len=1
	jsr .getbt2	; get more bits
	bcc .not1
	; Count is exactly 1 (most common case other than zero)
.yes1	lda (pSrc),y
	sta (pDst),y
	inc pSrc
	beq .src2Ay
	inc pDst
	clc
	bne .ifend	; common case
	beq .ldst	; otherwise, this is always taken
.not1	; count is not 1, so parse out a full gamma count
	lda #1
	jsr .gamma2
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
	bcs .src2A
.src2B	tya
	adc pDst
	sta pDst
	bcc +
.ldst	jsr .incdst
+	iny		; special case: long literal marked by len=255
	beq .lits
	ldy #0
.ifend	bcs .endchk	; normally skipped; self-modified to take when pDst+1 == pEnd+1
.seq	lda (pSrc),y
	inc pSrc
	beq .src1A
.src1B	asl
	php		; save high bit for later len check
	bmi .bigoff	; second-to-hi bit signals large offset
	lsr
	sta tmp
	ldx #$FF	; two's complement of zero
	bcc .gotoff	; always taken
.bigoff	asl		; sets carry
	sta tmp
	jsr .gamma
	lsr
	ror tmp
	lsr
	ror tmp
	eor #$FF	; make two's complement of offset hi-byte
	tax
	clc		; effectively add 1 to offset.
.gotoff	lda pDst
	sbc tmp
	sta pTmp
	txa
	adc pDst+1
	sta pTmp+1
.len	plp		; retrieve marker for match-len > 2
	bcc .short
.long	jsr .gamma	; longer matches handled here
	tax
-	lda (pTmp),y
	sta (pDst),y
	iny
	dex
	bne -
	; Match is always at least two bytes, so unroll that part.
.short	lda (pTmp),y
	sta (pDst),y
	iny
	lda (pTmp),y
	sta (pDst),y
	sec		; rather than increment Y an extra time
	tya		; advance dst ptr
	ldy #0		; as expected by lits loop
	adc pDst
	sta pDst
	bcs .dst1A
-	jmp .lits
.dst1A	jsr .incdst
	bcc -		; always taken

; Read an Elias Gamma value into A. Destroys X. Sets carry.
.gamma	lda #1
	asl bits
	bcc .gbit0
	beq .gfill1
	rts
.gfill1	jsr .getbts
.glup	bcc .gbit0
	rts
.gamma2
.gbit0	asl bits
	beq .gfill2
.gshift	rol
	asl bits
	bne .glup
	beq .gfill1
.gfill2	jsr .getbts
	bne .gshift	; always taken

; Get another 8 bits into our bit buffer. Destroys X. Preserves A. Requires Y=0.
; Alternately, use .getbt2 to preserve X and destroy A
.getbts	tax
.getbt2	lda (pSrc),y
	inc pSrc
	beq .src3A
.src3B	sec
	rol
	sta bits
	txa
	rts

.src3A	inc pSrc+1
	bne .src3B	; always taken

} ; end of zone

!ifdef PASS2 {
	;!warn "decomp spare: ", $E000 - *
	!if * > $E000 {
		!warn "Decomp grew too large."
	}
} else { ;PASS2
  !set PASS2=1
}
