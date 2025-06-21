package com.lahuca.lanecontrollervelocity.translation;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator;
import net.kyori.adventure.translation.AbstractTranslationStore;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

public class FallbackTranslationStoreImpl extends AbstractTranslationStore.StringBased<String> implements FallbackTranslationStore {

    private final FallbackTranslationStoreImpl.Translator translator;

    FallbackTranslationStoreImpl(final @NotNull Key name, final @NotNull MiniMessage miniMessage) {
        super(name);
        this.translator = new FallbackTranslationStoreImpl.Translator(Objects.requireNonNull(miniMessage, "miniMessage"));
    }

    @Override
    protected @NotNull String parse(final @NotNull String string, final @NotNull Locale locale) {
        return string;
    }

    @Override
    public @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale locale) {
        return null;
    }

    @Override
    public @Nullable Component translate(final @NotNull TranslatableComponent component, final @NotNull Locale locale) {
        return this.translator.translate(component, locale);
    }

    private final class Translator extends MiniMessageTranslator {

        private Translator(final @NotNull MiniMessage miniMessage) {
            super(miniMessage);
        }

        @Override
        protected @Nullable String getMiniMessageString(final @NotNull String key, final @NotNull Locale locale) {
            return FallbackTranslationStoreImpl.this.translationValue(key, locale);
        }

        @Override
        public @NotNull Key name() {
            return FallbackTranslationStoreImpl.this.name();
        }

        @Override
        public @NotNull TriState hasAnyTranslations() {
            return FallbackTranslationStoreImpl.this.hasAnyTranslations();
        }
    }

}
