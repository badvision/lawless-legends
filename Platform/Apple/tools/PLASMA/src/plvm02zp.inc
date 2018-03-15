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
;*                 VM ZERO PAGE LOCATIONS
;*
;**********************************************************
SRC     =   $06
SRCL    =   SRC
SRCH    =   SRC+1
DST     =   SRC+2
DSTL    =   DST
DSTH    =   DST+1
ESGUARD =   $BE
ESTKSZ  =   $20
ESTK    =   $C0
ESTKH   =   ESTK
ESTKL   =   ESTK+ESTKSZ/2
VMZP    =   ESTK+ESTKSZ
IFP     =   VMZP
IFPL    =   IFP
IFPH    =   IFP+1
PP      =   IFP+2
PPL     =   PP
PPH     =   PP+1
IPY     =   PP+2
ESP     =   IPY+1
JMPTMP  =   ESP+1
TMP     =   JMPTMP+1
TMPL    =   TMP
TMPH    =   TMP+1
