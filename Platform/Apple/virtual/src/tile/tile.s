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
;-- These variables are set in the outer draw section but can be destroyed by the inner routine
SECTION_X_START = $60   ; X Offset relative to current section being drawn 
SECTION_Y_START = $61   ; Y Offset relative to current section being drawn 
X_COUNTER       = $66   ; Loop counter used during drawing
Y_COUNTER       = $67   ; Loop counter used during drawing
Y_LOC           = $68   ; Current row being drawn (between 0 and VIEWPORT_WIDTH)
ROW_LOCATION    = $69   ; Used for pointing at row offset in map data
TILE_SOURCE     = $6D   ; Location of tile data

;----------------------------------------------------------------------
; Vectors used to call in from the outside.
	jmp INIT
	jmp DRAW
	jmp CROSS

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
;   0 Resource ID of next map section (north), FF = none
;   1 Resource ID of next map section (east), FF = none
;   2 Resource ID of next map section (south), FF = none
;   3 Resource ID of next map section (west), FF = none
;   4 Tileset resource id
;   5 Resource ID of script library (FF = none)
LOAD_SECTION
	CMP #$FF
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
; >> FINISH LOADING MAP SECTIONS
FINISH_MAP_LOAD
	LDA #FINISH_LOAD
	JMP mainLoader
