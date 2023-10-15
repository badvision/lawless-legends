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

; Other equates
!source "../include/mem.i"
!source "../include/prorwts.i"

target  = $3	; len 1
tmp	= $4	; len 2
pTmp    = $6    ; len 2
pBuf	= $8	; len 2
; free: $9-$F (but used by prorwts)
mask    = $14	; len 1


	jmp _writeMarks

;Input:	A-reg = row stride
;       X-reg = width
;       Y-reg = height
;       TOS-2 = pointer to map data, lo byte...
;       TOS-1 = ...and hi byte
;       TOS   = map number (with hi bit set if 3D)
saveMarks: !zone
	sta .stride
	stx .width
	sty .lineCt
	pla
	sta .ret+4
	pla
	sta .ret+1
.rescan	lda .width	; need to reload (not just txa) in case rescanning after write
	clc
	adc #7
	lsr
	lsr
	lsr
	sta tmp		; number of bytes per row
	lda #0
	clc
	ldy .lineCt	; must reload in case of rescan
-	adc tmp
	dey		; multiply height * bytes-per-row
	bne -		; Y=0 at end of loop (used below)
	tax		; X is # of encoded bytes...
	inx
	inx		; ...plus header size (2 bytes)
	pla		; map number to look for
	pha		; but keep on stack in case of rescanning or not found
	jsr scan
	bcs .found

.notfnd	txa
	adc pBuf	; carry already clear because scan failed
	bcc +
	lda pBuf+1
	cmp #>(bufEnd-256)
	bcc +
	jsr _writeMarks	; write out existing marks and reset buffer
	jmp .rescan
+	pla		; map number
	sta (pBuf),y
	iny
	txa		; get back record length
	sta (pBuf),y
	txa		; end of buffer offset
	tay
	lda #0		; end-of-buffer mark, and also mask
	sta (pBuf),y	; mark new end of buffer
	beq .go		; always taken

.found	pla		; done with map number - discard it
	lda #$FF
.go	sta mask

	ldy #2		; start just after header

	pla		; source data addr hi...
	sta .get+2
	pla		; ...and lo
	sta .get+1

.nxtrow	lda #$80
	sta tmp
	ldx #0
.get	lda $1111,x	; self-modified above
	asl
	asl		; shift $40 bit (automap) into carry
	ror tmp		; save the bit
	bcc +
	lda (pBuf),y
	and mask
	ora tmp
	sta (pBuf),y
	iny
	lda #$80	; restore sentinel
	sta tmp
+	inx
	cpx .width
	bne .get	; loop until row width is complete
	lda tmp
	cmp #$80
	beq +		; skip final store if no bits
-	lsr tmp		; low-align last set of bits in row
	bcc -
	lda (pBuf),y
	and mask
	ora tmp
	sta (pBuf),y
	iny
+	lda .get+1
	clc
	adc .stride	; add stride (self-modified above)
	sta .get+1
	bcc +
	inc .get+2
+	dec .lineCt
	bne .nxtrow
.ret	lda #11		; self-modified above
	pha
	lda #11		; self-modified above
	pha
	rts
.lineCt !byte 0
.width  !byte 0
.stride !byte 0

; Scan the buffer for A-reg as target map
; Advances pBuf through the buffer
; If a match is found, SEC, and pBuf points to the matching record
; Else, CLC and pBuf points to the buffer terminator
; In any case, Y=0 on exit
scan: !zone
	sta target
	lda #<bufStart
	sta pBuf
	lda #>bufStart
	sta pBuf+1
-	ldy #0
	lda (pBuf),y
	clc
	beq .ret	; if end of buffer, Z=1 and C=0
	cmp target
	beq .ret	; if match, Z=1 and C=1
	iny
	lda (pBuf),y
	clc
	adc pBuf
	sta pBuf
	bcc -
	inc pBuf+1
	lda pBuf+1
	cmp #>bufEnd
	bcc -		; if sane, continue
	brk
.ret	rts

_writeMarks: !zone
	; First, open the file and seek to offset $1200
	lda #<filename
	sta namlo
	lda #>filename
	sta namhi
	ldx #0
	stx auxreq
	ldy #$12
	txa		; 0=cmdseek
	clc		; opendir
	jsr callProRWTS
	; Read the length of the marks data
	ldx #$40	; read to $4000
	stx ldrhi
	ldy #0
	sty ldrlo
	ldx #2		; length 2
	lda #cmdread
	sec		; rdwrpart
	jsr callProRWTS
	; Read the existing marks data
	lda #2
	sta ldrlo
	lda #$40
	sta ldrhi
	ldx $4000
	ldy $4001
	lda #cmdread
	sec
	jsr callProRWTS
	; Begin scan
	lda #2
	sta pTmp
	lda #$40
	sta pTmp+1
.outer	ldy #0
	lda (pTmp),y
	beq .end
	jsr scan
	iny
	lda (pTmp),y	; need length in any case
	bcc .next	; if no match, skip this record
	pha		; save record length
	cmp (pBuf),y	; sanity check
	beq +
	brk		; bad: length mismatch
+	tax
	dex		; we're not merging header bits
	dex
-	iny
	lda (pTmp),y
	ora (pBuf),y	; merge the mark bits
	sta (pTmp),y
	dex
	bne -
	pla		; retrieve record length
	clc
.next	adc pTmp
	sta pTmp
	bcc .outer
	inc pTmp+1
	bne .outer	; always taken
.end	lda #0
	sta bufStart	; clear buffer of marks
	ldx #4		; reseek
-	sta rwts_mark,x
	dex
	bpl -
	ldx #0		; seek to start of marks on disk: offset $1200
	ldy #$12
	lda #cmdseek
	sec		; rdwrpart
	jsr callProRWTS
	ldx #0		; starting from $4000
	stx ldrlo
	lda #$40
	sta ldrhi
	ldy $4001
	lda $4000
	clc
	adc #2		; length of marks plus header
	bcc +
	iny
+	tax
	lda #cmdwrite	; write new marks
	sec
	; fall through to final ProRWTS command
callProRWTS:
	inc diskOpCt
	stx sizelo
	sty sizehi
	sta reqcmd
	bcc +
	jmp proRWTS	; rdwrpart; note: prorwts does not set status, so don't check it.
+	jsr proRWTS+3	; opendir; status is updated
	lda status
	bne .err
	rts
.err	brk

filename
 !byte 11 ; string len
 !raw "GAME.1.SAVE"	; 'raw' chars to get lo-bit ascii that ProDOS likes.

bufStart = *
	!byte 0
bufEnd	= $E000