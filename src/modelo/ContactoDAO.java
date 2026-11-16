package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Repositorio en memoria para mantener los contactos durante la ejecución.
 * No utiliza una base de datos externa.
 *
 * Las operaciones sobre contactos y bloqueos se sincronizan para evitar
 * condiciones de carrera entre los hilos de fondo y la interfaz.
 * ────────────────────────────────────────────────────────────────────────────
 */
public class ContactoDAO {
    // La lista es private final; el bloqueo se hace a nivel de método (this).
    private final List<Contacto> contactos = new ArrayList<>();
    private final Map<Contacto, BloqueoEdicion> bloqueos = new IdentityHashMap<>();

    public static final class BloqueoEdicion {
        private final Contacto contacto;

        private BloqueoEdicion(Contacto contacto) {
            this.contacto = contacto;
        }

        public Contacto getContacto() {
            return contacto;
        }
    }

    /** Agrega un contacto de forma sincronizada. */
    public synchronized void agregar(Contacto contacto) {
        contactos.add(contacto);
    }

    /** Comprueba duplicados y agrega el contacto bajo el mismo bloqueo. */
    public synchronized boolean agregarSiNoExiste(Contacto candidato) {
        boolean duplicado = contactos.stream().anyMatch(c ->
                c.getNombre().equalsIgnoreCase(candidato.getNombre())
                        && (c.getTelefono().equalsIgnoreCase(candidato.getTelefono())
                        || (!candidato.getEmail().isEmpty()
                        && c.getEmail().equalsIgnoreCase(candidato.getEmail()))));
        if (duplicado) return false;
        contactos.add(candidato);
        return true;
    }

    public synchronized boolean eliminar(Contacto contacto) {
        Contacto existente = buscar(contacto);
        if (existente == null || bloqueos.containsKey(existente)) return false;
        return contactos.remove(existente);
    }

    /** Elimina el contacto ubicado en el índice indicado. */
    public synchronized boolean eliminar(int indice) {
        if (indice < 0 || indice >= contactos.size()) {
            return false;
        }
        if (bloqueos.containsKey(contactos.get(indice))) return false;
        contactos.remove(indice);
        return true;
    }

    /** Reserva el contacto indicado; devuelve null si ya está reservado. */
    public synchronized BloqueoEdicion bloquearEdicion(Contacto seleccionado) {
        Contacto contacto = buscar(seleccionado);
        if (contacto == null || bloqueos.containsKey(contacto)) return null;
        BloqueoEdicion bloqueo = new BloqueoEdicion(contacto);
        bloqueos.put(contacto, bloqueo);
        return bloqueo;
    }

    /** Solo el propietario del bloqueo puede guardar los cambios. */
    public synchronized boolean modificar(BloqueoEdicion bloqueo, Contacto actualizado) {
        if (bloqueo == null || actualizado == null
                || bloqueos.get(bloqueo.contacto) != bloqueo) return false;
        int indice = indiceDe(bloqueo.contacto);
        if (indice < 0) return false;
        contactos.set(indice, actualizado);
        bloqueos.remove(bloqueo.contacto);
        return true;
    }

    /** Libera el bloqueo al cancelar la edición. */
    public synchronized void desbloquearEdicion(BloqueoEdicion bloqueo) {
        if (bloqueo != null && bloqueos.get(bloqueo.contacto) == bloqueo) {
            bloqueos.remove(bloqueo.contacto);
        }
    }

    private int indiceDe(Contacto contacto) {
        for (int i = 0; i < contactos.size(); i++) {
            if (contactos.get(i) == contacto) return i;
        }
        return -1;
    }

    private Contacto buscar(Contacto seleccionado) {
        if (seleccionado == null) return null;
        return contactos.stream().filter(c ->
                Objects.equals(c.getNombre(), seleccionado.getNombre())
                        && Objects.equals(c.getTelefono(), seleccionado.getTelefono())
                        && Objects.equals(c.getEmail(), seleccionado.getEmail()))
                .findFirst().orElse(null);
    }

    /**
     * Devuelve una copia inmutable de la lista.
     * Los hilos de fondo (p. ej. ValidacionContactoTask) llaman a este método
     * de forma segura sin bloquear la UI.
     */
    public synchronized List<Contacto> listar() {
        return Collections.unmodifiableList(new ArrayList<>(contactos));
    }

    public synchronized int total() {
        return contactos.size();
    }
}