!macro finishLoad keepOpen {
	LDX #keepOpen   ; 1 to keep open for next load, 0 for close so you can flip to HGR page 2
	JSR FINISH_MAP_LOAD
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
;   Each tile in memory can be 0-32, the flags are the upper 3 bits
;   0 0 0
;   | | `- Script assigned, triggers script lookup
;   | `--- Boundary (Can not walk on it)
;   `----- Visible obstruction (Can not see behind it)
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
	; Get new NW section
	LDY #NORTH
	LDA (NW_MAP_LOC),Y
	CMP #$FF
	BEQ .noMap
	TAX
	; Get new NE section
	LDA (NE_MAP_LOC),Y
	PHA
	TXA
	PHA
	+freeAllTiles
	+freeResource SW_MAP_LOC
	+freeResource SE_MAP_LOC
	LDA REL_Y
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
	+finishLoad 1   ; keep open for further loading
	+loadAllTiles
	+finishLoad 0   ; all done
	RTS
.noMap	INC REL_Y
	RTS
;----------------------------------------------------------------------
; >> CROSS EAST BOUNDARY (Load next section to the east)
!zone
CROSS_EAST
	LDA NE_MAP_ID
	CMP #$FF
	BEQ .noMap
	; Get new NE section
	LDY #EAST
	LDA (NE_MAP_LOC),Y
	CMP #$FF
	BEQ .noMap
	TAX
	; Get new SE section
	LDA (SE_MAP_LOC),Y
	PHA
	TXA
	PHA
	+freeAllTiles
	+freeResource NW_MAP_LOC
	+freeResource SW_MAP_LOC
	LDA REL_X
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
	+finishLoad 1   ; keep open for further loading
	+loadAllTiles
	+finishLoad 0   ; all done
	RTS
.noMap	DEC REL_X
	RTS
;----------------------------------------------------------------------
; >> CROSS SOUTH BOUNDARY (Load next section to the south)
!zone
CROSS_SOUTH
	LDA SW_MAP_ID
	CMP #$FF
	BEQ .noMap
	; Get new SW section
	LDY #SOUTH
	LDA (SW_MAP_LOC),Y
	CMP #$FF
	BEQ .noMap
	TAX
	; Get the new SE section
	LDA (SE_MAP_LOC),Y
	PHA
	TXA
	PHA
	+freeAllTiles
	+freeResource NW_MAP_LOC
	+freeResource NE_MAP_LOC
	LDA REL_Y
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
	+finishLoad 1   ; keep open for further loading
	+loadAllTiles
	+finishLoad 0   ; all done
	RTS
.noMap	DEC REL_Y
	RTS
;----------------------------------------------------------------------
; >> CROSS WEST BOUNDARY (load next section to the west)
!zone
CROSS_WEST
	; Get new NW section
	LDY #WEST
	LDA (NW_MAP_LOC),Y
	CMP #$FF
	BEQ .noMap
	TAX
	; Get the new SW section
	LDA (SW_MAP_LOC),Y
	PHA
	TXA
	PHA
	+freeAllTiles
	+freeResource NE_MAP_LOC
	+freeResource SE_MAP_LOC
	LDA REL_X
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
	+finishLoad 1   ; keep open for further loading
	+loadAllTiles
	+finishLoad 0   ; all done
	RTS
.noMap	INC REL_X
	RTS
;----------------------------------------------------------------------
; >> SET PLAYER TILE (A = tile)
;----------------------------------------------------------------------
; >> SET NPC TILE (A = tile, X,Y = coordinates in section)
;----------------------------------------------------------------------
; >> DRAW
!zone draw
!macro drawMapSection mapPtr, tilesetPtr, deltaX, deltaY {
    ; Determine X1 and X2 bounds for what is being drawn
	LDA REL_X
	SEC
	SBC #(deltaX+VIEWPORT_HORIZ_PAD)
	TAX
	BPL .10
	LDA #0
.10     STA SECTION_X_START
	TXA
	CLC
	ADC #VIEWPORT_WIDTH
	CMP #SECTION_WIDTH
	BMI .11
	LDA #SECTION_WIDTH
.11     
	SEC
	SBC SECTION_X_START
	STA DRAW_WIDTH
	BMI .30
    ; Determine Y1 and Y2 bounds for what is being drawn
	LDA REL_Y
	SEC
	SBC #(deltaY+VIEWPORT_VERT_PAD)
	TAX
	BPL .20
	LDA #0
.20     STA SECTION_Y_START
	TXA
	CLC
	ADC #VIEWPORT_HEIGHT
	CMP #SECTION_HEIGHT
	BMI .21
	LDA #SECTION_HEIGHT
.21
	SEC
	SBC SECTION_Y_START
	STA DRAW_HEIGHT
	BMI .30
	+move_word mapPtr, DRAW_SECTION
	+move_word tilesetPtr, TILE_BASE
	JSR MainDraw
.30
}

DRAW
; For each quadrant, display relevant parts of screen
!if DEBUG >= 2 { 
	+prStr : !text "Draw: REL_X=",0 
	+prByte REL_X
	+prStr : !text "REL_Y=",0
	+prByte REL_Y
	+crout
}
.checkNWQuad
	LDA #0
	STA DRAW_Y_START
	STA DRAW_X_START
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

	LDA DRAW_SECTION+1	; skip if no map section here
	BNE .gotMap
.noDraw
	RTS
.gotMap
	LDA DRAW_HEIGHT
	BEQ .noDraw
	BMI .noDraw
	STA Y_COUNTER
	LDA DRAW_Y_START
	BMI .noDraw
	STA Y_LOC
.rowLoop        
	LDA DRAW_WIDTH
	BEQ .noDraw
	BMI .noDraw
	STA X_COUNTER
	LDA DRAW_X_START
	BMI .noDraw
; Identify start of map data (upper left)
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

;Look up data offset (old bit shifting multiplication logic was buggy)
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
.next_col
; Get tile
	TXA
	TAY
	; show avatar in the center of the map
	CMP #VIEWPORT_HORIZ_PAD
	BNE .notAvatar
	LDA Y_LOC
	CMP #VIEWPORT_VERT_PAD
	BNE .notAvatar
	LDY GLOBAL_TILESET_LOC
	LDA GLOBAL_TILESET_LOC+1
	BNE .store_src		; always taken
.notAvatar
	LDA #0
	STA TILE_SOURCE+1
	LDA (ROW_LOCATION), Y
	BNE .not_empty		; zero means empty tile
.empty
	LDY #<emptyTile
	LDA #>emptyTile+1
	BNE .store_src		; always taken
.not_empty
	SEC
	SBC #1			; tile map is 1-based, tile set indexes are 0-based	
	; Calculate location of tile data == tile_base + ((tile & 31) * 32)
	AND #31
	ASL
	ASL
	ASL
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
	    DEC X_COUNTER
	    BEQ .next_row
	    TXA ; Outside the drawing part we need to put X back (divide by 2)
	    LSR
	    TAX
	    INX
	    JMP .next_col
; Increment row
.next_row
	DEC Y_COUNTER
	BNE .notDone
	RTS
.notDone
	INC Y_LOC
	INC SECTION_Y_START
	JMP .rowLoop
; Draw player


;----------------------------------------------------------------------
; >> INIT (reset map drawing vars, load initial map in A)
INIT
	; load the NW map section first
	STA NW_MAP_ID
	+startLoad
	LDA NW_MAP_ID
	+loadSection NW_MAP_LOC
	+finishLoad 1   ; keep open for further loading...
	+startLoad
	; from the NW section we can get the ID of the NE section
	LDY #EAST
	LDA (NW_MAP_LOC),Y
	STA NE_MAP_ID
	+loadSection NE_MAP_LOC
	; from the NW section we can get the ID of the SW section
	LDY #SOUTH
	LDA (NW_MAP_LOC),Y
	STA SW_MAP_ID
	+loadSection SW_MAP_LOC
	+finishLoad 1   ; keep open for further loading...
	+startLoad
	; if there's no SW section, there's also no SE section
	LDA #$FF
	STA SE_MAP_ID
	CMP SW_MAP_ID
	BEQ +
	; get the SE section from the SW section
	LDY #EAST
	LDA (SW_MAP_LOC),Y
	STA SE_MAP_ID
	+loadSection SE_MAP_LOC
+       +finishLoad 1   ; keep open for further loading
	+loadAllTiles
	+finishLoad 0   ; all done
	; set up the X and Y coordinates
	LDX #VIEWPORT_HORIZ_PAD
	LDY #VIEWPORT_VERT_PAD
	JSR SET_XY
	RTS

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

emptyTile !fill 32