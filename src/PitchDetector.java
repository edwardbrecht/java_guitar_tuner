import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;


public class PitchDetector {

    public boolean listening = false;

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

        // Preprocessing

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

        return (double) sampleRate / peakIndex;
    }

    public void stopListening() {
        listening = false;
    }

    public String frequencyToNoteName(double frequency) {
        if (frequency <= 0) {
            return "Invalid frequency";
        }

        double A4Frequency = 440.0; // The frequency of A4 in Hz

        // Calculate the number of semitones away from A4
        int semitones = (int) Math.round(12 * Math.log(frequency / A4Frequency) / Math.log(2));

        // Define an array of note names
        String[] noteNames = {
                "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"
        };

        // Calculate the note index within the octave
        int noteIndex = (semitones % 12);
        if (noteIndex < 0) {
            noteIndex = noteNames.length - Math.abs(noteIndex);
        }

        return noteNames[noteIndex];
    }

    public static void main(String[] args) {
        PitchDetector pd = new PitchDetector();
        GUI gui = new GUI();
        gui.b.addActionListener(e -> pd.stopListening());

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
        pd.listening = true;

        byte[] buffer = new byte[1024]; // Buffer size
        while (pd.listening) {
            targetDataLine.read(buffer, 0, buffer.length);
            // Process the audio data (e.g., save it to a file, analyze it, etc.)
            // Find the pitch
            double pitch = findPitch(buffer, 44100);

            // Update display
            gui.l1.setText(String.format("Frequency is %.2f Hz", pitch));
            gui.l2.setText(pd.frequencyToNoteName(pitch));

        }

        targetDataLine.stop();
        targetDataLine.close();
    }
}
