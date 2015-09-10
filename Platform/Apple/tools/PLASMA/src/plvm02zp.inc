;**************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;**************************************************************************************

;**********************************************************
;*
;* VM ZERO PAGE LOCATIONS
;*
;**********************************************************
SRC	=	$06
SRCL	=	SRC
SRCH	=	SRC+1
DST	=	SRC+2
DSTL	=	DST
DSTH	=	DST+1
ESTKSZ	=	$20
ESTK	=	$C0
ESTKL	=	ESTK
ESTKH	=	ESTK+ESTKSZ/2
VMZP	=	ESTK+ESTKSZ
IFP	=	VMZP
IFPL	=	IFP
IFPH	=	IFP+1
PP      =	IFP+2
PPL	=	PP
PPH	=	PP+1
IPY	=	PP+2
TMP	=	IPY+1
TMPL	=	TMP
TMPH	=	TMP+1
NPARMS	=	TMPL
FRMSZ	=	TMPH
DVSIGN	=	TMP+2
ESP     =       TMP+2
DROP	=	$EF
NEXTOP	=	$F0
FETCHOP	=	NEXTOP+3
IP	=	FETCHOP+1
IPL	=	IP
IPH	=	IPL+1
OPIDX	=	FETCHOP+6
OPPAGE	=	OPIDX+1
