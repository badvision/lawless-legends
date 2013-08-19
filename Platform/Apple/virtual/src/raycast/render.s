
CODEBEG = *

    .pc02 ; Enable 65c02 ops

; This code is written bottom-up. That is,
; simple routines first, then routines that
; call those to build complexity. The main
; code is at the very end. We jump to it now.
    JMP TEST

; Conditional assembly flags
DBLBUF = 0 ; whether to double-buffer
DEBUG = 0 ; turn on verbose logging

; Constants
TOPLINE = $2000
NLINES = 126
SKYCOL = $11 ; blue
GROUNDCOL = $2 ; orange / black

; My zero page
LINECT = $3 ; len 1
BUMP = $4 ; len 1
TXCOLNUM = $5 ; len 1
PLINE = $6 ; len 2
PDST = $8 ; len 2
PTEX = $A ; len 2
PBUMP = $C ; len 2
PIXNUM = $E ; len 1
BYTENUM = $F ; len 1
PTMP = $10 ; len 2
BACKBUF = $12 ; len 1 (value 0 or 1)
FRONTBUF = $13 ; len 1 (value 0 or 1)
PCAST = $14 ; len 2

; Monitor zero page
STARTADDR = $3C
ENDADDR = $3E
DESTADDR = $42

; Place to stick ProDOS names temporarily
NAMEBUF = $280

; Tables and buffers
SH0101 = $1000
SH0123 = $1100
SH0145 = $1200
SH0156 = $1300
SH0157 = $1400
SH4501 = $1500
SH4523 = $1600
SH4545 = $1700
SH4556 = $1800
SH4557 = $1900
BLITIDXL = $1A00 ; size $80
BLITIDXH = $1A80 ; size $80
DCMIDXL = $1B00 ; size $40 (one entry per two lines)
DCMIDXH = $1B40 ; size $40
X1B80 = $1B80 ; unused
DCMROLL = $1C00 ; size 11*(126/2) = 693 = $2B5, plus 1 for rts
CBLITROLL = $1F00 ; size 3*(126/2) = 189 = $BD, plus 2 for tya & rts

PRODOSBUF = $1000 ; temporary, before tbls built
SCREEN = $2000

TEXTURES = $4000 ; size $5550 (5460 bytes x 4 textures)
TEXSIZE = 5460
TEX0 = TEXTURES
TEX1 = TEX0+TEXSIZE
TEX2 = TEX1+TEXSIZE
TEX3 = TEX2+TEXSIZE
UN9550 = $9550 ; unused
BLITROLL = $A000 ; size 29*126 = 3654 = $E80, plus 1 for rts
BUMPS = $AF00 ; len 64*64 = $1000
GLOBALPG = $BF00 ; ProDOS global page
MLI = GLOBALPG ; also the call point for ProDOS MLI
MEMMAP = $BF58

; I/O locations
KBD = $C000
CLRAUXRD = $C002
SETAUXRD = $C003
CLRAUXWR = $C004
SETAUXWR = $C005
CLRAUXZP = $C008
SETAUXZP = $C009
KBDSTRB = $C010
CLRTEXT = $C050
SETTEXT = $C051
CLRMIXED = $C052
SETMIXED = $C053
PAGE1 = $C054
PAGE2 = $C055
CLRHIRES = $C056
SETHIRES = $C057

; ROM routines
AUXMOVE = $C311
PRNTAX = $F941
RDKEY = $FD0C
CROUT = $FD8E
PRBYTE = $FDDA
COUT = $FDED
PRERR = $FF2D
MONITOR = $FF69

; Pixel offsets for even and odd blit lines
BLITOFFE: .byte 5,8,11,1,17,20,24
BLITOFFO: .byte 34,37,40,30,46,49,53
; texture addresses
TEXADRL: .byte <TEX0,<TEX1,<TEX2,<TEX3
TEXADRH: .byte >TEX0,>TEX1,>TEX2,>TEX3
    ; mip level offsets
MIPOFFL: .byte <0,<4096,<5120,<5376,<5440,<5456,<5460
MIPOFFH: .byte >0,>4096,>5120,>5376,>5440,>5456,>5460

