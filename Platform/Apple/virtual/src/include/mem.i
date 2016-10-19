;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

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
;   Followed by one entry per resource. Each entry:
;     byte  0:    low nibble: resource type (1-15), 0 for end of table
;	          high nibble: reserved (should be zero)
;     byte  1:    resource number (1-255)
;     bytes 2-3:  number of on-disk bytes in resource (lo, hi)
;		  if resource is compressed, this is the compressed size, and the hi bit
;		  will be set. If so, bytes 4 and 5 will appear; if not, they'll be absent.
;    (bytes 4-5): (only present if compressed) decompressed size (lo, hi) 
;   The remainder of the file is the data for the resources, in order of their
;   table appearance.
;
; ------------------------------------------------------------
; Animated resource format (frame images, portraits, textures)
;   bytes 0-1: offset to animation header (or $0000 if not animated)
;   bytes 2-n: invariant image data
; Followed by animation header:
;   byte  0:   animation type (1=Forward, 2=Forward+Backward, 3=Random)
;   byte  1:   current anim dir
;   byte  2:   index of last frame (= number of frames *minus 1*)
;   byte  3:   current anim frame
; Followed by patches. Each patch:
;   bytes 0-1: length of patch (including this length header, and also anim hdr for 1st patch)
;   bytes 2-n: hunks, each hunk:
;              byte 0:    skip len (0-254) or $FF for end-of-patch
;              byte 1:    copy len (0-254)
;              bytes 2-n: bytes to copy

mainLoader	= $800
auxLoader	= mainLoader+3

;------------------------------------------------------------------------------
; Resource types

RES_TYPE_CODE	  = 1
RES_TYPE_2D_MAP	  = 2
RES_TYPE_3D_MAP	  = 3
RES_TYPE_TILESET  = 4
RES_TYPE_TEXTURE  = 5
RES_TYPE_SCREEN	  = 6
RES_TYPE_FONT	  = 7
RES_TYPE_MODULE   = 8
RES_TYPE_BYTECODE = 9
RES_TYPE_FIXUP    = 10
RES_TYPE_PORTRAIT = 11

;------------------------------------------------------------------------------
; Command codes

;------------------------------------------------------------------------------
RESET_MEMORY = $10
    ; Input: None
    ;
    ; Output: None
    ;
    ; Mark all non-locked memory segments as inactive.
    ;
    ; Note that this does *not* destroy the contents of memory, so for instance
    ; future QUEUE_LOAD commands may be able to re-use the existing contents
    ; of memory if they haven't been reallocated to something else.
    ;
    ; Also note that this does not erase the small-object heap if one has been
    ; established. But it also doesn't check that the heap has been properly
    ; protected by locking, that's your job.
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
CALC_FREE = $19
    ; Input: None
    ;
    ; Output: X-reg(lo) / Y-reg(hi) - bytes of memory currently free
    ;
    ; Calculate how much memory this loader has free. Call on main mem
    ; loader for main mem free, or aux mem loader for aux mem free.
		
;------------------------------------------------------------------------------
DEBUG_MEM = $1A
    ; Input: None
    ;
    ; Output: None
    ;
    ; Print out the currently allocated memory blocks and their states.

;------------------------------------------------------------------------------
CHECK_MEM = $1B
    ; Input: None
    ;
    ; Output: None
    ;
    ; Check that memory manager structures (and heap structures, if a heap
    ; has been set) are all intact.
        
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
    ; Input:  X-reg(lo) / Y-reg(hi): message pointer. Message can be:
    ; (1) a zero-terminated, hi-bit ASCII string, (assembly style), or
    ; (2) a length-prefixed, lo-bit ASCII string (PLASMA / ProDOS style)
    ;
    ; Output: Never returns
    ;
    ; Switches to text mode, prints out the error message pointed to by the 
    ; parameters, plus the call stack, and then halts the system (i.e. it waits 
    ; forever, user has to press Reset).
    ;
    ; This command halts and thus never returns.

;------------------------------------------------------------------------------
HEAP_SET = $20
    ; Input:  X-reg(lo) / Y-reg(hi): pointer to allocated block for heap
    ;
    ; Output: None
    ;
    ; Establishes a block of memory to use as a garbage collected small-object 
    ; heap. The block must be page-aligned and sized in whole pages, and 
    ; generally should be locked first.
    ;
    ; Also clears the table of heap types, so HEAP_ADD_TYPE will start again
    ; setting the global type ($80).

;------------------------------------------------------------------------------
HEAP_ADD_TYPE = $21
    ; Input:  X-reg(lo) / Y-reg(hi): pointer to type table
    ;
    ; Output: None
    ;
    ; Adds a type to the list of known heap types. Each type will be assigned
    ; a number starting at $80 and going up to $81, $82, etc. By convention 
    ; type $80 should be the single "Global" object from which all others live
    ; objects can be traced.
    ;
    ; The type table for the type should be laid out as follows:
    ;  byte 0: length byte (1 to 127)
    ;  byte 1: offset of first pointer (1 to 127)
    ;  byte 2: offset of second pointer
    ;  ...
    ;  byte n: zero (0) value marks end of table

