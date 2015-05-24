Screen memory specification:

	Screen is available in Bank #1 ($4000 - $7FFF)
	Bitmap is available from $6000 to $7F3F
	Video Color RAM is available from $5C00 to $5FF7
	Color RAM is available from $D800 to $DBE7 (this range is a default Color RAM range on a C64, cannot be changed)

	Sprite pointers are available from $5FF8 to $5FFF
	Sprites are available from $5b80 to $5bff (just two sprites)

Features:
	Graphic displayed in multicolor bitmap mode
	Map size can be freely changed (size of map is saved in header)
	64 tiles (each tile contains 4 characters (8x8 pixels each in multicolor mode))
	Joystick and keyboard control (WASD keys)
	Text can be added to any location
	Added some fonts

Tools used:
	64tass compiler
	GangEd for tiles (data saved separately as videomem, colormem and bitmap) and fonts
	SpritePad for sprites
