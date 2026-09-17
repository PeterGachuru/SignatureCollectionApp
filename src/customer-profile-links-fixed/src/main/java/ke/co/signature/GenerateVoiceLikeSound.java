package ke.co.signature;

import java.io.*;

public class GenerateVoiceLikeSound {

    public static void main(String[] args) throws Exception {

        int sampleRate = 44100;
        int durationSec = 10;

        // Human voice fundamental (try 110 for male, 220 for female)
        double fundamentalFreq = 80.0;

        byte[] audioData = new byte[sampleRate * durationSec * 2];

        int index = 0;

        for (int i = 0; i < sampleRate * durationSec; i++) {

            double t = i / (double) sampleRate;

            // --- Voice source (vocal cords) ---
            double source =
                    Math.sin(2 * Math.PI * fundamentalFreq * t) * 0.6 +
                            Math.sin(2 * Math.PI * fundamentalFreq * 2 * t) * 0.25 +
                            Math.sin(2 * Math.PI * fundamentalFreq * 3 * t) * 0.15;

            // --- Formants (mouth shaping) ---
            double formant1 = Math.sin(2 * Math.PI * 700 * t) * 0.3;
            double formant2 = Math.sin(2 * Math.PI * 1200 * t) * 0.2;
            double formant3 = Math.sin(2 * Math.PI * 2600 * t) * 0.1;

            // --- Breath noise ---
            double noise = (Math.random() * 2 - 1) * 0.02;

            // --- Envelope (speech loudness changes) ---
            double envelope = Math.sin(Math.PI * i / (sampleRate * durationSec));

            double sampleValue =
                    (source + formant1 + formant2 + formant3 + noise) * envelope;

            // Clamp
            sampleValue = Math.max(-1.0, Math.min(1.0, sampleValue));

            short sample = (short) (sampleValue * Short.MAX_VALUE);

            audioData[index++] = (byte) (sample & 0xff);
            audioData[index++] = (byte) ((sample >> 8) & 0xff);
        }

        writeWavFile("voice_like.wav", audioData, sampleRate);
        System.out.println("voice_like.wav created");
    }

    private static void writeWavFile(String filename, byte[] audioData, int sampleRate)
            throws IOException {

        try (FileOutputStream out = new FileOutputStream(filename)) {

            int byteRate = sampleRate * 2;

            out.write(new byte[] {
                    'R','I','F','F',
                    0,0,0,0,
                    'W','A','V','E',
                    'f','m','t',' ',
                    16,0,0,0,
                    1,0,
                    1,0,
                    (byte)(sampleRate), (byte)(sampleRate>>8), 0, 0,
                    (byte)(byteRate), (byte)(byteRate>>8), 0, 0,
                    2,0,
                    16,0,
                    'd','a','t','a',
                    0,0,0,0
            });

            out.write(audioData);
        }
    }
}
