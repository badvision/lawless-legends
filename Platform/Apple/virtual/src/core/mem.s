;@com.wudsn.ide.asm.hardware=APPLE2
; Memory manager
; ------------------
;
; See detailed description in mem.i

 * = $800

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

; Memory buffers
fileBuf		= $4000	; len $400
diskBuf 	= $4400	; len $800
DISK_BUF_SIZE	= $800
diskBufEnd	= $4C00
headerBuf 	= $4C00	; len $1400

; Other equates
prodosMemMap 	= $BF58

;------------------------------------------------------------------------------
; Initial vectors - these have to start at $800
codeBegin:
	clc
	bcc locationCheck
	jmp main_dispatch
	jmp aux_dispatch
locationCheck:
	jsr monrts
	tsx
	lda $100,x
	cmp #>*
	bne +
	jmp init
+	sta pSrc+1
	lda #>*
	sta pDst+1
	ldy #0
	sty pSrc
	sty pDst
	ldx #>(tableEnd-codeBegin+$100)
-	lda (pSrc),y
	sta (pDst),y
	iny
	bne -
	inc pSrc+1
	inc pDst+1
	dex
	bne -
	jmp codeBegin

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
!source "../include/debug.i"

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
.fail:	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "No more segments", 0

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
; Input:  X(lo)/Y(hi) - address to scan for
; Output: X-reg - segment found (zero if not found), N and Z set for X-reg
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
	lda pTmp	; compare scan address...
	eor tSegAdrLo,x	; ... to seg start lo
	bne +		; if not equal, leave carry set
	lda pTmp+1	; hi byte
	eor tSegAdrHi,x	; to hy byte
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
	bmi .next	; skip active blocks
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
	cmp #REQUEST_MEMORY
	bne +
	jmp main_request
+	cmp #QUEUE_LOAD
	bne +
	jmp main_queueLoad
+	cmp #LOCK_MEMORY
	bne +
	jmp main_lock
+	cmp #UNLOCK_MEMORY
	bne +
	jmp main_unlock
+	cmp #FREE_MEMORY
	bne +
	jmp main_free
+	cmp #CALC_FREE
	bne shared_dispatch
	jmp main_calcFree
shared_dispatch:
	cmp #RESET_MEMORY
	bne +
	jmp reset
+	cmp #SET_MEM_TARGET
	bne +
	stx targetAddr
	sty targetAddr+1
	rts
+	cmp #FATAL_ERROR
	bne +
	jmp fatalError
+	jmp nextLdVec	; Pass command to next chained loader

;------------------------------------------------------------------------------
aux_dispatch: !zone
	cmp #REQUEST_MEMORY
	bne +
	jmp aux_request
+	cmp #QUEUE_LOAD
	bne +
	jmp aux_queueLoad
+	cmp #LOCK_MEMORY
	bne +
	jmp aux_lock
+	cmp #UNLOCK_MEMORY
	bne +
	jmp aux_unlock
+	cmp #FREE_MEMORY
	bne +
	jmp aux_free
+	cmp #CALC_FREE
	bne +
	jmp aux_calcFree
+	jmp shared_dispatch

;------------------------------------------------------------------------------
; Print fatal error message (custom or predefined) and print the
; call stack, then halt.
fatalError: !zone
	sty pTmp+1	; save message ptr hi...
	stx pTmp	; ...and lo
	jsr setnorm	; set up text mode and vectors
	bit setText
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
.msg1:	lda .prefix,y	; print out prefix message
	beq +
	jsr cout
	iny
	bne .msg1
+	tay		; start at first byte of user message
.msg2	lda (pTmp),y
	beq .msg3
	jsr cout
	iny
	bne .msg2
.msg3:
!if DEBUG {
; Print call stack
	ldy #0
.msg4	lda .stkMsg,y
	beq +
	jsr cout
	iny
	bne .msg4
+	tsx		; start at current stack pointer
.stackLoop:
	lda $101,x	; JSR increments PC twice before pushing it
	sec
	sbc #2
	tay
	lda $102,x
	sbc #0
	sta .load+2
	and #$F0	; avoid accidentally grabbing data from the IO area
	cmp #$C0
	beq .next
.load:	lda $1000,y	; is there a JSR there?
	cmp #$20
	bne .next	; no, it's probably not an actual call
	lda .load+2
	jsr prbyte
	tya
	jsr prbyte
	lda #' '
	jsr cout
.next:	inx	; work up to...
	cpx #$FF	; ...top of stack
	bcc .stackLoop
}
	jsr crout
	jsr bell	; beep
