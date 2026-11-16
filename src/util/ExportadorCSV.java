package util;

import modelo.Contacto;

import javax.swing.SwingUtilities;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class ExportadorCSV {
    private static final Object CANDADO_ARCHIVO = new Object();

    private ExportadorCSV() {}

    public static void exportarEnSegundoPlano(List<Contacto> contactos, Path destino,
                                               Runnable alTerminar, Consumer<IOException> alFallar) {
        List<Contacto> copia = List.copyOf(contactos);
        new Thread(() -> {
            try {
                exportar(copia, destino);
                SwingUtilities.invokeLater(alTerminar);
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> alFallar.accept(ex));
            }
        }, "hilo-exportacion").start();
    }

    public static void exportar(List<Contacto> contactos, Path destino) throws IOException {
        synchronized (CANDADO_ARCHIVO) {
            try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
                writer.write("Nombre,Telefono,Email");
                writer.newLine();
                for (Contacto c : contactos) {
                    writer.write(String.join(",",
                            campo(c.getNombre()), campo(c.getTelefono()), campo(c.getEmail())));
                    writer.newLine();
                }
            }
        }
    }

    private static String campo(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\"", "\"\"")) + "\"";
    }
}
