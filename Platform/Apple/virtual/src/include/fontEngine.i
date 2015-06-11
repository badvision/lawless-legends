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
