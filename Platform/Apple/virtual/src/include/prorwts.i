;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

; ProRWTS equates
proRWTS     = $D000       ;over in aux LC (in bank 1 just like mem mgr)

status      = $3          ;returns non-zero on error
auxreq      = $a          ;set to 1 to read/write aux memory, else main memory is used
sizelo      = $6          ;set if enable_write=1 and writing, or reading, or if enable_seek=1 and seeking
sizehi      = $7          ;set if enable_write=1 and writing, or reading, or if enable_seek=1 and seeking
reqcmd      = $2          ;set (read/write/seek) if enable_write=1 or enable_seek=1
                          ;if allow_multi=1, bit 7 selects floppy drive in current slot
                          ;  (clear=drive 1, set=drive 2) during open call
                          ;bit 7 must be clear for read/write/seek on opened file
ldrlo       = $E          ;set to load address if override_adr=1
ldrhi       = $F          ;set to load address if override_adr=1
namlo       = $C          ;name of file to access
namhi       = $D          ;name of file to access

rwts_mark   = $18	  ;to reset seek ptr, zero out 4 bytes here

cmdseek     = 0           ;requires enable_seek=1
cmdread     = 1           ;requires enable_write=1
cmdwrite    = 2           ;requires enable_write=1
