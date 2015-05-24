
tilesBitmapTable_LO
	.for tileN=0, tileN<=63, tileN=tileN+1
		.byte <TILES+tileN*32-1
	.next

tilesBitmapTable_HI
	.for tileN=0, tileN<=63, tileN=tileN+1
		.byte >TILES+tileN*32-1
	.next

mapBitmapTable_LO
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte <VIEWPORT_BITMAP_OFFSET+SCREEN+tileY*320+tileX*16-1-640
		.next
	.next

mapBitmapTable_HI
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte >VIEWPORT_BITMAP_OFFSET+SCREEN+tileY*320+tileX*16-1-640
		.next
	.next

mapBitmapTable2_LO
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte <VIEWPORT_BITMAP_OFFSET+SCREEN+tileY*320+tileX*16-1-320-16
		.next
	.next

mapBitmapTable2_HI
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte >VIEWPORT_BITMAP_OFFSET+SCREEN+tileY*320+tileX*16-1-320-16
		.next
	.next

screenVideoRamTable_LO
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte <VIEWPORT_SCREEN_OFFSET+SCREEN_VIDEO_RAM+tileY*40+tileX*2-80
		.next
	.next

screenVideoRamTable_HI
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte >VIEWPORT_SCREEN_OFFSET+SCREEN_VIDEO_RAM+tileY*40+tileX*2-80
		.next
	.next

colorRamTable_LO
	.for tileY=0, tileY<=9*2, tileY=tileY+2
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte <VIEWPORT_SCREEN_OFFSET+SCREEN_COLOR_RAM+tileY*40+tileX*2-80
		.next
	.next

colorRamTable_HI
	.for tileY=0, tileY<=9*2, tileY=tileY+1+1
		.for tileX=0, tileX<=8, tileX=tileX+1
			.byte >VIEWPORT_SCREEN_OFFSET+SCREEN_COLOR_RAM+tileY*40+tileX*2-80
		.next
	.next

multiplyBy9
	.for mul=1, mul<=9, mul=mul+1
		.byte mul*9
	.next
