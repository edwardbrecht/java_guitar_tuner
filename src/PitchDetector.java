import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import org.apache.commons.math3.analysis.function.Cos;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.MathArrays;

public class PitchDetector {
    // Autocorrelation function
    public static double[] autocorrelation(short[] inputSignal) {
        int N = inputSignal.length;
        double[] autocorr = new double[N];

        for (int lag = 0; lag < N; lag++) {
            double sum = 0.0;
            for (int i = 0; i < N - lag; i++) {
                sum += inputSignal[i] * inputSignal[i + lag];
            }
            autocorr[lag] = sum;
        }

        return autocorr;
    }

    // Find the pitch (fundamental frequency) from the autocorrelation function
    public static double findPitch(byte[] inputSignal, int sampleRate) {
        // Convert the byte array to an array of shorts (16-bit PCM)
        short[] shortSignal = new short[inputSignal.length / 2];
        for (int i = 0; i < shortSignal.length; i++) {
            shortSignal[i] = (short) ((inputSignal[i * 2] & 0xFF) | (inputSignal[i * 2 + 1] << 8));
        }

        double[] autocorr = autocorrelation(shortSignal);

        // Find the first peak after the first zero crossing
        int firstZeroCrossing = 0;
        while (firstZeroCrossing < autocorr.length && autocorr[firstZeroCrossing] > 0) {
            firstZeroCrossing++;
        }

        int peakIndex = firstZeroCrossing;
        double maxCorrelation = 0.0;

        for (int i = firstZeroCrossing; i < autocorr.length; i++) {
            if (autocorr[i] > maxCorrelation) {
                maxCorrelation = autocorr[i];
                peakIndex = i;
            }
        }

        // Calculate pitch in Hertz
        double pitch = (double) sampleRate / peakIndex;

        return pitch;
    }

    // FFT METHOD
    private static final int SAMPLE_RATE = 44100; // Sample rate in Hz
    private static final int WINDOW_SIZE = 1024; // FFT window size (power of 2)
    private static final int MIN_FREQUENCY = 82;  // Minimum detectable frequency (E2)

    public static double detectPitch(byte[] audioBytes) {
        if (audioBytes.length != 2 * WINDOW_SIZE) {
            throw new IllegalArgumentException("Input data size must be twice the window size.");
        }

        // Convert bytes to doubles (16-bit PCM values)
        double[] audioData = new double[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            int sample = (audioBytes[i * 2] & 0xFF) | (audioBytes[i * 2 + 1] << 8);
            audioData[i] = sample / 32768.0; // Normalize to the range [-1, 1]
        }

        // Apply a window function to the audio data (e.g., Hamming window)
        double[] windowedData = MathArrays.ebeMultiply(audioData, getHammingWindow());

        // Perform FFT on the windowed data
        FastFourierTransformer transformer = new FastFourierTransformer(org.apache.commons.math3.transform.DftNormalization.STANDARD); // CHECK
        Complex[] fftResult = transformer.transform(windowedData, TransformType.FORWARD); // CHECK

        // Find the peak frequency in the FFT result
        int peakIndex = findPeakFrequencyIndex(fftResult);

        // Calculate the pitch in Hertz
        double pitch = peakIndex * SAMPLE_RATE / (double)WINDOW_SIZE;

        return pitch;
    }

    private static double[] getHammingWindow() {
        double[] window = new double[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (WINDOW_SIZE - 1));
        }
        return window;
    }

    private static int findPeakFrequencyIndex(Complex[] fftResult) {
        int maxIndex = -1;
        double maxMagnitude = Double.NEGATIVE_INFINITY;

        // Start from MIN_FREQUENCY Hz to avoid low-frequency noise
        int startIndex = (MIN_FREQUENCY * WINDOW_SIZE / SAMPLE_RATE);

        for (int i = startIndex; i < fftResult.length / 2; i++) {
            double magnitude = fftResult[i].abs();
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static void main(String[] args) {
        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100, // Sample rate (e.g., 44.1 kHz)
                16,    // Sample size in bits (e.g., 16 bits)
                1,     // Number of channels (1 for mono, 2 for stereo)
                2,     // Frame size
                44100, // Frame rate (samples per second)
                false  // Big-endian byte order (false for little-endian)
        );

        TargetDataLine targetDataLine;
        try {
            targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
            targetDataLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return;
        }

        targetDataLine.start();

        GUI gui = new GUI();

        byte[] buffer = new byte[WINDOW_SIZE * 2]; // Buffer size
        while (true) {
            targetDataLine.read(buffer, 0, buffer.length);
            // Process the audio data (e.g., save it to a file, analyze it, etc.)
            // Find the pitch
            double pitchAutoCorr = findPitch(buffer, 44100);
            double pitchFFT = detectPitch(buffer);
            System.out.println("AUTOCORR: Detected Pitch (Hz): " + pitchAutoCorr);
            System.out.println("FFT: Detected Pitch (Hz): " + pitchFFT);

            gui.l1.setText(String.format("AUTOCORR: Detected Pitch (Hz): %.2f", pitchAutoCorr));
            gui.l2.setText(String.format("FFT: Detected Pitch (Hz): %.2f", pitchFFT));

        }


    }
}
