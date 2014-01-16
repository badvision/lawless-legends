; Memory manager
; ------------------
;
; Memory is managed in variable-sized segments. In each 48kb memory bank (main and aux), 
; a linked list identifies each segment there as well as usage flags to mark free, used, 
; or reserved memory.
;
; Memory is marked as used as it is loaded by the loader, but the caller program
; should free it as soon as the memory is no longer in use.  It is very possible that 
; the memory will not be reclaimed right away and could be reinstated as in-use without 
; a loading penalty.
;
; Another scenario is that free memory will be relocated to auxiliary banks
; and potentially later restored to active memory at a later time.  Depending on the
; loader, this might be handled in different ways.  Aux memory should be kept open
; such that memory can still be reclaimed.  Extended memory (e.g. RamWorks and slinky
; ram expansions) can adopt a more linear and predictable model if they are large enough.
;
; Loading operations are performed in sets. A set is begun with the START_LOAD call, and
; subsequent QUEUE_LOAD requests are queued up. The set is then executed with 
; FINISH_LOAD. The purpose of queuing the requests is to allow the disk loader to
; sort the requests in storage order and thus optimize loading speed. During the period 
; between START_LOAD and FINISH_LOAD, the area in main memory from $4000 to $5FFF is 
; reserved for loader operations. Therefore, if hi-res graphics on page 2 are being
; displayed, it is important to copy them to page 1 ($2000.3FFF) and switch to that 
; page before queueing any loads.
;
; Additional loaders may be inserted between main/aux and the disk loader, to implement
; caching schemes in extended ram. The goal of using extended ram is to prevent future 
; disk access as much as possible, because even an inefficient o(N) memory search is 
; going to be faster than spinning up a disk.
;
; ----------------------------
; Segment table format in memory:
; Linked list of segments. Segments are generally indexed by the X register. There is
; one list of main memory, another list for aux mem. They are intermixed in the segment
; table space. First segment of main mem is always seg 0; first of aux mem is seg 1.
;
; tSegType,x:
;   FF00tttt nnnnnnnn llllllll aaaaaaaa bbbbbbbb
;   F = Flags
;     7 - Active/Inactive
;     6 - Locked (cannot be reclaimed for any reason)
;   t = Type of resource (1-15, 0 is invalid)
; tSegResNum,x:
;   resource number (1-255, 0 is invalid or no resource loaded)
; tSegAdrLo,x and tSegAdrHi,x:
;   address of segment in memory
; tSegLink,x:
;   link to next segment
;
; Essentially there are three distinct lists. Main mem (starts at seg 0), aux mem
; (starts at seg 1), and the unused list (starts at segment stored in unusedSeg).
;
; There is an extra link associated with each of (main, aux) mem. It's indexed by the 
; Y register (y=0 is main, y=1 is aux):
; 
; scanStart,y:
;   segment of last successful allocation. Used to quickly pick up for next
;   allocation.
;
; -------------------------
; Page file format on disk:
;   File must be named: GAME.PART.nnn where nnn is the partition number (0-15)
;   File Header:
;     bytes 0-1: Total # of bytes in header (lo, hi)
;     bytes 2-n: Table of repeated resource entries
;   Resource entry:
;     byte  0:   resource type (1-15), 0 for end of table
;     byte  1:   resource number (1-255)
;     bytes 2-3: number of bytes in resource (lo, hi)
;   The remainder of the file is the data for the resources, in order of their
;   table appearance.
;
startMemMgr = $800
mainLoader  = $803
auxLoader   = $806

;------------------------------------------------------------------------------
; Command codes

;------------------------------------------------------------------------------
RESET_MEMORY = $10
    ; Input: None
    ;
    ; Output: None
    ;
    ; Mark all memory as inactive, except the following areas in main memory 
    ; which are always locked:
    ;
    ; 0000.01FF: Zero page and stack
    ; 0200.02FF: Input buffer and/or scratch space
    ; 0300.03FF: System vectors, scratch space
    ; 0400.07FF: Text display
    ; 0800.0xFF: The memory manager and its page table
    ; 2000.5FFF: Hi-res graphics display buffers
    ; BF00.BFFF: ProDOS system page
    ;
    ; Note that this does *not* destroy the contents of memory, so for instance
    ; future RECALL_MEMORY commands may be able to re-use the existing contents
    ; of memory if they haven't been reallocated to something else.
    ;
    ; This command is acted upon and then passed on to chained loaders.

;------------------------------------------------------------------------------
REQUEST_MEMORY = $11
    ; Input:  X-reg(lo) / Y-reg(hi) - number of bytes to allocate
    ;
    ; Output: X-reg(lo) / Y-reg(hi) - address allocated
    ;
    ; Allocate a segment in the memory space of this loader. If there 
    ; isn't a large enough continguous memory segment available, the system 
    ; will be halted immediately with HALT_MEMORY.
    ;
    ; Normally this command chooses the location of the memory area; if you
    ; want to force it to use a particular location, use SET_MEM_TARGET first.
    ;
    ; To allocate main memory, call the main memory loader. To allocate aux
    ; mem, call that loader instead.
    ;
    ; This command is acted upon immediately and chained loaders are not called.

