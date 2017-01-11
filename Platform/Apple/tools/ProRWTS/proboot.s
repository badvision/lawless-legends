;license:BSD-3-Clause
;minimal open/read binary file in ProDOS filesystem
;copyright (c) Peter Ferrie 2016-2017
!cpu 6502
*=$800

                ;zpage usage, arbitrary selection except for the "PROM constant" ones
                tmpsec    = $3c
                reqsec    = $3d         ;PROM constant
                A1L       = $3c
                A1H       = $3d
                curtrk    = $40         ;PROM constant
                adrlo     = $44
                adrhi     = $45

                sizehi    = $53
                entries   = $f8         ;(internal) total number of entries in directory
                step      = $fd         ;(internal) state for stepper motor
                tmptrk    = $fe         ;(internal) temporary copy of current track
                phase     = $ff         ;(internal) current phase for seek

                ;constants
                scrn2p2   = $f87b
                dirbuf    = $e00        ;for size-optimisation
                PHASEOFF  = $c080
                Q6L       = $c08c

                !byte 1
                lsr
                bne init
                inc reqsec
                txa
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

filename        !byte filename_e - filename_b
filename_b      !text "LEGENDOS.SYSTEM" ;your start-up file, file is min 513 bytes, max 64kb
filename_e

init            txa
                tay
                ora #$80
                sta unrseek+1
                ora #$0c
                sta unrread1+1
                sta unrread2+1
                sta unrread3+1
                sta unrread4+1

-               lda fakeMLI-1, x
                sta $beff, x
                lda #$ff
                sta $bf0f, x
                dex
                bne -
                sty $bf30

                sta $200

opendir         ;read volume directory key block
                txa
                ldx #2
                jsr readdirsel

                ;include volume directory header in count

firstent        lda #<(dirbuf+4)
                sta A1L
                lda #>(dirbuf+4)
                sta A1H
nextent         ldy #0
                lda (A1L), y

                ;match name lengths before attempting to match names

                lda (A1L), y
                and #$0f
                tax
                inx
                !byte $2c
-               lda (A1L), y
                cmp filename, y
                beq foundname

                ;move to next directory in this block

+               clc
                lda A1L
                adc #$27
                sta A1L
                bcc +

                ;there can be only one page crossed, so we can increment instead of adc

                inc A1H
+               cmp #<(dirbuf+$1ff) ;4+($27*$0d)
                lda A1H
                sbc #>(dirbuf+$1ff)
                bcc nextent

                ;read next directory block when we reach the end of this block

                ldx dirbuf+2
                lda dirbuf+3
                jsr readdirsec
                bne firstent

foundname       iny
                dex
                bne -

                ;cache EOF (file size)

                ldy #$15
                lda (A1L), y
                cmp #1
                iny
                lda (A1L), y
                adc #1
                lsr
                sta sizehi

                ;cache KEY_POINTER

                ldy #$11
                lda (A1L), y
                tax
                iny
                lda (A1L), y

                ;read index block in case of sapling

                jsr readdirsec

                ;restore load offset

                asl adrhi

readfile
                ;fetch data block and read it

                ldy $41 ;zeroed by boot PROM
                inc $41
                ldx dirbuf, y
                lda dirbuf+256, y
                jsr seekread

                ;loop while size-$200 is non-zero

                dec sizehi
                bne readfile

readdone        jmp $2000

                ;no tricks here, just the regular stuff

seek            sty step
                asl phase
                txa
                asl
copy_cur        tax
                sta tmptrk
                sec
                sbc phase
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
                lda tmptrk
                ldx step2, y
+               stx tmpsec
                and #3
                rol
                tax
                lsr
unrseek
                lda PHASEOFF, x
--              ldx #$13
-               dex
                bne -
                dec tmpsec
                bne --
                bcs ---
                plp
                beq seekret
                pla
                inc step
                bne copy_cur

step1           !byte 1, $30, $28, $24, $20, $1e, $1d, $1c
step2           !byte $70, $2c, $26, $22, $1f, $1e, $1d, $1c

readadr
-               jsr readd5aa
                cmp #$96
                bne -
                ldy #3
-               sta curtrk
                jsr readnib
                rol
                sta tmpsec
                jsr readnib
                and tmpsec
                dey
                bne -
seekret         rts

readd5aa
--              jsr readnib
-               cmp #$d5
                bne --
                jsr readnib
                cmp #$aa
                bne -
                tay                    ;we need Y=#$AA later

readnib
unrread1
-               lda Q6L
                bpl -
                rts

readdirsel      sta adrlo
readdirsec
                ldy #>dirbuf
                sty adrhi

                ;convert block number to track/sector

seekread        lsr
                txa
                ror
                lsr
                lsr
                sta phase
                txa
                and #3
                php
                asl
                plp
                rol
                sta reqsec
                jsr readadr

                ;if track does not match, then seek

                ldx curtrk
                cpx phase
                beq checksec
                jsr seek

                ;force sector mismatch

                lda #$ff

                ;match or read sector

checksec        jsr cmpsec
                inc reqsec
                inc reqsec

                ;force sector mismatch

cmpsecrd        lda #$ff

cmpsec          cmp reqsec
                beq readdata
                jsr readadr
                beq cmpsec

                ;read sector data

readdata        jsr readd5aa
                eor #$ad                ;zero A if match
;;                bne *                   ;lock if read failure
unrread2
-               ldx Q6L
                bpl -
                eor nibtbl - $96, x
                sta bit2tbl - $aa, y
                iny
                bne -
unrread3
-               ldx Q6L
                bpl -
                eor nibtbl-$96, x
                sta (adrlo), y          ;the real address
                iny
                bne -
unrread4
-               ldx Q6L
                bpl -
                eor nibtbl-$96, x
                bne cmpsecrd
--              ldx #$a9
-               inx
                beq --
                lda (adrlo), y
                lsr bit2tbl - $aa, x
                rol
                lsr bit2tbl - $aa, x
                rol
                sta (adrlo), y
                iny
                bne -
readret         inc adrhi
                rts

bit2tbl         = $300 ;PROM constant
nibtbl          = bit2tbl + 108 ;PROM constant
dataend         = nibtbl + 106 ;PROM constant
readbuff
!byte $D3,$C1,$CE,$A0,$C9,$CE,$C3,$AE
