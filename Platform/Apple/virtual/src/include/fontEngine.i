;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

;The following control-codes are used.
;CODE__STATE__DESCRIPTION___
;Ctrl-A (1) foreground/character color
;Ctrl-B (2) background color
;Ctrl-C clear to end of line
;Ctrl-D clear to end of page
;Ctrl-E (3) extended character {A..I}
;Ctrl-F (4) font {0,1,2} (not implemented)
;Ctrl-T (5) horizonTal position {000..279} base-10
;Ctrl-V (6) vertical position {000..191}
;Ctrl-R (7) character/ticker rate {00..FF}
;Ctrl-L n/a toggle underLine mode
;Ctrl-M n/a Carriage return w/line feed
;Ctrl-N n/a Normal mode (un-toggle special modes)
;Ctrl-Q n/a Home cursor & clear screen
;Ctrl-\ n/a Ticker Tape scroll Mode 0=off
;Ctrl-] n/a Ticker Tape scroll Mode 1=on
;Ctrl-P n/a toggle between ticker/scroll mode
;Ctrl-U n/a (right arrow) move +1 column
;Ctrl-H n/a (left  arrow) move -1 column
;Ctrl-J n/a (down  arrow) move +1 row
;Ctrl-K n/a (up    arrow) move -1 row
;Ctrl-I n/a Inverse (swap foregnd/bkgnd colors)
;Ctrl-Y n/a center justify

fontEngine 	= $EC00
fontEngineLen	= $F00		; maximum (allows for some debug code)
SetFont		= fontEngine
SetWindow	= SetFont+3
GetWindow	= SetWindow+3
ClearWindow	= GetWindow+3
CopyWindow	= ClearWindow+3
DisplayChar	= CopyWindow+3
DisplayStr	= DisplayChar+3
CalcWidth	= DisplayStr+3
GetCursor	= CalcWidth+3
GetStr          = GetCursor+3
GetScreenLine	= GetStr+3
NextScreenLine	= GetScreenLine+3
SetScrollLock	= NextScreenLine+3