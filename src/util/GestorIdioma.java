package util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;


public class GestorIdioma {

    public interface IdiomaListener {
        void idiomaCambiado(String nuevoIdioma);
    }

    private static final String BUNDLE_BASE_NAME = "recursos.idiomas.Bundle";
    private static final List<IdiomaListener> listeners = new ArrayList<>();

    private static final Map<String, Locale> MAPA_LOCALES = new LinkedHashMap<>();
    static {
        MAPA_LOCALES.put("Español", Locale.forLanguageTag("es-ES"));
        MAPA_LOCALES.put("English", Locale.forLanguageTag("en-US"));
        MAPA_LOCALES.put("Français", Locale.forLanguageTag("fr-FR"));
    }

    private static String idiomaActual = "Español";
    private static Locale localeActual = MAPA_LOCALES.get("Español");
    private static ResourceBundle bundle;

    static {
        cargarBundle();
    }

    private static void cargarBundle() {
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, localeActual, new ResourceBundle.Control() {
                @Override
                public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                                ClassLoader loader, boolean reload)
                        throws IllegalAccessException, InstantiationException, java.io.IOException {
                    String bundleName = toBundleName(baseName, locale);
                    String resourceName = toResourceName(bundleName, "properties");
                    try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                        if (stream != null) {
                            return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        }
                    }
                    return super.newBundle(baseName, locale, format, loader, reload);
                }
            });
        } catch (Exception e) {
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, localeActual);
            } catch (Exception ex) {
                bundle = null;
            }
        }
    }

    public static String getIdiomaActual() {
        return idiomaActual;
    }

    public static Locale getLocaleActual() {
        return localeActual;
    }

    public static String[] getIdiomasDisponibles() {
        return MAPA_LOCALES.keySet().toArray(new String[0]);
    }

    public static void setIdioma(String nuevoIdioma) {
        if (MAPA_LOCALES.containsKey(nuevoIdioma) && !nuevoIdioma.equals(idiomaActual)) {
            idiomaActual = nuevoIdioma;
            localeActual = MAPA_LOCALES.get(nuevoIdioma);
            Locale.setDefault(localeActual);
            cargarBundle();
            notificarListeners();
        }
    }

    public static void setLocale(Locale nuevoLocale) {
        for (Map.Entry<String, Locale> entry : MAPA_LOCALES.entrySet()) {
            if (entry.getValue().getLanguage().equalsIgnoreCase(nuevoLocale.getLanguage())) {
                setIdioma(entry.getKey());
                return;
            }
        }
        localeActual = nuevoLocale;
        cargarBundle();
        notificarListeners();
    }

    public static String get(String clave) {
        if (bundle != null) {
            try {
                return bundle.getString(clave);
            } catch (MissingResourceException ignored) {}
        }
        return clave;
    }

    public static String get(String clave, Object... args) {
        String patron = get(clave);
        return MessageFormat.format(patron, args);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static void agregarListener(IdiomaListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removerListener(IdiomaListener listener) {
        listeners.remove(listener);
    }

    private static void notificarListeners() {
        for (IdiomaListener listener : listeners) {
            try {
                listener.idiomaCambiado(idiomaActual);
            } catch (Exception ignored) {}
        }
    }
}
