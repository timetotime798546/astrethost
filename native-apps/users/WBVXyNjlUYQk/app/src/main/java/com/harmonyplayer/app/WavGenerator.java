package com.harmonyplayer.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WavGenerator {

    public static File generateTrack(File cacheDir, int trackId) {
        String fileName = "track_" + trackId + ".wav";
        File file = new File(cacheDir, fileName);
        if (file.exists()) {
            return file; // Reuse if already generated
        }

        int sampleRate = 22050;
        double durationSeconds = 15.0;
        int numSamples = (int) (durationSeconds * sampleRate);
        int headerSize = 44;
        int dataSize = numSamples * 2; // 16-bit mono
        int totalSize = headerSize + dataSize;

        byte[] wavData = new byte[totalSize];

        // WAV Header configuration
        writeString(wavData, 0, "RIFF");
        writeInt(wavData, 4, totalSize - 8);
        writeString(wavData, 8, "WAVE");
        writeString(wavData, 12, "fmt ");
        writeInt(wavData, 16, 16); // subchunk 1 size (16 for PCM)
        writeShort(wavData, 20, (short) 1); // audio format (1 = PCM)
        writeShort(wavData, 22, (short) 1); // num channels (1 = mono)
        writeInt(wavData, 24, sampleRate); // sample rate
        writeInt(wavData, 28, sampleRate * 2); // byte rate (sample rate * block align)
        writeShort(wavData, 32, (short) 2); // block align
        writeShort(wavData, 34, (short) 16); // bits per sample
        writeString(wavData, 36, "data");
        writeInt(wavData, 40, dataSize);

        // Synthesize Audio Loops Mathematically
        int offset = 44;
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / sampleRate;
            short value = 0;

            if (trackId == 1) {
                // Neon Horizon (BPM 120, 0.5s per quarter note)
                double beat = t * 2.0; // 2 beats per second
                int beatIndex = (int) beat;
                double beatFraction = beat - beatIndex;

                // Bassline arpeggio wave
                double[] bassFreqs = {110.0, 110.0, 130.81, 130.81, 146.83, 146.83, 164.81, 196.0};
                double f1 = bassFreqs[beatIndex % bassFreqs.length];
                double bassWave = Math.sin(2.0 * Math.PI * f1 * t);

                // Lead melody wave
                double[] leadFreqs = {220.0, 261.63, 293.66, 329.63, 392.0, 329.63, 293.66, 261.63};
                double f2 = leadFreqs[(beatIndex / 2) % leadFreqs.length];
                double leadWave = Math.sin(2.0 * Math.PI * f2 * t) * Math.exp(-4.0 * beatFraction); // Exponential pluck effect

                double mixed = (bassWave * 0.4) + (leadWave * 0.3);
                value = (short) (mixed * 32767.0);

            } else if (trackId == 2) {
                // Retro Lullaby (BPM 85, 0.7s per beat)
                double beat = t / 0.7;
                int beatIndex = (int) beat;
                double beatFraction = beat - beatIndex;

                // Soft square/triangle wave melody
                double[] melodyFreqs = {261.63, 329.63, 392.0, 523.25, 440.0, 349.23, 392.0, 261.63};
                double f = melodyFreqs[beatIndex % melodyFreqs.length];

                // Triangle-like wave synthesis for classic chiptune feel
                double wave = (Math.abs((t * f) % 1.0 - 0.5) - 0.25) * 4.0;
                double decay = Math.exp(-2.5 * beatFraction);

                value = (short) (wave * 0.4 * decay * 32767.0);

            } else {
                // Cosmic Whispers (Ambient, slow 3s cycles)
                double baseFreq = 146.83 + 20.0 * Math.sin(2.0 * Math.PI * 0.1 * t); // Sweeping LFO
                double wave1 = Math.sin(2.0 * Math.PI * baseFreq * t);
                double wave2 = Math.sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.3; // Harmonic frequency

                // Pulse tremolo effect
                double tremolo = 0.5 + 0.5 * Math.sin(2.0 * Math.PI * 0.3 * t);
                double mixed = (wave1 + wave2) * 0.3 * tremolo;

                value = (short) (mixed * 32767.0);
            }

            wavData[offset + i * 2] = (byte) (value & 0xFF);
            wavData[offset + i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(wavData);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
        }

        return file;
    }

    private static void writeString(byte[] data, int offset, String s) {
        for (int i = 0; i < s.length(); i++) {
            data[offset + i] = (byte) s.charAt(i);
        }
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}