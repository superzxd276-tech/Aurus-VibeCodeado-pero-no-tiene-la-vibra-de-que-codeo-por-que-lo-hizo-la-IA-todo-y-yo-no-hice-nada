package com.fendrixx.aurus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .character('§')
            .build();

    @NotNull
    public static String format(@NotNull String message) {
        if (message.isEmpty()) return message;

        Component component = MINI_MESSAGE.deserialize(message)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);

        return SERIALIZER.serialize(component);
    }

    public static List<String> formatList(List<String> list) {
        if (list == null) return List.of();
        return list.stream().map(ColorUtils::format).collect(Collectors.toList());
    }

    @NotNull
    public static Component parse(@NotNull String message) {
        return MINI_MESSAGE.deserialize(message)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}