NEXTLINE:
    LDA PLINE+1 ; Hi byte of line
    CLC
    ADC #4 ; Next line is 1K up
    TAX
    EOR PLINE+1
    AND #$20 ; Past end of screen?
    BEQ @DONE ; If not, we're done
    TXA
    SEC
    SBC #$20 ; Back to start
    TAX
    LDA PLINE ; Lo byte
    CLC
    ADC #$80 ; Inner blks offset by 128 bytes
    STA PLINE
    BCC @DONE
    INX ; Next page
    TXA
    AND #7
    CMP #4 ; Still inside inner blk?
    BNE @DONE ; If so we're done
    TXA
    SEC
    SBC #4 ; Back to start of inner blk
    TAX
    LDA PLINE
    CLC
    ADC #$28 ; Outer blks offset by 40 bytes
    STA PLINE
@DONE:
    STX PLINE+1
    RTS

; Template for blitting code

BLITTPL: ; comments show byte offset
; even rows
    LDA SH0157 ;  0: pixel 3
    ASL ;  3: save half of pix 3 in carry
    ORA SH0101 ;  4: pixel 0
    ORA SH0123 ;  7: pixel 1
    ORA SH0145 ; 10: pixel 2
    STA (0),Y ; 13: even column
    INY ; 15: prep for odd
    LDA SH0101 ; 16: pixel 4
    ORA SH0123 ; 19: pixel 5
    ROL ; 22: recover half of pix 3
    ORA SH0156 ; 23: pixel 6 - after rol to ensure right hi bit
    STA (0),Y ; 26: odd column
    DEY ; 28: prep for even
; odd rows
    LDA SH4557 ; 29: pixel 3
    ASL ; 32: save half of pix 3 in carry
    ORA SH4501 ; 33: pixel 0
    ORA SH4523 ; 36: pixel 1
    ORA SH4545 ; 39: pixel 2
    STA (2),Y ; 42: even column
    INY ; 44: prep for odd
    LDA SH4501 ; 45: pixel 4
    ORA SH4523 ; 48: pixel 5
    ROL ; 51: recover half of pix 3
    ORA SH4556 ; 52: pixel 6 - after rol to ensure right hi bit
    STA (2),Y ; 55: odd column
    DEY ; 57: prep for even
    ; 58: total

; Create the unrolled blit code
MAKEBLIT:
    LDA #0 ; Start with line zero
    STA LINECT
    LDA #<TOPLINE ; Begin with the first screen line
    STA PLINE
    LDA #>TOPLINE
    STA PLINE+1
    LDA #<BLITROLL ; Store to blit unroll code buf
    STA PDST
    LDA #>BLITROLL
    STA PDST+1
@LINELUP:
; Copy the template
    LDY #57
@COPY:
    LDA BLITTPL,Y
    STA (PDST),Y
    DEY
    BPL @COPY
     ; Record the address for the even line
    JSR @STIDX
; Set the even line pointers
    LDY #14
    JSR @STLINE
    LDY #27
    JSR @STLINE
    ; Get ready for odd line
    JSR @ADVANCE
    ; Record the address for the odd line
    JSR @STIDX
; Set the odd line pointers
    LDY #14
    JSR @STLINE
    LDY #27
    JSR @STLINE
    ; Prepare for next iteration
    JSR @ADVANCE
; Loop until all lines are done
    LDA LINECT
    CMP #NLINES
    BNE @LINELUP
    JSR @STIDX ; Last addr to index
    JMP STRTS ; Finish with RTS for cleanliness
@STLINE: ; Subroutine to store PLINE to PDST
    LDA LINECT
    ASL
    STA (PDST),Y
    RTS
@STIDX: ; Subroutine to store tbl ptr to index
    LDY LINECT
    LDA PDST
    STA BLITIDXL,Y
    LDA PDST+1
    STA BLITIDXH,Y
    RTS
@ADVANCE: ; Subroutine to go to next unroll
    LDA #29
    JSR ADVPDST
    INC LINECT
    JMP NEXTLINE

