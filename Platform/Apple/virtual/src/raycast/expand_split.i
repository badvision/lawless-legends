;****************************************************************************************
; Copyright (C) 2016 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************
	
expand_split:
	; relocate the last $1000 bytes of the expander to the hard-to-get area of
	; aux langauge card.
	lda #<to_reloc
	sta tmp
	lda #>to_reloc
	sta tmp+1
	lda #<expand_104
	sta pTmp
	lda #>expand_104
	sta pTmp+1
	ldy #0
	ldx #$10
-	lda (tmp),y
	sta (pTmp),y
	iny
	bne -
	inc tmp+1
	inc pTmp+1
	dex
	bne -
	; restore vector to first expander now that we've split and relocated
	lda #<expand_0
	sta expand_vec
	lda #>expand_0
	sta expand_vec+1
	; and length to which the main segment has been truncated
	lda #<(expand_split-expand_vec)
	ldx #>(expand_split-expand_vec)
	rts

to_reloc = *
