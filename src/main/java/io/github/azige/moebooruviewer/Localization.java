package io.github.azige.moebooruviewer;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class Localization {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io/github/azige/moebooruviewer/Messages");

    public static ResourceBundle getBundle() {
        return BUNDLE;
    }

    public static String getString(String key) {
        return BUNDLE.getString(key);
    }

    public static String format(String key, Object... keys) {
        return MessageFormat.format(getString(key), keys);
    }
}
