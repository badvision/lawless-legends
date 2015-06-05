; Debug support routines (defined in core/mem.s)
_writeStr	= $80C
_prByte		= _writeStr+3
_prSpace	= _prByte+3
_prWord		= _prSpace+3
_prA		= _prWord+3
_prX		= _prA+3
_prY		= _prX+3
_crout		= _prY+3
_waitKey	= _crout+3

; Debug macros
!macro prStr {
	jsr _writeStr
}

!macro prByte addr {
	jsr _prByte
	!word addr
}

!macro prSpace {
	jsr _prSpace
}

!macro prChr chr {
	jsr _writeStr
	!byte chr, 0
}

!macro prA {
	jsr _prA
	jsr _prSpace
}

!macro prX {
	jsr _prX
	jsr _prSpace
}

!macro prY {
	jsr _prY
	jsr _prSpace
}

!macro prXA {
	jsr _prX
	jsr _prA
	jsr _prSpace
}

!macro prAX {
	jsr _prA
	jsr _prX
	jsr _prSpace
}

!macro prYA {
	jsr _prY
	jsr _prA
	jsr _prSpace
}

!macro prAY {
	jsr _prA
	jsr _prY
	jsr _prSpace
}

!macro prXY {
	jsr _prX
	jsr _prY
	jsr _prSpace
}

!macro prYX {
	jsr _prY
	jsr _prX
	jsr _prSpace
}

!macro prWord addr {
	jsr _prWord
	!word addr
}

!macro crout {
	jsr _crout
}

!macro waitKey {
	jsr _waitKey
}

