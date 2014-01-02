; Memory manager
; ------------------
;
; Memory is managed in 256-byte pages (to correspond with Prodos block sizes)
; In each 48kb memory bank (main and aux), a lookup table identifies what pages, 
; if any, are used there as well as usage flags to mark free, used, or reserved memory.
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
; Memory operations are performed in sets. A set is begun with the START_LOAD call, and
; subsequent QUEUE_LOAD requests are queued up. The set is then executed with 
; FINISH_LOAD. The purpose of queuing the requests is to allow the disk driver to
; sort the requests in storage order and thus optimize loading speed. During the period 
; between START_LOAD and FINISH_LOAD, the area in main memory from $4000 to 5FFF is 
; reserved for memory manager operations. Therefore, if hi-res graphics are showing it
; is important to copy them to page 1 ($2000.3FFF) and switch to that display before
; queueing loads.
;
; The goal of using extended ram is to prevent future disk access as much as possible,
; because even an inefficient o(N) memory search is going to be faster than spinning up
; a disk.
;
; ----------------------------
; Page table format in memory:
; FFFFtttt nnnnnnnn
; F = Flags
;   7 - Active/Inactive
;   6 - Primary (1) or Secondary (0)
;       Memory pages are allocated in chunks, the first page is always primary
;       So detecting primary pages means we can clear more than one page at a time
;   5 - Locked (cannot be reclaimed for any reason)
; t = Type of resource (1-15, 0 is invalid)
; n = Resource number (1-255, 0 is invalid)
;
; -------------------------
; Page file format on disk:
;   File must be named: GAME.PART.nnn where nnn is the partition number (0-15)
;   File Header:
;     byte 0: Total # of pages in header
;     byte 1-n: Table of repeated resource entries
;   Resource entry:
;     byte 0: resource type (1-15), 0 for end of table
;     byte 1: resource number (1-255)
;     byte 2: number of pages
;   The remainder of the file is the page data for the resources, in order of
;   their table appearance.
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
    ; Input:  X-reg - number of pages to allocate
    ;
    ; Output: A-reg - starting memory page that was allocated
    ;
    ; Allocate a number of blocks in the memory space of this loader. If there 
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
SET_MEM_TARGET = $12
    ; Input: X-reg - page number target
    ;
    ; Output: None
    ;
    ; Sets the target page in memory for the next REQUEST_MEMORY or QUEUE_LOAD 
    ; command. This will force allocation at a specific location instead 
    ; allowing the loader to choose.
    ;
    ; This is a one-shot command, i.e. as soon as an allocation is performed,
    ; subsequent allocations will revert to their normal behavior.
		
;------------------------------------------------------------------------------
START_LOAD = $13
    ; Input:  X-reg - disk partition number (0 for boot disk, 1-15 for others)
    ;
    ; Output: None
    ;
    ; Marks the start of a set of QUEUE_LOAD operations, that will be
    ; acted upon when FINISH_LOAD is finally called.
    ;
    ; The partition is recorded and passed on to chained loaders.


;------------------------------------------------------------------------------
QUEUE_LOAD = $14
    ; Input: X-reg - resource type
    ;        Y-reg - resource number
    ;
    ; Output: A-reg - memory page the load will occur at
    ;
    ; This is the main entry for loading resources from disk. It queues a load
    ; request, allocating main memory to hold the entire resource. Note that
    ; the load is only queued; it will be completed by FINISH_LOAD.
    ;
    ; Normally this command chooses the location of the memory area; if you
    ; want to force it to use a particular location, use SET_MEM_TARGET first.
    ;
    ; Note that if the data is already in memory, its former location will
    ; be returned and no disk access will be queued.
    ;
    ; The request is either acted upon by this loader, or passed onto the
    ; next chained loader. If there is no next loader, a FATAL_ERROR is 
    ; triggered.

;------------------------------------------------------------------------------
FINISH_LOAD = $15
    ; Input: None
    ;
    ; Output: None
    ;
    ; Completes all prior QUEUE_LOAD requests, clearing the queue. It's the
    ; last part of a START_LOAD / QUEUE_LOAD / FINISH_LOAD sequence.
    ;
    ; This command is acted upon by this loader and passed to chained loaders.

