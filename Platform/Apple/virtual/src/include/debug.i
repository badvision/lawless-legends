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

