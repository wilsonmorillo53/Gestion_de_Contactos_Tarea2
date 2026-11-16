package controlador;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import modelo.Contacto;
import modelo.ContactoDAO;

/**
 * 
 * Busca contactos en un Thread independiente para no bloquear la UI.
 *
 * Uso: new BusquedaContactoTask(dao, criterio, onResultado).ejecutar();
 */
public class BusquedaContactoTask {

    private final ContactoDAO dao;
    private final String criterio;
    private final Consumer<List<Contacto>> onResultado;

    public BusquedaContactoTask(ContactoDAO dao, String criterio,
                                 Consumer<List<Contacto>> onResultado) {
        this.dao         = dao;
        this.criterio    = criterio.toLowerCase();
        this.onResultado = onResultado;
    }

    /** Lanza el hilo independiente de búsqueda. */
    public void ejecutar() {
        new Thread(() -> {
            System.out.println("Hilo búsqueda iniciado: " + Thread.currentThread().getName());

            List<Contacto> resultado = dao.listar().stream()
                .filter(c -> c.getNombre().toLowerCase().contains(criterio)
                          || c.getTelefono().toLowerCase().contains(criterio)
                          || c.getEmail().toLowerCase().contains(criterio))
                .collect(Collectors.toList());

            // Regresa al EDT para actualizar la tabla de forma segura
            SwingUtilities.invokeLater(() -> onResultado.accept(resultado));

        }, "hilo-busqueda").start();
    }
}
