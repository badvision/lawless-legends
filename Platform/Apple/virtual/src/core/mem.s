;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
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

* = $4000			; PLASMA loader loads us initially at $2000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"
!source "../include/mem.i"
!source "../include/plasma.i"

; Constants
MAX_SEGS	= 96

DEBUG		= 0
SANITY_CHECK	= 0		; also prints out request data

; Zero page temporary variables
tmp		= $2	; len 2
pTmp		= $4	; len 2
reqLen		= $6	; len 2
resType		= $8	; len 1
resNum		= $9	; len 1
isAuxCmd	= $A	; len 1
unused____0B	= $B	; len 1
pSrc		= $C	; len 2
pDst		= $E	; len 2
pEnd		= $10	; len 2

; Mapping of ProRWTS register names to mem mgr registers:
; "status"	-> tmp+1
; "auxreq"	-> isAuxCmd
; "reqcmd"	-> tmp
; "sizelo"+hi	-> reqLen
; "ldrlo"+hi	-> pDst
; "namlo"+hi	-> pSrc

lx47Decomp	= $DF00

; ProRWTS constants
cmdseek		= 0
cmdread		= 1
cmdwrite	= 2

; ProRWTS locations
rwts_mark	= $18		; to reset seek ptr, zero out 4 bytes here
proRWTS		= $D000

; Memory buffers
unusedBuf	= $4000	; used to be for ProDOS file buf and a copy space, but no longer
headerBuf 	= $4C00	; len $1400

; Memory used only during garbage collection
gcHash_first	= $5000	; index is srcLo ^ srcHi; result points into remaining gcHash tables.
gcHash_srcLo	= $5100
gcHash_srcHi	= $5200
gcHash_link	= $5300
gcHash_dstLo	= $5400
gcHash_dstHi	= $5500

;------------------------------------------------------------------------------
; Relocate all the pieces to their correct locations and perform patching.
relocate:
; special: clear most of the lower 48k and the ProDOS bank of the LC
	bit setLcRW+lcBank1
	bit setLcRW+lcBank1
	ldx #8
.clr1	stx .clrst1+2
	stx .clrst2+2
	ldy #0
	tya
-	sta 0,y		; help catch null ptr problems
	iny
	cpy #$20
	bne -
	tay
.clrst1	sta $800,y
.clrst2	sta $880,y
	iny
	bpl .clrst1	; yes, bpl not bne, because we're doing 128 bytes per loop
	inx
	cpx #$40	; skip our own unrelocated code $4000.4FFF
	bne +
	ldx #$60
+	cpx #$C0	; skip IO space $C000.CFFF, our future home $D000.DEFF, and decomp $DF00.DFFF
	bne +
	ldx #$E0
+	cpx #$F6	; skip ProRWTS $F600.FEFF, and ROM vecs $FF00.FFFF
	bne .clr1

; first our lo memory piece goes to $800
	ldy #0
	ldx #>(loMemEnd-loMemBegin+$FF)
.lold	lda loMemBegin,y
.lost	sta $800,y
	iny
	bne .lold
	inc .lold+2
	inc .lost+2
	dex
	bne .lold
; verify that aux mem exists
	inx
	stx $E000
	sei			; disable interrupts while in aux
	sta setAuxZP
	inx
	stx $E000
	cpx $E000
	sta clrAuxZP
	cli
	bne .noaux
	dex
	cpx $E000
	beq .gotaux
.noaux	jsr inlineFatal : !text "AuxMemReq",0
; Copy the 6502 ROM vectors
.gotaux	ldx #5
	bit setLcWr+lcBank1	; read from ROM, write to LC RAM
-	lda $FFFA,x
	sta $FFFA,x
	dex
	bpl -
	; set up a BRK/IRQ vector in low mem
	lda $FFFE
	sta _jbrk+1
	lda $FFFF
	sta _jbrk+2
	lda #<brkHandler
	sta $FFFE
	lda #>brkHandler
	sta $FFFF
; Place the bulk of the memory manager code into the LC
	ldx #>(hiMemEnd-hiMemBegin+$FF)
.ld4	lda hiMemBegin,y
.st4	sta $D000,y
	iny
	bne .ld4
	inc .ld4+2
	inc .st4+2
	dex
	bne .ld4
	; fall through into init...

;------------------------------------------------------------------------------
init: !zone
	; Turning off IIc keyboard buffer prevents interrupts from destabilizing
	; the system. IIc detection code below is thanks to qkumba :)
	lda $FBB3
        cmp #$06
        bne +		; II or II+
        lda $FBC0
        bne +		; IIe or IIGS
	lda #$10	; turn off keyboard buffer on IIc
	sta $C0AA
	lda #$B
	sta $C0AB
+
; switch in mem mgr
	bit setLcRW+lcBank1
	bit setLcRW+lcBank1
; clear the segment tables
	lda #0
	tax
-	sta tSegLink,x
	sta tSegAdrLo,x
	sta tSegAdrHi,x
	sta tSegType,x
	sta tSegRes,x
	inx
	cpx #MAX_SEGS
	bne -
; clear other pointers
	sta targetAddr+1
	sta scanStart
	sta partFileOpen
	sta curPartition
	lda #<diskLoader
	sta nextLdVec+1
	lda #>diskLoader
	sta nextLdVec+2
	lda #1
	sta scanStart+1
; make reset go to monitor
	lda #<ROM_monitor
	sta resetVec
	lda #>ROM_monitor
	sta resetVec+1
	eor #$A5
	sta resetVec+2
; We'll set up 8 initial segments:
; 0: main $0000 -> 4, active + locked
; 1: aux  $0000 -> 2, active + locked
; 2: aux  $0800 -> 3, inactive			; TEMPORARY: until we figure out prob w aux screen holes
; 3: aux  $BFFD -> 0, active + locked
; 4: main $0xxx -> 5, inactive (xxx = end of mem mgr low mem portion)
; 5: main $2000 -> 6, active + locked
; 6: main $6000 -> 7, inactive
; 7: main $BFFD -> 8, active + locked
; 8: main $E000 -> 9, inactive
; 9: main $FFFA -> 0, active + locked
; First, the flags
	ldy #$C0		; flags for active + locked (with no resource)
	sty tSegType+0
	sty tSegType+1
	sty tSegType+3
	sty tSegType+5
	sty tSegType+7
	sty tSegType+9
; Next the links
	ldx #2
	stx tSegLink+1
	inx
	stx tSegLink+2
	inx
	stx tSegLink+0
	inx
	stx tSegLink+4
	inx
	stx tSegLink+5
	inx
	stx tSegLink+6
	inx
	stx tSegLink+7
	inx
	stx tSegLink+8
; Then the addresses
	lda #8  ; Temporarily avoid aux screen holes; normally this would be 2.
	sta tSegAdrHi+2
	dey
	sty tSegAdrHi+3
	sty tSegAdrHi+7
	lda #<lastLoMem
	sta tSegAdrLo+4
	lda #>lastLoMem
	sta tSegAdrHi+4
	lda #$20
	sta tSegAdrHi+5
	lda #$60
	sta tSegAdrHi+6
	lda #$E0
	sta tSegAdrHi+8
	lda #$FA
	sta tSegAdrLo+9
	lda #$FD
	sta tSegAdrLo+3
	sta tSegAdrLo+7
	lda #$FF
	sta tSegAdrHi+9
; Finally, form a long list of the remaining unused segments.
	ldx #10
	stx unusedSeg		; that's the first unused seg
.loop:	inx
	txa
	sta tSegLink-1,x
	cpx #MAX_SEGS-1		; did all segments yet?
	bne .loop		; no, loop again
; Allocate space for the PLASMA frame stack
!if SANITY_CHECK {
	lda #$20
	sta framePtr+1		; because sanity check verifies it's not $BE or $BF
}	
	ldx #0
	ldy #2			; 2 pages
	lda #REQUEST_MEMORY
	jsr main_dispatch
	stx framePtr
	stx outerFramePtr
	stx .frameSet+1
	stx frameChk+1
	sty .frameSet+2
	sty frameChk+2
	lda #$AA		; store sentinel byte at bottom of frame
.frameSet:
	sta $1111		; self-modified above
	iny			; twice for 2 pages: initial pointer at top of new space
	iny
	sty framePtr+1
	sty outerFramePtr+1
	dey
	dey
	lda #LOCK_MEMORY	; lock it in place forever
	jsr main_dispatch
; Load PLASMA module #1 from partition #1
	ldx #1
	lda #START_LOAD
	jsr main_dispatch
	ldx #RES_TYPE_MODULE
	ldy #1
	lda #QUEUE_LOAD
	jsr main_dispatch
	stx .gomod+1
	stx glibBase		; save addr for extern fixups
	sty .gomod+2
	sty glibBase+1
	lda #LOCK_MEMORY	; lock it in forever
	jsr main_dispatch
	lda #FINISH_LOAD	; and load it
	jsr main_dispatch
	ldx #$10		; set initial PLASMA eval stack index
.gomod:	jmp $1111		; jump to module to start the game

