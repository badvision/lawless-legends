;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

; Shared definitions for all modules

; Zero page temporary area. Modules can feel free to use the entire space,
; but must *not* count on it being preserved when other modules are in
; control, e.g. when calling other modules, or returning to them.
zpTempStart	= $2                ; 0 and 1 are reserved on c64
zpTempEnd	= $1F

; Zero page monitor locations
cswl		= $36
cswh		= $37
kswl		= $38
kswh		= $39
a2l		= $3E
a2h		= $3F

; Other monitor locations
inbuf		= $200
resetVec	= $3F2

; I/O soft switches
kbd		= $C000
clrAuxRd	= $C002
setAuxRd	= $C003
clrAuxWr	= $C004
setAuxWr	= $C005
clrAuxZP	= $C008
setAuxZP	= $C009
clrC3Rom	= $C00A
setC3Rom	= $C00B
clr80Vid	= $C00C
set80Vid	= $C00D
clrAltCh	= $C00E
setAltCh	= $C00F
kbdStrobe	= $C010
rdLCBnk2	= $C011		;reading from LC bank $Dx 2
rdLCRam		= $C012		;reading from LC RAM
rdRamRd		= $C013		;reading from aux/alt 48K
rdRamWr		= $C014		;writing to aux/alt 48K
rdCXRom		= $C015		;using internal Slot ROM
rdAuxZP		= $C016		;using Slot zero page, stack, & LC
rdC3Rom		= $C017		;using external (Slot) C3 ROM
rd80Col		= $C018		;80STORE is On- using 80-column memory mapping
rdVblBar	= $C019		;not VBL (VBL signal low)
rdText		= $C01A		;using text mode
rdMixed		= $C01B		;using mixed mode
rdPage2		= $C01C		;using text/graphics page2
rdHires		= $C01D		;using Hi-res graphics mode
rdAltCh		= $C01E		;using alternate character set
rd80Vid		= $C01F		;using 80-column display mode

clrText		= $C050
setText		= $C051
clrMixed	= $C052
setMixed	= $C053
page1		= $C054
page2		= $C055
clrHires	= $C056
setHires	= $C057
opnApple	= $C061
clsApple	= $C062

setLcRd		= $C080
setLcWr		= $C081
setROM		= $C082
setLcRW		= $C083
lcBank2		= 0
lcBank1		= 8

; ROM routines (call with care after switching to ROM bank)
ROM_prntax	= $F941
ROM_textinit	= $FB2F
ROM_home	= $FC58
ROM_rdkey	= $FD0C
ROM_getln1	= $FD6F
ROM_crout	= $FD8E
ROM_prbyte	= $FDDA
ROM_prhex	= $FDE3
ROM_cout	= $FDED
ROM_cout1	= $FDF0
ROM_setnorm	= $FE84
ROM_setkbd	= $FE89
ROM_setvid	= $FE93
ROM_prerr	= $FF2D
ROM_bell	= $FF3A
ROM_iosave	= $FF4A
ROM_iorest	= $FF3F
ROM_monrts	= $FF58
ROM_monitor	= $FF69
ROM_getnum	= $FFA7

