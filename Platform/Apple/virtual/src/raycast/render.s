
    .org $7000

; This code is written bottom-up. That is, simple routines first, 
; then routines that call those to build complexity. The main
; code is at the very end. We jump to it now.
    jmp main

; Conditional assembly flags
DOUBLE_BUFFER = 1       ; whether to double-buffer
DEBUG         = 0       ; turn on verbose logging

; Shared constants, zero page, buffer locations, etc.
    .include "render.i"

; Variables
backBuf:   .byte 0      ; (value 0 or 1)
frontBuf:  .byte 0      ; (value 0 or 1)
mapBase:   .word 0
nTextures: .byte 0

; texture addresses
MAX_TEXTURES = 20
texAddrLo: .res MAX_TEXTURES
texAddrHi: .res MAX_TEXTURES

; Debug macros
.macro DEBUG_STR str
.if DEBUG
    php
    pha
    jsr _writeStr
    .byte str,0
    pla
    plp
.endif
.endmacro

.macro DEBUG_BYTE byte
.if DEBUG
    php
    pha
    lda byte
    jsr prbyte
    lda #$A0
    jsr cout
    pla
    plp
.endif
.endmacro

.macro DEBUG_WORD word
.if DEBUG
    php
    pha
    lda word+1
    jsr prbyte
    lda word
    jsr prbyte
    lda #$A0
    jsr cout
    pla
    plp
.endif
.endmacro

.macro DEBUG_LN
.if DEBUG
    php
    pha
    jsr crout
    pla
    plp
.endif
.endmacro

.macro DEBUG_RDKEY
.if DEBUG
    php
    pha
    tya
    pha
    txa
    pha
    jsr rdkey
    pla
    tay
    pla
    tax
    pla
    plp
.endif
.endmacro

; Non-debug function to print a string. Does not preserve registers.
.macro WRITE_STR str
    jsr _writeStr
    .byte str,0
.endmacro

; Support to print a string following the JSR, in high or low bit ASCII, 
; terminated by zero. If the string has a period "." it will be followed 
; automatically by the next address and a CR.
_writeStr:
    pla
    clc
    adc #1
    sta @ld+1
    pla
    adc #0
    sta @ld+2
@ld:
    lda $2000
    beq @done
    ora #$80
    jsr cout
    cmp #$AE
    bne :+
    lda #$DB ; [
    jsr cout
    lda @ld+1
    clc
    adc #4
    pha
    lda @ld+2
    adc #0
    jsr prbyte
    pla
    jsr prbyte
    lda #$DD ; ]
    jsr cout
    jsr crout
:   inc @ld+1
    bne @ld
    inc @ld+2
    bne @ld     ; always taken
@done:
    lda @ld+2
    pha
    lda @ld+1
    pha
    rts

;-------------------------------------------------------------------------------
; Multiply two bytes, quickly but somewhat inaccurately, using logarithms.
; Utilizes tbl_log2_b_b and tbl_pow2_b_b, which translate to and from 3+5 bit
; fixed precision base 2 logarithms.
;
; Input : unsigned bytes in X and Y
; Output: unsigned byte in A of the *high* byte of the result only
;
umul_bb_b:
    cpx #4
    bcc @x_lt_4
    cpy #4
    bcc @y_lt_4
    lda tbl_log2_b_b,x  ; log2(x)
    clc
    adc tbl_log2_b_b,y  ; plus log2(y)
    tax
    lda tbl_pow2_b_b,x  ; 2 ^ (log2(x) + log2(y))  =  x * y
    rts
; handle cases 0..3 directly. This halved the size of the tables
; and made them more accurate.
@x_lt_4:
    lda #0
    cpx #2
    bcc @done   ; x=0 or x=1: the high byte of result will be zero
    beq @two
@three:
    cpy #86     ; x=3: 3*(0..85) results in hi=0
    bcc @done
    lda #1
    cpy #171    ; 3*(86..170) results in hi=1
    bcc @done
    lda #2      ; 3*(171..255) results in hi=2
    rts
@two:
    cpy #$80    ; x=2: high byte is 1 iff input >= 0x80
    bcc @done
    lda #1
@done:
    rts
@y_lt_4:
    stx tmp     ; switch X and Y
    tya
    tax
    ldy tmp
    jmp @x_lt_4 ; then re-use code

;-------------------------------------------------------------------------------
; Calculate log2 of a 16-bit number.
; Input: 16-bit unsigned int in A(lo)/X(hi)
; Output: fixed point 8+8 bit log2 in A(lo)/X(hi)
;
log2_w_w:
    cpx #0
    beq log2_b_w        ; hi-byte zero? only consider low byte
    stx tmp
    ldx #8              ; start with exponent=8
    lsr tmp             ; shift down
    beq @gotMant        ; until high byte is exactly 1
@highLup:
    ror                 ; save the bit we shifted out
    inx                 ; bump the exponent
    lsr tmp             ; shift next bit
    bne @highLup        ; loop again
@gotMant:
    ; mantissa now in A, exponent in X. Translate mantissa to log using table, and we're done
    tay
    lda tbl_log2_w_w,y
    rts

; Same as above but with with 8-bit input instead of 16. Same output though.
log2_b_w:
    cmp #0              ; special case: log(0) we call zero.
    beq @zero
@low:                   ; we know high byte is zero
    ldx #7              ; start with exponent=7
    asl                 ; shift up
    bcs @gotMant        ; until high byte would be exactly 1
@lowLup:
    dex                 ; bump exponent down
    asl                 ; shift next bit
    bcc @lowLup         ; loop again
@gotMant:
    ; mantissa now in A, exponent in X. Translate mantissa to log using table, and we're done
    tay
    lda tbl_log2_w_w,y
    rts
@zero:
    tax
    rts

;-------------------------------------------------------------------------------
; Calculate 2^n for a fixed-point n
; Input:  8.8 fixed precision number in Y(lo)/X(hi)
; Output: 16 bit unsigned int in A(lo)/X(hi)
;
pow2_w_w:
    lda tbl_pow2_w_w,y  ; table gives us log(2) -> mantissa in A
    cpx #8              ; check the exponent
    bcc @lo             ; less than 8? high byte will be zero.
    beq @mid            ; equal to 8? high byte will be one.
@hi:                    ; greater than 8: need to compute high byte
    ldy #1              ; start with one
    sty tmp
@hiLup:
    asl                 ; shift up
    rol tmp             ; including high byte
    dex                 ; count down exponent
    cpx #8              ; until we reach 8
    bne @hiLup
    ldx tmp             ; load computed high byte; proper low byte is already in A.
    rts
@lo:
    sec                 ; so we shift a 1 into the high bit of the low byte
@loLup:
    ror                 ; shift down
    inx                 ; count up exponent...
    cpx #8              ; ...until we hit 8
    bcc @loLup          ; handy because we need carry to be clear the next go-round
    ldx #0
    rts
@mid:                   ; exponent exactly 8 when we get here
    ldx #1              ; that means high byte should be 1
    rts

;-------------------------------------------------------------------------------
; Cast a ray  [ref BigBlue3_60]
; Input: pRayData, plus Y reg: precalculated ray data (4 bytes)
;        playerX, playerY (integral and fractional bytes of course)
;        pMap: pointer to current row on the map (mapBase + playerY{>}*height)
; Output: lineCt - height to draw in double-lines
;         txColumn - column in the texture to draw
castRay:
    ; First, grab the precalculated ray data from the table.
    ldx #1              ; default X step: forward one column of the map
    lda (pRayData),y    ; rayDirX
    bpl :+              ; if positive, don't negate
    eor #$FF            ; negate
    ldx #$FF            ; x step: back one column of the map
:   asl                 ; mul by 2 now that we've stripped the sign
    sta rayDirX         ; store x direction
    stx stepX           ; and x increment

    iny
    ldx #1              ; default y step: forward one row of the map
    lda (pRayData),y    ; rayDirY
    bpl :+              ; if positive, don't negate
    eor #$FF            ; negate
    ldx #$FF            ; y step: back one row of map
:   asl                 ; mul by 2 now that we've stripped the sign
    sta rayDirY         ; store y direction
    stx stepY           ; and y increment

    iny
    lda (pRayData),y    ; distance moved in X direction
    sta deltaDistX      ; for each step

    iny
    lda (pRayData),y    ; distance moved in Y direction
    sta deltaDistY      ; for each step

    DEBUG_STR "cast: ddX="
    DEBUG_BYTE deltaDistX
    DEBUG_STR "rdX="
    DEBUG_BYTE rayDirX
    DEBUG_STR "stx="
    DEBUG_BYTE stepX
    DEBUG_LN
    DEBUG_STR "      ddY="
    DEBUG_BYTE deltaDistY
    DEBUG_STR "rdY="
    DEBUG_BYTE rayDirY
    DEBUG_STR "sty="
    DEBUG_BYTE stepY
    DEBUG_LN

    ; Next we need to calculate the initial distance on each side
    ; Start with the X side
    lda playerX         ; fractional byte of player distance
    bit stepX           ; if stepping forward...
    bmi :+
    eor #$FF            ; invert initial dist
:   tax
    ldy deltaDistX      ; calculate fraction of delta
    jsr umul_bb_b
    sta sideDistX       ; to form initial side dist
    ; Now the Y side
    lda playerY         ; fractional byte of player distance
    bit stepY           ; if stepping forward...
    bmi :+
    eor #$FF            ; invert initial dist
:   tax
    ldy deltaDistY      ; calculate fraction of delta
    jsr umul_bb_b
    sta sideDistY       ; to form initial side dist

    DEBUG_STR "  initial sdx="
    DEBUG_BYTE sideDistX
    DEBUG_STR " sdy="
    DEBUG_BYTE sideDistY
    DEBUG_LN

    ; Start at the player's position, and init Y reg for stepping in the X dir
    ldy playerX+1
    sty mapX
    ldx playerY+1
    stx mapY

    ; Also init the min/max trackers
    sty minX
    sty maxX
    stx minY
    stx maxY

    ; the DDA algorithm
@DDA_step:
    lda sideDistX
    cmp sideDistY       ; decide whether it's closer to step in X dir or Y dir
    bcs @takeStepY
    ; taking a step in the X direction
@takeStepX:
    lda stepX           ; advance mapX in the correct direction
    bmi @negX
    inc mapX
    iny                 ; also the Y reg which indexes the map
    jmp @checkX
@negX:
    dec mapX
    dey
@checkX:
    .if DEBUG
    DEBUG_STR "  sideX"
    jsr @debugSideData
    .endif
    lda sideDistY       ; adjust side dist in Y dir
    sec
    sbc sideDistX
    sta sideDistY
    lda deltaDistX      ; re-init X distance
    sta sideDistX
    lda (pMap),y        ; check map at current X/Y position
    beq @DDA_step       ; nothing there? do another step.
    ; We hit something!
@hitX:
    DEBUG_STR "  Hit."
    sta txNum           ; store the texture number we hit
    lda #0
    sec
    sbc playerX         ; inverse of low byte of player coord
    sta dist            ; is fractional byte of dist.
    lda mapX            ; map X is the integer byte
    sbc playerX+1
    bit stepX
    bpl :+
    clc                 ; if stepping backward, add one to dist
    adc #1
:   sta dist+1
    ldx rayDirX         ; parameters for wall calculation
    ldy rayDirY
    lda stepY
    jsr @wallCalc       ; centralized code for wall calculation
    ; adjust wall X
    lda playerY         ; fractional player pos
    clc
    adc txColumn
    bit stepX           ; if stepping forward in X...
    bmi :+
    eor #$FF            ; ...invert the texture coord
