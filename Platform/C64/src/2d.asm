
RASTER_TOP_FRAME=170
REGISTER_01=%00110101 ;#$35 (53)

mapPosX = $0a ;and $0b
mapPosY = $0c ;and $0d
mapSizeX = $0e ;and $0f
mapSizeY = $10 ;$and 11

tile = $12
x = $13
y = $14

tileBitmapPointer_LO = $15
tileBitmapPointer_HI = $16
mapBitmapPointer_LO = $17
mapBitmapPointer_HI = $18
mapBitmapPointer2_LO = $19
mapBitmapPointer2_HI = $1a
screenVideoRamPointer_LO = $1b
screenVideoRamPointer_HI = $1c
colorRamPointer_LO = $1d
colorRamPointer_HI = $1e

posX = $1f ;Sprite position X
posY = $20 ;Sprite position Y

mapSizeXAdc = $21 ;and $22
mapPointer_LO = $23
mapPointer_HI = $24

mapPosXMax = $25 ;and $26
mapPosYMax = $27 ;and $28

movementBuffer = $29

textCursor = $2a

calculateDiffer_LO = $2b
calculateDiffer_HI = $2c

locationX = $2d ;and $2e
locationY = $2f ;and $30

textPointerData_LO = $31
textPointerData_HI = $32

mapOffsetPointer_LO = $33
mapOffsetPointer_HI = $34

playerPosition_LO = $35
playerPosition_HI = $36

outputCursor = $37
wordCursor = $38
ignoreNextCharacterFlag = $39

BANK_START_ADDR=$4000 ;$4000 - $7FFF
SCREEN=$6000 ;Bitmap range: $6000 - $7F3F
SCREEN_VIDEO_RAM=$5C00 ;Screen Video RAM range: $5C00 - $5FFF
SCREEN_COLOR_RAM=$d800 ;Screen Color RAM range: $d800 - $dbe7

;View Port: size of 18 x 18 characters
VIEWPORT_BITMAP_OFFSET=320*3+16
VIEWPORT_SCREEN_OFFSET=40*3+2

;Text Port: size of 16 x 14 characters
TEXTPORT_BITMAP_OFFSET=320*3+16+8*20
TEXTPORT_SCREEN_OFFSET=40*3+2+20

SPRITE_POINTER=SCREEN_VIDEO_RAM+$03F8
SPRITES=$5b80
SPRITE_BASE=(SPRITES - BANK_START_ADDR) / 64 ;calculate where first pointer to sprite bank is located

TILES=$8000 ;Max: 64 tiles, each tile contains 4 quadrates (each quadrates 8x8 pixel size)
TILES_VIDEO_RAM=TILES+64*4*8
TILES_COLOR_RAM=TILES_VIDEO_RAM+64*4

FONTS=$9000

MAP_DATA=$3000

MSX=$A000 ;somewhere around $A000 - $BFFF we will go with music if any (8k free for zaks)

MOVEMENT_DELAY = #6

	*=$0801
	.word ss,10
	.null $9e,^start
ss	.word 0
start

sei ;Disable IRQ's

lda #$7f ;Disable CIA IRQ's
sta $dc0d ;Disable timer interrupts
sta $dd0d

lda #REGISTER_01 ;Bank out kernal and basic ($e000-$ffff)
sta $01
	
lda #<Top_Frame_IRQ  ;Install RASTER IRQ
ldx #>Top_Frame_IRQ  ;into Hardware
sta $fffe   ;Interrupt Vector
stx $ffff

lda #$01
sta $d01a ;Enable RASTER IRQs

lda #RASTER_TOP_FRAME ;IRQ on line RASTER_TOP_FRAME
sta $d012

asl $d019 ;acknowledge video interrupts
bit $dc0d ;acknowledge CIA interrupts
bit $dd0d

lda #0
sta $d020
sta $d021

sta mapPosX
sta mapPosX+1
sta mapPosY
sta mapPosY+1
sta mapSizeXAdc
sta mapSizeXAdc+1