; Create code to clear the blit
MAKECBLIT:
    LDX #0
    LDY #0
@LUP:
    LDA @ST
    STA CBLITROLL,X
    INX
    LDA BLITIDXL,Y
    STA CBLITROLL,X
    INX
    LDA BLITIDXH,Y
@ST:
    STA CBLITROLL,X
    INX
    INY
    INY
    CPY #64
    BNE @NOSWITCH
    LDA @TYA ; switch from sky color to ground color
    STA CBLITROLL,X
    INX
@NOSWITCH:
    CPY #NLINES
    BNE @LUP
    LDA @RTS
    STA CBLITROLL,X
@RTS:
    RTS
@TYA:
    TYA

; Clear the blit
CLRBLIT:
    LDY #GROUNDCOL
CLRBLIT2:
    LDX BLITOFFE+0
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+1
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+2
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+3
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+4
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+5
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFE+6
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+0
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+1
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+2
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+3
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+4
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+5
    LDA #SKYCOL
    JSR CBLITROLL
    LDX BLITOFFO+6
    LDA #SKYCOL
    JMP CBLITROLL

; Construct the shift tables
MAKESHIFT:
    LDX #0
@SH01:
    TXA
    AND #3
@SH0101:
    STA SH0101,X
@SH0123:
    ASL
    ASL
    STA SH0123,X
@SH0145:
    ASL
    ASL
    ORA #$80
    STA SH0145,X
@SH0156:
    ASL
    ORA #$80
    STA SH0156,X
@SH0157:
    ASL
    ASL
    PHP
    LSR
    PLP
    ROR
    STA SH0157,X
@SH45:
    TXA
    LSR
    LSR
    LSR
    LSR
    AND #3
@SH4501:
    ORA #$80
    STA SH4501,X
@SH4523:
    ASL
    ASL
    ORA #$80
    STA SH4523,X
@SH4545:
    ASL
    ASL
    ORA #$80
    STA SH4545,X
@SH4556:
    ASL
    ORA #$80
    STA SH4556,X
@SH4557:
    ASL
    ASL
    PHP
    LSR
    PLP
    ROR
    STA SH4557,X
@NEXT:
    INX
    BNE @SH01
    RTS

; Template for decimation. Offsets in comments
DCMTPL:
    LDA (PTEX),Y ; 0
    STA BLITROLL,X ; 2
    STA BLITROLL+29,X ; 5
    LDA (PBUMP),Y ; 8
    TAY ; 10
    ; 11

; Unroll the decimation code
MAKEDCM:
    LDX #0 ; Line counter
    LDA #<DCMROLL
    STA PDST
    LDA #>DCMROLL
    STA PDST+1
@ONESET:
; Save address to the index
    JSR @STIDX
    LDY #11 ; Copy the template
@COPYSET: 
    LDA DCMTPL,Y
    STA (PDST),Y
    DEY
    BPL @COPYSET
    LDY #3
    JSR @STBLIT
    LDY #6
    JSR @STBLIT
    LDA #11
    JSR ADVPDST
@MORE:
    ; Loop until all lines done
    CPX #NLINES
    BCC @ONESET
    JSR @STIDX ; Last addr to index
    JMP STRTS ; Finish with an RTS for cleanliness
@STBLIT: ; Store current blit addr
    LDA BLITIDXL,X
    STA (PDST),Y
    INY
    LDA BLITIDXH,X
    STA (PDST),Y
    INX ; Next line
    RTS
@STIDX:
    TXA
    LSR ; One entry per two lines
    TAY
    LDA PDST
    STA DCMIDXL,Y
    LDA PDST+1
    STA DCMIDXH,Y
    RTS

STRTS:
    LDA #$60 ; Store an RTS at PDST
    LDY #0
    STA (PDST),Y
    RTS
ADVPDST: ; Add A to PDST
    CLC
    ADC PDST
    STA PDST
    BCC @RTS
    INC PDST+1
@RTS:
    RTS

; Clear all the memory we're going to fill
CLRMEM:
    LDX #$10
    LDA #$BE
    JMP CLRSCR2

