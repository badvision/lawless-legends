
codeBeg = *

    .pc02 ; Enable 65c02 ops

; This code is written bottom-up. That is,
; simple routines first, then routines that
; call those to build complexity. The main
; code is at the very end. We jump to it now.
    jmp test

; Conditional assembly flags
DOUBLE_BUFFER = 0 ; whether to double-buffer
DEBUG = 0 ; turn on verbose logging

; Constants
TOP_LINE = $2180 ; 24 lines down from top
NLINES = 126
SKY_COLOR = $11 ; blue
GROUND_COLOR = $2 ; orange / black
TEX_SIZE = 5460

; My zero page
lineCt = $3 ; len 1
bump = $4 ; len 1
txColNum = $5 ; len 1
pLine = $6 ; len 2
pDst = $8 ; len 2
pTex = $A ; len 2
pBump = $C ; len 2
pixNum = $E ; len 1
byteNum = $F ; len 1
pTmp = $10 ; len 2
bacKBuf = $12 ; len 1 (value 0 or 1)
frontBuf = $13 ; len 1 (value 0 or 1)
pCast = $14 ; len 2

; Other monitor locations
resetVec = $3F2

; Place to stick ProDOS names temporarily
nameBuf = $280

; Tables and buffers
shiftA01 = $1000
shiftA23 = $1100
shiftA45 = $1200
shiftA56 = $1300
shiftA57 = $1400
shiftB01 = $1500
shiftB23 = $1600
shiftB45 = $1700
shiftB56 = $1800
shiftB57 = $1900
blitIndexLo = $1A00 ; size $80
blitIndexHi = $1A80 ; size $80
dcmIndexLo = $1B00 ; size $40 (one entry per two lines)
dcmIndexHi = $1B40 ; size $40
X1B80 = $1B80 ; unused
decimRoll = $1C00 ; size 11*(126/2) = 693 = $2B5, plus 1 for rts
clrBlitRoll = $1F00 ; size 3*(126/2) = 189 = $BD, plus 2 for tya & rts

prodosBuf = $1000 ; temporary, before tbls built
screen = $2000

textures = $4000 ; size $5550 (5460 bytes x 4 textures)
tex0 = textures
tex1 = tex0+TEX_SIZE
tex2 = tex1+TEX_SIZE
tex3 = tex2+TEX_SIZE
UN9550 = $9550 ; unused
blitRoll = $A000 ; size 29*126 = 3654 = $E80, plus 1 for rts
bumps = $AF00 ; len 64*64 = $1000
globalPage = $BF00 ; ProDOS global page
MLI = globalPage ; also the call point for ProDOS MLI
memMap = $BF58

; I/O locations
kbd = $C000
clrAuxRd = $C002
setAuxRd = $C003
clrAuxWr = $C004
setAuxWr = $C005
clrAuxZP = $C008
setAuxZP = $C009
kbdStrobe = $C010
clrText = $C050
setText = $C051
clrMixed = $C052
setMixed = $C053
page1 = $C054
page2 = $C055
clrHires = $C056
setHires = $C057

; ROM routines
prntAX = $F941
rdKey = $FD0C
crout = $FD8E
prByte = $FDDA
cout = $FDED
prErr = $FF2D
monitor = $FF69

; Pixel offsets for even and odd blit lines
blitOffsetEven: .byte 5,8,11,1,17,20,24
blitOffsetOdd: .byte 34,37,40,30,46,49,53
; texture addresses
texAddrLo: .byte <tex0,<tex1,<tex2,<tex3
texAddrHi: .byte >tex0,>tex1,>tex2,>tex3
    ; mip level offsets
mipOffsetLo: .byte <0,<4096,<5120,<5376,<5440,<5456,<5460
mipOffsetHi: .byte >0,>4096,>5120,>5376,>5440,>5456,>5460

nextLine:
    lda pLine+1 ; Hi byte of line
    clc
    adc #4 ; Next line is 1K up
    tax
    eor pLine+1
    and #$20 ; Past end of screen?
    beq @done ; If not, we're done
    txa
    sec
    sbc #$20 ; Back to start
    tax
    lda pLine ; Lo byte
    clc
    adc #$80 ; Inner blks offset by 128 bytes
    sta pLine
    bcc @done
    inx ; Next page
    txa
    and #7
    cmp #4 ; Still inside inner blk?
    bne @done ; If so we're done
    txa
    sec
    sbc #4 ; Back to start of inner blk
    tax
    lda pLine
    clc
    adc #$28 ; Outer blks offset by 40 bytes
    sta pLine
