;@com.wudsn.ide.asm.hardware=APPLE2
;****************************************************************************************
; Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
; (the "License"); you may not use this file except in compliance with the License.
; You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
; Unless required by applicable law or agreed to in writing, software distributed under 
; the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
; ANY KIND, either express or implied. See the License for the specific language 
; governing permissions and limitations under the License.
;****************************************************************************************

;Title: Proportional Text HGR app
;Author: Andrew Hogan
;This is a collection of routines that prints proportional
;width fonts on the apple][ Hi-Res screen. These routines
;borrow heavily from code developed 3/2014 used to plot
;2240 digits of pi on HGR screen using 3x5 pixel font.
;And, proportional font plottong app for Dbl-Lo-Res
;developed 5/2014. These routines have been tailored 
;for the Lawless Legends project.

;RevCursColHsion list:

;8/08 v.01 original concept w/self contained demo
;9/29 v.05 8-bit BMP code for M, m, W, w
;9/30 v.08 i, j, t, p, q BMPs modified
;          ROM rtn calls replaced w/lookup table
;     v.09 1-pixel & 2-pixel scrolling (buggy), left
;10/1 v.10 center justify
;10/2 v.11 9pixel tall text
;10/3 v.12 inverse (black txt on white bkgnd)
;10/4 v.13 type-over - clears existing pixels
;10/5 v.16 underline txt
;10/6 v.20 demo & app split; ctrl-chr code added
;10/7 v.21 line length parse auto split accommodate
;10/8 v.22 plot blk pixels on blk bkgnd (erase char)
;10/13 v.23 input a string (one line only)
;10/14 v.24 input a single char
;10/27 v.25 comments updated

* = $E000

; Use hi-bit ASCII for Apple II
!convtab "../include/hiBitAscii.ct"

; Global definitions
!source "../include/global.i"
!source "../include/mem.i"
!source "../include/plasma.i"

DEBUG		= 0		; 1=some logging, 2=lots of logging

T1_Val		= $D	 	;zero page Temporary variables
T1_vLo		= $E
T1_vHi		= $F
zTmp1		= $10
zTmp2		= $11
zTmp3		= $12
HgrHrz		= $13		;horizontal index added to base adr
L_Adr		= $14		;Zero page address variable
H_Adr		= $15		;for general indrct adrs indexing
MskBytL		= $16		;Mask byte 1st
MskBytH		= $17		;Mask byte 2nd
MskByte		= $18		;Mask byte
unused19	= $19
PrsAdrL		= $1A		;pointer for string parsing (lo)
PrsAdrH		= $1B		;	(hi)
GBasL		= $26		;LoByte HGR mem pg base adr EABABxxx
GBasH		= $27		;HiByte PPPFGHCD for Y=ABCDEFGH P=page

InBufr		= $200		;Input Buffer
InBufrX		= $2FF		;Input Buffer index (length)

Kbd_Rd		= $C000		;read keyboard
Kbd_Clr		= $C010		;clear keyboard strobe

;Set the address of the font
SetFont		JMP DoSetFont	;API call address

;Set the window boundaries (byte-oriented bounds)
SetWindow	JMP SetWnd	;API call address 

;Clear the window
ClearWindow	JMP ClrHome	;API call address

;Copy the window pg 1 to pg 2
CopyWindow	JMP CpWnd	;API call address

;Display a character, including interpreting special codes
DisplayChar	JMP DoPlAsc

;Display a string, with proper word wrapping
DisplayStr	JMP DoParse	; API call address

;Calculate width of a string without plotting it.
;Does not do line breaking
CalcWidth	JMP DoCWdth

;Save the cursor position
SaveCursor	JMP SvCurs

;Restore the cursor position
RestCursor	JMP RsCurs

;To get a string of text up to 40 chars long using a
;flashing cursor use GetStr. It allows use of either
;left arrow or delete key to backspace.
GetStr		JMP In_Str

;If you know which of the {0..110} bitmapped characters
;you want plotted, you can bypass testing for control 
;codes, making this a faster way to plot.
PlotBmp		JMP PlotFnt	;API call address

;To get a single character using a flashing cursor on
;the HGR screen use GetAsc. The GetYN flag forces the
;user to input either Y or N. No character is output
;to the screen, which allows us to do a password input.
;ChrWdth, PltChar & AscChar will have values after the
;routine executes. Similar to Aplsft GET A$ construct.
GetYN_Flg	!byte $00	;flag: force Y/N input
CursIcn		!byte 103 	;cursor icon 103=cactus
GetAsc		JMP Get_Chr	;API call address

Font0		!word 0		;address of font
CharRate	!byte 0		;plot rate {0..FF} 0=fastest
WaitStat	!byte 0		;Wait State {0,1,2,3,4,5,6,7}
NoPlt_Flg	!byte 0		;flag: NO PLOT - only get width
InvTx_Flg	!byte 0		;flag: Inverse (black on white) text
MskTx_Flg	!byte 0 	;flag: mask HGR before plotting text
UndTx_Flg	!byte 0 	;flag: Underline text
CtrJs_Flg	!byte 0 	;flag: center justify
BkgColor	!byte 0		;color byte {0,80=blk,FF=wht,etc}
FrgColor	!byte $7F	;color byte
CursColL	!byte 0		;Lo-byte of 16-bit horz X-pos value 
CursColH	!byte 0		;Hi-byte X-position {0..279}
CursRow		!byte 0		;vertical Y-position {0..191}
ChrWdth		!byte 0		;character width (number of pixels)
PltChar		!byte 0		;character to be plotted {0..110}
AscChar		!byte 0		;Ascii Char value {$80..$FF}

;Just record the font address.
DoSetFont	STA Font0
		STY Font0+1
		RTS

;This routine converts (x,y) {0..279,0..191} to HGR address 
;{$2000..$3FFF} and an offset bit (for the pixel within the
;byte) using a lookup table and integer division. 
;Then, it loops and stores the HGR addresses (2-byte) for
;9 consecutive lines in an array.
;For HPplot (x,y) y={0..191} represented by bits y=ABCDEFGH
;HGR mem map occupies {$2000..$2FFF} so 'y' is used to make
;a base address for indirect indexed addressing along with
;value $2=PPP=010 for HGR page-1 and $4 for HGR page-2.
;Applesoft ROM routine HPOSN uses page bits PPP and y-axis
;bits ABCDEFG to create based address |PPPFGHCD|EABAB000|
;which is stored in zero page $26, $27; $E5 stores offset.
;This routine uses the same zero page addresses.

GetAdr	LDA CursRow	;Get vert cursor position & save it
	PHA  		;Then, clear storage used for indexing
	LDX #$00	;into array of stored addrs, 1 per line
	STX zTmp3	;Lp1 {0..9} vertical size of the bitmap
	JSR GetOfst	;Get bit offset by dividing {0..279} by 7
GA_Lp1	JSR GetBase	;Get initial addrs location frm lookup tbl
GA_Lp2	LDY zTmp3	;Get index into stored addresses
	CLC
	LDA GBasL	;get lo-byte of HGR address
	ADC HgrHrz	;add the horizontal position offset byte
	STA GA_Ary,Y	;store the composite address in the array
	INY
	LDA GBasH	;load the hi-byte of HGR address; store 
	STA GA_Ary,Y	;that also (each array element is 2-bytes)
	INY
	STY zTmp3	;save the index value of the array
	TYA
	CMP #18 	;when we've stored 9 pairs, we're done
	BPL GA_Done
	INC CursRow	;increment the {0..191} down one line
	LDA CursRow
	AND #$07	;check if it crosses the 'n'* 7th line
	BEQ GA_Lp1	;if so, use ROM to recalc new addrs, else,
	LDA GBasH	;get HiByt of adrs wrd |pppFGHcd|eABABxxx|
;;	CLC  		;(line position is ABCDEFGH bit pattern)
	ADC #$04	;increment the FGH bit pattern
	STA GBasH	;and save the result. This is faster
	BNE GA_Lp2	;than using the GetBase routine every time.
GA_Done	PLA  		;restore vertical position
	STA CursRow 
	RTS
GetBase	LDX CursRow	;Look-up the base HGR memory location
GetBasX	LDA HgrTbHi,X	;(ie. the mem address of the left edge
	STA GBasH	;of the HGR screen)
	LDA HgrTbLo,X	;using a look-up table 192 bytes long x2
	STA GBasL
	RTS
GetOfst	LDA CursColL	;Div 16-bit numerator by 8-bit denominator
	STA HgrHrz	;HgrHrz 7to0 bit numerator
	LDA CursColH
	STA zTmp2	;zTmp2 15to8 bit numerator
	LDX #8
	ASL HgrHrz
D16x8L1	ROL
	BCS D16x8L2
	CMP #7
	BCC D16x8L3
D16x8L2	SBC #7
	SEC
D16x8L3	ROL HgrHrz	;8-bit quotient (offset byte)
	DEX
	BNE D16x8L1
	STA H_Bit	;8-bit remainder (offset bit)
	RTS
H_Bit	!byte 0 	;offset bit - pixels {0..7}
GA_Ary	!fill 18,$0	;Define Storage: 16 bytes preset to 0
; 
;This routine plots the bitmap patters on the HGR screen
;using indirect addressing based on the array of 2-byte 
;address words generated by the prior code.

ChrX10L	!byte $00	;char val x9 offset into bitmap table
ChrX10H	!byte $00	;LO & HI bytes
ChrX10i	!byte $00	;char font bmp index
Byt2nd	!byte $00	;2nd byte of bitmap when stratling 2-bytes
Flg2nd	!byte $00	;flag indicating 2nd byte is needed
MlpIdx	!byte $00	;Main loop index into address table
Flg8xcp	!byte $00	;flag: 8-pixel char exception
FlgBchr	!byte $00	;flag: black character

PlotFnt	LDX NoPlt_Flg
	BNE GetWdth
	JSR GetAdr	;first, load DS_Ary w/2-byte HGR addrs
	LDA FrgColor
	AND #$7F
	EOR #$7F
	STA FlgBchr
GetWdth	LDA #0
	STA MskBytH	;clear mask byte
	STA Flg8xcp	;clear 8 pixel char exception
	TAY
	STA ChrX10H	;clear HI byte of x10 multiplier
	LDA PltChar	;load the font char {0..110}
	ASL  
	STA ChrX10L	;multiply it by 10 to get an index value
	ASL  		;into the array of bytes that make-up the
	ROL ChrX10H	;1-byte (wide) by 10-byte (long) block of
	ASL
	ROL ChrX10H
	ADC ChrX10L 
	STA ChrX10L 
	BCC +
	INC ChrX10H	;save index value {0..990}
+
	CLC
	LDA Font0	;get base address of Font bitmap table
	ADC ChrX10L	;and add the PlotChar x10 offset to it
	STA L_Adr
	LDA Font0+1
	ADC ChrX10H 
	STA H_Adr

	STY MlpIdx	;clear Main Loop index
	LDA (L_Adr),Y	;get character width
	STA ChrWdth
	LDX NoPlt_Flg
	BEQ PltGo
	RTS
PltGo	TAX
	LDA MskTblC,X
	STA MskByte
	STA MskBytL
	CPX #8
	BNE Tst7th
	LDA #3
	STA MskBytH
Tst7th	CPX #7
	BNE MrgnOk
	LDA #1
	STA MskBytH

MrgnOk	INY

LpPlot	STY ChrX10i
	LDA #0  	;right shifted pixels 'fall-off' to the
	STA Flg2nd	;and clear the flag
	STA Byt2nd
	LDX InvTx_Flg
	BEQ InvSkp1
	LDX ChrWdth
	CPX #7
	BMI NoRtBar
	EOR #$01
	STA Byt2nd	;adjacent, 2nd-byte; so start w/blnk byte
NoRtBar	LDA (L_Adr),Y	;get 1-byte (of 9) containg the bitmap img
	LDX UndTx_Flg	;underline flag
	BEQ NoIUlin
	CPY #8
	BNE NoIUlin
	LDA MskByte
NoIUlin	EOR #$FF
	AND MskByte	;these 8 lines of code added 9/29/2014 to
	STA zTmp3	;mask off non-plotted XORed bits
	JMP InvSkp2

InvSkp1	STA Byt2nd
	LDA (L_Adr),Y	;get 1-byte (of 9) containing bitmap
	LDX UndTx_Flg	;underline flag
	BEQ NoTUlin
	CPY #8
	BNE NoTUlin
	LDA MskByte
NoTUlin	STA zTmp3	;save it for future lft/rght pixel shiftng
InvSkp2	LDX ChrWdth	;get character width
	CPX #7
	BMI Flg2Skp
	STX Flg2nd	;if >= 7 then set 2nd-byte-flag
Flg2Skp	CPX #8 		;accommodate 8-bit wide M m W w chars.
	BNE No8th
	TAX 		;if 8 pixel char
	ASL 		;then shift high bit out
	ROL Byt2nd	;and roll bit into 2nd byte
	TXA
	AND #$7F	;and strip off the high bit
	STA zTmp3	;and save bitmap
	LDX UndTx_Flg
	BEQ No8Ulin
	CPY #8
	BNE No8Ulin
	LDA #3
	ORA Byt2nd
	STA Byt2nd
No8Ulin	LDX H_Bit
	CPX #6 		;if 8 pixel char AND
	BNE No8th	;H_Bit (offset) is max
	STX Flg8xcp	;then set 8 pixel exception flag

No8th	LDX H_Bit	;get pixel offset {0..7} 
	BEQ NoAdj	;if 0, bitmap in array needs no adjustmt
	LDA zTmp3	;get the pixel pattern to be shifted
			;    because HGR doesn't display 8th bit
	ASL  		;shift pixel pattern 2x, to 7th bit
			;       (representing the last visible pixel)
LpLBmp	ASL  		;into the CGA_Ary flag. That CGA_Ary value is
	ROL Byt2nd	;is then 'ROL'ed into the 2nd-byte.
	DEX  		;decrement the loop index
	BNE LpLBmp	;if another shift needed, then loop again
			;     now shift the pixel pattrn back 1 positn
	LSR  		;so all pixels are visible [8th bit not
	STA zTmp3	;visible]. Save the pixel pattern.
	DEY
	BNE LpLMskp	;Only shift mask bits on 1st loop

	LDX H_Bit	;Do the same shifting for the mask bits.
	LDA MskBytL
	ASL
LpLMsk	ASL
	ROL MskBytH
	DEX
	BNE LpLMsk
	LSR
	STA MskBytL

LpLMskp	LDA Byt2nd	;if pixels got rolled into 2nd byte
	ORA Flg2nd	;then set 2nd-byte-flag
	STA Flg2nd
	LDX MskTx_Flg	;code needed when using mask mode
	BEQ NoAdj	;and char pixels blank
	LDA MskBytH
	BEQ NoAdj
	STA Flg2nd

NoAdj	LDX MlpIdx	;get indx into 2-byt adrs wrds {0,2,4,etc}
	LDA GA_Ary,X	;get lo-byte HGR address word from array
	STA zTmp1	;save in Zpage adrs (to do indrct adrssng)
	INX  		;update 2-byte index to point to the hi-byte
	LDA GA_Ary,X	;get hi-byte HGR address word
	STA zTmp2	;save in Zpage+1 address
	LDY MskTx_Flg
	BEQ NoMask
	LDY #0  	;clear the byte offset index
	LDA MskBytL	;Load mask bit pattern

; MH Note: The following section (DoAgn up to but not including DoAgnNM) is never used
; in Lawless Legends, because we never set mask mode.
DoAgn	PHA
	AND zTmp3
	STA zTmp3
	PLA
	EOR #$FF	;flip the mask so it works with AND
	AND (zTmp1),Y	;Mask off bits where char BMP will go
	ORA zTmp3	;add the char BMP bits into the pixels
	ORA #$80	;   (set high bit for the demo)
	STA (zTmp1),Y	;write to HGR. Use indrct, indxd adrssing
	LDA Flg2nd	;check if pixel pattern crosses 2-bytes
	BEQ Chk8xcp	;if not, then skip to next line of bitmap
	STY Flg2nd	;else, first, clear the flag
	LDA Byt2nd	;get the 2nd byte
	STA zTmp3	;store it in pixel pattern to be plotted
	LDA MskBytH
	INY  		;increment the byte offset index
	BNE DoAgn	;go plot the 2nd half of the pixel pattern
Chk8xcp	CMP InvTx_Flg	;save carry for later
	LDA Flg8xcp
	BEQ SkpLine
	INY
	LDA (zTmp1),Y
	BCC Chk8xcI	;CMP was non-zero
	AND #$FE
	STA (zTmp1),Y
	BCS SkpLine
Chk8xcI	ORA #1
	STA (zTmp1),Y
	BNE SkpLine

NoMask	;;LDY #0  	;clear the byte offset index
DoAgnNM	LDA FlgBchr
	BEQ NoBchrP
	LDA zTmp3
	EOR #$FF
	AND (zTmp1),Y	;get HGR pixels
	STA zTmp3
NoBchrP	LDA (zTmp1),Y	;get HGR pixels
	ORA zTmp3	;add the char BMP bits into the pixels
	ORA #$80	;   (set high bit for the demo)
	STA (zTmp1),Y	;write to HGR. Use indrct, indxd adrssing
	LDA Flg2nd	;check if pixel pattern crosses 2-bytes
	BEQ SkpLine	;if not, then skip to next line of bitmap
	STY Flg2nd	;else, first, clear the flag
	LDA Byt2nd	;get the 2nd byte
	STA zTmp3	;store it in pixel pattern to be plotted
	INY  		;increment the byte offset index
	BNE DoAgnNM	;go plot the 2nd half of the pixel pattern

SkpLine	INX  		;increment the array index
	STX MlpIdx 
	CPX #$12	;if > 18 then we're done
	BEQ DonePlot
	LDY ChrX10i	;else get the index into the bitmap array
	INY  		;advanc indx to nxt pixl pattern in array
	JMP LpPlot	;loop thru all 9 pixl patrns in the bitmap
DonePlot
	LDX CtrJs_Flg 
	BEQ MvCurs
	JSR CtrJstfy
MvCurs	JMP AdvCurs
MskTblC	!byte $01 	;Mask Table for Chars
	!byte $03
	!byte $07
	!byte $0F
	!byte $1F
	!byte $3F
	!byte $7F
	!byte $FF
	!byte $FF

;This section advances the character cursor to the right n pixels.
;If the cursor gets to the right side of the screen then it 
;returns to the left, 0, and advances, vertically, 9 lines.

CursY	!byte 24 	;Cursor home position - Y vert
CursXl	!byte 154 	;Cursor home lo byte - X horz
CursXh	!byte 0 	;Cursor home hi byte - X horz
CursXml	!byte 210 	;Cursor midpoint lo byte - X horz
CursXmh	!byte 0 	;Cursor midpoint hi byte - X horz
CursXrl	!byte 7 	;Cursor lo byte right boundary
CursXrh	!byte 1 	;Cursor hi byte right boundary
CursYb	!byte 130 	;Cursor txt bottom boundary
AdvCurs	LDA CursColL	;get lo-byte of {0..279}
	SEC
	ADC ChrWdth	;add char width (about 5 pixels)
	STA CursColL	;save lo-byte
	LDA CursColH	;get hi-byte of {0..279}
	ADC #0
	STA CursColH
	CMP CursXrh	;if pixel position {0..255} 
	BMI DoneCurs	;then done
	LDA CursColL	;else check if past 263
	CMP CursXrl
	BMI DoneCurs	;if not then done
DoCrLf	LDA CtrJs_Flg
	BEQ Adv154
	LDA CursXml
	BNE Adv210
Adv154	LDA CursXl	;if so then reset the horizontal
Adv210	STA CursColL	;position to 154
	LDA CursXh
	STA CursColH
	STA WrdWdth	;and, clear Word Width total
	STA TtlScrl	;and ticker scroll total
	LDA CursRow	;Get vertical {0..191}
	ADC #8  	;increment by 9 lines, down (carry is set already)
	CMP CursYb	;check if it's past 130
	BCC DoneLin	;if not then done
	JMP ScrlTxt	;else scroll the text up 1 line
DoneLin	STA CursRow	;save vertical position
DoneCurs LDA CharRate	;get character rate / delay time
	BEQ Wait_skp	;skip if no wait
	JMP WtL_Wait	;delay before plotting next char
Wait_skp RTS

;Wait that can be interrupted by a key or button press.
WtL_V1	!byte $10
WtL_Wait LSR  		;cut param in half to make compatible
WtL_Lp1	STA WtL_V1	;with values used by MON_WAIT
WtL_Lp2	LDX Kbd_Rd	;check for key press (but do NOT
	BMI WtL_Prs	;clear the keyboard strobe)
	DEC WtL_V1
	BNE WtL_Lp2	;count down to 0
	SEC
	SBC #1 		;and then count down again & again
	BNE WtL_Lp1	;starting from 1 less than before
	RTS
WtL_Prs	LDA #0 	;	if wait interrupted then do
	STA CharRate	;plotting as fast as possible
	LDA #$FF
	STA ChBflip
	RTS

;Routine: Save the cursor position. There is exactly one save slot.
BCursColL	!byte 0		;Saved Lo-byte of 16-bit horz X-pos value 
BCursColH	!byte 0		;Saved Hi-byte X-position {0..279}
BCursRow	!byte 0		;Saved vertical Y-position {0..191}
SvCurs	LDA CursColL
	STA BCursColL
	LDA CursColH
	STA BCursColH
	LDA CursRow
	STA BCursRow
	RTS

;Routine: Restore the cursor position. There is exactly one save slot.
RsCurs	LDA BCursColL
	STA CursColL
	LDA BCursColH
	STA CursColH
	LDA BCursRow
	STA CursRow
	RTS

;Routine: Set window boundaries. Paramaters are pushed on the PLASMA
;stack in the order Top, Bottom, Left, Right. But because that stack
;grows downward, we see them in this order:
SW_RT	= 0
SW_LT	= 1
SW_BTM	= 2
SW_TOP	= 3
SetWnd	LDA evalStkL+SW_TOP,X	;get top coord
	STA CursY		;save the top Y coord
	STA CursRow		;also as current cursor vertical pos
	TAY
	DEY			;adjust by 1
	STY TpMrgn		;	for scrolling margin
	LDA evalStkL+SW_BTM,X	;get bottom coord
	STA CursYb		;save the bottom Y coord
	TAY
	DEY			;adjust by 1
	STY BtMrgn		;	for scrolling margin
	LDA evalStkL+SW_LT,X	;lo byte of left X
	STA CursXl
	LDA evalStkH+SW_LT,X	;hi byte of left X
	STA CursXh
	LDA evalStkL+SW_RT,X	;lo byte of right X
	STA CursXrl
	LDA evalStkH+SW_RT,X	;hi byte of right X
	STA CursXrh
	LDA CursXl		;sum the left X and right X
	CLC
	ADC CursXrl
	TAY			;save lo byte
	LDA CursXh		;sum the hi byte
	ADC CursXrh
	LSR			;divide by 2 to find the midpoint
	STA CursXmh		;save midpoint lo byte
	TYA
	ROR			;divide lo byte by 2, with bit from hi byte
	STA CursXml		;save midpoint hi byte
	LDA CursXrl		;need to figure out byte number of right side
	STA CursColL
	LDA CursXrh
	STA CursColH
	JSR GetOfst		;we have a routine for that
	LDA HgrHrz
	STA RtMrgn		;that's the right margin for scrolling
	LDA CursXl		;similarly we need to figure out the byte number of the left side
	STA CursColL
	LDA CursXh
	STA CursColH
	JSR GetOfst
	LDA HgrHrz
	STA LfMrgn		;that's the left margin for scrolling
	LDA #0
	STA TtlScrl		;clear centering variables
	STA WrdWdth
	RTS

;Routine: Scroll screen up 1 character line
;This routine scrolls a window defined by 
;Left, Right, Top, Bottom - Margin parameters.
;It scrolls using by moving bytes (so scrolling
;only works along 7-pixel boundaries of HGR).
;Vertically, the margins can be anywhere along
;{0..191}. Horizontally, the margins are at the
;borders of {0, 7, 14, 21 .. 273} pixel columns.
LfMrgn	!byte 22 	;left margin (byte #)
RtMrgn	!byte 38 	;right margin (byte #)
TpMrgn	!byte 23 	;top margin (v-line)
BtMrgn	!byte 135 	;bottom margin (v-line)
ScrlTxt	LDX TpMrgn 
	STX zTmp3	;Duplicate top margin val in zero-pg
ScrLp1	LDX zTmp3
	INX  		;go thru each line 1 by 1
	STX zTmp3
	JSR GetBasX	;Get base value of line address
	LDA GBasL
	STA zTmp1	;and save into zero-pg
	LDA GBasH
	STA zTmp2
	TXA
	CLC
	ADC #9  	;go down 9 (txt is 9 pixels tall)
	TAX
	JSR GetBasX	;Get base address, again
	LDY LfMrgn
ScrLp2	LDA (GBasL),Y	;copy the pixels from down screen
	STA (zTmp1),Y	;to 9 lines up
	INY
	CPY RtMrgn	;do from left margin to right margin
	BNE ScrLp2
	CPX BtMrgn	;keep looping until all the way to
	BNE ScrLp1	;the bottom margin.

	LDX zTmp3	;Clear the last 9 pixel lines
ScrLp3	INX  		;so a new text line can be plotted
	JSR ClrChkF	;Check background color
	BEQ ScrClbw	;then clear the bottom txt row
	JMP ClrSlp3
ScrClbw	JMP ClrSlp1

;Routine: clear screen
;Home cursor within the window boudaries set by margin params
;and clear the window.
ClrFlip	!byte 0
ClrFlpF	!byte 0
ClrHome	LDA CursXl	;home the cursor
	STA CursColL	;{0..279} lo byte
	LDA CursXh
	STA CursColH	;{0..279} hi byte  
	LDA CursY
	STA CursRow	;{0..191}
	JSR ClrChkF	;check if B/W or Color
	LDX TpMrgn	;get top margin & use it
	LDA ClrFlpF
	BNE ClrColr
ClrSlp1	JSR GetBasX	;to get the base address
	LDY LfMrgn
	LDA BkgColor
	ORA #$80	;   (set high bit for the demo)
ClrSlp2	STA (GBasL),Y
	INY
	CPY RtMrgn
	BNE ClrSlp2
	INX
	CPX BtMrgn
	BCC ClrSlp1	;loop while less than
	BEQ ClrSlp1	;...or equal
	RTS

ClrColr	LDA BkgColor
	TAY
	EOR #$7F
	STA ClrFlip
	LDA LfMrgn
	AND #1
	BEQ ClrSlp3
	TYA
	STA ClrFlip 
ClrSlp3	JSR GetBasX	;to get the base address
	LDY LfMrgn
	LDA ClrFlip
	ORA #$80	;   (set high bit for the demo)
ClrSlp4	STA (GBasL),Y
	INY
	CPY RtMrgn
	BNE ClrSlp4
	INX
	CPX BtMrgn
	BNE ClrSlp3
	RTS

ClrChkF	LDA BkgColor
	AND #$7F
	EOR #$7F
	BEQ ClrChk1
	EOR #$7F
ClrChk1	STA ClrFlpF
	RTS

;Routine: copy hi-res page 1 to page 2, the window area only
CpWnd	LDX TpMrgn
CpWnd1	LDA HgrTbHi,X	;(ie. the mem address of the left edge
	STA GBasH	;of the HGR screen)
	EOR #$60	;turn off $20 bit, turn on $40 bit to get page 2
	STA H_Adr
	LDA HgrTbLo,X	;using a look-up table 192 bytes long x2
	STA GBasL
	STA L_Adr
	LDY LfMrgn
CpWnd2	LDA (GBasL),Y
	STA (L_Adr),Y
	INY
	CPY RtMrgn
	BNE CpWnd2
	INX
	CPX BtMrgn
	BNE CpWnd1
	RTS

;Routine: parser w/auto line break
DoParse	STA PrsAdrL
	STY PrsAdrH
	LDA CursXrl	;right coord
	SEC
	SBC CursXl	;minus left coord
	STA LinWdth	;equals line width (max of 255 for now)
	LDY #0  	;parse starting at beginning
	STY TtlWdth
	LDA (PrsAdrL),Y ;Get the length
	STA Pa_Len
	INY
Pa_Lp0	STY Pa_iBgn 
Pa_Lp1	STY Pa_iSv
	LDA (PrsAdrL),Y ;Get the character
	STA AscChar
	CPY Pa_Len	;reached end of string?
	BCC Pa_Go
	BNE Pa_Spc
Pa_Go	ORA #$80	;set hi bit for consistent tests
	STA AscChar
	CMP #$8D
	BEQ Pa_Spc
	LDX #1
	STX NoPlt_Flg	;set NO PLOT flag
	JSR TestChr 	;do plot routine to strip off Ctrl
	LDX #0  	;codes & get char width
	STX NoPlt_Flg 	;clear NO PLOT flag
	LDA ChrWdth
	BEQ Pa_Tskp
	SEC  		;use SEC to always 'add 1'
	ADC TtlWdth
	STA TtlWdth
Pa_Tskp	LDA AscChar
	CMP #' '
	BEQ Pa_Spc
	LDA TtlWdth
	CMP LinWdth
	BPL Pa_ToFr 	;too far! force CR/LF
	LDY Pa_iSv
	INY
	BNE Pa_Lp1
Pa_ToFr	!if DEBUG { +prChr '+' }
	;MH: I added this, but it doesn't actually work. Skips first char on line sometimes.
	;LDY Pa_iSv	;if word too big
	;CPY Pa_iBgn	;	for one line
	;BEQ Pa_Spc	;		then split the word
	LDA #$8D
	STA AscChar
	!if DEBUG { +prChr '!' : ora #$80 : jsr cout }
	JSR TestChr
	LDY Pa_iBgn
	LDA #0
	STA TtlWdth
	BEQ Pa_Lp1
;
Pa_Spc	LDY Pa_iSv
	STY Pa_iEnd
	LDY Pa_iBgn
	CPY Pa_iEnd
	BEQ Pa_Dn2
Pa_Lp2	STY Pa_iSv
	LDA (PrsAdrL),Y ;Get the character
	STA AscChar 	;**add code
	!if DEBUG { ora #$80 : jsr cout }
	JSR TestChr 	;if space & at left then don't plot
	LDY Pa_iSv
	INY
	CPY Pa_iEnd
	BNE Pa_Lp2
Pa_Dn2	STY Pa_iSv
	CPY Pa_Len	;end of the message?
	BCC Pa_Dn2b
	BNE ParsDn	;if so, stop here
Pa_Dn2b	LDA TtlWdth
	CMP LinWdth
	BPL Pa_Dn3
	LDA (PrsAdrL),Y ;Get the character
	CMP #$8D
	BEQ Pa_Dn3
	STA AscChar
	!if DEBUG { +prChr '>' : ora #$80 : jsr cout }
	JSR TestChr 
	JMP Pa_Dn4
Pa_Dn3	LDY Pa_iSv
	INY
	STY Pa_iBgn
	JMP Pa_ToFr
Pa_Dn4	LDY Pa_iSv
	INY
	JMP Pa_Lp0
ParsDn	!if DEBUG { +prChr '<' : +crout : BIT $C053 }
	RTS
;
;Routine: Calculate width of string without plotting it
DoCWdth	STA PrsAdrL
	STY PrsAdrH
	LDY #0  	;parse starting at beginning
	STY TtlWdth
	LDA (PrsAdrL),Y ;Get the length
	STA Pa_Len
	INY
Cw_Lp	LDA (PrsAdrL),Y ;Get the character
	STA AscChar
	CPY Pa_Len	;reached end of string?
	BCC Cw_Go
	BEQ Cw_Go
	LDA TtlWdth	;return width in A=lo/Y=hi
	LDY #0
	RTS
Cw_Go	ORA #$80	;set hi bit for consistent tests
	STA AscChar
	STY Pa_iSv
	LDX #1
	STX NoPlt_Flg	;set NO PLOT flag
	JSR TestChr 	;do plot routine to strip off Ctrl
	LDX #0  	;codes & get char width
	STX NoPlt_Flg 	;clear NO PLOT flag
	LDA ChrWdth
	BEQ Cw_Tskp
	SEC  		;use SEC to always 'add 1'
	ADC TtlWdth
	STA TtlWdth
Cw_Tskp	LDY Pa_iSv
	INY
	JMP Cw_Lp
;
LinWdth	!byte 112 	;max line width
TtlWdth	!byte $00 	;total word width
Pa_iBgn	!byte $00 	;parser indx begin
Pa_iEnd	!byte $00 	;parser indx end
Pa_iSv	!byte $00	;Save Msg Y index
Pa_Len	!byte $00	;length of string

;Center Justify
;Start with the cursor in the center column, 
;Get width of letter to be plotted, use ticker scrolling 
;to shift the line half the char width prior to plotting it.
WrdWdth	!byte 0 	;sum of char widths
WrdScrl	!byte 0 	;half of word width
LtrScrl	!byte 0 	;half of char width
TtlScrl	!byte 0 	;cumulative sum of scrolls
LpNScrl	!byte 0 	;number of scroll loops
CtrJstfy CLC
	LDA WrdWdth
	ADC ChrWdth
	ADC #1
	STA WrdWdth	;WrdWdth = WrdWdth + ChrWdth
	TAX
	LDA ChrWdth
	LSR 
	STA LtrScrl	;LtrScrl = ChrWdth / 2
	CLC
	LDA TtlScrl
	ADC LtrScrl
	STA TtlScrl	;TtlScrl = TtlScrl + LtrScrl
	TXA  		;Get WrdWdth
	LSR
	STA WrdScrl	;WrdScrl = WrdWdth / 2
	SEC
	SBC TtlScrl
	TAX  		;Delta = WrdScrl - TtlScrl
	CLC
	ADC TtlScrl
	LDY Flg2px	;2-pixel shifting?
	BEQ CtrSTs	;if not, Save TtlScrl
	LSR  		;else divide by 2
	ASL  		;then x 2 (to make even #)
CtrSTs	STA TtlScrl	;Save TtlScrl
	TXA  		;Get Delta
	CLC
	ADC LtrScrl
	LDY Flg2px	;2-pixel shifting? 
	BEQ CtrSLps	;if not, Save LpNScrl
	LSR
CtrSLps	STA LpNScrl	;Save # of scroll loops
	LDA CursColL	;get current column LoByte
	SEC  		;(CLC is intentional, here)
	SBC LpNScrl	;bump it back
	STA CursColL	;save lo-byte
	BCS +
	DEC CursColH	;get hi-byte of {0..279}
+	LDA LpNScrl	;Get # of scroll loops
CtrLp1	JSR Sc1_Bgn
	DEC LpNScrl
	BNE CtrLp1
	RTS

;Routine: Scroll char width
Tikr_Mod !byte 0 	;ticker mode
Tikr_Flg !byte 0 	;ticker flag
NoBf_Flg !byte 0 	;no-buffer-use flag
Sc1_Tkr	RTS

;Routine: shift one line of text left by 2 pixels
;Using a buffer of unshifted and preshifted pixel
;bytes, this routine uses ASLs and ROLs to shift 
;8 consecutive lines
Sc1_Bgn	LDX #0
	STX Sc1LpC
	LDA CursRow 
	PHA
	SEC
	LDA RtMrgn
	SBC LfMrgn
	STA MrgnVl
; 
Sc1_LpM	JSR GetBase
	CLC
	LDA GBasL
	ADC LfMrgn
	STA GBasL

Sc1_Lp0	LDY #0
Sc1_Lp1	LDA (GBasL),Y	;get pixel byte
	STA Ary1,Y	;save unaltered pixels
	TAX
	AND #$80
	STA AryHB,Y	;save High (color) bit
	TXA
	ASL  		;shift pixels right
	STA Ary2,Y	;save shifted pixels
	INY
	CPY MrgnVl
	BNE Sc1_Lp1
	LDA #0
	STA Ary1,Y	;Clear buffer byte

	LDX Flg2px
	BNE Sc1_2px

	TAX
	TAY
Sc1_LpS	INX
	LDA Ary1,X	;Get unaltered pixels
	DEX
	LSR  		;shift them left
	LDA Ary2,X	;get shifted pixels
	ROR  		;roll them left
	LSR  		;1-more left to get past high bit
	ORA AryHB,X	;put high bit back into byte
	STA (GBasL),Y	;plot on screen
	INY
	INX
	CPX MrgnVl
	BNE Sc1_LpS
	BEQ Sc1_Nxt

Sc1_2px	TAX
	TAY
Sc1_Lp2	INX
	LDA Ary1,X	;Get unaltered pixels
	DEX
	LSR  		;shift them left
	ROR Ary2,X	;ROL them into right shifted pixels
	LSR  		;shift them left again
	LDA Ary2,X	;get shifted pixels
	ROR  		;roll them left
	LSR  		;1-more left to get past high bit
	ORA AryHB,X	;put high bit back into byte
	STA (GBasL),Y	;plot on screen
	INY
	INX
	CPX MrgnVl
	BNE Sc1_Lp2

Sc1_Nxt	INC Sc1LpC
	LDA Sc1LpC
	CMP #9
	BEQ Sc1_Dn
	INC CursRow	;increment the {0..191} down one line
	LDA CursRow
	AND #$07	;check if it crosses the 'n'* 7th line
	BEQ Sc1_LpM	;if so, use ROM to recalc new addrs, else,
	LDA GBasH	;get HiByt of adrs wrd |pppFGHcd|eABABxxx|
;;	CLC  		;(line position is ABCDEFGH bit pattern)
	ADC #$04	;increment the FGH bit pattern
	STA GBasH	;and save the result. This is faster
	JMP Sc1_Lp0	;than using the GetBase routine every time.
Sc1_Dn	PLA  		;restore vertical position
	STA CursRow 
	RTS

MrgnVl	!byte 0  	;Margin Value
Sc1LpC	!byte 0  	;Loop count
Flg2px	!byte 0  	;Flag: shift two pixels (vs. one)
Ary1	!fill 21,0 	;unshifted pixel bytes
Ary2	!fill 21,0 	;shifted pixel bytes
AryHB	!fill 21,0 	;high bits

LdSvFlg	!byte $00
BmpBkp	!fill 16,$0	;Define Storage: 16 bytes preset to 0

;Routine: Get Char
;Input a single character using flashing cursor.
;Control characters are captured except for 
;Ctrl-E which is used to enter extended chars.
Get_Chr	LDA #0 		;clear
	STA InBfrX	;index into input buffer
	JSR In_sCur	;save cursor position
Get_Lp1	JSR CurFlsh	;flash cursor
	LDA Kbd_Rd	;check keybd for char
	BPL Get_Lp1	;if none, keep looping
	STA Kbd_Clr	;else clear kbd strobe
	LDX WaitStat	;waiting for ext char?
	BEQ Get_Ext	;if not, check for Ctl-E
	JSR In_eChP	;else get ext char val
	JMP Get_Wdt	;and its width
Get_Ext	CMP #$85
	BNE Get_Ch3	;Ctrl-E (extended char)
	LDA #3
	STA WaitStat	;if pressed, wait for val
	BNE Get_Lp1
Get_Ch3	LDX InBfrX	;else normal char pressed
	STA InBufr,X	;store ASCII char w/hi-bit
	AND #$7F	;strip off hi-bit
	STA AscChar	;save it
	SEC
	SBC #32 	;adjust to {0..95}
	BMI Get_tYN
	STA PltChar	;save char val to be plotted
Get_Wdt	LDX #1
	STX NoPlt_Flg	;set No Plot flag
	JSR PlotFnt	;get the char width
	LDX #0 		;clear No Plot flag
	STX NoPlt_Flg
Get_tYN	LDX GetYN_Flg	;test for Y/N
	BEQ Get_Dn	;if no test, then done
	LDA AscChar 
	AND #$5F	;force upper case
	ORA #$80	;force hi-bit
	CMP #'Y'
	BEQ Get_Dn
	CMP #'N'
	BEQ Get_Dn
	JSR SndErr	;if not Y/N, sound ERR
	JMP Get_Lp1	;and keep looping
Get_Dn	JSR In_Bfr	;save char to input buffer
	LDA ChBufr	;restore PltChar & ChrWdth
	STA PltChar	;after flashing cursor
	LDA CwBufr	;is erased
	STA ChrWdth
	JMP In_Exit	;append $0 delimiter

;Routine: Sound Error tone
;Used to give user feedback that the input is bad
SndErr	LDY #$03 	;loop thrice
SndLp3	LDX #$64	;Load X-register w/pitch val
SndLp2	TXA
	CLC   		;Clear carry flag
SndLp1	SBC #$01	;Subtract from Acc with carry
	BNE SndLp1	;loop if Acc is not 0
	STA $C030	;toggle the speaker output
	DEX 		;Decrement X-register
	CPX #$32	;Compare to Pitch parameter
	BNE SndLp2	;loop if not =al
	DEY 		;step 'Loops' parameter down to zero
	BNE SndLp3	;loop if not =al
	RTS

;Routine: Input string (simple, one-line)
;Input a line of characters keeping track of cursor
;positions by way of character widths, allowing use
;of delete key; ignore control keys except Escape, 
;Return, and Delete; treat left arrow like Delete.
;The ASCII string is stored in the input buffer at
;$200 followed by $00 delimiter & the length of the
;string is at $2FF.
In_Str	LDA #0
	STA WaitStat 	;clear wait state
	STA InBfrX	;clear buffer index
	JSR In_sCur	;save cursor position
In_Key	JSR CurFlsh	;flash cursor
	LDA Kbd_Rd	;read the keyboard
	BPL In_Key	;if not pressed, loop
	STA Kbd_Clr	;else clear the kbd strobe
	LDX WaitStat	;get wait status
	BEQ In_cTst	;if none then test ctrl chars
	JSR In_eChP	;else, get extended char
	JMP In_Plt
In_cTst	CMP #$85 
	BNE In_cTs2	;Ctrl-E (extended char)
	LDA #3 		;set wait state for extended char
	STA WaitStat
	BNE In_Key
In_cTs2	CMP #$9B	;check for ESC key
	BNE In_cTs3	;if ESC then exit app
	PLA
	PLA
	RTS
In_cTs3	CMP #$FF
	BNE In_cTs4
	JMP In_DEL	;DELETE key?
In_cTs4	CMP #$88
	BNE In_cTs5
	JMP In_DEL	;Left Arrow key?
In_cTs5	CMP #$8D
	BNE In_cTs6	;Return key?
In_Exit	LDA #0
	LDX InBfrX
	STX InBufrX
	STA InBufr,X
	STA ChBufr,X
	STA CwBufr,X
	RTS
In_cTs6	CMP #' '
	BMI In_Key	;ignore all other Ctl keys
	LDX InBfrX
	STA InBufr,X	;store ASCII char w/hi-bit
	AND #$7F	;strip off hi-bit
	SEC
	SBC #32 	;adjust to {0..95}
	STA PltChar	;store character to be plotted
	STA NwPChar	;and save it
In_Plt	LDX #1
	STX NoPlt_Flg	;set No Plot flag
	JSR PlotFnt	;get the char width
	LDX #0 		;clear No Plot flag
	STX NoPlt_Flg
	CLC
	LDA CursColL	;does new char width go past
	ADC ChrWdth	;right margin?
	STA CursColL
	LDA CursColH
	ADC #0
	STA CursColH
	CMP CursXrh	;if so, ignore it, sound ERR,
	BMI In_Bchk	;wait for different key press
	LDA CursColL	;allow 2 more pixels for cursor
	ADC #1
	CMP CursXrl 
	BPL In_Err
In_Bchk	LDX InBfrMx
	CPX InBfrX	;check for buffer overflow
	BPL In_SvCh	;if ok, store in buffer
In_Err	JSR In_rCur	;else, restore cursor position
	JSR SndErr	;and make ERR sound
	JMP In_Key
In_Bfr	LDX InBfrX
	LDA PltChar	;save plot val & char width
	STA ChBufr,X	;into respective buffers
	LDA ChrWdth
	STA CwBufr,X
	INX
	STX InBfrX
	LDX #0
	STX ChBflip	;reset cursor s=ence
	JMP CurBplt	;erase cursor
In_SvCh	JSR In_Bfr 
	LDA NwPChar	;restore new plot char
	STA PltChar
	JSR PlotFnt	;plot it
	JSR In_sCur	;save new cursor position
	JMP In_Key
In_sCur	LDA CursColL	;get cursor position and
	STA sCrsXl	;save it
	LDA CursColH
	STA sCrsXh
	LDA CursRow
	STA sCrsY
	RTS
In_rCur	LDA sCrsXl	;restore cursor position
	STA CursColL
	LDA sCrsXh
	STA CursColH
	LDA sCrsY
	STA CursRow
	RTS
In_eChP	AND #$0F	;clamp the value
	CLC
	ADC #95 	;calculate offset
	STA PltChar	;store character to be plotted
	STA NwPChar	;and save it
	ADC #32
	STA AscChar
	ADC #160
	LDX InBfrX
	STA InBufr,X	;store char in buffer
	LDX #0
	STX WaitStat	;clear wait state
	RTS
In_DEL	LDX InBfrX	;get buffer index
	BEQ In_DlDn	;make sure it's not zero
	DEX
	STX InBfrX	;decrement it
	LDA CwBufr,X	;get char width from buffer
	STA ChrWdth	;save it
	CLC
	LDA CursColL	;subtract char width from
	SBC ChrWdth	;cursor position, to reposition
	STA CursColL	;cursor one char to the left
	BCS +
	DEC CursColH
+	JSR In_sCur	;save new cursor position
	LDA ChBufr,X	;get char from buffer
	STA PltChar	;save it
	LDX #$80
	STX FrgColor	;set foregnd color to black
	JSR DelPlot	;delete the character
In_DlDn	JMP In_Key
CurFlsh	LDA #$C0	;set wait time
	JSR WtL_Lp1	;count down to flash
	LDA ChBflip
	EOR #$FF
	STA ChBflip
	BMI CurPlot
CurBplt	LDA #$80
	STA FrgColor
CurPlot	LDA CursIcn	;get cursor icon/char
DelPlot	STA PltChar	;store it
	JSR PlotFnt	;plot it
	LDA #$FF
	STA FrgColor
	JSR In_rCur	;put cursor left of icon
	LDA #0
	STA PltChar	;plot char val
	STA ChrWdth	;char width val
	RTS
NwPChar	!byte 0 	;new char's Font index value
InBfrX	!byte 0  	;x reg index into buffer
InBfrMx	!byte 40 	;max buffer size
sCrsXl	!byte 0 	;start X cursor position lo byt
sCrsXh	!byte 0 	;start X cursor hi byte
sCrsY	!byte 0  	;start Y cursor position
ChBflip	!byte 0 	;blink flip
ChBufr	!fill 40,0	;input buffer ($200 not used)
CwBufr	!fill 40,0	;Char Width Buffer

;Test for Control Keys when using ASCII characters
DoPlAsc	STA AscChar	;store the ASCII character
TestChr	LDX #0
	STX ChrWdth
	AND #$7F	;strip off HiBit
	TAX 		;save it
	AND #$E0	;check for Ctrl-character
	BEQ TestCtl	;if so, test it
	TXA
	CLC 		;else
	SBC #31 	;adjust to {0..95}
	STA PltChar	;store character to be plotted
	LDA WaitStat	;get wait status
	BNE WaitPrm	;if waiting, then get parameter
	JMP PlotFnt	;else done
WaitPrm	JMP GetParm

;This section tests for control characters. 
;The following control-codes are used. Those that
;require a paramter to follow will set a wait state
;CODE__STATE__DESCRIPTION___
;Ctrl-A (1) foreground/character color (not implemented)
;Ctrl-B (2) background color 
;Ctrl-E (3) extended character {A..I} 
;Ctrl-F (4) font {0,1,2} (not implemented)
;Ctrl-T (5) horizonTal position {000..279} base-10
;Ctrl-V (6) vertical position {000..191}
;Ctrl-R (7) character/ticker rate {00..FF} (not used)
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
TestCtl	TXA 		;restore character
	CMP #$0D	;Ctrl-M?
	BNE TCl_01	;no - keep testing
	JMP DoCrLf	;yes - do CR / LF
TCl_01	CMP #$11	;Ctrl-Q?
	BNE TCl_02	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_01a
	RTS
TCl_01a	JMP ClrHome	;yes - do HOME command
TCl_02	CMP #$01	;Ctrl-A? foreground color
	BNE TCl_03	;no - keep testing
	STA WaitStat	;yes - set wait state
	RTS
TCl_03	CMP #$02	;Ctrl-B? background color
	BNE TCl_04	;no - keep testing
	STA WaitStat	;yes - set wait state
	RTS
TCl_04	CMP #$05	;Ctrl-E? extended character
	BNE TCl_05	;no - keep testing
	LDA #3 		;yes - set wait state
	STA WaitStat
	RTS
TCl_05	CMP #$06	;Ctrl-F? change font
	BNE TCl_06	;no - keep testing
; LDA #4 	;yes - not implemented
; STA WaitStat
	RTS
TCl_06	CMP #$14	;Ctrl-T? HTAB
	BNE TCl_07	;no - keep testing
	LDA #5 		;yes - set wait state
	STA WaitStat
	RTS
TCl_07	CMP #$16	;Ctrl-V? VTAB
	BNE TCl_08	;no - keep testing
	LDA #6 		;yes - set wait state
	STA WaitStat
	RTS
TCl_08	CMP #$12	;Ctrl-R? Char/Ticker rate
	BNE TCl_09	;no - keep testing
	LDA #7 		;yes - not used
	STA WaitStat
	RTS
TCl_09	CMP #$1C	;Ctrl-\? Ticker mode OFF
	BNE TCl_10	;no - keep testing
TCl_09a	LDA #0
	STA Tikr_Mod	;clear ticker mode
	STA Tikr_Flg	;and clear ticker flag
	RTS
TCl_10	CMP #$1D	;Ctrl-]? Ticker mode ON
	BNE TCl_11	;no - keep testing
TCl_10a	LDA #$FF	;set ticker mode
	STA Tikr_Mod	;ticker flag is set later
	RTS
TCl_11	CMP #$10	;Ctrl-P? toggle ticker
	BNE TCl_12	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_11a
	RTS
TCl_11a	LDA Tikr_Mod
	BNE TCl_09a
	JMP TCl_10a
TCl_12	CMP #$09	;Ctrl-I? inverse (tab key)
	BNE TCl_13	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_12a
	RTS
TCl_12a	LDA InvTx_Flg	;toggle flag byte
	EOR #$FF
	STA InvTx_Flg
	RTS
TCl_13	CMP #$0C	;Ctrl-L underline mode
	BNE TCl_14	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_13a
	RTS
TCl_13a	LDA UndTx_Flg	;toggle flag byte
	EOR #$FF
	STA UndTx_Flg
	RTS
TCl_14	CMP #$15	;Ctrl-U right arrow
	BNE TCl_15	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_14a
	LDA #1
	STA ChrWdth
	RTS
TCl_14a	LDA #0 		;since moving right only one dot
	STA ChrWdth	;char width param is set to 0
	LDA Tikr_Flg
	BNE TCl_14t	;if not using ticker scrolling
	JMP AdvCurs	;advance the cursor, else,
TCl_14t	DEC NoBf_Flg	;set do-not-use-buffer-data flag
; JSR SetGrClr	;GrColor(byte) <- BkgColor(nibble)
; JMP iT_Begn	;and initialize ticker params
	RTS
TCl_15	CMP #$08	;Ctrl-H left arrow
	BNE TCl_16	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_15a
	RTS
TCl_15a	LDA Tikr_Flg
	BNE TCl_15t	;if not using ticker
	LDA CursColL	;then move cursor left one dot
	BNE +
	DEC CursColH
+	DEC CursColL
	SEC
	LDA CursXl
	SBC CursColL
	LDA CursXh
	SBC CursColH
	BCC TCl_15t
	LDA CursXl
	STA CursColL
	LDA CursXh
	STA CursColH
TCl_15t	RTS
TCl_16	CMP #$0A	;Ctrl-J down arrow
	BNE TCl_17	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_16a
	RTS
TCl_16a	CLC
	LDA CursRow	;move cursor down 1 pixel
	ADC #1
	CMP CursYb	;check for bottom of window
	BCC TCl_16t
	LDA CursYb
TCl_16t	STA CursRow
	RTS
TCl_17	CMP #$0B	;Ctrl-K up arrow
	BNE TCl_19	;no - keep testing
	LDA NoPlt_Flg
	BEQ TCl_17a
	RTS
TCl_17a	LDA CursRow
	SEC 		;move cursor up 1 pixel
	SBC #1
	CMP TpMrgn	;check for top of window
	BCS TCl_17t
	LDA TpMrgn
TCl_17t	STA CursRow
	RTS
TCl_19	CMP #$19	;Ctrl-Y center justifY
	BNE TCl_20
	LDA NoPlt_Flg
	BEQ TCl_19a
	RTS
TCl_19a	LDA CtrJs_Flg	;get current state
	BNE TCl_19t	;if set, then clear it
	LDA #$FF
	STA Tikr_Mod	;set ticker mode flag
	STA CtrJs_Flg	;set center justify flag
TCl_19c	LDA CursXml
	STA CursColL
	LDA CursXmh
	STA CursColH
	RTS
TCl_19t	LDA #0
	STA CtrJs_Flg	;set center justify flag
	STA Tikr_Mod	;set ticker mode flag
	RTS
TCl_20	CMP #$0E	;Ctrl-N normal txt mode
	BNE TCl_XX
	LDA #0
	STA InvTx_Flg
	STA MskTx_Flg
	STA UndTx_Flg
	STA CtrJs_Flg
	STA CharRate
	STA BkgColor
TCl_XX	RTS

;Act on parameters that follow control key.
;Foreground, background, special character and change font
;parameters are only one alpha character long. Htab, Vtab
;and char/ticker rate parameters are 2 alpha chars long.
;Color is Hex char {0..F}
;Font is {0..2}
;Change font is {A..O} corresponding to BMP {96..110}
;Vtab & Htab is {00..39} decimal, leading zero required
;Char/Ticker rate is {00..FF}, leading zero required
Flg_FBc	!byte $00 	;Foreground background color
Flg_Prm2 !byte $00	;Parameter digit 1st/2nd
Wp_Dig1	!byte $00 	;1st digit of parameter
Wp_Dig2	!byte $00 	;2nd digit of parameter
Wp_Dig3	!byte $00 	;3rd digit of parameter
GetParm	LDA WaitStat	;Get the Wait State
	CMP #1 		;1=Foreground color
	BNE WPr_01	;no - keep looking
	LDA #0 		;yes - 
	STA Flg_FBc	;clear Fore/Bkgnd clr flag
	JMP Wp_StClr	;set color
WPr_01	CMP #2 		;2=Background color
	BNE Wpr_02
	LDA #1 		;yes - 
	STA Flg_FBc	;set Fore/Bkgnd clr flag
	JMP Wp_StClr
Wpr_02	CMP #3 		;3=Special Character
	BNE Wpr_03
	JMP Wp_eChar 
Wpr_03	CMP #4 		;4=Change Font
	BNE Wpr_04
	JMP Wp_CFnt 
Wpr_04	CMP #5 		;5=HTAB
	BNE Wpr_05
	JMP Wp_Tab 
Wpr_05	CMP #6 		;6=VTAB
	BNE Wpr_06
	JMP Wp_Tab 
Wpr_06	CMP #7 		;7=Change Char/Ticker Rate
	BNE Wpr_Clr
	JMP Wp_cRate
	RTS
Wpr_Clr	LDX #0 		;clear the wait parameter flags
	STX Flg_Prm2
	STX Wp_Dig1
	STX WaitStat 
	RTS 		;restore alpha char

;Chage Color
Wp_StClr TXA 		;restore the alpha char
	SEC
	SBC #$30	;change Chr"#" to Val#
	AND #$1F	;mask off most letters/chars
	CMP #$10	;check of 'dirty' Val#
	AND #$0F	;strip off low nibble
	BCC WpClrOk 	;shift to #
Wp_Ashft ;;CLC 		;which is 'A..F'
	ADC #8 		;shift to numeric =ivalent
	AND #$07	;mask it to be safe
WpClrOk	TAX
	LDA HclrTbl,X
	LDX Flg_FBc	;get Fore/Bkgnd clr flag
	BEQ Wp_SvFC	;0 = foreground
	STA BkgColor	;1 = background
	JMP Wpr_Clr
Wp_SvFC	STA FrgColor
	JMP Wpr_Clr
HclrTbl	!byte $00 	;0-black (hi bit clear)
	!byte $2A 	;1-green
	!byte $55 	;2-magenta
	!byte $7F 	;3-white
	!byte $80 	;4-black (hi bit set)
	!byte $AA 	;5-orange
	!byte $D5 	;6-blue
	!byte $FF 	;7-white

;Write Extended Character
Wp_eChar TXA 		;restore alpha char
	SEC
	SBC #64 	;adjust and
	AND #$1F	;clamp the value
	CMP #$12	;and check it
	BMI Wp_eChr1
	AND #$0F
Wp_eChr1 CLC
	ADC #95 	;calculate offset
	STA PltChar	;store the char to be drawn
	JSR Wpr_Clr
	JMP PlotFnt	;plot the character

;Change Font
Wp_CFnt	TXA 		;restore alpha char
	SEC
	SBC #$30	;change Chr"#" to Val#
	AND #$03	;mask off digit
;;pf: this CMP is broken
	CMP #4
	BEQ Wp_CfDn
; STA Slct_Fnt	;store the font selection
; JSR SetFTBA	;update table parameters
Wp_CfDn	RTS 		;JMP Wpr_Clr

;** Do Tabs ** revised 6/30/2015 by AJH
;hTab - sets plot cursor column {0..279} 2-byte
;vTab - sets plot cursor row {0..191} 1-byte
;Format of the command is: <tab char>###
;However the code is written to accommodate one,
;two or three numerals [following the tab char]
;then followed by a non-numeral.
;The tab works relative to the window parameters
;used by the "AdvCurs" routine.
;The ### digits are treated as base-10.
Flg_PsC	!byte 0		;flag: plot separator char
;
Wp_Tab	TXA		;restore alpha char
	SEC
	SBC #$30	;attempt to change Chr"#" to Val#
	BCC Wp_CkPrm2	;alpha char < '0', so ## delimited
	CMP #$0A	;is alpha char > '9'?
	BPL Wp_CkPrm2	;if so then ## delimited
	LDX Flg_Prm2
	BNE Wp_Tdg2	;is 1st of 3 digits?
	STA Wp_Dig1	;if so, save in Dig1
	INC Flg_Prm2	;inc index of parm digit #
	RTS
Wp_Tdg2	DEX
	BNE Wp_Tdg3	;is 2nd of 3 digits?
	STA Wp_Dig2	;if so, save in Dig2
	INC Flg_Prm2	;inc index of parm digit #
	RTS
Wp_Tdg3	STA Wp_Dig3	;save 3rd digit
	INC Flg_Prm2	;inc index of parm digit #
Wp_CkPrm2 LDX Flg_Prm2	;check index value
	BNE Wp_CmbNz	;non-zero number of digits
	JMP Wp_LdHtVt	;when no digits, load margin
;combine the parm digits - from none, up to 3 digits
Wp_CmbNz DEX
	BNE Wp_CmbN2	;is parm single digit?
	LDA Wp_Dig1
	STA T1_vLo	;if so, then use it as low byte
	JMP Wp_CkHtVt	;check hTab/vTab value
Wp_CmbN2 DEX
	BNE Wp_CmbN3	;is parm 2-digit?
	LDA Wp_Dig1
	JSR Wp_Tmx10	;multiply 1st digit by 10
	ADC Wp_Dig2	;save combined value
	STA T1_vLo	;in low byte
	JMP Wp_CkHtVt	;check hTab/vTab value
Wp_Tmx10 ASL 		;multiply by 10
	STA T1_Val	;one ASL is 2x
	ASL
	ASL 		;three ASL is 8x
	ADC T1_Val	;10x = 8x + 2x
	RTS
Wp_CmbN3 LDA Wp_Dig1	;combine 3 digits
	CMP #3		;clamp highest digit to 2
	BMI Wp_CmbN3c	;since no tab is > 2##
	LDA #2
Wp_CmbN3c JSR Wp_Tmx10	;multiply clamped digit x10
	JSR Wp_Tmx10	;multiply x10 again
	STA T1_vLo	;save x100 in low byte
	LDA Wp_Dig2	;get 2nd digit
	JSR Wp_Tmx10	;multiply x10
	ADC T1_vLo	;combine x100 + x10
	STA T1_vLo	;save it
	LDA #0
	ADC #0		;update hi-bit for
	STA T1_vHi	;values > 25#
	LDA Wp_Dig3	;get 3rd digit
	ADC T1_vLo	;combine x100+x10+x1
	STA T1_vLo	;save it
	LDA #0
	JMP Wp_CfHtVt	;chk Ht/Vt val & clr flg
;when no digits, load margin by setting tab
Wp_LdHtVt LDA #0	;to zero
	STA T1_vLo	;clear lo-byte
;check Htab / Vtab value & assign the parameter
Wp_CkHtVt LDA #0	;clear hi-byte when
	STA T1_vHi	;parm is 1 or 2-digit
	LDA #1
Wp_CfHtVt STA Flg_PsC	;set Plot Separator flag
	LDA WaitStat
	CMP #5		;is param for hTab?
	BNE Wp_VtVal	;no - then go do vTab
	LDA T1_vLo	;yes - then hTab
;
	CLC 		;hTAB: get param add it to 
	ADC CursXl	;left window margin {0..278}
	STA CursColL	;move plot cursor from the
	LDA T1_vHi	;left margin to the tab value
	ADC CursXh
	STA CursColH	
	SEC 		;Check to make sure the tab
	LDA CursXrl	;didn't put the plot cursor
	SBC #1		;beyond the right margin.
	SBC CursColL	;By subtracting 1 pixels
	LDA CursXrh	;it puts us on the
	SBC CursColH	;right margin.
	BCS Wp_TbOk
	LDA CursXrl	;If too far, fix hTab
	STA CursColL
	LDA CursXrh
	STA CursColH
	JMP Wp_TbOk
;
Wp_VtVal LDA T1_vLo	;vTAB: get param & add it to 
	CLC 		;botm window margin {0..190}
	ADC CursY	;Move plot cursor from the
	STA CursRow	;top margin to the tab value
	SEC 		;Check to make sure the tab
	LDA CursYb	;didn't put the plot cursor
	SBC #1		;beyond the bottommargin.
	SBC CursRow
	BCS Wp_TbOk
	LDA CursYb	;If too far, fix vTab
	STA CursRow
;
Wp_TbOk	LDA Flg_PsC	;if param was 3-digits
	BEQ Wp_Tdn	;then done
Wp_ClrPfl LDA #0	;else, for 1 or 2-digits,
	STA Flg_PsC	;clear the Plot Separator flag
	JSR Wpr_Clr	;Clear the wait state flags
	JMP TestChr	;and plot the non-numeric char
Wp_Tdn	JMP Wpr_Clr	;Clear wait state flags & end

;Chage char/ticker rate
;these digits are treated as base-16
Wp_cRate TXA 		;restore alpha char
	SEC
	SBC #$30	;change Chr"#" to Val#
	AND #$1F	;mask off digit
	CMP #10 	;digit >9
	BMI Wp_RvOk	;no - ok
	SBC #7 		;make A..F be 11..15
Wp_RvOk	TAX
	LDA Flg_Prm2	;is 2nd of 2 digits?
	BNE Wp_rCmb	;yes - combine
	TXA  		;no - clamp to {0..F}
	AND #$0F
	STA Wp_Dig1	;and save it
	INC Flg_Prm2	;set 2nd digit flag
	RTS
Wp_rCmb	STX Wp_Dig2	;save digit
	LDA Wp_Dig1	;get 1st digit
	ASL 		;shift it
	ASL
	ASL
	ASL
	ORA Wp_Dig2	;combine the digits
	STA CharRate	;store the rate parameter
	JMP Wpr_Clr

HgrTbHi !byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F
	!byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F
	!byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $20,$24,$28,$2C,$30,$34,$38,$3C
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $21,$25,$29,$2D,$31,$35,$39,$3D
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $22,$26,$2A,$2E,$32,$36,$3A,$3E
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F
	!byte $23,$27,$2B,$2F,$33,$37,$3B,$3F

HgrTbLo !byte $00,$00,$00,$00,$00,$00,$00,$00
	!byte $80,$80,$80,$80,$80,$80,$80,$80
	!byte $00,$00,$00,$00,$00,$00,$00,$00
	!byte $80,$80,$80,$80,$80,$80,$80,$80
	!byte $00,$00,$00,$00,$00,$00,$00,$00
	!byte $80,$80,$80,$80,$80,$80,$80,$80
	!byte $00,$00,$00,$00,$00,$00,$00,$00
	!byte $80,$80,$80,$80,$80,$80,$80,$80
	!byte $28,$28,$28,$28,$28,$28,$28,$28
	!byte $A8,$A8,$A8,$A8,$A8,$A8,$A8,$A8
	!byte $28,$28,$28,$28,$28,$28,$28,$28
	!byte $A8,$A8,$A8,$A8,$A8,$A8,$A8,$A8
	!byte $28,$28,$28,$28,$28,$28,$28,$28
	!byte $A8,$A8,$A8,$A8,$A8,$A8,$A8,$A8
	!byte $28,$28,$28,$28,$28,$28,$28,$28
	!byte $A8,$A8,$A8,$A8,$A8,$A8,$A8,$A8
	!byte $50,$50,$50,$50,$50,$50,$50,$50
	!byte $D0,$D0,$D0,$D0,$D0,$D0,$D0,$D0
	!byte $50,$50,$50,$50,$50,$50,$50,$50
	!byte $D0,$D0,$D0,$D0,$D0,$D0,$D0,$D0
	!byte $50,$50,$50,$50,$50,$50,$50,$50
	!byte $D0,$D0,$D0,$D0,$D0,$D0,$D0,$D0
	!byte $50,$50,$50,$50,$50,$50,$50,$50
	!byte $D0,$D0,$D0,$D0,$D0,$D0,$D0,$D0