; Clear the screens
CLRSCR:
    LDX #>SCREEN
    .if DBLBUF
    LDA #>SCREEN + $40 ; both hi-res screens
    .else
    LDA #>SCREEN + $20 ; one hi-res screen
    .endif
CLRSCR2:
    STA @LIMIT+1
    LDY #0
    STY PDST
    TYA
@OUTER:
    STX PDST+1
@INNER:
    STA (PDST),Y
    INY
    BNE @INNER
    INX
@LIMIT:
    CPX #>SCREEN + $20
    BNE @OUTER
    RTS

; Make a simple texture with alternating colors.
; Input: Y = tex num
;        A, X: color numbers, 0-3
SIMPLETEX:
    STA @LD1+1
    TXA
    ASL
    ASL
    ASL
    ASL
    STA @LD2+1
    LDA TEXADRL,Y
    STA PDST
    LDA TEXADRH,Y
    STA PDST+1
    LDX #>TEXSIZE
    LDY #0
    STY @LIM+1
@OUTER:
@LD1:
    LDA #0
@LD2:
    ORA #0
@LUP:
    STA (PDST),Y
    INY
@LIM:
    CPY #0
    BNE @LUP
    INC PDST+1
    DEX
    BMI @DONE
    BNE @OUTER
    LDA #<TEXSIZE ; partial last page
    STA @LIM+1
    JMP @OUTER
@DONE: RTS

; Generate the table of "bumps" for decimation
MAKEBUMPS:
    LDA #63 ; 126/2 bump lists
    STA LINECT
    LDA #<BUMPS
    STA PDST
    LDA #>BUMPS
    STA PDST+1
    LDA #0
    STA @RATIOL
    LDA #1
    STA @RATIOH
; Goal is to make ratio = 63 divided by targetSize.
; The calculation is cool & weird, but I verified
; in Python that the logic actually works. You
; start with hi=1, lo=0. To calculate the next
; step, add hi to low and take the sum mod the next
; target size. To use the ratio, increment by hi
; and lo. Whenever the low byte goes beyond the
; target size, add an extra to hi.
@ONEPASS:
    LDA LINECT ; Init A with the lo byte = target size
    LSR ; ...div 2
    LDX #0 ; Hi byte always starts at zero
    LDY #0 ; Location to store at
@BUMPLUP:
    CLC  ; increment lo byte by ratio
    ADC @RATIOL
    CMP LINECT ; if we wrap around, need extra hi-byte bump
    BCC @NOBM
    SEC
    SBC LINECT
    INX
@NOBM:
    PHA ; save lo byte
    TXA ; now work on hi byte
    CLC
    ADC @RATIOH
    TAX
    STA (PDST),Y ; store to the table
    TAY ; next loc to store
    CPX #63 ; check for end of column
    PLA ; get lo byte back
    BCC @BUMPLUP ; loop until whole column is done
    LDA #64
    JSR ADVPDST ; advance dst to next column
@NEXT:
    DEC LINECT ; all columns complete?
    BEQ @DONE
    LDA @RATIOL ; next ratio calculation (see explanation above)
    CLC
    ADC @RATIOH
@MODLUP:
    CMP LINECT
    BCC @NOMOD
    INC @RATIOH
    SEC
    SBC LINECT
    BNE @MODLUP ; this must indeed be a loop
@NOMOD:
    STA @RATIOL
    JMP @ONEPASS ; next column
@DONE:
    RTS
@RATIOL: .byte 0
@RATIOH: .byte 0

; Decimate a column of the texture
; Input: Y - texture number
;        TXCOLNUM - src column num in the texture
;        PIXNUM - dst pixel num in the blit roll
;        LINECT - height to render, in dbl lines
; The output will be vertically centered.
DCMCOL:
    ; if height is zero, render nothing
    LDA LINECT
    BNE @NOTZERO
    RTS
@NOTZERO:
    ; determine mip level in X reg
    LDX #0
    LDA LINECT
    STA @ADJHT
    LDA TXCOLNUM
    STA @ADJCOL
    LDA #32