;------------------------------------------------------------------------------
LOCK_MEMORY = $12
    ; Input:  X-reg(lo) / Y-reg(hi) - address of segment to lock
    ;
    ; Output: None
    ;
    ; Locks a previously requested or loaded segment of memory so that it
    ; cannot be reclaimed by RESET_MEMORY.
    ;
    ; This command is acted upon immediately and chained loaders are not called.

;------------------------------------------------------------------------------
UNLOCK_MEMORY = $13
    ; Input: X-reg(lo) / Y-reg(hi) - address of segment to unlock (must be start 
    ;    of a memory area that was previously locked)
    ;
    ; Output: None
    ;
    ; Mark a segment of memory as unlocked, so it can be reclaimed by
    ; RESET_MEMORY.
		
;------------------------------------------------------------------------------
SET_MEM_TARGET = $14
    ; Input:  X-reg(lo) / Y-reg(hi) - address to target
    ;
    ; Output: None
    ;
    ; Sets the target address in memory for the next REQUEST_MEMORY or QUEUE_LOAD 
    ; command. This will force allocation at a specific location instead 
    ; allowing the loader to choose.
    ;
    ; This is a one-shot command, i.e. as soon as an allocation is performed,
    ; subsequent allocations will revert to their normal behavior.
		
;------------------------------------------------------------------------------
START_LOAD = $15
    ; Input:  X-reg - disk partition number (0 for boot disk, 1-15 for others)
    ;
    ; Output: None
    ;
    ; Marks the start of a set of QUEUE_LOAD operations, that will be
    ; acted upon when FINISH_LOAD is finally called.
    ;
    ; The partition is recorded and passed on to chained loaders.

;------------------------------------------------------------------------------
QUEUE_LOAD = $16
    ; Input: X-reg - resource type
    ;        Y-reg - resource number
    ;
    ; Output: X-reg(lo) / Y-reg (hi) - address the load will occur at
    ;
    ; This is the main entry for loading resources from disk. It queues a load
    ; request, allocating memory to hold the entire resource. Note that
    ; the load is only queued; it may not be completed until FINISH_LOAD.
    ;
    ; Normally this command chooses the location of the memory area; if you
    ; want to force it to use a particular location, use SET_MEM_TARGET first.
    ;
    ; Note that if the data is already in memory (in active or inactive state), 
    ; it will be activated if necessary and its former location will be 
    ; returned and no disk access will be queued.
    ;
    ; The request is either acted upon by this loader, or passed onto the
    ; next chained loader. If there is no next loader, a FATAL_ERROR is 
    ; triggered.

;------------------------------------------------------------------------------
FINISH_LOAD = $17
    ; Input: None
    ;
    ; Output: None
    ;
    ; Completes all prior QUEUE_LOAD requests, clearing the queue. It's the
    ; last part of a START_LOAD / QUEUE_LOAD / FINISH_LOAD sequence.
    ;
    ; This command is acted upon by this loader and passed to chained loaders.

;------------------------------------------------------------------------------
FREE_MEMORY = $18
    ; Input: X-reg(lo) / Y-reg(hi) - address of segment to mark as free (must 
    ;     be start of a memory area that was previously requested or loaded)
    ;
    ; Output: None
    ;
    ; Mark a segment of memory as free, or rather inactive, so that it can be 
    ; reused. This also clears the lock bit!
		
;------------------------------------------------------------------------------
CHAIN_LOADER = $1E
    ; Input: X-reg / Y-reg - pointer to loader (X=lo, Y=hi) to add to chain
    ;
    ; Output: None
    ;
    ; Add a loader to the chain just after this loader. The current next
    ; loader (if there is one) will be passed to the new loader with another
    ; CHAIN_LOADER command.
    ;
    ; The purpose of a loader chain is to insert faster devices between the
    ; main/aux loader (fastest) and the disk loader (slowest). Note that the 
    ; main mem and aux mem loaders are conceptually one; a chained loader will
    ; always be inserted after them, not between them.
		
;------------------------------------------------------------------------------
FATAL_ERROR = $1F
    ; Input:  X-reg(lo) / Y-reg(hi): message pointer
    ;
    ; Output: Never returns
    ;
    ; Switches to text mode, prints out the zero-terminated ASCII error message 
    ; pointed to by the parameters, plus the call stack, and then halts the 
    ; system (i.e. it waits forever, user has to press Reset).
    ;
    ; This command halts and thus never returns.

;------------------------------------------------------------------------------
; code begins here
  .org $800

  .include "../include/global.i"

  ; constants
  MAX_SEGS = 96

  ; zero page
  reqLen        = $6    ; length 2
  resType       = $8    ; length 1
  resNum        = $9    ; length 1
  isAuxCommand  = $A    ; length 1

  ; memory buffers
  fileBuffer = $4000    ; len $400
  auxBuffer  = $4400    ; len $800
  headerBuf  = $4C00    ; len $1400

  ; Initial vectors
  jmp init
  jmp main_dispatch
  jmp aux_dispatch

DEBUG = 1
  .include "../include/debug.i"

;------------------------------------------------------------------------------
; Variables
targetAddr:
  .word 0
unusedSeg:
  .byte 0
scanStart:
  .byte 0, 1            ; main, aux
segNum:
  .byte 0
nextLoaderVec: 
  jmp diskLoader
curPartition:
  .byte 0
partitionFileRef:
  .byte 0

