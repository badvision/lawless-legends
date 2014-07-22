; Shared definitions for the rendering code

!source "../include/global.i"

; Constants
TOP_LINE	= $2180	; 24 lines down from top
NLINES		= 128
TEX_SIZE	= $555	; 32x32 + 16x16 + 8x8 + 4x4 + 2x2 + 1x1
PLASMA_FRAME_SIZE = $200

; Byte offset for each pixel in the blit unroll
BLIT_OFF0	= 5
BLIT_OFF1	= 8
BLIT_OFF2	= 11
BLIT_OFF3	= 1
BLIT_OFF4	= 17
BLIT_OFF5	= 20
BLIT_OFF6	= 24

BLIT_STRIDE	= 29

MAX_TEXTURES	= 62	; sized to fit memory space below

; Renderer zero page
tmp		= $4	; len 2
pTmp		= $6	; len 2
pDst		= $8	; len 2
pTex		= $A	; len 2
pixNum		= $C	; len 1
byteNum		= $D	; len 1
__unused0E	= $E	; len 6
pRayData	= $14	; len 2
txNum		= $16	; len 1
txColumn	= $17	; len 1
pLine		= $18	; len 2
rayDirX		= $1A	; len 1
rayDirY		= $1B	; len 1
stepX		= $1C	; len 1
stepY		= $1D	; len 1
mapX		= $1E	; len 1
mapY		= $1F	; len 1
sideDistX	= $50	; len 1
sideDistY	= $51	; len 1
deltaDistX	= $52	; len 1
deltaDistY	= $53	; len 1
dist		= $54	; len 2
diff		= $56	; len 2
pMap		= $58	; len 2
lineCt		= $5A	; len 1
depth		= $5B	; len 1
screenCol	= $5C	; len 1
playerDir	= $5D	; len 1
playerX		= $5E	; len 2 (hi=integer, lo=fraction)
playerY		= $60	; len 2 (hi=integer, lo=fraction)
mapWidth	= $62	; len 1
mapHeight	= $63	; len 1
spriteX		= $64	; len 2
spriteY		= $66	; len 2
plasmaFrames	= $68	; len 2
backBuf		= $6A	; len 1
frontBuf	= $6B	; len 1

; Sprite calculations zero page
bSgnSinT	= $90
bSgnCosT	= $91
bSgnDx		= $92
bSgnDy		= $93
bSgnRy		= $94
_unused95	= $95
wLogSinT	= $96
wLogCosT	= $98
wLogDx		= $9A
wLogDy		= $9C
wTxColBump	= $9E
wRx		= $A0
wRy		= $A2
wLogRy		= $A4
wLogSqRx	= $A6
wLogSqRy	= $A8
wSqDist		= $AA
wLogDist	= $AC
wLogSize	= $AE
wSize		= $B0
wSpriteTop	= $B2
wSpriteLeft	= $B4

;---------------------------------
; The following are in aux mem...
expandVec	= $2000
; back to main mem
;---------------------------------

; Main-mem tables and buffers
tableStart	= $A200
decodeTo01	= tableStart+$0000
decodeTo01b	= tableStart+$0100
decodeTo23	= tableStart+$0200
decodeTo23b	= tableStart+$0300
decodeTo45	= tableStart+$0400
decodeTo56	= tableStart+$0500
decodeTo57	= tableStart+$0600
clrBlitRollE	= tableStart+$0700	; size 3*(128/2) = $C0, plus 2 for tya and rts
clrBlitRollO	= tableStart+$07C2	; size 3*(128/2) = $C0, plus 2 for tya and rts
texAddrLo	= tableStart+$0884
texAddrHi	= texAddrLo + MAX_TEXTURES
blitRoll	= tableStart+$0900	; Unrolled blitting code. Size 29*128	= $E80, plus 1 for rts
tableEnd	= tableStart+$1781

; mipmap level offsets
MIP_OFFSET_0	= 0
MIP_OFFSET_1	= $400	; 32*32
MIP_OFFSET_2	= $500	; 32*32 + 16*16
MIP_OFFSET_3	= $540	; 32*32 + 16*16 + 8*8
MIP_OFFSET_4	= $550	; 32*32 + 16*16 + 8*8 + 4*4
MIP_OFFSET_5	= $554	; 32*32 + 16*16 + 8*8 + 4*4 + 2*2