;------------------------------------------------------------------------------
HEAP_ALLOC = $22
    ; Input:  X-reg: string length $00-7F, or type code $80-FF
    ;
    ; Output: X-reg(lo) / Y-reg(hi): pointer to allocated object space
    ;
    ; Allocates an object on the heap. If X <= $7F, it's a string object
    ; (no internal pointers). If X >= $80, it's a typed object corresponding
    ; to the types added with HEAP_TYPE.
    ;
    ; The first byte of the returned block will be the length or type code,
    ; (which you must never change), and subsequent bytes are initialized to 
    ; all zero.
    ;
    ; By convention, the very first block allocated should be of the "Global" 
    ; type ($80) and all other live objects must be traceable from there.
    ;
    ; If there's no room on the heap, an fatal error will be thrown. The system
    ; assumes that HEAP_COLLECT is not safe to run at any time. Rather, you
    ; should call it periodically when you're certain no pointers to heap
    ; objects (except the global object) are on the system stack.
    ;
    ; Note: strings of length zero are considered valid and supported.

;------------------------------------------------------------------------------
HEAP_INTERN = $23
    ; Input:  X-reg(lo) / Y-reg(hi): PLASMA-style string in regular RAM
    ;
    ; Output: X-reg(lo) / Y-reg(hi): pointer to allocated object space
    ;
    ; Checks for existing, or allocates space for and copies, a PLASMA-style
    ; string starting with a length byte.
    ;
    ; If an identical string is already on the heap, returns a pointer to that.
    ; Else, allocates heap space and copy the string into it.

;------------------------------------------------------------------------------
HEAP_COLLECT = $24
    ; Input:  None.
    ;
    ; Output: X-reg(lo) / Y-reg(hi): new top of heap after collection. If you
    ;            need to know free space, subtract this from the end-of-heap.
    ;
    ; Traces objects in the heap to determine which ones are "live", that is,
    ; reachable from the very first object allocated. By convention, that first
    ; object should be of the "Global" type, i.e. $80, and everything reachable
    ; from pointers there is considered live.
    ;
    ; Live objects are then coalesced together in contiguous memory, squeezing 
    ; out any objects that can no longer be reached.
    ;
    ; NOTE: The main memory area from $4000.5FFF is used during the collection
    ; process. Therefore, HEAP_COLLECT should not be run during a
    ; START_LOAD..FINISH_LOAD sequence, nor when hi-res page 2 is being shown.

;------------------------------------------------------------------------------
; Convenience for writing assembly routines in PLASMA source
; Macro param: number of parameters passed from PLASMA to the asm routine
; 1. Save PLASMA's X register index to evalStk
; 2. Verify X register is in the range 0-$10
; 3. Load the *last* parameter into A=lo, Y=hi
; 4. Run the calling routine (X still points into evalStk for add'l params if needed)
; 5. Restore PLASMA's X register, and advance it over the parameter(s)
; 6. Store A=lo/Y=hi into PLASMA return value
; 7. Return to PLASMA
!macro asmPlasm nArgs {
    ldy #nArgs
    jsr _asmPlasm
}
!macro asmPlasm_bank2 nArgs {
    ldy #nArgs
    jsr _asmPlasm_bank2
}
_asmPlasm = auxLoader+3
_asmPlasm_bank2 = _asmPlasm+3

; Debug support routines (defined in core/mem.s)
_safeBell   = _asmPlasm_bank2+3
_safeCout   = _safeBell+3
_safePrbyte = _safeCout+3
_safeHome   = _safePrbyte+3
_safePrhex  = _safeHome+3
_safeRdkey  = _safePrhex+3
_writeStr   = _safeRdkey+3
_prByte     = _writeStr+3
_prSpace    = _prByte+3
_prWord     = _prSpace+3
_prA        = _prWord+3
_prX        = _prA+3
_prY        = _prX+3
_crout      = _prY+3
_waitKey    = _crout+3
_internalErr = _waitKey+3
fixedRTS    = _internalErr+3

; Debug macros
!macro safeBell {
    jsr _safeBell
}

!macro safeCout {
    jsr _safeCout
}

!macro safePrbyte {
    jsr _safePrbyte
}

!macro safeHome {
    jsr _safeHome
}

!macro safePrhex {
    jsr _safePrhex
}

!macro safeRdkey {
    jsr _safeRdkey
}

!macro prStr {
    jsr _writeStr
}

!macro prByte addr {
    jsr _prByte
    !word addr
}

!macro prSpace {
    jsr _prSpace
}

!macro prChr chr {
    jsr _writeStr
    !byte chr, 0
}

!macro prA {
    jsr _prA
    jsr _prSpace
}

!macro prX {
    jsr _prX
    jsr _prSpace
}

!macro prY {
    jsr _prY
    jsr _prSpace
}

!macro prXA {
    jsr _prX
    jsr _prA
    jsr _prSpace
}

!macro prAX {
    jsr _prA
    jsr _prX
    jsr _prSpace
}

!macro prYA {
    jsr _prY
    jsr _prA
    jsr _prSpace
}

!macro prAY {
    jsr _prA
    jsr _prY
    jsr _prSpace
}

!macro prXY {
    jsr _prX
    jsr _prY
    jsr _prSpace
}

!macro prYX {
    jsr _prY
    jsr _prX
    jsr _prSpace
}

!macro prWord addr {
    jsr _prWord
    !word addr
}

!macro crout {
    jsr _crout
}

!macro waitKey {
    jsr _waitKey
}

!macro internalErr chr {
    jsr _internalErr
    !byte chr
}
