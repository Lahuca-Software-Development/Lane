package com.lahuca.lane;

import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * A class that loads all the language files into the global translator.
 */
public class MessagesResourceBundle {

    private final File messagesFolder;
    private final String prefix;
    private final Locale defaultLocale;
    private final String defaultResource;

    /**
     * Constructs a new MessagesResourceBundle.
     * This can be used to set up language files into the global translator.
     * @param messagesFolder the folder where the language files should be stored
     * @param prefix the prefix of the language files
     * @param defaultLocale the default locale to place the default resource at
     * @param defaultResource the default resource to write, it should be in the resources folder
     */
    public MessagesResourceBundle(File messagesFolder, String prefix, Locale defaultLocale, String defaultResource) {
        Objects.requireNonNull(messagesFolder, "messagesFolder cannot be null");
        Objects.requireNonNull(prefix, "prefix cannot be null");
        Objects.requireNonNull(defaultLocale, "defaultLocale cannot be null");
        Objects.requireNonNull(defaultResource, "defaultResource cannot be null");
        this.messagesFolder = messagesFolder;
        this.prefix = prefix;
        this.defaultLocale = defaultLocale;
        this.defaultResource = defaultResource;
    }

    /**
     * Initializes the resource bundles
     * If the default resource bundle is not present, loads the default from the bundled JAR at the default resource location.
     * @return {@code true} if the default resource bundle is now present, {@code false} otherwise
     */
    public boolean initialize() {
        // Resource Bundles
        File defaultResourceBundle = new File(messagesFolder, prefix + "_" + defaultLocale.toLanguageTag() + ".properties");
        if(!defaultResourceBundle.exists()) {
            // File does not exist
            if(!defaultResourceBundle.getParentFile().exists()) {
                // Parent folder does not exist, create it.
                if(!defaultResourceBundle.getParentFile().mkdirs()) {
                    // We could not make parent folder, stop
                    return false;
                }
            }
            try {
                if(!defaultResourceBundle.createNewFile()) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
            // Create default
            boolean done = false;
            try (InputStream inputStream = getClass().getResourceAsStream("/" + defaultResource)) {
                if (inputStream != null) {
                    Files.copy(inputStream, defaultResourceBundle.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    done = true;
                }
            } catch (IOException ignored) {
                return false;
            }
            if(!done) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loads the resource bundles into the global translator.
     * @param store the store to load the resource bundles into
     * @throws IOException if an I/O error occurs
     */
    public void loadResourceBundles(TranslationStore.StringBased<?> store) throws IOException {
        File[] files = messagesFolder.listFiles((dir, name) -> name.endsWith(".properties"));
        if (files == null) return;

        for (File file : files) {
            Locale locale = parseLocale(file.getName());

            try (InputStream in = new FileInputStream(file);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                ResourceBundle bundle = new PropertyResourceBundle(reader);
                store.registerAll(locale, bundle, true);
            }
        }
        GlobalTranslator.translator().addSource(store);
    }

    /**
     * Parses the locale from the filename
     * @param filename the filename
     * @return the locale
     */
    private static Locale parseLocale(String filename) {
        // messages_en_US_v.properties → en_US_v
        String base = filename.replace(".properties", "");

        // Remove prefix like "messages" (everything before first underscore)
        int underscoreIndex = base.indexOf('_');
        if (underscoreIndex == -1) return Locale.ROOT;

        String localePart = base.substring(underscoreIndex + 1); // get just "en", "en_US", etc.
        String[] parts = localePart.split("_");

        return switch (parts.length) {
            case 1 -> Locale.of(parts[0]); // en
            case 2 -> Locale.of(parts[0], parts[1]); // en_US
            case 3 -> Locale.of(parts[0], parts[1], parts[2]); // en_US_v
            default -> Locale.ROOT;
        };
    }


}
