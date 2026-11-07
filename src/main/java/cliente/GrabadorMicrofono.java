package cliente;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Graba el micrófono a WAV 16 kHz mono 16 bit (el formato que Whisper procesa sin
 * remuestrear). Java puro, sin JavaFX. Corta solo al llegar a {@link #SEGUNDOS_MAXIMOS}.
 */
public class GrabadorMicrofono {

    public static final int SEGUNDOS_MAXIMOS = 60;

    private static final AudioFormat FORMATO = new AudioFormat(16000f, 16, 1, true, false);
    private static final int BYTES_MAXIMOS = (int) FORMATO.getFrameRate() * FORMATO.getFrameSize() * SEGUNDOS_MAXIMOS;

    private TargetDataLine linea;
    private Thread hilo;
    private ByteArrayOutputStream pcm;

    public synchronized boolean isGrabando() {
        return linea != null;
    }

    public synchronized void iniciar() throws LineUnavailableException {
        if (linea != null) {
            return;
        }
        TargetDataLine nueva = AudioSystem.getTargetDataLine(FORMATO);
        nueva.open(FORMATO);
        nueva.start();
        ByteArrayOutputStream destino = new ByteArrayOutputStream();
        hilo = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (destino.size() < BYTES_MAXIMOS) {
                int leidos = nueva.read(buffer, 0, buffer.length);
                if (leidos <= 0) {
                    break; // la línea se detuvo/cerró
                }
                destino.write(buffer, 0, leidos);
            }
        }, "cerberusai-microfono");
        hilo.setDaemon(true);
        hilo.start();
        linea = nueva;
        pcm = destino;
    }

    /** Detiene la grabación y devuelve el audio como WAV (vacío si no se grababa). */
    public synchronized byte[] detener() throws IOException, InterruptedException {
        if (linea == null) {
            return new byte[0];
        }
        linea.stop();
        linea.close();
        hilo.join(2000);
        byte[] datos = pcm.toByteArray();
        linea = null;
        hilo = null;
        pcm = null;

        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(datos), FORMATO,
                datos.length / FORMATO.getFrameSize());
        AudioSystem.write(audio, AudioFileFormat.Type.WAVE, wav);
        return wav.toByteArray();
    }
}
