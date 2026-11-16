package controlador;

import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import modelo.Contacto;
import modelo.ContactoDAO;

/**
 * 
 * Valida si un contacto es duplicado usando un Thread explícito.
 */
public class ValidacionContactoTask {

    private final ContactoDAO dao;
    private final Contacto candidato;
    private final Runnable onValido;
    private final Consumer<String> onDuplicado;

    public ValidacionContactoTask(ContactoDAO dao, Contacto candidato,
                                  Consumer<String> onDuplicado, Runnable onValido) {
        this.dao         = dao;
        this.candidato   = candidato;
        this.onDuplicado = onDuplicado;
        this.onValido    = onValido;
    }

    /** Lanza el hilo de validación. */
    public void ejecutar() {
        new Thread(() -> {
            boolean agregado = dao.agregarSiNoExiste(candidato);

            SwingUtilities.invokeLater(() -> {
                if (agregado) onValido.run();
                else onDuplicado.accept("El contacto ya está registrado.");
            });

        }, "hilo-validacion").start();
    }
}
