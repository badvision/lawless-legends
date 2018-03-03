;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************
	
; Select mipmap level 0 (64x64 pixels = 32x32 bytes)
; On entry, txColumn should be in A-reg
selectMip0:
	; pTex is already pointing at level 0, no need to adjust its level.
	; However, we do need to move it to the correct column. Currently txColumn
	; is 0..255 pixels, which we need to translate to 0..31 columns; that's
	; a divide by 8. But then we need to multiply by 32 bytes per column,
	; so (1/8)*32 = 4, so we need to multiply by 4 after masking.
	sta clrAuxZP
	ldy #0
	sty tmp
	and #$F8		; retain upper 5 bits of txColumn
	asl
	rol tmp			; multiplied by 2
	asl
	rol tmp			; multiplied by 4
	ldy tmp
mipReady:
	sta clrAuxZP
	ldx txNum
	bne +			; check for the special 'blank' texture
	lda #0
	ldy #0
+	clc			; adjust pTex by that much
	adc pTex
	tax
	tya
	adc pTex+1
	ldy pixNum		; get offset into the blit roll for this column
	sta setAuxZP
	stx pTex
	sta pTex+1
	ldx .blitOffsets,y
	ldy #$FF		; default to copying from top of column (will be 0 after initial INY in unrolled code)
	clv			; so code can use BVC to branch always without BRA
	rts
.blitOffsets: !byte BLIT_OFF0,BLIT_OFF1,BLIT_OFF2,BLIT_OFF3,BLIT_OFF4,BLIT_OFF5,BLIT_OFF6

; Select mipmap level 0 (32x32 pixels = 16x16 bytes)
; On entry, txColumn should be in A-reg
selectMip1:
	; pTex is pointing at level 0, so we need to move it to level 1.
	; Then we need to move it to the correct column. Currently txColumn
	; is 0..255 pixels, which we need to translate to 0..15 columns; that's
	; a divide by 16. But then we need to multiply by 16 bytes per column,
	; so (1/16)*16 = 1 ==> no multiply needed.
	and #$F0		; retain upper 4 bits
	ldy #>MIP_OFFSET_1	; adjust to mip level 1
	bne mipReady		; always taken

; Select mipmap level 2 (16x16 pixels = 8x8 bytes)
; On entry, txColumn should be in A-reg
selectMip2:
	; pTex is pointing at level 0, so we need to move it to level 2.
	; Then we need to move it to the correct column. Currently txColumn
	; is 0..255 pixels, which we need to translate to 0..8 columns; that's
	; a divide by 32. But then we need to multiply by 8 bytes per column,
	; so (1/32)*8 = 1/4 ==> overall we need to divide by 4.
	and #$E0		; retain upper 3 bits
	lsr			; div by 2
	lsr			; div by 4
	; no need to add #<MIP_OFFSET_2, since it is zero.
	ldy #>MIP_OFFSET_2	; adjust to mip level 2
	bne mipReady		; always taken

; Select mipmap level 3 (8x8 pixels = 4x4 bytes)
; On entry, txColumn should be in A-reg
selectMip3:
	; pTex is pointing at level 0, so we need to move it to level 3.
	; Then we need to move it to the correct column. Currently txColumn
	; is 0..255 pixels, which we need to translate to 0..3 columns; that's
	; a divide by 64. But then we need to multiply by 4 bytes per column,
	; so (1/64)*4 = 1/16 ==> overall we need to divide by 16.
	and #$C0		; retain upper 2 bits
	lsr			; div by 2
	lsr			; div by 4
	lsr			; div by 8
	lsr			; div by 16
	clc
	adc #<MIP_OFFSET_3
	ldy #>MIP_OFFSET_3	; adjust to mip level 3
	bne mipReady		; always taken

; Select mipmap level 4 (4x4 pixels = 2x2 bytes)
; On entry, txColumn should be in A-reg
selectMip4:
	; pTex is pointing at level 0, so we need to move it to level 4.
	; Then we need to move it to the correct column. Currently txColumn
	; is 0..255 pixels, which we need to translate to 0..1 columns; that's
	; a divide by 128. But then we need to multiply by 2 bytes per column,
	; so (1/128)*2 = 1/64 ==> overall we need to divide by 64
	and #$80		; retain the high bit
	beq +			; if not set, result should be zero
	lda #64			; else result should be 64
+   	clc
	adc #<MIP_OFFSET_4
	ldy #>MIP_OFFSET_4	; adjust to mip level 4
	bne mipReady		; always taken

; Select mipmap level 5 (2x2 pixels = 1x1 bytes)
; On entry, txColumn should be in A-reg
selectMip5:
	; Mip level 5 is super-easy: it's one byte. Not much choice there.
	lda #<MIP_OFFSET_5
	ldy #>MIP_OFFSET_5
	bne mipReady		; always taken
