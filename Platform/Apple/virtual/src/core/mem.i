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
startMemMgr	= $800
mainLoader	= $803
auxLoader	= $806

;------------------------------------------------------------------------------
; Resource types

RES_TYPE_CODE	= 1
RES_TYPE_2D_MAP	= 2
RES_TYPE_3D_MAP	= 3
RES_TYPE_TILE	= 4
RES_TYPE_TEXTURE= 5
RES_TYPE_SCREEN	= 6

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
    ; 4000.5FFF: Reserved during queue operations
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
    ; Input: X-reg = 0 to close out and release $4000.5fff,
    ;        X-reg = 1 to keep open (for anticipated immediate queueing)
    ;
    ; Output: None
    ;
    ; Completes all prior QUEUE_LOAD requests, clearing the queue. It's the
    ; last part of a START_LOAD / QUEUE_LOAD / FINISH_LOAD sequence. If more
    ; loads are anticipated right away, set X-reg to 1 to keep the loader
    ; open which will make them faster. If no more loads right away, set
    ; X-reg to 0.
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

