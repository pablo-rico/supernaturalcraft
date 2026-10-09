package org.papiricoh.supernaturalcraft.magic.spell;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.gen.GeneratedFormula;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Where a sigil id is looked up (v0.17): the {@code sigil} datapack registry first, then the hunter's Men of Letters archive,
 * whose generated formulas have virtual ids ({@code supernaturalcraft:formula/<n>}, {@link GeneratedFormula}). Everything that
 * resolves a sigil for a player goes through here; on the server pass {@code Legacies.archive(player)}, on the client
 * {@code ClientLegacy.archive()}, or null where no hunter is involved.
 */
public final class SigilLookup {

    /** The local hunter's archive on the client (set by {@code ClientLegacy}); null on a dedicated server. */
    private static Supplier<Archive> local = () -> null;

    private SigilLookup() {
    }

    /** Called once by the client: where tooltips and the book find the reader's formulas. */
    public static void setLocal(Supplier<Archive> archive) {
        local = archive;
    }

    /** The client reader's archive (for names and glyphs in tooltips), or null. */
    public static @Nullable Archive local() {
        return local.get();
    }

    /** A sigil's display name: a generated formula's Latin name, else the lang entry. */
    public static MutableComponent name(@Nullable Archive archive, ResourceLocation id) {
        GeneratedFormula f = formula(archive, id);
        return f != null ? Component.literal(f.name()) : Component.translatable(SigilComponent.translationKey(id));
    }

    /** A sigil's description: for a formula, a line naming the sigil it was worked from. */
    public static MutableComponent description(@Nullable Archive archive, ResourceLocation id) {
        GeneratedFormula f = formula(archive, id);
        return f != null ? Component.translatable("sigil.supernaturalcraft.formula.desc", name(archive, f.base()))
                : Component.translatable(SigilComponent.translationKey(id) + ".desc");
    }

    /** Where a sigil is found: for a formula, the hunter's own research. */
    public static MutableComponent source(@Nullable Archive archive, ResourceLocation id) {
        return formula(archive, id) != null ? Component.translatable("sigil.supernaturalcraft.formula.source")
                : Component.translatable(SigilComponent.translationKey(id) + ".source");
    }

    /** The registered sigil a formula was worked from, or {@code id} itself (its glyph, its JEI page). */
    public static ResourceLocation glyphOf(@Nullable Archive archive, ResourceLocation id) {
        GeneratedFormula f = formula(archive, id);
        return f != null ? f.base() : id;
    }

    public static @Nullable SigilComponent get(RegistryAccess access, @Nullable Archive archive, ResourceLocation id) {
        SigilComponent s = access.registry(SNRegistries.SIGIL).map(r -> r.get(id)).orElse(null);
        if (s != null || archive == null) return s;
        int index = GeneratedFormula.indexOf(id);
        if (index < 0) return null;
        GeneratedFormula f = archive.formula(index);
        return f == null ? null : f.sigil();
    }

    /** @return the generated formula behind {@code id}, or null (a registered sigil, or unknown) */
    public static @Nullable GeneratedFormula formula(@Nullable Archive archive, ResourceLocation id) {
        int index = GeneratedFormula.indexOf(id);
        return archive == null || index < 0 ? null : archive.formula(index);
    }

    /** Every sigil the hunter could know: the registry's, then their generated formulas, in order. */
    public static Map<ResourceLocation, SigilComponent> all(RegistryAccess access, @Nullable Archive archive) {
        Map<ResourceLocation, SigilComponent> out = new LinkedHashMap<>();
        access.registry(SNRegistries.SIGIL).ifPresent((Registry<SigilComponent> r) -> r.entrySet()
                .forEach(e -> out.put(e.getKey().location(), e.getValue())));
        if (archive != null) for (GeneratedFormula f : archive.formulas()) out.put(f.id(), f.sigil());
        return out;
    }
}