;------------------------------------------------------------------------------
; Vectors and debug support code - these go in low memory at $800
loMemBegin: !pseudopc $800 {
	jmp j_main_dispatch
	jmp j_aux_dispatch
	jmp __asmPlasmNoRet
	jmp __asmPlasmRet

; Vectors for debug macros
	jmp __safeBell
	jmp __safeCout
	jmp __safePrbyte
	jmp __safeHome
	jmp __safePrhex
	jmp __safeRdkey
	jmp __writeStr
	jmp __prByte
	jmp __prSpace
	jmp __prWord
	jmp __prA
	jmp __prX
	jmp __prY
	jmp __crout
	jmp __waitKey
	jmp __internalErr
_fixedRTS:
	rts			; fixed place to find RTS, for setting V flag

j_main_dispatch:
	bit setLcRW+lcBank1	; switch in mem mgr
	bit setLcRW+lcBank1
	jsr main_dispatch
	bit setLcRW+lcBank2	; back to PLASMA
	rts

j_aux_dispatch:
	bit setLcRW+lcBank1	; switch in mem mgr
	bit setLcRW+lcBank1
	jsr aux_dispatch
	bit setLcRW+lcBank2	; back to PLASMA
	rts

;------------------------------------------------------------------------------
; Print fatal error message then halt.

inlineFatal:
!if DEBUG {
	jsr printMem
}
	pla
	tax
	pla
	tay
	inx
	bne fatalError
	iny
fatalError: !zone
	sty pTmp+1	; save message ptr hi...
	stx pTmp	; ...and lo
	jsr saveLCState
	jsr ROM_setnorm	; set up text mode and vectors
	bit setText
	bit page1
	jsr ROM_setvid
	jsr ROM_setkbd
	lda $24		; check if we're already at start of screen line
	beq +		; no, no need for CR
	jsr ROM_crout	; carriage return to get to start of screen line
+	ldy #40		; set up to print 40 dashes
	lda #'-'
.dash:	jsr ROM_cout
	dey
	bne .dash
	jsr restLCState
	+prStr : !text "FATAL ERROR: ",0
	ldx #$FF	; for asm str, max length
	lda (pTmp),y	; first byte (Y ends at 0 in loop above)
	bmi .msg	; 	if hi bit, it's a zero-terminated asm string
	tax		; else it's the length byte of a PLASMA string
	iny		; advance to first char
.msg	lda (pTmp),y
	beq .done
	ora #$80	; set hi bit of PLASMA strings for cout
	+safeCout
	iny
	dex
	bne .msg
.done:	bit setROM
	jsr ROM_bell
.hang: 	jmp .hang	; loop forever

;------------------------------------------------------------------------------
; BRK and IRQ handler: switch to ROM, call default handler, switch back
brkHandler:
	sta $45			; preserve A reg
	pla			; retrieve saved P reg
	pha
	and #$10		; check for BRK bit
	beq +			; if not brk, handle without bank switch
	bit setROM		; for BRK, do bank switch
	bit $c051		; also switch to text screen 1
	bit $c054
+	lda $45
_jbrk	jmp $1111		; self-modified by init

;------------------------------------------------------------------------------
; Call to ProRWTS in the aux LC
; Parameters in zero page locs $2-$F.
; Clear carry to call opendir, set carry to call rdwrpart
; On return, A contains status (for opendir only)
callProRWTS:
	; Copy the parameters to aux zero page
	ldx #$F
-	sta clrAuxZP
	lda 0,x
	sta setAuxZP
	sta 0,x
	dex
	bpl -
	bcc +
	jsr proRWTS	; rdwrpart
	jmp ++
+	jsr proRWTS+3	; opendir
	lda tmp+1	; grab the status code (only applicable for opendir)
++	sta clrAuxZP
	rts

;------------------------------------------------------------------------------
disk_rewind: !zone
	lda #0
	ldx #3			; clear all 32 bits
-	sta setAuxZP
	sta rwts_mark,x		; rewind the ProRWTS seek pointer
	sta clrAuxZP
	sta curMarkPos,x	; reset our record of the current mark
	dex
	bpl -
	rts

;------------------------------------------------------------------------------
; Utility routine for convenient assembly routines in PLASMA code. 
; Params: Y=number of parameters passed from PLASMA routine
; 1. Save PLASMA's X register index to evalStk
; 2. Verify X register is in the range 0-$10
; 3. Load the *last* parameter into A=lo, Y=hi
; 4. Write-enable Language Card bank 2
; 5. Run the calling routine (X still points into evalStk for add'l params if needed)
; 6. Restore PLASMA's X register, and advance it over the parameter(s)
; 7. (optional) store A=lo/Y=hi as PLASMA return value
; 8. Return to PLASMA
__asmPlasmRet: !zone {
	sec
	dey		; leave 1 slot for ret value
	!byte $a9	; skips over following clc
__asmPlasmNoRet:
	clc
	; adjust PLASMA stack pointer to skip over params
	sty tmp
	pla		; save address of calling routine, so we can call it
	tay
	pla
	sta .jsr+2
	iny
	sty .jsr+1
	bne +
	inc .jsr+2
+	php		; save carry flag marker for return or not
	cpx #$11
	bcs .badx	; X must be in range 0..$10
	txa		; current arg stack top
	adc tmp		; carry cleared by cpx (and/or adc) above
	pha		; and save that
	cmp #$11	; again, X must be in range 0..$10
	bcs .badx
frameChk:
	lda $1111	; self-modified by init code
	cmp #$AA	; check for sentinel value
	bne .badfrm
	bit setLcRW+lcBank2
	bit setLcRW+lcBank2
	lda evalStkL,x	; get last param to A=lo
	ldy evalStkH,x	; ...Y=hi
.jsr	jsr $1111	; call the routine to do work
	sta tmp		; stash return value lo
	pla
	tax		; restore adjusted PLASMA stack pointer
	plp
	bcc +
	lda tmp
	sta evalStkL,x	; store return value
	tya
	sta evalStkH,x
+	rts		; and return to PLASMA interpreter
; X reg ran outside valid range. Print and abort.
.badx	+prStr : !text $8D,"X=",0
	+prX
	jsr inlineFatal : !text "PlasmXRng",0
; Frame sentinel overwritten (likely too many strings)
.badfrm	jsr inlineFatal : !text "PlasmFrm",0
}

;------------------------------------------------------------------------------
; Debug code to support macros

; Save registers and also the state of the language card switches, then switch
; to ROM.
__iosaveROM: !zone {
	jsr saveLCState
	jmp ROM_iosave
}

saveLCState: !zone {
	php
	pha
	lda rdLCBnk2
	sta savedLCBnk2State
	bit setROM
	pla
	plp
	rts
}

; Restore registers and also the state of the language card switches.
__iorestLC:
	jsr ROM_iorest
	; fall through to restLCState
restLCState: ; Restore the state of the language card bank switch
	php
	bit savedLCBnk2State
	bmi +
	bit setLcRW+lcBank1
	bit setLcRW+lcBank1
	plp
	rts
+	bit setLcRW+lcBank2
	bit setLcRW+lcBank2
	plp
	rts

; Fetch a byte pointed to by the first entry on the stack, and advance that entry.
; Does LC switching to be sure to get the byte from the correct bank (if it happens
; to be in LC space).
_getStackByte !zone {
	inc $101,x
	bne +
	inc $102,x
+	lda $101,x
	sta .ld+1
	lda $102,x
	sta .ld+2
	jsr restLCState
.ld:   	lda $2000
	cmp setROM
	cmp #0		; fix N flag
	rts
}

; Support to print a string following the JSR, in high or low bit ASCII, 
; terminated by zero. If the string has a period "." it will be followed 
; automatically by a carriage return. Preserves all registers.
__writeStr: !zone {
	jsr __iosaveROM
	tsx
.loop:	jsr _getStackByte
	beq .done
	jsr ROM_cout
	cmp #$AE	; "."
	bne .loop
	jsr ROM_crout
	jmp .loop
.done:	jmp __iorestLC
}

__prByte: !zone {
	jsr __iosaveROM
	ldy #0
	; fall through to _prShared...
}

_prShared: !zone {
	tsx
	jsr _getStackByte
	sta .ld+1
	jsr _getStackByte
	sta .ld+2
.lup	jsr restLCState
.ld	lda $2000,y
	bit setROM
	jsr ROM_prbyte	; not safePrbyte, because we already switched to ROM
	dey
	bpl .lup
	lda #$A0
	jsr ROM_cout	; not safeCout, because we already switched to ROM
	jmp __iorestLC
}

__safeBell: !zone {
	jsr saveLCState
	jsr ROM_bell
	jmp restLCState
}

__safeCout: !zone {
	jsr saveLCState
	jsr ROM_cout
	jmp restLCState
}

__safePrhex: !zone {
	jsr saveLCState
	jsr ROM_prhex
	jmp restLCState
}

__safeRdkey: !zone {
	jsr saveLCState
	jsr ROM_rdkey
	jmp restLCState
}

__prSpace: !zone {
	php
	pha
	lda #$A0
	jsr __safeCout
	pla
	plp
	rts
}

__prWord: !zone {
	jsr __iosaveROM
	ldy #1
	bne _prShared	; always taken
}

__safePrbyte:
	jsr saveLCState
	jsr ROM_prbyte
	jmp restLCState

__safeHome:
	jsr saveLCState
	jsr ROM_home
	jmp restLCState

__prA:	jsr __iosaveROM
	lda $45 	; A reg stored here
pr_AXY_shared:
	jsr ROM_prbyte
	jmp __iorestLC
	
__prX:
	jsr __iosaveROM
	lda $46		; X reg stored here
	jmp pr_AXY_shared

__prY:
	jsr __iosaveROM
	lda $47		; y reg stored here
	jmp pr_AXY_shared

__crout: !zone {
	jsr __iosaveROM
	jsr ROM_crout
	jmp __iorestLC
}

__waitKey: !zone {
	jsr __iosaveROM
	jsr ROM_rdkey
	jmp __iorestLC
}

; Support for very compact abort in the case of internal errors. Prints
; a single-character code as a fatal error.
__internalErr: !zone {
	+prStr : !text $8D,"err=",0
	tsx
	jsr _getStackByte
	jsr __safeCout
	jsr inlineFatal : !text "Internal",0
}

; Space (in main RAM) for saving the state of the LC bank switch
savedLCBnk2State !byte 0

; Filename of the partition to open. The number gets fiddled by code.
partFilename:
 !byte 11 ; string len
 !raw "GAME.PART.1"	; 'raw' chars to get lo-bit ascii that ProDOS likes.

;------------------------------------------------------------------------------
; Heap management routines

; Check if blk pSrc is in GC hash; optionally add it if not.
; Input : pSrc = blk to check/add
;	  Carry set = add if not found, clear = check only, don't add
; Output: Y-reg = index
;	  Carry set = found, clear = not found (added if requested)
gcHash_chk: !zone
	lda pSrc
	eor pSrc+1
	tax
	lda gcHash_first,x
	beq .notfnd
-	tay
	lda gcHash_srcLo,y
	eor pSrc
	beq .found
	lda gcHash_link,y
	bne -
.notfnd	bcc .ret
	inc gcHash_top
	beq .corrup		; too many blks, or infinite loop? barf out
	ldy gcHash_top
	lda pSrc
	sta gcHash_srcLo,y
	lda pSrc+1
	sta gcHash_srcHi,y
	lda #0
	sta gcHash_dstHi,y
	lda gcHash_first,x
	sta gcHash_link,y
	tya
	sta gcHash_first,x
	clc
.ret	rts
.found	sec
	rts
.corrup	jmp heapCorrupt

; Verify integrity of memory manager structures
memCheck: !zone
	jsr heapCheck	; heap check (if there is one)
	ldx #0		; check main bank
	jsr .chk
	ldx #1		; 	then aux
.chk	lda tSegLink,x
	tay
	beq .done
	lda tSegAdrLo,y	; verify addresses are in ascending order
	cmp tSegAdrLo,x
	lda tSegAdrHi,y
	sbc tSegAdrHi,x
	tya
	tax
	bcs .chk
	jsr inlineFatal : !text "MTblCorrupt",0
.done	rts

; Verify the integrity of the heap
heapCheck: !zone
	jsr startHeapScan
.blklup	bcc +		; if top-of-heap, we're done
	rts
+	bvs .isobj	; handle objects separately
	; it's a string; check its characters
	beq .nxtblk	; handle zero-length string
.stlup	iny
	lda (pSrc),y
	beq heapCorrupt	; strings shouldn't have zero bytes embedded
	bmi heapCorrupt	; strings should be lo-bit ASCII
	dex
	bne .stlup
	; advance to next heap block
.nxtblk	jsr nextHeapBlk
	jmp .blklup	; go again
	; it's an object; check its pointers
.isobj	lda typeTblL,x	; get type table address for this type
	sta .getoff+1	; set up for pointer offset fetching
	lda typeTblH,x	;	hi byte too
	sta .getoff+2
	ldx #0		; type entry starts at len byte, which we immediately skip
.tscan	inx
.getoff	lda $1000,x	; self-modified above: get next pointer offset for type
	beq .nxtblk	; zero marks end of offset table
	tay
	iny		; not much we can do to validate lo byte, so skip it
	cpy reqLen	; ensure offset is within type length
	beq +		; 	very end is ok
	bcs heapCorrupt	;		beyond end is not ok
+	lda (pSrc),y	; get hi byte of ptr
	beq .tscan	; null is ok
	cmp heapStartPg	; else check if < start of heap
	bcc heapCorrupt
	cmp heapEndPg	; or >= than end of heap
	bcc .tscan
	; fall through to heapCorrupt...

heapCorrupt:
       +prWord pTmp
       jsr inlineFatal : !text "HeapCorrupt",0

; Begin a heap scan by setting pTmp to start-of-heap, then returns
; everything as per nextHeapBlk below
startHeapScan: !zone
	lda #0
	ldx heapStartPg
	sta pDst		; used in gc2 sweep init
	stx pDst+1
	jmp .pgadv

; Add tmp to pSrc, and sanity check against top of heap.
; Get current block on heap during heap scan
; Output: reqLen (and A): size of heap block
;	  N and Z: set according to size of heap block
;	  Y: 0
;         C: set if end of heap reached
;         V: clear if string, set if object
;	  X: index into type table (if block is an object),
;            or string length (if block is a string)
nextHeapBlk:
	lda pSrc
	ldx pSrc+1
	sec		; add 1 to account for type byte or string len byte
	adc reqLen
	bcc +
	inx
.pgadv	stx pSrc+1
+	sta pSrc
	cpx heapEndPg
	bcs heapCorrupt
getHeapBlk:
	ldy #0		; so we always return a nice useful Y index
	cmp heapTop	; have we reached current top-of-heap?
	txa
	sbc heapTop+1
	bcs .done	;	if so we're done
	lda (pSrc),y
	bmi .isobj
	clv		; it's a stirng
	tax
.gotlen	sta reqLen
.done	rts
.isobj	and #$7F
	tax
	cpx nTypes
	bcs heapCorrupt
	bit fixedRTS	; set V flag
	lda typeLen,x
	bvs .gotlen	; always taken

; Phase 1 of Garbage Collection: mark accessible heap blocks starting from the root block
gc1_mark: !zone
	ldx #0			; clear the hash table and set pSrc to the global blk
	stx gcHash_top
	stx pSrc
	lda heapStartPg
	sta pSrc+1
	txa
-	sta gcHash_first,x
	inx
	bne -
	sec			; sec means add if not found
	jsr gcHash_chk		; seed the hash, and thus our queue, with the global block
	clv			; clear V flag to mark phase 1
	bvc .start
; Phase 3 of Garbage Collection: fix all pointers
gc3_fix:
	bit fixedRTS		; set V flag to mark phase 3
.start	lda #0
	sta resNum		; initialize block counter (note: blk #0 in hash is not used)
.outer	inc resNum		; advance to next block in hash
	ldx resNum
	cpx gcHash_top		; finished all blocks?
	beq .trav		; 	last blk? if so still need to trav it
	bcs .done		; 		or if past last blk, we're done
.trav	ldy gcHash_srcLo,x	; get pointer to block, lo byte first
	lda gcHash_srcHi,x	;	then hi byte
	bvc +
	ldy gcHash_dstLo,x	; in pointer fix mode, use the block's final location
	lda gcHash_dstHi,x
+	sty pTmp		; store object pointer so we can dereference it
	sta pTmp+1
	ldy #0			; first byte
	lda (pTmp),y		;	is the type
	bpl .outer		; 		or, if not hi bit, just a string so skip (no ptrs)
	and #$7F		; mask off hi bit to get type number
	tax
	lda typeTblL,x		; get pointer to type table
	sta .ldof+1
	lda typeTblH,x
	sta .ldof+2
	ldx #1			; skip size byte, access first ptr offset
.ldof	lda $1111,x		; self-modified above: get offset entry
	beq .outer		; zero marks end of list -> go to next block
	tay
	stx tmp+1		; save index into type entry
	lda (pTmp),y		; grab pointer at entry offset
	sta pSrc
	sty .fix+1		; save pointer offset for use if fixing pointers in phase 3
	iny
	lda (pTmp),y
	sta pSrc+1
	ora pSrc
	beq .next		; skip null pointer
	sec			; sec = we want to add to hash if it's not there
	bvc +
	clc			; in phase 3, we don't want to add to hash
+	jsr gcHash_chk		; go add it to the hash
	bvc .next		; skip pointer fixing in phase 1
	bcc .corrup		; in phase 3, pointer must be in hash!
	tya
	tax			; put block number in X
.fix	ldy #11			; restore pointer offset (self-modified above)
	lda gcHash_dstLo,x	; get new location
	sta (pTmp),y		; update the pointer
	iny
	lda gcHash_dstHi,x	; hi byte too
	sta (pTmp),y
.next	ldx tmp+1		; restore type entry index
	inx			; next offset entry
	bne .ldof		; always taken
.done	rts
.corrup	jmp heapCorrupt

; Phase 2 of Garbage Collection: sweep all accessible blocks together
gc2_sweep: !zone
	jsr startHeapScan	; init pSrc and pDst, set reqLen to length of first block
	bcs .done		; stop if heap is empty
.outer	;;clc			; clc = do not add to hash
	jsr gcHash_chk		; is this block in hash?
	bcc .advSrc		; if not in hash, skip this block
	lda pDst
	sta gcHash_dstLo,y	; record new address
	eor pSrc
	sta tmp
	lda pDst+1
	sta gcHash_dstHi,y	;	in hash table
	eor pSrc+1
	ora tmp			; this will be zero iff all 16 bits of pSrc == pDst
	beq .advDst		; if equal, no need to copy
	ldy #0
	ldx reqLen		; index for byte-copy count
	inx			; set up to copy type/len byte as well
.cplup	lda (pSrc),y
	sta (pDst),y
	iny
	dex
	bne .cplup
.advDst	lda pDst		; advance dest
	sec			; add 1 for type/len byte
	adc reqLen
	sta pDst
	bcc .advSrc
	inc pDst+1
.advSrc	jsr nextHeapBlk		; advance to next src block
	bcc .outer		; and process it
.done	lda pDst		; done sweeping, so set new heap top.
	sta heapTop
	lda pDst+1
	sta heapTop+1
	rts

closePartFile: !zone
	lda partFileOpen	; check if open
	beq .done
	!if DEBUG { +prStr : !text "ClosePart.",0 }
	dec partFileOpen
.done	rts

heapCollect: !zone
	; can't collect why anything queued for load
	lda nSegsQueued
	bne .unfin
	jsr closePartFile
	jsr gc1_mark		; mark reachable blocks
	jsr gc2_sweep		; sweep them into one place
	jsr gc3_fix		; adjust all pointers
	jsr heapClr		; and clear newly freed space
	ldx heapTop		; return new top-of-heap in x=lo/y=hi
	ldy heapTop+1
	rts
.unfin:	jsr inlineFatal : !text "NdFinish",0

; Allocate a block on the heap. X = $00.7F for string block, $80.FF for a typed obj.
; And yes, type $80 is valid (conventionally used for the Global Object).
; And yes, string length $00 is valid (it's an empty string).
heapAlloc: !zone
	lda heapTop
	sta pSrc
	lda heapTop+1
	sta pSrc+1
	ldy #0
	txa
	sta (pSrc),y	; save obj type or string len on heap
	bpl .gotlen
	lda typeLen-$80,x
.gotlen	ldy pSrc+1
	sec		; add 1 to include type byte or len byte for strings
	adc pSrc
	bcc +
	iny
	cpy heapEndPg
	bcs .needgc
+	sta heapTop
	sty heapTop+1
retPSrc:
	ldx pSrc	; return ptr in X=lo/Y=hi
	ldy pSrc+1
	rts
.needgc	jsr inlineFatal : !text "NeedCollect",0

; Re-use existing string or allocate new and copy.
heapIntern: !zone
	stx pTmp
	sty pTmp+1
	tya
	ora pTmp	; check for null input ptr
	bne +
	rts		; input null -> output null
+	jsr startHeapScan
	bcs .notfnd	; handle case of empty heap
.blklup	bvs .nxtblk
	; it's a string
	inx		; +1 to compare length byte also
.stlup	lda (pTmp),y	; compare string bytes until non-matching (or all bytes done)
	cmp (pSrc),y
	bne .nxtblk
	iny
	dex
	bne .stlup
	; found a match!
.found	beq retPSrc
	; advance to next heap block
.nxtblk	jsr nextHeapBlk
	bcc .blklup	; go process next block
.notfnd	lda (pTmp),y	; get string length
	pha		; save it
	tax
	jsr heapAlloc	; make space for it
	pla		; string length back
	tax
	inx		; add 1 to copy length byte also
	ldy #0
.cplup	lda (pTmp),y	; copy the string's characters
	sta (pSrc),y
	iny
	dex
	bne .cplup
	beq .found	; always taken

;------------------------------------------------------------------------------
; Show or hide the disk activity icon (at the top of hi-res page 1). The icon consists of a 4x4
; block of blue pixels surrounded by a black border.
; Params: show/hide ($FF, or 0)
showDiskActivity: !zone
	cmp diskActState
	beq .done
	sta diskActState
	ldx #0
	stx pTmp
	ldy #$F8                ; offset of screen holes
	lda #$20                ; first line of screen is $2000
-	sta pTmp+1
	cmp #$30                ; pre-check for last line
	bit diskActState        ; check mode
	beq +
	lda (pTmp,x)            ; show mode
	sta (pTmp),y
	lda #$85
	bcc ++                  ; first 4 lines
	lda #0                  ; last line
	beq ++                  ; always taken
+ 	lda (pTmp),y            ; hide mode
++	sta (pTmp,x)
	lda pTmp+1
	clc
	adc #4
	cmp #$34                ; Do 5 lines: $2000, $2400, $2800, $2C00, $3000; Stop before $3400.
	bne -
.done	rts

lastLoMem = *
} ; end of !pseodupc $800
loMemEnd = *

;------------------------------------------------------------------------------
; The remainder of the code gets relocated up into the Language Card, bank 1.
hiMemBegin: !pseudopc $D000 {

;------------------------------------------------------------------------------
; Variables
targetAddr:	!word 0
unusedSeg:	!byte 0
scanStart:	!byte 0, 1	; main, aux
segNum:		!byte 0
nextLdVec:	jmp diskLoader
curPartition:	!byte 0
partFileOpen:	!byte 0
curMarkPos:	!fill 4		; really 3, but 1 extra to match ProRWTS needs
setMarkPos:	!fill 3
nSegsQueued:	!byte 0
bufferDigest:	!fill 4
diskActState:	!byte 0
floppyDrive:	!byte 0

;------------------------------------------------------------------------------
; Heap management variables
MAX_TYPES 	= 16

nTypes		!byte 0
typeTblL	!fill MAX_TYPES
typeTblH	!fill MAX_TYPES
typeLen		!fill MAX_TYPES		; length does not include type byte

heapStartPg	!byte 0
heapEndPg	!byte 0
heapTop		!word 0
gcHash_top	!byte 0

;------------------------------------------------------------------------------
grabSegment: !zone
; Input: None
; Output: Y-reg = segment grabbed
; Note: Does not disturb X reg
	ldy unusedSeg	; first unused segment
	beq .fail	; ran out?
	lda tSegLink,y	; no, grab next segment in list
	sta unusedSeg	; that is now first unused
	rts		; return with Y = the segment grabbed
.fail:	jsr inlineFatal : !text "MaxSegs", 0

;------------------------------------------------------------------------------
releaseSegment: !zone
; Input: Y-reg = segment to release
	lda unusedSeg	; previous head of list
	sta tSegLink,y	; point this segment at it
	sty unusedSeg	; this segment is now head of list
	tya		; seg num to accumulator
	ldx isAuxCmd
	cmp scanStart,x	; was this seg the scan start?
	bne +		; no, things are fine
	txa		; yes, need to reset the scan start
	sta scanStart,x	; scan at first mem block for (main or aux)
+	rts

;------------------------------------------------------------------------------
scanForAddr: !zone
; Input:  X(lo)/Y(hi) - address to scan for (gets stored in pTmp)
; Output: X-reg - segment found (zero if not found), N and Z set for X-reg
;	  Y-reg - next segment in chain
;         carry clear if addr == seg start, set if addr != seg start
	stx pTmp	; save target addr
	sty pTmp+1
	ldx isAuxCmd	; grab correct starting segment (0=main mem, 1=aux)
.loop:	ldy tSegLink,x	; grab link to next segment, which we'll need regardless
	lda pTmp	; compare pTmp
	cmp tSegAdrLo,x	; to this seg addr
	lda pTmp+1	; including...
	sbc tSegAdrHi,x	; ...hi byte
	bcc .next	; if pTmp < seg addr then keep searching
	lda pTmp	; compare pTmp
	cmp tSegAdrLo,y	; to *next* seg addr
	lda pTmp+1	; including...
	sbc tSegAdrHi,y	; ...hi byte
	bcc .found	; if pTmp < next seg addr then perfect!
.next:	tya		; next in chain
	tax		; to X reg index
	bne .loop	; non-zero = not end of chain - loop again
	rts		; fail with X=0
.found:	lda pTmp	; compare scan address lo...
	eor tSegAdrLo,x	; ... to seg start lo
	bne +		; if not equal, set carry
	lda pTmp+1	; hi byte
	eor tSegAdrHi,x	; to hi byte
	beq ++		; if equal, leave carry clear
+	sec		; addr is not equal, set carry
++	txa
	rts		; all done

;------------------------------------------------------------------------------
scanForResource: !zone
; Input:  resType, resNum: resource type and number to scan for
; Output: X-reg - segment found (zero if not found). N and Z set based on X reg.
	ldy isAuxCmd	; grab correct starting segment
	ldx scanStart,y	; start scanning at last scan point
	stx .next+1	; it also marks the ending point. Yes, self-modifying code.
.loop:	ldy tSegLink,x	; grab link to next segment, which we'll need regardless
	lda tSegRes,x	; check number
	cmp resNum	; same resource number?
	bne .next	; no, check next seg
	lda tSegType,x	; check get flag + type byte
	and #$F		; mask off flags to get just the type
	cmp resType	; same type?
	bne .next	; no, check next seg
	ldy isAuxCmd	; index for setting next scan start
	txa		; set N and Z flags for return
	sta scanStart,y	; set this seg as next scanning start
	rts		; all done!
.next:	cpy #11		; did we loop around to starting point? (filled in at beg)
	beq .fail	; if so, failed to find what we wanted
	tya		; next in chain
	tax		; to X reg index
	bne .loop	; not end of chain - loop again
	ldx isAuxCmd	; start over at beginning of memory chain
	cpx .next+1	; back where we started?
	bne .loop	; no, loop again
.fail:	ldx #0		; failure return
	rts

;------------------------------------------------------------------------------
scanForAvail: !zone
; Input:  reqLen - 16-bit length to scan for
; Output: X-reg - segment found (zero if not found)
	ldy isAuxCmd	; grab correct starting segment
	ldx scanStart,y	; start scanning at last scan point
	stx .next+1	; it also marks the ending point. Yes, self-modifying code.
.loop:	ldy tSegLink,x	; grab link to next segment, which we'll need regardless
	lda tSegType,x	; check flags
	bne .next	; skip allocated blocks (even if inactive)
	lda tSegAdrLo,x	; calc this seg addr plus len
	clc
	adc reqLen
	sta .cmp1+1	; save for later comparison (self-modifying)
	lda tSegAdrHi,x	; all 16 bits
	adc reqLen+1
	sta .cmp2+1	; again save for later (self-modifying)
	lda tSegAdrLo,y	; compare *next* seg start addr
.cmp1:	cmp #11		; self-modified earlier
	lda tSegAdrHi,y	; all 16 bits
.cmp2:	sbc #11		; self-modified earlier
	bcc .next	; next seg addr < (this seg addr + len)? no good - keep looking
	ldy isAuxCmd	; index for setting next scan start
	txa		; set N and Z flags for return
	sta scanStart,y	; set this seg as next scanning start
	rts		; all done!
.next:	cpy #11		; did we loop around to starting point? (filled in at beg)
	beq .fail	; if so, failed to find what we wanted
	tya		; next in chain
	tax		; to X reg index
	bne .loop	; not end of chain - loop again
	ldx isAuxCmd	; start over at beginning of memory chain
	cpx .next+1	; back where we started?
	bne .loop	; no, loop again
.fail:	ldx #0		; failure return
	rts

;------------------------------------------------------------------------------
main_dispatch: !zone
+	pha
	lda #0
	beq .go
aux_dispatch:
	pha
	lda #1
.go	sta isAuxCmd
	pla
!if SANITY_CHECK { jsr saneStart : jsr + : jmp saneEnd }
+	cmp #REQUEST_MEMORY
	bne +
	jmp mem_request
+	cmp #QUEUE_LOAD
	bne +
	jmp mem_queueLoad
+	cmp #LOCK_MEMORY
	bne +
	jmp mem_lock
+	cmp #UNLOCK_MEMORY
	bne +
	jmp mem_unlock
+	cmp #FREE_MEMORY
	bne +
	jmp mem_free
+	cmp #DEBUG_MEM
	bne +
	jmp printMem
+	cmp #CALC_FREE
	bne +
	jmp mem_calcFree
+	cmp #RESET_MEMORY
	bne +
	jmp reset
+	cmp #SET_MEM_TARGET
	bne +
	stx targetAddr
	sty targetAddr+1
	rts
+	cmp #CHECK_MEM
	bne +
	jmp memCheck
+	cmp #HEAP_SET
	bne +
	jmp heapSet
+	cmp #HEAP_ADD_TYPE
	bne +
	jmp heapAddType
+	cmp #HEAP_ALLOC
	bne +
	jmp heapAlloc
+	cmp #HEAP_INTERN
	bne +
	jmp heapIntern
+	cmp #HEAP_COLLECT
	bne +
	jmp heapCollect
+	cmp #FATAL_ERROR
	bne +
	jmp fatalError
+	cmp #ADVANCE_ANIMS
	bne +
	jmp advanceAnims
+	jmp nextLdVec	; Pass command to next chained loader

;------------------------------------------------------------------------------
; Sanity check mode
!if SANITY_CHECK {
saneStart: !zone {
	sta saneEnd+2	; save cmd num for end-checking
	cmp #ADVANCE_ANIMS
	beq .skip
	pha
	jsr saneCheck
	+prChr 'M'
	lda isAuxCmd
	beq +
	+prChr 'a'
+	pla : pha : +prA
	cmp #RESET_MEMORY
	beq +
	cmp #HEAP_COLLECT
	beq +
	+prX
	cmp #START_LOAD
	beq +
	cmp #FINISH_LOAD
	beq +
	+prY
+	pla
.skip	rts
}

saneCheck: !zone {
	lda $E1
	cmp #$BE
	bcc +
	+internalErr 's'
+	rts
}

saneEnd: !zone {
	pha
	lda #$11	; self-modified earlier by saneStart
	cmp #ADVANCE_ANIMS
	beq .skip
	cmp #REQUEST_MEMORY
	beq .val
	cmp #QUEUE_LOAD
	beq .val
	cmp #HEAP_ALLOC
	beq .val
	cmp #HEAP_INTERN
	beq .val
	cmp #HEAP_COLLECT
	bne .noval
.val	+prStr : !text "->",0
	+prYX
.noval	jsr saneCheck
	+prStr : !text "m.",0
.skip	pla
	rts
}
}

;------------------------------------------------------------------------------
printMem: !zone
	lda $24		; check if we're already at start of screen line
	beq +		; no, no need for CR
	+crout
+	lda isAuxCmd
	bne aux_printMem
main_printMem:
	+prStr : !text "MainMem:",0
	ldy #0
	beq +
aux_printMem:
	+prStr : !text "AuxMem: ",0
	ldy #1
+	ldx #14
.printSegs:
-	+prSpace
	dex
	bne -
	lda #'$'
	+safeCout
	lda tSegAdrHi,y
	+safePrbyte
	lda tSegAdrLo,y
	+safePrbyte
	lda #','
	+safeCout
	lda #'L'
	+safeCout
	lda tSegLink,y
	tax
	lda tSegAdrLo,x
	sec
	sbc tSegAdrLo,y
	pha
	lda tSegAdrHi,x
	sbc tSegAdrHi,y
	+safePrbyte
	pla
	+safePrbyte
	lda tSegType,y
	tax
	asl		; check bits 6 (#$40) and 7 (#$80)
	bpl +		; was not #$40
	lda #'*'
	bne ++
+	lda #'+'
	bcs ++		; was #$80
	lda #'-'
++	+safeCout
	txa
	and #$F
	tax
	+safePrhex
	txa
	beq +
	lda #':'
	+safeCout
	lda tSegRes,y
	+prA
	jmp .next
+	ldx #4
-	+prSpace
	dex
	bne -
.next:	ldx #3		; 2 spaces before next entry
	lda tSegLink,y
	tay
	bne .printSegs
	+crout
	rts

;------------------------------------------------------------------------------
reset: !zone
; Set all non-locked pages to inactive.
	ldx #0			; main mem first
	jsr .inactivate		; inactivate its non-locked segments
	ldx #1			; then aux mem
	jsr .inactivate		; same thing
	lda #RESET_MEMORY	; get command code back
	jmp nextLdVec		; and allow chained loaders to reset also
.inactivate:	lda tSegType,x	; get flag byte for this segment
	asl			; segment locked?
	bmi .next		; yes, skip it
	lsr			; mask off the 'active' bit
	sta tSegType,x		; save it back
.next:	lda tSegLink,x		; get link to next seg
	tax			; to X reg, and test if end of chain (x=0)
	bne .inactivate		; no, not end of chain, so loop again
	rts

;------------------------------------------------------------------------------
outOfMemErr: !zone
	!if DEBUG = 0 { jsr printMem }  ; always print, even in non-debug mode
	jsr inlineFatal : !text "OutOfMem", 0

;------------------------------------------------------------------------------
reservedErr: !zone
	!if DEBUG = 0 { jsr printMem }  ; always print, even in non-debug mode
	jsr inlineFatal : !text "DblAlloc", 0

;------------------------------------------------------------------------------
mem_request: !zone
	stx reqLen		; save requested length
	sty reqLen+1		; all 16 bits
shared_alloc:
	lda #1
	sta .reclaimFlg		; we will try to reclaim once
.try:	ldy targetAddr+1	; see if SET_MEM_TARGET was called
	bne .gotTarget		; no, we need to choose location
; no target address has been specified, we need to choose one
.chooseAddr:
	jsr scanForAvail	; scan for an available block
	bne .noSplitStart	; if found, go into normal split checking
; failed to find a block. If we haven't tried reclaiming, do so now
.recl	dec .reclaimFlg		; first time: 1 -> 0, second time 0 -> $FF
	bmi outOfMemErr         ; so if it's second time, give up
	jsr reclaim             ; first time, do a reclaim pass
	jmp .try                ; and try again
.notFound:
	jmp invalParam
; target addr was specified. See if we can fulfill the request.
.gotTarget:
	ldx targetAddr		; all 16 bits
	jsr scanForAddr		; locate block containing target addr
	beq .notFound		; fail if we couldn't find it
	lda tSegType,x		; check flags
	bmi reservedErr		; if already active, can't re-allocate it
	ldy tSegLink,x		; get link to next seg
	bcc .noSplitStart	; scanForAddr clears carry if addr is equal
; need to split current segment into (cur..targetAddr) and (targetAddr..next)
.splitStart:
	jsr grabSegment		; get a new segment, index in Y (doesn't disturb X)
	lda targetAddr
	sta tSegAdrLo,y		; targetAddr is start of new segment
	lda targetAddr+1
	sta tSegAdrHi,y		; all 16 bits
	lda #0
	sta tSegType,y		; clear flags on new segment
	sta tSegType,x		; and old segment
	lda tSegLink,x		; get link to next existing seg
	sta tSegLink,y		; link new segment to it
	tya
	sta tSegLink,x		; link cur segment to new segment
	tax			; new segment number to X
; segment begins at exactly the right address. Does it end at the right addr?
.noSplitStart:
	lda tSegAdrLo,x		; calc seg addr + reqLen
	clc
	adc reqLen
	sta .reqEnd		; save for later
	lda tSegAdrHi,x		; add all 16 bits
	adc reqLen+1
	sta .reqEnd+1
	ldy tSegLink,x		; index of next seg to Y reg
	lda .reqEnd		; compare lo byte first
	sec
	sbc tSegAdrLo,y
	sta .cmpLo+1		; save partial result
	lda .reqEnd+1		; compare hi byte
	sbc tSegAdrHi,y
.cmpLo	ora #$11		; self-modified a few lines ago
	beq .noSplitEnd
	bcs .recl		; req end > start of next block, need to reclaim
; need to split current segment into (cur..reqEnd) and (reqEnd..next)
.splitEnd:
	jsr grabSegment		; get a new segment, index in Y (doesn't disturb X)
	lda .reqEnd
	sta tSegAdrLo,y		; reqEnd is start of new segment
	lda .reqEnd+1
	sta tSegAdrHi,y		; save all 16 bits
	lda #0
	sta tSegType,y		; clear flags on new segment
	lda tSegLink,x		; get link to next existing seg
	sta tSegLink,y		; link new segment to it
	tya
	sta tSegLink,x		; link cur segment to new segment
; current segment begins and ends at exactly the right addresses
.noSplitEnd:
	lda #$80		; flag segment as active, not locked, not holding a resource
	sta tSegType,x		; save the flags and type
	lda #0
	sta targetAddr+1	; clear target address for next time
	sta tSegRes,x		; might as well clear resource number too
	lda tSegAdrLo,x		; get address for return
	ldy tSegAdrHi,x		; all 16 bits
	stx segNum		; save seg num in case internal caller routine needs it
	tax			; adr lo to proper register
	rts			; all done!
.reqEnd: !word 0
.reclaimFlg: !byte 0

;------------------------------------------------------------------------------
; Free everything that's inactive, in both the main and aux banks. We do both
; at the same time to guarantee that we never have the main part of a module
; without its aux part, or vice versa.
reclaim: !zone
	!if DEBUG { +prStr : !text "Reclaim.",0 }
	lda isAuxCmd	; save whether current command is aux or not
	pha
	lda #1		; we do aux bank first
	sta isAuxCmd
.outer	ldx isAuxCmd	; grab correct starting segment (0=main mem, 1=aux)
.loop:	ldy tSegLink,x	; grab link to next segment, which we'll need regardless
	lda tSegType,x	; check flag and type of this seg
	bmi .next	; active? Skip it.
	lda #0		; clear all flags and type for this seg
	sta tSegType,x
.next:	tya		; next in chain
	tax		; to X reg index
	bne .loop	; non-zero = not end of chain - loop again
	jsr coalesce	; coalesce all free segments together
	dec isAuxCmd	; do main bank after aux
	bpl .outer	; back around for that bank
	pla
	sta isAuxCmd	; restore aux mode
	rts		; all done

;------------------------------------------------------------------------------
; Join adjacent free blocks of memory, where "free" is defined as having 
; resource type zero and no flags. Note that it will not join inactive blocks
; that still have a resource type. Operates on the current bank only.
coalesce: !zone
	ldx isAuxCmd		; grab correct starting segment (0=main mem, 1=aux)
.loop:	ldy tSegLink,x		; grab link to next segment, which we'll need regardless
	beq .done		; no next segment, nothing to join to ==> done
	lda tSegType,x		; check flag and type of this seg
	ora tSegType,y		; and next seg
	bne .next		; if either is active or has a type, can't combine
	; we can combine the next segment into this one.
	!if DEBUG >= 3 { jsr .debug }
	lda tSegLink,y
	sta tSegLink,x
	stx tmp
	jsr releaseSegment	; releases seg in Y
	ldy tmp			; check this segment again, in a tricky way
.next:	tya			; next in chain
	tax			; to X reg index
	bne .loop		; non-zero = not end of chain - loop again
.done	rts
!if DEBUG >= 3 {
.debug	+prStr : !text "Coalesce ",0
	pha
	txa
	pha
	lda tSegAdrLo,y
	ldx tSegAdrHi,y
	+prXA
	+crout
	pla
	tax
	pla
	rts	
}


;------------------------------------------------------------------------------
shared_scan: !zone
	php		; save carry (set to check active flg, clr to skip check)
	jsr scanForAddr	; scan for block that matches
	beq invalParam	; if not found, invalid
	bcs invalParam	; if addr not exactly equal, invalid
	plp
	lda tSegType,x	; get existing flags
	bcc +		; optionally, skip check of active flag
	bpl invalParam	; must be an active block
+	rts

invalParam: !zone
	jsr inlineFatal : !text "InvalParam", 0

;------------------------------------------------------------------------------
; If the resource is a module, this will locate the corresponding bytecode
; in aux mem. 
; Returns the segment found in X, or 0 if n/a. Sets Z flag appropriately.
shared_byteCodeAlso:
	lda tSegType,x
	and #$F
	cmp #RES_TYPE_MODULE
	beq +
	lda #0
	rts
+	lda #RES_TYPE_BYTECODE
	sta resType
	lda tSegRes,x
	sta resNum
	lda #1
	sta isAuxCmd
	jsr scanForResource
	bne +
	+internalErr 'b'	; it better be present!
+	lda tSegType,x
	rts

;------------------------------------------------------------------------------
mem_lock: !zone
	sec			; do check active flag in scan
	jsr shared_scan		; scan for exact memory block
	ora #$40		; set the 'locked' flag
	sta tSegType,x		; store flags back
	jsr shared_byteCodeAlso
	beq +
	ora #$40
	sta tSegType,x
+	rts			; all done

;------------------------------------------------------------------------------
mem_unlock: !zone
	sec			; do check active flag in scan
	jsr shared_scan		; scan for exact memory block
	and #$BF		; mask off the 'locked' flag
	sta tSegType,x		; store flags back
	jsr shared_byteCodeAlso
	beq +
	and #$BF
	sta tSegType,x
+	rts			; all done

;------------------------------------------------------------------------------
mem_free: !zone
	clc			; do not check for active flg (ok to multiple free)
	jsr shared_scan		; scan for exact memory block
	pha
	and #$40		; check lock flag
	beq +
	jmp invalParam		; must unlock block before freeing it (also prevents accidentally freeing $0000)
+	pla
	and #$3F		; remove the 'active' and 'locked' flags
	sta tSegType,x		; store flags back
	and #$F			; get down to just the type, without the flags
	cmp #RES_TYPE_BYTECODE	; explicitly freeing bytecode obj?
	beq .fatal		; that is not allowed
	cmp #RES_TYPE_MODULE	; freeing a module?
	bne .done		; no, all done
	lda #RES_TYPE_BYTECODE	; we need to look for the corresponding 
	sta resType		; byte code object
	lda #1
	sta isAuxCmd		; it should be over in aux mem
	lda tSegRes,x		; with the matching segment number
	sta resNum
	jsr scanForResource	; go look for the block
	beq .done		; it really ought to be there, but if not, avoid trashing
	lda tSegType,x		; get current flags
	and #$3F		; remove the 'active' and 'locked' flags
	sta tSegType,x		; store flags back
.done	rts			; all done
.fatal	jsr inlineFatal : !text "NoFreeBcode", 0

;------------------------------------------------------------------------------
mem_calcFree: !zone
; Input:  pTmp - address to scan for
; Output: X-reg - segment found (zero if not found), N and Z set for X-reg
;         carry clear if addr == seg start, set if addr != seg start
	lda #0		; clear out free space counter
	sta reqLen
	sta reqLen+1
	ldx isAuxCmd
.loop:	ldy tSegLink,x	; grab link to next segment
	lda tSegType,x	; get type with flags
	bmi .next	; if active, skip to next
	lda tSegAdrLo,y	; found free space: calculate its length
	sec
	sbc tSegAdrLo,x
	sta tmp
	lda tSegAdrHi,y
	sbc tSegAdrHi,x
	sta tmp+1
	lda tmp		; then add that length to the total so far
	clc
	adc reqLen
	sta reqLen
	lda tmp+1
	adc reqLen+1
	sta reqLen+1
.next:	tya		; next in chain
	tax		; to X reg index
	bne .loop	; non-zero = not end of chain - loop again
	ldx reqLen	; get calculated total into X
	ldy reqLen+1	; and Y
	rts		; all done

;------------------------------------------------------------------------------
mem_queueLoad: !zone
	stx resType		; save resource type
	sty resNum		; save resource number
	cpx #RES_TYPE_MODULE	; loading a module?
	beq .module		; extra work for modules
.notMod	jsr scanForResource	; scan to see if we already have this resource in mem
	beq .notFound		; nope, pass to next loader
.found	stx segNum		; save seg num for later
	lda tSegType,x		; get flags
	ora #$80		; reactivate if necessary
	sta tSegType,x		; save modified flag
	lda tSegAdrHi,x		; get seg address hi
	tay			; in Y for return
	lda tSegAdrLo,x		; addr lo
	tax			; in X for return
	lda targetAddr+1	; was specific address requested?
	beq .noChkTarg		; if not, skip target check
	cpx targetAddr		; verify addr lo
	bne .redo
	cpy targetAddr+1	; verify addr hi
	bne .redo
.noChkTarg:
	lda #0
	sta targetAddr+1	; clear targ addr for next time
	rts			; all done
; different address requested than what we have: clear current block.
.redo:	lda #0
	ldx segNum
	sta tSegType,x
	sta tSegRes,x
; fall through to re-load the resource
.notFound:
	ldx resType		; restore res type
	ldy resNum		; and number
	lda #QUEUE_LOAD		; set to re-try same operation
	jmp nextLdVec		; pass to next loader
; extra work for modules
.module	jsr .scanForBytecode	; do we have the aux mem part?
	beq .reload
	stx .modres+1
	jsr .scanForModule
	beq .reload
.modres	ldy #11			; self-modified above
	lda tSegType,y
	ora #$80		; reactivate bytecode if necessary
	sta tSegType,y
	bne .found		; (always taken) we have both parts -- no need for fixups
; The following is for the unusual situation where somehow we have the main memory
; part (the module) without the aux part (the bytecode). If we allowed that to go
; forward, we'd end up running fixups on both parts, and double-fixing-up the module
; is a very bad thing (fixups should not be cumulative). So we force both parts out
; of memory before proceeding.
.reload	jsr .scanForBytecode
	jsr .forceFree		; if bytecode without module, forcibly free it
	jsr .scanForModule
	jsr .forceFree		; if module without bytecode, forcibly free it
; back to normal work
	jsr .notMod		; queue the main memory part of the module
	stx .modRet+1		; save address of main load for eventual return
	sty .modRet+3		; yes, self-modifying
	lda #QUEUE_LOAD
	ldx #RES_TYPE_BYTECODE
	ldy resNum
	jsr aux_dispatch	; load the aux mem part (the bytecode)
	lda #QUEUE_LOAD
	ldx #RES_TYPE_FIXUP	; queue loading of the fixup resource
	ldy resNum
	jsr aux_dispatch
.modRet ldx #11			; all done; return address of the main memory block.
	ldy #22
	rts
.scanForModule:
	ldy #RES_TYPE_MODULE
	lda #0
	beq .scanX
.scanForBytecode:
	ldy #RES_TYPE_BYTECODE
	lda #1
.scanX	sty resType
	sta isAuxCmd
	jmp scanForResource
.forceFree:
	cpx #0
	beq ++
	lda tSegType,x
	and #$C0		; make sure not active and not locked
	beq +
	+internalErr 'L' 	; should have been freed
+	lda #0
	sta tSegType,x		; force reload so fixup works right
++	rts

;------------------------------------------------------------------------------
diskLoader: !zone
	cmp #START_LOAD
	bne +
	jmp disk_startLoad
+	cmp #QUEUE_LOAD
	bne +
	jmp disk_queueLoad
+	cmp #FINISH_LOAD
	bne +
	jmp disk_finishLoad
+	cmp #RESET_MEMORY
	bne +
	rts			; do nothing
+	jsr inlineFatal : !text "InvalCmd", 0

;------------------------------------------------------------------------------
; Calculate a digest of the file and header buffers, and store it in
; bufferDigest.
; Returns: Z if digest the same as last time
calcBufferDigest: !zone
	lda #>headerBuf
	sta .ld1+2
	sta .ld3+2
	lda #>headerBuf+$A00
	sta .ld2+2
	sta .ld4+2
	ldy #0
	sty tmp
	sty tmp+1
	sty tmp+2
	sty tmp+3
	ldx #6		; sum 6 pages in each area - covers first part of heap collect zone also
	clc
.sum	lda tmp
	rol
.ld1	adc $1000,y	; high byte self-modified earlier
	sta tmp

	lda tmp+1
	rol
.ld2	adc $1000,y	; high byte self-modified earlier
	sta tmp+1

	lda tmp+2
	rol
.ld3	adc $1080,y	; high byte self-modified earlier
	sta tmp+2

	lda tmp+3
	rol
.ld4	adc $1080,y	; high byte self-modified earlier
	sta tmp+3

	iny
	iny
	bpl .sum	; every even offset 0..126

	inc .ld1+2	; go to next page
	inc .ld2+2
	inc .ld3+2
	inc .ld4+2
	dex
	bne .ld1

	; Now compare with the old digest, and replace the old digest.
.cmp	ldy #0
	ldx #3
-	lda tmp,x
	cmp bufferDigest,x
	beq +
	iny
+	sta bufferDigest,x
	dex
	bpl -
	cpy #0	; Y=0 if new digest equals old digest
	rts

;------------------------------------------------------------------------------
openPartition: !zone
	!if DEBUG { +prStr : !text "OpenPart ",0 : +prByte curPartition : +crout }
	; Make sure to read header into main mem, even if outer cmd is aux
	lda isAuxCmd
	pha
.retry	lda #0
	sta isAuxCmd		; header buf always in main mem
	lda floppyDrive
	sta .origFloppy
; complete the partition file name, changing "1" to "2" if opening partition 2.
.mkname	lda curPartition
	bne +
	jmp sequenceError
+	clc
	adc #$30		; "0" in lo-bit ProDOS compatible ASCII
	sta partFilename+11
; open the file
.open	lda #<partFilename
	sta pSrc
	lda #>partFilename
	sta pSrc+1
	lda #<headerBuf
	sta pDst
	lda #>headerBuf
	sta pDst+1
	lda #2			; read 2 bytes (which will tell us how long the header is)
	sta reqLen
	lda #0
	sta reqLen+1
	lda #cmdread
	ora floppyDrive		; $80 for drive 2
	sta tmp
	clc
	jsr callProRWTS		; opendir
	bne .flip		; status: zero=ok, 1=err
	sta curMarkPos+1	; by opening we did an implicit seek to zero
	sta curMarkPos+2
	lda #2			; and then we read 2 bytes
	sta curMarkPos
; read the full header
.opened	lda headerBuf		; grab header size
	sec
	sbc #2			; minus size of the size
	sta reqLen		; set to read that much.
	lda headerBuf+1		; hi byte too
	sbc #0
	sta reqLen+1
	lda #<(headerBuf+2)
	sta pDst
	lda #>(headerBuf+2)
	sta pDst+1
	lda #cmdread
	jsr readAndAdj
	inc partFileOpen	; remember we've opened it now
	pla
	sta isAuxCmd		; back to aux if that's what outer was using
	rts
.flip	lda floppyDrive
	eor #$80
	sta floppyDrive
	cmp .origFloppy
	bne .open
; ask user to insert the disk
; TODO: handle dual drive configuration
.insert	+safeHome
	+prStr : !text "Insert disk ",0
	bit $c051
	lda curPartition
	clc
	adc #"0"
	+safeCout
	+waitKey
	+safeHome
	bit $c050
	jmp .retry		; try again
.origFloppy: !byte 0

;------------------------------------------------------------------------------
sequenceError: !zone
	jsr inlineFatal : !text "BadSeq", 0

;------------------------------------------------------------------------------
disk_startLoad: !zone
	txa
	beq sequenceError	; partition zero is illegal
	cpx curPartition	; switching partition?
	stx curPartition	;  (and store the new part num in any case)
	bne .new		; if different, close the old one
	lda partFileOpen
	beq .done		; if nothing already open, we're okay with that.
	jsr calcBufferDigest	; same partition - check that buffers are still intact
	beq .done		; if correct partition file already open, we're done.
.new	lda nSegsQueued		; make sure nothing queued before switching
	bne sequenceError
	jsr closePartFile
.done	rts

;------------------------------------------------------------------------------
startHeaderScan: !zone
	lda #0
	sta pTmp
	lda #>headerBuf
	sta pTmp+1
	ldy #2			; start scanning the partition header just after len
	rts

;------------------------------------------------------------------------------
disk_queueLoad: !zone
	stx resType		; save resource type
	sty resNum		; and resource num
	lda #$FF
	jsr showDiskActivity	; graphical marker that disk activity happening
	inc nSegsQueued		; record the fact that we're queuing a seg
	lda partFileOpen	; check if we've opened the file yet
	bne +			; yes, don't re-open
	jsr openPartition	; open the partition file
+	jsr startHeaderScan	; start scanning the partition header
.scan:	lda (pTmp),y		; get resource type
	beq .notFound		; if zero, this is end of table: failed to find the resource
	iny
	and #$F			; mask off any flags
	cmp resType		; is it the type we're looking for?
	bne .bump3		; no, skip this resource
	lda (pTmp),y		; get resource num
	cmp resNum		; is it the number we're looking for
	bne .bump3		; no, skip this resource
	; Yay! We found the one we want.
	dey
	lda (pTmp),y		; Get the resource type back
	ora #$80		; special mark to say, "We want to load this segment"
	ldx isAuxCmd		; if aux, set an extra-special flag so load will go there
	beq +
	ora #$40		; record that the eventual disk read should go to aux mem
+	sta (pTmp),y		; save modified type byte with the flags added to it
	iny			; advance past resource type now that we're done with it
	iny			; skip over resource num
	lda (pTmp),y		; grab the length in bytes
	tax			; stash away
	iny
	lda (pTmp),y		; hi byte too
	bpl +			; if uncompressed, treat as normal
	iny			; otherwise, advance to get uncomp size
	lda (pTmp),y		; lo byte
	tax
	iny
	lda (pTmp),y		; and hi byte
+	stx reqLen		; save the uncompressed length
	sta reqLen+1		; both bytes
	!if DEBUG { +prStr : !text "ucLen=",0 : +prWord reqLen : +crout }
; Load the bytecode of the gamelib (first) bytecode module at the highest possible point
; (to reduce fragmentation of the rest of aux mem) 
	lda resType
	cmp #RES_TYPE_BYTECODE
	bne +
	lda resNum
	cmp #1
	bne +
	; Take $BFFD - size. Why $BFFD and not $C000? Because decomp temporarily overwrites 3-byte "unnderlap" after.
	lda #$FD
	sec
	sbc reqLen
	sta targetAddr
	lda #$BF
	sbc reqLen+1
	sta targetAddr+1
+	jsr shared_alloc	; reserve memory for this resource (main or aux as appropriate)
	stx tmp			; save lo part of addr temporarily
	ldx segNum		; get the segment number back
	lda resType		; put resource type in segment descriptor
	ora #$80		; add 'active' flag
	sta tSegType,x
	lda resNum		; put resource number in segment descriptor
	sta tSegRes,x
	ldx tmp			; get back lo part of addr
	rts			; success! all done.
.notFound:
	jsr inlineFatal : !text "ResNotFnd", 0
.bump3:	iny			; skip resource number
	iny			; skip lo byte of length
	lda (pTmp),y		; get hi byte of length.
	bpl +			; if hi bit clear, it's not compressed
	iny			; skip uncompressed size too
	iny
+	iny			; advance to next entry
	bpl +			; if Y is small, loop again
	jsr adjYpTmp		; keep it small
+	jmp .scan		; go for more

;------------------------------------------------------------------------------
readAndAdj:
	sta tmp			; store cmd num
	sec			; calling rdwrpart (not opendir)
	jsr callProRWTS		; and seek or read on the underlying file
	; Advance our record of the mark position by the specified # of bytes.
	; reqLen is still intact, because ProRWTS changes its copy in aux zp only
	lda curMarkPos
	clc
	adc reqLen
	sta curMarkPos
	lda curMarkPos+1
	adc reqLen+1
	sta curMarkPos+1
	bcc +
	inc curMarkPos+2
+	rts

;------------------------------------------------------------------------------
disk_seek: !zone
	lda setMarkPos
	sec
	sbc curMarkPos
	sta reqLen
	tax
	lda setMarkPos+1
	sbc curMarkPos+1
	sta reqLen+1
	lda setMarkPos+2
	sbc curMarkPos+2
	bcc .back
	bne .far
	txa			; check for already there
	ora reqLen+1
	bne .go
	rts
.go	lda #cmdseek
	jmp readAndAdj
.back	jsr disk_rewind
	beq disk_seek		; always taken
.far	lda #$FF		; seek forward $FFFF bytes
	sta reqLen
	sta reqLen+1
	jsr .go
	jmp disk_seek		; and try again
.done	rts

;------------------------------------------------------------------------------
disk_finishLoad: !zone
	lda nSegsQueued		; see if we actually queued anything
	beq .done		; if nothing queued, we're done
	jsr disk_rewind		; ProRWTS' file position may have been overwritten; reset it.
	lda headerBuf           ; grab # header bytes
	sta setMarkPos          ; set to start reading at first non-header byte in file
	lda headerBuf+1         ; hi byte too
	sta setMarkPos+1
	lda #0			; hi-hi byte (it's a 24 bit quantity altogether)
	sta setMarkPos+2
	sta .nFixups		; might as well clear fixup count while we're at it
	jsr startHeaderScan	; start scanning the partition header
.scan:	lda (pTmp),y		; get resource type byte
	bne .notdone		; zero = end of header
	; At the end, record new buffer digest, and perform all fixups
	jsr calcBufferDigest
	lda .nFixups		; any fixups encountered?
	beq .done
	jsr doAllFixups		; found fixups - execute and free them
.done	lda #0
	sta nSegsQueued		; we loaded everything, so record that fact
	jmp showDiskActivity	; finally turn off disk activity marker (A is already zero)
.notdone:
	bmi .load		; hi bit set -> queued for load
	iny			; not set, not queued, so skip over it
	iny
	bne .next
.load:	tax			; save aux flag ($40) to decide where to read
	and #$3F		; mask off the aux flag and load flag
	sta (pTmp),y		; to restore the header to its pristine state
	iny
	and #$F			; mask to get just the resource type
	sta resType		; save type for later
	cmp #RES_TYPE_FIXUP	; along the way, keep a lookout for fixups
	bne +
	inc .nFixups
+	lda (pTmp),y		; get resource num
	iny
	sta resNum		; save resource number
	lda #0			; start by assuming main mem
	sta isAuxCmd
	txa			; get the flags back
	and #$40		; check for aux flag
	beq +			; not aux, skip
	inc isAuxCmd		; set aux flag
+	sty .ysave		; Save Y so we can resume scanning later.
	!if DEBUG { jsr .debug1 }
	jsr disk_seek		; move the file pointer to the current block
	ldy .ysave
	lda (pTmp),y		; grab resource length on disk
	sta reqLen		; save for reading
	sta pEnd
	iny
	lda (pTmp),y		; hi byte too
	php			; save hi bit (isCompressed) for later check
	and #$7F		; mask off the flag to get the real (on-disk) length
	sta reqLen+1
	iny			; if compressed, we also have the uncomp len avail next
	lda (pTmp),y		; fetch uncomp length
	sta pEnd		; and save it
	iny
	lda (pTmp),y		; hi byte of uncomp len
	sta pEnd+1		; save uncomp len hi byte
	jsr scanForResource	; find the segment number allocated to this resource
	beq .addrErr		; it better have been allocated
	lda tSegAdrLo,x		; grab the address
	ldy tSegAdrHi,x		; hi byte too
	sta pDst		; and save it for later
	sty pDst+1
	!if DEBUG { jsr .debug2 }
	plp			; retrieve isCompressed flag
	bmi .readAndDecomp	; if so, go do read/decompress thing
	lda #cmdread		; else, just read.
	jsr readAndAdj
.resume	ldy .ysave
.next	lda (pTmp),y		; lo byte of length
	clc
	adc setMarkPos		; advance mark position exactly that far
	sta setMarkPos
	iny
	lda (pTmp),y		; hi byte of length
	bpl +			; if hi bit is clear, resource is uncompressed
	iny			; skip compressed size
	iny
	and #$7F		; mask off the flag
+	adc setMarkPos+1	; bump the high byte of the file mark pos
	sta setMarkPos+1
	bcc +
	inc setMarkPos+2	; account for partitions > 64K
+	iny			; increment to next entry
	bpl +			; if Y index is is small, no need to adjust
	jsr adjYpTmp		; adjust pTmp and Y to make it small again
+	jmp .scan		; back for more
.addrErr:
	jmp invalParam
.readAndDecomp:
	; Calculate end of uncompressed data, and start of compressed data.
	; We overlap the compressed and uncompressed as much as possible, e.g.:
	;   DDDDDDDDDDDDDD
	;        SSSSSSSSSsss  ; sss is the 3-byte 'underlap'
	sta pSrc
	sty pSrc+1
	clc
	adc pEnd
	sta pEnd		; reuse pEnd for end ptr
	tax
	tya
	adc pEnd+1
	sta pEnd+1
	tay
	txa
	adc #3			; this is the max "underlap" required for decompressing overlapped buffers
	bcc +
	iny
+	sec
	sbc reqLen		; then back up by the compressed size
	sta pDst		; and load the compressed data there
	tya
	sbc reqLen+1
	sta pDst+1
	; save the 3 byte underlap bytes because we're going to read over them
	ldy #2
	ldx isAuxCmd
	sta clrAuxRd,x		; from aux mem if appropriate
-	lda (pEnd),y
	pha
	dey
	bpl -
	sta clrAuxRd
	; Now read the raw (compressed) data
	lda #cmdread
	jsr readAndAdj
	!if DEBUG { jsr .debug3 }
	; Stuff was read to into pDst. Now that becomes the source. Decompressor is set up
	; to decompress *from* our pDst to our pSrc. Its labels are swapped.
	ldx isAuxCmd
	sta clrAuxRd,x		; switch to r/w aux mem if appropriate
	sta clrAuxWr,x
	jsr lx47Decomp		; remaining work is for the dedicated decompressor
	; restore the underlap bytes
	ldy #0
-	pla
	sta (pEnd),y
	iny
	cpy #3
	bne -
	sta clrAuxRd
	sta clrAuxWr
	jmp .resume		; always taken

.ysave:		!byte 0
.nFixups:	!byte 0

!if DEBUG {
.debug1:+prStr : !text "Ld t=",0
	pha
	lda resType
	+safePrhex
	+prSpace
	pla
	+prStr : !text "n=",0
	+prByte resNum
	+prStr : !text "aux=",0
	pha
	lda isAuxCmd
	+safePrhex
	+prSpace
	pla
	rts
.debug2:+prStr : !text "rawLen=",0
	+prWord reqLen
	+prStr : !text "dst=",0
	+prWord pDst
	+crout
	rts
.debug3:+prStr : !text "decomp ",0
	+prStr : !text "src=",0
	+prWord pDst
	+prStr : !text "dst=",0
	+prWord pSrc
	+prStr : !text "end=",0
	+prWord pEnd
	+crout
	rts
} ; end DEBUG

;------------------------------------------------------------------------------
adjYpTmp: !zone
	tya
	and #$7F		; adjust Y index to keep it small
	tay
	lda pTmp		; and bump pointer...
	eor #$80		; ...by 128 bytes
	sta pTmp
	bmi +			; if still the same page, we're done
	inc pTmp+1		; go to next page
+	rts

;------------------------------------------------------------------------------
lz4Decompress: !zone
; TODO: replace with LX47
	brk

;------------------------------------------------------------------------------
; Apply fixups to all modules that were loaded this round, and free the fixup
; resources from memory.
doAllFixups: !zone
	!if DEBUG >= 3 { +prStr : !text "Doing all fixups.",0 }
	; Now scan aux mem for fixup segments
	cli			; prevent interrupts while we mess around in aux mem
	ldx #1			; start at first aux mem segment (0=main mem, 1=aux)
.loop:	lda tSegType,x		; grab flags & type
	and #$F			; just type now
	cmp #RES_TYPE_FIXUP
	beq .found
.next:	lda tSegLink,x		; next in chain
	tax			; to X reg index
	bne .loop		; non-zero = not end of chain - loop again
	sei			; allow interrupts again
	lda #1
	sta isAuxCmd
	jmp coalesce		; really free up the fixup blocks by coalescing them into free mem

.found	; Found one fixup seg.
	lda tSegAdrLo,x		; grab its address
	sta pSrc		; save to the accessor routine
	lda tSegAdrHi,x		; hi byte too
	sta pSrc+1

	; Find the corresponding main-mem seg
	stx .resume+1		; save scan position so we can resume later
	lda tSegRes,x		; get seg num for this fixup
	sta resNum		; that's what we're looking for
	lda #RES_TYPE_MODULE	; type module is in main mem
	sta resType		; that's the type
	lda #0			; look in main mem
	sta isAuxCmd
	jsr scanForResource
	bne +			; we better find it
.barf	+internalErr 'F'
+	lda tSegAdrLo,x		; get the segment's address
	sta .mainBase		; and save it
	lda tSegAdrHi,x		; hi byte too
	sta .mainBase+1

	; Find the corresponding aux-mem seg
	lda #RES_TYPE_BYTECODE	; it's of type bytecode
	sta resType
	inc isAuxCmd		; it'll be in aux mem
	jsr scanForResource
	beq .barf		; we better find it
+	lda tSegAdrLo,x
	sta .auxBase
	lda tSegAdrHi,x
	sta .auxBase+1

	!if DEBUG >= 3 { jsr .debug1 }

	; Process the fixups
.proc	jsr .fetchFixup		; get key byte
	pha			; save it aside
	ldx #0			; normal mode
	asl			; get hi bit
	bcc +			; 0=normal, 1=glib
	ldx #4			; glib mode - use glib base addr instead of main addr
+	asl			; check second-to-hi bit, which indicates main=0 or aux=1
	bcs .fxAux		; yes, it's aux mem fixup
.fxMain	jsr .fetchFixup		; get the lo byte of the offset (and set y to 0)
	clc
	adc .mainBase
	sta pDst
	pla
	and #$3F		; mask off the flags
	adc .mainBase+1
	sta pDst+1
	!if DEBUG >= 3 { jsr .debug2 }
	clc
	jsr .adMain		; recalc and store lo byte
	iny
	inx
	jsr .adMain		; recalc and store hi byte
	bne .proc		; always taken
.adMain	lda (pDst),y		; get num to add to offset
	adc .mainBase,x		; add the offset
	sta (pDst),y		; *STORE* back the result
	rts
.fxAux	cmp #$FC		; end of fixups? ($FF shifted up two bits)
	beq .stubs		; if so, go do the stubs
	jsr .fetchFixup		; get lo byte of offset (and set y to 0)
	clc
	adc .auxBase
	sta pDst
	pla
	and #$3F		; mask off the flags
	adc .auxBase+1
	sta pDst+1
	!if DEBUG >= 3 { jsr .debug3 }
	sta setAuxWr
	jsr .adAux		; recalc and store lo byte
	iny
	inx
	jsr .adAux		; recalc and store hi byte
	sta clrAuxWr
	bne .proc		; always taken
.adAux	sta setAuxRd
	lda (pDst),y		; get num to add to offset
	sta clrAuxRd
	adc .mainBase,x		; add the offset
	sta (pDst),y		; *STORE* back the result
	rts
.stubs	; fix up the stubs
	pla			; discard saved value
	lda .mainBase
	sta pDst
	lda .mainBase+1
	sta pDst+1
.stub	ldy #0
	lda (pDst),y
	cmp #$20		; aux mem stubs marked by JSR $3DC
	bne .resume		; not a stub, resume scanning
	iny
	lda (pDst),y
	cmp #$DC
	bne .resume		; not a stub, resume scanning
	iny
	lda (pDst),y
	cmp #$03
	bne .resume		; not a stub, resume scanning
	; found a stub, adjust it.
	!if DEBUG >= 3 { jsr .debug4 }
	clc
	ldx #0
	jsr .adStub
	inx
	jsr .adStub
	lda pDst
	clc
	adc #5
	sta pDst
	bcc .stub
	inc pDst+1
	bne .stub		; always taken
.adStub	iny
	lda (pDst),y
	adc .auxBase,x
	sta (pDst),y
	rts
.resume ldx #11			; self-modified earlier
	lda #0
	sta tSegType,x		; mark this fixup free, so that coalesce can free it
	jmp .next		; go scan for more fixup blocks

.fetchFixup:
	ldy #0
	sta setAuxRd
	lda (pSrc),y
	sta clrAuxRd
	inc pSrc		; and advance the pointer
	bne +
	inc pSrc+1		; hi byte too, if necessary
+	rts
!if DEBUG >= 3 {
.debug1	+prStr : !text "Found fixup, res=",0
	+prByte resNum
	+prStr : !text "mainBase=",0
	+prWord .mainBase
	+prStr : !text "auxBase=",0
	+prWord .auxBase
	+crout
	rts
.debug2	+prStr : !text "  main fixup, addr=",0
	+prWord pDst
	+crout
	rts
.debug3	+prStr : !text "  aux fixup, addr=",0
	+prWord pDst
	+crout
	rts
.debug4	+prStr : !text "  main stub, addr=",0
	+prWord pDst
	+crout
	rts
}
.mainBase !word 0
.auxBase  !word 0
glibBase  !word $1111

;------------------------------------------------------------------------------
; More heap management routines

;------------------------------------------------------------------------------
; Establish a new heap
heapSet: !zone
	txa		; addr must be page-aligned
	beq +
.inval	jmp invalParam
+	lda isAuxCmd
	bne .inval	; must be in main mem
	sec		; check for valid
	jsr shared_scan
	lda tSegAdrLo,y	; end must also be page-aligned
	bne .inval
	; Good to go. Record the start and end pages
	lda pTmp+1
	sta heapStartPg
	tax
	sta heapTop+1
	lda tSegAdrHi,y
	sta heapEndPg
	ldy #0
	sta heapTop
	sty nTypes
	lda targetAddr+1	; see if heap top was specified
	beq +			; no, default to empty heap
	tax			; yes, use specified address
	ldy targetAddr
+	stx heapTop+1		; set heap top
	sty heapTop
	; fall through to:
; Zero memory heapTop.heapEnd
heapClr: !zone
	lda #0
	sta targetAddr+1	; clear target addr now that we're done with heap top
	ldx heapTop
	ldy heapTop+1
.pg	sty .st+2
.st	sta $1000,x	; self-modified above
	inx
	bne .st
	iny
	cpy heapEndPg
	bne .pg
	rts

;------------------------------------------------------------------------------
; Set the table for the next type in order. Starts with type $80, then $81, etc.
; By convention, type $80 is used for the Global object, from which all other
; live objects are reachable (and invalid garbage if not reachable from there).
;
; x=ptr lo, y = ptr hi. 
; Tbl: type size 01-7F including type byte, 
;      then (byte) offsets of ptrs 01-7F within type, 
;      then 0.
heapAddType: !zone
	tya		; save addr hi
	ldy nTypes
	cpy #MAX_TYPES
	bmi +
	+internalErr 'T'
+	sta typeTblH,y	; addr hi
	sta .ld+2
	txa		; addr lo
	sta typeTblL,y
.ld	lda $1000,x	; self-modified above: fetch length byte
	sec
	sbc #1		; adjust to be like a string, in that it doesn't include type byte itself
	sta typeLen,y	; save that too
	inc nTypes	; bump type count
	rts

;------------------------------------------------------------------------------
; Advance all animated resources by one frame.
; Params: X = direction change (0=no change, 1=change).
;             Only applied to resources marked as "forward/backward" order.
; 	  Y = number of frames to skip.
;             Only applied to resources marked as "random" order.
; Returns: non-zero if any animated resources processed
advanceAnims: !zone {
	stx resType	; store direction-change
	sty resNum	; store frames-to-skip
	lda #0
	sta .ret1+1	; clear count of animated resources found
	sta .ret2+1	; clear count of actual changes made
	cli		; no interrupts while we read and write aux mem
	ldx isAuxCmd	; grab starting segment for main or aux mem
	sta clrAuxRd,x	; read and
	sta clrAuxWr,x	;	write aux or main mem, depending on how called
.loop:	lda tSegType,x	; segment flags and type
	bpl .next	; skip non-active
	and #$F		; get type
	cmp #RES_TYPE_PORTRAIT
	beq .anim	; found an animated resource type
	cmp #RES_TYPE_SCREEN
	beq .anim
	cmp #RES_TYPE_TEXTURE
	beq .anim
	bne .next	; not animated; skip
.anim	lda tSegAdrLo,x	; pointer to start of resource
	sta pTmp
	lda tSegAdrHi,x	; ...hi byte too
	sta pTmp+1
	ldy #1
	lda (pTmp),y	; check anim header offset
	dey		; (pf was right - dey was missing here)
	ora (pTmp),y
	beq .next	; if zero, resource is not animated
	txa		; save link number we're scanning
	pha
	inc .ret1+1	; mark the fact that we do have animated resources
	jsr advSingleAnim ; do the work to advance this one resource
	pla		; restore link number we're scanning
	tax
.next	lda tSegLink,x	; next in chain
	tax		; to X reg index
	bne .loop	; non-zero = not end of chain - loop again
.ret1	ldx #0		; return count of number anim resources found (self-modified above)
.ret2	ldy #0		; return count of number we actually changed
	sta clrAuxRd	; read and
	sta clrAuxWr	;	write main mem
	sei		; allow interrupts again now that we're done with aux mem
	rts

; Advance a single animated resource. On entry:
;   pTmp -> base (2-byte offset followed by main image data)
advSingleAnim:
	ldy #0
	lda (pTmp),y	; grab offset
	clc
	adc pTmp	; add to starting addr
	sta tmp		; to obtain addr of animation header
	iny
	lda (pTmp),y	; hi byte too
	adc pTmp+1
	sta tmp+1

	iny		; now y=2, index number of frames
	lda (tmp),y
	adc #$FF	; minus one to get last frame (carry clear from prev add)
	sta .maxFrame	; save it for later reference
	iny		; now y=3, index current frame number
	lda (tmp),y
	sta .curFrame	; save it for comparison later
	!if DEBUG = 2 { jsr .dbgB1 }

.chkr	ldy #0
	lda (tmp),y	; get animation type (1=Forward, 2=Forward/Backward, 3=Forward+Stop, 4=Random)
	cmp #4		; is it random?
	bne .chkfs
	ldx resNum	; number of frames to skip
-	beq .doptch	; if zero, done skipping
	lda #1		; direction = forward
	jsr .fwbk	; advance one frame
	dex		; loop for...
	jmp -		; ...specified number of skips

.chkfs	iny		; index of current dir
	cmp #3		; is it a forward+stop anim?
	bne .chkfb
	lda .curFrame	; compare cur frame
	eor .maxFrame	; to (nFrames-1)
	bne .adv	; if not there yet, advance.
	rts		; we're at last frame; nothing left to do.

.chkfb	cmp #2		; is it a forward+backward anim?
	bne .adv
	lda resType	; get change to dir
	beq .adv	; not changing? just advance
.switch	lda #0		; invert current dir (1->FF, or FF->1)
	sec
	sbc (tmp),y
	sta (tmp),y	; store new dir

.adv	lda (tmp),y	; get current dir
	jsr .fwbk	; advance the frame number in that direction
.doptch	ldy #3		; index current frame
	lda (tmp),y
	cmp .curFrame	; compare to what it was
	bne +		; if not equal, we have work to do
	rts		; no change, all done.
+	inc .ret2+1	; advance count of number of things we actually changed
	pha
	lda .curFrame
	jsr applyPatch	; un-patch old frame
	pla
	jmp applyPatch	; apply patch for the new frame

.fwbk	ldy #3		; index of current frame number
	clc
	adc (tmp),y	; advance in direction
	bpl +		; can only be negative if dir=-1 and we wrapped around
	lda .maxFrame	; go to (previously saved) last frame number
+	dey		; index of number of frames
	cmp (tmp),y	; are we at the limit of number of frames?
	bne +
	lda #0		; back to start
+	iny		; index of current frame number
	sta (tmp),y	; and store it
	!if DEBUG = 2 { jsr .dbgB2 }
	rts

.curFrame !byte 0
.maxFrame !byte 0

!if DEBUG = 2 { 
.dbgin	sta clrAuxRd
	sta clrAuxWr
	bit $c051
	rts
.dbgout	+crout
	+waitKey
	bit $c050
	sta setAuxRd
	sta setAuxWr
	rts
.dbgB1	jsr .dbgin
	+prStr : !text "single ",0
	+prWord pTmp
	+prWord tmp
	+prByte .curFrame
	+prByte .maxFrame
	jmp .dbgout
.dbgB2	jsr .dbgin
	+prStr : !text "fwbk ",0
	+prA
	jmp .dbgout
}

; Patch (or un-patch) an entry. On entry:
;   A-reg - patch number to apply
;   pTmp  - offset just before main image
;   tmp   - anim hdr
; Those pointers are unmodified by this routine.
applyPatch:
	tax		; patch zero?
	beq .done	; if so, nothing to do
	sta reqLen	; index of patch number to find
	!if DEBUG = 2 { jsr .dbgC1 }

	ldx #3
-	lda tmp,x	; copy pointers to load/store data: tmp->pSrc, pTmp->pDst
	sta pSrc,x
	dex
	bpl -

	lda #2		; skip initial offset in dest
	jsr .dstadd
	lda #4		; skip animation header in source
	jsr .srcadd

	; loop to skip patches until we find the right one
-	dec reqLen	; it starts at 1, which means first patch.
	beq +
	ldy #0
	lda (pSrc),y	; low byte of patch len
	pha
	iny
	lda (pSrc),y	; hi byte of patch len
	inx		; -> pSrc+1
	jsr .ptradd	; skip by # pages in patch
	pla		; get lo byte of len back
	jsr .srcadd	; skip pSrc past last partial page in patch
	jmp -
+	!if DEBUG = 2 { jsr .dbgC2 }

	; pSrc now points at the patch to apply
	; pDst now points at the base data to modify
	lda #2		; skip length hdr of this patch
.sksrc	jsr .srcadd
.hunk	jsr .ldsrc	; get # bytes to skip in dst
	cmp #$FF	; check for done marker
	beq .done
	jsr .dstadd	; skip that far in dst
	jsr .ldsrc	; get # bytes to copy from src to dst
	tax
	beq .hunk	; if nothing to copy, go to next hunk
.cplup	lda (pSrc),y	; swap src <-> dst
	pha
	lda (pDst),y
	sta (pSrc),y
	pla
	sta (pDst),y
	iny
	dex		; loop for all bytes to copy
	bne .cplup
	tya		; Y has steadily advanced to the total count
	jsr .dstadd	; advance over copied bytes in dst
	tya		; get the count back again
	bne .sksrc	; advance over copied bytes in src, and process next hunk (always taken)

; get a byte from (pSrc) and advance past it. Sets Y to zero.
.ldsrc	ldy #0
	lda (pSrc),y	; pointer is self-modified
	inc pSrc
	bne .done
	inc pSrc+1
.done	rts

; routine with two entry points; advances either pSrc or pDst
.srcadd	ldx #pSrc	; advance pSrc by A-reg bytes
	bne .ptradd	; always taken
.dstadd	ldx #pDst	; advance pDst by A-reg bytes
.ptradd	clc
	adc 0,x
	sta 0,x
	bcc +
	inc 1,x
+	rts

!if DEBUG = 2 { 
.dbgC1	jsr .dbgin
	+prStr : !text "apply ",0
	+prByte reqLen
	jmp .dbgout
.dbgC2	jsr .dbgin
	+prStr : !text "patch ",0
	+prWord pSrc
	jmp .dbgout
} ; end of debug
} ; end of zone

;------------------------------------------------------------------------------
; Segment tables

tSegLink	!fill MAX_SEGS
tSegType	!fill MAX_SEGS
tSegRes		!fill MAX_SEGS
tSegAdrLo	!fill MAX_SEGS
tSegAdrHi	!fill MAX_SEGS

;------------------------------------------------------------------------------
; Marker for end of the tables, so we can compute its length
tableEnd = *

; Be careful not to grow past the size of the LC bank
!ifdef PASS2 {
!if DEBUG {
	!warn "mmgr spare: ", lx47Decomp - tableEnd
	!if tableEnd >= lx47Decomp {
		!error "Memory manager grew too large."
	}
} ; DEBUG
} else { ;PASS2
  !set PASS2=1
}

} ; end of !pseudopc $D000
hiMemEnd = *
