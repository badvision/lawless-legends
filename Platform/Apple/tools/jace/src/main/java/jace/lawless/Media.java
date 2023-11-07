package jace.lawless;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javafx.util.Duration;

public class Media {
    int totalSamples = 0;
    float totalDuration = 0;
    long sampleRate = 0;
    boolean isStereo = true;
    ShortBuffer sampleBuffer;

    public Media(String resourcePath) throws IOException {
        // Get caononical file path from relative resource path
        String canonicalPath = URLDecoder.decode(getClass().getResource(resourcePath).getPath(), "UTF-8");
        System.out.println("Loading media: " + canonicalPath);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.callocInt(1);
            Long decoder = STBVorbis.stb_vorbis_open_filename(canonicalPath, error, null);
            if (decoder == null || decoder <= 0) {
                throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + getError(error.get(0)));
            }
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            totalDuration = STBVorbis.stb_vorbis_stream_length_in_seconds(decoder);
            sampleRate = info.sample_rate();
            isStereo = info.channels() == 2;

            sampleBuffer = MemoryUtil.memAllocShort(totalSamples);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, isStereo?2:1, sampleBuffer);
            STBVorbis.stb_vorbis_close(decoder);
            sampleBuffer.rewind();
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    public String getError(int vorbisErrorCode) {
        switch (vorbisErrorCode) {
            case STBVorbis.VORBIS__no_error:
                return "VORBIS_no_error";
            case STBVorbis.VORBIS_need_more_data:
                return "VORBIS_need_more_data";
            case STBVorbis.VORBIS_invalid_api_mixing:
                return "VORBIS_invalid_api_mixing";
            case STBVorbis.VORBIS_outofmem:
                return "VORBIS_outofmem";
            case STBVorbis.VORBIS_feature_not_supported:
                return "VORBIS_feature_not_supported";
            case STBVorbis.VORBIS_too_many_channels:
                return "VORBIS_too_many_channels";
            case STBVorbis.VORBIS_file_open_failure:
                return "VORBIS_file_open_failure";
            case STBVorbis.VORBIS_seek_without_length:
                return "VORBIS_seek_without_length";
            case STBVorbis.VORBIS_unexpected_eof:
                return "VORBIS_unexpected_eof";
            case STBVorbis.VORBIS_seek_invalid: 
                return "VORBIS_seek_invalid";
            case STBVorbis.VORBIS_invalid_setup:
                return "VORBIS_invalid_setup";
            case STBVorbis.VORBIS_invalid_stream:
                return "VORBIS_invalid_stream";
            case STBVorbis.VORBIS_missing_capture_pattern:
                return "VORBIS_missing_capture_pattern";
            case STBVorbis.VORBIS_invalid_stream_structure_version:
                return "VORBIS_invalid_stream_structure_version";
            case STBVorbis.VORBIS_continued_packet_flag_invalid:
                return "VORBIS_continued_packet_flag_invalid";
            case STBVorbis.VORBIS_incorrect_stream_serial_number:
                return "VORBIS_incorrect_stream_serial_number";
            case STBVorbis.VORBIS_invalid_first_page:
                return "VORBIS_invalid_first_page";
            case STBVorbis.VORBIS_bad_packet_type:
                return "VORBIS_bad_packet_type";
            case STBVorbis.VORBIS_cant_find_last_page:
                return "VORBIS_cant_find_last_page";
            case STBVorbis.VORBIS_seek_failed:
                return "VORBIS_seek_failed";
            case STBVorbis.VORBIS_ogg_skeleton_not_supported:
                return "VORBIS_ogg_skeleton_not_supported";
            default:
                return "Unknown error code: " + vorbisErrorCode;
        }
    }

    public void close() {
        MemoryUtil.memFree(sampleBuffer);
    }

    public void seekToTime(Duration millis) {
        int sampleNumber = (int) (millis.toMillis() * sampleRate / 1000);
        sampleNumber = Math.max(0, Math.min(sampleNumber, totalSamples));
        sampleBuffer.position(sampleNumber * (isStereo ? 2 : 1));
    }

    public boolean isEnded() {
        return sampleBuffer.remaining() == 0;
    }

    public void restart() {
        sampleBuffer.rewind();
    }

    public short getNextLeftSample() {
        // read next sample for left and right channels
        if (isEnded()) {
            return 0;
        }
        return sampleBuffer.get();
    }

    public short getNextRightSample() {
        if (isEnded()) {
            return 0;
        }
        return isStereo ? sampleBuffer.get() : sampleBuffer.get(sampleBuffer.position());
    }

    public java.time.Duration getCurrentTime() {
        int sampleNumber = sampleBuffer.position() / (isStereo ? 2 : 1);
        return java.time.Duration.ofMillis((long) (sampleNumber * 1000 / sampleRate));
    }

    public float getTotalDuration() {
        return totalDuration;
    }    
}