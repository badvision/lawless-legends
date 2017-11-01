Converting MIDI files to internal music secquencer files:

cvtmid.py, the MIDI file converter, uses the mido package: https://mido.readthedocs.io/en/latest/

Simply put a MIDI file in the same directory as ctvmid.py and type:
'''
./cvtmid.py midifile.mid > midifile.seq
'''

The midifile.seq output file is an ACME assembly file that can be included in another assembly file, a PLASMA file, or assembled into a binary file which can be loaded later. Simply type:
'''
acme --setpc 0x1000 -o seqfile.bin midifile.seq
'''
The starting address is irrelevant, but ACME requires one to assemble properly.
