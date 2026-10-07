package org.papiricoh.supernaturalcraft.ritual;

import java.util.List;
import java.util.Locale;

/** Whether a book's words hold a name. Pure: letters only, any case, across page breaks. */
public final class WrittenName {

    private WrittenName() {
    }

    public static String normalise(String text) {
        StringBuilder b = new StringBuilder(text.length());
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) if (Character.isLetter(c)) b.append(c);
        return b.toString();
    }

    public static boolean holds(List<String> pages, String title, String name) {
        String wanted = normalise(name);
        if (wanted.isEmpty()) return false;
        if (title != null && normalise(title).contains(wanted)) return true;
        return normalise(String.join("", pages)).contains(wanted);
    }
}
