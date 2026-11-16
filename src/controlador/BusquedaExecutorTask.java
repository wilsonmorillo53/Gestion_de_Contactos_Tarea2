package controlador;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import modelo.Contacto;
import modelo.ContactoDAO;

/**
 * 
 * Búsqueda de contactos usando ExecutorService (pool de hilos).
 *
 * Ventaja sobre Thread puro: cancela búsquedas anteriores si el usuario
 * escribe rápido, evitando resultados desordenados.
 *
 * Uso: BusquedaExecutorTask.buscar(dao, criterio, onResultado);
 */
public class BusquedaExecutorTask {

    // Pool de 1 hilo dedicado a búsquedas — se reutiliza en cada consulta
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "hilo-executor-busqueda"));

    private static Future<?> tareaActual = null; // referencia para cancelar

    /**
     * Envía la búsqueda al ExecutorService.
     * Cancela la búsqueda anterior si aún estaba en curso.
     */
    public static void buscar(ContactoDAO dao, String criterio,
                               Consumer<List<Contacto>> onResultado) {
        // Cancelar búsqueda previa (si el usuario escribe rápido)
        if (tareaActual != null && !tareaActual.isDone()) {
            tareaActual.cancel(true);
        }

        final String texto = criterio.toLowerCase();

        tareaActual = EXECUTOR.submit(() -> {
            System.out.println("ExecutorService hilo: " + Thread.currentThread().getName());

            List<Contacto> resultado = dao.listar().stream()
                .filter(c -> c.getNombre().toLowerCase().contains(texto)
                          || c.getTelefono().toLowerCase().contains(texto)
                          || c.getEmail().toLowerCase().contains(texto))
                .collect(Collectors.toList());

            // Regresa al EDT para actualizar la UI
            SwingUtilities.invokeLater(() -> onResultado.accept(resultado));
        });
    }

    /** Cierra el pool al salir de la aplicación. */
    public static void cerrar() {
        EXECUTOR.shutdown();
    }
}
