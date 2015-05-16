;@com.wudsn.ide.asm.hardware=APPLE2
; Tile routines
; ------------------
;     
 * = $6000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"
!source "../include/mem.i"
!source "../include/plasma.i"

DEBUG       = 0     ; 1=some logging, 2=lots of logging

HEADER_LENGTH=6
SECTION_WIDTH=22
SECTION_HEIGHT=23
VIEWPORT_WIDTH=9
VIEWPORT_HEIGHT=9
VIEWPORT_VERT_PAD=4 ; This is the distance between the center of the viewport and the top/bottom
VIEWPORT_HORIZ_PAD=4 ; This is the distance between the center of the viewport and the left/right

;--------------
;  0 1 2 3 4 5 6 7 8 9
;0           .\
;1           . |_ VERT PAD
;2           . |
;3           ./
;4           X . . . .
;5            \_____/
;6               |
;7              HORIZ PAD
;8
;9

MAX_MAP_ID=254  ; This means that the total map area can be as big as 5588x5842 tiles!

REL_X=$50   ; Will always be in the range 0-43
REL_Y=$51   ; Will always be in the range 0-45
; Map quadrant data pointers (Maybe move these to screen holes in 2078-207f?  There might be no advantage to using ZP for these)
NW_MAP_LOC=$52
NE_MAP_LOC=$54
SW_MAP_LOC=$56
SE_MAP_LOC=$58
NW_TILESET_LOC=$90
NE_TILESET_LOC=$92
SW_TILESET_LOC=$94
SE_TILESET_LOC=$96
GLOBAL_TILESET_LOC=$98
; Map section IDs (255 = not loaded)
NOT_LOADED=$FF
NW_MAP_ID=$5A
NE_MAP_ID=$5B   
SW_MAP_ID=$5C
SE_MAP_ID=$5D

NORTH   =0
EAST    =1
SOUTH   =2
WEST    =3