.inf: 	jmp .inf	; and loop forever
.prefix:!text "FATAL ERROR: ", 0
.stkMsg:!text $8D,"Call stk: ", 0

;------------------------------------------------------------------------------
init: !zone
; put something interesting on the screen :)
	jsr home
	+prStr : !text "Welcome to MythOS.",0
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
; 4: main $0xxx -> 5, inactive (xxx = end of mem mgr tables)
; 5: main $2000 -> 6, active + locked
; 6: main $6000 -> 3, inactive
; 7: main $BF00 -> 0, active + locked
; First, the flags
	lda #$C0		; flags for active + locked (with no resource)
	sta tSegType+0
	sta tSegType+1
	sta tSegType+3
	sta tSegType+5
	sta tSegType+7
; Next the links
	ldx #2
	stx tSegLink+1
	inx
	stx tSegLink+2
	ldx #4
	stx tSegLink+0
	inx
	stx tSegLink+4
	inx
	stx tSegLink+5
	inx
	stx tSegLink+6
; Then the addresses
	lda #2
	sta tSegAdrHi+2
	ldy #$C0
	sty tSegAdrHi+3
	dey
	sty tSegAdrHi+7
	lda #<tableEnd
	sta tSegAdrLo+4
	lda #>tableEnd
	sta tSegAdrHi+4
	lda #$40
	sta tSegAdrHi+5
	lda #$60
	sta tSegAdrHi+6
; Finally, form a long list of the remaining unused segments.
	ldx #8
	stx unusedSeg		; that's the first unused seg
	ldy #9
.loop:	tya
	sta tSegLink,x
	inx
	iny
	cpy #MAX_SEGS		; did all segments yet?
	bne .loop		; no, loop again
; Allocate space for the PLASMA frame stack
	ldx #0
	ldy #2			; 2 pages
	lda #REQUEST_MEMORY
	jsr mainLoader
	stx framePtr
	iny			; twice for 2 pages: initial pointer at top of new space
	iny
	sty framePtr+1
; Load PLASMA module #1
	ldx #0
	lda #START_LOAD
	jsr mainLoader
	ldx #RES_TYPE_MODULE
	ldy #1
	lda #QUEUE_LOAD
	jsr mainLoader
	stx .gomod+1
	sty .gomod+2
	lda #LOCK_MEMORY	; lock it in forever
	jsr mainLoader
	ldx #1			; keep open for efficiency's sake
	lda #FINISH_LOAD
	jsr mainLoader
	ldx #$10		; initial eval stack index
.gomod:	jmp $1111		; jump to module for further bootstrapping

;------------------------------------------------------------------------------
!if DEBUG {
printMem: !zone
	jsr printMain
	jmp printAux
printMain:
	+prStr : !text "Listing main mem segments.",0
	ldy #0
	jmp .printSegs
printAux:
	+prStr : !text "Listing aux mem segments.",0
	ldy #1
.printSegs:
	tya
	tax
	lda #'s'
	jsr .prChrEq
	lda #'t'
	ldx tSegType,y
	jsr .prChrEq
	lda #'n'
	ldx tSegRes,y
	jsr .prChrEq
	lda #'a'
	ldx tSegAdrHi,y
	jsr .prChrEq
	lda tSegAdrLo,y
	jsr prbyte
	jsr crout
.next:	lda tSegLink,y
	tay
	bne .printSegs
	rts
.prChrEq:
	pha
	lda #$A0
	jsr cout
	pla
	jsr cout
	lda #'='
	jsr cout
	txa
	jmp prbyte
} ; end zone

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
	lda #0			; default to putting fixups at $8000, to avoid fragmentation
	sta fixupHint
	lda #$80		
	sta fixupHint+1
	rts

;------------------------------------------------------------------------------
outOfMemErr: !zone
	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Out of mem", 0

