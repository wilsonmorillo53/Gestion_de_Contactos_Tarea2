package vista.componentes;

import util.FuenteUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class CampoTextoRedondeado extends JTextField {

    private int radioBorde = 8;
    private Color colorBorde = new Color(176, 200, 222);
    private Color colorBordeFoco = new Color(0, 120, 215);
    private boolean conFoco = false;

    public CampoTextoRedondeado(int columnas) {
        super(columnas);
        configurar();
    }

    private void configurar() {
        setOpaque(false);
        setBackground(Color.WHITE);
        setForeground(new Color(30, 30, 30));
        setCaretColor(new Color(0, 82, 155));
        setFont(FuenteUtil.cargarFuente(13f));
        setBorder(new EmptyBorder(5, 10, 5, 10));

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                conFoco = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                conFoco = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo
        g2.setColor(getBackground());
        g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radioBorde, radioBorde);

        // Borde
        g2.setColor(conFoco ? colorBordeFoco : colorBorde);
        g2.setStroke(new BasicStroke(conFoco ? 1.8f : 1.2f));
        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radioBorde, radioBorde);

        g2.dispose();
        super.paintComponent(g);
    }
}