;------------------------------------------------------------------------------
FREE_MEMORY = $16
    ; Input: X-reg - starting page number to mark free (must be start of a
    ;                memory area that was requested, locked, or loaded)
    ;
    ; Output: None
    ;
    ; Mark a block of memory as free, or rather inactive, so that it can be 
    ; reused. This also clears the lock bit!
		
;------------------------------------------------------------------------------
CHAIN_LOADER = $17
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
FATAL_ERROR = $18
    ; Input:  X-reg / Y-reg: message pointer (X=lo, Y=hi)
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

  ; zero page
  pPageTbl1 = $6        ; length 2
  pPageTbl2 = $8        ; length 2

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
isAuxCommand:
  .byte 0
scanStart:
  .byte 0
targetPage:
  .byte 0
nextLoaderVec: 
  jmp diskLoader
curPartition:
  .byte 0
partitionFileRef:
  .byte 0
nHeaderPages:
  .byte 0

;------------------------------------------------------------------------------
main_dispatch:
  cmp #REQUEST_MEMORY
  bne :+
  jmp main_request
: cmp #QUEUE_LOAD
  bne shared_dispatch
  jmp main_queueLoad
shared_dispatch:
  cmp #RESET_MEMORY
  bne :+
  jmp reset
: cmp #SET_MEM_TARGET
  bne :+
  stx targetPage
  rts
: cmp #FREE_MEMORY
  bne :+
  jmp main_free
: cmp #FATAL_ERROR
  bne :+
  jmp fatalError
; Pass command to next chained loader
  jmp nextLoaderVec

;------------------------------------------------------------------------------
aux_dispatch:
  cmp #REQUEST_MEMORY
  bne :+
  jmp aux_request
: cmp #QUEUE_LOAD
  bne :+
  jmp aux_queueLoad
: cmp #FREE_MEMORY
  bne :+
  jmp aux_free
: jmp shared_dispatch

;------------------------------------------------------------------------------
; Print fatal error message (custom or predefined) and print the
; call stack, then halt.
fatalError:
  sty pTmp+1    ; save message ptr hi...
  stx pTmp      ; ...and lo
  jsr setnorm   ; set up text mode and vectors
  jsr textinit
  jsr setvid
  jsr setkbd
  ldy #0
: lda fatalMsg,y  ; print out prefix message
  beq :+
  ora #$80
  jsr cout
  iny
  bne :-
: ldy #0        ; start at first byte of message
: lda (pTmp),y
  beq :+
  ora #$80      ; ensure hi-bit ASCII for cout
  jsr cout
  iny
  bne :-
  .if DEBUG
; Print call stack
: ldy #0
: lda stackMsg,y
  beq :+
  ora #$80
  jsr cout
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
  and #$C0      ; avoid accidentally grabbing data from the IO area
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
  lda #$A0
  jsr cout
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
  .byte $8D,"FATAL ERROR: ", 0
stackMsg:
  .byte $8D,"  Call stack: ", 0

;------------------------------------------------------------------------------
init:
  ; clear the page tables
  ldy #0
  tya
: sta main_pageTbl1,y
  sta main_pageTbl2,y
  sta aux_pageTbl1,y
  sta aux_pageTbl2,y
  iny
  cpy #$C0
  bne :-
  ; Lock page zero through end of memory manager
  ldy #0
  lda #$80+$20
: sta main_pageTbl1,y
  iny
  cpy #>tableEnd       ; next page after page tables
  bne :-
  ; Lock both hi-res pages
  ldy #$20
: sta main_pageTbl1,y
  iny
  cpy #$60
  bne :-
  ; Lock ProDOS system page
  sta main_pageTbl1+$BF
  ; Lock pages 0 and 1 in aux mem
  sta aux_pageTbl1
  sta aux_pageTbl1+1
  .if DEBUG
  jmp test
  .endif
  rts

;------------------------------------------------------------------------------
  .if DEBUG
test:
  DEBUG_STR "Testing memory manager."
  jmp reservedErr
  .endif

;------------------------------------------------------------------------------
main_setup:
  lda #<main_pageTbl1
  sta pPageTbl1
  lda #>main_pageTbl1
  sta pPageTbl1+1
  lda #<main_pageTbl2
  sta pPageTbl2
  lda #>main_pageTbl2
  sta pPageTbl2+1
  lda #0
  sta isAuxCommand
  rts