@MIPLUP:
    CMP LINECT
    BCC @GOTMIP
    INX
    ASL @ADJHT
    LSR @ADJCOL
    LSR
    CMP #2
    BCS @MIPLUP
@GOTMIP:
    .if DEBUG
    LDA #"t"
    JSR COUT
    TYA
    JSR PRBYTE
    LDA #" "
    JSR COUT

    LDA #"h"
    JSR COUT
    LDA LINECT
    JSR PRBYTE
    LDA #" "
    JSR COUT

    LDA #"m"
    JSR COUT
    TXA
    JSR PRBYTE
    LDA #" "
    JSR COUT
    .endif

    ; calc addr of tex
    LDA TEXADRL,Y
    CLC
    ADC MIPOFFL,X
    STA PDST
    LDA TEXADRH,Y
    ADC MIPOFFH,X
    STA PDST+1

    .if DEBUG
    LDA #"a"
    JSR COUT
    LDA PDST+1
    JSR PRBYTE
    LDA PDST
    JSR PRBYTE
    LDA #" "
    JSR COUT
    .endif

@CALCOFF: ; calc offset within tex
    LDA #0
    STA PTEX+1
    LDA @ADJCOL
@SHIFT:
    ASL
    ROL PTEX+1
    INX ; Note: destroys mip level
    CPX #6
    BNE @SHIFT

    .if DEBUG
    PHA
    LDA #"x"
    JSR COUT
    LDA @ADJCOL
    JSR PRBYTE
    LDA #" "
    JSR COUT

    LDA #"o"
    JSR COUT
    LDA PTEX+1
    JSR PRBYTE
    PLA
    PHA
    JSR PRBYTE
    LDA #" "
    JSR COUT
    PLA
    .endif

    CLC
    ADC PDST
    STA PTEX
    LDA PTEX+1
    ADC PDST+1
    STA PTEX+1
; calculate bump table ptr
    LDX @ADJHT
    JSR CALCBUMP
    ; figure first line in decim unroll
    LDA #63
    SEC
    SBC LINECT ; height 63 is first in decim tbl
    LSR
    TAX
    LDA DCMIDXL,X
    STA @CALL+1
    LDA DCMIDXH,X
    STA @CALL+2
    ; figure last line of decim unroll
    TXA
    CLC
    ADC LINECT
    TAX
    LDA DCMIDXL,X
    STA PTMP
    LDA DCMIDXH,X
    STA PTMP+1
; determine blit offset for writing
    LDY PIXNUM
    LDX BLITOFFE,Y
    ; store RTS so decim returns @ right moment
    LDY #0
    LDA (PTMP),Y ; save existing byte
    PHA
    LDA @RTS
    STA (PTMP),Y

    .if DEBUG
    PHX
    PHY
    JSR RDKEY
    PHA
    JSR CROUT
    PLA
    PLY
    PLX
    CMP #$9B
    BNE @NOTESC
    BRK
@NOTESC:
    NOP
    .endif

@CALL:
    JSR DCMROLL
; fix RTS to what it was before
    LDY #0
    PLA
    STA (PTMP),Y

    .if DEBUG
    LDY BYTENUM ; to see results early
    STA SETAUXZP
    JSR BLITROLL
    STA CLRAUXZP
    .endif

@RTS:
    RTS
@ADJHT: .byte 0
@ADJCOL: .byte 0

; Calc pointer into the bump table
; Input: X - height to render in dbl lines
CALCBUMP:
    STX @SUB+1
    LDA #0
    STA PBUMP+1
    LDA #63 ; bump 63 is actually first
    SEC
@SUB:
    SBC #0
    BPL @NOTNEG
    LDA #0
@NOTNEG:

    .if DEBUG
    PHA
    LDA #"b"
    JSR COUT
    PLA
    PHA
    JSR PRBYTE
    LDA #" "
    JSR COUT
    PLA
    .endif

    LDX #6
@LUP:
    ASL
    ROL PBUMP+1
    DEX
    BNE @LUP
    CLC
    ADC #<BUMPS
    STA PBUMP
    LDA PBUMP+1
    ADC #>BUMPS
    STA PBUMP+1

    .if DEBUG
    LDA #"p"
    JSR COUT
    LDA PBUMP+1
    JSR PRBYTE
    LDA PBUMP
    JSR PRBYTE
    LDA #" "
    JSR COUT
    .endif

    RTS

