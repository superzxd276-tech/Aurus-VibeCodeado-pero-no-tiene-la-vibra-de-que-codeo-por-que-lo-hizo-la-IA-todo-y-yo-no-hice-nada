package com.fendrixx.aurus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Matriz de caracteres ultra-rápida (Acceso O(1)) en lugar de un pesado Map
    private static final String[] LEGACY_MAP = new String[256];

    static {
        LEGACY_MAP['0'] = "<black>"; LEGACY_MAP['1'] = "<dark_blue>";
        LEGACY_MAP['2'] = "<dark_green>"; LEGACY_MAP['3'] = "<dark_aqua>";
        LEGACY_MAP['4'] = "<dark_red>"; LEGACY_MAP['5'] = "<dark_purple>";
        LEGACY_MAP['6'] = "<gold>"; LEGACY_MAP['7'] = "<gray>";
        LEGACY_MAP['8'] = "<dark_gray>"; LEGACY_MAP['9'] = "<blue>";
        LEGACY_MAP['a'] = "<green>"; LEGACY_MAP['b'] = "<aqua>";
        LEGACY_MAP['c'] = "<red>"; LEGACY_MAP['d'] = "<light_purple>";
        LEGACY_MAP['e'] = "<yellow>"; LEGACY_MAP['f'] = "<white>";
        LEGACY_MAP['r'] = "<reset>"; LEGACY_MAP['l'] = "<bold>";
        LEGACY_MAP['o'] = "<italic>"; LEGACY_MAP['n'] = "<underlined>";
        LEGACY_MAP['m'] = "<strikethrough>"; LEGACY_MAP['k'] = "<obfuscated>";
    }

    /**
     * Bucle de análisis de una sola pasada. (Zero Regex allocation).
     * Convierte TODO (&a, §a, &#ff0000) a MiniMessage sin usar memoria innecesaria.
     */
    private static String convertToMiniMessage(@NotNull String text) {
        if (text.isEmpty()) return text;

        StringBuilder sb = new StringBuilder(text.length() + 32);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '§' || c == '&') && i + 1 < text.length()) {
                // 1. Detección de Hexadecimales Legacy (Ej: &#FF0000 o &x&f&f...)
                if (c == '&' && text.charAt(i + 1) == '#' && i + 7 < text.length()) {
                    boolean isHex = true;
                    for (int j = 2; j <= 7; j++) {
                        char hc = text.charAt(i + j);
                        if (!((hc >= '0' && hc <= '9') || (hc >= 'a' && hc <= 'f') || (hc >= 'A' && hc <= 'F'))) {
                            isHex = false; break;
                        }
                    }
                    if (isHex) {
                        sb.append("<#").append(text, i + 2, i + 8).append(">");
                        i += 7; // Saltamos el código entero
                        continue;
                    }
                }

                // 2. Detección de color normal (Ej: &a, §b, &l)
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code < 256 && LEGACY_MAP[code] != null) {
                    sb.append(LEGACY_MAP[code]);
                    i++; // Saltamos la letra del código
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * IMPORTANTE: Ahora devuelve 'Component', que es lo que Paper 1.20+ nativamente usa
     * en vez de Strings con "§". Esto es perfecto para Inventory Titles, Messages, etc.
     */
    @NotNull
    public static Component format(@NotNull String message) {
        if (message.isEmpty()) return Component.empty();

        String processed = convertToMiniMessage(message);

        // Deserializa usando MiniMessage y remueve el Italic asqueroso por defecto de Minecraft
        return MINI_MESSAGE.deserialize(processed)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Usar SOLO si alguna librería de terceros extremadamente vieja (Spigot)
     * te obliga por fuerza bruta a pasarle un String, de lo contrario, usa siempre format()
     */
    @NotNull
    public static String formatToString(@NotNull String message) {
        return LegacyComponentSerializer.legacySection().serialize(format(message));
    }
}
