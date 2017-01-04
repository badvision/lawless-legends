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
; LegendOS bootstrapping loader
; -----------------------------

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Handy global defs
!source "../include/global.i"

DEBUG	= 0

* = $2000

tmp	= $2		; len 2
pTmp	= $4		; len 2
bits	= $6		; len 1

pSrc	= $C		; len 2
pDst	= $E		; len 2
pEnd	= $10		; len 2

pData	= $80		; len 2
pRun	= $82		; len 2

decomp	= $DF00

init	; Init pointer to blocks we're going to move/decompress
	lda #<dataStart
	sta pData
	lda #>dataStart
	sta pData+1
	; temporary: copy ROM so we can debug decompressor
	bit setLcWr	; read from ROM, write to LC ram
	bit setLcWr
	ldy #0
	sty pSrc
	ldx #$f8
--	stx pSrc+1
-	lda (pSrc),y
	sta (pSrc),y
	iny
	bne -
	inx
	bne --
	; First is the decompressor itself (special: just copy one page)
	jsr getBlk
	bit setLcRW+lcBank1	; switch in target bank
	bit setLcRW+lcBank1
-	lda (pSrc),y
.st	sta decomp,y
	iny
	bne -
	; Next comes ProRWTS
	jsr runBlk
	; Then PLASMA
	jsr runBlk
	; And finally the memory mgr (fall through)
runBlk	jsr getBlk	; get block size and calc pointers
!if DEBUG { 
	lda #1		; turn on printer
	jsr $FE95
	jsr debug
}
	bit setLcRW+lcBank1
	jsr decomp	; decompress the code
!if DEBUG {
	lda #"R"
	jsr ROM_cout
	jsr ROM_crout
}
	jmp $4000	; and run it so it'll relocate itself

getByte	ldy #0
	lda (pData),y
	inc pData
	bne +
	inc pData+1
+	rts

getWord	jsr getByte
	pha
	jsr getByte
	tax
	pla
	clc
	rts

getBlk	; Get uncompressed len from the block header,
	; and calculate dest end (based on start of $4000)
	jsr getWord
	sta pEnd
	txa
	adc #$40
	sta pEnd+1

	; Get compressed len
	jsr getWord
	pha		; save lo byte of len

	; We're now looking at start of compressed data. Record that pointer.
	lda pData
	sta pSrc
	lda pData+1
	sta pSrc+1

	; We always decompress to $4000
	sty pDst	; Y already zero
	lda #$40
	sta pDst+1

	; Add compressed length to get to start of next block
	pla		; get len back
	adc pData
	sta pData
	txa
	adc pData+1
	sta pData+1

	rts

!if DEBUG {
debug	jsr ROM_crout
	lda #"B"
	jsr ROM_cout
	lda pSrc+1
	ldx pSrc
	jsr .pr
	lda pDst+1
	ldx pDst
	jsr .pr
	lda pEnd+1
	ldx pEnd
	jsr ROM_prntax
	jmp ROM_crout
.pr	jsr ROM_prntax
	lda #" "
	jmp ROM_cout
}

dataStart = *