:   sta txColumn
    .if DEBUG
    jmp @debugFinal
    .else
    rts
    .endif
    ; taking a step in the Y direction
@takeStepY:
    lda pMap            ; get ready to switch map row
    bit stepY           ; advance mapY in the correct direction
    bmi @negY
    inc mapY
    clc
    adc mapWidth
    bcc @checkY
    inc pMap+1
    bne @checkY         ; always taken
@negY:
    dec mapY
    sec
    sbc mapWidth
    bcs @checkY
    dec pMap+1
@checkY:
    sta pMap
    .if DEBUG
    DEBUG_STR "  sideY"
    jsr @debugSideData
    .endif
    lda sideDistX       ; adjust side dist in Y dir
    sec
    sbc sideDistY
    sta sideDistX
    lda deltaDistY      ; re-init Y distance
    sta sideDistY
    lda (pMap),y        ; check map at current X/Y position
    bne @hitY           ; nothing there? do another step.
    jmp @DDA_step
@hitY:
    ; We hit something!
    DEBUG_STR "  Hit."
    sta txNum           ; store the texture number we hit
    lda #0
    sec
    sbc playerY         ; inverse of low byte of player coord
    sta dist            ; is fractional byte of dist.
    lda mapY            ; map X is the integer byte
    sbc playerY+1
    bit stepY
    bpl :+
    clc                 ; if stepping backward, add one to dist
    adc #1
:   sta dist+1
    ldx rayDirY         ; parameters for wall calculation
    ldy rayDirX
    lda stepX
    jsr @wallCalc       ; centralized code for wall calculation
    ; adjust wall X
    lda playerX         ; fractional player pos
    clc
    adc txColumn
    bit stepY           ; if stepping backward in Y
    bpl :+
    eor #$FF            ; ...invert the texture coord
:   sta txColumn
    .if DEBUG
    jmp @debugFinal
    .else
    rts
    .endif

    ; wall calculation: X=dir1, Y=dir2, A=dir2step
@wallCalc:
    pha                 ; save step
    tya
    pha                 ; save dir2
    txa
    jsr log2_b_w        ; calc log2(dir1)
    sta @sub1+1         ; save it for later subtraction
    stx @sub2+1
    lda dist            ; calc abs(dist)
    ldx dist+1          ; dist currently in A(lo)/X(hi)
    bpl @notNeg
    lda #0              ; invert distance if negative to get absolute value
    sec
    sbc dist
    tay
    lda #0
    sbc dist+1
    tax                 ; get inverted dist into A(lo)/X(hi)
    tya
@notNeg:
    .if DEBUG
    sta a2l
    stx a2h
    DEBUG_STR "  dist="
    DEBUG_WORD a2l
    .endif
    jsr log2_w_w        ; calculate log2(abs(dist))
    sec
@sub1:
    sbc #0              ; subtract log2(dir1)
    sta diff
    txa
@sub2:
    sbc #0
    sta diff+1
    DEBUG_STR "diff="
    DEBUG_WORD diff
    ; Calculate texture coordinate
    pla                 ; get dir2 back
    jsr log2_b_w        ; calculate log2(dir2)
    clc
    adc diff            ; sum = diff + log2(dir2)
    tay
    txa
    adc diff+1
    tax
    .if DEBUG
    sty a2l
    stx a2h
    DEBUG_STR "sum="
    DEBUG_WORD a2l
    DEBUG_LN
    .endif
    jsr pow2_w_w        ; calculate 2 ^ sum
    ; fractional part (A-reg) of result is texture coord
    tax
    pla                 ; retrieve the step direction
    bpl :+              ; if positive, don't flip the texture coord
    txa
    eor #$FF            ; negative, flip the coord
    tax
:   stx txColumn
    ; Calculate line height
    ; we need to subtract diff from log2(64) which is $0600
    lda #0
    sec
    sbc diff
    tay
    sta depth
    lda #6
    sbc diff+1
    tax
    lsr                 ; Depth is 4 bits of exponent + upper 4 bits of mantissa
    ror depth
    lsr
    ror depth
    lsr
    ror depth
    lsr
    ror depth
    jsr pow2_w_w        ; calculate 2 ^ (log(64) - diff)  =~  64.0 / dist
    cpx #0
    beq :+
    lda #$FF            ; clamp large line heights to 255
:   sta lineCt
    ; Update min/max trackers
    lda mapX
    cmp minX
    bcs :+
    sta minX
:   cmp maxX
    bcc :+
    sta maxX
:   lda mapY
    cmp minY
    bcs :+
    sta minY
:   cmp maxY
    bcc :+
    sta maxY
:   rts                 ; all done with wall calculations
    .if DEBUG
@debugSideData:
    DEBUG_STR ", mapX="
    DEBUG_BYTE mapX
    DEBUG_STR "mapY="
    DEBUG_BYTE mapY
    DEBUG_STR "sdx="
    DEBUG_BYTE sideDistX
    DEBUG_STR "sdy="
    DEBUG_BYTE sideDistY
    DEBUG_LN
    rts
@debugFinal:
    DEBUG_STR "  lineCt="
    DEBUG_BYTE lineCt
    DEBUG_STR "txNum="
    DEBUG_BYTE txNum
    DEBUG_STR "txCol="
    DEBUG_BYTE txColumn
    DEBUG_LN
    jsr rdkey
    .endif

; Advance pLine to the next line on the hi-res screen
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

; Draw a ray that was traversed by calcRay
drawRay:
    ; Make a pointer to the selected texture
    ldx txNum
    dex                 ; translate tex 1..4 to 0..3
    lda texAddrLo,x
    sta pTex
    lda texAddrHi,x
    sta pTex+1
    DEBUG_STR "Ready to call, pTex="
    DEBUG_WORD pTex
    DEBUG_LN
    ; jump to the unrolled expansion code for the selected height
    lda lineCt
    asl
    bcc :+
    lda #254            ; clamp max height
:   sta $10B            ; set vector offset
    DEBUG_STR "Calling expansion code."
    jmp $100            ; was copied here earlier from @callIt

; Template for blitting code [ref BigBlue3_70]
blitTemplate: ; comments show byte offset
    lda decodeTo57 ;  0: pixel 3
    asl ;  3: save half of pix 3 in carry
    ora decodeTo01 ;  4: pixel 0
    ora decodeTo23 ;  7: pixel 1
    ora decodeTo45 ; 10: pixel 2
    sta (0),y ; 13: even column
    iny ; 15: prep for odd
    lda decodeTo01b ; 16: pixel 4
    ora decodeTo23b ; 19: pixel 5
    rol ; 22: recover half of pix 3
    ora decodeTo56 ; 23: pixel 6 - after rol to ensure right hi bit
    sta (0),y ; 26: odd column
    dey ; 28: prep for even
    ; 29 bytes total

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
    ldy #29
@copy:
    lda blitTemplate,y
    sta (pDst),y
    dey
    bpl @copy
; Set the line pointers
    ldy #14
    jsr @storeLine
    ldy #27
    jsr @storeLine
    ; Get ready for odd line
    jsr @advance
; Loop until all lines are done
    lda lineCt
    cmp #NLINES
    bne @lineLup
    jmp storeRTS ; Finish with RTS for cleanliness
@storeLine: ; Subroutine to store pLine to pDst
    lda lineCt
    asl
    sta (pDst),y
    rts
@advance: ; Subroutine to go to next unroll
    lda #29
    jsr advPDst
    inc lineCt
    jmp nextLine

; Add A to pDst
advPDst:
    clc
    adc pDst
    sta pDst
    bcc @rts
    inc pDst+1
@rts:
    rts

; Store a RTS at pDst
storeRTS:
    lda #$60
    ldy #0
    sta (pDst),y
    rts

; Create code to clear the blit
makeClrBlit:
    lda #<blitRoll
    sta pDst
    lda #>blitRoll
    sta pDst+1
    ldx #0
    ldy #0
@lup:
    lda @st
    sta clrBlitRollE,x
    sta clrBlitRollO,x
    inx
    lda pDst
    sta clrBlitRollE,x
    clc
    adc #29
    sta clrBlitRollO,x
    inx
    lda pDst+1
@st:
    sta clrBlitRollE,x
    adc #0
    sta clrBlitRollO,x
    inx
    lda pDst
    clc
    adc #29*2
    sta pDst
    lda pDst+1
    adc #0
    sta pDst+1
    iny
    iny
    cpy #64
    bne @noSwitch
    lda @tya ; switch from sky color to ground color
    sta clrBlitRollE,x
    sta clrBlitRollO,x
    inx
@noSwitch:
    cpy #NLINES
    bne @lup
    lda @rts
    sta clrBlitRollE,x
    sta clrBlitRollO,x
@rts:
    rts
@tya:
    tya

; Clear the blit
clearBlit:
    lda byteNum
    and #2
    bne @alt
    ldx #BLIT_OFF0
    jsr @clear1
    ldx #BLIT_OFF1
    jsr @clear2
    ldx #BLIT_OFF2
    jsr @clear1
    ldx #BLIT_OFF3
    jsr @clear2
    ldx #BLIT_OFF4
    jsr @clear1
    ldx #BLIT_OFF5
    jsr @clear2
    ldx #BLIT_OFF6
    jmp @clear1
@alt:
    ldx #BLIT_OFF0
    jsr @clear2
    ldx #BLIT_OFF1
    jsr @clear1
    ldx #BLIT_OFF2
    jsr @clear2
    ldx #BLIT_OFF3
    jsr @clear1
    ldx #BLIT_OFF4
    jsr @clear2
    ldx #BLIT_OFF5
    jsr @clear1
    ldx #BLIT_OFF6
    jmp @clear2
@clear1:
    ldy #GROUND_COLOR_E
    lda #SKY_COLOR_E
    jsr clrBlitRollO
    ldy #GROUND_COLOR_O
    lda #SKY_COLOR_O
    jmp clrBlitRollE
@clear2:
    ldy #GROUND_COLOR_E
    lda #SKY_COLOR_E
    jsr clrBlitRollE
    ldy #GROUND_COLOR_O
    lda #SKY_COLOR_O
    jmp clrBlitRollO

; Construct the pixel decoding tables
makeDecodeTbls:
    ldx #0
@shiftA:
    ; bit 5 controls the high bit (orange/blue vs green/purple)
    txa
    asl
    asl
    and #$80
    sta tmp+1
    ; extract only bits 1 and 3 for the pixel data
    txa
    and #8      ; bit 3
    lsr
    lsr
    sta tmp
    txa
    and #2      ; bit 1
    lsr
    ora tmp
@decodeTo01:
    ora tmp+1
    sta decodeTo01,x
@decodeTo01b:   ; put hi bit in bit 6 instead of 7
    bpl :+
    ora #$40
:   sta decodeTo01b,x
@decodeTo23:
    asl
    asl
    ora tmp+1
    sta decodeTo23,x
@decodeTo23b:   ; put hi bit in bit 6 instead of 7
    bpl :+
    ora #$40
:   sta decodeTo23b,x
@decodeTo45:
    asl
    asl
    ora tmp+1
    sta decodeTo45,x
@decodeTo56:
    asl
    ora tmp+1
    sta decodeTo56,x
@decodeTo57:
    asl
    asl
    php
    lsr
    plp
    ror
    sta decodeTo57,x
@next:
    inx
    bne @shiftA
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
    sta 0,x
    sty 1,x
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
    lda backBuf
    asl
    asl
    asl
    asl
    asl
    clc
    adc #$20
    sta setAuxZP
    ldx #0
