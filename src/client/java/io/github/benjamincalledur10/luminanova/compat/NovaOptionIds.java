package io.github.benjamincalledur10.luminanova.compat;

import java.util.Locale;
import net.minecraft.resources.Identifier;

/** Translation keys may contain capitals; resource identifiers must never do so. */
public final class NovaOptionIds {
    private NovaOptionIds() { }

    public static Identifier of(String translationKey) {
        return Identifier.fromNamespaceAndPath("luminanova", translationKey.toLowerCase(Locale.ROOT));
    }
}
