package jace.hardware;

import java.util.function.Function;

import jace.apple2e.VideoDHGR;
import jace.apple2e.VideoNTSC;
import jace.config.DeviceEnum;
import jace.core.Computer;
import jace.core.Video;
import jace.lawless.LawlessVideo;

public enum VideoImpls implements DeviceEnum<Video> {
    DHGR("Double-Hires graphics", VideoDHGR.class, VideoDHGR::new),
    NTSC("NTSC Emulation", VideoNTSC.class, VideoNTSC::new),
    Lawless("Lawless enhanced video", LawlessVideo.class, LawlessVideo::new);

    private final String name;
    private final Class<? extends Video> clazz;
    private final Function<Computer, Video> factory;

    VideoImpls(String name, Class<? extends Video> clazz, Function<Computer, Video> factory) {
        this.name = name;
        this.clazz = clazz;
        this.factory = factory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Video create(Computer computer) {
        return factory.apply(computer);
    }

    @Override
    public boolean isInstance(Video video) {
        return clazz.isInstance(video);
    }
}