@lup:
    eor 1,x
    and #$60            ; only two bits control the screen buf
    eor 1,x
    sta 1,x
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
    pla ; save ret addr
    tay
    pla
    tax
    pla
    sta @mliCommand+2 ; load addr lo
    pla
    sta @mliCommand+3 ; load addr hi
    txa ; restore ret addr
    pha
    tya
    pha
    lda #$CA ; read
    sta @mliCommand+5 ; also length (more than enough)
    ldx #4
    jsr @doMLI
@close:
    lda #0
    sta @mliCommand+1 ; close all
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
    jsr prbyte
    jsr prerr
    ldx #$FF
    txs
    jmp monitor
@mliCommand: .res 10 ; 10 bytes should be plenty

; Copy pTmp -> pDst (advancing both), length in X(lo) / Y(hi)
copyMem:
    txa
    pha
    tya
    ldy #0
    tax
    beq @lastPg
@pageLup:
    lda (pTmp),y
    sta (pDst),y
    iny
    bne @pageLup
    inc pTmp+1
    inc pDst+1
    dex
    bne @pageLup
@lastPg:
    pla
    beq @done
    tax
@byteLup:
    lda (pTmp),y
    sta (pDst),y
    inc pTmp
    bne :+
    inc pTmp+1
:   inc pDst
    bne :+
    inc pDst+1
:   dex
    bne @byteLup
@done:
    rts

; Read a byte from pTmp and advance it. No regs except A are disturbed.
readPtmp:
    lda pTmp
    sta @ld+1
    lda pTmp+1
    sta @ld+2
    inc pTmp
    bne @ld
    inc pTmp+1
@ld: lda $100
    rts

;-------------------------------------------------------------------------------
initMem:
    DEBUG_STR "Clearing memory map."
    ; Clear ProDOS mem map so it lets us load stuff anywhere we want
    ldx #$18
    lda #1
@memLup:
    sta memMap-1,x
    lda #0
    dex
    bne @memLup
    ; Make reset go to monitor
    lda #<monitor
    sta resetVec
    lda #>monitor
    sta resetVec+1
    eor #$A5
    sta resetVec+2
    ; Copy the expansion caller to low stack.
    ldx #12
:   lda @callIt,x
    sta $100,x
    dex
    bpl :-
    rts
@callIt:
    sta setAuxRd
    jsr $10A
    sta clrAuxRd
    rts
    jmp (expandVec)

;-------------------------------------------------------------------------------
; Establish the initial player position and direction [ref BigBlue3_10]
setPlayerPos:
    ; X=1.5
    lda #1
    sta playerX+1
    lda #$80
    sta playerX
    ; Y=2.5
    lda #2
    sta playerY+1
    lda #$80
    sta playerY
    ; direction=0
    lda #0
    sta playerDir
    rts

;-------------------------------------------------------------------------------
; Load the texture expansion code, copy it to aux mem
loadFiles:
    DEBUG_STR "Loading files."
    lda #>expandVec
    sta pTmp+1
    sta pDst+1
    pha
    lda #<expandVec
    sta pTmp
    sta pDst
    pha
    ldx #<@expandName
    lda #>@expandName
    jsr bload
    ldx #<(textures-expandVec)
    ldy #>(textures-expandVec)
    sta setAuxWr
    jsr copyMem
    sta clrAuxWr

; Load the map + texture pack
    lda #8      ; load at $800
    sta pTmp+1
    pha
    lda #0
    sta pTmp
    pha
    ldx #<@mapPackName
    lda #>@mapPackName
    jsr bload

; First comes the map
    jsr readPtmp
    cmp #'M'
    beq :+
    WRITE_STR "M rec missing."
    brk
    ; map starts with width & height
:   jsr readPtmp
    sta mapWidth
    jsr readPtmp
    sta mapHeight
    ; next comes length
    jsr readPtmp
    tax
    jsr readPtmp
    tay
    ; then the map data
    lda pTmp
    sta mapBase
    lda pTmp+1
    sta mapBase+1
    ; skip the map data to find the first texture
    txa
    clc
    adc pTmp
    sta pTmp
    tya
    adc pTmp+1
    sta pTmp+1

; Copy the textures to aux mem
    lda #<textures
    sta pDst
    lda #>textures
    sta pDst+1
    lda #0
    sta nTextures

@cpTex:
    jsr readPtmp
    beq @cpTexDone
    cmp #'T'
    beq :+
    WRITE_STR "T rec missing"
    brk
:   jsr readPtmp        ; len lo
    tax
    jsr readPtmp        ; len hi
    pha
    ldy nTextures       ; record texture address
    lda pDst
    sta texAddrLo,y
    lda pDst+1
    sta texAddrHi,y
    inc nTextures
    pla
    tay
    sta setAuxWr
    jsr copyMem         ; copy the texture to aux mem
    sta clrAuxWr
    jmp @cpTex          ; next texture
@cpTexDone:
    DEBUG_STR "Loaded "
    DEBUG_BYTE nTextures
    DEBUG_STR "textures."
    .if DEBUG
    DEBUG_STR "tex1="
    lda texAddrLo+1
    sta tmp
    lda texAddrHi+1
    sta tmp+1
    DEBUG_WORD tmp
    DEBUG_LN
    .endif

    ; load the fancy frame
    DEBUG_STR "Loading frame."
    lda #>$2000
    pha
    lda #<$2000
    pha
    ldx #<@frameName
    lda #>@frameName
    jsr bload
    ; copy the frame to the other buffer also
    .if DOUBLE_BUFFER
    lda #>$4000
    pha
    lda #<$4000
    pha
    ldx #<@frameName
    lda #>@frameName
    jsr bload
    .endif
    rts

@expandName: .byte 10
    .byte "/LL/EXPAND"
@mapPackName: .byte 19
    .byte "/LL/ASSETS/MAP.PACK"
@frameName: .byte 16
    .byte "/LL/ASSETS/FRAME"

;-------------------------------------------------------------------------------
; Set up front and back buffers, go to hires mode, and clear for first blit.
graphInit:
    lda #0
    sta frontBuf
    .if DOUBLE_BUFFER
    lda #1
    .endif
    sta backBuf

    .if DEBUG
    DEBUG_STR "Staying in text mode."
    .else
    bit clrText
    bit setHires
    .endif
    rts

;-------------------------------------------------------------------------------
; Render one whole frame
renderFrame:
    .if DOUBLE_BUFFER
    jsr setBackBuf
    .endif

    ; Initialize pointer into precalculated ray table, based on player dir.
    ; The table has 256 bytes per direction.
    lda #0
    sta pRayData
    lda playerDir
    clc
    adc #>precast_0
    sta pRayData+1

    ; Calculate pointer to the map row based on playerY
    lda mapBase         ; start at row 0, col 0 of the map
    ldy mapBase+1
    ldx playerY+1       ; integral part of player's Y coord
    beq @gotMapRow
    clc
@mapLup:                ; advance forward one row
    adc mapWidth
    bcc :+
    iny
    clc
:   dex                 ; until we reach players Y coord
    bne @mapLup
@gotMapRow:
    tax                 ; map row ptr now in X(lo) / Y(hi)

    .if DEBUG
    stx tmp
    sty tmp+1
    DEBUG_STR "Initial pMap="
    DEBUG_WORD tmp
    DEBUG_LN
    ldx tmp
    ldy tmp+1
    .endif

    lda #0
    sta pixNum
    sta byteNum
    ; A-reg needs to be zero at this point -- it is the ray offset.
    ; Calculate the height, texture number, and texture column for one ray
    ; [ref BigBlue3_50]
@oneCol:
    stx pMap            ; set initial map pointer for the ray
    sty pMap+1
    pha                 ; save ray offset
    tay                 ; ray offset where it needs to be
    lda pMap+1          ; save map row ptr
    pha
    txa
    pha
    jsr castRay         ; cast the ray across the map
    lda pixNum
    bne :+
    jsr clearBlit       ; clear blit on the first pixel
:   jsr drawRay         ; and draw the ray
    .if DEBUG
    DEBUG_STR "Done drawing ray "
    tsx
    lda $103,x          ; retrieve ray offset
    lsr
    lsr
    jsr prbyte
    DEBUG_LN
    .endif
    inc pixNum          ; do we need to flush the pixel buffer?
    lda pixNum
    cmp #7
    bne @nextCol        ; not yet
@flush:
    ; flush the blit
    DEBUG_STR "Flushing."
    ldy byteNum
    iny                 ; move to right 2 bytes to preserve frame border
    iny
    sta setAuxZP
    jsr blitRoll
    sta clrAuxZP
    lda #0
    sta pixNum
    inc byteNum
    inc byteNum
@nextCol:
    pla
    tax
    pla
    tay
    pla
    clc
    adc #4              ; advance to next ray
    cmp #$FC            ; check for end of ray table
    beq @done
    jmp @oneCol         ; go back for another ray
@done:
    rts

;-------------------------------------------------------------------------------
; Move the player forward a quarter step
moveForward:
    lda playerDir
    asl
    asl
    tax
    lda playerX
    clc
    adc walkDirs,x
    sta playerX
    lda playerX+1
    adc walkDirs+1,x
    sta playerX+1
    lda playerY
    clc
    adc walkDirs+2,x
    sta playerY
    lda playerY+1
    adc walkDirs+3,x
    sta playerY+1
    rts

;-------------------------------------------------------------------------------
; Move the player forward a quarter step
moveBackward:
    lda playerDir
    asl
    asl
    tax
    lda playerX
    sec
    sbc walkDirs,x
    sta playerX
    lda playerX+1
    sbc walkDirs+1,x
    sta playerX+1
    lda playerY
    sec
    sbc walkDirs+2,x
    sta playerY
    lda playerY+1
    sbc walkDirs+3,x
    sta playerY+1
    rts

;-------------------------------------------------------------------------------
; Rotate player 22.5 degrees to the left
rotateLeft:
    dec playerDir
    lda playerDir
    cmp #$FF
    bne :+
    lda #15
:   sta playerDir
    rts

;-------------------------------------------------------------------------------
; Rotate player 22.5 degrees to the right
rotateRight:
    inc playerDir
    lda playerDir
    cmp #16
    bne :+
    lda #0
:   sta playerDir
    rts

;-------------------------------------------------------------------------------
; Flip back buffer onto the screen
flip:
    .if DOUBLE_BUFFER
    ldx backBuf
    lda frontBuf
    sta backBuf
    stx frontBuf
    lda page1,x
    .endif
    rts

;-------------------------------------------------------------------------------
; The real action
main:
    ; Put ourselves high on the stack
    ldx #$FF
    txs
    ; Set up memory
    jsr initMem
    jsr setPlayerPos
    jsr loadFiles
    ; Build all the unrolls and tables
    DEBUG_STR "Making tables."
    jsr makeBlit
    jsr makeClrBlit
    jsr makeDecodeTbls
    jsr makeLines
    jsr graphInit
    bit clrMixed
    ; Render the frame and flip it onto the screen
@nextFrame:
    jsr renderFrame
    jsr flip
    ; wait for a key
    DEBUG_STR "Done rendering, waiting for key."
@pauseLup:
    lda kbd             ; check for key
    bpl @pauseLup       ; loop until one is pressed
    sta kbdStrobe       ; eat the keypress
    and #$7F            ; convert to low-bit ASCII because assembler uses that
    cmp #$60            ; lower-case?
    bcc :+              ; no
    sec
    sbc #$20            ; yes, convert to upper case
    ; Dispatch the keypress [ref BigBlue3_40]