;------------------------------------------------------------------------------
aux_setup:
  lda #<aux_pageTbl1
  sta pPageTbl1
  lda #>aux_pageTbl1
  sta pPageTbl1+1
  lda #<aux_pageTbl2
  sta pPageTbl2
  lda #>aux_pageTbl2
  sta pPageTbl2+1
  lda #$40              ; special flag for marking aux queueing
  sta isAuxCommand
  rts

;------------------------------------------------------------------------------
reset:
  ; Set all non-locked pages to inactive.
  jsr main_setup
  jsr @inactivate
  jsr aux_setup
  jsr @inactivate
  jmp nextLoaderVec     ; allow chained loaders to reset also
@inactivate:
  ldy #0
@loop:
  lda (pPageTbl1),y
  tax
  and #$20              ; check lock bit
  bne :+                ; skip if page is locked
  txa
  and #$7F
  sta (pPageTbl1),y
: cpy #$C0              ; stop at end of 48K mem bank
  bne @loop
  rts

;------------------------------------------------------------------------------
main_request:
  jsr main_setup
shared_request:
  lda targetPage        ; see if SET_MEM_TARGET was called
  ldy #0
  sty targetPage        ; clear it for next time
  tay
  bne @gotPage          ; if SET_MEM_TARGET was called, don't scan
  ; need to scan for a block that has enough pages
  stx tmp               ; save number of pages
  ldy scanStart         ; start scanning at end of last block
  dey
@blockLoop:
  iny                   ; try next page
  sty tmp+1             ; remember starting page of area
  ldx #0                ; initialize count of free pages found
@pageLoop:
  cpy scanStart         ; are we back where we started?
  beq outOfMemErr       ; if so, we have failed.
  cpy #$C0              ; reached end of mem?
  bne :+                ; no, proceed
  ldy #$FF              ; yes, start over at beginning of memory
  bne @blockLoop        ; always taken
: lda (pPageTbl1),y     ; is page active?
  bmi @blockLoop        ; yes active, skip it
  iny
  inx                   ; got one more inactive page
  cpx tmp               ; is it enough?
  bcc @pageLoop         ; no, keep going
@foundBlock:            ; yes, got enough
  ldy tmp+1             ; recall starting page
@gotPage:
  lda #$C0              ; mark first page as $80 (active) + $40 (primary)
: cpy #$C0              ; all pages from $C0.FF are reserved
  bcs reservedErr
  pha
  lda (pPageTbl1),y     ; don't want to reserve same area twice
  bne reservedErr
  pla
  sta (pPageTbl1),y
  lda #$80              ; mark subsequent pages as $80 (active) but not primary
  iny
  dex
  bne :-
  sty scanStart         ; start next scan at page after the last one allocated
  lda tmp+1             ; return starting page
  rts

;------------------------------------------------------------------------------
reservedErr:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Mem already reserved", 0

;------------------------------------------------------------------------------
outOfMemErr:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Out of mem", 0

;------------------------------------------------------------------------------
aux_request:
  jsr aux_setup
  jmp shared_request

;------------------------------------------------------------------------------
main_free:
  jsr main_setup
shared_free:
  txa                   ; move page num
  tay                   ; from X to Y
  lda (pPageTbl1),y     ; fetch flags of first page
  and #$C0              ; active and primary?
  bpl @err              ; no, that's an error
  lda (pPageTbl1),y     ; clear the active flag
  and #$7F
  sta (pPageTbl1),y
  iny
@loop:
  lda (pPageTbl1),y
  bpl @done
  and #$7F
  sta (pPageTbl1),y
  iny
  cpy #$C0
  bne @loop
@done:
  rts
@err:
  jmp reservedErr

aux_free:
  jsr aux_setup
  jmp shared_free

;------------------------------------------------------------------------------
main_queueLoad:
  jsr main_setup
shared_queueLoad:
  stx tmp               ; save resource type
  sty tmp+1             ; save resource number
  ; Scan to see if we already have this resource in memory
  ldy #0