;-- Variables used in drawing which can not be changed by the inner drawing loop
DRAW_X_START    = $5E   ; Starting column being drawn (between 0 and VIEWPORT_WIDTH)
DRAW_Y_START    = $5F   ; Starting row being drawn (between 0 and VIEWPORT_WIDTH)
DRAW_WIDTH      = $62   ; Number of columns to draw for current section (cannot be destroyed by drawing loop)
DRAW_HEIGHT     = $63   ; Number of rows to draw for current section (cannot be destroyed by drawing loop)
DRAW_SECTION    = $64   ; Location of section data being drawn
TILE_BASE       = $6B   ; Location of tile data
INDEX_MAP_ID	= $6F	; ID of the first map section
;-- These variables are set in the outer draw section but can be destroyed by the inner routine
SECTION_X_START = $60   ; X Offset relative to current section being drawn 
SECTION_Y_START = $61   ; Y Offset relative to current section being drawn 
X_COUNTER       = $66   ; Loop counter used during drawing
Y_COUNTER       = $67   ; Loop counter used during drawing
Y_LOC           = $68   ; Current row being drawn (between 0 and VIEWPORT_WIDTH)
ROW_LOCATION    = $69   ; Used for pointing at row offset in map data
TILE_SOURCE     = $6D   ; Location of tile data
SCRIPTS_LOC	= $9A	; Location of script module
CALC_MODE       = $9C	; Flag to indicate calculate mode (non-zero) or normal draw mode (zero)
AVATAR_TILE	= $9D	; Tile map entry under the avatar
AVATAR_X        = $9E	; X coordinate within avatar map section
AVATAR_Y        = $9F	; Y coordinate within avatar map section
AVATAR_SECTION  = $A0	; Location of section data the avatar is within (pointer)
N_HORZ_SECT	= $A2	; Number of horizontal sections in the whole map
N_VERT_SECT	= $A3	; Number of vertical sections in the whole map
ORIGIN_X	= $A4	; 16-bit origin for X (add REL_X to get avatar's global map X)
ORIGIN_Y	= $A6	; 16-bit origin for Y (add REL_Y to get avatar's global map Y)
AVATAR_DIR	= $A8	; direction (0-15, though only 0,4,8,12 are valid)
PLASMA_X	= $A9	; save for PLASMA's X reg
next_zp		= $AA

;----------------------------------------------------------------------
; Here are the entry points for PLASMA code. Identical API for 2D and 3D.
	JMP pl_initMap 		; params: pMapData, x, y, dir
	JMP pl_flipToPage1	; params: none; return: nothing
	JMP pl_getPos		; params: @x, @y; return: nothing
	JMP pl_setPos		; params: x (0-255), y (0-255); return: nothing
	JMP pl_getDir		; params: none; return: dir (0-15)
	JMP pl_setDir		; params: dir (0-15); return: nothing
	JMP pl_advance		; params: none; return: 0 if same, 1 if new map tile, 2 if new and scripted
	JMP pl_setColor		; params: slot (0=sky/1=ground), color (0-15); return: nothing
	jmp pl_render		; params: none

; Debug support -- must come after jump vectors, since it's not just macros.
!source "../include/debug.i"

;----------------------------------------------------------------------
; >> START LOADING MAP SECTIONS
START_MAP_LOAD
	LDX #0
	LDA #START_LOAD
	JMP mainLoader
!macro startLoad {
	JSR START_MAP_LOAD
}

;----------------------------------------------------------------------
; >> LOAD MAP SECTION
;   Section number is in A
;   Returns location of loaded data (Y = hi, X = lo)
;   First 6 bytes are header information
;   0 Resource ID of next map section (north), FF = none. 
;     For first map section, records # sections wide instead.
;     Subsequent sections are numbered sequentially from first.
;   1 Resource ID of next map section (east), FF = none
;     For first map section, records # sections high instead.
;     Subsequent sections are numbered sequentially from first.
;   2 Resource ID of next map section (south), FF = none
;   3 Resource ID of next map section (west), FF = none
;   4 Tileset resource id
;   5 Resource ID of script library (FF = none)
LOAD_SECTION
	CMP #NOT_LOADED
	BNE .doLoad
	LDX #00     ; This is a bogus map section, don't load
	LDY #00
	RTS
.doLoad TAY         ; resource # in Y
!if DEBUG {
	+prStr : !text "loadSection: ",0
	+prY
}
	LDX #RES_TYPE_2D_MAP
	LDA #QUEUE_LOAD
	JSR mainLoader
!if DEBUG {
	+prStr : !text "->",0
	+prYX
	+crout
}
	RTS
!macro loadSection ptr {
	JSR LOAD_SECTION
	STX ptr 
	STY ptr+1
}

;----------------------------------------------------------------------
; >> RELEASE MAP SECTION OR TILESET
!macro freeResource ptr {
    ; --> free up unused resource
	LDX ptr
	LDY ptr+1
	BEQ +		; skip if null ptr
	LDA #FREE_MEMORY
	JSR mainLoader
+
}

;----------------------------------------------------------------------
; >> LOAD TILES
;   Load tile resource (A = Resource ID)
LOAD_TILESET
	TAY	; resource # in Y
!if DEBUG {
	+prStr : !text "loadTileset ",0
	+prY
}
	LDX #RES_TYPE_TILESET
	LDA #QUEUE_LOAD
	JSR mainLoader
!if DEBUG {
	+prStr : !text "-> ",0
	+prYX
	+crout
}
	RTS
!macro loadTileset mapId, mapData, ptr {
	LDY #0
	LDX mapId
	INX	; if map id is $FF, X is now zero
	BEQ +	; and if so, skip loading tileset
	LDY #4
	LDA (mapData),Y
	JSR LOAD_TILESET
+	STX ptr
	STY ptr+1
}
;----------------------------------------------------------------------
; >> MOVE NORTH
;   Check for boundary
;   If none, check for map boundary
;       If so, move to bottom of next map
;   If not at boundary
;       Move up one row
;       Check to see if viewport is crossing section boundary;          
;   Does new location have a script assigned?
;       execute script
;----------------------------------------------------------------------
; >> MOVE EAST
;   (same as move north, might be able to overlap functionality)
;----------------------------------------------------------------------
; >> MOVE SOUTH
;   (same as move north, might be able to overlap functionality)
;----------------------------------------------------------------------
; >> MOVE WEST
;   (same as move north, might be able to overlap functionality)
;----------------------------------------------------------------------
; >> GET TILE IN CARDINAL DIRECTION AND FLAGS 
;   (Returns Tile # in Y, Flags in A)
;   Each tile in memory can be 0-63, the flags are the upper 2 bits
;   0 0
;   | `--- Obstructed / Boundary (Can not walk on it)
;   `----- Script assigned, triggers script lookup
;----------------------------------------------------------------------
; >> SET X,Y COORDINATES FOR VIEWPORT CENTER
SET_XY
	STX REL_X
	STY REL_Y
	RTS
;----------------------------------------------------------------------
; >> TRIGGER SCRIPT AT TILE (X,Y = Coordinates in section)
;----------------------------------------------------------------------
!macro move_word from, to {
	+move_byte from, to
	+move_byte from+1, to+1
}

!macro move_byte from, to {
	LDX from
	STX to
}

FREE_ALL_TILES
	+freeResource GLOBAL_TILESET_LOC
	+freeResource NW_TILESET_LOC
	+freeResource NE_TILESET_LOC
	+freeResource SW_TILESET_LOC
	+freeResource SE_TILESET_LOC
	RTS     
!macro freeAllTiles {
	JSR FREE_ALL_TILES
}

LOAD_ALL_TILES
	; global tileset first
	LDX #RES_TYPE_TILESET
	LDY #1			; global tileset fixed at resource #1
	LDA #QUEUE_LOAD
	JSR mainLoader
	STX GLOBAL_TILESET_LOC
	STY GLOBAL_TILESET_LOC+1
	; then the set for each map section in turn
	+loadTileset NW_MAP_ID, NW_MAP_LOC, NW_TILESET_LOC
	+loadTileset NE_MAP_ID, NE_MAP_LOC, NE_TILESET_LOC
	+loadTileset SW_MAP_ID, SW_MAP_LOC, SW_TILESET_LOC
	+loadTileset SE_MAP_ID, SE_MAP_LOC, SE_TILESET_LOC
	RTS
!macro loadAllTiles {
	JSR LOAD_ALL_TILES
}

FREE_SCRIPTS
	+freeResource SCRIPTS_LOC
	RTS

!macro freeScripts {
	JSR FREE_SCRIPTS
}

!zone
LOAD_SCRIPTS
	JSR CALC		; determine which map avatar is on
	LDA AVATAR_SECTION+1	; no section? no scripts
	BEQ .none
	LDY #5
	LDA (AVATAR_SECTION),Y	; check script module ID
	BNE .got		; if any, go load it
.none	LDA #0			; else, no scripts
	STA SCRIPTS_LOC
	STA SCRIPTS_LOC+1
	RTS
.got	TAY	; resource # in Y
!if DEBUG {
	+prStr : !text "loadScripts ",0
	+prY
}
	LDX #RES_TYPE_MODULE
	LDA #QUEUE_LOAD
	JSR mainLoader
!if DEBUG {
	+prStr : !text "-> ",0
	+prYX
	+crout
}
	STX SCRIPTS_LOC
	STY SCRIPTS_LOC+1
	RTS

!macro loadScripts {
	JSR LOAD_SCRIPTS
}

!macro finishLoad keepOpen {
       LDX #keepOpen   ; 1 to keep open for next load, 0 for close so you can flip to HGR page 2
       LDA #FINISH_LOAD
       JSR mainLoader
}

FINISH_MAP_LOAD
	+finishLoad 1   	; keep open for further loading
	+loadAllTiles
	+loadScripts
	+finishLoad 0   	; all done
	LDA SCRIPTS_LOC+1	; are there scripts?
	BNE .scr		; yes, go init them
	RTS			; no, we're done
.scr	!if DEBUG { +prStr : !text "Calling script init.",0 }
	LDX PLASMA_X
        BIT setLcRW+lcBank2	; switch PLASMA runtime back in
	JSR .callit		; perform script init
        BIT setROM		; switch out PLASMA so we're ready to render
!if DEBUG { +prStr : !text "Back from script init.",0 }
	RTS
.callit	JMP (SCRIPTS_LOC)	; the init function is always first in the script module

; >> CHECK CROSSINGS
!zone
CROSS
	LDA REL_Y
	CMP #VIEWPORT_VERT_PAD
	BPL .10
	JSR CROSS_NORTH
.10	LDA REL_Y
	CMP #(2*SECTION_HEIGHT)-VIEWPORT_VERT_PAD
	BMI .20
	JSR CROSS_SOUTH
.20	LDA REL_X
	CMP #VIEWPORT_HORIZ_PAD
	BPL .30
	JSR CROSS_WEST
.30	LDA REL_X
	CMP #(2*SECTION_WIDTH)-VIEWPORT_HORIZ_PAD
	BMI .40
	JSR CROSS_EAST
.40	RTS

; >> CROSS NORTH BOUNDARY (Load next section to the north)
!zone
CROSS_NORTH
	; Do not allow advancing way past edge of map
	LDA NW_MAP_ID
	AND NE_MAP_ID
	CMP #NOT_LOADED
	BNE .ok
	INC REL_Y
	RTS
.ok	; Get new NW section
	LDX #NOT_LOADED
	LDY NW_MAP_ID
	CPY INDEX_MAP_ID	; the first map section
	BEQ .gotNW 		;	doesn't have north and west links
	CPY #NOT_LOADED
	BEQ .gotNW
	LDY #NORTH
	LDA (NW_MAP_LOC),Y
	TAX
.gotNW	LDA #NOT_LOADED
	LDY NE_MAP_ID
	CPY INDEX_MAP_ID
	BEQ .gotNE
	CPY #NOT_LOADED
	BEQ .gotNE
	LDY #NORTH
	LDA (NE_MAP_LOC),Y
.gotNE	PHA
	TXA
	PHA
	+freeAllTiles
	+freeScripts
	+freeResource SW_MAP_LOC
	+freeResource SE_MAP_LOC
	LDA ORIGIN_Y
	SEC
	SBC #SECTION_HEIGHT
	STA ORIGIN_Y
	BCS +
	DEC ORIGIN_Y+1
+	LDA REL_Y
	CLC
	ADC #SECTION_HEIGHT
	STA REL_Y
	+move_byte NW_MAP_ID, SW_MAP_ID
	+move_word NW_MAP_LOC, SW_MAP_LOC
	+move_byte NE_MAP_ID, SE_MAP_ID
	+move_word NE_MAP_LOC, SE_MAP_LOC
	; Load new NW section
	+startLoad
	PLA
	STA NW_MAP_ID
	+loadSection NW_MAP_LOC
	; Load the new NE section
	PLA
	STA NE_MAP_ID
	+loadSection NE_MAP_LOC
	JMP FINISH_MAP_LOAD
;----------------------------------------------------------------------
; >> CROSS EAST BOUNDARY (Load next section to the east)
!zone
CROSS_EAST
	; Do not allow advancing way past edge of map
	LDA NE_MAP_ID
	AND SE_MAP_ID
	CMP #NOT_LOADED
	BNE .ok
	DEC REL_X
	RTS
.ok	; Get new NE section
	LDY #EAST
	LDX NE_MAP_ID
	CPX #NOT_LOADED
	BEQ .gotNE
	LDA (NE_MAP_LOC),Y
	TAX
.gotNE	; Get new SE section
	LDA SE_MAP_ID
	CMP #NOT_LOADED
	BEQ .gotSE
	LDA (SE_MAP_LOC),Y
.gotSE	PHA
	TXA
	PHA
	+freeAllTiles
	+freeScripts
	+freeResource NW_MAP_LOC
	+freeResource SW_MAP_LOC
	; Adjust origin and relative pos
	LDA ORIGIN_X
	CLC
	ADC #SECTION_WIDTH
	STA ORIGIN_X
	BCC +
	INC ORIGIN_X+1
+	LDA REL_X
	SEC
	SBC #SECTION_WIDTH
	STA REL_X
	+move_byte NE_MAP_ID, NW_MAP_ID
	+move_word NE_MAP_LOC, NW_MAP_LOC
	+move_byte SE_MAP_ID, SW_MAP_ID
	+move_word SE_MAP_LOC, SW_MAP_LOC
	; Load new NE section
	+startLoad
	PLA
	STA NE_MAP_ID
	+loadSection NE_MAP_LOC
	; Load the new SE section
	PLA
	STA SE_MAP_ID
	+loadSection SE_MAP_LOC
	jmp FINISH_MAP_LOAD
;----------------------------------------------------------------------
; >> CROSS SOUTH BOUNDARY (Load next section to the south)
!zone
CROSS_SOUTH
	; Do not allow advancing way past edge of map
	LDA SW_MAP_ID
	AND SE_MAP_ID
	CMP #NOT_LOADED
	BNE .ok
	DEC REL_Y
	RTS
.ok	; Get new SW section
	LDY #SOUTH
	LDA SW_MAP_ID
	CMP #NOT_LOADED
	BEQ .gotSW
	LDA (SW_MAP_LOC),Y
.gotSW	TAX
	LDA SE_MAP_ID
	CMP #NOT_LOADED
	BEQ .gotSE
	LDA (SE_MAP_LOC),Y
.gotSE	PHA
	TXA
	PHA
	+freeAllTiles
	+freeScripts
	+freeResource NW_MAP_LOC
	+freeResource NE_MAP_LOC
	LDA ORIGIN_Y
	CLC
	ADC #SECTION_HEIGHT
	STA ORIGIN_Y
	BCC +
	INC ORIGIN_Y+1
+	LDA REL_Y
	SEC
	SBC #SECTION_HEIGHT
	STA REL_Y
	+move_byte SW_MAP_ID, NW_MAP_ID
	+move_word SW_MAP_LOC, NW_MAP_LOC
	+move_byte SE_MAP_ID, NE_MAP_ID
	+move_word SE_MAP_LOC, NE_MAP_LOC
	; Load new SW section
	+startLoad
	PLA
	STA SW_MAP_ID
	+loadSection SW_MAP_LOC
	; Load the new SE section
	PLA
	STA SE_MAP_ID
	+loadSection SE_MAP_LOC
	jmp FINISH_MAP_LOAD
;----------------------------------------------------------------------
; >> CROSS WEST BOUNDARY (load next section to the west)
!zone
CROSS_WEST
	; Do not allow advancing way past edge of map
	LDA NW_MAP_ID
	AND SW_MAP_ID
	CMP #NOT_LOADED
	BNE .ok
	INC REL_X
	RTS
.ok	; Get new NW section
	LDX #NOT_LOADED
	LDY NW_MAP_ID
	CPY INDEX_MAP_ID	; the first map section
	BEQ .gotNW 		;	doesn't have north and west links
	CPY #NOT_LOADED
	BEQ .gotNW
	LDY #WEST
	LDA (NE_MAP_LOC),Y
	TAX
.gotNW	LDA #NOT_LOADED
	LDY SW_MAP_ID
	CPY INDEX_MAP_ID
	BEQ .gotSW
	CPY #NOT_LOADED
	BEQ .gotSW
	LDY #WEST
	LDA (SW_MAP_LOC),Y
.gotSW	PHA
	TXA
	PHA
	+freeAllTiles
	+freeScripts
	+freeResource NE_MAP_LOC
	+freeResource SE_MAP_LOC
	; Adjust origin and relative pos
	LDA ORIGIN_X
	SEC
	SBC #SECTION_WIDTH
	STA ORIGIN_X
	BCS +
	DEC ORIGIN_X+1
+	LDA REL_X
	CLC
	ADC #SECTION_WIDTH
	STA REL_X
	+move_byte NW_MAP_ID, NE_MAP_ID
	+move_word NW_MAP_LOC, NE_MAP_LOC
	+move_byte SW_MAP_ID, SE_MAP_ID
	+move_word SW_MAP_LOC, SE_MAP_LOC
	; Load new NW section
	+startLoad
	PLA
	STA NW_MAP_ID
	+loadSection NW_MAP_LOC
	; Load the new SW section
	PLA
	STA SW_MAP_ID
	+loadSection SW_MAP_LOC
	jmp FINISH_MAP_LOAD
;----------------------------------------------------------------------
; >> SET PLAYER TILE (A = tile)
;----------------------------------------------------------------------
; >> SET NPC TILE (A = tile, X,Y = coordinates in section)
;----------------------------------------------------------------------
; >> DRAW
!zone draw
!macro drawMapSection mapPtr, tilesetPtr, deltaX, deltaY {
	+move_word mapPtr, DRAW_SECTION
	+move_word tilesetPtr, TILE_BASE
	LDX #(deltaX+VIEWPORT_HORIZ_PAD)
	LDY #(deltaY+VIEWPORT_VERT_PAD)
	JSR MainDraw
}

;----------------------------------------------------------------------
; >> pl_render
; Params: none; return: none
; Draw at the current position.
pl_render:
DRAW:	LDA #0
	BEQ +
CALC:	LDA #1
+	STA CALC_MODE
; For each quadrant, display relevant parts of screen
!if DEBUG >= 2 { 
	+prStr : !text "Draw: REL_X=",0 
	+prByte REL_X
	+prStr : !text "REL_Y=",0
	+prByte REL_Y
	+prStr : !text "CALC_MODE=",0
	+prByte CALC_MODE
	+crout
}
.checkNWQuad
	LDA #0
	STA DRAW_Y_START
	STA DRAW_X_START
	STA AVATAR_SECTION+1
	!if DEBUG >= 2 { +prStr : !text "NW quad.",0 }
	+drawMapSection NW_MAP_LOC, NW_TILESET_LOC, 0, 0
.checkNEQuad
	LDA DRAW_WIDTH
	BPL +
	LDA #0
+	STA DRAW_X_START
	!if DEBUG >= 2 { +prStr : !text "NE quad.",0 }
	+drawMapSection NE_MAP_LOC, NE_TILESET_LOC, SECTION_WIDTH, 0
.checkSWQuad
	LDA DRAW_HEIGHT
	BPL +
	LDA #0
+	STA DRAW_Y_START
	LDA #0
	STA DRAW_X_START
	!if DEBUG >= 2 { +prStr : !text "SW quad.",0 }
	+drawMapSection SW_MAP_LOC, SW_TILESET_LOC, 0, SECTION_HEIGHT
.checkSEQuad
	LDA DRAW_WIDTH
	BPL +
	LDA #0
+	STA DRAW_X_START
	!if DEBUG >= 2 { +prStr : !text "SE quad.",0 }
	+drawMapSection SE_MAP_LOC, SE_TILESET_LOC, SECTION_WIDTH, SECTION_HEIGHT
	RTS

MainDraw
;----- Tracking visible tile data -----
;There are a total of 512 screen holes in a hires page located in xx78-xx7F and xxF8-xxFF
;We only need 81 screen holes to track the 9x9 visible area.  So to do this a little translation is needed
;      78  79  7a  7b  7c  7d  7e  7f  f8 
;2000   
;2100  
;2200  
; .
; .
;2800
;
; The calculation goes like this:  Page + $78 + (Row * $100) + (Col & 7) + ((Col & 8) << 4)
; When the display is drawn, the screen hole is compared to see if there is a different tile to draw 
; and if there is not then the tile is skipped.  Otherwise the tile is drawn, etc.
;--------------------------------------

; COL_OFFSET and ROW_OFFSET specify where on the screen the whole map gets drawn.
; Adjust these so it lands at the right place in the frame image.
COL_OFFSET = 2
ROW_OFFSET = 3

	STX .subX + 1		; set up subtraction operands...
	STY .subY + 1		; ... i.e. self-modify them.
        ; sanity checks
	LDA DRAW_X_START	; skip on negative start values
	BMI .ok
	LDA DRAW_Y_START
	BMI .noDraw
	STA Y_LOC		; also set up initial row counter from Y start
	BPL .ok			; always taken
.noDraw	RTS
.ok	; Determine X1 and X2 bounds for what is being drawn
	LDA REL_X
	SEC
.subX	SBC #11			; operand gets self-modified above
	TAX
	BPL +
	LDA #0
+	STA SECTION_X_START
	TXA
	CLC
	ADC #VIEWPORT_WIDTH
	CMP #SECTION_WIDTH
	BMI +
	LDA #SECTION_WIDTH
+	SEC
	SBC SECTION_X_START
	STA DRAW_WIDTH
	BMI .noDraw		; skip if draw width is negative
	BEQ .noDraw		; ...or zero
    ; Determine Y1 and Y2 bounds for what is being drawn
	LDA REL_Y
	SEC
.subY	SBC #11			; operand gets self-modified above
	TAX
	BPL +
	LDA #0
+	STA SECTION_Y_START
	TXA
	CLC
	ADC #VIEWPORT_HEIGHT
	CMP #SECTION_HEIGHT
	BMI +
	LDA #SECTION_HEIGHT
+	SEC
	SBC SECTION_Y_START
	STA DRAW_HEIGHT
	STA Y_COUNTER
	BMI .noDraw		; skip if draw height is negative
	BEQ .noDraw		; ...or zero

!if DEBUG >= 2 {
	+prStr : !text "   DR_X_ST=",0
	+prByte DRAW_X_START
	+prStr : !text "SEC_X_ST=",0
	+prByte SECTION_X_START
	+prStr : !text "DR_W=", 0
	+prByte DRAW_WIDTH
	+crout
	+prStr : !text "  +DR_Y_ST=",0
	+prByte DRAW_Y_START
	+prStr : !text "SEC_Y_ST=",0
	+prByte SECTION_Y_START
	+prStr : !text "DR_H=", 0
	+prByte DRAW_HEIGHT
	+crout
}

.rowLoop        
	LDA DRAW_WIDTH
	STA X_COUNTER
	; Self-modifying code: Update all the STA statements in the drawTile section
	LDA Y_LOC
	ASL		; double because each tile is two rows high
	TAY
	LDA tblHGRl+ROW_OFFSET, Y
	CLC
	ADC #COL_OFFSET
	TAX
	INX
	!for store, 16 {
	    STA .drawTile+((store-1)*12)+3
	    STX .drawTile+((store-1)*12)+9
	    !if store = 8 {
		LDA tblHGRl+ROW_OFFSET+1, Y
		CLC
		ADC #COL_OFFSET
		TAX
		INX
	    }
	}
	LDA tblHGRh+ROW_OFFSET, Y
	!for store, 16 {
	    STA .drawTile+((store-1)*12)+4
	    STA .drawTile+((store-1)*12)+10
	    !if store = 8 {
		LDA tblHGRh+ROW_OFFSET+1, Y
	    } else {
		; We have to calculate the start of the next row but only if we are not already at the last row
		!if store < 16 {
		    adc #$04
		}
	    }
	}

;Look up map data offset (old bit shifting multiplication logic was buggy)
;Note that for null map (DRAW_SECTION+1 == 0) this result simply gets overridden later.
	LDA DRAW_SECTION
	LDY DRAW_SECTION + 1
	CLC
	LDX SECTION_Y_START
	ADC tblMAPl,X
	BCC +
	INY			; handle carry from prev add
+	CPX #12			; rows 0-11 are on 1st page of map, 12-22 on 2nd page
	BCC +
	INY			; go to 2nd pg
	CLC
+	ADC SECTION_X_START
	BCC +
	INY			; handle carry from prev add
+	SEC
	SBC DRAW_X_START	; because it gets added back in later by indexing with X
	BCS +
	DEY
+	STA ROW_LOCATION
	STY ROW_LOCATION + 1
	LDX DRAW_X_START        
; Display row of tiles
.draw_col
; Get tile
	TXA
	TAY
	; show avatar in the center of the map
	CMP #VIEWPORT_HORIZ_PAD
	BNE .notAvatar
	LDA Y_LOC
	CMP #VIEWPORT_VERT_PAD
	BNE .notAvatar
	LDA (ROW_LOCATION),Y
	STA AVATAR_TILE
	TYA
	SEC
	SBC DRAW_X_START
	CLC
	ADC SECTION_X_START
	STA AVATAR_X
	LDA SECTION_Y_START
	STA AVATAR_Y
!if DEBUG >= 2 {
	+prStr : !text "Avatar X=",0
	+prByte AVATAR_X
	+prStr : !text "Y=",0
	+prByte AVATAR_Y
	+crout
}
	LDA DRAW_SECTION
	STA AVATAR_SECTION
	LDA DRAW_SECTION + 1
	STA AVATAR_SECTION + 1
	LDY GLOBAL_TILESET_LOC	; first tile in global tileset is avatar
	LDA GLOBAL_TILESET_LOC+1
	BNE .store_src		; always taken
.notAvatar
	LDA DRAW_SECTION+1
	BEQ .empty		; handle null map: treat it as entirely empty
	LDA (ROW_LOCATION), Y
	BNE .not_empty		; zero means empty tile
.empty
	LDY #<emptyTile
	LDA #>emptyTile+1
	BNE .store_src		; always taken
.not_empty
	; Calculate location of tile data == tile_base + (((tile & 63) - 1) * 32)
	LDY #0
	STY TILE_SOURCE+1
	AND #63
	SEC
	SBC #1			; tile map is 1-based, tile set indexes are 0-based	
	ASL
	ASL
	ASL
	ROL TILE_SOURCE+1
	ASL
	ROL TILE_SOURCE+1
	ASL
	ROL TILE_SOURCE+1
	CLC
	ADC TILE_BASE
	TAY
	LDA TILE_SOURCE+1
	ADC TILE_BASE + 1
.store_src
	STY TILE_SOURCE
	STA TILE_SOURCE+1

	LDA CALC_MODE			; check the mode
	BEQ .doneCalculatingTileLocation ; zero is normal mode (draw)
	JMP .next_col			; non-zero is calc mode (so don't draw)

.doneCalculatingTileLocation
;   Is there a NPC there?
;     No, use map tile
;     Yes, use NPC tile
; Compare tile to last-drawn tile
; Skip if no change
; Is this the first time we are drawing this row?
;   -- It is, update the draw pointers
; If tile is different then redraw
;   -- unrolled loop for 16 rows at a time
	    LDY #$00
	    TXA ; In the drawing part, we need X=X*2
	    ASL
	    TAX
.drawTile   !for row, 16 {
		LDA (TILE_SOURCE),Y	;0
		STA $2000, X    	;2
		INY                 	;5
		LDA (TILE_SOURCE),Y 	;6
		STA $2000, X		;8
		!if row < 16 {
		    INY			;11
		}
	    }
	    TXA ; Outside the drawing part we need to put X back (divide by 2)
	    LSR
	    TAX
.next_col   DEC X_COUNTER
	    BEQ .next_row
	    INX
	    JMP .draw_col
; Increment row
.next_row
	DEC Y_COUNTER
	BNE .notDone
	RTS
.notDone
	INC Y_LOC
	INC SECTION_Y_START
	JMP .rowLoop

FinishCalc
	BRK

;----------------------------------------------------------------------
; >> pl_initMap
; params: mapNum, pMapData, x, y, dir
pl_initMap: !zone
	STX PLASMA_X	; save PLASMA's eval stack pos

        ; PLASMA code has already loaded the Northwest-most map section. Record its ID and address.
        LDA evalStkL+4,X
        STA INDEX_MAP_ID
        LDA evalStkL+3,X
        STA NW_MAP_LOC
        LDA evalStkH+3,X
        STA NW_MAP_LOC+1

	; Get the number of horizontal and vertical sections (overloaded the meaning of
	; the North and West for the first map segment only)
	LDY #NORTH
	LDA (NW_MAP_LOC),Y
	STA N_HORZ_SECT
	LDY #WEST
	LDA (NW_MAP_LOC),Y
	STA N_VERT_SECT

!if DEBUG {
	+prStr : !text "mapID=",0
	+prByte INDEX_MAP_ID
	+prStr : !text "loc=",0
	+prWord NW_MAP_LOC
	+prStr : !text "nhorz=",0
	+prByte N_HORZ_SECT
	+prStr : !text "nvert=",0
	+prByte N_VERT_SECT
	+crout
}

	; Record the facing direction for later use
	JSR pl_setDir

	; The bullk of the work is taken care of by a helper function.
	INX			; skip dir so that X/Y pos are at top of eval stack
	JMP SETPOS

;----------------------------------------------------------------------
; >> pl_setPos
; Params: X, Y
pl_setPos:
SETPOS:
	; Figure out which map sections we need to load.
	; We can temporarily use the DRAW_* variables for our work here, since
	; we're not actually drawing yet.
	LDA #<(0-SECTION_WIDTH)
	STA ORIGIN_X
	LDA #>(0-SECTION_WIDTH)
	STA ORIGIN_X+1
	LDA #<(0-SECTION_HEIGHT)
	STA ORIGIN_Y
	LDA #>(0-SECTION_HEIGHT)
	STA ORIGIN_Y+1
	LDA #0
	STA DRAW_WIDTH
	STA DRAW_HEIGHT

	LDA evalStkL+1,X	; X lo
	STA REL_X
	LDA evalStkH+1,X	; X hi
	STA X_COUNTER
	LDA evalStkL,X		; Y lo
	STA REL_Y
	LDA evalStkH,X		; Y hi
	STA Y_COUNTER

!if DEBUG {
	+prStr : !text "X=",0
	+prByte X_COUNTER
	+prByte REL_X
	+prStr : !text "Y=",0
	+prByte Y_COUNTER
	+prByte REL_Y
	+crout
}

	LDA #NOT_LOADED
	STA NW_MAP_ID
	STA NE_MAP_ID
	STA SW_MAP_ID
	LDA INDEX_MAP_ID
	STA SE_MAP_ID

	; Adjust REL_X and REL_Y because the algorithm puts the
        ; target section in SE.
	LDA REL_X
	CLC
	ADC #SECTION_WIDTH
	STA REL_X
	LDA REL_Y
	CLC
	ADC #SECTION_HEIGHT
	STA REL_Y

.testx	; First, let's to the horizontal aspect.
!if DEBUG { jsr .dbg }
	LDA X_COUNTER		; high byte of X
	BNE +			; if set, we have a long way to go
	LDA REL_X		; low byte of X
	CMP #(2*SECTION_WIDTH)-VIEWPORT_HORIZ_PAD
	BCC .testy
+	; go east young person
	!if DEBUG { +prStr : !text "Go east.", 0 }
	LDY SE_MAP_ID
	STY SW_MAP_ID
	INY
	INC DRAW_WIDTH
	LDA DRAW_WIDTH
	CMP N_HORZ_SECT
	BCC +
	LDY #NOT_LOADED
+	STY SE_MAP_ID
	; adjust origin
	LDA ORIGIN_X
	CLC
	ADC #SECTION_WIDTH
	STA ORIGIN_X
	BCC +
	INC ORIGIN_X+1
+	; adjust X
	LDA REL_X
	SEC
	SBC #SECTION_WIDTH
	STA REL_X
	BCS .testx
	DEC X_COUNTER
	BCC .testx		; always taken

.testy	; Now let's do the vertical aspect.
!if DEBUG { jsr .dbg }
-	LDA Y_COUNTER		; high byte of Y
	BNE +			; if set, we have a long way to go
	LDA REL_Y		; low byte of Y
	CMP #(2*SECTION_HEIGHT)-VIEWPORT_VERT_PAD
	BCC .load
+	; go south young person
	!if DEBUG { +prStr : !text "Go south.", 0 }
	LDA SW_MAP_ID
	STA NW_MAP_ID
	CMP #NOT_LOADED
	BEQ +
	CLC
	ADC N_HORZ_SECT
	INC DRAW_HEIGHT
	LDY DRAW_HEIGHT
	CPY N_VERT_SECT
	BCC +
	LDA #NOT_LOADED
+	STA SW_MAP_ID
	LDA SE_MAP_ID
	STA NE_MAP_ID
	CMP #NOT_LOADED
	BEQ +
	CLC
	ADC N_HORZ_SECT
	CPY N_VERT_SECT
	BCC +
	LDA #NOT_LOADED
+	STA SE_MAP_ID
	; adjust origin
	LDA ORIGIN_Y
	CLC
	ADC #SECTION_HEIGHT
	STA ORIGIN_Y
	BCC +
	INC ORIGIN_Y+1
+	; adjust Y
	LDA REL_Y
	SEC
	SBC #SECTION_HEIGHT
	STA REL_Y
	BCS .testy
	DEC Y_COUNTER
	BCC .testy		; always taken

.load	
!if 0 {
	; Adjust REL_X and REL_Y because the algorithm above puts the
        ; target section in SE.
	LDA REL_X
	CLC
	ADC #SECTION_WIDTH
	STA REL_X
	LDA REL_Y
	CLC
	ADC #SECTION_HEIGHT
	STA REL_Y
}
	; At this point, all sections are correct, 
	; and REL_X, REL_Y, ORIGIN_X and ORIGIN_Y are set.
	; Time to load the map segments.
	+startLoad
	LDA NW_MAP_ID
	+loadSection NW_MAP_LOC
	LDA NE_MAP_ID
	+loadSection NE_MAP_LOC
	LDA SW_MAP_ID
	+loadSection SW_MAP_LOC
	LDA SE_MAP_ID
	+loadSection SE_MAP_LOC
	JSR FINISH_MAP_LOAD

	; And finally, render the first frame.
	JSR DRAW
!if DEBUG { JSR .dbg }
	RTS

!if DEBUG {
.dbg	+prStr : !text "NW=",0 : +prByte NW_MAP_ID
	+prStr : !text "NE=",0 : +prByte NE_MAP_ID
	+prStr : !text "SW=",0 : +prByte SW_MAP_ID
	+prStr : !text "SE=",0 : +prByte SE_MAP_ID
	+crout
	+prStr : !text "O_X=",0 : +prWord ORIGIN_X
	+prStr : !text "R_X=",0 : +prByte REL_X
	+prStr : !text "O_Y=",0 : +prWord ORIGIN_Y
	+prStr : !text "R_Y=",0 : +prByte REL_Y
	+crout
	rts
}

;----------------------------------------------------------------------
; >> pl_flipToPage1
; No-op, because in 2D we don't use hi-res page 2
pl_flipToPage1:
	rts

;----------------------------------------------------------------------
; >> pl_setColor
; No-op, because in 2D we don't have sky and ground colors
pl_setColor:
	rts

;----------------------------------------------------------------------
; >> pl_getPos
; Params: @X, @Y
pl_getPos: !zone {
!if DEBUG {
	+prStr : !text "O_X=",0 : +prWord ORIGIN_X
	+prStr : !text "R_X=",0 : +prByte REL_X
	+prStr : !text "O_Y=",0 : +prWord ORIGIN_Y
	+prStr : !text "R_Y=",0 : +prByte REL_Y
	+crout
}
	LDA ORIGIN_Y
	CLC
	ADC REL_Y
	JSR .sto
	LDA ORIGIN_Y+1
	ADC #0
	JSR .sto2
	INX
	LDA ORIGIN_X
	CLC
	ADC REL_X
	JSR .sto
	LDA ORIGIN_X+1
	ADC #0
	JMP .sto2
.sto	LDY evalStkL,X		; lo byte of address
	STY .sto2+1
	LDY evalStkH,X		; hi byte of address
	STY .sto2+2
	LDY #0
.sto2	STA $1000,Y		; self-modified above
	INY
	RTS
}

;----------------------------------------------------------------------
; >> pl_getDir
; Params: None
pl_getDir:
	LDA AVATAR_DIR		; take our 0..3
	ASL			; 	and translate
	ASL			;		to 0..15
	LDY #0
	RTS

;----------------------------------------------------------------------
; >> pl_setDir
; Params: dir
pl_setDir:
	LDA evalStkL,X		; take input 0..15
	LSR			; 	and translate
	LSR			;		to our 0..3
	STA AVATAR_DIR
	RTS

;----------------------------------------------------------------------
INNER_ADVANCE: !zone {
        LDA AVATAR_DIR
        CMP #NORTH
        BNE +
        LDA ORIGIN_Y		; if at the very top of all map segs, don't move
        CLC
        ADC REL_Y
        STA .or1+1
        LDA ORIGIN_Y+1
        ADC #0
.or1    ORA #$11		; self-modified above
        BEQ .skip
        DEC REL_Y
        JMP .check

+	CMP #EAST
	BNE +
	INC REL_X
	BNE .check		; always taken

+	CMP #SOUTH
	BNE +
	INC REL_Y
	BNE .check		; always taken

+	CMP #WEST
	BNE +
        LDA ORIGIN_X		; if at the very left of all map segs, don't move
        CLC
        ADC REL_X
        STA .or2+1
        LDA ORIGIN_X+1
        ADC #0
.or2    ORA #$11		; self-modified above
	BEQ .skip
	DEC REL_X
	JMP .check

+	BRK			; if it's not 1 of the 4 dirs, what could it be?

.check	JMP CROSS		; possibly load new map segments
.skip	RTS
}

;----------------------------------------------------------------------
; >> pl_advance
; Params: none; return: 0 if blocked, 1 if same, 2 if new map tile, 3 if new and scripted
; Advance in the current direction
pl_advance: !zone {
	STX PLASMA_X		; save PLASMA eval stk pos

        LDA REL_X		; save X
        PHA
        LDA REL_Y		; and save Y
        PHA			; 	for later comparison

        JSR INNER_ADVANCE

        JSR CALC
	LDA AVATAR_TILE		; get tile flags
	BPL +			; no hi bit = not obstructed

	; Player moved to an obstructed place. Undo!
	LDA AVATAR_DIR
	PHA
	CLC
	ADC #2			; North <-> South... East <-> West
	AND #3
	STA AVATAR_DIR
	JSR INNER_ADVANCE	; move back the opposite dir
	JSR CALC
	PLA
	STA AVATAR_DIR

+	LDY #0			; default return: didn't move
	PLA
	EOR REL_Y
	STA .or+1
	PLA
	EOR REL_X
.or	ORA #11			; self-modified above
	BEQ .ret
	LDY #2			; moved, so return at least 2.
	LDA AVATAR_TILE
	AND #$40		; check script flag
	BEQ .ret
	INY			; moved and also new place is scripted, return 3.
.ret	TYA
	LDY #0			; hi byte of return always zero
	RTS
}

tblHGRl     
	!byte   $00,$80,$00,$80,$00,$80,$00,$80
	!byte   $28,$A8,$28,$A8,$28,$A8,$28,$A8
	!byte   $50,$D0,$50,$D0,$50,$D0,$50,$D0

tblHGRh 
	!byte   $20,$20,$21,$21,$22,$22,$23,$23
	!byte   $20,$20,$21,$21,$22,$22,$23,$23
	!byte   $20,$20,$21,$21,$22,$22,$23,$23

tblMAPl	!for row, 23 {
		!byte <((row-1)*22)+6
	}

emptyTile: !fill 32
