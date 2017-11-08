#!/usr/bin/python

import sys
#from mido import MidiFile
import mido

mid = mido.MidiFile(sys.argv[1])
timescale = 16.0
totaltime = 0
eventtime = 0.0
for msg in mid:
    eventtime += msg.time * timescale
    #print '; time = ', msg.time
    if msg.type == 'note_on' or msg.type == 'note_off':
        if eventtime > 0.0 and eventtime < 0.5:
            eventtime = 0.5
        deltatime = int(eventtime + 0.5)
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
        if msg.channel == 9 or msg.channel == 8:
            #
            # Percussion
            #
            if vol > 0:
                print '\t!BYTE\t${0:02X}, ${1:02X}, ${2:02X}\t; Percussion {3:d} Chan {4:d} Dur {5:d}'.format(deltatime, msg.note ^ 0x40, (lrchan << 7) | vol, msg.note, msg.channel, vol)
                eventtime = 0.0
        else:
            #
            # Note
            #
            print '\t!BYTE\t${0:02X}, ${1:02X}, ${2:02X}\t; Note {3:d} Chan {4:d} Vol {5:d}'.format(deltatime, 0x80 | (octave << 4) | onote, (lrchan << 7) | vol, msg.note, msg.channel, vol)
            eventtime = 0.0
    elif msg.type == 'set_tempo':
        pass
        timescale = msg.tempo / 500000.0 * 16.0
        print '; timescale = ', timescale
    elif msg.type == 'time_signature':
        pass
    elif msg.type == 'control_chage':
        pass
    elif msg.type == 'program_change':
        pass
print '\t!BYTE\t${0:02X}, $00, $00'.format(int(eventtime + 0.5))