@done:
    stx pLine+1
    rts

; Template for blitting code

blitTemplate: ; comments show byte offset
; even rows
    lda shiftA57 ;  0: pixel 3
    asl ;  3: save half of pix 3 in carry
    ora shiftA01 ;  4: pixel 0
    ora shiftA23 ;  7: pixel 1
    ora shiftA45 ; 10: pixel 2
    sta (0),Y ; 13: even column
    iny ; 15: prep for odd
    lda shiftA01 ; 16: pixel 4
    ora shiftA23 ; 19: pixel 5
    rol ; 22: recover half of pix 3
    ora shiftA56 ; 23: pixel 6 - after rol to ensure right hi bit
    sta (0),Y ; 26: odd column
    dey ; 28: prep for even
; odd rows
    lda shiftB57 ; 29: pixel 3
    asl ; 32: save half of pix 3 in carry
    ora shiftB01 ; 33: pixel 0
    ora shiftB23 ; 36: pixel 1
    ora shiftB45 ; 39: pixel 2
    sta (2),Y ; 42: even column
    iny ; 44: prep for odd
    lda shiftB01 ; 45: pixel 4
    ora shiftB23 ; 48: pixel 5
    rol ; 51: recover half of pix 3
    ora shiftB56 ; 52: pixel 6 - after rol to ensure right hi bit
    sta (2),Y ; 55: odd column
    dey ; 57: prep for even
    ; 58: total

; Create the unrolled blit code
makeBlit:
    lda #0 ; Start with line zero
    sta lineCt
    lda #<TOP_LINE ; Begin with the first screen line
    sta pLine
    lda #>TOP_LINE
    sta pLine+1
    lda #<blitRoll ; Store to blit unroll code buf
    sta pDst
    lda #>blitRoll
    sta pDst+1
@lineLup:
; Copy the template
    ldy #57
@copy:
    lda blitTemplate,Y
    sta (pDst),Y
    dey
    bpl @copy
     ; Record the address for the even line
    jsr @storeIndex
; Set the even line pointers
    ldy #14
    jsr @storeLine
    ldy #27
    jsr @storeLine
    ; Get ready for odd line
    jsr @advance
    ; Record the address for the odd line
    jsr @storeIndex
; Set the odd line pointers
    ldy #14
    jsr @storeLine
    ldy #27
    jsr @storeLine
    ; Prepare for next iteration
    jsr @advance
; Loop until all lines are done
    lda lineCt
    cmp #NLINES
    bne @lineLup
    jsr @storeIndex ; Last addr to index
    jmp storeRTS ; Finish with RTS for cleanliness
@storeLine: ; Subroutine to store pLine to pDst
    lda lineCt
    asl
    sta (pDst),Y
    rts
@storeIndex: ; Subroutine to store tbl ptr to index
    ldy lineCt
    lda pDst
    sta blitIndexLo,Y
    lda pDst+1
    sta blitIndexHi,Y
    rts
@advance: ; Subroutine to go to next unroll
    lda #29
    jsr advPDst
    inc lineCt
    jmp nextLine

; Create code to clear the blit
makeClrBlit:
    ldx #0
    ldy #0
@lup:
    lda @st
    sta clrBlitRoll,X
    inx
    lda blitIndexLo,Y
    sta clrBlitRoll,X
    inx
    lda blitIndexHi,Y
@st:
    sta clrBlitRoll,X
    inx
    iny
    iny
    cpy #64
    bne @noSwitch
    lda @tya ; switch from sky color to ground color
    sta clrBlitRoll,X
    inx
@noSwitch:
    cpy #NLINES
    bne @lup
    lda @rts
    sta clrBlitRoll,X
@rts:
    rts
@tya:
    tya

; Clear the blit
clearBlit:
    ldy #GROUND_COLOR
clearBlit2:
    ldx blitOffsetEven+0
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+1
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+2
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+3
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+4
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+5
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetEven+6
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+0
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+1
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+2
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+3
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+4
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+5
    lda #SKY_COLOR
    jsr clrBlitRoll
    ldx blitOffsetOdd+6
    lda #SKY_COLOR
    jmp clrBlitRoll

; Construct the shift tables
makeShift:
    ldx #0
@shiftA:
    txa
    and #3
@shiftA01:
    sta shiftA01,X
@shiftA23:
    asl
    asl
    sta shiftA23,X
@shiftA45:
    asl
    asl
    ora #$80
    sta shiftA45,X