:   cmp #'W'            ; 'W' for forward
    bne :+
    jsr moveForward
    jmp @nextFrame
:   cmp #'X'            ; 'X' alternative for 'S'
    bne :+
    lda #'S'
:   cmp #'S'            ; 'S' for backward
    bne :+
    jsr moveBackward
    jmp @nextFrame
:   cmp #'A'            ; 'A' for left
    bne :+
    jsr rotateLeft
    jmp @nextFrame
:   cmp #'D'            ; 'D' for right
    bne :+
    jsr rotateRight
    jmp @nextFrame
:   cmp #$1B            ; ESC to exit
    beq @done
    jmp @pauseLup       ; unrecognized key: go back and get another one.
@done:
    ; back to text mode
    bit setText
    bit page1
    ; quit to monitor
    ldx #$FF
    txs
    jmp monitor

; Following are log/pow lookup tables. For speed, align them on a page boundary.
    .align 256

; Table to translate an unsigned byte to 3+5 bit fixed point log2 [ref BigBlue3_20]
tbl_log2_b_b:
    .byte $00,$00,$00,$00,$00,$07,$0C,$11,$15,$19,$1C,$1F,$22,$24,$27,$29
    .byte $2B,$2D,$2E,$30,$32,$33,$34,$36,$37,$38,$3A,$3B,$3C,$3D,$3E,$3F
    .byte $40,$41,$42,$43,$44,$44,$45,$46,$47,$48,$48,$49,$4A,$4A,$4B,$4C
    .byte $4C,$4D,$4E,$4E,$4F,$50,$50,$51,$51,$52,$52,$53,$53,$54,$54,$55
    .byte $55,$56,$56,$57,$57,$58,$58,$59,$59,$59,$5A,$5A,$5B,$5B,$5B,$5C
    .byte $5C,$5D,$5D,$5D,$5E,$5E,$5E,$5F,$5F,$5F,$60,$60,$61,$61,$61,$61
    .byte $62,$62,$62,$63,$63,$63,$64,$64,$64,$65,$65,$65,$65,$66,$66,$66
    .byte $67,$67,$67,$67,$68,$68,$68,$68,$69,$69,$69,$69,$6A,$6A,$6A,$6A
    .byte $6B,$6B,$6B,$6B,$6C,$6C,$6C,$6C,$6D,$6D,$6D,$6D,$6D,$6E,$6E,$6E
    .byte $6E,$6F,$6F,$6F,$6F,$6F,$70,$70,$70,$70,$70,$71,$71,$71,$71,$71
    .byte $72,$72,$72,$72,$72,$72,$73,$73,$73,$73,$73,$74,$74,$74,$74,$74
    .byte $74,$75,$75,$75,$75,$75,$75,$76,$76,$76,$76,$76,$76,$77,$77,$77
    .byte $77,$77,$77,$78,$78,$78,$78,$78,$78,$79,$79,$79,$79,$79,$79,$79
    .byte $7A,$7A,$7A,$7A,$7A,$7A,$7A,$7B,$7B,$7B,$7B,$7B,$7B,$7B,$7C,$7C
    .byte $7C,$7C,$7C,$7C,$7C,$7D,$7D,$7D,$7D,$7D,$7D,$7D,$7D,$7E,$7E,$7E
    .byte $7E,$7E,$7E,$7E,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F,$7F

; Table to translate 3+5 bit fixed point log2 back to an unsigned byte
tbl_pow2_b_b:
    .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
    .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
    .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
    .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
    .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
    .byte $00,$00,$00,$00,$00,$00,$01,$01,$01,$01,$01,$01,$01,$01,$01,$01
    .byte $01,$01,$01,$01,$01,$01,$01,$01,$01,$01,$01,$02,$02,$02,$02,$02
    .byte $02,$02,$02,$02,$02,$02,$02,$02,$03,$03,$03,$03,$03,$03,$03,$03
    .byte $04,$04,$04,$04,$04,$04,$04,$05,$05,$05,$05,$05,$05,$06,$06,$06
    .byte $06,$06,$07,$07,$07,$07,$08,$08,$08,$09,$09,$09,$09,$0A,$0A,$0A
    .byte $0B,$0B,$0C,$0C,$0C,$0D,$0D,$0E,$0E,$0F,$0F,$10,$10,$11,$11,$12
    .byte $13,$13,$14,$14,$15,$16,$17,$17,$18,$19,$1A,$1B,$1C,$1D,$1D,$1E
    .byte $20,$21,$22,$23,$24,$25,$26,$28,$29,$2A,$2C,$2D,$2F,$30,$32,$34
    .byte $35,$37,$39,$3B,$3D,$3F,$41,$43,$45,$48,$4A,$4C,$4F,$52,$54,$57
    .byte $5A,$5D,$60,$63,$67,$6A,$6D,$71,$75,$79,$7D,$81,$85,$8A,$8E,$93
    .byte $98,$9D,$A2,$A7,$AD,$B3,$B8,$BF,$C5,$CB,$D2,$D9,$E0,$E8,$EF,$F7

; Table to translate 8-bit mantissa to fractional part of log2
tbl_log2_w_w:
    .byte $00,$01,$02,$04,$05,$07,$08,$09,$0B,$0C,$0E,$0F,$10,$12,$13,$15
    .byte $16,$17,$19,$1A,$1B,$1D,$1E,$1F,$21,$22,$23,$25,$26,$27,$28,$2A
    .byte $2B,$2C,$2E,$2F,$30,$31,$33,$34,$35,$36,$38,$39,$3A,$3B,$3D,$3E
    .byte $3F,$40,$41,$43,$44,$45,$46,$47,$49,$4A,$4B,$4C,$4D,$4E,$50,$51
    .byte $52,$53,$54,$55,$57,$58,$59,$5A,$5B,$5C,$5D,$5E,$60,$61,$62,$63
    .byte $64,$65,$66,$67,$68,$69,$6A,$6C,$6D,$6E,$6F,$70,$71,$72,$73,$74
    .byte $75,$76,$77,$78,$79,$7A,$7B,$7C,$7D,$7E,$7F,$80,$81,$83,$84,$85
    .byte $86,$87,$88,$89,$8A,$8B,$8C,$8C,$8D,$8E,$8F,$90,$91,$92,$93,$94
    .byte $95,$96,$97,$98,$99,$9A,$9B,$9C,$9D,$9E,$9F,$A0,$A1,$A2,$A2,$A3
    .byte $A4,$A5,$A6,$A7,$A8,$A9,$AA,$AB,$AC,$AD,$AD,$AE,$AF,$B0,$B1,$B2
    .byte $B3,$B4,$B5,$B5,$B6,$B7,$B8,$B9,$BA,$BB,$BC,$BC,$BD,$BE,$BF,$C0
    .byte $C1,$C2,$C2,$C3,$C4,$C5,$C6,$C7,$C8,$C8,$C9,$CA,$CB,$CC,$CD,$CD
    .byte $CE,$CF,$D0,$D1,$D1,$D2,$D3,$D4,$D5,$D6,$D6,$D7,$D8,$D9,$DA,$DA
    .byte $DB,$DC,$DD,$DE,$DE,$DF,$E0,$E1,$E1,$E2,$E3,$E4,$E5,$E5,$E6,$E7
    .byte $E8,$E8,$E9,$EA,$EB,$EB,$EC,$ED,$EE,$EF,$EF,$F0,$F1,$F2,$F2,$F3
    .byte $F4,$F5,$F5,$F6,$F7,$F7,$F8,$F9,$FA,$FA,$FB,$FC,$FD,$FD,$FE,$FF

; Table to translate fractional part of log2 back to 8-bit mantissa
tbl_pow2_w_w:
    .byte $00,$00,$01,$02,$02,$03,$04,$04,$05,$06,$07,$07,$08,$09,$09,$0A
    .byte $0B,$0C,$0C,$0D,$0E,$0E,$0F,$10,$11,$11,$12,$13,$14,$14,$15,$16
    .byte $17,$17,$18,$19,$1A,$1A,$1B,$1C,$1D,$1E,$1E,$1F,$20,$21,$21,$22
    .byte $23,$24,$25,$25,$26,$27,$28,$29,$29,$2A,$2B,$2C,$2D,$2D,$2E,$2F
    .byte $30,$31,$32,$32,$33,$34,$35,$36,$37,$37,$38,$39,$3A,$3B,$3C,$3D
    .byte $3D,$3E,$3F,$40,$41,$42,$43,$44,$44,$45,$46,$47,$48,$49,$4A,$4B
    .byte $4C,$4C,$4D,$4E,$4F,$50,$51,$52,$53,$54,$55,$56,$57,$57,$58,$59
    .byte $5A,$5B,$5C,$5D,$5E,$5F,$60,$61,$62,$63,$64,$65,$66,$67,$68,$69
    .byte $6A,$6B,$6C,$6D,$6E,$6F,$70,$71,$72,$73,$74,$75,$76,$77,$78,$79
    .byte $7A,$7B,$7C,$7D,$7E,$7F,$80,$81,$82,$83,$84,$85,$86,$87,$88,$89
    .byte $8A,$8C,$8D,$8E,$8F,$90,$91,$92,$93,$94,$95,$96,$97,$99,$9A,$9B
    .byte $9C,$9D,$9E,$9F,$A0,$A2,$A3,$A4,$A5,$A6,$A7,$A8,$AA,$AB,$AC,$AD
    .byte $AE,$AF,$B1,$B2,$B3,$B4,$B5,$B6,$B8,$B9,$BA,$BB,$BC,$BE,$BF,$C0
    .byte $C1,$C3,$C4,$C5,$C6,$C7,$C9,$CA,$CB,$CC,$CE,$CF,$D0,$D1,$D3,$D4
    .byte $D5,$D7,$D8,$D9,$DA,$DC,$DD,$DE,$E0,$E1,$E2,$E4,$E5,$E6,$E7,$E9
    .byte $EA,$EB,$ED,$EE,$EF,$F1,$F2,$F4,$F5,$F6,$F8,$F9,$FA,$FC,$FD,$FF

