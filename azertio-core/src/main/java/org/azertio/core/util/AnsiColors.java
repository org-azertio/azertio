package org.azertio.core.util;

public final class AnsiColors {

    public static final String RESET  = "[0m";
    public static final String BOLD   = "[1m";
    public static final String GREEN  = "[32m";
    public static final String RED    = "[31m";
    public static final String YELLOW = "[33m";

    private static final boolean ENABLED =
        System.getenv("NO_COLOR") == null &&
        !"dumb".equals(System.getenv("TERM")) &&
        System.console() != null;

    private AnsiColors() {}

    public static String color(String text, String ansiCode) {
        return ENABLED ? ansiCode + text + RESET : text;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }
}