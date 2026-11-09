package vista;

import util.FuenteUtil;
import util.GestorIdioma;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PanelEstadisticas extends JPanel {

    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblConEmail = new JLabel();
    private final JLabel lblSinEmail = new JLabel();
    private final JLabel lblUltimo = new JLabel();

    private int totalActual = 0;
    private int conEmailActual = 0;
    private int sinEmailActual = 0;
    private String ultimoActual = null;

    public PanelEstadisticas() {
        setBackground(new Color(234, 242, 249));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel contenedorTarjetas = new JPanel(new GridLayout(4, 1, 12, 12));
        contenedorTarjetas.setOpaque(false);

        lblTitulo.setFont(FuenteUtil.cargarFuenteBold(16f));
        lblTitulo.setForeground(new Color(11, 58, 98));
        lblTitulo.setBorder(new EmptyBorder(0, 5, 15, 0));
        add(lblTitulo, BorderLayout.NORTH);

        Font fuenteItem = FuenteUtil.cargarFuente(14f);
        for (JLabel etiqueta : new JLabel[]{lblTotal, lblConEmail, lblSinEmail, lblUltimo}) {
            etiqueta.setFont(fuenteItem);
            etiqueta.setForeground(new Color(30, 45, 60));
            JPanel tarjeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(200, 218, 235));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tarjeta.setOpaque(false);
            tarjeta.add(etiqueta);
            contenedorTarjetas.add(tarjeta);
        }

        add(contenedorTarjetas, BorderLayout.CENTER);

        // Suscribirse a cambios de idioma
        GestorIdioma.agregarListener(nuevoIdioma -> refrescarTextos());

        refrescarTextos();
    }

    public void actualizar(int total, int conEmail, int sinEmail, String ultimo) {
        this.totalActual = total;
        this.conEmailActual = conEmail;
        this.sinEmailActual = sinEmail;
        this.ultimoActual = ultimo;
        refrescarTextos();
    }

    public void refrescarTextos() {
        lblTitulo.setText(GestorIdioma.get("stats.title"));
        lblTotal.setText(GestorIdioma.get("stats.total") + totalActual);
        lblConEmail.setText(GestorIdioma.get("stats.with_email") + conEmailActual);
        lblSinEmail.setText(GestorIdioma.get("stats.without_email") + sinEmailActual);
        String txtUltimo = (ultimoActual == null || ultimoActual.isEmpty())
                ? GestorIdioma.get("stats.none")
                : ultimoActual;
        lblUltimo.setText(GestorIdioma.get("stats.last_added") + txtUltimo);
    }
}