; Precalculated ray initialization parameters. One table for each of the 16 angles.
; Each angle has 63 rays, and each ray is provided with 4 parameters (one byte each param):
; dirX, dirY, deltaX, deltaY. [ref BigBlue3_30]
precast_0:
    .byte $72,$C7,$3E,$7C
    .byte $72,$C9,$3D,$7E
    .byte $72,$CB,$2C,$5E
    .byte $72,$CD,$39,$7E
    .byte $72,$CE,$30,$6E
    .byte $72,$D0,$35,$7E
    .byte $72,$D2,$33,$7E
    .byte $72,$D4,$31,$7E
    .byte $72,$D6,$2F,$7E
    .byte $72,$D7,$28,$70
    .byte $72,$D9,$2B,$7E
    .byte $72,$DB,$29,$7E
    .byte $72,$DD,$27,$7E
    .byte $72,$DF,$25,$7E
    .byte $72,$E0,$23,$7E
    .byte $72,$E2,$21,$7E
    .byte $72,$E4,$1F,$7E
    .byte $72,$E6,$1D,$7E
    .byte $72,$E8,$18,$70
    .byte $72,$E9,$19,$7E
    .byte $72,$EB,$17,$7E
    .byte $72,$ED,$14,$78
    .byte $72,$EF,$13,$7E
    .byte $72,$F1,$11,$7E
    .byte $72,$F2,$0F,$7E
    .byte $72,$F4,$0D,$7E
    .byte $72,$F6,$0B,$7E
    .byte $72,$F8,$09,$7E
    .byte $72,$FA,$07,$7E
    .byte $72,$FB,$05,$7E
    .byte $72,$FD,$03,$7E
    .byte $72,$FF,$01,$7E
    .byte $72,$01,$01,$7F
    .byte $72,$03,$03,$7E
    .byte $72,$05,$04,$65
    .byte $72,$06,$07,$7E
    .byte $72,$08,$09,$7F
    .byte $72,$0A,$0B,$7E
    .byte $72,$0C,$0A,$61
    .byte $72,$0E,$0F,$7E
    .byte $72,$0F,$0C,$59
    .byte $72,$11,$13,$7E
    .byte $72,$13,$15,$7F
    .byte $72,$15,$17,$7E
    .byte $72,$17,$18,$79
    .byte $72,$18,$18,$70
    .byte $72,$1A,$1A,$71
    .byte $72,$1C,$1F,$7E
    .byte $72,$1E,$1C,$6B
    .byte $72,$20,$23,$7E
    .byte $72,$21,$20,$6D
    .byte $72,$23,$27,$7E
    .byte $72,$25,$1B,$53
    .byte $72,$27,$2B,$7E
    .byte $72,$29,$28,$70
    .byte $72,$2A,$2F,$7E
    .byte $72,$2C,$23,$5A
    .byte $72,$2E,$33,$7E
    .byte $72,$30,$2D,$6B
    .byte $72,$32,$30,$6E
    .byte $72,$33,$34,$73
    .byte $72,$35,$2C,$5E
    .byte $72,$37,$3D,$7E
    .res 4 ; to bring it up to 256 bytes per angle
precast_1:
    .byte $7F,$F7,$09,$7F
    .byte $7E,$F9,$05,$56
    .byte $7E,$FA,$05,$6F
    .byte $7D,$FC,$04,$7D
    .byte $7C,$FE,$02,$6A
    .byte $7B,$FF,$00,$7F
    .byte $7B,$01,$01,$7C
    .byte $7A,$03,$02,$5C
    .byte $79,$04,$04,$71
    .byte $79,$06,$06,$79
    .byte $78,$08,$06,$5E
    .byte $77,$09,$09,$73
    .byte $77,$0B,$09,$61
    .byte $76,$0D,$0D,$79
    .byte $75,$0E,$0B,$5A
    .byte $75,$10,$0E,$66
    .byte $74,$12,$12,$76
    .byte $73,$13,$15,$7D
    .byte $72,$15,$14,$6D
    .byte $72,$17,$19,$7E
    .byte $71,$18,$0E,$41
    .byte $70,$1A,$19,$6C
    .byte $70,$1C,$1D,$75
    .byte $6F,$1D,$20,$79
    .byte $6E,$1F,$1B,$60
    .byte $6E,$21,$22,$72
    .byte $6D,$22,$23,$6F
    .byte $6C,$24,$2A,$7F
    .byte $6C,$26,$22,$61
    .byte $6B,$27,$2A,$72
    .byte $6A,$29,$2E,$77
    .byte $69,$2B,$33,$7E
    .byte $69,$2C,$24,$55
    .byte $68,$2E,$2E,$68
    .byte $67,$30,$35,$73
    .byte $67,$31,$32,$68
    .byte $66,$33,$3F,$7E
    .byte $65,$35,$40,$7B
    .byte $65,$36,$43,$7C
    .byte $64,$38,$3C,$6B
    .byte $63,$3A,$39,$62
    .byte $63,$3B,$2F,$4E
    .byte $62,$3D,$49,$75
    .byte $61,$3F,$47,$6E
    .byte $60,$40,$54,$7E
    .byte $60,$42,$50,$74
    .byte $5F,$44,$2F,$42
    .byte $5E,$45,$56,$75
    .byte $5E,$47,$5B,$78
    .byte $5D,$49,$56,$6E
    .byte $5C,$4A,$53,$67
    .byte $5C,$4C,$53,$64
    .byte $5B,$4E,$64,$75
    .byte $5A,$4F,$5F,$6C
    .byte $5A,$51,$56,$5F
    .byte $59,$53,$6C,$74
    .byte $58,$54,$70,$75
    .byte $58,$56,$7A,$7C
    .byte $57,$58,$5C,$5B
    .byte $56,$59,$6C,$68
    .byte $55,$5B,$71,$6A
    .byte $55,$5D,$68,$5F
    .byte $54,$5E,$64,$59
    .res 4 ; to bring it up to 256 bytes per angle
precast_2:
    .byte $79,$28,$2A,$7F
    .byte $77,$2A,$29,$76
    .byte $76,$2B,$2A,$74
    .byte $75,$2C,$1A,$45
    .byte $74,$2D,$28,$66
    .byte $72,$2F,$2A,$67
    .byte $71,$30,$24,$55
    .byte $70,$31,$33,$74
    .byte $6E,$32,$2A,$5C
    .byte $6D,$34,$35,$70
    .byte $6C,$35,$37,$70
    .byte $6B,$36,$39,$70
    .byte $69,$38,$3A,$6E
    .byte $68,$39,$42,$79
    .byte $67,$3A,$41,$73
    .byte $66,$3B,$45,$76
    .byte $64,$3D,$48,$77
    .byte $63,$3E,$4B,$78
    .byte $62,$3F,$42,$66
    .byte $60,$41,$54,$7E
    .byte $5F,$42,$2F,$44
    .byte $5E,$43,$57,$7A
    .byte $5D,$44,$4C,$67
    .byte $5B,$46,$60,$7E
    .byte $5A,$47,$55,$6C
    .byte $59,$48,$5B,$70
    .byte $57,$49,$5E,$70
    .byte $56,$4B,$6E,$7F
    .byte $55,$4C,$6E,$7B
    .byte $54,$4D,$6C,$75
    .byte $52,$4F,$66,$6B
    .byte $51,$50,$7D,$7F
    .byte $50,$51,$40,$3F
    .byte $4F,$52,$6C,$67
    .byte $4D,$54,$76,$6D
    .byte $4C,$55,$72,$66
    .byte $4B,$56,$71,$62
    .byte $49,$57,$51,$44
    .byte $48,$59,$7B,$64
    .byte $47,$5A,$7F,$64
    .byte $46,$5B,$7A,$5D
    .byte $44,$5D,$7E,$5D
    .byte $43,$5E,$7B,$58
    .byte $42,$5F,$51,$38
    .byte $41,$60,$7F,$55
    .byte $3F,$62,$77,$4D
    .byte $3E,$63,$7B,$4D
    .byte $3D,$64,$72,$45
    .byte $3B,$66,$5E,$37
    .byte $3A,$67,$7A,$45
    .byte $39,$68,$77,$41
    .byte $38,$69,$5B,$30
    .byte $36,$6B,$6E,$38
    .byte $35,$6C,$72,$38
    .byte $34,$6D,$74,$37
    .byte $32,$6E,$7F,$3A
    .byte $31,$70,$6D,$30
    .byte $30,$71,$5C,$27
    .byte $2F,$72,$4C,$1F
    .byte $2D,$74,$4F,$1F
    .byte $2C,$75,$72,$2B
    .byte $2B,$76,$7F,$2E
    .byte $2A,$77,$73,$28
    .res 4 ; to bring it up to 256 bytes per angle
precast_3:
    .byte $60,$53,$62,$71
    .byte $5E,$54,$61,$6D
    .byte $5D,$55,$74,$7F
    .byte $5B,$55,$69,$70
    .byte $59,$56,$67,$6B
    .byte $58,$57,$5A,$5B
    .byte $56,$58,$7D,$7B
    .byte $54,$58,$5E,$5A
    .byte $53,$59,$66,$5F
    .byte $51,$5A,$4A,$43
    .byte $4F,$5A,$7D,$6E
    .byte $4E,$5B,$7C,$6A
    .byte $4C,$5C,$6A,$58
    .byte $4A,$5C,$43,$36
    .byte $49,$5D,$4E,$3D
    .byte $47,$5E,$74,$58
    .byte $45,$5E,$66,$4B
    .byte $44,$5F,$49,$34
    .byte $42,$60,$64,$45
    .byte $40,$60,$7F,$55
    .byte $3F,$61,$4F,$33
    .byte $3D,$62,$7D,$4E
    .byte $3B,$63,$49,$2C
    .byte $3A,$63,$6E,$40
    .byte $38,$64,$7B,$45
    .byte $36,$65,$6F,$3C
    .byte $35,$65,$62,$33
    .byte $33,$66,$7F,$40
    .byte $31,$67,$4F,$26
    .byte $30,$67,$75,$36
    .byte $2E,$68,$71,$32
    .byte $2C,$69,$76,$32
    .byte $2B,$69,$79,$31
    .byte $29,$6A,$4B,$1D
    .byte $27,$6B,$7A,$2D
    .byte $26,$6C,$4D,$1B
    .byte $24,$6C,$7E,$2A
    .byte $22,$6D,$72,$24
    .byte $21,$6E,$7C,$25
    .byte $1F,$6E,$67,$1D
    .byte $1D,$6F,$66,$1B
    .byte $1C,$70,$79,$1E
    .byte $1A,$70,$5F,$16
    .byte $18,$71,$74,$19
    .byte $17,$72,$7D,$19
    .byte $15,$72,$62,$12
    .byte $13,$73,$77,$14
    .byte $12,$74,$48,$0B
    .byte $10,$75,$50,$0B
    .byte $0E,$75,$62,$0C
    .byte $0D,$76,$5D,$0A
    .byte $0B,$77,$56,$08
    .byte $09,$77,$66,$08
    .byte $08,$78,$7D,$08
    .byte $06,$79,$78,$06
    .byte $04,$79,$70,$04
    .byte $03,$7A,$5B,$02
    .byte $01,$7B,$7B,$01
    .byte $FF,$7B,$5B,$00
    .byte $FE,$7C,$69,$02
    .byte $FC,$7D,$7C,$04
    .byte $FA,$7E,$6E,$05
    .byte $F9,$7E,$67,$06
    .res 4 ; to bring it up to 256 bytes per angle