;------------------------------------------------------------------------------
grabSegment:
  ; Output: Y-reg = segment grabbed
  ; Note: Does not disturb X reg
  ldy unusedSeg         ; first unused segment
  beq @noMore           ; ran out?
  lda tSegLink,y        ; no, grab next segment in list
  sta unusedSeg         ; that is now first unused
  rts                   ; return with Y = the segment grabbed
@noMore:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "No more segments", 0

;------------------------------------------------------------------------------
releaseSegment:
  ; Input: X-reg = segment to release
  lda unusedSeg         ; previous head of list
  sta tSegLink,x        ; point this segment at it
  stx unusedSeg         ; this segment is now head of list
  txa                   ; seg num to accumulator
  ldy isAuxCommand
  cmp scanStart,y       ; was this seg the scan start?
  bne :+                ; no, things are fine
  tya                   ; yes, need to reset the scan start
  sta scanStart,y       ; scan at first mem block for (main or aux)
: rts

;------------------------------------------------------------------------------
scanForAddr:
  ; Input:  pTmp - address to scan for
  ; Output: X-reg - segment found (zero if not found), N and Z set for X-reg
  ;         carry clear if addr == seg start, set if addr != seg start
  ldx isAuxCommand      ; grab correct starting segment (0=main mem, 1=aux)
@loop:
  ldy tSegLink,x        ; grab link to next segment, which we'll need regardless
  lda pTmp              ; compare pTmp
  cmp tSegAdrLo,x       ; to this seg addr
  lda pTmp+1            ; including...
  sbc tSegAdrHi,x       ; ...hi byte
  bcc @next             ; if pTmp < seg addr then keep searching
  lda pTmp              ; compare pTmp
  cmp tSegAdrLo,y       ; to *next* seg addr
  lda pTmp+1            ; including...
  sbc tSegAdrHi,y       ; ...hi byte
  bcc @found            ; if pTmp < next seg addr then perfect!
@next:
  tya                   ; next in chain
  tax                   ; to X reg index
  bne @loop             ; non-zero = not end of chain - loop again
  rts                   ; fail with X=0
@found:
  sec                   ; start out assuming addr != seg start
  lda pTmp              ; compare scan address...
  eor tSegAdrLo,x       ; ... to seg start lo
  bne :+                ; if not equal, leave carry set
  lda pTmp+1            ; hi byte
  eor tSegAdrHi,x       ; to hy byte
  bne :+                ; again, if not equal, leave carry set
  clc                   ; addr is equal, clear carry
: txa
  rts                   ; all done

;------------------------------------------------------------------------------
scanForResource:
  ; Input:  resType, resNum: resource type and number to scan for
  ; Output: X-reg - segment found (zero if not found). N and Z set based on X reg.
  ldy isAuxCommand      ; grab correct starting segment
  ldx scanStart,y       ; start scanning at last scan point
  stx @next+1           ; it also marks the ending point. Yes, self-modifying code.
@loop:
  ldy tSegLink,x        ; grab link to next segment, which we'll need regardless
  lda tSegResNum,x      ; check number
  cmp resNum            ; same resource number?
  bne @next             ; no, check next seg
  lda tSegType,x        ; check get flag + type byte
  and #$F               ; mask off flags to get just the type
  cmp resType           ; same type?
  bne @next             ; no, check next seg
  ldy isAuxCommand      ; index for setting next scan start
  txa                   ; set N and Z flags for return
  sta scanStart,y       ; set this seg as next scanning start
  rts                   ; all done!
@next:
  cpy #11               ; did we loop around to starting point? (filled in at beg)
  beq @fail             ; if so, failed to find what we wanted
  tya                   ; next in chain
  tax                   ; to X reg index
  bne @loop             ; not end of chain - loop again
  ldy isAuxCommand      ; end of chain; start at first seg (0=main, 1=aux)
  jmp @next             ; keep going until we succeed or reach starting point
@fail:
  ldx #0                ; failure return
  rts

;------------------------------------------------------------------------------
scanForAvail:
  ; Input:  reqLen - 16-bit length to scan for
  ; Output: X-reg - segment found (zero if not found)
  ldy isAuxCommand      ; grab correct starting segment
  ldx scanStart,y       ; start scanning at last scan point
  stx @next+1           ; it also marks the ending point. Yes, self-modifying code.
@loop:
  ldy tSegLink,x        ; grab link to next segment, which we'll need regardless
  lda tSegType,x        ; check flags
  bmi @next             ; skip active blocks
  lda tSegAdrLo,x       ; calc this seg addr plus len
  clc
  adc reqLen
  sta @cmp1+1           ; save for later comparison (self-modifying)
  lda tSegAdrHi,x       ; all 16 bits
  adc reqLen+1
  sta @cmp2+1           ; again save for later (self-modifying)
  lda tSegAdrLo,y       ; compare *next* seg start addr
@cmp1:
  cmp #11               ; self-modified earlier
  lda tSegAdrHi,y       ; all 16 bits
@cmp2:
  sbc #11               ; self-modified earlier
  bcc @next             ; next seg addr < (this seg addr + len)? no good - keep looking
  ldy isAuxCommand      ; index for setting next scan start
  txa                   ; set N and Z flags for return
  sta scanStart,y       ; set this seg as next scanning start
  rts                   ; all done!