@scanLoop:
  lda (pPageTbl1),y
  and #$40              ; primary? that is, start of an area?
  bcc @skip             ; no, skip it
  lda (pPageTbl1),y
  and #$F               ; extract resource type
  cmp tmp               ; is it the type we're looking for?
  bne @skip             ; no, skip it
  lda (pPageTbl2),y     ; get resource number
  cmp tmp+1             ; is it the resource # we're looking for?
  beq @found            ; yes! found what we want.
@skip:
  iny                   ; next page
  cpy #$C0              ; end of memory?
  bne @scanLoop         ; no, keep scanning
  ldx tmp               ; yes, end of memory. Recall type parameter
  ldy tmp+1             ; recall resource number
  jmp nextLoaderVec     ; call next loader so it can load the resource
@found:
  lda (pPageTbl1),y
  ora #$80              ; mark this area as active now
  sta (pPageTbl1),y
  tya                   ; transfer page num to A-reg for API return
  rts

;------------------------------------------------------------------------------
aux_queueLoad:
  jsr aux_setup
  jmp shared_queueLoad

;------------------------------------------------------------------------------
diskLoader:
  cmp #START_LOAD
  bne :+
  jmp disk_startLoad
: cmp #QUEUE_LOAD
  bne :+
  jmp disk_queueLoad
: cmp #FINISH_LOAD
  bne @cmdError
  jmp disk_finishLoad
@cmdError:
  ldx #<:+
  ldy #>:+
  jmp fatalError
  .byte "Invalid command", 0

;------------------------------------------------------------------------------
openPartition:
  ; complete the partition file name
  lda curPartition
  clc
  adc #'0'              ; assume partitions 0-9 for now
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
  ; Read the first byte, which tells us how many pages in the header
  lda #<headerBuf
  sta readAddr
  lda #>headerBuf
  sta readAddr+1
  lda #1
  sta readLen
  lda #0
  sta readLen+1
  jsr @doRead
  ; Grab the number of header pages, then read in the rest of the header
  ldx headerBuf
  stx nHeaderPages
  dex
  stx readLen+1
  lda #$FF
  sta readLen
  lda #1
  sta readAddr
  ; fall into @doRead
@doRead:
  jsr mli
  .byte MLI_READ
  .word readParams
  bcs prodosError
  rts
@openParams:
  .byte 3               ; number of params
  .word @filename       ; pointer to file name
  .word fileBuffer      ; pointer to buffer
@openFileRef:
  .byte 0               ; returned file number
@filename:
  .byte 11              ; length
  .byte "GAME.PART."
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
  lda #<headerBuf       ; start scanning the partition header
  sta pTmp
  lda #>headerBuf
  sta pTmp+1
  ldy #0
  rts

;------------------------------------------------------------------------------
bump128:
  tya
  and #$7F
  tay
  lda pTmp
  eor #$80
  sta pTmp
  bne :+
  inc pTmp+1
: rts

;------------------------------------------------------------------------------
disk_queueLoad:
  stx tmp               ; save resource type
  sty tmp+1             ; and resource num
  lda partitionFileRef  ; check if we've opened the file yet
  bne :+                ; yes, don't re-open
  jsr openPartition     ; open the partition file
: jsr startHeaderScan   ; start scanning the partition header
@scan:
  lda (pTmp),y          ; get resource type
  beq @notFound         ; if zero, this is end of table: failed to find the resource
  cmp tmp               ; is it the type we're looking for?
  bne @next             ; no, skip this resource
  iny
  lda (pTmp),y          ; get resource num
  dey
  cmp tmp+1             ; is it the number we're looking for
  bne @next             ; no, skip this resource
  lda (pTmp),y          ; Yay! We found the one we want. Prepare to mark it
  ora #$80              ; special mark to say, "We want to load this segment"
  ora isAuxCommand      ; record whether the eventual disk read should go to aux mem
  sta (pTmp),y
  rts                   ; success! all done.
@next:
  iny
  iny
  iny
  bpl @scan
  jsr bump128
  jmp @scan
@notFound:
  ldx #<:+
  ldy #>:+
  jmp fatalError
: .byte "Resource not found", 0

;------------------------------------------------------------------------------
disk_finishLoad:
  lda partitionFileRef  ; See if we actually queued anything (and opened the file)
  bne :+                ; non-zero means we have work to do
  rts                   ; nothing to do - return immediately
