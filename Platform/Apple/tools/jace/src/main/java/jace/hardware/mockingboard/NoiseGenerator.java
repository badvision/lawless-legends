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

/**
 * Noise generator of the PSG sound chip.
 * Created on April 18, 2006, 5:47 PM
 * @author Brendan Robert (BLuRry) brendan.robert@gmail.com 
 */
public class NoiseGenerator extends TimedGenerator {
    int rng = 0x003333;
    public NoiseGenerator(int _clock,int _sampleRate) {
        super(_clock, _sampleRate);
    }
    @Override
    public int stepsPerCycle() {
        return 4;
    }
    public void step() {
        int stateChanges = updateCounter();
        for (int i=0; i < stateChanges; i++)
            updateRng();
    }
    public static final int BIT17 = 0x010000;
    public void updateRng() {
        rng = ((rng & 1) != 0 ? rng ^ 0x24000 : rng) >> 1;
        if ((rng & 1) == 1) {
            state = !state;
        }
    }
    
    boolean state = false;
    public boolean isOn() {
        return state;
    }
}