@next:
  cpy #11               ; did we loop around to starting point? (filled in at beg)
  beq @fail             ; if so, failed to find what we wanted
  tya                   ; next in chain
  tax                   ; to X reg index
  bne @loop             ; not end of chain - loop again
  ldx isAuxCommand      ; end of chain; start at first seg (0=main, 1=aux)
  jmp @next             ; keep going until we succeed or reach starting point
@fail:
  ldx #0                ; failure return
  rts

;------------------------------------------------------------------------------
main_dispatch:
  cmp #REQUEST_MEMORY
  bne :+
  jmp main_request
: cmp #QUEUE_LOAD
  bne :+
  jmp main_queueLoad
: cmp #LOCK_MEMORY
  bne :+
  jmp main_lock
: cmp #UNLOCK_MEMORY
  bne :+
  jmp main_unlock
: cmp #FREE_MEMORY
  bne shared_dispatch
  jmp main_free
shared_dispatch:
  cmp #RESET_MEMORY
  bne :+
  jmp reset
: cmp #SET_MEM_TARGET
  bne :+
  stx targetAddr
  sty targetAddr+1
  rts
: cmp #FATAL_ERROR
  bne :+
  jmp fatalError
; Pass command to next chained loader
: jmp nextLoaderVec

;------------------------------------------------------------------------------
aux_dispatch:
  cmp #REQUEST_MEMORY
  bne :+
  jmp aux_request
: cmp #QUEUE_LOAD
  bne :+
  jmp aux_queueLoad
: cmp #LOCK_MEMORY
  bne :+
  jmp aux_lock
: cmp #UNLOCK_MEMORY
  bne :+
  jmp aux_unlock
: cmp #FREE_MEMORY
  bne :+
  jmp aux_free
: jmp shared_dispatch

;------------------------------------------------------------------------------
orCout:
  ora #$80
  jmp cout

;------------------------------------------------------------------------------
; Print fatal error message (custom or predefined) and print the
; call stack, then halt.
fatalError:
  sty pTmp+1    ; save message ptr hi...
  stx pTmp      ; ...and lo
  jsr setnorm   ; set up text mode and vectors
  bit setText
  jsr setvid
  jsr setkbd
  lda $24
  beq :+
  jsr crout
: ldy #40
  lda #'-'
: jsr orCout
  dey
  bne :-
: lda fatalMsg,y  ; print out prefix message
  beq :+
  jsr orCout
  iny
  bne :-
: ldy #0        ; start at first byte of message
: lda (pTmp),y
  beq :+
  jsr orCout
  iny
  bne :-
  .if DEBUG
; Print call stack
: ldy #0
: lda stackMsg,y
  beq :+
  jsr orCout
  iny
  bne :-
: tsx           ; start at current stack pointer
@stackLoop:
  lda $101,x     ; JSR increments PC twice before pushing it
  sec
  sbc #2
  tay
  lda $102,x
  sbc #0
  sta @load+2
  and #$F0      ; avoid accidentally grabbing data from the IO area
  cmp #$C0
  beq @next
@load:
  lda $1000,y   ; is there a JSR there?
  cmp #$20
  bne @next     ; no, it's probably not an actual call
  lda @load+2
  jsr prbyte
  tya
  jsr prbyte
  lda #' '
  jsr orCout
@next:
  inx           ; work up to...
  cpx #$FF      ; ...top of stack
  bcc @stackLoop
  .endif
  jsr crout
; Beep, and loop forever
  jsr bell
loopForever: 
  jmp loopForever
fatalMsg:
  .byte "FATAL ERROR: ", 0
stackMsg:
  .byte $8D,"Call stk: ", 0

;------------------------------------------------------------------------------
init:
  ; clear the segment tables
  ldy #0
  tya
: sta tSegLink,y
  sta tSegAdrLo,y
  sta tSegAdrHi,y
  sta tSegType,y
  sta tSegResNum,y
  iny
  cpy #MAX_SEGS
  bne :-
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
  lda #$C0              ; flags for active + locked (with no resource)
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
  lda #$20
  sta tSegAdrHi+5
  lda #$60
  sta tSegAdrHi+6
  ; Finally, form a long list of the remaining unused segments.
  ldx #8
  stx unusedSeg         ; that's the first unused seg
  ldy #9
@loop:
  tya
  sta tSegLink,x
  inx
  iny
  cpy #MAX_SEGS         ; did all segments yet?
  bne @loop             ; no, loop again
  .if DEBUG
  jmp test
  .else
  rts
  .endif

;------------------------------------------------------------------------------
  .if DEBUG
printMem:
  DEBUG_STR "Listing main mem segments."
  ldy #0
  jsr @printSegs
  DEBUG_STR "Listing aux mem segments."
  ldy #1
@printSegs:
  tya
  tax
  lda #'s'
  jsr @prChrEq
  lda #'t'
  ldx tSegType,y
  jsr @prChrEq
  lda #'n'
  ldx tSegResNum,y
  jsr @prChrEq
  lda #'a'
  ldx tSegAdrHi,y
  jsr @prChrEq
  lda tSegAdrLo,y
  jsr prbyte
  jsr crout
@next:
  lda tSegLink,y
  tay
  bne @printSegs
  rts
