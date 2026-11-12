package com.breathe.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Soft synthesized chimes (no asset files): high = inhale, mid = hold, low = exhale. */
public class SoundPlayer {
    private static final int SR = 44100;
    private static final double[] FREQ = {659.25, 523.25, 440.0};
    private final AudioTrack[] tracks = new AudioTrack[3];

    public SoundPlayer() {
        for (int i = 0; i < 3; i++) {
            try { tracks[i] = build(chime(FREQ[i])); } catch (RuntimeException ignored) { }
        }
    }

    private static short[] chime(double f) {
        int n = (int) (SR * 1.1);
        short[] b = new short[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double env = Math.exp(-3.8 * t) * Math.min(1.0, t / 0.015);   // soft attack, slow decay
            double s = Math.sin(2 * Math.PI * f * t)
                    + 0.35 * Math.sin(2 * Math.PI * 2 * f * t) * Math.exp(-6 * t)
                    + 0.12 * Math.sin(2 * Math.PI * 3 * f * t) * Math.exp(-9 * t);
            b[i] = (short) (s * env * 0.28 * Short.MAX_VALUE);
        }
        return b;
    }

    private static AudioTrack build(short[] pcm) {
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC).build();
        t.write(pcm, 0, pcm.length);
        return t;
    }

    public void play(int phase) {
        AudioTrack t = tracks[phase == 0 ? 0 : phase == 2 ? 2 : 1];
        if (t == null) return;
        try { t.stop(); t.reloadStaticData(); t.play(); } catch (IllegalStateException ignored) { }
    }

    public void release() {
        for (AudioTrack t : tracks) if (t != null) { try { t.release(); } catch (RuntimeException ignored) { } }
    }
}