sta movementBuffer

lda #4
sta posX
sta posY

;Get map size
lda MAP_DATA
sta mapSizeX
lda MAP_DATA+1
sta mapSizeX+1

lda MAP_DATA+2
sta mapSizeY
lda MAP_DATA+3
sta mapSizeY+1

;Get map boundaries
lda MAP_DATA ;first for x
sec
sbc #8
sta mapPosXMax
lda MAP_DATA+1
sec
sbc #0
sta mapPosXMax+1

lda MAP_DATA+2 ;then for y
sec
sbc #8
sta mapPosYMax
lda MAP_DATA+3
sec
sbc #0
sta mapPosYMax+1

;Set length of a single row
clc
lda mapSizeX
adc #1
sta mapSizeXAdc
lda mapSizeX+1
adc #0
sta mapSizeXAdc+1

;Clear View Port Screen
lda #0
ldx #160
clearViewPortLoop
	.for part=0, part<=49, part=part+1
		sta SCREEN + part*160-1,x
	.next
dex

beq +
jmp clearViewPortLoop
+

;Clear Text Port Screen
lda #1

ldy #16

-
	.for part=0, part<=13, part=part+1
		sta SCREEN_VIDEO_RAM + TEXTPORT_SCREEN_OFFSET -1 + part*40,y
		sta SCREEN_COLOR_RAM + TEXTPORT_SCREEN_OFFSET -1 + part*40,y
	.next
	dey
	bne -
	
	
;Set Screen Bank
lda #$3b
sta $d011 ;Enable Bitmap Mode

lda #$18
sta $d016 ;Enable Multicolor Mode

;Set Bank #1 (from $4000 - $7FFF)
lda #%00000010
sta $dd00

;Set Video Memory for $2C00 offset ($5C00 - $5FFF for bits 4-7 = %0111) and second half of bank for bitmap (bits 1-3 = %100)
lda #%01111000
sta $d018

cli ;Allow IRQ's

;Draw a map
jsr drawMap

;Set sprites
lda #SPRITE_BASE+0
sta SPRITE_POINTER+1

lda #SPRITE_BASE+1
sta SPRITE_POINTER+0

;Enable sprites
lda #1+2
sta $d015

;Set multicolor mode for sprites
lda $d01c
ora #$FF
sta $d01c

;Set sprites colours
lda #8
sta $d027+0

lda #0
sta $d027+1

;Set global multi colours for sprites
lda #3
sta $d025
lda #1
sta $d026

;Set sprites hi byte positions
lda #%00000000
sta $d010

;no graphic overlay on sprites
lda #%00000000
sta $d01b

;Set player on the screen
jsr setPlayer
jsr searchText

;Infinity Loop!
jmp *

;IRQs
.include "includes/irqs/topFrameIRQ.asm"

;Includes
.include "includes/drawMap.asm"
.include "includes/drawTile.asm"
.include "includes/drawPlayer.asm"
.include "includes/searchText.asm"
.include "includes/drawText.asm"
.include "includes/checkTerrain.asm"

;Tables
.include "includes/tables/drawTileTables.asm"
.include "includes/tables/playerPositionTables.asm"
.include "includes/tables/textTables.asm"

*=TILES
.binary "binary/bitmap.PRG",2,64*4*8

*=TILES_VIDEO_RAM
.binary "binary/videomem.PRG",2,64*4

*=TILES_COLOR_RAM
.binary "binary/colormem.PRG",2,64*4

*=FONTS
.binary "binary/fonts.map",0,8*8*130


*=MAP_DATA
;.binary "binary/map.bin" ;TODO: As we have no map file yet, let's use fake map for test purposes
.include "binary/map.asm"

*=SPRITES
.binary "binary/sprites.raw",0,64*2 ;only two sprites!

Raster_Indicator
	.byte 0
Raster_Indicator_Key_Pressed
	.byte 0
.byte $FF