@prChrEq:
  pha
  lda #$A0
  jsr cout
  pla
  jsr orCout
  lda #'='
  jsr orCout
  txa
  jmp prbyte
  .endif

;------------------------------------------------------------------------------
  .if DEBUG
test:
  DEBUG_STR "Start load."
  ldx #0                ; partition 0
  lda #START_LOAD
  jsr mainLoader
  DEBUG_STR "Set mem target."
  ldx #0
  ldy #$70              ; load at $7000
  lda #SET_MEM_TARGET
  jsr mainLoader
  DEBUG_STR "Queue load."
  ldx #1                ; TYPE_CODE = 1
  ldy #1                ; code resource #1
  lda #QUEUE_LOAD
  jsr mainLoader
  DEBUG_STR "Finish load."
  lda #FINISH_LOAD
  jsr mainLoader
  DEBUG_STR "Test complete."
  jmp monitor

  ; obsolete test
  DEBUG_STR "Testing memory manager."
@loop:
  ldx #1
  ldy #4
  lda #REQUEST_MEMORY
  jsr mainLoader
  stx tmp
  sty tmp+1
  DEBUG_STR "Main alloc: "
  DEBUG_WORD tmp
  DEBUG_LN
  ldx #2
  ldy #4
  lda #REQUEST_MEMORY
  jsr auxLoader
  stx tmp
  sty tmp+1
  DEBUG_STR "Aux alloc: "
  DEBUG_WORD tmp
  DEBUG_LN
  jmp @loop
  rts

  .endif

;------------------------------------------------------------------------------
reset:
  ; Set all non-locked pages to inactive.
  ldx #0                ; main mem first
  jsr @inactivate       ; inactivate its non-locked segments
  ldx #1                ; then aux mem
  jsr @inactivate       ; same thing
  lda #RESET_MEMORY     ; get command code back
  jmp nextLoaderVec     ; and allow chained loaders to reset also
@inactivate:
  lda tSegType,x        ; get flag byte for this segment
  tax
  and #$40              ; segment locked?
  bne @next             ; yes, skip it
  txa                   ; no, get back flags
  and #$7F              ; mask off the 'active' bit
  sta tSegType,x        ; save it back
@next:
  lda tSegLink,x        ; get link to next seg
  tax                   ; to X reg, and test if end of chain (x=0)
  bne @inactivate       ; no, not end of chain, so loop again
  rts                   ; yes, end of chain: done

;------------------------------------------------------------------------------
outOfMemErr:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Out of mem", 0

;------------------------------------------------------------------------------
reservedErr:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Mem reserved", 0

;------------------------------------------------------------------------------
main_request:
  lda #0                ; index for main mem
  beq shared_request    ; always taken
aux_request:
  lda #1                ; index for aux mem
shared_request:
  sta isAuxCommand      ; save whether we're working on main or aux mem
  stx reqLen            ; save requested length
  sty reqLen+1          ; all 16 bits
shared_alloc:
  lda #1
  sta @reclaimFlg       ; we will try to reclaim once
@try:
  lda targetAddr+1      ; see if SET_MEM_TARGET was called
  bne @gotTarget        ; no, we need to choose location
@chooseAddr:
  ; no target address has been specified, we need to choose one
  jsr scanForAvail      ; scan for an available block
  bne @noSplitStart     ; if found, go into normal split checking
  ; failed to find a block. If we haven't tried reclaiming, do so now
  dec @reclaimFlg       ; first time: 1 -> 0, second time 0 -> $FF
  bmi outOfMemErr       ; so if it's second time, give up
  jsr reclaim           ; first time, do a reclaim pass
  jmp @chooseAddr       ; and try again
@notFound:
  jmp invalidAddrErr
@gotTarget:
  ; target addr was specified. See if we can fulfill the request.
  sta pTmp+1            ; save target addr
  lda targetAddr        ; all 16 bits
  sta pTmp
  jsr scanForAddr       ; locate block containing target addr
  beq @notFound         ; fail if we couldn't find it
  lda tSegType,x        ; check flags
  bmi reservedErr       ; if already active, can't re-allocate it
  bcc @noSplitStart     ; scanForAddr clears carry if addr is equal
