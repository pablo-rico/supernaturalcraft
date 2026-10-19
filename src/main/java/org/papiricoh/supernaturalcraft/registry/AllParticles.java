package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.fx.MagicParticleOptions;

public class AllParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, SupernaturalCraft.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HELLFIRE = register("hellfire");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GRACE = register("grace");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SIGIL = register("sigil");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEMON_SMOKE = register("demon_smoke");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_MOTE = register("void_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST = register("frost");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH = register("ash");
    /** Azazel's smoke: sulphur-yellow where the demons' is black. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> YELLOW_SMOKE = register("yellow_smoke");
    /** Lilith's light: white, cold, everywhere once her vessel splits. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHITE_LIGHT = register("white_light");
    /** Metatron's ink: black, edged with gold. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INK = register("ink");
    /** A loose page, fluttering. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PAGE = register("page");
    /** A typed letter, rising off the page as the Author writes or unwrites an arena. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INK_LETTER = register("ink_letter");
    /** A scrap of torn manuscript. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PAGE_SCRAP = register("page_scrap");
    /** A golden mote of the Author's light. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GOLDEN_MOTE = register("golden_mote");

    /** A bowl spell's smoke, in the spell's own colour. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> BOWL_SMOKE =
            PARTICLE_TYPES.register("bowl_smoke", () -> new ParticleType<net.minecraft.core.particles.ColorParticleOption>(false) {
                @Override
                public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec() {
                    return net.minecraft.core.particles.ColorParticleOption.codec(this);
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, net.minecraft.core.particles.ColorParticleOption> streamCodec() {
                    return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);
                }
            });

    /** Pestilence's flies, buzzing round a swarm. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLY = register("fly");
    /** A spore of plague, drifting off his clouds and his trail. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLAGUE_SPORE = register("plague_spore");
    /** A soul on its way out: Famine's feeding, Death's reapers, the light out of limbo. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL_WISP = register("soul_wisp");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }

    /** One of Michael's steel feathers, spinning down. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEEL_FEATHER = register("steel_feather");
    /** A ray off his halo, his lance, the Throne Room's light. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HALO_RAY = register("halo_ray");

    // --- v0.21: the magic's coloured particles (ids in MagicAssets.PARTICLES; options MagicParticleOptions) ---------------
    public static final java.util.Map<String, DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>>> MAGIC =
            registerMagic(org.papiricoh.supernaturalcraft.magic.MagicAssets.PARTICLES);
    /** A bright point: a bolt's sparks, a cast's flash. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>> MAGIC_SPARK = MAGIC.get("magic_spark");
    /** A tumbling glyph off a cast circle or lit chalk. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>> MAGIC_GLYPH = MAGIC.get("magic_glyph");
    /** An ember rising off infernal work. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>> RUNE_EMBER = MAGIC.get("rune_ember");
    /** A slow, drifting mote of witchcraft. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>> WITCH_MOTE = MAGIC.get("witch_mote");
    /** A falling grain of salt. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>> SALT_GRAIN = MAGIC.get("salt_grain");

    private static java.util.Map<String, DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>>> registerMagic(
            java.util.List<String> ids) {
        java.util.Map<String, DeferredHolder<ParticleType<?>, ParticleType<MagicParticleOptions>>> out = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            out.put(id, PARTICLE_TYPES.register(id, () -> new ParticleType<MagicParticleOptions>(false) {
                @Override
                public com.mojang.serialization.MapCodec<MagicParticleOptions> codec() {
                    return MagicParticleOptions.codec(this);
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, MagicParticleOptions> streamCodec() {
                    return MagicParticleOptions.streamCodec(this);
                }
            }));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    public static void init() {
    }
}
