package controlador;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class NotificacionTask {
    private static final BlockingQueue<String> COLA = new LinkedBlockingQueue<>();
    private static volatile Consumer<String> mostrar = mensaje -> { };
    private static boolean iniciado;

    private NotificacionTask() {}

    public static synchronized void iniciar(Consumer<String> destino) {
        mostrar = Objects.requireNonNull(destino);
        if (iniciado) return;
        iniciado = true;
        Thread hilo = new Thread(() -> {
            while (true) {
                try {
                    String mensaje = COLA.take();
                    SwingUtilities.invokeLater(() -> mostrar.accept(mensaje));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "hilo-notificacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    public static void publicar(String mensaje) {
        if (mensaje != null && !mensaje.isBlank()) COLA.offer(mensaje);
    }
}
