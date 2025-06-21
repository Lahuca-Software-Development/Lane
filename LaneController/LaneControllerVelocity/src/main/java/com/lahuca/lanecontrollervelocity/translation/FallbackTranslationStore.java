package com.lahuca.lanecontrollervelocity.translation;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.TranslationStore;
import org.jetbrains.annotations.NotNull;

public interface FallbackTranslationStore extends TranslationStore.StringBased<String> {

    static @NotNull FallbackTranslationStore create(final @NotNull Key name) {
        return create(name, MiniMessage.miniMessage());
    }

    static @NotNull FallbackTranslationStore create(final @NotNull Key name, final @NotNull MiniMessage miniMessage) {
        return new FallbackTranslationStoreImpl(name, miniMessage);
    }

}