@shiftA56:
    asl
    ora #$80
    sta shiftA56,X
@shiftA57:
    asl
    asl
    php
    lsr
    plp
    ror
    sta shiftA57,X
@shiftB:
    txa
    lsr
    lsr
    lsr
    lsr
    and #3
@shiftB01:
    ora #$80
    sta shiftB01,X
@shiftB23:
    asl
    asl
    ora #$80
    sta shiftB23,X
@shiftB45:
    asl
    asl
    ora #$80
    sta shiftB45,X
@shiftB56:
    asl
    ora #$80
    sta shiftB56,X
@shiftB57:
    asl
    asl
    php
    lsr
    plp
    ror
    sta shiftB57,X
@next:
    inx
    bne @shiftA
    rts

; Template for decimation. Offsets in comments
decimTemplate:
    lda (pTex),Y ; 0
    sta blitRoll,X ; 2
    sta blitRoll+29,X ; 5
    lda (pBump),Y ; 8
    tay ; 10
    ; 11

; Unroll the decimation code
makeDCM:
    ldx #0 ; Line counter
    lda #<decimRoll
    sta pDst
    lda #>decimRoll
    sta pDst+1
@oneSet:
; Save address to the index
    jsr @storeIndex
    ldy #11 ; Copy the template
@copySet: 
    lda decimTemplate,Y
    sta (pDst),Y
    dey
    bpl @copySet
    ldy #3
    jsr @storeBlit
    ldy #6
    jsr @storeBlit
    lda #11
    jsr advPDst
@more:
    ; Loop until all lines done
    cpx #NLINES
    bcc @oneSet
    jsr @storeIndex ; Last addr to index
    jmp storeRTS ; Finish with an RTS for cleanliness
@storeBlit: ; Store current blit addr
    lda blitIndexLo,X
    sta (pDst),Y
    iny
    lda blitIndexHi,X
    sta (pDst),Y
    inx ; Next line
    rts
@storeIndex:
    txa
    lsr ; One entry per two lines
    tay
    lda pDst
    sta dcmIndexLo,Y
    lda pDst+1
    sta dcmIndexHi,Y
    rts

storeRTS:
    lda #$60 ; Store an rts at pDst
    ldy #0
    sta (pDst),Y
    rts
advPDst: ; Add A to PDST
    clc
    adc pDst
    sta pDst
    bcc @rts
    inc pDst+1
@rts:
    rts

; Clear all the memory we're going to fill
clearMem:
    ldx #$10
    lda #$BE
    jmp clearScreen2

; Clear the screens
clearScreen:
    ldx #>screen
    .if DOUBLE_BUFFER
    lda #>screen + $40 ; both hi-res screens
    .else
    lda #>screen + $20 ; one hi-res screen
    .endif
clearScreen2:
    sta @limit+1
    ldy #0
    sty pDst
    tya
@outer:
    stx pDst+1
@inner:
    sta (pDst),Y
    iny
    bne @inner
    inx
@limit:
    cpx #>screen + $20
    bne @outer
    rts

; Make a simple texture with alternating colors.
; Input: Y = tex num
;        A, X: color numbers, 0-3
simpleTexture:
    sta @load1+1
    txa
    asl
    asl
    asl
    asl
    sta @load2+1
    lda texAddrLo,Y
    sta pDst
    lda texAddrHi,Y
    sta pDst+1
    ldx #>TEX_SIZE
    ldy #0
    sty @limit+1
@outer:
@load1:
    lda #0
@load2:
    ora #0
@lup:
    sta (pDst),Y
    iny
@limit:
    cpy #0
    bne @lup
    inc pDst+1
    dex
    bmi @done
    bne @outer
    lda #<TEX_SIZE ; partial last page
    sta @limit+1
    jmp @outer
@done: rts

; Generate the table of "bumps" for decimation
makeBumps:
    lda #63 ; 126/2 bump lists
    sta lineCt
    lda #<bumps
    sta pDst
    lda #>bumps
    sta pDst+1
    lda #0
    sta @ratioLo
    lda #1
    sta @ratioHi
; Goal is to make ratio = 63 divided by targetSize.
; The calculation is cool & weird, but I verified
; in Python that the logic actually works. You
; start with hi=1, lo=0. To calculate the next
; step, add hi to low and take the sum mod the next
; target size. To use the ratio, increment by hi
; and lo. Whenever the low byte goes beyond the
; target size, add an extra to hi.
@onePass:
    lda lineCt ; Init A with the lo byte = target size
    lsr ; ...div 2
    ldx #0 ; Hi byte always starts at zero
    ldy #0 ; Location to store at
