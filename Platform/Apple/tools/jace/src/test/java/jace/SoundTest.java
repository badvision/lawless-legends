package jace;

import java.util.concurrent.ExecutionException;

import jace.core.SoundMixer;
import jace.core.SoundMixer.SoundBuffer;

public class SoundTest {
    
    // @Test
    public void soundGenerationTest() {
        try {
        System.out.println("Performing sound test...");
        System.out.println("Create mixer");
        SoundMixer mixer = new SoundMixer(null);
        System.out.println("Attach mixer");
        mixer.attach();
        System.out.println("Allocate buffer");
        SoundBuffer buffer = SoundMixer.createBuffer(false);
        System.out.println("Generate sound");
        for (int i = 0; i < 100000; i++) {
            // Gerate a sin wave with a frequency sweep so we can tell if the buffer is being fully processed
            double x = Math.sin(i*i * 0.0001);
            buffer.playSample((short) (Short.MAX_VALUE * x));
        }
        System.out.println("Closing buffer");
        buffer.shutdown();
        System.out.println("Deactivating sound");
        mixer.detach();
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