; Build table of screen line pointers
; on aux zero-page
MAKELINES:
    LDA #0
    STA LINECT
    LDA #<TOPLINE
    STA PLINE
    LDA #>TOPLINE
    STA PLINE+1
@LUP:
    LDA LINECT
    ASL
    TAX
    LDA PLINE
    LDY PLINE+1
    STA SETAUXZP
    STA 0,X
    STY 1,X
    STA CLRAUXZP
    JSR NEXTLINE
    INC LINECT
    LDA LINECT
    CMP #NLINES
    BNE @LUP
    RTS

; Set screen lines to current back buf
SETBKBUF:
; calculate screen start
    LDA BACKBUF
    ASL
    ASL
    ASL
    ASL
    ASL
    CLC
    ADC #$20
    STA SETAUXZP
    STA $FF
    LDX #0
@LUP:
    LDA 1,X
    AND #$1F
    ORA $FF
    STA 1,X
    INX
    INX
    BNE @LUP
    STA CLRAUXZP
    RTS

; Load file, len-prefixed name in A/X (hi/lo), to addr on stack
; (push hi byte first, then push lo byte)
BLOAD:
    STX @MLICMD+1 ; filename lo
    STA @MLICMD+2 ; filename hi
    LDA #<PRODOSBUF
    STA @MLICMD+3
    LDA #>PRODOSBUF
    STA @MLICMD+4
    LDA #$C8 ; open
    LDX #3
    JSR @DOMLI
    LDA @MLICMD+5 ; get handle and put it in place
    STA @MLICMD+1
    PLY ; save ret addr
    PLX
    PLA
    STA @MLICMD+2 ; load addr lo
    PLA
    STA @MLICMD+3 ; load addr hi
    PHX ; restore ret addr
    PHY
    LDA #$CA ; read
    STA @MLICMD+5 ; also length (more than enough)
    LDX #4
    JSR @DOMLI
@CLOSE:
    STZ @MLICMD+1 ; close all
    LDA #$CC
    LDX #1
    ; fall through
@DOMLI:
    STA @MLIOP
    STX @MLICMD
    JSR MLI
@MLIOP: .byte 0
    .addr @MLICMD
    BCS @ERR
    RTS
@ERR:
    JSR PRBYTE
    JSR PRERR
    LDX #$FF
    TXS
    JMP MONITOR
@MLICMD: .res 10 ; 10 bytes should be plenty

; Copy X pages starting at pg Y to aux mem
CPTOAUX:
    STA SETAUXWR
    STY PDST+1
    LDY #0
    STY PDST
@LUP:
    LDA (PDST),Y
    STA (PDST),Y
    INY
    BNE @LUP
    INC PDST+1
    DEX
    BNE @LUP
    STA CLRAUXWR
    RTS

; Fetch the next byte from the pre-raycasted data
; Note this routine needs to be copied to aux mem.
GETCAST:
    LDY #0
    STA SETAUXRD
    LDA (PCAST),Y
    STA CLRAUXRD
    INC PCAST
    BNE @DONE
    INC PCAST+1
@DONE:
    RTS

; Test code to see if things really work
TEST:
    ; clear ProDOS mem map so it lets us load
    LDX #$18
    LDA #1
@MEMLUP:
    STA MEMMAP-1,X
    LDA #0
    DEX
    BNE @MEMLUP
; load the pre-raycast data
    LDA #$20 ; addr hi
    PHA
    LDA #0 ; addr lo
    PHA
    LDX #<@PRECASTNM
    LDA #>@PRECASTNM
    JSR BLOAD
; copy it to aux mem
    LDY #$20
    LDX #$60
    JSR CPTOAUX
    LDA #0 ; set ptr to it
    STA PCAST
    LDA #$20
    STA PCAST+1
; copy our code to aux mem
    LDY #>CODEBEG
    LDX #>CODEEND - >CODEBEG + 1
    JSR CPTOAUX