;------------------------------------------------------------------------------
reservedErr: !zone
	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Mem reserved", 0

;------------------------------------------------------------------------------
main_request: !zone
	lda #0			; index for main mem
	beq shared_request	; always taken
aux_request:
	lda #1			; index for aux mem
shared_request:
	sta isAuxCmd		; save whether we're working on main or aux mem
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
	dec .reclaimFlg		; first time: 1 -> 0, second time 0 -> $FF
	bmi outOfMemErr		; so if it's second time, give up
	jsr reclaim		; first time, do a reclaim pass
	jmp .chooseAddr		; and try again
.notFound:
	jmp invalAddr
; target addr was specified. See if we can fulfill the request.
.gotTarget:
	ldx targetAddr		; all 16 bits
	jsr scanForAddr		; locate block containing target addr
	beq .notFound		; fail if we couldn't find it
	lda tSegType,x		; check flags
	bmi reservedErr		; if already active, can't re-allocate it
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
	cmp tSegAdrHi,y		; is end at exactly the right place?
	bne .splitEnd
	lda .reqEnd
	cmp tSegAdrLo,y		; compare all 16 bits
	beq .noSplitEnd
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
	lda isAuxCmd	; save whether current command is aux or not
	pha
	lda #1		; we do aux bank first
	sta isAuxCmd
.outer	ldx isAuxCmd	; grab correct starting segment (0=main mem, 1=aux)
.loop:	ldy tSegLink,x	; grab link to next segment, which we'll need regardless
	lda tSegType,x	; check flag and type of this seg
	bmi .next
	lda #0
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
	bmi .next		; if either is active or has a type, can't combine
	; we can combine the next segment into this one.
	!if DEBUG { jsr .debug }
	lda tSegLink,y
	sta tSegLink,x
	stx tmp
	jsr releaseSegment	; releases seg in Y
	ldy tmp			; check this segment again, in a tricky way
.next:	tya			; next in chain
	tax			; to X reg index
	bne .loop		; non-zero = not end of chain - loop again
