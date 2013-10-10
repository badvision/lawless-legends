;Memory manager
;------------------
;Memory is managed in 512-byte blocks (to correspond with Prodos block sizes)
;In every 64kb memory bank, a lookup table lives at $800-$8FF and identifies
;what blocks, if any, are used there as well as usage flags to mark free,
;used, or reserved memory.
;
;Memory is marked as used as it is loaded by the loader, but the caller program
;should mark memory as free as soon as the memory is no longer in use.  It is
;very possible that the memory will not be reclaimed right away and could be
;reinstated as in-use without a loading penalty.
;
;Another scenario is that free memory will be relocated to auxiliary banks
;and potentially later restored to active memory at a later time.  Depending on the
;driver, this might be handled in different ways.  Aux memory should be kept open
;such that memory can still be reclaimed.  Extended memory (e.g. RamWorks and slinky
;ram expansions) can adopt a more linear and predictable model if they are large enough.
;
;The goal of using extended ram is to prevent future disk access as much as possible,
;because even an inefficient o(N) memory search is going to be faster than spinning up
;a disk.
;-----------------
;Page table format:
;FFFFFppp nnnnnnnn
;F = Flags
;	7 - Active/Inactive
;   6 - Locked (Cannot reclaim for any reason)
;   5 - Code (1) or Data (0)
;   4 - Primary (1) or Secondary (0)
;       Memory blocks are allocated in chunks, the first block is always primary
;       So detecting primary blocks means we can clear more than one block at a time
;   3 - Reserved
;p = Data partition (0-7)
;n = Block number (0-256)
;
*=$900
PAGE_TABLE 		= $800
READ 			equ $CA
READ_BLOCK		equ $80
ERROR_IO		equ $27
NOT_CONNECTED 	equ $28

INIT_BLOCK_TABLE
		; Set up block table, marking ZP, Stack, $800-$9FF, Hires, and Prodos system areas as locked.
		; Large memory drivers should keep blocks in their canonical order if they have 
		; enough space (>= 800kb).  Since it is only needed to reserve memory in AUX and Main memory,
		; other drivers can omit block tables if they have sufficient memory.  In these 
		; situations it might work best if large memory drivers use an alternate
		; approach to a block table as it makes sense to do so.

REQUEST_MEMORY
		; Free memory, relocating anything in the way to auxiliary ram if possible
		; The end result is that the caller gets a target area of ram to use that is supposedly
		; not in use by anything else (provided it plays nicely with others...)
		; If we have to satisfy the target block location (A > 0) then active flags are
		; ignored right away.  It is necessary for transition code to always make sure
		; that non-relocatable code segments are loaded in their required places before
		; JMPing to them.
		;
		; A = requested target block location, 0 = don't care
		;   0 is used because we will never want to load into the stack or into zero page on purpose
		;   Some loaders in the chain will let us use block 0, but that's only for AUX banks
		; Y = number of blocks to reserve
		; Returns: A = target location
		; 		    If carry set, allocation failed!

LOCK_MEMORY
		; Set memory as locked so that it cannot be reclaimed for any reason.
		; A = starting block number
		; Y = number of blocks to lock (0 or 1 both mean one block)

RELOCATE_MEMORY
		; Move blocks down the loader chain to whoever can hold them
		; The goal is to relocate blocks and avoid going back to disk if possible
		; If this goal cannot be met, then cest la vie! :-)
		; A reallocation request is reviewed block by block.  A loader can ignore
		; relocation requests for blocks they still have in memory, even if inactive
		; A = starting block to relocate
		; Y = number of blocks to relocate
		; Returns: Nothing
		
DEACTIVATE_MEMORY
		; Mark a block in memory as free, or rather inactive, so that it can be reused
		; This also clears the lock bit!   Subsequent blocks are also freed if their
		; primary bit is not set.  Callers shouldn't use this directly, you
		; should use REQUEST_MEMORY!
		; A = block number
		; Return: nothing
		
FIND_BLOCK
		; Check to see if block is in memory
		; A = partition number (0-7)
		; Y = block number
		; Returns:
		; 	   Carry clear if block found
		;	   A = block number in memory (LSB)
		;	   Y = block number in memory (MSB)
		;	   Carry set if block not found
		;	   See load block for failure status codes

RECALL_BLOCKS
		; This is the main loader stub, used to transfer a set of blocks from the most accessible
		; location to main memory.  For main memory, this is a stub to move memory if possible
		; or just delegate the work of loading down the chain to the next available device 
		; in the loader chain.
		; If the block is not found and there are no more loaders in the chain, 
		; then an error code is returned.
		; When blocks are loaded, only the first block is marked primary, others are marked
		; as secondary.
		; A = Requested target block location in memory
		; X = NNNNNPPP; 
				NNNNN = number of blocks to load (+1 added to value, so 11111 will load 32 blocks)
				PPP = Partition number (0-7)
		; Y = Starting block number (0-256)
		; Returns: Carry clear if successful
		;   If carry is set, check A for failure status:
		;       255 = I/O error
		;		  0 = partition not found (disk switch required)
		;		  1 = block not found (partition size is too small, probably needs boot disk)
		JSR MLI
		db READ_BLOCK
		db 3	; param count
		db 1	; unit num (Bit 7 = drive, Bits 6-4 = slot)
		dw 00 	; buffer location (little endian)
		dw 00	; block # (little endian)

REGISTER_LOADER
		; Register loader to the end to the loader chain.
		; So if this has a chain loader registered already, perform a JMP to that loader
		; Priority is lowest-to-highest order so things should be in this order:
		; 00: Main memory
		; 10: Slinky ram (>= 1mb -- can hold full game)
		; 20: RamWorks (>= 1mb -- can hold full game)
		; 30: Slinky ram (< 800kb -- unable to hold full game)
		; 40: RamWorks (< 800kb -- unable to hold full game)
		; 50: Aux memory
		; 60: Hard drive/800kb -- Holds full game but slow
		; 70: Disk II -- Unable to hold full game and also very slow
		; 80: Serial -- Able to hold full game but really slow and requires extra checks
		;
		; Inputs:
		; A = Priority
		; Y = MSB loader address
		; X = LSB loader address
		
		
		
		
			
	
		
		
		
		
		
