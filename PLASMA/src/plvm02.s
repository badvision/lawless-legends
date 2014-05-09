;**********************************************************
;*
;* SYSTEM ROUTINES AND LOCATIONS
;*
;**********************************************************
;*
;* MONITOR SPECIAL LOCATIONS AND PRODOS MLI
;*
CSWL	=	$36
CSWH	=	$37
PROMPT 	=	$33
PRODOS	=	$BF00
MACHID	=	$BF98
;*
;* HARDWARE ADDRESSES
;*
KEYBD	=	$C000
CLRKBD	=	$C010
SPKR	=	$C030
LCRDEN 	=	$C080
LCWTEN	=	$C081
ROMEN	=	$C082
LCRWEN 	=	$C083
LCBNK2	=	$00
LCBNK1	=	$08
ALTZPOFF=	$C008
ALTZPON	=	$C009
ALTRDOFF=	$C002
ALTRDON	=	$C003
ALTWROFF=	$C004
ALTWRON	=	$C005
	!SOURCE	"plvm02zp.inc"
ESP	=	DST+2
;**********************************************************
;*
;* INTERPRETER INSTRUCTION POINTER INCREMENT MACRO
;*
;**********************************************************
	!MACRO	INC_IP	{
	INY
	BNE	* + 4
	INC	IPH
	}
;***********************************************
;*
;* INTERPRETER INITIALIZATION
;*
;***********************************************
*	=	$2000
	LDX     #$FF
	TXS
;*
;* INSTALL PAGE 3 VECTORS
;*
	LDY	#$20
- 	LDA	PAGE3,Y
	STA	$03D0,Y
	DEY
	BPL	-
;*
;* MOVE VM INTO LANGUAGE CARD
;*
	BIT	LCRWEN+LCBNK2
	BIT	LCRWEN+LCBNK2
	LDA	#<VMCORE
	STA	SRCL
	LDA	#>VMCORE
	STA	SRCH
	LDA	#$00
	STA	DSTL
	LDA	#$D0
	STA	DSTH
	LDY	#$00
-	LDA	(SRC),Y         ; COPY VM+CMD INTO LANGUAGE CARD
	STA	(DST),Y
	INY
	BNE	-
	INC	SRCH
	INC	DSTH
	LDA	DSTH
	CMP	#$E0
	BNE	-
;*
;* MOVE FIRST PAGE OF 'BYE' INTO PLACE
;*
	LDY     #$00
	STY     SRCL
	LDA	#$D1
	STA	SRCH
-	LDA	(SRC),Y
	STA	$1000,Y
	INY
	BNE	-
;*
;* LOOK FOR STARTUP FILE
;*
	JSR     PRODOS          ; OPEN AUTORUN
	!BYTE	$C8
	!WORD	OPENPARMS
	BCC     +
	JMP     NOAUTO
+	LDA     REFNUM
	STA     NLPARMS+1
	JSR     PRODOS
	!BYTE	$C9
	!WORD	NLPARMS
	BCC     +
	JMP    	NOAUTO
+	LDA     REFNUM
	STA     READPARMS+1
	JSR     PRODOS
	!BYTE	$CA
	!WORD	READPARMS
	BCC     +
	JMP     NOAUTO
+	LDX     READPARMS+6
	STX     $0280
	JSR     PRODOS
	!BYTE	$CC
	!WORD	CLOSEPARMS
	JMP     CMDEXEC		; CALL CM
NOAUTO	JMP	BYE
AUTORUN !BYTE	7
	!TEXT	"AUTORUN"
OPENPARMS !BYTE	3
	!WORD	AUTORUN
	!WORD	$0800
REFNUM	!BYTE	0
NLPARMS	!BYTE	3
	!BYTE	0
	!BYTE	$7F
	!BYTE	$0D
READPARMS !BYTE	4
	!BYTE	0
	!WORD	$0281
	!WORD	$0080
	!WORD	0
CLOSEPARMS !BYTE 1
	!BYTE	0