: sta @setMarkFileRef    ; copy the file ref number to our MLI param blocks
  sta readFileRef
  sta @closeFileRef
  lda nHeaderPages      ; set to start reading at first non-header page in file
  sta @setMarkPos+1
  lda #0
  sta @setMarkPos+2
  sta readAddr
  sta readLen
  jsr startHeaderScan   ; start scanning the partition header
@scan:
  lda (pTmp),y          ; get resource type byte
  iny
  tax                   ; save type for later
  beq @done             ; if zero, this is end of table and we're done
  cmp tmp               ; is it the type we're looking for?
  bne @bump2            ; no, skip this resource
  lda (pTmp),y          ; get resource num
  iny
  cmp tmp+1             ; is it the number we're looking for
  bne @bump1            ; no, skip this resource
  sty @ysave            ; found the resource. Save Y so we can resume later.
  tay                   ; resource num in Y
  stx isAuxCommand      ; save flag ($40) to decide where to read
  txa
  and #$F               ; mask to get just the resource type
  tax                   ; resource type in X
  jsr main_queueLoad    ; find the page number allocated to this resource
  sta readTargetPage    ; save for later
  ldy @ysave            ; get back to entry in partition header
  lda (pTmp),y          ; # of pages to read
  sta nPagesToRead      ; save for later
  jsr mli               ; move the file pointer to the current block
  .byte MLI_SET_MARK
  .word @setMarkParams
  bcs @err
  bit isAuxCommand
  bvs @auxRead          ; decide whether we're reading to main or aux
  jsr readToMain
  bcc @resume           ; always taken
@auxRead:
  jsr readToAux
@resume:
  ldy @ysave
  jmp @next
@bump2:
  iny
@bump1:
  iny
@next:
  lda (pTmp),y          ; number of pages
  clc
  adc @setMarkPos+1     ; advance mark position that far
  sta @setMarkPos+1
  bcc :+
  inc @setMarkPos+2     ; account for files > 64K
: iny                   ; increment to next entry
  bpl @scan
  jsr bump128
  jmp @scan
@done:
  jsr mli               ; now that we're done loading, we can close the partition file
  .byte MLI_CLOSE
  .word @closeParams
  bcs @err
  lda #0                ; zero out...
  sta partitionFileRef  ; ... the file reference so we know it's no longer open
  rts
@err:
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

readTargetPage:
  .byte 0
nPagesToRead:
  .byte 0

;------------------------------------------------------------------------------
readToMain:
  lda readTargetPage
  sta readAddr+1
  lda nPagesToRead
  sta readLen+1
  jsr mli
  .byte MLI_READ
  .word readParams
  bcs @err
  rts
@err:
  jmp prodosError

;------------------------------------------------------------------------------
readToAux:
  lda #>auxBuffer       ; we're reading into a buffer in main mem
  sta readAddr+1
  lda #>readTargetPage  ; set up copy target page
  sta @st+1
@nextGroup:
  lda nPagesToRead      ; see how many pages we want
  cmp #8                ; 8 or less?
  bcc :+                ; yes, read exact
  lda #8                ; no, limit to 8 pages max
: sta readLen+1         ; save number of pages
  jsr mli               ; now read them
  .byte MLI_READ
  .word readParams
  bcs @err
  lda #>auxBuffer       ; set up copy pointers
  sta @ld+1
  sta setAuxWr          ; copy from main to aux mem
  ldy #0
@ld:
  lda $100,y            ; from main mem
@st:
  sta $100,y            ; to aux mem
  iny
  bne @ld               ; loop to copy a whole page
  sta clrAuxWr          ; normal memory writes
  dec nPagesToRead      ; dec total number of pages
  beq @done             ; when zero, we're done
  inc @ld+1             ; prep for next page
  inc @st+1
  dec readLen+1         ; copy only number of pages we read this round
  bne @ld               ; loop to copy next page
  beq @nextGroup        ; always taken
@done:
  rts
@err:
  jmp prodosError

;------------------------------------------------------------------------------
; Page tables
  .align 256
main_pageTbl1 = *
main_pageTbl2 = main_pageTbl1 + $C0
aux_pageTbl1  = main_pageTbl2 + $C0
aux_pageTbl2  = aux_pageTbl1 + $C0

;------------------------------------------------------------------------------
; Marker for end of the tables, so we can compute its length
tableEnd = *
