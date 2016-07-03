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

* = $2000			; PLASMA loader loads us initially at $2000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"
!source "../include/mem.i"
!source "../include/plasma.i"

; Constants
MAX_SEGS	= 96

DO_COMP_CHECKSUMS = 0		; during compression debugging
DEBUG_DECOMP 	= 0
DEBUG		= 0
SANITY_CHECK	= 0		; also prints out request data

; Zero page temporary variables
tmp		= $2	; len 2
pTmp		= $4	; len 2
reqLen		= $6	; len 2
resType		= $8	; len 1
resNum		= $9	; len 1
isAuxCmd	= $A	; len 1
isCompressed	= $B	; len 1
pSrc		= $C	; len 2
pDst		= $E	; len 2
ucLen		= $10	; len 2
checksum	= $12	; len 1

plasmaNextOp	= $F0	; PLASMA's dispatch loop
plasmaXTbl	= $D300	; op table for auXiliary code execution

; Memory buffers
fileBuf		= $4000	; len $400
diskBuf 	= $4400	; len $800
DISK_BUF_SIZE	= $800
diskBufEnd	= $4C00
headerBuf 	= $4C00	; len $1400

; Memory used only during garbage collection
gcHash_first	= $5000	; index is srcLo ^ srcHi; result points into remaining gcHash tables.
gcHash_srcLo	= $5100
gcHash_srcHi	= $5200
gcHash_link	= $5300
gcHash_dstLo	= $5400
gcHash_dstHi	= $5500

; Other equates
prodosMemMap 	= $BF58

;------------------------------------------------------------------------------
!macro callMLI cmd, parms {
	lda #cmd
	ldx #<parms
	ldy #>parms
	jsr _callMLI
}

;------------------------------------------------------------------------------
; Relocate all the pieces to their correct locations and perform patching.
relocate:
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
; set up to copy the ProDOS code from main memory to aux
	bit setLcRW+lcBank1	; only copy bank 1, because bank 2 is PLASMA runtime
	bit setLcRW+lcBank1	; 	write to it
; verify that aux mem exists
	inx
	stx $D000
	sta setAuxZP
	inx
	stx $D000
	cpx $D000
	bne .noaux
	sta clrAuxZP
	dex
	cpx $D000
	beq .gotaux
.noaux	jsr inlineFatal : !text "AuxMemReq",0
.gotaux	ldx #$D0
.pglup	stx .ld+2
	stx .st+2
.bylup	sta clrAuxZP		; get byte from main LC
.ld	lda $D000,y
	sta setAuxZP		; temporarily turn on aux LC
.st	sta $D000,y
	iny
	bne .bylup
	inx			; all pages until we hit $00
	bne .pglup
	sta clrAuxZP		; ...back to main LC
; patch into the main ProDOS MLI entry point
	ldx #$4C	; jmp
	stx $BFBB
	lda #<enterProDOS1
	sta $BFBC
	lda #>enterProDOS1
	sta $BFBD
; patch into the interrupt handler
	stx $BFEB
	lda #<enterProDOS2
	sta $BFEC
	lda #>enterProDOS2
	sta $BFED
; patch into the shared MLI/IRQ exit routine
	stx $BFA0
	lda #<exitProDOS
	sta $BFA1
	lda #>exitProDOS
	sta $BFA2
; now blow away the main RAM LC area as a check
	ldx #$D0
	tya
.clrlup	stx .st2+2
.st2	sta $D000,Y
	iny
	bne .st2
	inx
	cpx #$F8
	bne .clrlup
; it's very convenient to have the monitor in the LC for debugging
	bit setLcWr+lcBank1	; read from ROM, write to LC RAM
.cpmon	stx .ld3+2
	stx .st3+2
.ld3	lda $F800,Y
.st3	sta $F800,Y
	iny
	bne .ld3
	inx
	bne .cpmon
; Place the bulk of the memory manager code into the newly cleared LC
	ldx #>hiMemBegin
.cpmm	stx .ld4+2
.ld4	lda hiMemBegin,y
.st4	sta $D000,y
	iny
	bne .ld4
	inc .st4+2
	inx
	cpx #>(hiMemEnd+$100)
	bne .cpmm
	lda .st4+2
	cmp #$E0
	bcc +
	+internalErr 'N' ; mem mgr got too big!
+

; Patch PLASMA's memory accessors to gain access to writing main LC, and
; to read/write bank 2 in aux LC.

	; first copy the stub code into PLASMA's bank
	bit setLcRW+lcBank2	; PLASMA's bank
	ldx #(plasmaAccessorsEnd-plasmaAccessorsStart-1)
-	lda plasmaAccessorsStart,x
	sta LBXX,x
	dex
	bpl -

	; now record PLASMA's original routines, and patch it to call our stubs
	lda plasmaXTbl+$60	; LBX
	sta oLBX+1
	lda #<LBXX
	sta plasmaXTbl+$60
	lda plasmaXTbl+$61
	sta oLBX+2
	lda #>LBXX
	sta plasmaXTbl+$61

	lda plasmaXTbl+$62	; LWX
	sta oLWX+1
	lda #<LWXX
	sta plasmaXTbl+$62
	lda plasmaXTbl+$63
	sta oLWX+2
	lda #>LWXX
	sta plasmaXTbl+$63

	lda plasmaXTbl+$70	; SBX
	sta oSBX+1
	lda #<SBXX
	sta plasmaXTbl+$70
	lda plasmaXTbl+$71
	sta oSBX+2
	lda #>SBXX
	sta plasmaXTbl+$71

	lda plasmaXTbl+$72	; SWX
	sta oSWX+1
	lda #<SWXX
	sta plasmaXTbl+$72
	lda plasmaXTbl+$73
	sta oSWX+2
	lda #>SWXX
	sta plasmaXTbl+$73

	; fall through into init...

;------------------------------------------------------------------------------
init: !zone
; grab the prefix of the current drive
	lda #<prodosPrefix
	sta getPfxAddr
	lda #>prodosPrefix
	sta getPfxAddr+1
	+callMLI MLI_GET_PREFIX, getPfxParams
	bcc +
	jmp prodosError
+	lda prodosPrefix
	and #$F		; strip off drive/slot, keep string len
	sta prodosPrefix
; switch in mem mgr
	bit setLcRW+lcBank1
	bit setLcRW+lcBank1