@splitStart:
  ; need to split current segment into (cur..targetAddr) and (targetAddr..next)
  jsr grabSegment       ; get a new segment, index in Y (doesn't disturb X)
  lda targetAddr
  sta tSegAdrLo,y       ; targetAddr is start of new segment
  lda targetAddr+1
  sta tSegAdrHi,y       ; all 16 bits
  lda #0
  sta tSegType,y        ; clear flags on new segment
  sta tSegType,x        ; and old segment
  lda tSegLink,x        ; get link to next existing seg
  sta tSegLink,y        ; link new segment to it
  tya
  sta tSegLink,x        ; link cur segment to new segment
  tax                   ; new segment number to X
@noSplitStart:
  ; segment begins at exactly the right address. Does it end at the right addr?
  lda tSegAdrLo,x       ; calc seg addr + reqLen
  clc
  adc reqLen
  sta @reqEnd           ; save for later
  lda tSegAdrHi,x       ; add all 16 bits
  adc reqLen+1
  sta @reqEnd+1
  ldy tSegLink,x        ; index of next seg to Y reg
  cmp tSegAdrHi,y       ; is end at exactly the right place?
  bne @splitEnd
  lda @reqEnd
  cmp tSegAdrLo,y       ; compare all 16 bits
  beq @noSplitEnd
@splitEnd:
  ; need to split current segment into (cur..reqEnd) and (reqEnd..next)
  jsr grabSegment       ; get a new segment, index in Y (doesn't disturb X)
  lda @reqEnd
  sta tSegAdrLo,y       ; reqEnd is start of new segment
  lda @reqEnd+1
  sta tSegAdrHi,y       ; save all 16 bits
  lda #0
  sta tSegType,y        ; clear flags on new segment
  lda tSegLink,x        ; get link to next existing seg
  sta tSegLink,y        ; link new segment to it
  tya
  sta tSegLink,x        ; link cur segment to new segment
@noSplitEnd:
  ; current segment begins and ends at exactly the right addresses
  lda #$80              ; flag segment as active, not locked, not holding a resource
  sta tSegType,x        ; save the flags and type
  lda #0
  sta targetAddr        ; clear targetAddr for next future request
  sta targetAddr+1
  sta tSegResNum,x      ; might as well clear resource number too
  lda tSegAdrLo,x       ; get address for return
  ldy tSegAdrHi,x       ; all 16 bits
  stx segNum            ; save seg num in case internal caller routine needs it
  tax                   ; adr lo to proper register
  rts                   ; all done!
@reqEnd:
  .word 0
@reclaimFlg:
  .byte 0

;------------------------------------------------------------------------------
reclaim:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Reclaim not impl yet", 0

;------------------------------------------------------------------------------
shared_scan:
  sta isAuxCommand      ; save whether main or aux mem
  stx pTmp              ; save addr lo...
  sty pTmp+1            ; ... and hi
  jsr scanForAddr       ; scan for block that matches
  beq invalidAddrErr    ; if not found, invalid
  bcs invalidAddrErr    ; if addr not exactly equal, invalid
  lda tSegType,x        ; get existing flags
  bpl invalidAddrErr    ; must be an active block
  rts

invalidAddrErr:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Invalid addr", 0

;------------------------------------------------------------------------------
main_lock:
  lda #0                ; index for main-mem request
  beq shared_lock       ; always taken
aux_lock:
  lda #1                ; index for aux-mem request
shared_lock:
  jsr shared_scan       ; scan for exact memory block
  ora #$40              ; set the 'locked' flag
  sta tSegType,x        ; store flags back
  rts                   ; all done

;------------------------------------------------------------------------------
main_unlock:
  lda #0                ; index for main-mem request
  beq shared_unlock     ; always taken
aux_unlock:
  lda #1                ; index for aux-mem request
shared_unlock:
  jsr shared_scan       ; scan for exact memory block
  and #$BF              ; mask off the 'locked' flag
  sta tSegType,x        ; store flags back
  rts                   ; all done

;------------------------------------------------------------------------------
main_free:
  lda #0                ; index for main-mem request
  beq shared_free       ; always taken
aux_free:
  lda #1                ; index for aux-mem request
shared_free:
  jsr shared_scan       ; scan for exact memory block
  and #$3F              ; remove the 'active' and 'locked' flags
  sta tSegType,x        ; store flags back
  rts                   ; all done

;------------------------------------------------------------------------------
main_queueLoad:
  lda #0                ; flag for main mem
  beq shared_queueLoad  ; always taken
aux_queueLoad:
  lda #1                ; flag for aux mem
shared_queueLoad:
  sta isAuxCommand      ; save whether main or aux
  stx resType           ; save resource type
  sty resNum            ; save resource number
  jsr scanForResource   ; scan to see if we already have this resource in mem
  beq @notFound         ; nope, pass to next loader
  lda tSegType,x        ; get flags
  ora #$80              ; reactivate if necessary
  sta tSegType,x        ; save modified flag
  lda tSegAdrHi,x       ; get seg address hi
  tay                   ; in Y for return
  lda tSegAdrLo,x       ; addr lo
  tax                   ; in X for return
  rts                   ; all done
@notFound:
  ldx resType           ; restore res type
  ldy resNum            ; and number
  lda #QUEUE_LOAD       ; set to re-try same operation
  jmp nextLoaderVec     ; pass to next loader

;------------------------------------------------------------------------------
diskLoader:
  cmp #START_LOAD
  bne :+
  jmp disk_startLoad
: cmp #QUEUE_LOAD
  bne :+
  jmp disk_queueLoad
: cmp #FINISH_LOAD
  bne :+
  jmp disk_finishLoad
: ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Invalid command", 0

;------------------------------------------------------------------------------
openPartition:
  ; complete the partition file name
  lda curPartition
  clc
  adc #'0'              ; assume partition numbers range from 0..9 for now
  sta @partNumChar
  ; open the file
  jsr mli
  .byte MLI_OPEN
  .word @openParams
  bcs prodosError
  ; grab the file number, since we're going to keep it open
  lda @openFileRef
  sta partitionFileRef
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
  lda headerBuf         ; grab header size
  sta readLen           ; set to read that much. Will actually get 2 extra bytes,
                        ; but that's no biggie.
  lda headerBuf+1       ; hi byte too
  sta readLen+1
  lda #2                ; read just after the 2-byte length
  sta readAddr
  jmp readToMain        ; finish by reading the rest of the header
@openParams:
  .byte 3               ; number of params
  .word @filename       ; pointer to file name
  .word fileBuffer      ; pointer to buffer
@openFileRef:
  .byte 0               ; returned file number
@filename:
  .byte 15              ; length
  .byte "/LL/GAME.PART."        ; TODO: Figure out how to avoid specifying full path.
                                ; If I leave it out, ProDOS complains with error $40.
@partNumChar:
  .byte "x"             ; x replaced by partition number

readParams:
  .byte 4               ; number of params
readFileRef:
  .byte 0
readAddr:
  .word 0
readLen:
  .word 0
readGot:
  .word 0

;------------------------------------------------------------------------------
prodosError:
  pha
  lsr
  lsr
  lsr
  lsr
  jsr @digit
  sta @errNum
  pla
  jsr @digit
  sta @errNum+1
  ldx #<@msg
  ldy #>@msg
  jmp fatalError
@digit:
  and #$F
  ora #$B0
  cmp #$BA
  bcc :+
  adc #6
: rts
@msg:
  .byte "ProDOS error $"
@errNum:
  .byte "xx"
  .byte 0

;------------------------------------------------------------------------------
disk_startLoad:
  ; Make sure we don't get start without finish
  lda curPartition
  bne sequenceError
  ; Just record the partition number; it's possible we won't actually be asked
  ; to queue anything, so we should put off opening the file.
  stx curPartition
  rts

;------------------------------------------------------------------------------
sequenceError:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Bad sequence", 0

;------------------------------------------------------------------------------
startHeaderScan:
  lda #2                ; start scanning the partition header just after len
  sta pTmp
  lda #>headerBuf
  sta pTmp+1
  ldy #0
  rts

;------------------------------------------------------------------------------
disk_queueLoad:
  stx resType           ; save resource type
  sty resNum            ; and resource num
  lda partitionFileRef  ; check if we've opened the file yet
  bne :+                ; yes, don't re-open
  jsr openPartition     ; open the partition file
: jsr startHeaderScan   ; start scanning the partition header
@scan:
  lda (pTmp),y          ; get resource type
  beq @notFound         ; if zero, this is end of table: failed to find the resource
  iny
  cmp resType           ; is it the type we're looking for?
  bne @bump3            ; no, skip this resource
  lda (pTmp),y          ; get resource num
  cmp resNum            ; is it the number we're looking for
  bne @bump3            ; no, skip this resource
  iny                   ; Yay! We found the one we want.
  lda (pTmp),y          ; grab the length in bytes
  sta reqLen            ; save for later
  iny
  lda (pTmp),y          ; hi byte too
  sta reqLen+1
  dey                   ; back to start of record
  dey
  dey
  lda (pTmp),y          ; Get the resource type back
  ora #$80              ; special mark to say, "We want to load this segment"
  ldx isAuxCommand      ; if aux, set an extra-special flag so load will go there
  beq :+
  ora #$40              ; record that the eventual disk read should go to aux mem
: sta (pTmp),y          ; save modified type byte with the flags added to it
  jsr shared_alloc      ; reserve memory for this resource (main or aux as appropriate)
  stx tmp               ; save lo part of addr temporarily
  ldx segNum            ; get the segment number back
  lda resType           ; put resource type in segment descriptor
  ora #$80              ; add 'active' flag
  sta tSegType,x
  lda resNum            ; put resource number in segment descriptor
  sta tSegResNum,x
  ldx tmp               ; get back lo part of addr
  rts                   ; success! all done.
@bump3:
  iny                   ; skip the remaining 3 bytes of the 4-byte record
  iny
  iny
  bne @scan             ; happily, 4-byte records always end at page boundary
  inc pTmp+1            ; page boundary hit - go to next page of header
  bne @scan             ; always taken
@notFound:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Resource not found", 0
@resLen:
  .byte 0

;------------------------------------------------------------------------------
disk_finishLoad:
  lda partitionFileRef  ; See if we actually queued anything (and opened the file)
  bne :+                ; non-zero means we have work to do
  rts                   ; nothing to do - return immediately
: sta @setMarkFileRef   ; copy the file ref number to our MLI param blocks
  sta readFileRef
  sta @closeFileRef
  lda headerBuf         ; grab # header bytes
  sta @setMarkPos       ; set to start reading at first non-header byte in file
  lda headerBuf+1       ; hi byte too
  sta @setMarkPos+1
  lda #0
  sta @setMarkPos+2
  jsr startHeaderScan   ; start scanning the partition header
@scan:
  lda (pTmp),y          ; get resource type byte
  beq @done             ; zero = end of header
  bmi @load             ; hi bit set -> queued for load
  iny                   ; not set, not queued, so skip over it
  iny
  bne @next             ; always taken
@load:
  tax                   ; save flag ($40) to decide where to read
  and #$F               ; mask to get just the resource type
  sta resType           ; save type for later
  iny
  lda (pTmp),y          ; get resource num
  iny
  sta resNum            ; save resource number
  jsr @debug
  txa                   ; get the flags back
  ldx #0                ; index for main mem
  and #$40              ; check for aux flag
  beq :+                ; not aux, skip
  inx                   ; index for aux mem
: stx isAuxCommand      ; save main/aux selector
  sty @ysave            ; Save Y so we can resume scanning later.
  lda (pTmp),y          ; grab resource length
  sta readLen           ; save for reading
  iny
  lda (pTmp),y          ; hi byte too
  sta readLen+1
  jsr scanForResource   ; find the segment number allocated to this resource
  beq @addrErr          ; it better have been allocated
  lda tSegAdrLo,x
  sta readAddr
  lda tSegAdrHi,x
  sta readAddr+1
  jsr @debug2
  jsr mli               ; move the file pointer to the current block
  .byte MLI_SET_MARK
  .word @setMarkParams
  bcs @prodosErr
  bit isAuxCommand
  bvs @auxRead          ; decide whether we're reading to main or aux
  jsr readToMain
  bcc @resume           ; always taken
@auxRead:
  jsr readToAux
@resume:
  ldy @ysave
@next:
  lda (pTmp),y          ; lo byte of length
  clc
  adc @setMarkPos       ; advance mark position exactly that far
  sta @setMarkPos
  iny
  lda (pTmp),y          ; hi byte of length
  adc @setMarkPos+1
  sta @setMarkPos+1
  bcc :+
  inc @setMarkPos+2     ; account for partitions > 64K
: iny                   ; increment to next entry
  bne @scan             ; exactly 64 4-byte entries per 256 bytes
  inc pTmp+1
  bne @scan             ; always taken
@done:
  jsr mli               ; now that we're done loading, we can close the partition file
  .byte MLI_CLOSE
  .word @closeParams
  bcs @prodosErr
  lda #0                ; zero out...
  sta partitionFileRef  ; ... the file reference so we know it's no longer open
  rts
@prodosErr:
  jmp prodosError
@setMarkParams:
  .byte 2               ; param count
@setMarkFileRef:
  .byte 0               ; file reference
@setMarkPos:
  .byte 0               ; mark position (3 byte integer)
  .byte 0
  .byte 0
@closeParams:
  .byte 1               ; param count
@closeFileRef:
  .byte 0               ; file ref to close
@ysave:
  .byte 0
@addrErr:
  jmp invalidAddrErr
@debug:
  DEBUG_STR "Going to load: type="
  DEBUG_BYTE resType
  DEBUG_STR "num="
  DEBUG_BYTE resNum
  rts
@debug2:
  DEBUG_STR "readLen="
  DEBUG_WORD readLen
  DEBUG_STR "readAddr="
  DEBUG_WORD readAddr
  DEBUG_STR "."

;------------------------------------------------------------------------------
readToMain:
  jsr mli
  .byte MLI_READ
  .word readParams
  bcs @err
  rts
@err:
  jmp prodosError

;------------------------------------------------------------------------------
readToAux:
  lda readAddr
  sta @st+1
  lda readAddr+1
  sta @st+2
  lda #0
  sta readAddr
  lda #>auxBuffer       ; we're reading into a buffer in main mem
  sta readAddr+1
  lda readLen
  sta reqLen
  lda readLen+1
  sta reqLen+1
@nextGroup:
  ldx reqLen
  lda reqLen+1          ; see how many pages we want
  cmp #8                ; 8 or less?
  bcc :+                ; yes, read exact amount
  lda #8                ; no, limit to 8 pages max
  ldx #0
: stx readLen
  sta readLen+1         ; save number of pages
  jsr mli               ; now read
  .byte MLI_READ
  .word readParams
  bcs @err
  lda #>auxBuffer       ; set up copy pointers
  sta @ld+2
  lda readLen           ; set up:
  sta @chk+1            ; partial page check
  ldy #0
  ldx readLen+1         ; number of whole pages
@copy:
  sta setAuxWr          ; copy from main to aux mem
@ld:
  lda $100,y            ; from main mem
@st:
  sta $100,y            ; to aux mem
  iny
  beq @pageEnd          ; end of full page? - work to do
@chk:
  cpy #11               ; end of partial page? (self-modified earlier)
  bne @ld
  txa                   ; last page?
  bne @ld
  beq @next
@pageEnd:
  sta clrAuxWr          ; normal memory writes so we can increment
  inc @ld+2             ; self-modifying
  inc @st+2
  dex                   ; dec page count
  bne @copy             ; any pages left? go more
  lda readLen
  bne @copy             ; partial page left? go more
@next:
  sta clrAuxWr          ; normal memory writes
  lda reqLen            ; decrement reqLen by the amount we read
  sec
  sbc readLen
  sta reqLen
  sta @or+1             ; save lo for later
  lda reqLen+1          ; all 16 bits of reqLen
  sbc readLen+1
  sta reqLen+1
@or:
  ora #11               ; self-modified above
  bne @nextGroup        ; anything left to read, do more
  rts                   ; all done
@err:
  jmp prodosError

;------------------------------------------------------------------------------
; Segment tables

 .if DEBUG
 .align 256
 .endif

tSegLink   = *
tSegType   = tSegLink + MAX_SEGS
tSegResNum = tSegType + MAX_SEGS
tSegAdrLo  = tSegResNum + MAX_SEGS
tSegAdrHi  = tSegAdrLo + MAX_SEGS

;------------------------------------------------------------------------------
; Marker for end of the tables, so we can compute its length
tableEnd = tSegAdrHi + MAX_SEGS
