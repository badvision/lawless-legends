/*
 * Copyright 2018 org.badvision.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jace.core;

/**
 * Encapsulate a thread-managed sound device, abstracting aspects of buffer and device management
 */
public abstract class SoundGeneratorDevice extends Device {
    public SoundGeneratorDevice(Computer computer) {
        super(computer);
    }
    
    @Override
    public void reconfigure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void attach() {
        super.attach();
        
    }
    
    @Override
    public void detach() {
        super.detach();
        
    }
    
}
