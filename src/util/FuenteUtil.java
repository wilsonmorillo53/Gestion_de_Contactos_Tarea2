package util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public class FuenteUtil {

    private static Font baseFont = null;

    static {
        try {
            InputStream is = FuenteUtil.class.getResourceAsStream("/recursos/fuentes/Segoe UI.ttf");
            if (is == null) {
                is = FuenteUtil.class.getResourceAsStream("/recursos/fuentes/Segoe.ttf");
            }
            if (is != null) {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
            }
        } catch (Exception ignored) {}
    }

    public static Font cargarFuente(float tamaño) {
        return cargarFuente(Font.PLAIN, tamaño);
    }

    public static Font cargarFuenteBold(float tamaño) {
        return cargarFuente(Font.BOLD, tamaño);
    }

    public static Font cargarFuente(int estilo, float tamaño) {
        return new Font("Segoe UI", estilo, (int) tamaño);
    }
}
