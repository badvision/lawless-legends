; Shared definitions for all modules

; Zero page temporary area. Modules can feel free to use the entire space,
; but must *not* count on it being preserved when other modules are in
; control, e.g. when calling other modules, or returning to them.
zpTempStart = $2                ; 0 and 1 are reserved on c64
zpTempEnd   = $1F

; Zero page monitor locations
a2l      = $3E
a2h      = $3F

; Other monitor locations
resetVec = $3F2

; PRODOS
mli = $BF00
MLI_QUIT          = $65
MLI_GET_TIME      = $82
MLI_CREATE        = $C0
MLI_DESTROY       = $C1
MLI_RENAME        = $C2
MLI_SET_FILE_INFO = $C3
MLI_GET_FILE_INFO = $C4
MLI_ONLINE        = $C5
MLI_SET_PREFIX    = $C6
MLI_GET_PREFIX    = $C7
MLI_OPEN          = $C8
MLI_NEWLINE       = $C9
MLI_READ          = $CA
MLI_WRITE         = $CB
MLI_CLOSE         = $CC
MLI_FLUSH         = $CD
MLI_SET_MARK      = $CE
MLI_GET_MARK      = $CF
MLI_SET_EOF       = $D0
MLI_GET_EOF       = $D1
MLI_SET_BUF       = $D2
MLI_GET_BUF       = $D3

; I/O soft switches
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

