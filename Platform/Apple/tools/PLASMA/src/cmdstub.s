;**************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;**************************************************************************************

INTERP	=	$03D0
LCRDEN 	=	$C080
LCWTEN	=	$C081
ROMEN	=	$C082
LCRWEN 	=	$C083
LCBNK2	=	$00
LCBNK1	=	$08
	!SOURCE	"plvm02zp.inc"
;*
;* MOVE CMD DOWN TO $1000-$2000
;*
	LDA	#<_CMDBEGIN
	STA	SRCL
	LDA	#>_CMDBEGIN
	STA	SRCH
	LDY	#$00
	STY	DSTL
	LDA	#$10
	STA	DSTH
-	LDA	(SRC),Y
	STA	(DST),Y
	INY
	BNE	-
	INC	SRCH
	INC	DSTH
	LDA	DSTH
	CMP	#$20		; STOP WHEN DST=$2000 REACHED
	BNE	-
	LDA	#<_CMDEND
	STA	SRCL
	LDA	#>_CMDEND
	STA	SRCH
;
; INIT VM ENVIRONMENT STACK POINTERS
;
	LDA	#$00		; INIT FRAME POINTER
	STA	IFPL
	LDA	#$BF
	STA	IFPH
	LDX	#$FE		; INIT STACK POINTER (YES, $FE. SEE GETS)
	TXS
        LDX	#ESTKSZ/2	; INIT EVAL STACK INDEX
	JMP	$1000
_CMDBEGIN =	*
	!PSEUDOPC	$1000 {
	!SOURCE	"cmd.a"
_CMDEND	=	*
}