precast_4:
    .byte $39,$72,$7F,$40
    .byte $37,$72,$5F,$2E
    .byte $35,$72,$7E,$3B
    .byte $33,$72,$5F,$2B
    .byte $32,$72,$47,$1F
    .byte $30,$72,$72,$30
    .byte $2E,$72,$54,$22
    .byte $2C,$72,$71,$2C
    .byte $2A,$72,$43,$19
    .byte $29,$72,$7B,$2C
    .byte $27,$72,$7B,$2A
    .byte $25,$72,$56,$1C
    .byte $23,$72,$71,$23
    .byte $21,$72,$77,$23
    .byte $20,$72,$73,$20
    .byte $1E,$72,$67,$1B
    .byte $1C,$72,$41,$10
    .byte $1A,$72,$75,$1B
    .byte $18,$72,$79,$1A
    .byte $17,$72,$7D,$19
    .byte $15,$72,$73,$15
    .byte $13,$72,$7D,$15
    .byte $11,$72,$6A,$10
    .byte $0F,$72,$6F,$0F
    .byte $0E,$72,$6D,$0D
    .byte $0C,$72,$74,$0C
    .byte $0A,$72,$67,$09
    .byte $08,$72,$7D,$09
    .byte $06,$72,$7D,$07
    .byte $05,$72,$7D,$05
    .byte $03,$72,$7D,$03
    .byte $01,$72,$7D,$01
    .byte $FF,$72,$7E,$01
    .byte $FD,$72,$7E,$03
    .byte $FB,$72,$7E,$05
    .byte $FA,$72,$7E,$07
    .byte $F8,$72,$7E,$09
    .byte $F6,$72,$7E,$0B
    .byte $F4,$72,$7E,$0D
    .byte $F2,$72,$7E,$0F
    .byte $F1,$72,$7E,$11
    .byte $EF,$72,$7E,$13
    .byte $ED,$72,$7E,$15
    .byte $EB,$72,$7E,$17
    .byte $E9,$72,$7E,$19
    .byte $E8,$72,$7E,$1B
    .byte $E6,$72,$7E,$1D
    .byte $E4,$72,$7E,$1F
    .byte $E2,$72,$7E,$21
    .byte $E0,$72,$7E,$23
    .byte $DF,$72,$7E,$25
    .byte $DD,$72,$7E,$27
    .byte $DB,$72,$7E,$29
    .byte $D9,$72,$7E,$2B
    .byte $D7,$72,$7E,$2D
    .byte $D6,$72,$7E,$2F
    .byte $D4,$72,$7E,$31
    .byte $D2,$72,$7E,$33
    .byte $D0,$72,$7E,$35
    .byte $CE,$72,$47,$1F
    .byte $CD,$72,$7E,$39
    .byte $CB,$72,$7E,$3B
    .byte $C9,$72,$7E,$3D
    .res 4 ; to bring it up to 256 bytes per angle
precast_5:
    .byte $09,$7F,$7E,$09
    .byte $07,$7E,$67,$06
    .byte $06,$7E,$6E,$05
    .byte $04,$7D,$7C,$04
    .byte $02,$7C,$69,$02
    .byte $01,$7B,$5B,$00
    .byte $FF,$7B,$7B,$01
    .byte $FD,$7A,$5B,$02
    .byte $FC,$79,$70,$04
    .byte $FA,$79,$78,$06
    .byte $F8,$78,$7D,$08
    .byte $F7,$77,$66,$08
    .byte $F5,$77,$56,$08
    .byte $F3,$76,$5D,$0A
    .byte $F2,$75,$62,$0C
    .byte $F0,$75,$50,$0B
    .byte $EE,$74,$48,$0B
    .byte $ED,$73,$77,$14
    .byte $EB,$72,$62,$12
    .byte $E9,$72,$7D,$19
    .byte $E8,$71,$74,$19
    .byte $E6,$70,$5F,$16
    .byte $E4,$70,$79,$1E
    .byte $E3,$6F,$66,$1B
    .byte $E1,$6E,$67,$1D
    .byte $DF,$6E,$7C,$25
    .byte $DE,$6D,$72,$24
    .byte $DC,$6C,$7E,$2A
    .byte $DA,$6C,$4D,$1B
    .byte $D9,$6B,$7A,$2D
    .byte $D7,$6A,$4B,$1D
    .byte $D5,$69,$79,$31
    .byte $D4,$69,$76,$32
    .byte $D2,$68,$71,$32
    .byte $D0,$67,$75,$36
    .byte $CF,$67,$4F,$26
    .byte $CD,$66,$7F,$40
    .byte $CB,$65,$62,$33
    .byte $CA,$65,$6F,$3C
    .byte $C8,$64,$7B,$45
    .byte $C6,$63,$6E,$40
    .byte $C5,$63,$49,$2C
    .byte $C3,$62,$7D,$4E
    .byte $C1,$61,$4F,$33
    .byte $C0,$60,$7F,$55
    .byte $BE,$60,$64,$45
    .byte $BC,$5F,$49,$34
    .byte $BB,$5E,$66,$4B
    .byte $B9,$5E,$74,$58
    .byte $B7,$5D,$4E,$3D
    .byte $B6,$5C,$43,$36
    .byte $B4,$5C,$6A,$58
    .byte $B2,$5B,$7C,$6A
    .byte $B1,$5A,$7D,$6E
    .byte $AF,$5A,$4A,$43
    .byte $AD,$59,$66,$5F
    .byte $AC,$58,$5E,$5A
    .byte $AA,$58,$7D,$7B
    .byte $A8,$57,$5A,$5B
    .byte $A7,$56,$67,$6B
    .byte $A5,$55,$69,$70
    .byte $A3,$55,$74,$7F
    .byte $A2,$54,$61,$6D
    .res 4 ; to bring it up to 256 bytes per angle
precast_6:
    .byte $D8,$79,$7E,$2A
    .byte $D6,$77,$73,$28
    .byte $D5,$76,$7F,$2E
    .byte $D4,$75,$7A,$2E
    .byte $D3,$74,$4F,$1F
    .byte $D1,$72,$4C,$1F
    .byte $D0,$71,$76,$32
    .byte $CF,$70,$7D,$37
    .byte $CE,$6E,$7F,$3A
    .byte $CC,$6D,$72,$36
    .byte $CB,$6C,$72,$38
    .byte $CA,$6B,$6E,$38
    .byte $C8,$69,$6E,$3A
    .byte $C7,$68,$77,$41
    .byte $C6,$67,$73,$41
    .byte $C5,$66,$6A,$3E
    .byte $C3,$64,$72,$45
    .byte $C2,$63,$7B,$4D
    .byte $C1,$62,$77,$4D
    .byte $BF,$60,$7F,$55
    .byte $BE,$5F,$51,$38
    .byte $BD,$5E,$7E,$5A
    .byte $BC,$5D,$7E,$5D
    .byte $BA,$5B,$7A,$5D
    .byte $B9,$5A,$5E,$4A
    .byte $B8,$59,$7B,$64
    .byte $B7,$57,$51,$44
    .byte $B5,$56,$78,$68
    .byte $B4,$55,$72,$66
    .byte $B3,$54,$76,$6D
    .byte $B1,$52,$56,$52
    .byte $B0,$51,$7F,$7D
    .byte $AF,$50,$7D,$7F
    .byte $AE,$4F,$52,$56
    .byte $AC,$4D,$6C,$75
    .byte $AB,$4C,$66,$72
    .byte $AA,$4B,$68,$78
    .byte $A9,$49,$5E,$70
    .byte $A7,$48,$5B,$70
    .byte $A6,$47,$4A,$5E
    .byte $A5,$46,$60,$7E
    .byte $A3,$44,$4C,$67
    .byte $A2,$43,$5A,$7E
    .byte $A1,$42,$2F,$44
    .byte $A0,$41,$54,$7E
    .byte $9E,$3F,$4D,$77
    .byte $9D,$3E,$4B,$78
    .byte $9C,$3D,$48,$77
    .byte $9A,$3B,$3E,$6A
    .byte $99,$3A,$41,$73
    .byte $98,$39,$42,$79
    .byte $97,$38,$3A,$6E
    .byte $95,$36,$39,$70
    .byte $94,$35,$37,$70
    .byte $93,$34,$36,$72
    .byte $92,$32,$2A,$5C
    .byte $90,$31,$37,$7D
    .byte $8F,$30,$32,$76
    .byte $8E,$2F,$2A,$67
    .byte $8C,$2D,$28,$66
    .byte $8B,$2C,$2E,$7A
    .byte $8A,$2B,$2A,$74
    .byte $89,$2A,$29,$76
    .res 4 ; to bring it up to 256 bytes per angle
precast_7:
    .byte $AD,$60,$62,$55
    .byte $AC,$5E,$64,$59
    .byte $AB,$5D,$68,$5F
    .byte $AB,$5B,$71,$6A
    .byte $AA,$59,$6C,$68
    .byte $A9,$58,$5C,$5B
    .byte $A8,$56,$7A,$7C
    .byte $A8,$54,$70,$75
    .byte $A7,$53,$6C,$74
    .byte $A6,$51,$56,$5F
    .byte $A6,$4F,$5F,$6C
    .byte $A5,$4E,$64,$75
    .byte $A4,$4C,$53,$64
    .byte $A4,$4A,$53,$67
    .byte $A3,$49,$56,$6E
    .byte $A2,$47,$5B,$78
    .byte $A2,$45,$56,$75
    .byte $A1,$44,$2F,$42
    .byte $A0,$42,$50,$74
    .byte $A0,$40,$54,$7E
    .byte $9F,$3F,$47,$6E
    .byte $9E,$3D,$49,$75
    .byte $9D,$3B,$2F,$4E
    .byte $9D,$3A,$39,$62
    .byte $9C,$38,$3C,$6B
    .byte $9B,$36,$43,$7C
    .byte $9B,$35,$40,$7B
    .byte $9A,$33,$3F,$7E
    .byte $99,$31,$32,$68
    .byte $99,$30,$35,$73
    .byte $98,$2E,$2E,$68
    .byte $97,$2C,$24,$55
    .byte $97,$2B,$33,$7E
    .byte $96,$29,$2E,$77
    .byte $95,$27,$2A,$72
    .byte $94,$26,$22,$61
    .byte $94,$24,$2A,$7F
    .byte $93,$22,$23,$6F
    .byte $92,$21,$22,$72
    .byte $92,$1F,$1B,$60
    .byte $91,$1D,$20,$79
    .byte $90,$1C,$1D,$75
    .byte $90,$1A,$19,$6C
    .byte $8F,$18,$0E,$41
    .byte $8E,$17,$19,$7E
    .byte $8E,$15,$14,$6D
    .byte $8D,$13,$15,$7D
    .byte $8C,$12,$12,$76
    .byte $8B,$10,$0E,$66
    .byte $8B,$0E,$0B,$5A
    .byte $8A,$0D,$0D,$79
    .byte $89,$0B,$09,$61
    .byte $89,$09,$09,$73
    .byte $88,$08,$06,$5E
    .byte $87,$06,$06,$79
    .byte $87,$04,$04,$71
    .byte $86,$03,$02,$5C
    .byte $85,$01,$01,$7C
    .byte $85,$FF,$00,$7F
    .byte $84,$FE,$02,$6A
    .byte $83,$FC,$04,$7D
    .byte $82,$FA,$05,$6F
    .byte $82,$F9,$05,$56
    .res 4 ; to bring it up to 256 bytes per angle