@bumpLup:
    clc  ; increment lo byte by ratio
    adc @ratioLo
    cmp lineCt ; if we wrap around, need extra hi-byte bump
    bcc @noBump
    sec
    sbc lineCt
    inx
@noBump:
    pha ; save lo byte
    txa ; now work on hi byte
    clc
    adc @ratioHi
    tax
    sta (pDst),Y ; store to the table
    tay ; next loc to store
    cpx #63 ; check for end of column
    pla ; get lo byte back
    bcc @bumpLup ; loop until whole column is done
    lda #64
    jsr advPDst ; advance dst to next column
@next:
    dec lineCt ; all columns complete?
    beq @done
    lda @ratioLo ; next ratio calculation (see explanation above)
    clc
    adc @ratioHi
@modLup:
    cmp lineCt
    bcc @noMod
    inc @ratioHi
    sec
    sbc lineCt
    bne @modLup ; this must indeed be a loop
@noMod:
    sta @ratioLo
    jmp @onePass ; next column
@done:
    rts
@ratioLo: .byte 0
@ratioHi: .byte 0

; Decimate a column of the texture
; Input: Y - texture number
;        txColNum - src column num in the texture
;        pixNum - dst pixel num in the blit roll
;        lineCt - height to render, in dbl lines
; The output will be vertically centered.
decimateCol:
    ; if height is zero, render nothing
    lda lineCt
    bne @notZero
    rts
@notZero:
    ; determine mip level in X reg
    ldx #0
    lda lineCt
    sta @adjustedHeight
    lda txColNum
    sta @adjustedCol
    lda #32
@mipLup:
    cmp lineCt
    bcc @gotMip
    inx
    asl @adjustedHeight
    lsr @adjustedCol
    lsr
    cmp #2
    bcs @mipLup
@gotMip:
    .if DEBUG
    lda #"t"
    jsr cout
    tya
    jsr prByte
    lda #" "
    jsr cout

    lda #"h"
    jsr cout
    lda lineCt
    jsr prByte
    lda #" "
    jsr cout

    lda #"m"
    jsr cout
    txa
    jsr prByte
    lda #" "
    jsr cout
    .endif

    ; calc addr of tex
    lda texAddrLo,Y
    clc
    adc mipOffsetLo,X
    sta pDst
    lda texAddrHi,Y
    adc mipOffsetHi,X
    sta pDst+1

    .if DEBUG
    lda #"a"
    jsr cout
    lda pDst+1
    jsr prByte
    lda pDst
    jsr prByte
    lda #" "
    jsr cout
    .endif

@calcOffset: ; calc offset within tex
    lda #0
    sta pTex+1
    lda @adjustedCol
@shift:
    asl
    rol pTex+1
    inx ; Note: destroys mip level
    cpx #6
    bne @shift

    .if DEBUG
    pha
    lda #"x"
    jsr cout
    lda @adjustedCol
    jsr prByte
    lda #" "
    jsr cout

    lda #"o"
    jsr cout
    lda pTex+1
    jsr prByte
    pla
    pha
    jsr prByte
    lda #" "
    jsr cout
    pla
    .endif

    clc
    adc pDst
    sta pTex
    lda pTex+1
    adc pDst+1
    sta pTex+1
; calculate bump table ptr
    ldx @adjustedHeight
    jsr calcBump
    ; figure first line in decim unroll
    lda #63
    sec
    sbc lineCt ; height 63 is first in decim tbl
    lsr
    tax
    lda dcmIndexLo,X
    sta @call+1
    lda dcmIndexHi,X
    sta @call+2
    ; figure last line of decim unroll
    txa
    clc
    adc lineCt
    tax
    lda dcmIndexLo,X
    sta pTmp
    lda dcmIndexHi,X
    sta pTmp+1
; determine blit offset for writing
    ldy pixNum
    ldx blitOffsetEven,Y
    ; store rts so decim returns @ right moment
    ldy #0
    lda (pTmp),Y ; save existing byte
    pha
    lda @rts
    sta (pTmp),Y

    .if DEBUG
    phx
    phy
    jsr rdKey
    pha
    jsr crout
    pla
    ply
    plx
    cmp #$9B
    bne @notEscape
    brk
@notEscape:
    nop
    .endif

@call:
    jsr decimRoll