.done	rts
!if DEBUG {
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
	sta isAuxCmd	; save whether main or aux mem
	jsr scanForAddr	; scan for block that matches
	beq invalAddr	; if not found, invalid
	bcs invalAddr	; if addr not exactly equal, invalid
	lda tSegType,x	; get existing flags
	bpl invalAddr	; must be an active block
	rts

invalAddr: !zone
	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Invalid addr", 0

;------------------------------------------------------------------------------
; If the resource is a module, this will locate the corresponding bytecode
; in aux mem. 
; Returns the segment found in X, or 0 if n/a. Sets Z flag appropriately.
shared_byteCodeAlso:
	lda resType
	cmp #RES_TYPE_MODULE
	beq +
	lda #0
	rts
+	lda #RES_TYPE_BYTECODE
	sta resType
	lda #1
	sta isAuxCmd
	jsr scanForResource
	bne +
	brk			; it better be present!
+	lda tSegType,x
	rts

;------------------------------------------------------------------------------
main_lock: !zone
	lda #0			; index for main-mem request
	beq shared_lock		; always taken
aux_lock:
	lda #1			; index for aux-mem request
shared_lock:
	jsr shared_scan		; scan for exact memory block
	ora #$40		; set the 'locked' flag
	sta tSegType,x		; store flags back
	jsr shared_byteCodeAlso
	beq +
	ora #$40
	sta tSegType,x
+	rts			; all done

;------------------------------------------------------------------------------
main_unlock: !zone
	lda #0			; index for main-mem request
	beq shared_unlock	; always taken
aux_unlock:
	lda #1			; index for aux-mem request
shared_unlock:
	jsr shared_scan		; scan for exact memory block
	and #$BF		; mask off the 'locked' flag
	sta tSegType,x		; store flags back
	jsr shared_byteCodeAlso
	beq +
	and #$BF
	sta tSegType,x
+	rts			; all done

;------------------------------------------------------------------------------
main_free: !zone
	lda #0			; index for main-mem request
	beq shared_free		; always taken
aux_free:
	lda #1			; index for aux-mem request
shared_free:
	jsr shared_scan		; scan for exact memory block
	and #$3F		; remove the 'active' and 'locked' flags
	sta tSegType,x		; store flags back
	and #$F			; get down to just the type, without the flags
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

;------------------------------------------------------------------------------
main_calcFree: !zone
; Input:  pTmp - address to scan for
; Output: X-reg - segment found (zero if not found), N and Z set for X-reg
;         carry clear if addr == seg start, set if addr != seg start
	ldx #0
	beq shared_calcFree
aux_calcFree:
	ldx #1
shared_calcFree:
	lda #0		; clear out free space counter
	sta reqLen
	sta reqLen+1
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
main_queueLoad: !zone
	lda #0			; flag for main mem
	beq shared_queueLoad 	; always taken
aux_queueLoad:
	lda #1			; flag for aux mem
shared_queueLoad:
	sta isAuxCmd 		; save whether main or aux
	stx resType		; save resource type
	sty resNum		; save resource number
	cpx #RES_TYPE_MODULE	; loading a module?
	beq .module		; extra work for modules
.notMod	jsr scanForResource	; scan to see if we already have this resource in mem
	beq .notFound		; nope, pass to next loader
	stx segNum		; save seg num for later
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
	cpy targetAddr		; verify addr hi
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
	lda #RES_TYPE_MODULE
	sta resType
	lda #0
	sta isAuxCmd
	jsr scanForResource	; do we have the main mem part?
	beq .reload	
	rts			; we have both parts already -- no need for fixups
.reload	lda #RES_TYPE_MODULE
	sta resType
	lda #0
	sta isAuxCmd
	jsr .notMod		; queue the main memory part of the module
	stx .modRet+1		; save address of main load for eventual return
	sty .modRet+3		; yes, self-modifying
	ldx #RES_TYPE_BYTECODE
	ldy resNum
	jsr aux_queueLoad	; load the aux mem part (the bytecode)
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
.frag	ldx #RES_TYPE_FIXUP	; queue loading of the fixup resource
	ldy resNum
	jsr aux_queueLoad
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
+	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Invalid command", 0

;------------------------------------------------------------------------------
openPartition: !zone
	!if DEBUG { +prStr : !text "Opening part file.",0 }
; complete the partition file name
	lda curPartition
	clc
	adc #'0'		; assume partition numbers range from 0..9 for now
	sta .partNumChar
; open the file
	jsr mli
	!byte MLI_OPEN
	!word .openParams
	bcs prodosError
; grab the file number, since we're going to keep it open
	lda .openFileRef
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

.openParams:	!byte 3		; number of params
		!word .filename	; pointer to file name
		!word fileBuf	; pointer to buffer
.openFileRef:	!byte 0		; returned file number
.filename:	!byte 15	; length
		!raw "/LL/GAME.PART."	; TODO: Figure out how to avoid specifying full path. "raw" for ProDOS
		; If I leave it out, ProDOS complains with error $40.
.partNumChar:	!raw "x"	; "x" replaced by partition number

readParams:	!byte 4	; number of params
readFileRef:	!byte 0
readAddr:	!word 0
readLen:	!word 0
readGot:	!word 0

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
	ldx #<.msg
	ldy #>.msg
	jmp fatalError
.digit:	and #$F
	ora #$B0
	cmp #$BA
	bcc +
	adc #6
+	rts
.msg:	!text "ProDOS error $"
.num:	!text "xx"
	!byte 0

;------------------------------------------------------------------------------
disk_startLoad: !zone
; Make sure we don't get start without finish
	lda curPartition
	bne sequenceError
; Just record the partition number; it's possible we won't actually be asked
; to queue anything, so we should put off opening the file.
	stx curPartition
	rts

;------------------------------------------------------------------------------
sequenceError: !zone
	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Bad sequence", 0

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
	ldx #<+
	ldy #>+
	jmp fatalError
+	!text "Resource not found", 0
.resLen: !byte 0

;------------------------------------------------------------------------------
disk_finishLoad: !zone
	stx .keepOpenChk+1	; store flag telling us whether to keep open (1) or close (0)
	lda partFileRef		; see if we actually queued anything (and opened the file)
	bne +			; non-zero means we have work to do
	rts			; nothing to do - return immediately
+	sta .setMarkFileRef	; copy the file ref number to our MLI param blocks
	sta readFileRef
	lda headerBuf		; grab # header bytes
	sta .setMarkPos		; set to start reading at first non-header byte in file
	lda headerBuf+1		; hi byte too
	sta .setMarkPos+1
	lda #0
	sta .setMarkPos+2
	sta .nFixups
	jsr setupDecomp		; one-time init for decompression code
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
	jsr mli			; move the file pointer to the current block
	!byte MLI_SET_MARK
	!word .setMarkParams
	bcs .prodosErr
	jsr lz4Decompress	; decompress (or copy if uncompressed)
.resume	ldy .ysave
.next	lda (pTmp),y		; lo byte of length
	clc
	adc .setMarkPos		; advance mark position exactly that far
	sta .setMarkPos
	iny
	lda (pTmp),y		; hi byte of length
	bpl +			; if hi bit is clear, resource is uncompressed
	iny			; skip compressed size
	iny
	and #$7F		; mask off the flag
+	adc .setMarkPos+1	; bump the high byte of the file mark pos
	sta .setMarkPos+1
	bcc +
	inc .setMarkPos+2	; account for partitions > 64K
+	iny			; increment to next entry
	bpl +			; if Y index is is small, no need to adjust
	jsr adjYpTmp		; adjust pTmp and Y to make it small again
+	jmp .scan		; back for more
.prodosErr:
	jmp prodosError
.addrErr:
	jmp invalAddr

.setMarkParams:	!byte 2		; param count
.setMarkFileRef:!byte 0		; file reference
.setMarkPos:	!byte 0		; mark position (3 byte integer)
		!byte 0
		!byte 0
.ysave:		!byte 0
.nFixups:	!byte 0

!if DEBUG {
.debug1:+prStr : !text "Loading: t=",0
	+prByte resType
	+prStr : !text "n=",0
	+prByte resNum
	+prStr : !text "aux=",0
	+prByte isAuxCmd
	rts
.debug2:+prStr : !text "len=",0
	+prWord reqLen
	+prStr : !text "dst=",0
	+prWord pDst : +crout
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
	sta .closeFileRef
	jsr mli			; now that we're done loading, we can close the partition file
	!byte MLI_CLOSE
	!word .closeParams
	bcs .prodosErr
	rts
.prodosErr:
	jmp prodosError
.closeParams:
	!byte 1			; param count
.closeFileRef:
	!byte 0			; file ref to close

;------------------------------------------------------------------------------
readToMain: !zone
	jsr mli
	!byte MLI_READ
	!word readParams
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
	stx .auxWr4+1
	stx .auxWr2+1
	sty .auxRd1+1		; and the read switches too
	sty .auxRd2+1
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
	lda #0              	; have we finished all pages?
	bne .decodeMatch	; no, keep going
	bit isCompressed
	bpl +			; if not compressed, no extra work at end
	pla			; toss unused match length
	!if DO_COMP_CHECKSUMS { jsr .verifyCksum }
+	rts			; all done!
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
.auxWr2	sta setAuxWr		; self-modified earlier, based on isAuxCmd
	; Subroutine does the work. Runs in stack area so it can write *and* read aux mem
	jsr .matchCopy		; copy match bytes (aux->aux, or main->main)
	sta clrAuxWr		; back to writing main mem
	inc ucLen+1		; to make it zero for the next match decode
+ 	ldy tmp			; restore index to source pointer
	jmp .getToken		; go on to the next token in the compressed stream
	; Subroutine to copy bytes, either main->main or aux->aux. We put it down in the
	; stack space ($100) so it can access either area. The stack doesn't get bank-switched
	; by setAuxRd/clrAuxRd.
.matchShadow_beg = *
!pseudopc $100 {
.matchCopy:
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
	sta clrAuxRd		; page bump needs to operate in main mem
	jsr .nextDstPage	; do the bump
.auxRd2	sta setAuxRd		; and back to aux mem (if isAuxCmd)
+	dey			; count bytes -- first page yet?
	bne .srcLoad		; loop for more
	dec ucLen+1		; count pages
	bpl .srcLoad		; loop for more. NOTE: this would fail if we had blocks >= 32K
	sta clrAuxRd		; back to reading main mem, for mem mgr code
+	rts			; done copying bytes
}
.matchShadow_end = *
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
	brk			; checksum doesn't match -- abort!
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
	sta clrAuxWr		; write to main mem so we can increment stuff in code blocks
	inc .srcLoad+2		; inc offset pointer for match copies
	inc .dstStore1+2	; inc pointers for dest stores
	inc .dstStore2+2
	dec .endChk2+1		; decrement total page counter
.auxWr4	sta setAuxWr		; go back to writing aux mem (self-modified for aux or main)
	rts
  
; Copy the match shadow down to the stack area so it can copy from aux to aux.
; This needs to be called once before any decompression is done. We shouldn't
; rely on it being preserved across calls to the memory manager.
setupDecomp:
	ldx #.matchShadow_end - .matchShadow_beg - 1
-	lda .matchShadow_beg,x	; get the copy from main RAM
	sta .matchCopy,x	; and put it down in stack space where it can access both main and aux
	dex			; next byte
	bpl -			; loop until we grab them all (including byte 0)
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
	!if DEBUG { +prStr : !text "Doing all fixups.",0 }
	; copy the shadow code down to $100, so we can read aux mem bytes
	ldx #.fixupShadow_end - .fixupShadow - 1
-	lda .fixupShadow,x
	sta .getFixupByte,x
	dex
	bpl -
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
	brk
+	lda tSegAdrLo,x		; get the segment's address
	sta .mainBase		; and save it
	lda tSegAdrHi,x		; hi byte too
	sta .mainBase+1

	; Find the corresponding aux-mem seg
	lda #RES_TYPE_BYTECODE	; it's of type bytecode
	sta resType
	inc isAuxCmd		; it'll be in aux mem
	jsr scanForResource
	bne +			; we better find it
	brk
+	lda tSegAdrLo,x
	sta .auxBase
	lda tSegAdrHi,x
	sta .auxBase+1

	!if DEBUG { jsr .debug1 }

	; Process the fixups
.proc	jsr .fetchFixup		; get key byte
	tax			; save it aside, and also check the hi bit
	bmi .fxAux		; yes, it's aux mem fixup
.fxMain	jsr .fetchFixup		; get the lo byte of the offset
	clc
	adc .mainBase
	sta pDst
	txa
	adc .mainBase+1
	sta pDst+1
	!if DEBUG { jsr .debug2 }
	clc
	jsr .adMain
	iny
	jsr .adMain
	bne .proc		; always taken
.adMain	lda (pDst),y
	adc .mainBase,y
	sta (pDst),y
	rts
.fxAux	cmp #$FF		; end of fixups?
	beq .stubs		; if so, go do the stubs
	jsr .fetchFixup		; get lo byte of offset
	clc
	adc .auxBase
	sta pDst
	txa
	and #$7F		; mask off the hi bit flag
	adc .auxBase+1
	sta pDst+1
	!if DEBUG { jsr .debug3 }
	sta setAuxWr
	jsr .adAux
	iny
	jsr .adAux
	sta clrAuxWr
	bne .proc		; always taken
.adAux	jsr .getBytecode
	adc .mainBase,y
	sta (pDst),y
	rts
.stubs	; fix up the stubs
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
	!if DEBUG { jsr .debug4 }
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
	jsr .getFixupByte	; get a byte from aux mem
	inc pSrc		; and advance the pointer
	bne +
	inc pSrc+1		; hi byte too, if necessary
+	rts

.fixupShadow: 
!pseudopc $100 {
.getFixupByte:
	sta setAuxRd
	lda (pSrc),y
	sta clrAuxRd
	rts
.getBytecode:
	sta setAuxRd
	lda (pDst),y
	sta clrAuxRd
	rts
}
.fixupShadow_end = *
!if DEBUG {
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

;------------------------------------------------------------------------------
; Segment tables

!if DEBUG { !align 255,0 }

tSegLink	= * : !fill MAX_SEGS
tSegType	= * : !fill MAX_SEGS
tSegRes		= * : !fill MAX_SEGS
tSegAdrLo	= * : !fill MAX_SEGS
tSegAdrHi	= * : !fill MAX_SEGS

;------------------------------------------------------------------------------
; Marker for end of the tables, so we can compute its length
tableEnd = *
