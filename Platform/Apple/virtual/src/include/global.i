; Shared definitions for all modules

; Zero page temps
tmp      = $2  ; len 2
pTmp     = $4  ; len 2

; Zero page monitor locations
a2l      = $3E
a2h      = $3F

; Other monitor locations
resetVec = $3F2

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
textinit  = $FB2F
rdkey     = $FD0C
getln1    = $FD6F
crout     = $FD8E
prbyte    = $FDDA
cout      = $FDED
setnorm   = $FE84
setkbd    = $FE89
setvid    = $FE93
prerr     = $FF2D
bell      = $FF3A
monitor   = $FF69
getnum    = $FFA7

