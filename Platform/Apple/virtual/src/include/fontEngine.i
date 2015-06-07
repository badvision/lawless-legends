;
; Proportional Font Engine
; by Andrew Hogan
;

fontEngine = $E000

SetFont = fontEngine
PlotAsc = SetFont+3
ClearWindow = PlotAsc+3