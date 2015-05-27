fontsTable_LO
	.for l=0, l<=60, l=l+1
		.byte <FONTS+l*8-1
	.next

fontsTable_HI
	.for l=0, l<=60, l=l+1
		.byte >FONTS+l*8-1
	.next

textPortTable_LO
	.for step=0, step<=13, step=step+1
		.for l=0, l<=15, l=l+1
			.byte <TEXTPORT_BITMAP_OFFSET+SCREEN-1+step*320+l*8
		.next
	.next

textPortTable_HI
	.for step=0, step<=13, step=step+1
		.for l=0, l<=15, l=l+1
			.byte >TEXTPORT_BITMAP_OFFSET+SCREEN-1+step*320+l*8
		.next
	.next

textPortColorTable_LO
	.for step=0, step<=13, step=step+1
		.for l=0, l<=15, l=l+1
			.byte <TEXTPORT_SCREEN_OFFSET+SCREEN_COLOR_RAM+step*40+l
		.next
	.next

textPortColorTable_HI
	.for step=0, step<=13, step=step+1
		.for l=0, l<=15, l=l+1
			.byte >TEXTPORT_SCREEN_OFFSET+SCREEN_COLOR_RAM+step*40+l
		.next
	.next
