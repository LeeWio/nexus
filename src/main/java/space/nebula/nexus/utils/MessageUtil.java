package space.nebula.nexus.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility class for resolving localized messages.
 */
@Component
@RequiredArgsConstructor
public class MessageUtil {

    private final MessageSource messageSource;

    /**
     * Get a localized message for the given key.
     */
    public String get(String key) {
        return get(key, (Object[]) null);
    }

    /**
     * Get a localized message for the given key and arguments.
     */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get a localized message using a code as a key.
     */
    public String get(int code) {
        return get(String.valueOf(code));
    }

    /**
     * Get the current locale.
     */
    public Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}
