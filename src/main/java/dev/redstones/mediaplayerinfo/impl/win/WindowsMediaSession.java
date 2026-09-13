package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.MediaInfo;

public class WindowsMediaSession {
    private final MediaInfo media;
    private final String owner;
    private final int index;

    public WindowsMediaSession(MediaInfo media, String owner, int index) {
        this.media = media;
        this.owner = owner;
        this.index = index;
    }

    public MediaInfo getMedia() {
        return this.media;
    }

    public String getOwner() {
        return this.owner;
    }

    public int getIndex() {
        return this.index;
    }

    public native void play();
    public native void pause();
    public native void playPause();
    public native void stop();
    public native void next();
    public native void previous();
}
