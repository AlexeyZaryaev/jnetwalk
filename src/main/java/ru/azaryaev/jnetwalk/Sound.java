package ru.azaryaev.jnetwalk;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.InputStream;

public final class Sound {

    private static Clip clickClip;

    private Sound() {}

    public static void load() {
        clickClip = loadClip("/sounds/clatz.wav");
    }

    public static void playClick() {
        play(clickClip);
    }

    private static Clip loadClip(String path) {
        try (InputStream is = Sound.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Sound not found: " + path);
                return null;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.err.println("Can't load " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static void play(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }
}