; put something interesting on the screen :)
	jsr home
	+prStr : !text "Welcome to Mythos.",0
; close all files
	lda #0
	jsr closeFile
; clear ProDOS mem map so it lets us load stuff anywhere we want
	ldx #$18
	lda #0
.clr:	sta prodosMemMap-1,x
	dex
	bne .clr
; clear the segment tables
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
	sta partFileRef
	sta curPartition
	lda #<diskLoader
	sta nextLdVec+1
	lda #>diskLoader
	sta nextLdVec+2
	lda #1
	sta scanStart+1
; make reset go to monitor
	lda #<monitor
	sta resetVec
	lda #>monitor
	sta resetVec+1
	eor #$A5
	sta resetVec+2
; We'll set up 8 initial segments:
; 0: main $0000 -> 4, active + locked
; 1: aux  $0000 -> 2, active + locked
; 2: aux  $0200 -> 3, inactive
; 3: aux  $C000 -> 0, active + locked
; 4: main $0xxx -> 5, inactive (xxx = end of mem mgr low mem portion)
; 5: main $4000 -> 6, active + locked
; 6: main $6000 -> 7, inactive
; 7: main $BF00 -> 8, active + locked
; 8: main $E000 -> 9, inactive
; 9: main $F800 -> 0, active + locked
; First, the flags
	lda #$C0		; flags for active + locked (with no resource)
	sta tSegType+0
	sta tSegType+1
	sta tSegType+3
	sta tSegType+5
	sta tSegType+7
	sta tSegType+9
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
	lda #2
	sta tSegAdrHi+2
	ldy #$C0
	sty tSegAdrHi+3
	dey
	sty tSegAdrHi+7
	lda #<lastLoMem
	sta tSegAdrLo+4
	lda #>lastLoMem
	sta tSegAdrHi+4
	lda #$40
	sta tSegAdrHi+5
	lda #$60
	sta tSegAdrHi+6
	lda #$E0
	sta tSegAdrHi+8
	lda #$F8
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
; Reserve hi-res page 1
	lda #SET_MEM_TARGET
	ldx #0
	ldy #$20		; at $2000
	jsr main_dispatch
	lda #REQUEST_MEMORY
	ldx #0
	ldy #$20		; length $2000
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
	txa
	pha			; save addr for scanning later
	sty .gomod+2
	tya
	pha
	lda #LOCK_MEMORY	; lock it in forever
	jsr main_dispatch
	ldx #1			; keep open for efficiency's sake
	lda #FINISH_LOAD
	jsr main_dispatch
; find the end of the stubs in the first module
	pla			; hi byte
	sta pTmp+1
	pla			; lo byte
	sta pTmp
	ldy #0
-	lda (pTmp),y
	cmp #$20		; look for first non-JSR
	bne +
	lda pTmp
	clc
	adc #5			; not found, advance by 5 bytes (size of one stub)
	sta pTmp
	bcc -
	inc pTmp+1
	bne -
+	lda pTmp		; store the result
	sta glibBase
	lda pTmp+1
	sta glibBase+1
	ldx #$10		; initial eval stack index
.gomod:	jmp $1111		; jump to module for further bootstrapping

;------------------------------------------------------------------------------
; Special PLASMA memory accessors

