;
; Proportional Font Engine
; by Andrew Hogan
;

fontEngine = $E000

SetFont		= fontEngine
SetWindow	= SetFont+3
ClearWindow	= SetWindow+3
DisplayStr	= ClearWindow+3
