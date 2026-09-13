package dev.redstones.mediaplayerinfo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class MediaInfo {
    private final String title;
    private final String artist;
    private final byte[] artworkPng;
    private final long position;
    private final long duration;
    private final boolean playing;
    private BufferedImage artwork;

    public MediaInfo(String title, String artist, byte[] artworkPng, long position, long duration, boolean playing) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.artworkPng = artworkPng != null ? artworkPng : new byte[0];
        this.position = position;
        this.duration = duration;
        this.playing = playing;
    }

    public String getTitle() {
        return this.title;
    }

    public String getArtist() {
        return this.artist;
    }

    public byte[] getArtworkPng() {
        return this.artworkPng;
    }

    public long getPosition() {
        return this.position;
    }

    public long getDuration() {
        return this.duration;
    }

    public boolean getPlaying() {
        return this.playing;
    }

    public BufferedImage getArtwork() {
        if (this.artwork == null && this.artworkPng != null && this.artworkPng.length > 0) {
            try {
                this.artwork = ImageIO.read(new ByteArrayInputStream(this.artworkPng));
            } catch (Exception ignored) {}
        }
        return this.artwork;
    }
}
