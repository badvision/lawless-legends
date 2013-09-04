; Shared definitions for the rendering code

; Constants
TOP_LINE       = $2180 ; 24 lines down from top
NLINES         = 128
SKY_COLOR_E    = 1 ; blue
SKY_COLOR_O    = 1 ; blue
GROUND_COLOR_E = 4 ; orange
GROUND_COLOR_O = 0 ; black
TEX_SIZE       = $555 ; 32x32 + 16x16 + 8x8 + 4x4 + 2x2 + 1x1

; Byte offset for each pixel in the blit unroll
BLIT_OFF0 = 5
BLIT_OFF1 = 8
BLIT_OFF2 = 11
BLIT_OFF3 = 1
BLIT_OFF4 = 17
BLIT_OFF5 = 20
BLIT_OFF6 = 24

BLIT_STRIDE = 29

; Renderer zero page
lineCt     = $3  ; len 1
txNum      = $4  ; len 1
txColumn   = $5  ; len 1
pLine      = $6  ; len 2
pDst       = $8  ; len 2
pTex       = $A  ; len 2
pixNum     = $C  ; len 1
byteNum    = $D  ; len 1
pTmp       = $E  ; len 2
tmp        = $10 ; len 2
backBuf    = $12 ; len 1 (value 0 or 1)
frontBuf   = $13 ; len 1 (value 0 or 1)
pRayData   = $14 ; len 2
playerX    = $16 ; len 2 (hi=integer, lo=fraction)
playerY    = $18 ; len 2 (hi=integer, lo=fraction)
rayDirX    = $1A ; len 1
rayDirY    = $1B ; len 1
stepX      = $1C ; len 1
stepY      = $1D ; len 1
mapX       = $1E ; len 1
mapY       = $1F ; len 1
sideDistX  = $50 ; len 1
sideDistY  = $51 ; len 1
deltaDistX = $52 ; len 1
deltaDistY = $53 ; len 1
dist       = $54 ; len 2
diff       = $56 ; len 2
playerDir  = $58 ; len 1

; Other monitor locations
a2l      = $3E
a2h      = $3F
resetVec = $3F2

; Tables and buffers
decodeTo01   = $800
decodeTo23   = $900
decodeTo45   = $A00
decodeTo56   = $B00
decodeTo57   = $C00
clrBlitRollE = $D00 ; size 3*(128/2) = $C0, plus 2 for tya and rts
clrBlitRollO = $DC2 ; size 3*(128/2) = $C0, plus 2 for tya and rts
XF00         = $E00 ; unused

prodosBuf    = $AC00 ; temporary, before building the tables
screen       = $2000

;---------------------------------
; The following are all in aux mem...
expandVec  = $800 ; size with expandCode: $30E9
expandCode = $900
textures   = $3900
tex0       = textures
tex1       = tex0+TEX_SIZE
tex2       = tex1+TEX_SIZE
tex3       = tex2+TEX_SIZE
texEnd     = tex3+TEX_SIZE
; back to main mem
;---------------------------------

blitRoll   = $B000      ; Unrolled blitting code. Size 29*128 = $E80, plus 1 for rts
MLI        = $BF00      ; Entry point for ProDOS MLI
memMap     = $BF58      ; ProDOS memory map

; I/O locations
kbd       = $C000
clrAuxRd  = $C002
setAuxRd  = $C003
clrAuxWr  = $C004
setAuxWr  = $C005
clrAuxZP  = $C008
setAuxZP  = $C009
kbdStrobe = $C010
clrText   = $C050
setText   = $C051
clrMixed  = $C052
setMixed  = $C053
page1     = $C054
page2     = $C055
clrHires  = $C056
setHires  = $C057

; ROM routines
prntax    = $F941
rdkey     = $FD0C
getln1    = $FD6F
crout     = $FD8E
prbyte    = $FDDA
cout      = $FDED
prerr     = $FF2D
monitor   = $FF69
getnum    = $FFA7

; mipmap level offsets
MIP_OFFSET_0 = 0
MIP_OFFSET_1 = $400     ; 32*32
MIP_OFFSET_2 = $500     ; 32*32 + 16*16
MIP_OFFSET_3 = $540     ; 32*32 + 16*16 + 8*8
MIP_OFFSET_4 = $550     ; 32*32 + 16*16 + 8*8 + 4*4
MIP_OFFSET_5 = $554     ; 32*32 + 16*16 + 8*8 + 4*4 + 2*2