; fix rts to what it was before
    ldy #0
    pla
    sta (pTmp),Y

    .if DEBUG
    ldy byteNum ; to see results early
    sta setAuxZP
    jsr blitRoll
    sta clrAuxZP
    .endif

@rts:
    rts
@adjustedHeight: .byte 0
@adjustedCol: .byte 0

; Calc pointer into the bump table
; Input: X - height to render in dbl lines
calcBump:
    stx @sub+1
    lda #0
    sta pBump+1
    lda #63 ; bump 63 is actually first
    sec
@sub:
    sbc #0
    bpl @notNeg
    lda #0
@notNeg:

    .if DEBUG
    pha
    lda #"b"
    jsr cout
    pla
    pha
    jsr prByte
    lda #" "
    jsr cout
    pla
    .endif

    ldx #6
@lup:
    asl
    rol pBump+1
    dex
    bne @lup
    clc
    adc #<bumps
    sta pBump
    lda pBump+1
    adc #>bumps
    sta pBump+1

    .if DEBUG
    lda #"p"
    jsr cout
    lda pBump+1
    jsr prByte
    lda pBump
    jsr prByte
    lda #" "
    jsr cout
    .endif

    rts

; Build table of screen line pointers
; on aux zero-page
makeLines:
    lda #0
    sta lineCt
    lda #<TOP_LINE
    sta pLine
    lda #>TOP_LINE
    sta pLine+1
@lup:
    lda lineCt
    asl
    tax
    lda pLine
    ldy pLine+1
    sta setAuxZP
    sta 0,X
    sty 1,X
    sta clrAuxZP
    jsr nextLine
    inc lineCt
    lda lineCt
    cmp #NLINES
    bne @lup
    rts

; Set screen lines to current back buf
setBackBuf:
; calculate screen start
    lda bacKBuf
    asl
    asl
    asl
    asl
    asl
    clc
    adc #$20
    sta setAuxZP
    sta $FF
    ldx #0
@lup:
    lda 1,X
    and #$1F
    ora $FF
    sta 1,X
    inx
    inx
    bne @lup
    sta clrAuxZP
    rts

; Load file, len-prefixed name in A/X (hi/lo), to addr on stack
; (push hi byte first, then push lo byte)
bload:
    stx @mliCommand+1 ; filename lo
    sta @mliCommand+2 ; filename hi
    lda #<prodosBuf
    sta @mliCommand+3
    lda #>prodosBuf
    sta @mliCommand+4
    lda #$C8 ; open
    ldx #3
    jsr @doMLI
    lda @mliCommand+5 ; get handle and put it in place
    sta @mliCommand+1
    ply ; save ret addr
    plx
    pla
    sta @mliCommand+2 ; load addr lo
    pla
    sta @mliCommand+3 ; load addr hi
    phx ; restore ret addr
    phy
    lda #$CA ; read
    sta @mliCommand+5 ; also length (more than enough)
    ldx #4
    jsr @doMLI
@close:
    stz @mliCommand+1 ; close all
    lda #$CC
    ldx #1
    ; fall through
@doMLI:
    sta @mliOp
    stx @mliCommand
    jsr MLI
@mliOp: .byte 0
    .addr @mliCommand
    bcs @err
    rts
@err:
    jsr prByte
    jsr prErr
    ldx #$FF
    txs
    jmp monitor
@mliCommand: .res 10 ; 10 bytes should be plenty

; Copy X pages starting at pg Y to aux mem
copyToAux:
    sta setAuxWr
    sty pDst+1
    ldy #0
    sty pDst
@lup:
    lda (pDst),Y
    sta (pDst),Y
    iny
    bne @lup
    inc pDst+1
    dex
    bne @lup
    sta clrAuxWr
    rts

; Fetch the next byte from the pre-raycasted data
; Note this routine needs to be copied to aux mem.
getCast:
    ldy #0
    sta setAuxRd
    lda (pCast),Y
    sta clrAuxRd
    inc pCast
    bne @done
    inc pCast+1
@done:
    rts

; Test code to see if things really work
test:
    ; clear ProDOS mem map so it lets us load
    ldx #$18
    lda #1
@memLup:
    sta memMap-1,X
    lda #0
    dex
    bne @memLup
    ; make reset go to monitor
    lda #<monitor
    sta resetVec
    lda #>monitor
    sta resetVec+1
    eor #$A5
    sta resetVec+2
; load the pre-raycast data
    lda #$20 ; addr hi
    pha
    lda #0 ; addr lo
    pha
    ldx #<@precastName
    lda #>@precastName
    jsr bload
