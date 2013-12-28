; Memory manager
; ------------------
;
; Memory is managed in 256-byte pages (to correspond with Prodos block sizes)
; In each 48kb memory bank (main and aux), a lookup table identifies what pages, 
; if any, are used there as well as usage flags to mark free, used, or reserved memory.
;
; Memory is marked as used as it is loaded by the loader, but the caller program
; should mark memory as free as soon as the memory is no longer in use.  It is
; very possible that the memory will not be reclaimed right away and could be
; reinstated as in-use without a loading penalty.
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
;   6 - Locked (Cannot reclaim for any reason)
;   5 - Primary (1) or Secondary (0)
;       Memory pages are allocated in chunks, the first page is always primary
;       So detecting primary pages means we can clear more than one page at a time
;   4 - Loaded (Memory contains copy of disk-based resource identified below)
; t = Type of resource (1-15, 0 is invalid)
; n = Resource number (1-255, 0 is invalid)
;
; -------------------------
; Page file format on disk:
;   File must be named: DISK.PART.nnn where nnn is the partition number (0-15)
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
mainLoader = $800
auxLoader  = $803

; Monitor routines
setNorm = $FE84
monInit = $FB2F
setVid = $FE93
setKbd = $FE89

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
    ; 0800.xxxx: The memory manager and its page table
    ; BF00.BFFF: ProDOS system page
    ;
    ; Note that this does *not* destroy the contents of memory, so for instance
    ; future RECALL_MEMORY commands may be able to re-use the existing contents
    ; of memory if they haven't been reallocated to something else.
    ;
    ; This command is acted upon and then passed on to chained loaders.

;------------------------------------------------------------------------------
LOCK_MEMORY = $11
    ; Input:  X-reg - page address for start of reservation
    ;         Y-reg - number of pages to reserve
    ;
    ; Output: None
    ;
    ; Reserve a specific area of memory. If it cannot be reserved for any reason,
    ; a FATAL_ERROR is triggered.
    ;
    ; This command is acted upon immediately and chained loaders are not called.

;------------------------------------------------------------------------------
REQUEST_MEMORY = $12
    ; Input:  X-reg - number of pages to allocate
    ;
    ; Output: X-reg - starting memory page that was allocated
    ;
    ; Allocate a number of blocks in the memory space of this loader. If there 
    ; isn't a large enough continguous memory segment available, the system 
    ; will be halted immediately with HALT_MEMORY.
    ;
    ; To allocate main memory, call the main memory loader. To allocate aux
    ; mem, call that loader instead.
    ;
    ; This command is acted upon immediately and chained loaders are not called.

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
    ; Output: X-reg - memory page the load will occur at
    ;
    ; This is the main entry for loading resources from disk. It queues a load
    ; request, allocating main memory to hold the entire resource. Note that
    ; the load is only queued; it will be completed by FINISH_LOAD.
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
FATAL_ERROR = $17
    ; Input:  X-reg / Y-reg: message number or pointer (see below)
    ;
    ; Output: Never returns
    ;
    ; Switches to text mode, prints out a predefined or custom error message, 
    ; plus the call stack, and then halts the system (i.e. it waits forever, 
    ; user has to press Reset).
    ;
    ; If Y-reg is zero, this prints one of these predefined messages based on
    ; X-reg:
    LOAD_ERR_INVALID_COMMAND = $01
    LOAD_ERR_BAD_PARAM       = $02
    LOAD_ERR_OUT_OF_MEMORY   = $03
    LOAD_ERR_RESERVED_MEM    = $04
    LOAD_ERR_UNKNOWN         = $05
    ;
    ; If Y-reg is non-zero, it's taken as the high byte of a pointer to
    ; a zero-terminated, ASCII message to print. X-reg is the low byte of the
    ; message pointer.
    ;
    ; This command halts and thus never returns.

;------------------------------------------------------------------------------
; code begins here
* = $800
 jmp mainLoader
 jmp auxLoader

nextLoaderVec: jmp diskLoader

mainLoader:
  lda #0        ; incremented after init
  bne :+
  jmp init
: cmp #RESET_MEMORY
  bne :+
  jmp main_reset
: cmp #LOCK_MEMORY
  bne :+
  jmp main_lock
: cmp #REQUEST_MEMORY
  bne :+
  jmp main_req
: cmp #FATAL_ERROR
  bne :+
  jmp fatalError
: cmp #START_LOAD
  bcc cmdError
  cmp #FINISH_LOAD+1
  bcs cmdError
; Found a command that needs to be chained to next loader
  jmp nextLoaderVec

cmdError:
  ldx #LOAD_ERR_INVALID_COMMAND
  ldy #0
fatalError:
  tya
  bne customErr
predefErr:
  dex
  beq foundErrMsg
: iny
  lda errorText,y
  bne :-
  beq predefErr
foundErrMsg:
  tya
  clc
  adc #<errorText
  tax
  ldy #>errorText
  bcc customErr
  iny
customErr:
  sty pTmp+1
  stx pTmp
; Set up text mode, print message
printErr:
  jsr setNorm
  jsr monInit
  jsr setVid
  jsr setKbd
  jsr crout
  jsr crout
  ldy #0
: lda (pTmp),y
  beq :+
  jsr cout
  iny
  bne :-
; Print call stack
  jsr crout
  tsx
: cpx #$FF
  beq :+
  inx
  lda 100,x
  sec
  sbc #2
  sta pTmp
  lda 101,x
  sbc #0
  sta pTmp+1
  and #$C0
  cmp #$C0
  beq :-
  ldy #0
  lda (pTmp),y
  cmp #$20
  bne :-
  lda pTmp+1
  jsr prByte
  lda pTmp
  jsr prByte
  lda #$A0
  jsr cout
  jmp :-
; Beep, and loop forever
: jsr bell
loopForever: jmp loopForever

errorText:
  .byte "Invalid command", 0
  .byte "Bad parameter", 0
  .byte "Reserved memory", 0
  .byte "Unknown error", 0
  .byte 0
