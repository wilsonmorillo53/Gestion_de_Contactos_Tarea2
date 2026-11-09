package vista.componentes;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class IconoUtil {

    private static final Map<String, ImageIcon> cache = new HashMap<>();

    public static ImageIcon getIcono(String nombre, int ancho, int alto) {
        String key = nombre + "_" + ancho + "x" + alto;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        try {
            InputStream is = IconoUtil.class.getResourceAsStream("/recursos/iconos/" + nombre);
            if (is != null) {
                BufferedImage original = ImageIO.read(is);
                if (original != null) {
                    BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = escalada.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.drawImage(original, 0, 0, ancho, alto, null);
                    g2.dispose();

                    ImageIcon icon = new ImageIcon(escalada);
                    cache.put(key, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}

        // Fallback: icono vacío transparente
        BufferedImage empty = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        ImageIcon icon = new ImageIcon(empty);
        cache.put(key, icon);
        return icon;
    }
}

