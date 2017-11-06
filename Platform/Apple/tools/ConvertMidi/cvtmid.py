#!/usr/bin/python

import sys
#from mido import MidiFile
import mido

mid = mido.MidiFile(sys.argv[1])
totaltime = 0
perctime  = 0
for msg in mid:
    if msg.type == 'note_on' or msg.type == 'note_off':
        if msg.time > 0.0 and msg.time < 1.0/16.0:
            msg.time = 1.0/16.0
        deltatime = int(msg.time * 16)
        octave    = int(msg.note / 12 - 1)
        onote     = int(msg.note % 12)
        lrchan    = int(msg.channel & 1)
        vol       = int(msg.velocity >> 3)
        if msg.velocity > 0 and vol == 0:
            vol = 1
        if msg.type == 'note_off':
            vol = 0
        if octave < 0:
            octave = 0
        totaltime += deltatime
        if msg.channel == 9:
            #
            # Percussion
            #
            if vol > 0 and (totaltime - perctime) > 4:
                print '\t!BYTE\t${0:02X}, ${1:02X}, ${2:02X}\t; Percussion'.format(deltatime, msg.note >> 3, 2)
                perctime = totaltime
        else:
            #
            # Note
            #
            print '\t!BYTE\t${0:02X}, ${1:02X}, ${2:02X}\t; Note {3:d} Chan {4:d} Vol {5:d}'.format(deltatime, 0x80 | (octave << 4) | onote, (lrchan << 7) | vol, msg.note, msg.channel, vol)
print '\t!BYTE\t$00, $00, $00'