plasmaAccessorsStart:
!pseudopc $DF00 {

; Plasma load byte/word with special handling for $D000.DFFF in aux language card
LBXX: !zone {
	clv			; clear V to denote LBX (as opposed to LWX)
	bvc .shld		; always taken
LWXX:	bit monrts		; set V to denote LWX (not LBX)
.shld	lda evalStkH,x		; hi byte of address to load from
	cmp #$D0
	bcc .norm
	cmp #$E0
	bcs .norm
	sty tmp
	jsr l_LXXX		; helper function in low memory because it switches us out (we're in main LC)
	ldy tmp
	jmp plasmaNextOp
.norm	ora evalStkL,x
	beq nullPtr
	bvs oLWX
oLBX:	jmp $1111		; modified to be addr of original LBX
oLWX:	jmp $1111		; modified to be addr of original LWX
nullPtr	sta clrAuxRd
	jsr inlineFatal : !text "NullPtr",0

} ; zone

; Plasma store byte/word with special handling for $D000.DFFF and $E000.FFFF
SBXX: !zone {
	clv			; clear V to denote SBX (as opposed to SWX)
	bvc .shst		; always taken
SWXX:	bit monrts		; set V to denote SWX (not SBX)
.shst	lda evalStkH+1,x	; get hi byte of pointer to store to
	cmp #$D0		; in $0000.CFFF range,
	bcc .norm		;	just do normal store
	sta setLcRW+lcBank2	; PLASMA normally write-protects the LC,
	sta setLcRW+lcBank2	; 	but let's allow writing there. Don't use BIT as it affects V flg.
	cmp #$E0		; in $E000.FFFF range do normal store after write-enable
	bcs .norm
	sty tmp
	jsr l_SXXX		; helper function in low memory because it switches us out (we're in main LC)
	ldy tmp
	inx
	inx
	jmp plasmaNextOp
.norm	ora evalStkL+1,x
	beq nullPtr
	bvs oSWX
oSBX:	jmp $1111		; modified to be addr of original SBX
oSWX:	jmp $1111		; modified to be addr of original SWX
} ; zone
} ; pseudopc
plasmaAccessorsEnd:

;------------------------------------------------------------------------------
; Vectors and debug support code - these go in low memory at $800
loMemBegin: !pseudopc $800 {
	jmp j_main_dispatch
	jmp j_aux_dispatch
	jmp __asmPlasm
	jmp __asmPlasm_bank2

; Vectors for debug macros
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

j_init:
	bit setLcRW+lcBank1	; switch in mem mgr
	bit setLcRW+lcBank1
	jsr init
	bit setLcRW+lcBank2	; back to PLASMA
	rts

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
	jsr setnorm	; set up text mode and vectors
	bit setText
	bit page1
	jsr setvid
	jsr setkbd
	lda $24		; check if we're already at start of screen line
	beq +		; no, no need for CR
	jsr crout	; carriage return to get to start of screen line
+	ldy #40		; set up to print 40 dashes
	lda #'-'
.dash:	jsr cout
	dey
	bne .dash
	+prStr : !text "FATAL ERROR: ",0

	ldx #$FF	; for asm str, max length
	lda (pTmp),y	; first byte (Y ends at 0 in loop above)
	bmi .msg	; 	if hi bit, it's a zero-terminated asm string
	tax		; else it's the length byte of a PLASMA string
	iny		; advance to first char
.msg	lda (pTmp),y
	beq .done
	ora #$80	; set hi bit of PLASMA strings for cout
	jsr cout
	iny
	dex
	bne .msg
.done:	jsr bell
.hang: 	jmp .hang	; loop forever

;------------------------------------------------------------------------------
; Normal entry point for ProDOS MLI calls. This patches the code at $BFBB.
enterProDOS1: !zone
	pla		; saved A reg
	sta .ld2+1
	pla		; lo byte of ret addr
	sta .ld1+1
	pla		; hi byte of ret addr
	sta setAuxZP	; switch to aux stack/ZP/LC
	pha		; hi byte of ret addr
.ld1	lda #11		; self-modified earlier
	pha		; lo byte of ret addr
.ld2	lda #11		; saved A reg
	pha
	lda $E000	; this is what the original code at $BFBB did
	jmp $BFBE	; jump back in where ProDOS enter left off

;------------------------------------------------------------------------------
; Entry point for ProDOS interrupt handler. This patches the code at $BFEB.
enterProDOS2: !zone
	pla		; saved P reg
	sta .ld2+1
	pla		; ret addr lo
	sta .ld1+1
	pla		; ret addr hi
	sta setAuxZP	; switch to aux stack/ZP/LC
	pha
.ld1	lda #11		; self-modified earlier
	pha
.ld2	lda #11		; 	ditto
	pha
	bit $C08B	; this is what the original code at $BFEB did
	jmp $BFEE	; back to where ProDOS left off

;------------------------------------------------------------------------------
; Shared exit point for ProDOS MLI and IRQ handlers. This patches the code
; at $BFA0.
exitProDOS: !zone
	pla			; saved A reg
	sta .ld3+1
	pla			; P-reg for RTI
	sta .ld2+1
	pla			; hi byte of ret addr
	sta .ld1+1
	pla			; lo byte of ret addr
	sta clrAuxZP		; back to main stack/ZP/LC
	pha			; lo byte of ret addr
.ld1	lda #11			; self-modified earlier
	pha			; hi byte of ret addr
.ld2	lda #11			;	ditto
	pha			; P-reg for RTI
.ld3	lda #11			; self-modified earlier (the saved A reg)
	; Note! We leave LC bank 1 enabled, since that's where the memory
	; manager lives, and it's the only code that calls ProDOS.
	rti			; RTI pops P-reg and *exact* return addr (not adding 1)

;------------------------------------------------------------------------------
; Replacement memory accessors for PLASMA, so we can utilize language card mem
; including the hard-to-reach aux-bank $D000.DFFF

l_LXXX:	!zone {
	sta .ld+2
	lda evalStkL,x
	sta .ld+1
	sta setAuxZP
	ldy #1
.ldlup	sta .lhb+1
.ld	lda $1111,y		; self-modified above
	dey
	bne .ldlup
	sta clrAuxZP
	sta evalStkL,x
	bvc +
.lhb	lda #11			; self-modified above
	sta evalStkH,x
+	rts
}

l_SXXX:	!zone {
	sta .st+2		; in $D000.DFFF range, we jump through hoops to write to AUX LC
	lda evalStkL+1,x	; lo byte of pointer
	sta .st+1
	lda evalStkH,x		; hi byte of value to store
	sta .shb+1
	lda evalStkL,x		; lo byte of value
	sta setAuxZP		; switch to aux LC
+	ldy #0
.st	sta $1111,y		; self-modified above
	bvc +			; for SBX, don't write hi byte
.shb	lda #11			; self-modified above
	iny
	cpy #2
	bne .st			; loop to write hi byte also
+	sta clrAuxZP		; back to main LC
	rts
}

;------------------------------------------------------------------------------
; Utility routine for convenient assembly routines in PLASMA code. 
; Params: Y=number of parameters passed from PLASMA routine
; 0. (optional) switch to Language Card bank 2
; 1. Save PLASMA's X register index to evalStk
; 2. Verify X register is in the range 0-$10
; 3. Load the *last* parameter into A=lo, Y=hi
; 4. Run the calling routine (X still points into evalStk for add'l params if needed)
; 5. Restore PLASMA's X register, and advance it over the parameter(s)
; 6. Store A=lo/Y=hi into PLASMA return value
; 7. Return to PLASMA
__asmPlasm_bank2:
	bit setLcRW+lcBank2
	bit setLcRW+lcBank2
__asmPlasm: !zone
	cpx #$11
	bcs .badx	; X must be in range 0..$10
	; adjust PLASMA stack pointer to skip over params
	dey		; leave 1 slot for ret value
	sty tmp
	pla		; save address of calling routine, so we can call it
	tay
	pla
	iny
	sty .jsr+1
	bne .noadd
	adc #1
.noadd
	sta .jsr+2
	txa
.add	adc tmp		; carry cleared by cpx above
	pha		; and save that
	cmp #$11	; again, X must be in range 0..$10
	bcs .badx
frameChk:
	lda $1111	; self-modified by init code
	cmp #$AA	; check for sentinel value
	bne .badFrame
	lda evalStkL,x	; get last param to A=lo
	ldy evalStkH,x	; ...Y=hi
.jsr	jsr $1111	; call the routine to do work
	sta tmp		; stash return value lo
	pla
	tax		; restore adjusted PLASMA stack pointer
	lda tmp
	sta evalStkL,x	; store return value
	tya
	sta evalStkH,x
	rts		; and return to PLASMA interpreter
.badx	; X reg ran outside valid range. Print and abort.
	+prStr : !text $8D,"X=",0
	+prX
	jsr inlineFatal : !text "PlasmXRng",0
.badFrame:
	jsr inlineFatal : !text "PlasmFrm",0

;------------------------------------------------------------------------------
; Debug code to support macros

; Fetch a byte pointed to by the first entry on the stack, and advance that entry.
_getStackByte !zone {
	inc $101,x
	bne +
	inc $102,x
+	lda $101,x
	sta .ld+1
	lda $102,x
	sta .ld+2
.ld:   	lda $2000
	rts
}

; Support to print a string following the JSR, in high or low bit ASCII, 
; terminated by zero. If the string has a period "." it will be followed 
; automatically by a carriage return. Preserves all registers.
__writeStr: !zone {
	jsr iosave
	tsx
.loop:	jsr _getStackByte
	beq .done
	jsr cout
	cmp #$AE	; "."
	bne .loop
	jsr crout
	jmp .loop
.done:	jmp iorest
}

__prByte: !zone {
	jsr iosave
	ldy #0
	; fall through to _prShared...
}

_prShared: !zone {
	tsx
	jsr _getStackByte
	sta .ld+1
	jsr _getStackByte
	sta .ld+2
.ld:	lda $2000,y
	jsr prbyte
	dey
	bpl .ld
	+prSpace
	jmp iorest
}

__prSpace: !zone {
	php
	pha
	lda #$A0
	jsr cout
	pla
	plp
	rts
}

__prWord: !zone {
	jsr iosave
	ldy #1
	bne _prShared	; always taken
}

__prA: !zone {
	php
	pha
	jsr prbyte
	pla
	plp
	rts
}
	
__prX: !zone {
	php
	pha
	txa
	jsr prbyte
	pla
	plp
	rts
}

__prY: !zone {
	php
	pha
	tya
	jsr prbyte
	pla
	plp
	rts
}

__crout: !zone {
	php
	pha
	jsr crout
	pla
	plp
	rts
}

__waitKey: !zone {
	jsr iosave
	jsr rdkey
	jmp iorest
}

; Support for very compact abort in the case of internal errors. Prints
; a single-character code as a fatal error.
__internalErr: !zone {
	+prStr : !text $8D,"err=",0
	tsx
	jsr _getStackByte
	jsr cout
	jsr inlineFatal : !text "Internal",0
}


; Call MLI from main memory rather than LC, since it lives in aux LC.
_callMLI:	sta .cmd
		stx .params
		sty .params+1
		jsr mli
.cmd		!byte 0
.params		!word 0
		rts

; Our ProDOS param blocks can't be in LC ram
openParams:	!byte 3		; param count
		!word filename	; pointer to file name
		!word fileBuf	; pointer to buffer
openFileRef:	!byte 0		; returned file number

; ProDOS prefix of the boot disk
prodosPrefix: !fill 16

; Buffer for forming the full filename
filename: !fill 28	; 16 for prefix plus 11 for "/GAME.PART.1"

readParams:	!byte 4		; param count
readFileRef:	!byte 0		; file ref to read
readAddr:	!word 0
readLen:	!word 0
readGot:	!word 0

setMarkParams:	!byte 2		; param count
setMarkFileRef:	!byte 0		; file reference to set mark in
setMarkPos:	!byte 0		; mark position (3 byte integer)
		!byte 0
		!byte 0

closeParams:	!byte 1		; param count
closeFileRef:	!byte 0		; file ref to close

getPfxParams:	!byte 1		; param count
getPfxAddr:	!word 0		; pointer to buffer

multiDiskMode:	!byte 0		; hardcoded to YES for now

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
; Heap management routines

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
	jsr startHeapScan
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

!if DEBUG = 0 {
debugOnly:
	jsr inlineFatal : !text "DebugOnly",0	
}

; Verify integrity of memory manager structures
memCheck: !zone
!if DEBUG = 0 { jmp debugOnly }
!if DEBUG {
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
} ; if DEBUG

heapCorrupt:
       ldx pTmp
       lda pTmp+1
       jsr prntax
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
	bit monrts	; set V flag
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
	bit monrts		; set V flag to mark phase 3
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
.outer	clc			; clc = do not add to hash
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

heapCollect: !zone
	lda partFileRef		; check if the buffer space is already in use
	bne .partOpen
	jsr gc1_mark		; mark reachable blocks
	jsr gc2_sweep		; sweep them into one place
	jsr gc3_fix		; adjust all pointers
	jsr heapClr		; and clear newly freed space
	ldx heapTop		; return new top-of-heap in x=lo/y=hi
	ldy heapTop+1
	rts
.partOpen:
	jsr inlineFatal : !text "NdClose",0

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
partFileRef: 	!byte 0
fixupHint:	!word 0

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
.found:	sec		; start out assuming addr != seg start
	lda pTmp	; compare scan address lo...
	eor tSegAdrLo,x	; ... to seg start lo
	bne +		; if not equal, leave carry set
	lda pTmp+1	; hi byte
	eor tSegAdrHi,x	; to hi byte
	bne +		; again, if not equal, leave carry set
	clc		; addr is equal, clear carry
+	txa
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
!if DEBUG {
+	cmp #DEBUG_MEM
	bne +
	jmp printMem
}
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
+	jmp nextLdVec	; Pass command to next chained loader

;------------------------------------------------------------------------------
; Sanity check mode
!if SANITY_CHECK {
saneStart: !zone {
	pha
	tya
	pha
	txa
	pha
	jsr saneCheck
	pla
	tax
	pla
	tay
	+prChr 'M'
	lda isAuxCmd
	beq +
	+prChr 'a'
+	pla : pha : +prA
	+prX : +prY
	pla
	rts
}

saneCheck: !zone {
	lda $BF00
	cmp #$4C
	beq +
	+internalErr 'S'
+	lda $E1
	cmp #$BE
	bcc +
	+internalErr 's'
+	rts
}

saneEnd: !zone {
	pha
	tya
	pha
	txa
	pha
	jsr saneCheck
	+prChr 'm'
	+crout
	pla
	tax
	pla
	tay
	pla
	rts
}
}

;------------------------------------------------------------------------------
!if DEBUG {
printMem: !zone
	lda $24		; check if we're already at start of screen line
	beq +		; no, no need for CR
	jsr crout	; carriage return to get to start of screen line
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
	jsr cout
	lda tSegAdrHi,y
	jsr prbyte
	lda tSegAdrLo,y
	jsr prbyte
	lda #','
	jsr cout
	lda #'L'
	jsr cout
	lda tSegLink,y
	tax
	lda tSegAdrLo,x
	sec
	sbc tSegAdrLo,y
	pha
	lda tSegAdrHi,x
	sbc tSegAdrHi,y
	jsr prbyte
	pla
	jsr prbyte
	lda tSegType,y
	tax
	and #$40
	beq +
	lda #'*'
	bne ++
+	lda #'+'
	cpx #0
	bmi ++
	lda #'-'
++	jsr cout
	txa
	and #$F
	tax
	jsr prhex
	txa
	beq +
	lda #':'
	jsr cout
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
	beq +
	jmp .printSegs
+	jmp crout
}

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
	tay
	and #$40		; segment locked?
	bne .next		; yes, skip it
	tya			; no, get back flags
	and #$7F		; mask off the 'active' bit
	sta tSegType,x		; save it back
.next:	lda tSegLink,x		; get link to next seg
	tax			; to X reg, and test if end of chain (x=0)
	bne .inactivate		; no, not end of chain, so loop again
;;	lda #0			; default to putting fixups at $8000, to avoid fragmentation
	sta fixupHint
	lda #$80		
	sta fixupHint+1
	rts

;------------------------------------------------------------------------------
outOfMemErr: !zone
	!if DEBUG { jsr printMem }
	jsr inlineFatal : !text "OutOfMem", 0

;------------------------------------------------------------------------------
reservedErr: !zone
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
	bmi outOfMemErr		; so if it's second time, give up
	jsr reclaim		; first time, do a reclaim pass
	jmp .try		; and try again
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
	!if DEBUG >= 2 { jsr .debug }
	lda tSegLink,y
	sta tSegLink,x
	stx tmp
	jsr releaseSegment	; releases seg in Y
	ldy tmp			; check this segment again, in a tricky way
.next:	tya			; next in chain
	tax			; to X reg index
	bne .loop		; non-zero = not end of chain - loop again
.done	rts
!if DEBUG >= 2 {
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
	!if DEBUG { jsr printMem }
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
.module	lda #RES_TYPE_BYTECODE
	sta resType
	lda #1
	sta isAuxCmd
	jsr scanForResource	; do we have the aux mem part?
	beq .reload
	stx .modres+1
	lda #RES_TYPE_MODULE
	sta resType
	lda #0
	sta isAuxCmd
	jsr scanForResource	; do we have the main mem part?
	beq .reload
.modres	ldy #11			; self-modified above
	lda tSegType,y
	ora #$80		; reactivate bytecode if necessary
	sta tSegType,y
	bne .found		; (always taken) we have both parts -- no need for fixups
.reload	lda #RES_TYPE_MODULE
	sta resType
	lda #0
	sta isAuxCmd
	jsr .notMod		; queue the main memory part of the module
	stx .modRet+1		; save address of main load for eventual return
	sty .modRet+3		; yes, self-modifying
	lda #QUEUE_LOAD
	ldx #RES_TYPE_BYTECODE
	ldy resNum
	jsr aux_dispatch	; load the aux mem part (the bytecode)
	; try to pick a location for the fixups that we can free without fragmenting everything.
	ldx fixupHint
	ldy fixupHint+1
	jsr scanForAddr		; locate block containing target addr
	beq .frag		; block gone? um, how. Well, whatever.
	lda tSegType,x		; check flags
	bmi .frag		; if already active, we'll just have to suffer the fixup creating fragmentation
	lda fixupHint		; Okay, found a good place to put it
	sta targetAddr
	lda fixupHint+1
	sta targetAddr+1
.frag	lda #QUEUE_LOAD
	ldx #RES_TYPE_FIXUP	; queue loading of the fixup resource
	ldy resNum
	jsr aux_dispatch
	lda fixupHint		; advance hint for next fixup by the size of this fixup
	clc
	adc reqLen
	sta fixupHint
	lda fixupHint+1		; hi byte too
	adc reqLen+1
	sta fixupHint+1
.modRet ldx #11			; all done; return address of the main memory block.
	ldy #22
	rts

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
openPartition: !zone
	!if DEBUG { +prStr : !text "Opening part file ",0 : +prByte curPartition : +crout }
; complete the partition file name, changing "1" to "2" if we're in multi-disk mode
; and opening partition 2.
.mkname	ldx #1
	ldy #1
-	lda prodosPrefix,x
	sta filename,y
	cmp #$31		; "1"
	bne +
	lda multiDiskMode
	beq +			; don't change if single-disk mode
	lda curPartition
	cmp #2
	bcc +
	lda #$32		; "2"
	sta filename,y
+	cpx prodosPrefix	; done with full length of prefix?
	beq +
	inx
	iny
	bne -			; always taken
+	ldx #0
-	lda .fileStr,x
	beq +++
	cmp #$31		; "1"
	bne ++
	lda curPartition
	bne +
	jmp sequenceError	; partition number must be >= 1
+	clc
	adc #$30
++	sta filename,y
	inx
	iny
	bne -			; always taken
+++	dey
	sty filename		; total length
; open the file
.open	+callMLI MLI_OPEN, openParams
	bcc .opened
	cmp #$46		; file not found?
	bne +
	lda #1			; enter into
	sta multiDiskMode	;   multi-disk mode
	bne .mkname		; and retry
+	cmp #$45		; volume not found?
	beq .insert		; ask user to insert the disk
	jmp prodosError		; no, misc error - print it and die
; grab the file number, since we're going to keep it open
.opened	lda openFileRef
	sta partFileRef
	sta readFileRef
; Read the first two bytes, which tells us how long the header is.
	lda #<headerBuf
	sta readAddr
	lda #>headerBuf
	sta readAddr+1
	lda #2
	sta readLen
	lda #0
	sta readLen+1
	jsr readToMain
	lda headerBuf		; grab header size
	sta readLen		; set to read that much. Will actually get 2 extra bytes,
				; but that's no biggie.
	lda headerBuf+1		; hi byte too
	sta readLen+1
	lda #2			; read just after the 2-byte length
	sta readAddr
	jmp readToMain		; finish by reading the rest of the header
; ask user to insert the disk
.insert	jsr home
	+prStr : !text "Insert disk ",0
	bit $c051
	lda curPartition
	clc
	adc #"0"
	jsr cout
	jsr rdkey
	jsr home
	bit $c050
	jmp .open		; try again
.fileStr !raw "/GAME.PART.1",0	; 'raw' chars to get lo-bit ascii that ProDOS likes.

;------------------------------------------------------------------------------
sequenceError: !zone
	jsr inlineFatal : !text "BadSeq", 0

;------------------------------------------------------------------------------
prodosError: !zone
	pha
	lsr
	lsr
	lsr
	lsr
	jsr .digit
	sta .num
	pla
	jsr .digit
	sta .num+1
	jsr inlineFatal
.msg:	!text "ProDOSErr $"
.num:	!text "xx"
	!byte 0
.digit:	and #$F
	ora #$B0
	cmp #$BA
	bcc +
	adc #6
+	rts

;------------------------------------------------------------------------------
disk_startLoad: !zone
; Make sure we don't get start without finish
	txa
	beq sequenceError	; partition zero is illegal
	cpx curPartition	; ok to open same partition twice without close
	beq .nop
	lda curPartition
	bne sequenceError	; error to open new partition without closing old
; Just record the partition number; it's possible we won't actually be asked
; to queue anything, so we should put off opening the file.
	stx curPartition
.nop	rts

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
	lda partFileRef		; check if we've opened the file yet
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
	!if DEBUG { +prStr : !text "uclen=",0 : +prWord reqLen : +crout }
	jsr shared_alloc	; reserve memory for this resource (main or aux as appropriate)
	stx tmp			; save lo part of addr temporarily
	ldx segNum		; get the segment number back
	lda resType		; put resource type in segment descriptor
	ora #$80		; add 'active' flag
	sta tSegType,x
	lda resNum		; put resource number in segment descriptor
	sta tSegRes,x
	ldx tmp			; get back lo part of addr
	rts			; success! all done.
.bump3:	iny			; skip resource number
	iny			; skip lo byte of length
	lda (pTmp),y		; get hi byte of length.
	bpl +			; if hi bit clear, it's not compressed
	iny			; skip uncompressed size too
	iny
+	iny			; advance to next entry
	bpl .scan		; if Y is small, loop again
	jsr adjYpTmp		; keep it small
	jmp .scan		; go for more
.notFound:
	+prStr : !text "p=",0 : +prByte curPartition
	+prStr : !text "t=",0 : +prByte resType
	+prStr : !text "n=",0 : +prByte resNum
	jsr inlineFatal : !text "ResNotFnd", 0
.resLen: !byte 0
;------------------------------------------------------------------------------
disk_finishLoad: !zone
	stx .keepOpenChk+1	; store flag telling us whether to keep open (1) or close (0)
	lda partFileRef		; see if we actually queued anything (and opened the file)
	bne .work		; non-zero means we have work to do
	txa
	bne +
	sta curPartition	; if "closing", clear the partition number
+	rts			; nothing to do - return immediately
.work	sta setMarkFileRef	; copy the file ref number to our MLI param blocks
	sta readFileRef
	lda headerBuf		; grab # header bytes
	sta setMarkPos		; set to start reading at first non-header byte in file
	lda headerBuf+1		; hi byte too
	sta setMarkPos+1
	lda #0
	sta setMarkPos+2
	sta .nFixups
	jsr startHeaderScan	; start scanning the partition header
.scan:	lda (pTmp),y		; get resource type byte
	bne .notEnd		; non-zero = not end of header
	; end of header. Check if we need to close the file.
.keepOpenChk:
	lda #11			; self-modified to 0 or 1 at start of routine
	bne +			; 1 means leave open, 0 means close
	!if DEBUG { +prStr : !text "Closing partition file.",0 }
	lda partFileRef		; close the partition file
	jsr closeFile
	lda #0			; zero out...
	sta partFileRef		; ... the file reference so we know it's no longer open
	sta curPartition	; ... and the partition number
+	lda .nFixups		; any fixups encountered?
	beq +
	jsr doAllFixups		; found fixups - execute and free them
+	rts
.notEnd	bmi .load		; hi bit set -> queued for load
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
	lda (pTmp),y		; grab resource length on disk
	sta reqLen		; save for reading
	sta ucLen
	iny
	lda (pTmp),y		; hi byte too
	sta isCompressed	; save flag
	and #$7F		; mask off the flag to get the real (on-disk) length
	sta reqLen+1
	bit isCompressed	; retrieve flag
	bpl +			; if uncompressed, we also have the ucLen now
	iny			; is compressed
	lda (pTmp),y		; fetch uncomp length
	sta ucLen		; and save it
	iny
	lda (pTmp),y		; hi byte of uncomp len
+	sta ucLen+1		; save uncomp len hi byte
	jsr scanForResource	; find the segment number allocated to this resource
	beq .addrErr		; it better have been allocated
	lda tSegAdrLo,x		; grab the address
	sta pDst		; and save it to the dest point for copy or decompress
	lda tSegAdrHi,x		; hi byte too
	sta pDst+1
	!if DEBUG { jsr .debug2 }
	+callMLI MLI_SET_MARK, setMarkParams  ; move the file pointer to the current block
	bcs .prodosErr
!if DEBUG >= 2 { +prStr : !text "Deco.",0 }
	jsr lz4Decompress	; decompress (or copy if uncompressed)
!if DEBUG >= 2 { +prStr : !text "Done.",0 }
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
.prodosErr:
	jmp prodosError
.addrErr:
	jmp invalParam

.ysave:		!byte 0
.nFixups:	!byte 0

!if DEBUG {
.debug1:+prStr : !text "Ld t=",0
	pha
	lda resType
	jsr prhex
	lda #" "
	jsr cout
	pla
	+prStr : !text "n=",0
	+prByte resNum
	+prStr : !text "aux=",0
	pha
	lda isAuxCmd
	jsr prhex
	lda #" "
	jsr cout
	pla
	rts
.debug2:+prStr : !text "rawLen=",0
	+prWord reqLen
	+prStr : !text "dst=",0
	+prWord pDst
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
closeFile: !zone
	sta closeFileRef
	+callMLI MLI_CLOSE, closeParams
	bcs .prodosErr
	rts
.prodosErr:
	jmp prodosError

;------------------------------------------------------------------------------
readToMain: !zone
	+callMLI MLI_READ, readParams
	bcs .err
	rts
.err:	jmp prodosError

;------------------------------------------------------------------------------
readToBuf: !zone
; Read as much data as we can, up to min(compLen, bufferSize) into the diskBuf.
	lda #0
	sta readAddr		; buffer addr always on even page boundary
	sta pSrc
	lda #>diskBuf		; we're reading into a buffer in main mem
	sta readAddr+1
	sta pSrc+1		; restart src pointer at start of buffer
.nextGroup:
	ldx reqLen
	lda reqLen+1		; see how many pages we want
	cmp #>DISK_BUF_SIZE	; less than our max?
	bcc +			; yes, read exact amount
	lda #>DISK_BUF_SIZE	; no, limit to size of buffer
	ldx #0
+	stx readLen
	sta readLen+1		; save number of pages
	jsr readToMain		; now read
	lda reqLen		; decrement reqLen by the amount we read
	sec
	sbc readLen
	sta reqLen
	lda reqLen+1		; all 16 bits of reqLen
	sbc readLen+1
	sta reqLen+1
	ldy #0			; index for reading first byte
	rts			; all done

;------------------------------------------------------------------------------
lz4Decompress: !zone
; Input: pSrc - pointer to source data
;        pDst - pointer to destination buffer
;        ucLen  - length of *destination* data (16-bit)
;	 isCompressed - if hi bit set, decompress; if not, just copy.
; All inputs are destroyed by the process.

!macro LOAD_YSRC {
	lda (pSrc),y		; load byte
	iny			; inc low byte of ptr
	bne +			; non-zero, done
	jsr nextSrcPage		; zero, need to go to next page
+
}

	!if DEBUG_DECOMP { jsr .debug1 }
	jsr readToBuf		; read first pages into buffer
	ldx #<clrAuxWr		; start by assuming write to main mem
	ldy #<clrAuxRd		; and read from main mem
	lda isAuxCmd		; if we're decompressing to aux...
	beq +			; no? keep those values
	inx			; yes, write to aux mem
	iny			; and read from aux mem
+ 	stx .auxWr1+1		; set all the write switches for aux/main
	stx .auxWr3+1
	stx .auxWr2+1
	sty .auxRd1+1		; and the read switches too
+	ldx pDst		; calculate the end of the dest buffer
	txa			; also put low byte of ptr in X (where we'll use it constantly)
	clc
	adc ucLen		; add in the uncompressed length
	sta .endChk1+1		; that's what we'll need to check to see if we're done
	lda ucLen+1		; grab, but don't add, hi byte of dest length
	adc #0			; no, we don't add pDst+1 - see endChk2
	sta .endChk2+1		; this is essentially a count of dest page bumps
	lda pDst+1		; grab the hi byte of dest pointer
	sta .dstStore1+2	; self-modify our storage routines
	sta .dstStore2+2
	ldy pSrc		; Y will always track the hi byte of the source ptr
	lda #0			; so zero out the low byte of the ptr itself
	sta pSrc
	!if DO_COMP_CHECKSUMS {
	sta checksum
	}
	bit isCompressed	; check compression flag
	bpl .goLit		; not compressed? Treat as a single literal sequence.
	sta ucLen+1		; ucLen+1 always needs to be zero
	; Grab the next token in the compressed data
.getToken:
	+LOAD_YSRC		; load next source byte
	pha			; save the token byte. We use half now, and half later
	lsr			; shift to get the hi 4 bits...
	lsr
	lsr			; ...into the lo 4 bits
	lsr
	beq .endChk1		; if ucLen=0, there is no literal data.
	cmp #$F			; ucLen=15 is a special marker
	bcc +			; not special, go copy the literals
	jsr .longLen		; special marker: extend the length
+	sta ucLen		; record resulting length (lo byte)
.goLit:
	!if DEBUG_DECOMP { jsr .debug2 }
+
.auxWr1	sta setAuxWr		; this gets self-modified depending on if target is in main or aux mem
.litCopy:			; loop to copy the literals
	+LOAD_YSRC		; grab a literal source byte
.dstStore1:
	sta $1100,x		; hi-byte gets self-modified to point to dest page
	!if DO_COMP_CHECKSUMS {
	eor checksum
	sta checksum
	}
	inx			; inc low byte of ptr
	bne +			; non-zero, done
	jsr .nextDstPage	; zero, need to go to next page
+	dec ucLen		; count bytes
	bne +			; low count = 0?
	lda ucLen+1		; hi count = 0?
	beq .endChk		; both zero - end of loop
+	lda ucLen		; did low byte wrap around?
	cmp #$FF
	bne .litCopy		; no, go again
	dec ucLen+1		; yes, decrement hi byte
	jmp .litCopy		; and go again
.endChk	sta clrAuxWr		; back to writing main mem	  
.endChk1:
	cpx #11			; end check - self-modified earlier
	bcc .decodeMatch	; if less, keep going
.endChk2:
	lda #0              	; have we finished all pages? - self modified and decremented
	bmi .endBad		; negative? that's very bad (because we never have blocks >= 32Kbytes)
	bne .decodeMatch	; non-zero? keep going.
	bit isCompressed
	bpl +			; if not compressed, no extra work at end
	pla			; toss unused match length
	!if DO_COMP_CHECKSUMS { jsr .verifyCksum }
+	rts			; all done!
.endBad	+internalErr 'O'	; barf out
	; Now that we've finished with the literals, decode the match section
.decodeMatch:
	+LOAD_YSRC		; grab first byte of match offset
	sta tmp			; save for later
	cmp #0
	bmi .far		; if hi bit is set, there will be a second byte
	!if DO_COMP_CHECKSUMS { jsr .verifyCksum }
	lda #0			; otherwise, second byte is assumed to be zero
	beq .doInv		; always taken
.far:	+LOAD_YSRC		; grab second byte of offset
	asl tmp			; toss the unused hi bit of the lo byte
 	lsr			; shift out lo bit of the hi byte
	ror tmp			; to fill in the hi bit of the lo byte
.doInv:	sta tmp+1		; got the hi byte of the offset now
	lda #0			; calculate zero minus the offset, to obtain ptr diff
	sec
	sbc tmp
	sta .srcLoad+1		; that's how much less to read from
	lda .dstStore2+2	; same with hi byte of offset
	sbc tmp+1
	sta .srcLoad+2		; to hi byte of offsetted pointer
	!if DEBUG_DECOMP { jsr .debug3 }
.getMatchLen:
	pla			; recover the token byte
	and #$F			; mask to get just the match length
	clc
	adc #4			; adjust: min match is 4 bytes
	cmp #$13		; was it the special value $0F? ($F + 4 = $13)
	bne +			; if not, no need to extend length
	jsr .longLen		; need to extend the length
+	sty tmp			; save index to source pointer, so we can use Y...
	!if DEBUG_DECOMP { sta ucLen : jsr .debug4 }
	tay			; ...to count bytes
	bne +
	dec ucLen+1		; special case for len being an exact multiple of 256
+
.auxWr2	sta setAuxWr		; self-modified earlier, based on isAuxCmd
.auxRd1	sta setAuxRd  		; self-modified based on isAuxCmd
.srcLoad:
	lda $1100,x		; self-modified earlier for offsetted source
.dstStore2:
	sta $1100,x		; self-modified earlier for dest buffer
	!if DO_COMP_CHECKSUMS {
	eor checksum
	sta checksum
	}
	inx			; inc to next src/dst byte
	bne +			; non-zero, skip page bump
	jsr .nextDstPage	; do the bump
+	dey			; count bytes -- first page yet?
	bne .srcLoad		; loop for more
	dec ucLen+1		; count pages
	bpl .srcLoad		; loop for more. NOTE: this would fail if we had blocks >= 32K
	sta clrAuxRd		; back to reading main mem, for mem mgr code
	sta clrAuxWr		; back to writing main mem
	inc ucLen+1		; to make it zero for the next match decode
+ 	ldy tmp			; restore index to source pointer
	jmp .getToken		; go on to the next token in the compressed stream
	; Subroutine called when length token = $F, to extend the length by additional bytes
.longLen:
-	sta ucLen		; save what we got so far
	+LOAD_YSRC		; get another byte
	cmp #$FF		; check for special there-is-more marker byte
	php			; save result of that
	clc
	adc ucLen		; add $FF to ucLen
	bcc +			; no carry, only lo byte has changed
	inc ucLen+1		; increment hi byte of ucLen
+	plp			; retrieve comparison status from earlier
	beq -			; if it was $FF, go back for more len bytes
	rts

	!if DO_COMP_CHECKSUMS {
.verifyCksum:
	+LOAD_YSRC
	!if DEBUG_DECOMP {
	+prStr : !text "cksum exp=",0
	pha
	jsr prbyte
	+prStr : !text " got=",0
	+prByte checksum
	+crout
	pla
	}
	cmp checksum		; get computed checksum
	beq +			; should be zero, because compressor stores checksum byte as part of stream
	+internalErr 'C'	; checksum doesn't match -- abort!
+	rts
	}

nextSrcPage:
	pha			; save byte that was loaded
	inc pSrc+1		; go to next page
	lda pSrc+1		; check the resulting page num
	cmp #>diskBufEnd	; did we reach end of buffer?
	bne +			; if not, we're done
	sta clrAuxWr		; buffer is in main mem
	txa
	pha
	jsr readToBuf		; read more pages
	pla
	tax
.auxWr3	sta setAuxWr		; go back to writing aux mem (self-modified for aux or main)
+	pla			; restore loaded byte
	rts

.nextDstPage:
	inc .srcLoad+2		; inc offset pointer for match copies
	inc .dstStore1+2	; inc pointers for dest stores
	inc .dstStore2+2
	dec .endChk2+1		; decrement total page counter
	rts
  
!if DEBUG_DECOMP {
.debug1	+prStr : !text "Decompressing: isComp=",0
	+prByte isCompressed
	+prStr : !text "isAux=",0
	+prByte isAuxCmd
	+prStr : !text "compLen=",0
	+prWord reqLen
	+prStr : !text "uncompLen=",0
	+prWord ucLen
	+crout
	rts
.debug2	+prStr : !text "Lit ptr=",0
	tya
	clc
	adc pSrc
	sta .dbgTmp
	lda pSrc+1
	adc #0
	sta .dbgTmp+1
	+prWord .dbgTmp
	+prStr : !text "len=",0
	+prWord ucLen
	+crout
	rts
.debug3	+prStr : !text "Match src=",0
	txa			; calculate src address with X (not Y!) as offset
	clc
	adc .srcLoad+1
	sta .dbgTmp
	lda .srcLoad+2
	adc #0
	sta .dbgTmp+1
	+prWord .dbgTmp
	+prStr : !text "dst=",0
	txa			; calculate dest address with X as offset
	clc
	adc .dstStore2+1
	sta tmp
	lda .dstStore2+2
	adc #0
	sta tmp+1
	+prWord tmp
	+prStr : !text "offset=",0
	lda tmp			; now calculate the difference
	sec
	sbc .dbgTmp
	sta .dbgTmp
	lda tmp+1
	sbc .dbgTmp+1
	sta .dbgTmp+1
	+prWord .dbgTmp		; and print it
	rts
.debug4	+prStr : !text "len=",0
	+prWord ucLen
	+crout
	rts
.dbgTmp	!word 0
}

;------------------------------------------------------------------------------
; Apply fixups to all modules that were loaded this round, and free the fixup
; resources from memory.
doAllFixups: !zone
	!if DEBUG >= 2 { +prStr : !text "Doing all fixups.",0 }
	; Now scan aux mem for fixup segments
	ldx #1			; start at first aux mem segment (0=main mem, 1=aux)
.loop:	lda tSegType,x		; grab flags & type
	and #$F			; just type now
	cmp #RES_TYPE_FIXUP
	beq .found
.next:	lda tSegLink,x		; next in chain
	tax			; to X reg index
	bne .loop		; non-zero = not end of chain - loop again
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

	!if DEBUG >= 2 { jsr .debug1 }

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
	!if DEBUG >= 2 { jsr .debug2 }
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
	!if DEBUG >= 2 { jsr .debug3 }
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
	!if DEBUG >= 2 { jsr .debug4 }
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
!if DEBUG >= 2 {
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
; Segment tables

!if DEBUG { !align 255,0 }

tSegLink	!fill MAX_SEGS
tSegType	!fill MAX_SEGS
tSegRes		!fill MAX_SEGS
tSegAdrLo	!fill MAX_SEGS
tSegAdrHi	!fill MAX_SEGS

;------------------------------------------------------------------------------
; Marker for end of the tables, so we can compute its length
tableEnd = *

} ; end of !pseudopc $D000
hiMemEnd = *