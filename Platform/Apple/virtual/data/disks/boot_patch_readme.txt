Boot patch

This patch sets the stack pointer very early in the boot process.
It works around the behavior of Jace (and possibly other emu's and maybe some real hardware) that sets a random stack pointer at startup.

801: 38 B0 03
80B: 08 8A 29 70 4A 4A 4A 4A -> 8A A2 FF 9A 08 20 F8 09
900: 3F 09 26
9F8: 00 00 00 00 00 00 00 00 -> AA 29 70 4A 4A 4A 4A 60
