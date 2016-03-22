;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

;
; Proportional Font Engine
; by Andrew Hogan
;

fontEngine 	= $E000
SetFont		= fontEngine
SetWindow	= SetFont+3
ClearWindow	= SetWindow+3
CopyWindow	= ClearWindow+3
DisplayChar	= CopyWindow+3
DisplayStr	= DisplayChar+3
CalcWidth	= DisplayStr+3
SaveCursor	= CalcWidth+3
RestCursor	= SaveCursor+3
GetStr          = RestCursor+3