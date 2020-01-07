;license:BSD-3-Clause
;minimal open/read binary file in ProDOS filesystem
;copyright (c) Peter Ferrie 2016-2018
!cpu 6502
!to "proboot",plain
*=$800

                ;zpage usage, arbitrary selection except for the "PROM constant" ones
                tmpval    = $3c
                reqsec    = $3d         ;PROM constant
                A2L       = $3e
                A2H       = $3f
                phase     = $40
                reqtrk    = $41         ;PROM constant
                adrlo     = $26
                adrhi     = $27

                sizehi    = $53
                step      = $fd         ;(internal) state for stepper motor

                ;constants
                scrn2p2   = $f87b
                dirbuf    = $1e00       ;for size-optimisation
                PHASEOFF  = $c080
                Q6L       = $c08c

                !byte 1
                lsr
                bne init
goprom2         inc reqsec
goprom1         txa
                jsr scrn2p2
                ora #$c0
                pha
                lda #$5b
                pha
                rts

fakeMLI         pla
                tax
                inx
                inx
                inx
                txa
                pha
                rts
fakeMLI_e

filename        !byte filename_e - filename_b
filename_b      !text "LEGENDOS.SYSTEM" ;your start-up file, file is max 40kb
filename_e

init
-               txa
                jsr scrn2p2
                ora #$c0
                sta $be30, y
                lda fakeMLI_e-$100, y
                sta $be00+fakeMLI_e-fakeMLI, y
                iny
                bne -
                stx $bf30
                sty $200
                sta $801

opendir         ;read volume directory key block
                ldx #2

                ;include volume directory header in count

firstent        lda #>dirbuf
                sta adrhi
                sta A2H
                jsr seekread
                lda #4
                sta A2L
nextent         ldy #0

                ;match name lengths before attempting to match names

                lda (A2L), y
                and #$0f
                tax
                inx
-               cmp filename, y
                beq foundname

                ;move to next directory in this block

                clc
                lda A2L
                adc #$27
                sta A2L
                bcc +

                ;there can be only one page crossed, so we can increment instead of adc

                inc A2H
+               cmp #$ff ;4+($27*$0d)
                bne nextent

                ;read next directory block when we reach the end of this block

                ldx dirbuf+2
                ldy dirbuf+3
                bcs firstent

foundname       iny
                lda (A2L), y
                dex
                bne -
                stx $ff

                ;cache KEY_POINTER

                ldy #$11
                lda (A2L), y
                tax
                iny
                lda (A2L), y
                tay

                lda #>dirbuf
                sta adrhi

readfile        jsr seekread

                ;fetch data block and read it

blockind        ldy $ff
                inc $ff
                ldx dirbuf, y
                lda dirbuf+256, y
                tay
                bne readfile
                txa
                bne readfile

readdone        jmp $2000

step1           !byte 1, $30, $28, $24, $20, $1e, $1d, $1c
step2           !byte $70, $2c, $26, $22, $1f, $1e, $1d, $1c

readadr
-               jsr readd5aa
                eor #$96
                bne -
                sta step
                jsr +
+               jsr readnib
                rol
                sta tmpval
                bcs readnib

readd5aa        ldx $2b
--              jsr readnib
-               cmp #$d5
                bne --
                jsr readnib
                cmp #$aa
                bne -

readnib
-               lda Q6L, x
                bpl -
seekret         rts

                ;convert block number to track/sector

seekread        tya
                lsr
                txa
                ror
                lsr
                lsr
                sta reqtrk
                txa
                and #3
                php
                asl
                plp
                rol
                sta reqsec
                jsr readadr
                and tmpval

                ;if track does not match, then seek

                cmp reqtrk
                beq checksec
                asl reqtrk
                asl
copy_cur        tax
                sta phase
                sec
                sbc reqtrk
                beq +++
                bcs +
                eor #$ff
                inx
                bcc ++
+               sbc #1
                dex
++              cmp step
                bcc +
                lda step
+               cmp #8
                bcs +
                tay
                sec
+               txa
                pha
                ldx step1, y
+++             php
                bne +
---             clc
                lda phase
                ldx step2, y
+               stx tmpval
                and #3
                rol
                ora $2b
                tax
                lsr
                lda PHASEOFF, x
--              ldx #$12
-               dex
                bpl -
                dec tmpval
                bne --
                bcs ---
                plp
                beq shifttrk
                pla
                inc step
                bne copy_cur

shifttrk        lsr reqtrk

checksec        ldx $2b
                jsr goprom1
                jmp goprom2

!byte $D3,$C1,$CE,$A0,$C9,$CE,$C3,$AE