; copy it to aux mem
    ldy #$20
    ldx #$60
    jsr copyToAux
    lda #0 ; set ptr to it
    sta pCast
    lda #$20
    sta pCast+1
; copy our code to aux mem
    ldy #>codeBeg
    ldx #>codeEnd - >codeBeg + 1
    jsr copyToAux
; set up everything else
    jsr clearMem
    ; load the textures
    lda #>tex0
    pha
    lda #<tex0
    pha
    ldx #<@tex0Name
    lda #>@tex0Name
    jsr bload

    lda #>tex1
    pha
    lda #<tex1
    pha
    ldx #<@tex1name
    lda #>@tex1name
    jsr bload

    lda #>tex2
    pha
    lda #<tex2
    pha
    ldx #<@tex2name
    lda #>@tex2name
    jsr bload

    lda #>tex3
    pha
    lda #<tex3
    pha
    ldx #<@tex3name
    lda #>@tex3name
    jsr bload

    ; load the fancy frame
    lda #>$2000
    pha
    lda #<$2000
    pha
    ldx #<@frameName
    lda #>@frameName
    jsr bload

; build all the unrolls and tables
    jsr makeBlit
    jsr makeClrBlit
    jsr makeShift
    jsr makeDCM
    jsr makeBumps
    jsr makeLines
; set up front and back buffers
    lda #0
    sta frontBuf
    .if DOUBLE_BUFFER
    lda #1
    .endif
    sta bacKBuf

    bit clrText
    bit setHires

    lda #63
    sta lineCt
    lda #1
    sta @dir
    jsr clearBlit
@oneLevel:
    lda #0
    sta pixNum
    sta byteNum
    .if DOUBLE_BUFFER
    jsr setBackBuf
    .endif

    .if DEBUG
    lda pCast+1
    jsr prByte
    lda pCast
    jsr prByte
    jsr crout
    .endif

@oneCol:
    jsr getCast ; first byte is height
    cmp #$FF
    bne @noReset
; found end of cast data, start over
    lda #0
    sta pCast
    lda #$20
    sta pCast+1
    jsr getCast
@noReset:
    cmp #63
    bcc @heightOk
    lda #62
@heightOk:
    sta lineCt
    jsr getCast ; second byte is tex num and tex col
    pha
    and #$3F
    cmp #63
    bcc @columnOk
    lda #62
@columnOk:
    sta txColNum
    pla
    lsr ; div by 64
    lsr
    lsr
    lsr
    lsr
    lsr
    tay ; Y now holds tex num
    jsr decimateCol
    inc pixNum
    lda pixNum
    cmp #7
    bne @oneCol
@flush:
    ldy byteNum
    iny
    iny
    sta setAuxZP
    jsr blitRoll
    sta clrAuxZP
    jsr clearBlit
    lda #0
    sta pixNum
    inc byteNum
    inc byteNum
    lda byteNum
    cmp #18
    bne @oneCol
@nextLevel:
; flip onto the screen
    .if DOUBLE_BUFFER
    ldx bacKBuf
    lda frontBuf
    sta bacKBuf
    stx frontBuf
    lda page1,X
    .endif
    ; adv past FE in cast data
    jsr getCast
    cmp #$FE
    bne @err
    jsr getCast
    cmp #$FE
    beq @incDec
@err:
    brk
@incDec:
    lda kbd ; stop if ESC is pressed
    cmp #$9B
    beq @done
    cmp #$A0 ; pause if space is pressed
    bne @notSpace
    bit kbdStrobe
@pauseLup:
    lda kbd
    bpl @pauseLup
@notSpace:
    jmp @oneLevel
@done:
    sta kbdStrobe ; eat the keypress
    bit setText
    bit page1
; quit the ProDOS way
    inc resetVec+2 ; invalidate reset vector
    jsr MLI
    .byte $65
    .addr @quitParms
@quitParms:
    .byte 4, 0
    .word 0
    .byte 0
    .word 0

@dir: .byte 1
@tex0Name: .byte 21
    .byte "/LL/ASSETS/BUILDING01"
@tex1name: .byte 21
    .byte "/LL/ASSETS/BUILDING02"
@tex2name: .byte 21
    .byte "/LL/ASSETS/BUILDING03"
@tex3name: .byte 21
    .byte "/LL/ASSETS/BUILDING04"
@precastName: .byte 18
    .byte "/LL/ASSETS/PRECAST"
@frameName: .byte 16
    .byte "/LL/ASSETS/FRAME"

codeEnd = *