; set up everything else
    JSR CLRMEM
    ; load the textures
    LDA #>TEX0
    PHA
    LDA #<TEX0
    PHA
    LDX #<@TEX0NAME
    LDA #>@TEX0NAME
    JSR BLOAD

    LDA #>TEX1
    PHA
    LDA #<TEX1
    PHA
    LDX #<@TEX1NAME
    LDA #>@TEX1NAME
    JSR BLOAD

    LDA #>TEX2
    PHA
    LDA #<TEX2
    PHA
    LDX #<@TEX2NAME
    LDA #>@TEX2NAME
    JSR BLOAD

    LDA #>TEX3
    PHA
    LDA #<TEX3
    PHA
    LDX #<@TEX3NAME
    LDA #>@TEX3NAME
    JSR BLOAD
; build all the unrolls and tables
    JSR MAKEBLIT
    JSR MAKECBLIT
    JSR MAKESHIFT
    JSR MAKEDCM
    JSR MAKEBUMPS
    JSR MAKELINES
    JSR CLRSCR
; set up front and back buffers
    LDA #0
    STA FRONTBUF
    .if DBLBUF
    LDA #1
    .endif
    STA BACKBUF

    BIT CLRTEXT
    BIT SETHIRES

    LDA #63
    STA LINECT
    LDA #1
    STA @DIR
    JSR CLRBLIT
@ONELVL:
    LDA #0
    STA PIXNUM
    STA BYTENUM
    .if DBLBUF
    JSR SETBKBUF
    .endif

    .if DEBUG
    LDA PCAST+1
    JSR PRBYTE
    LDA PCAST
    JSR PRBYTE
    JSR CROUT
    .endif

@ONECOL:
    JSR GETCAST ; first byte is height
    CMP #$FF
    BNE @NORESET
; found end of cast data, start over
    LDA #0
    STA PCAST
    LDA #$20
    STA PCAST+1
    JSR GETCAST
@NORESET:
    CMP #63
    BCC @HTOK
    LDA #62
@HTOK:
    STA LINECT
    JSR GETCAST ; second byte is tex num and tex col
    PHA
    AND #$3F
    CMP #63
    BCC @COLOK
    LDA #62
@COLOK:
    STA TXCOLNUM
    PLA
    LSR ; div by 64
    LSR
    LSR
    LSR
    LSR
    LSR
    TAY ; Y now holds tex num
    JSR DCMCOL
    INC PIXNUM
    LDA PIXNUM
    CMP #7
    BNE @ONECOL
@FLUSH:
    LDY BYTENUM
    STA SETAUXZP
    JSR BLITROLL
    STA CLRAUXZP
    JSR CLRBLIT
    LDA #0
    STA PIXNUM
    INC BYTENUM
    INC BYTENUM
    LDA BYTENUM
    CMP #18
    BNE @ONECOL
@NEXTLVL:
; flip onto the screen
    .if DBLBUF
    LDX BACKBUF
    LDA FRONTBUF
    STA BACKBUF
    STX FRONTBUF
    LDA PAGE1,X
    .endif
    ; adv past FE in cast data
    JSR GETCAST
    CMP #$FE
    BNE @ERR
    JSR GETCAST
    CMP #$FE
    BEQ @INCDEC
@ERR:
    BRK
@INCDEC:
    LDA KBD ; stop if ESC is pressed
    CMP #$9B
    BEQ @DONE
    JMP @ONELVL
@DONE:
    STA KBDSTRB ; eat the keypress
    BIT SETTEXT
    BIT PAGE1
    RTS
@DIR: .byte 1
@TEX0NAME: .byte 21
    .byte "/LL/ASSETS/BUILDING01"
@TEX1NAME: .byte 21
    .byte "/LL/ASSETS/BUILDING02"
@TEX2NAME: .byte 21
    .byte "/LL/ASSETS/BUILDING03"
@TEX3NAME: .byte 21
    .byte "/LL/ASSETS/BUILDING04"
@PRECASTNM: .byte 18
    .byte "/LL/ASSETS/PRECAST"

CODEEND = *

