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
	lda #$B0	; bcs
	sta .ifend
	jsr .chkdst
	ldy #0		; In lit loop Y must be zero
	beq .lits2	; always taken

.incdst	inc pDst+1
.chkdst	ldx pDst+1
	cpx pEnd+1
	bne +
	ldx #$90	; bcc
	stx .ifend
+	clc
	rts

.dst1A	jsr .incdst
	bcc .dst1B	; always taken

.endchk	cmp pEnd	; check for done at end of each literal string
	bcc .seq
	bne .bad
	rts
.bad	brk

.src1A	inc pSrc+1
	clc
	bcc .src1B	; always taken

.src2A	inc pSrc+1
	bne .src2B	; always taken

.lits	asl bits	; get bit that tells us whether there's a literal string
	bne +		; ran out of bits in bit buffer?
.lits2	jsr .getbts	; get more bits
+	bcc .ifend	; if bit was zero, no literals: go straight to sequence (after end check)
	jsr .gamma	; Yes we have literals. Get the count.
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
	bcs .src1A
.src1B	tya
	adc pDst
	sta pDst
	bcs .dst1A
.dst1B	iny		; special case: long literal marked by len=255
	beq .lits
	ldy #0
.ifend	bcs .endchk	; normally skipped; self-modified to take when pDst+1 == pEnd+1
.seq	lda (pSrc),y
	inc pSrc
	beq .src2A
.src2B	asl
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
.gotoff	lda pDst
	clc		; effectively add 1 to offset.
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
-	bcc .lits
	jsr .incdst
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
.gbit0	asl bits
	beq .gfill2
.gshift	rol
	asl bits
	bne .glup
	beq .gfill1
.gfill2	jsr .getbts
	bne .gshift	; always taken

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
	rts

} ; end of zone

!ifdef PASS2 {
	!warn "decomp spare: ", $E000 - *
	!if * > $E000 {
		!warn "Decomp grew too large."
	}
} else { ;PASS2
  !set PASS2=1
}
