package jace.lawless;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import javafx.util.Duration;

public class Media {
    long streamHandle = -1;
    long totalSamples = 0;
    float totalDuration = 0;
    long sampleRate = 0;
    boolean isStereo = true;
    ShortBuffer sampleBuffer = ShortBuffer.allocate(2);

    public Media(String resourcePath) throws IOException {
        // Load resource into memory
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
    
            IntBuffer error = IntBuffer.allocate(1);
            streamHandle = STBVorbis.stb_vorbis_open_memory(byteBuffer, error, null);
            
            MemoryStack stack = MemoryStack.stackPush();
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(streamHandle, info);
            totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(streamHandle);
            totalDuration = STBVorbis.stb_vorbis_stream_length_in_seconds(streamHandle);
            sampleRate = info.sample_rate();
            isStereo = info.channels() == 2;
            info.free();
        }
    }

    public void close() {
        STBVorbis.stb_vorbis_close(streamHandle);
    }

    public void seekToTime(Duration millis) {
        int sampleNumber = (int) (millis.toMillis() * sampleRate / 1000);
        STBVorbis.stb_vorbis_seek(streamHandle, sampleNumber);
    }

    public boolean isEnded() {
        return STBVorbis.stb_vorbis_get_sample_offset(streamHandle) >= totalSamples;
    }

    public void restart() {
        STBVorbis.stb_vorbis_seek_start(streamHandle);
    }

    public short getNextLeftSample() {
        // read next sample for left and right channels
        int numSamples = STBVorbis.stb_vorbis_get_frame_short_interleaved(streamHandle, isStereo ? 2 : 1, sampleBuffer);
        if (numSamples == 0) {
            return 0;
        }
        return sampleBuffer.get(0);
    }

    public short getNextRightSample() {
        return isStereo ? sampleBuffer.get(1) : sampleBuffer.get(0);
    }

    public java.time.Duration getCurrentTime() {
        int sampleNumber = STBVorbis.stb_vorbis_get_sample_offset(streamHandle);
        return java.time.Duration.ofMillis((long) (sampleNumber * 1000 / sampleRate));
    }    
}