/*
 * Copyright (C) 2012 Brendan Robert (BLuRry) brendan.robert@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jace.hardware.mockingboard;

import jace.hardware.CardMockingboard;

/**
 * Square-wave generator used in the PSG sound chip.
 * Created on April 18, 2006, 5:48 PM
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */
public class SoundGenerator extends TimedGenerator {
    int amplitude;
    boolean useEnvGen;
    boolean active;
    boolean noiseActive;
    boolean inverted;
    public SoundGenerator(int _clock, int _sampleRate) {
        super(_clock, _sampleRate);
    }
    
    @Override
    public int stepsPerCycle() {
        return 8;
    }
    public void setAmplitude(int _amp) {
        amplitude = (_amp & 0x0F);
        useEnvGen = (_amp & 0x010) != 0;
    }
    public void setActive(boolean _active) {
        active = _active;
    }
    public void setNoiseActive(boolean _active) {
        noiseActive = _active;
    }

    @Override
    public void setPeriod(int _period) {
        super.setPeriod(_period);
    }
    
    public int step(NoiseGenerator noiseGen, EnvelopeGenerator envGen) {
        int stateChanges = updateCounter();
        if (((stateChanges & 1) == 1)) inverted = !inverted;
        double amp = stateChanges == 0 ? 1 : 1.0 / Math.max(stateChanges-1, 1);
        int vol = useEnvGen ? envGen.getEffectiveAmplitude() : amplitude;
        boolean on = noiseActive && noiseGen.isOn() || (active && inverted);
        return on ? (int) (CardMockingboard.VolTable[vol] * amp) : 0;
    }
    
    @Override
    public void reset() {
        super.reset();
        amplitude = 0;
        useEnvGen = false;
        active = false;
        noiseActive = false;
        inverted = false;
    }
}