precast_8:
    .byte $8E,$39,$3F,$7E
    .byte $8E,$37,$3D,$7E
    .byte $8E,$35,$3B,$7E
    .byte $8E,$33,$39,$7E
    .byte $8E,$32,$37,$7E
    .byte $8E,$30,$35,$7E
    .byte $8E,$2E,$33,$7E
    .byte $8E,$2C,$31,$7E
    .byte $8E,$2A,$2F,$7E
    .byte $8E,$29,$2D,$7E
    .byte $8E,$27,$2B,$7E
    .byte $8E,$25,$29,$7E
    .byte $8E,$23,$27,$7E
    .byte $8E,$21,$25,$7E
    .byte $8E,$20,$23,$7E
    .byte $8E,$1E,$21,$7E
    .byte $8E,$1C,$1F,$7E
    .byte $8E,$1A,$1D,$7E
    .byte $8E,$18,$1B,$7E
    .byte $8E,$17,$19,$7E
    .byte $8E,$15,$17,$7E
    .byte $8E,$13,$15,$7E
    .byte $8E,$11,$13,$7E
    .byte $8E,$0F,$11,$7E
    .byte $8E,$0E,$0F,$7E
    .byte $8E,$0C,$0D,$7E
    .byte $8E,$0A,$0B,$7E
    .byte $8E,$08,$09,$7E
    .byte $8E,$06,$07,$7E
    .byte $8E,$05,$05,$7E
    .byte $8E,$03,$03,$7E
    .byte $8E,$01,$01,$7E
    .byte $8E,$FF,$01,$7F
    .byte $8E,$FD,$03,$7F
    .byte $8E,$FB,$04,$65
    .byte $8E,$FA,$07,$7F
    .byte $8E,$F8,$09,$7F
    .byte $8E,$F6,$0A,$73
    .byte $8E,$F4,$0A,$61
    .byte $8E,$F2,$0C,$65
    .byte $8E,$F1,$0C,$59
    .byte $8E,$EF,$0B,$49
    .byte $8E,$ED,$15,$7F
    .byte $8E,$EB,$16,$79
    .byte $8E,$E9,$18,$79
    .byte $8E,$E8,$19,$75
    .byte $8E,$E6,$1A,$71
    .byte $8E,$E4,$1E,$7A
    .byte $8E,$E2,$1C,$6B
    .byte $8E,$E0,$21,$77
    .byte $8E,$DF,$20,$6D
    .byte $8E,$DD,$1E,$61
    .byte $8E,$DB,$1B,$53
    .byte $8E,$D9,$1D,$55
    .byte $8E,$D7,$29,$73
    .byte $8E,$D6,$2C,$76
    .byte $8E,$D4,$2F,$79
    .byte $8E,$D2,$24,$59
    .byte $8E,$D0,$2D,$6B
    .byte $8E,$CE,$30,$6E
    .byte $8E,$CD,$34,$73
    .byte $8E,$CB,$2C,$5E
    .byte $8E,$C9,$3C,$7C
    .res 4 ; to bring it up to 256 bytes per angle
precast_9:
    .byte $81,$09,$09,$7F
    .byte $82,$07,$05,$56
    .byte $82,$06,$05,$6F
    .byte $83,$04,$04,$7D
    .byte $84,$02,$02,$6A
    .byte $85,$01,$00,$7F
    .byte $85,$FF,$01,$7C
    .byte $86,$FD,$02,$5C
    .byte $87,$FC,$04,$71
    .byte $87,$FA,$06,$79
    .byte $88,$F8,$06,$5E
    .byte $89,$F7,$09,$73
    .byte $89,$F5,$09,$61
    .byte $8A,$F3,$0D,$79
    .byte $8B,$F2,$0B,$5A
    .byte $8B,$F0,$0E,$66
    .byte $8C,$EE,$12,$76
    .byte $8D,$ED,$15,$7D
    .byte $8E,$EB,$14,$6D
    .byte $8E,$E9,$19,$7E
    .byte $8F,$E8,$0E,$41
    .byte $90,$E6,$19,$6C
    .byte $90,$E4,$1D,$75
    .byte $91,$E3,$20,$79
    .byte $92,$E1,$1B,$60
    .byte $92,$DF,$22,$72
    .byte $93,$DE,$23,$6F
    .byte $94,$DC,$2A,$7F
    .byte $94,$DA,$22,$61
    .byte $95,$D9,$2A,$72
    .byte $96,$D7,$2E,$77
    .byte $97,$D5,$33,$7E
    .byte $97,$D4,$24,$55
    .byte $98,$D2,$2E,$68
    .byte $99,$D0,$35,$73
    .byte $99,$CF,$32,$68
    .byte $9A,$CD,$3F,$7E
    .byte $9B,$CB,$40,$7B
    .byte $9B,$CA,$43,$7C
    .byte $9C,$C8,$3C,$6B
    .byte $9D,$C6,$39,$62
    .byte $9D,$C5,$2F,$4E
    .byte $9E,$C3,$49,$75
    .byte $9F,$C1,$47,$6E
    .byte $A0,$C0,$54,$7E
    .byte $A0,$BE,$50,$74
    .byte $A1,$BC,$2F,$42
    .byte $A2,$BB,$56,$75
    .byte $A2,$B9,$5B,$78
    .byte $A3,$B7,$56,$6E
    .byte $A4,$B6,$53,$67
    .byte $A4,$B4,$53,$64
    .byte $A5,$B2,$64,$75
    .byte $A6,$B1,$5F,$6C
    .byte $A6,$AF,$56,$5F
    .byte $A7,$AD,$6C,$74
    .byte $A8,$AC,$70,$75
    .byte $A8,$AA,$7A,$7C
    .byte $A9,$A8,$5C,$5B
    .byte $AA,$A7,$6C,$68
    .byte $AB,$A5,$71,$6A
    .byte $AB,$A3,$68,$5F
    .byte $AC,$A2,$64,$59
    .res 4 ; to bring it up to 256 bytes per angle
precast_10:
    .byte $87,$D8,$2A,$7F
    .byte $89,$D6,$29,$76
    .byte $8A,$D5,$2A,$74
    .byte $8B,$D4,$1A,$45
    .byte $8C,$D3,$28,$66
    .byte $8E,$D1,$2A,$67
    .byte $8F,$D0,$24,$55
    .byte $90,$CF,$33,$74
    .byte $92,$CE,$2A,$5C
    .byte $93,$CC,$35,$70
    .byte $94,$CB,$37,$70
    .byte $95,$CA,$39,$70
    .byte $97,$C8,$27,$4A
    .byte $98,$C7,$42,$79
    .byte $99,$C6,$3D,$6C
    .byte $9A,$C5,$45,$76
    .byte $9C,$C3,$48,$77
    .byte $9D,$C2,$4B,$78
    .byte $9E,$C1,$4B,$74
    .byte $A0,$BF,$54,$7E
    .byte $A1,$BE,$2F,$44
    .byte $A2,$BD,$57,$7A
    .byte $A3,$BC,$4C,$67
    .byte $A5,$BA,$60,$7E
    .byte $A6,$B9,$55,$6C
    .byte $A7,$B8,$5B,$70
    .byte $A9,$B7,$5E,$70
    .byte $AA,$B5,$6E,$7F
    .byte $AB,$B4,$6E,$7B
    .byte $AC,$B3,$6C,$75
    .byte $AE,$B1,$66,$6B
    .byte $AF,$B0,$7C,$7E
    .byte $B0,$AF,$40,$3F
    .byte $B1,$AE,$6C,$67
    .byte $B3,$AC,$76,$6D
    .byte $B4,$AB,$7C,$6F
    .byte $B5,$AA,$71,$62
    .byte $B7,$A9,$51,$44
    .byte $B8,$A7,$7B,$64
    .byte $B9,$A6,$7F,$64
    .byte $BA,$A5,$7A,$5D
    .byte $BC,$A3,$7E,$5D
    .byte $BD,$A2,$7B,$58
    .byte $BE,$A1,$51,$38
    .byte $BF,$A0,$7F,$55
    .byte $C1,$9E,$7A,$4F
    .byte $C2,$9D,$7B,$4D
    .byte $C3,$9C,$72,$45
    .byte $C5,$9A,$5E,$37
    .byte $C6,$99,$7A,$45
    .byte $C7,$98,$77,$41
    .byte $C8,$97,$5B,$30
    .byte $CA,$95,$6E,$38
    .byte $CB,$94,$72,$38
    .byte $CC,$93,$74,$37
    .byte $CE,$92,$7F,$3A
    .byte $CF,$90,$6D,$30
    .byte $D0,$8F,$5C,$27
    .byte $D1,$8E,$4C,$1F
    .byte $D3,$8C,$4F,$1F
    .byte $D4,$8B,$72,$2B
    .byte $D5,$8A,$7F,$2E
    .byte $D6,$89,$73,$28
    .res 4 ; to bring it up to 256 bytes per angle
precast_11:
    .byte $A0,$AD,$62,$71
    .byte $A2,$AC,$61,$6D
    .byte $A3,$AB,$74,$7F
    .byte $A5,$AB,$69,$70
    .byte $A7,$AA,$67,$6B
    .byte $A8,$A9,$5A,$5B
    .byte $AA,$A8,$7D,$7B
    .byte $AC,$A8,$5E,$5A
    .byte $AD,$A7,$66,$5F
    .byte $AF,$A6,$4A,$43
    .byte $B1,$A6,$7D,$6E
    .byte $B2,$A5,$7C,$6A
    .byte $B4,$A4,$6A,$58
    .byte $B6,$A4,$43,$36
    .byte $B7,$A3,$4E,$3D
    .byte $B9,$A2,$74,$58
    .byte $BB,$A2,$66,$4B
    .byte $BC,$A1,$49,$34
    .byte $BE,$A0,$64,$45
    .byte $C0,$A0,$7F,$55
    .byte $C1,$9F,$4F,$33
    .byte $C3,$9E,$7D,$4E
    .byte $C5,$9D,$49,$2C
    .byte $C6,$9D,$6E,$40
    .byte $C8,$9C,$7B,$45
    .byte $CA,$9B,$6F,$3C
    .byte $CB,$9B,$62,$33
    .byte $CD,$9A,$7F,$40
    .byte $CF,$99,$4F,$26
    .byte $D0,$99,$75,$36
    .byte $D2,$98,$71,$32
    .byte $D4,$97,$76,$32
    .byte $D5,$97,$79,$31
    .byte $D7,$96,$4B,$1D
    .byte $D9,$95,$7A,$2D
    .byte $DA,$94,$4D,$1B
    .byte $DC,$94,$7E,$2A
    .byte $DE,$93,$72,$24
    .byte $DF,$92,$7C,$25
    .byte $E1,$92,$67,$1D
    .byte $E3,$91,$66,$1B
    .byte $E4,$90,$79,$1E
    .byte $E6,$90,$5F,$16
    .byte $E8,$8F,$74,$19
    .byte $E9,$8E,$7D,$19
    .byte $EB,$8E,$62,$12
    .byte $ED,$8D,$77,$14
    .byte $EE,$8C,$48,$0B
    .byte $F0,$8B,$50,$0B
    .byte $F2,$8B,$62,$0C
    .byte $F3,$8A,$5D,$0A
    .byte $F5,$89,$56,$08
    .byte $F7,$89,$66,$08
    .byte $F8,$88,$7D,$08
    .byte $FA,$87,$78,$06
    .byte $FC,$87,$70,$04
    .byte $FD,$86,$5B,$02
    .byte $FF,$85,$7B,$01
    .byte $01,$85,$5B,$00
    .byte $02,$84,$69,$02
    .byte $04,$83,$7C,$04
    .byte $06,$82,$6E,$05
    .byte $07,$82,$67,$06
    .res 4 ; to bring it up to 256 bytes per angle
