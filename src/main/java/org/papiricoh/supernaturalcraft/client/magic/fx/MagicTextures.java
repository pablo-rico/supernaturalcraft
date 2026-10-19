package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.MagicAssets;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The magic's effect textures ({@link MagicAssets}) with their fallbacks in one place: until the art lands (or if a file is
 * missing) each one falls back to an older white-on-transparent texture of the mod, so nothing ever draws the missing-texture
 * checkerboard. Lookups are cached and forgotten on a resource reload ({@link #reload}).
 */
public final class MagicTextures {

    /** Fallback for the cast circle and every seal: the ward's sigil circle. */
    public static final ResourceLocation WARD_CIRCLE = SupernaturalCraft.asResource("textures/effect/ward_circle.png");
    /** Fallback for the glyph strip: the soft energy band. */
    public static final ResourceLocation ENERGY_RING = SupernaturalCraft.asResource("textures/effect/energy_ring.png");
    /** The beam's texture (rings and shockwaves along the ground). */
    public static final ResourceLocation BEAM = SupernaturalCraft.asResource("textures/effect/beam.png");

    private static final Map<String, ResourceLocation> RESOLVED = new HashMap<>();
    private static final Map<String, Boolean> PRESENT = new HashMap<>();
    private static final Map<ResourceLocation, Float> ASPECT = new HashMap<>();

    private MagicTextures() {
    }

    /** Forgets every lookup (resource packs reloaded, the art regenerated). */
    public static void reload() {
        RESOLVED.clear();
        PRESENT.clear();
        ASPECT.clear();
    }

    public static ResourceLocation castCircle() {
        return pick(MagicAssets.CAST_CIRCLE, WARD_CIRCLE);
    }

    public static ResourceLocation glyphStrip() {
        return pick(MagicAssets.GLYPH_STRIP, ENERGY_RING);
    }

    /** The bolt's core, or null to draw a procedural star instead. */
    public static @Nullable ResourceLocation boltCore() {
        return exists(MagicAssets.BOLT_CORE) ? SupernaturalCraft.asResource(MagicAssets.BOLT_CORE) : null;
    }

    /** A bowl spell's seal by its {@link MagicAssets#SEALS} index (the cast circle when none or missing). */
    public static ResourceLocation seal(int index) {
        if (index < 0 || index >= MagicAssets.SEALS.size()) return castCircle();
        return pick(MagicAssets.seal(MagicAssets.SEALS.get(index)), castCircle());
    }

    /** Width over height of a texture (read once from its PNG header; 1 if unreadable). */
    public static float aspect(ResourceLocation texture) {
        return ASPECT.computeIfAbsent(texture, MagicTextures::readAspect);
    }

    private static ResourceLocation pick(String path, ResourceLocation fallback) {
        return RESOLVED.computeIfAbsent(path, p -> exists(p) ? SupernaturalCraft.asResource(p) : fallback);
    }

    private static boolean exists(String path) {
        return PRESENT.computeIfAbsent(path, p -> {
            try {
                return Minecraft.getInstance().getResourceManager().getResource(SupernaturalCraft.asResource(p)).isPresent();
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    private static float readAspect(ResourceLocation texture) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (res.isEmpty()) return 1f;
            try (InputStream in = res.get().open()) {
                byte[] head = in.readNBytes(24);
                if (head.length < 24) return 1f;
                int w = ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16) | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF);
                int h = ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16) | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF);
                return w > 0 && h > 0 ? w / (float) h : 1f;
            }
        } catch (Exception e) {
            return 1f;
        }
    }
}