PAGE3	=	*
	!PSEUDOPC	$03D0 {
;*
;* PAGE 3 VECTORS INTO INTERPRETER
;*
	BIT	LCRDEN+LCBNK2	; $03D0 - DIRECT INTERP ENTRY
	JMP	INTERP
	BIT	LCRDEN+LCBNK2	; $03D6 - INDIRECT INTERP ENTRY
	JMP	IINTRP
	BIT	LCRDEN+LCBNK2	; $03DC - INDIRECT INTERPX ENTRY
	JMP	IINTRPX
}
VMCORE	=	*
	!PSEUDOPC	$D000 {
;*
;* OPCODE TABLE
;*
OPTBL 	!WORD	ZERO,ADD,SUB,MUL,DIV,MOD,INCR,DECR		; 00 02 04 06 08 0A 0C 0E
	!WORD	NEG,COMP,BAND,IOR,XOR,SHL,SHR,IDXW		; 10 12 14 16 18 1A 1C 1E
	!WORD	LNOT,LOR,LAND,LA,LLA,CB,CW,SWAP			; 20 22 24 26 28 2A 2C 2E
	!WORD	DROP,DUP,PUSH,PULL,BRGT,BRLT,BREQ,BRNE		; 30 32 34 36 38 3A 3C 3E
	!WORD	ISEQ,ISNE,ISGT,ISLT,ISGE,ISLE,BRFLS,BRTRU	; 40 42 44 46 48 4A 4C 4E
	!WORD	BRNCH,IBRNCH,CALL,ICAL,ENTER,LEAVE,RET,NEXTOP 	; 50 52 54 56 58 5A 5C 5E
	!WORD	LB,LW,LLB,LLW,LAB,LAW,DLB,DLW			; 60 62 64 66 68 6A 6C 6E
	!WORD	SB,SW,SLB,SLW,SAB,SAW,DAB,DAW			; 70 72 74 76 78 7A 7C 7E
;*
;* OPXCODE TABLE
;*
OPXTBL  !WORD	ZEROX,ADDX,SUBX,MULX,DIVX,MODX,INCRX,DECRX     	; 00 02 04 06 08 0A 0C 0E
	!WORD	NEGX,COMPX,BANDX,IORX,XORX,SHLX,SHRX,IDXWX     	; 10 12 14 16 18 1A 1C 1E
	!WORD	LNOTX,LORX,LANDX,LAX,LLAX,CBX,CWX,SWAPX		; 20 22 24 26 28 2A 2C 2E
	!WORD	DROPX,DUPX,PUSHX,PULLX,BRGTX,BRLTX,BREQX,BRNEX	; 30 32 34 36 38 3A 3C 3E
	!WORD	ISEQX,ISNEX,ISGTX,ISLTX,ISGEX,ISLEX,BRFLSX,BRTRUX; 40 42 44 46 48 4A 4C 4E
	!WORD	BRNCHX,IBRNCHX,CALLX,ICALX,ENTERX,LEAVEX,RETX,NEXTOPX; 50 52 54 56 58 5A 5C 5E
	!WORD	LBX,LWX,LLBX,LLWX,LABX,LAWX,DLBX,DLWX		; 60 62 64 66 68 6A 6C 6E
	!WORD	SBX,SWX,SLBX,SLWX,SABX,SAWX,DABX,DAWX		; 70 72 74 76 78 7A 7C 7E
;*
;* 'BYE' COMMAND PROCESSING
;*
	!PSEUDOPC	$1000 {
;*
;* CLEAR COMMAND LINE LENGTH BYTE IF CALLED FROM 'BYE'
;*
BYE	LDY	DEFCMD
        STY     $0280		; SET DEFAULT COMMAND WHEN CALLED FROM 'BYE'
-	LDA	DEFCMD,Y
	STA	$0280,Y
	DEY
	BNE	-
;*
;* MOVE REST OF CMD FROM LANGUAGE CARD
;*
CMDEXEC	STY	SRCL
	STY	DSTL
	LDA	#$D2
	STA	SRCH
	LDA	#$11
	STA	DSTH
	BIT	LCRDEN+LCBNK2
-	LDA	(SRC),Y
	STA	(DST),Y
	INY
	BNE	-
	INC	SRCL
	INC	DSTL
	LDA	SRCL
	CMP	#$E0
	BNE	-
;*
;* DEACTIVATE 80 COL CARDS
;*
	BIT	ROMEN
	LDY	#4
-	LDA	DISABLE80,Y
	JSR	$FDED
	DEY
	BPL	-
	BIT	$C054		; SET TEXT MODE
	BIT	$C051
	BIT	$C058
	JSR	$FC58		; HOME
	JMP	START
DISABLE80 !BYTE	21, 13, '1', 26, 13
DEFCMD	!BYTE	3
	!TEXT	"CMD"
;*
;* JUMP TO INTERPRETER
;*
START	LDA	#$00
	STA	IFPL
	LDA	#$BF
	STA	IFPH
	LDX	#$FF
	TXS
        LDX	#ESTKSZ/2
	!SOURCE "cmdexec.a"
}
;*
;* ENTER INTO BYTECODE INTERPRETER
;*
INTERP	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	PLA
        STA     IPL
        PLA
        STA     IPH
	LDY	#$01
	BNE	FETCHOP
IINTRP	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	PLA
        STA     TMPL
        PLA
        STA     TMPH
	LDY	#$02
	LDA     (TMP),Y
        STA	IPH
	DEY
	LDA     (TMP),Y
	STA	IPL
        DEY
	LDY	#$00
	BEQ	FETCHOP
IINTRPX	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	PLA
        STA     TMPL
        PLA
        STA     TMPH
	LDY	#$02
	LDA     (TMP),Y
        STA	IPH
	DEY
	LDA     (TMP),Y
	STA	IPL
        DEY
	LDY	#$00
	BEQ	FETCHOPX
;*
;* INTERP BYTECODE IN MAIN MEM
;*
NEXTOPH	INC	IPH
	BNE	FETCHOP
DROP	INX
NEXTOP	INY
	BEQ	NEXTOPH
FETCHOP LDA	(IP),Y
	STA	*+4
	JMP	(OPTBL)
;*
;* INTERP BYTECODE IN AUX MEM
;*
NEXTOPHX INC	IPH
	BNE	FETCHOPX
DROPX	INX
NEXTOPX CLI
	INY
	BEQ	NEXTOPHX
FETCHOPX SEI
	STA	ALTRDON
	LDA	(IP),Y
	ORA	#$80		; SELECT OPX OPCODES
	STA	*+4
	JMP	(OPXTBL)
;*
;* INDIRECT JUMP TO (TMP)
;*
JMPTMP	JMP	(TMP)
;*
;* ADD TOS TO TOS-1
;*
ADD 	LDA	ESTKL,X
	CLC
	ADC	ESTKL+1,X
	STA	ESTKL+1,X
	LDA	ESTKH,X
	ADC	ESTKH+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
ADDX 	LDA	ESTKL,X
	CLC
	ADC	ESTKL+1,X
	STA	ESTKL+1,X
	LDA	ESTKH,X
	ADC	ESTKH+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* SUB TOS FROM TOS-1
;*
SUB 	LDA	ESTKL+1,X
	SEC
	SBC	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
SUBX 	LDA	ESTKL+1,X
	SEC
	SBC	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* SHIFT TOS-1 LEFT BY 1, ADD TO TOS-1
;*
IDXW 	LDA	ESTKL,X
	ASL
	ROL	ESTKH,X
	CLC
	ADC	ESTKL+1,X
	STA	ESTKL+1,X
	LDA	ESTKH,X
	ADC	ESTKH+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
IDXWX 	LDA	ESTKL,X
	ASL
	ROL	ESTKH,X
	CLC
	ADC	ESTKL+1,X
	STA	ESTKL+1,X
	LDA	ESTKH,X
	ADC	ESTKH+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* INTERNAL MULTIPLY ALGORITHM
;*
_MUL	STY	IPY
	LDY	#$00
	STY	TMPL		; PRODL
	STY	TMPH		; PRODH
	LDY	#$10
MUL1 	LSR	ESTKH,X		; MULTPLRH
	ROR	ESTKL,X		; MULTPLRL
	BCC	MUL2
	LDA	ESTKL+1,X	; MULTPLNDL
	CLC
	ADC	TMPL		; PRODL
	STA	TMPL
	LDA	ESTKH+1,X	; MULTPLNDH
	ADC	TMPH		; PRODH
	STA	TMPH
MUL2 	ASL	ESTKL+1,X	; MULTPLNDL
	ROL	ESTKH+1,X	; MULTPLNDH
	DEY
	BNE	MUL1
	INX
	LDA	TMPL		; PRODL
	STA	ESTKL,X
	LDA	TMPH		; PRODH
	STA	ESTKH,X
	LDY	IPY
	RTS
;*
;* MUL TOS-1 BY TOS
;*
MUL	JSR	_MUL
	JMP	NEXTOP
;
MULX	JSR	_MUL
	JMP	NEXTOPX	
;*
;* INTERNAL DIVIDE ALGORITHM
;*
_NEG 	LDA	#$00
	SEC
	SBC	ESTKL,X
	STA	ESTKL,X
	LDA	#$00
	SBC	ESTKH,X
	STA	ESTKH,X
	RTS
_DIV	STY	IPY
	LDA	ESTKH,X
	AND	#$80
	STA	DVSIGN
	BPL	_DIV1
	JSR	_NEG
	INC	DVSIGN
_DIV1 	LDA	ESTKH+1,X
	BPL	_DIV2
	INX
	JSR	_NEG
	DEX
	INC	DVSIGN
	BNE	_DIV3
_DIV2 	ORA	ESTKL+1,X	; DVDNDL
	BNE	_DIV3
	STA	TMPL
	STA	TMPH
	RTS
_DIV3 	LDY	#$11		; #BITS+1
	LDA	#$00
	STA	TMPL		; REMNDRL
	STA	TMPH		; REMNDRH
_DIV4 	ASL	ESTKL+1,X	; DVDNDL
	ROL	ESTKH+1,X	; DVDNDH
	DEY
	BCC	_DIV4
	STY	ESTKL-1,X
_DIV5 	ROL	TMPL		; REMNDRL
	ROL	TMPH		; REMNDRH
	LDA	TMPL		; REMNDRL
	SEC
	SBC	ESTKL,X		; DVSRL
	TAY
	LDA	TMPH		; REMNDRH
	SBC	ESTKH,X		; DVSRH
	BCC	_DIV6
	STA	TMPH		; REMNDRH
	STY	TMPL		; REMNDRL
_DIV6 	ROL	ESTKL+1,X	; DVDNDL
	ROL	ESTKH+1,X	; DVDNDH
	DEC	ESTKL-1,X
	BNE	_DIV5
	LDY	IPY
	RTS
;*
;* NEGATE TOS
;*
NEG 	LDA	#$00
	SEC
	SBC	ESTKL,X
	STA	ESTKL,X
	LDA	#$00
	SBC	ESTKH,X
	STA	ESTKH,X
	JMP	NEXTOP
;
NEGX 	LDA	#$00
	SEC
	SBC	ESTKL,X
	STA	ESTKL,X
	LDA	#$00
	SBC	ESTKH,X
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* DIV TOS-1 BY TOS
;*
DIV 	JSR	_DIV
	INX
	LSR	DVSIGN		; SIGN(RESULT) = (SIGN(DIVIDEND) + SIGN(DIVISOR)) & 1
	BCS	NEG
	JMP	NEXTOP
;
DIVX 	JSR	_DIV
	INX
	LSR	DVSIGN		; SIGN(RESULT) = (SIGN(DIVIDEND) + SIGN(DIVISOR)) & 1
	BCS	NEGX
	JMP	NEXTOPX
;*
;* MOD TOS-1 BY TOS
;*
MOD	JSR	_DIV
	INX
	LDA	TMPL		; REMNDRL
	STA	ESTKL,X
	LDA	TMPH		; REMNDRH
	STA	ESTKH,X
	LDA	DVSIGN		; REMAINDER IS SIGN OF DIVIDEND
	BMI	NEG
	JMP	NEXTOP
;
MODX	JSR	_DIV
	INX
	LDA	TMPL		; REMNDRL
	STA	ESTKL,X
	LDA	TMPH		; REMNDRH
	STA	ESTKH,X
	LDA	DVSIGN		; REMAINDER IS SIGN OF DIVIDEND
	BMI	NEGX
	JMP	NEXTOPX
;*
;* INCREMENT TOS
;*
INCR 	INC	ESTKL,X
	BNE	INCR1
	INC	ESTKH,X
INCR1 	JMP	NEXTOP
;
INCRX 	INC	ESTKL,X
	BNE	INCRX1
	INC	ESTKH,X
INCRX1 	JMP	NEXTOPX
;*
;* DECREMENT TOS
;*
DECR 	LDA	ESTKL,X
	BNE	DECR1
	DEC	ESTKH,X
DECR1 	DEC	ESTKL,X
	JMP	NEXTOP
;
DECRX 	LDA	ESTKL,X
	BNE	DECRX1
	DEC	ESTKH,X
DECRX1 	DEC	ESTKL,X
	JMP	NEXTOPX
;*
;* BITWISE COMPLIMENT TOS
;*
COMP 	LDA	#$FF
	EOR	ESTKL,X
	STA	ESTKL,X
	LDA	#$FF
	EOR	ESTKH,X
	STA	ESTKH,X
	JMP	NEXTOP
;
COMPX 	LDA	#$FF
	EOR	ESTKL,X
	STA	ESTKL,X
	LDA	#$FF
	EOR	ESTKH,X
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* BITWISE AND TOS TO TOS-1
;*
BAND 	LDA	ESTKL+1,X
	AND	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	AND	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
BANDX 	LDA	ESTKL+1,X
	AND	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	AND	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* INCLUSIVE OR TOS TO TOS-1
;*
IOR 	LDA	ESTKL+1,X
	ORA	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	ORA	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
IORX 	LDA	ESTKL+1,X
	ORA	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	ORA	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* EXLUSIVE OR TOS TO TOS-1
;*
XOR 	LDA	ESTKL+1,X
	EOR	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	EOR	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
XORX 	LDA	ESTKL+1,X
	EOR	ESTKL,X
	STA	ESTKL+1,X
	LDA	ESTKH+1,X
	EOR	ESTKH,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* SHIFT TOS-1 LEFT BY TOS
;*
SHL	STY	IPY
	LDA	ESTKL,X
	CMP	#$08
	BCC	SHL1
	LDY	ESTKL+1,X
	STY	ESTKH+1,X
	LDY	#$00
	STY	ESTKL+1,X
	SBC	#$08
SHL1 	TAY
	BEQ	SHL3
SHL2 	ASL	ESTKL+1,X
	ROL	ESTKH+1,X
	DEY
	BNE	SHL2
SHL3 	INX
	LDY	IPY
	JMP	NEXTOP
;
SHLX 	STY	IPY
	LDA	ESTKL,X
	CMP	#$08
	BCC	SHLX1
	LDY	ESTKL+1,X
	STY	ESTKH+1,X
	LDY	#$00
	STY	ESTKL+1,X
	SBC	#$08
SHLX1 	TAY
	BEQ	SHLX3
SHLX2 	ASL	ESTKL+1,X
	ROL	ESTKH+1,X
	DEY
	BNE	SHLX2
SHLX3 	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* SHIFT TOS-1 RIGHT BY TOS
;*
SHR	STY	IPY
	LDA	ESTKL,X
	CMP	#$08
	BCC	SHR2
	LDY	ESTKH+1,X
	STY	ESTKL+1,X
	CPY	#$80
	LDY	#$00
	BCC	SHR1
	DEY
SHR1 	STY	ESTKH+1,X
	SEC
	SBC	#$08
SHR2 	TAY
	BEQ	SHR4
	LDA	ESTKH+1,X
SHR3 	CMP	#$80
	ROR
	ROR	ESTKL+1,X
	DEY
	BNE	SHR3
	STA	ESTKH+1,X
SHR4 	INX
	LDY	IPY
	JMP	NEXTOP
;
SHRX 	STY	IPY
	LDA	ESTKL,X
	CMP	#$08
	BCC	SHRX2
	LDY	ESTKH+1,X
	STY	ESTKL+1,X
	CPY	#$80
	LDY	#$00
	BCC	SHRX1
	DEY
SHRX1 	STY	ESTKH+1,X
	SEC
	SBC	#$08
SHRX2 	TAY
	BEQ	SHRX4
	LDA	ESTKH+1,X
SHRX3 	CMP	#$80
	ROR
	ROR	ESTKL+1,X
	DEY
	BNE	SHRX3
	STA	ESTKH+1,X
SHRX4 	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* LOGICAL NOT
;*
LNOT	LDA	ESTKL,X
	ORA	ESTKH,X
	BEQ	LNOT1
	LDA	#$FF
LNOT1	EOR	#$FF
	STA	ESTKL,X
	STA	ESTKH,X
	JMP	NEXTOP
;
LNOTX 	LDA	ESTKL,X
	ORA	ESTKH,X
	BEQ	LNOTX1
	LDA	#$FF
LNOTX1	EOR	#$FF
	STA	ESTKL,X
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* LOGICAL AND
;*
LAND 	LDA	ESTKL,X
	ORA	ESTKH,X
	BEQ	LAND1
	LDA	ESTKL+1,X
	ORA	ESTKH+1,X
	BEQ	LAND1
	LDA	#$FF
LAND1 	STA	ESTKL+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
LANDX 	LDA	ESTKL,X
	ORA	ESTKH,X
	BEQ	LANDX1
	LDA	ESTKL+1,X
	ORA	ESTKH+1,X
	BEQ	LANDX1
	LDA	#$FF
LANDX1 	STA	ESTKL+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* LOGICAL OR
;*
LOR 	LDA	ESTKL,X
	ORA	ESTKH,X
	ORA	ESTKL+1,X
	ORA	ESTKH+1,X
	BEQ	LOR1
	LDA	#$FF
LOR1 	STA	ESTKL+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOP
;
LORX 	LDA	ESTKL,X
	ORA	ESTKH,X
	ORA	ESTKL+1,X
	ORA	ESTKH+1,X
	BEQ	LORX1
	LDA	#$FF
LORX1 	STA	ESTKL+1,X
	STA	ESTKH+1,X
	INX
	JMP	NEXTOPX
;*
;* SWAP TOS WITH TOS-1
;*
SWAP	STY	IPY
	LDA	ESTKL,X
	LDY	ESTKL+1,X
	STA	ESTKL+1,X
	STY	ESTKL,X
	LDA	ESTKH,X
	LDY	ESTKH+1,X
	STA	ESTKH+1,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
;
SWAPX	STY	IPY
	LDA	ESTKL,X
	LDY	ESTKL+1,X
	STA	ESTKL+1,X
	STY	ESTKL,X
	LDA	ESTKH,X
	LDY	ESTKH+1,X
	STA	ESTKH+1,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOPX
;*
;* DUPLICATE TOS
;*
DUP 	DEX
	LDA	ESTKL+1,X
	STA	ESTKL,X
	LDA	ESTKH+1,X
	STA	ESTKH,X
	JMP	NEXTOP
;
DUPX 	DEX
	LDA	ESTKL+1,X
	STA	ESTKL,X
	LDA	ESTKH+1,X
	STA	ESTKH,X
	JMP	NEXTOPX

;*
;* PUSH FROM EVAL STACK TO CALL STACK
;*
PUSH 	LDA	ESTKL,X
	PHA
	LDA	ESTKH,X
	PHA
	INX
	JMP	NEXTOP
;
PUSHX 	LDA	ESTKL,X
	PHA
	LDA	ESTKH,X
	PHA
	INX
	JMP	NEXTOPX
;*
;* PULL FROM CALL STACK TO EVAL STACK
;*
PULL 	DEX
	PLA
	STA	ESTKH,X
	PLA
	STA	ESTKL,X
	JMP	NEXTOP
;
PULLX 	DEX
	PLA
	STA	ESTKH,X
	PLA
	STA	ESTKL,X
	JMP	NEXTOPX
;*
;* CONSTANT
;*
ZERO 	DEX
	LDA	#$00
	STA	ESTKL,X
	STA	ESTKH,X
	JMP	NEXTOP
CB 	DEX
	+INC_IP
	LDA	(IP),Y
	STA	ESTKL,X
	LDA	#$00
	STA	ESTKH,X
	JMP	NEXTOP
;
ZEROX 	DEX
	LDA	#$00
	STA	ESTKL,X
	STA	ESTKH,X
	JMP	NEXTOPX
CBX 	DEX
	+INC_IP
	LDA	(IP),Y
	STA	ESTKL,X
	LDA	#$00
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* LOAD ADDRESS & LOAD CONSTANT WORD (SAME THING, WITH OR WITHOUT FIXUP)
;*
LA	=	*
CW	DEX
	+INC_IP
 	LDA	(IP),Y
	STA	ESTKL,X
	+INC_IP
 	LDA	(IP),Y
	STA	ESTKH,X
	JMP	NEXTOP
;
LAX	=	*
CWX	DEX
	+INC_IP
 	LDA	(IP),Y
	STA	ESTKL,X
	+INC_IP
 	LDA	(IP),Y
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* LOAD VALUE FROM ADDRESS TAG
;*
LB 	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	STA	ESTKL,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
LW 	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
       	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	STA	ESTKL,X
	INY
	LDA	(TMP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
;
LBX	STA	ALTRDOFF
	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	STA	ESTKL,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOPX
LWX	STA	ALTRDOFF
 	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
       	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	STA	ESTKL,X
	INY
	LDA	(TMP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOPX
;*
;* LOAD ADDRESS OF LOCAL FRAME OFFSET
;*
LLA 	+INC_IP
 	LDA	(IP),Y
	DEX
	CLC
	ADC	IFPL
	STA	ESTKL,X
	LDA	#$00
	ADC	IFPH
	STA	ESTKH,X
	JMP	NEXTOP
;
LLAX 	+INC_IP
 	LDA	(IP),Y
	DEX
	CLC
	ADC	IFPL
	STA	ESTKL,X
	LDA	#$00
	ADC	IFPH
	STA	ESTKH,X
	JMP	NEXTOPX
;*
;* LOAD VALUE FROM LOCAL FRAME OFFSET
;*
LLB 	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	DEX
	LDA	(IFP),Y
	STA	ESTKL,X
	LDA	#$00
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
LLW 	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	DEX
	LDA	(IFP),Y
	STA	ESTKL,X
	INY
	LDA	(IFP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
;
LLBX	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	DEX
	STA	ALTRDOFF
	LDA	(IFP),Y
	STA	ESTKL,X
	LDA	#$00
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
LLWX	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	DEX
	STA	ALTRDOFF
	LDA	(IFP),Y
	STA	ESTKL,X
	INY
	LDA	(IFP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
;*
;* LOAD VALUE FROM ABSOLUTE ADDRESS
;*
LAB 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	DEX
	STA	ESTKL,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
LAW 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	(TMP),Y
	DEX
	STA	ESTKL,X
	INY
	LDA	(TMP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOP
;
LABX 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	(TMP),Y
	DEX
	STA	ESTKL,X
	STY	ESTKH,X
	LDY	IPY
	JMP	NEXTOPX
LAWX 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	(TMP),Y
	DEX
	STA	ESTKL,X
	INY
	LDA	(TMP),Y
	STA	ESTKH,X
	LDY	IPY
	JMP	NEXTOPX
;*
;* STORE VALUE TO ADDRESS
;*
SB 	LDA	ESTKL+1,X
	STA	TMPL
	LDA	ESTKH+1,X
	STA	TMPH
	LDA	ESTKL,X
	STY	IPY
	LDY	#$00
	STA	(TMP),Y
	INX
	INX
	LDY	IPY
	JMP	NEXTOP
SW 	LDA	ESTKL+1,X
	STA	TMPL
	LDA	ESTKH+1,X
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	INX
	INX
	LDY	IPY
	JMP	NEXTOP
;
SBX	LDA	ESTKL+1,X
	STA	TMPL
	LDA	ESTKH+1,X
	STA	TMPH
	LDA	ESTKL,X
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	STA	(TMP),Y
	INX
	INX
	LDY	IPY
	JMP	NEXTOPX
SWX	LDA	ESTKL+1,X
	STA	TMPL
	LDA	ESTKH+1,X
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	INX
	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* STORE VALUE TO LOCAL FRAME OFFSET
;*
SLB 	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INX
	LDY	IPY
	JMP	NEXTOP
SLW 	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INY
	LDA	ESTKH,X
	STA	(IFP),Y
	INX
	LDY	IPY
	JMP	NEXTOP
;
SLBX 	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	STA	ALTRDOFF
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INX
	LDY	IPY
	JMP	NEXTOPX
SLWX	+INC_IP
 	LDA	(IP),Y
	STY	IPY
	STA	ALTRDOFF
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INY
	LDA	ESTKH,X
	STA	(IFP),Y
	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* STORE VALUE TO LOCAL FRAME OFFSET WITHOUT POPPING STACK
;*
DLB 	+INC_IP
	LDA	(IP),Y
	STY	IPY
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	LDY	IPY
	JMP	NEXTOP
DLW 	+INC_IP
	LDA	(IP),Y
	STY	IPY
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INY
	LDA	ESTKH,X
	STA	(IFP),Y
	LDY	IPY
	JMP	NEXTOP
;
DLBX 	+INC_IP
	LDA	(IP),Y
	STY	IPY
	STA	ALTRDOFF
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	LDY	IPY
	JMP	NEXTOPX
DLWX	+INC_IP
	LDA	(IP),Y
	STY	IPY
	STA	ALTRDOFF
	TAY
	LDA	ESTKL,X
	STA	(IFP),Y
	INY
	LDA	ESTKH,X
	STA	(IFP),Y
	LDY	IPY
	JMP	NEXTOPX
;*
;* STORE VALUE TO ABSOLUTE ADDRESS
;*
SAB 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	LDA	ESTKL,X
	STY	IPY
	LDY	#$00
	STA	(TMP),Y
	INX
	LDY	IPY
	JMP	NEXTOP
SAW 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	INX
	LDY	IPY
	JMP	NEXTOP
;
SABX 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	LDA	ESTKL,X
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	STA	(TMP),Y
	INX
	LDY	IPY
	JMP	NEXTOPX
SAWX	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* STORE VALUE TO ABSOLUTE ADDRESS WITHOUT POPPING STACK
;*
DAB 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	LDY	IPY
	JMP	NEXTOP
DAW 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	LDY	IPY
	JMP	NEXTOP
;
DABX 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	LDY	IPY
	JMP	NEXTOPX
DAWX	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	STY	IPY
	STA	ALTRDOFF
	LDY	#$00
	LDA	ESTKL,X
	STA	(TMP),Y
	INY
	LDA	ESTKH,X
	STA	(TMP),Y
	LDY	IPY
	JMP	NEXTOPX
;*
;* COMPARES
;*
ISEQ	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	BNE	ISEQ1
	LDA	ESTKH,X
	CMP	ESTKH+1,X
	BNE	ISEQ1
	DEY
ISEQ1 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISEQX	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	BNE	ISEQ1
	LDA	ESTKH,X
	CMP	ESTKH+1,X
	BNE	ISEQX1
	DEY
ISEQX1 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;
ISNE	STY	IPY
	LDY	#$FF
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	BNE	ISNE1
	LDA	ESTKH,X
	CMP	ESTKH+1,X
	BNE	ISNE1
	INY
ISNE1 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISNEX	STY	IPY
	LDY	#$FF
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	BNE	ISNE1
	LDA	ESTKH,X
	CMP	ESTKH+1,X
	BNE	ISNEX1
	INY
ISNEX1 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;
ISGE	STY	IPY
	LDY	#$00
	LDA	ESTKL+1,X
	CMP	ESTKL,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	BVC	ISGE1
	EOR	#$80
ISGE1 	BMI	ISGE2
	DEY
ISGE2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISGEX	STY	IPY
	LDY	#$00
	LDA	ESTKL+1,X
	CMP	ESTKL,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	BVC	ISGEX1
	EOR	#$80
ISGEX1 	BMI	ISGEX2
	DEY
ISGEX2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;
ISGT	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	LDA	ESTKH,X
	SBC	ESTKH+1,X
	BVC	ISGT1
	EOR	#$80
ISGT1 	BPL	ISGT2
	DEY
ISGT2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISGTX	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	LDA	ESTKH,X
	SBC	ESTKH+1,X
	BVC	ISGTX1
	EOR	#$80
ISGTX1 	BPL	ISGTX2
	DEY
ISGTX2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;
ISLE	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	LDA	ESTKH,X
	SBC	ESTKH+1,X
	BVC	ISLE1
	EOR	#$80
ISLE1 	BMI	ISLE2
	DEY
ISLE2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISLEX	STY	IPY
	LDY	#$00
	LDA	ESTKL,X
	CMP	ESTKL+1,X
	LDA	ESTKH,X
	SBC	ESTKH+1,X
	BVC	ISLEX1
	EOR	#$80
ISLEX1 	BMI	ISLEX2
	DEY
ISLEX2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;
ISLT	STY	IPY
	LDY	#$00
	LDA	ESTKL+1,X
	CMP	ESTKL,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	BVC	ISLT1
	EOR	#$80
ISLT1 	BPL	ISLT2
	DEY
ISLT2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOP
;
ISLTX	STY	IPY
	LDY	#$00
	LDA	ESTKL+1,X
	CMP	ESTKL,X
	LDA	ESTKH+1,X
	SBC	ESTKH,X
	BVC	ISLTX1
	EOR	#$80
ISLTX1 	BPL	ISLTX2
	DEY
ISLTX2 	STY	ESTKL+1,X
	STY	ESTKH+1,X
	INX
	LDY	IPY
	JMP	NEXTOPX
;*
;* BRANCHES
;*
BRTRU 	INX
	LDA	ESTKH-1,X
	ORA	ESTKL-1,X
	BNE	BRNCH
NOBRNCH	+INC_IP
	+INC_IP
	JMP	NEXTOP
BRFLS 	INX
	LDA	ESTKH-1,X
	ORA	ESTKL-1,X
	BNE	NOBRNCH
BRNCH	LDA	IPH
	STA	TMPH
	LDA	IPL
	+INC_IP
	CLC
	ADC	(IP),Y
	STA	TMPL
	LDA	TMPH
	+INC_IP
	ADC	(IP),Y
	STA	IPH
	LDA	TMPL
	STA	IPL
	DEY
	DEY
	JMP	NEXTOP
BREQ 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BNE	NOBRNCH
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BEQ	BRNCH
	BNE	NOBRNCH
BRNE 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BNE	BRNCH
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BEQ	NOBRNCH
	BNE	BRNCH
BRGT 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	LDA	ESTKH-1,X
	SBC	ESTKH,X
	BMI	BRNCH
	BPL	NOBRNCH
BRLT 	INX
	LDA	ESTKL,X
	CMP	ESTKL-1,X
	LDA	ESTKH,X
	SBC	ESTKH-1,X
	BMI	BRNCH
	BPL	NOBRNCH
IBRNCH	LDA	IPL
	CLC
	ADC	ESTKL,X
	STA	IPL
	LDA	IPH
	ADC	ESTKH,X
	STA	IPH
	INX
	JMP	NEXTOP
;
BRTRUX	INX
	LDA	ESTKH-1,X
	ORA	ESTKL-1,X
	BNE	BRNCHX
NOBRNCHX +INC_IP
	+INC_IP
	JMP	NEXTOPX
BRFLSX 	INX
	LDA	ESTKH-1,X
	ORA	ESTKL-1,X
	BNE	NOBRNCHX
BRNCHX	LDA	IPH
	STA	TMPH
	LDA	IPL
	+INC_IP
	CLC
	ADC	(IP),Y
	STA	TMPL
	LDA	TMPH
	+INC_IP
	ADC	(IP),Y
	STA	IPH
	LDA	TMPL
	STA	IPL
	DEY
	DEY
	JMP	NEXTOPX
BREQX 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BNE	NOBRNCHX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BEQ	BRNCHX
	BNE	NOBRNCHX
BRNEX 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BNE	BRNCHX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	BEQ	NOBRNCHX
	BNE	BRNCHX
BRGTX 	INX
	LDA	ESTKL-1,X
	CMP	ESTKL,X
	LDA	ESTKH-1,X
	SBC	ESTKH,X
	BMI	BRNCHX
	BPL	NOBRNCHX
BRLTX 	INX
	LDA	ESTKL,X
	CMP	ESTKL-1,X
	LDA	ESTKH,X
	SBC	ESTKH-1,X
	BMI	BRNCHX
	BPL	NOBRNCHX
IBRNCHX	LDA	IPL
	CLC
	ADC	ESTKL,X
	STA	IPL
	LDA	IPH
	ADC	ESTKH,X
	STA	IPH
	INX
	JMP	NEXTOPX
;*
;* CALL INTO ABSOLUTE ADDRESS (NATIVE CODE)
;*
CALL 	+INC_IP
	LDA	(IP),Y
	STA	TMPL
	+INC_IP
	LDA	(IP),Y
	STA	TMPH
	LDA	IPH
	PHA
	LDA	IPL
	PHA
	TYA
	PHA
	JSR	JMPTMP
	PLA
	TAY
	PLA
	STA	IPL
	PLA
	STA	IPH
	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	JMP	NEXTOP
;
CALLX 	+INC_IP
	LDA	(IP),Y
	PHA
	+INC_IP
	LDA	(IP),Y
	STA	ALTRDOFF
	STA	TMPH
	PLA
	STA	TMPL
	LDA	IPH
	PHA
	LDA	IPL
	PHA
	TYA
	PHA
	CLI
	JSR	JMPTMP
	PLA
	TAY
	PLA
	STA	IPL
	PLA
	STA	IPH
	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	JMP	NEXTOPX
;*
;* INDIRECT CALL TO ADDRESS (NATIVE CODE)
;*
ICAL 	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
	INX
	LDA	IPH
	PHA
	LDA	IPL
	PHA
	TYA
	PHA
	JSR	JMPTMP
	PLA
	TAY
	PLA
	STA	IPL
	PLA
	STA	IPH
	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	JMP	NEXTOP
;
ICALX 	LDA	ESTKL,X
	STA	TMPL
	LDA	ESTKH,X
	STA	TMPH
	INX
	LDA	IPH
	PHA
	LDA	IPL
	PHA
	TYA
	PHA
	STA	ALTRDOFF
	CLI
	JSR	JMPTMP
	PLA
	TAY
	PLA
	STA	IPL
	PLA
	STA	IPH
	STA	LCRWEN+LCBNK2	; WRITE ENABLE LANGUAGE CARD
	STA	LCRWEN+LCBNK2
	JMP	NEXTOPX
;*
;* ENTER FUNCTION WITH FRAME SIZE AND PARAM COUNT
;*
ENTER 	+INC_IP
	LDA	(IP),Y
	STA	FRMSZ
	+INC_IP
	LDA	(IP),Y
	STA	NPARMS
	STY	IPY
        LDA	IFPL
	PHA
	SEC
	SBC	FRMSZ
	STA	IFPL
	LDA	IFPH
	PHA
	SBC	#$00
	STA	IFPH
	LDY	#$01
	PLA
	STA	(IFP),Y
	DEY
	PLA
	STA	(IFP),Y
	LDA	NPARMS
	BEQ	ENTER5
	ASL
	TAY
	INY
ENTER4  LDA	ESTKH,X
	STA	(IFP),Y
	DEY
	LDA	ESTKL,X
	STA	(IFP),Y
	DEY
	INX
	DEC	TMPL
	BNE	ENTER4
ENTER5  LDY	IPY
	JMP	NEXTOP
;
ENTERX	+INC_IP
	LDA	(IP),Y
	STA	FRMSZ
	+INC_IP
	LDA	(IP),Y
	STA	NPARMS
	STY	IPY
	STA	ALTRDOFF
        LDA	IFPL
	PHA
	SEC
	SBC	FRMSZ
	STA	IFPL
	LDA	IFPH
	PHA
	SBC	#$00
	STA	IFPH
	LDY	#$01
	PLA
	STA	(IFP),Y
	DEY
	PLA
	STA	(IFP),Y
	LDA	NPARMS
	BEQ	ENTERX5
	ASL
	TAY
	INY
ENTERX4	LDA	ESTKH,X
	STA	(IFP),Y
	DEY
	LDA	ESTKL,X
	STA	(IFP),Y
	DEY
	INX
	DEC	TMPL
	BNE	ENTERX4
ENTERX5	LDY	IPY
	JMP	NEXTOPX
;*
;* LEAVE FUNCTION
;*
LEAVE 	LDY	#$01
	LDA	(IFP),Y
	DEY
	PHA
	LDA	(IFP),Y
	STA	IFPL
	PLA
	STA	IFPH
RET 	RTS
;
LEAVEX 	STA	ALTRDOFF
	LDY	#$01
	LDA	(IFP),Y
	DEY
	PHA
	LDA	(IFP),Y
	STA	IFPL
	PLA
	STA	IFPH
	CLI
	RTS
RETX	STA	ALTRDOFF
	CLI
	RTS
VMEND	=	*
}