precast_12:
    .byte $C7,$8E,$7F,$40
    .byte $C9,$8E,$5F,$2E
    .byte $CB,$8E,$4F,$25
    .byte $CD,$8E,$5F,$2B
    .byte $CE,$8E,$47,$1F
    .byte $D0,$8E,$72,$30
    .byte $D2,$8E,$79,$31
    .byte $D4,$8E,$71,$2C
    .byte $D6,$8E,$43,$19
    .byte $D7,$8E,$7B,$2C
    .byte $D9,$8E,$7B,$2A
    .byte $DB,$8E,$56,$1C
    .byte $DD,$8E,$71,$23
    .byte $DF,$8E,$77,$23
    .byte $E0,$8E,$73,$20
    .byte $E2,$8E,$67,$1B
    .byte $E4,$8E,$41,$10
    .byte $E6,$8E,$75,$1B
    .byte $E8,$8E,$79,$1A
    .byte $E9,$8E,$7D,$19
    .byte $EB,$8E,$73,$15
    .byte $ED,$8E,$7D,$15
    .byte $EF,$8E,$6A,$10
    .byte $F1,$8E,$6F,$0F
    .byte $F2,$8E,$6D,$0D
    .byte $F4,$8E,$74,$0C
    .byte $F6,$8E,$67,$09
    .byte $F8,$8E,$7D,$09
    .byte $FA,$8E,$7D,$07
    .byte $FB,$8E,$7D,$05
    .byte $FD,$8E,$7D,$03
    .byte $FF,$8E,$7D,$01
    .byte $01,$8E,$7E,$01
    .byte $03,$8E,$7E,$03
    .byte $05,$8E,$7E,$05
    .byte $06,$8E,$7E,$07
    .byte $08,$8E,$7E,$09
    .byte $0A,$8E,$7E,$0B
    .byte $0C,$8E,$7E,$0D
    .byte $0E,$8E,$7E,$0F
    .byte $0F,$8E,$7E,$11
    .byte $11,$8E,$7E,$13
    .byte $13,$8E,$7E,$15
    .byte $15,$8E,$7E,$17
    .byte $17,$8E,$7E,$19
    .byte $18,$8E,$7E,$1B
    .byte $1A,$8E,$7E,$1D
    .byte $1C,$8E,$7E,$1F
    .byte $1E,$8E,$7E,$21
    .byte $20,$8E,$7E,$23
    .byte $21,$8E,$7E,$25
    .byte $23,$8E,$7E,$27
    .byte $25,$8E,$7E,$29
    .byte $27,$8E,$7E,$2B
    .byte $29,$8E,$7E,$2D
    .byte $2A,$8E,$7E,$2F
    .byte $2C,$8E,$7E,$31
    .byte $2E,$8E,$7E,$33
    .byte $30,$8E,$7E,$35
    .byte $32,$8E,$7E,$37
    .byte $33,$8E,$7E,$39
    .byte $35,$8E,$7E,$3B
    .byte $37,$8E,$7E,$3D
    .res 4 ; to bring it up to 256 bytes per angle
precast_13:
    .byte $F7,$81,$7E,$09
    .byte $F9,$82,$67,$06
    .byte $FA,$82,$6E,$05
    .byte $FC,$83,$7C,$04
    .byte $FE,$84,$69,$02
    .byte $FF,$85,$5B,$00
    .byte $01,$85,$7B,$01
    .byte $03,$86,$5B,$02
    .byte $04,$87,$70,$04
    .byte $06,$87,$78,$06
    .byte $08,$88,$7D,$08
    .byte $09,$89,$66,$08
    .byte $0B,$89,$56,$08
    .byte $0D,$8A,$5D,$0A
    .byte $0E,$8B,$62,$0C
    .byte $10,$8B,$50,$0B
    .byte $12,$8C,$48,$0B
    .byte $13,$8D,$77,$14
    .byte $15,$8E,$62,$12
    .byte $17,$8E,$7D,$19
    .byte $18,$8F,$74,$19
    .byte $1A,$90,$5F,$16
    .byte $1C,$90,$79,$1E
    .byte $1D,$91,$66,$1B
    .byte $1F,$92,$67,$1D
    .byte $21,$92,$7C,$25
    .byte $22,$93,$72,$24
    .byte $24,$94,$7E,$2A
    .byte $26,$94,$4D,$1B
    .byte $27,$95,$7A,$2D
    .byte $29,$96,$4B,$1D
    .byte $2B,$97,$79,$31
    .byte $2C,$97,$76,$32
    .byte $2E,$98,$71,$32
    .byte $30,$99,$75,$36
    .byte $31,$99,$4F,$26
    .byte $33,$9A,$7F,$40
    .byte $35,$9B,$62,$33
    .byte $36,$9B,$6F,$3C
    .byte $38,$9C,$7B,$45
    .byte $3A,$9D,$6E,$40
    .byte $3B,$9D,$49,$2C
    .byte $3D,$9E,$7D,$4E
    .byte $3F,$9F,$4F,$33
    .byte $40,$A0,$7F,$55
    .byte $42,$A0,$64,$45
    .byte $44,$A1,$49,$34
    .byte $45,$A2,$66,$4B
    .byte $47,$A2,$74,$58
    .byte $49,$A3,$4E,$3D
    .byte $4A,$A4,$43,$36
    .byte $4C,$A4,$6A,$58
    .byte $4E,$A5,$7C,$6A
    .byte $4F,$A6,$7D,$6E
    .byte $51,$A6,$4A,$43
    .byte $53,$A7,$66,$5F
    .byte $54,$A8,$5E,$5A
    .byte $56,$A8,$7D,$7B
    .byte $58,$A9,$5A,$5B
    .byte $59,$AA,$67,$6B
    .byte $5B,$AB,$69,$70
    .byte $5D,$AB,$74,$7F
    .byte $5E,$AC,$61,$6D
    .res 4 ; to bring it up to 256 bytes per angle
precast_14:
    .byte $28,$87,$7E,$2A
    .byte $2A,$89,$73,$28
    .byte $2B,$8A,$7F,$2E
    .byte $2C,$8B,$7A,$2E
    .byte $2D,$8C,$4F,$1F
    .byte $2F,$8E,$4C,$1F
    .byte $30,$8F,$76,$32
    .byte $31,$90,$7D,$37
    .byte $32,$92,$7F,$3A
    .byte $34,$93,$72,$36
    .byte $35,$94,$72,$38
    .byte $36,$95,$6E,$38
    .byte $38,$97,$6E,$3A
    .byte $39,$98,$77,$41
    .byte $3A,$99,$73,$41
    .byte $3B,$9A,$6A,$3E
    .byte $3D,$9C,$72,$45
    .byte $3E,$9D,$7B,$4D
    .byte $3F,$9E,$77,$4D
    .byte $41,$A0,$7F,$55
    .byte $42,$A1,$51,$38
    .byte $43,$A2,$7E,$5A
    .byte $44,$A3,$7E,$5D
    .byte $46,$A5,$7A,$5D
    .byte $47,$A6,$5E,$4A
    .byte $48,$A7,$7B,$64
    .byte $49,$A9,$51,$44
    .byte $4B,$AA,$78,$68
    .byte $4C,$AB,$72,$66
    .byte $4D,$AC,$76,$6D
    .byte $4F,$AE,$56,$52
    .byte $50,$AF,$7F,$7D
    .byte $51,$B0,$7D,$7F
    .byte $52,$B1,$52,$56
    .byte $54,$B3,$6C,$75
    .byte $55,$B4,$66,$72
    .byte $56,$B5,$68,$78
    .byte $57,$B7,$5E,$70
    .byte $59,$B8,$5B,$70
    .byte $5A,$B9,$4A,$5E
    .byte $5B,$BA,$60,$7E
    .byte $5D,$BC,$4C,$67
    .byte $5E,$BD,$5A,$7E
    .byte $5F,$BE,$2F,$44
    .byte $60,$BF,$54,$7E
    .byte $62,$C1,$4D,$77
    .byte $63,$C2,$4B,$78
    .byte $64,$C3,$48,$77
    .byte $66,$C5,$3E,$6A
    .byte $67,$C6,$41,$73
    .byte $68,$C7,$42,$79
    .byte $69,$C8,$3A,$6E
    .byte $6B,$CA,$39,$70
    .byte $6C,$CB,$37,$70
    .byte $6D,$CC,$36,$72
    .byte $6E,$CE,$2A,$5C
    .byte $70,$CF,$37,$7D
    .byte $71,$D0,$32,$76
    .byte $72,$D1,$2A,$67
    .byte $74,$D3,$28,$66
    .byte $75,$D4,$2E,$7A
    .byte $76,$D5,$2A,$74
    .byte $77,$D6,$29,$76
    .res 4 ; to bring it up to 256 bytes per angle
precast_15:
    .byte $53,$A0,$62,$55
    .byte $54,$A2,$64,$59
    .byte $55,$A3,$68,$5F
    .byte $55,$A5,$71,$6A
    .byte $56,$A7,$6C,$68
    .byte $57,$A8,$5C,$5B
    .byte $58,$AA,$7A,$7C
    .byte $58,$AC,$70,$75
    .byte $59,$AD,$6C,$74
    .byte $5A,$AF,$56,$5F
    .byte $5A,$B1,$5F,$6C
    .byte $5B,$B2,$64,$75
    .byte $5C,$B4,$53,$64
    .byte $5C,$B6,$53,$67
    .byte $5D,$B7,$56,$6E
    .byte $5E,$B9,$5B,$78
    .byte $5E,$BB,$56,$75
    .byte $5F,$BC,$2F,$42
    .byte $60,$BE,$50,$74
    .byte $60,$C0,$54,$7E
    .byte $61,$C1,$47,$6E
    .byte $62,$C3,$49,$75
    .byte $63,$C5,$2F,$4E
    .byte $63,$C6,$39,$62
    .byte $64,$C8,$3C,$6B
    .byte $65,$CA,$43,$7C
    .byte $65,$CB,$40,$7B
    .byte $66,$CD,$3F,$7E
    .byte $67,$CF,$32,$68
    .byte $67,$D0,$35,$73
    .byte $68,$D2,$2E,$68
    .byte $69,$D4,$24,$55
    .byte $69,$D5,$33,$7E
    .byte $6A,$D7,$2E,$77
    .byte $6B,$D9,$2A,$72
    .byte $6C,$DA,$22,$61
    .byte $6C,$DC,$2A,$7F
    .byte $6D,$DE,$23,$6F
    .byte $6E,$DF,$22,$72
    .byte $6E,$E1,$1B,$60
    .byte $6F,$E3,$20,$79
    .byte $70,$E4,$1D,$75
    .byte $70,$E6,$19,$6C
    .byte $71,$E8,$0E,$41
    .byte $72,$E9,$19,$7E
    .byte $72,$EB,$14,$6D
    .byte $73,$ED,$15,$7D
    .byte $74,$EE,$12,$76
    .byte $75,$F0,$0E,$66
    .byte $75,$F2,$0B,$5A
    .byte $76,$F3,$0D,$79
    .byte $77,$F5,$09,$61
    .byte $77,$F7,$09,$73
    .byte $78,$F8,$06,$5E
    .byte $79,$FA,$06,$79
    .byte $79,$FC,$04,$71
    .byte $7A,$FD,$02,$5C
    .byte $7B,$FF,$01,$7C
    .byte $7B,$01,$00,$7F
    .byte $7C,$02,$02,$6A
    .byte $7D,$04,$04,$7D
    .byte $7E,$06,$05,$6F
    .byte $7E,$07,$05,$56
    .res 4 ; to bring it up to 256 bytes per angle

wLog256:      .word $0800
wLogViewDist: .word $0E3F

; Movement amounts when walking at each angle
; Each entry consists of an X bump and a Y bump, in 8.8 fixed point
walkDirs:
    .word $0040, $0000
    .word $003B, $0018
    .word $002D, $002D
    .word $0018, $003B
    .word $0000, $0040
    .word $FFE8, $003B
    .word $FFD3, $002D
    .word $FFC5, $0018
    .word $FFC0, $0000
    .word $FFC5, $FFE8
    .word $FFD3, $FFD3
    .word $FFE8, $FFC5
    .word $0000, $FFC0
    .word $0018, $FFC5
    .word $002D, $FFD3
    .word $003B, $FFE8

; Sin of each angle, in log8.8 format plus the high bit being the sign (0x8000 = negative)
sinTbl:
    .word $0000, $8699, $877F, $87E1, $8800, $87E1, $877F, $8699
    .word $8195, $0699, $077F, $07E1, $0800, $07E1, $077F, $0699

