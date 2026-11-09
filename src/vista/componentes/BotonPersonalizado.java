package vista.componentes;

import util.FuenteUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BotonPersonalizado extends JButton {

    private Color colorNormal;
    private Color colorHover;
    private Color colorPresionado;
    private int radioBorde = 10;
    private boolean vertical = false;

    public BotonPersonalizado(String texto, Icon icono, Color colorBase, boolean vertical) {
        super(texto, icono);
        this.colorNormal = colorBase;
        this.colorHover = calcularHover(colorBase);
        this.colorPresionado = calcularPresionado(colorBase);
        this.vertical = vertical;

        configurarEstilo();
    }

    private void configurarEstilo() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(FuenteUtil.cargarFuenteBold(13f));

        if (vertical) {
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setIconTextGap(6);
            setMargin(new Insets(6, 8, 6, 8));
        } else {
            setVerticalTextPosition(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setIconTextGap(8);
            setMargin(new Insets(6, 10, 6, 10));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                repaint();
            }
        });
    }

    public void setColorBase(Color nuevoColor) {
        this.colorNormal = nuevoColor;
        this.colorHover = calcularHover(nuevoColor);
        this.colorPresionado = calcularPresionado(nuevoColor);
        repaint();
    }

    private Color calcularHover(Color c) {
        int r = Math.min(255, (int) (c.getRed() * 1.15) + 15);
        int g = Math.min(255, (int) (c.getGreen() * 1.15) + 15);
        int b = Math.min(255, (int) (c.getBlue() * 1.15) + 15);
        return new Color(r, g, b);
    }

    private Color calcularPresionado(Color c) {
        int r = Math.max(0, (int) (c.getRed() * 0.85));
        int g = Math.max(0, (int) (c.getGreen() * 0.85));
        int b = Math.max(0, (int) (c.getBlue() * 0.85));
        return new Color(r, g, b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fondo;
        if (!isEnabled()) {
            fondo = new Color(170, 185, 200);
        } else if (getModel().isPressed()) {
            fondo = colorPresionado;
        } else if (getModel().isRollover()) {
            fondo = colorHover;
        } else {
            fondo = colorNormal;
        }

        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radioBorde, radioBorde);
        g2.dispose();

        super.paintComponent(g);
    }
}

