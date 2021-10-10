;****************************************************************************************
; Copyright (C) 2021 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
; ANY KIND, either express or implied. See the License for the specific language
; governing permissions and limitations under the License.
;****************************************************************************************

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"
!source "../include/mem.i"
!source "../include/sound.i"
!source "../include/plasma.i"

* = genSound

; hardware
spkr  = $C030

; init
doGenSound:
!zone {
; stack offsets
.o_delay = 0
.o_velo  = 1
.o_noise = 2

; variables
.negdur = $2 ; 2 bytes
.upx    = $4
.dnx    = $5
.pbits  = $6 ; 2 bytes
.rnd    = $8

  eor #$FF
  sta .negdur
  tya
  eor #$FF
  sta .negdur+1
  inx
  stx .upx
  inx
  inx
  inx
  stx .dnx

  lda #$EC
  sta .pbits+1
  ldy #0
  sty .pbits        ; 28 bytes

.cycle:
  sta spkr

  lda .o_velo+evalStkL,x
  clc
  adc .o_delay+evalStkL,x
  sta .o_delay+evalStkL,x
  lda .o_velo+evalStkH,x
  adc .o_delay+evalStkH,x
  pha
  ror                     ; carry out from the addition...
  eor .o_velo+evalStkH,x  ; ... should match sign of the velocity
  asl                     ; stach match status in carry
  pla
  bcc +                   ; if match, accept the result...
  lda .o_delay+evalStkH,x ; ... else over/underflow occurred; revert.
+ sta .o_delay+evalStkH,x

  sec
- sbc #1
  pha
  pla
  pha
  pla
  bcs -

.calcnoise:
  lda .rnd
  asl       ; cute trick: asl + adc #0 is a lossless rotate that also clears carry
  adc (.pbits),y
  sta .rnd
  dey
  bne .npause
  inc .pbits+1
  bne +
  ldy #$EC
  sty .pbits+1
+ ldy #$FF
.npause:
  and .o_noise+evalStkL,x
  sec
- sbc #1
  pha
  pla
  pha
  pla
  bcs -

.flip
  cpx .dnx
  ldx .upx
  bcs +
  ldx .dnx
+

.nextdur:
  inc .negdur
  bne .cycle
  inc .negdur+1
  bne .cycle

stop:
  rts
}
