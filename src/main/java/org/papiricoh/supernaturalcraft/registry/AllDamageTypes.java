package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Damage types are a datapack registry; these keys are bootstrapped by datagen. */
public class AllDamageTypes {

    public static final ResourceKey<DamageType> SMITE = key("smite");
    public static final ResourceKey<DamageType> HELLFIRE = key("hellfire");
    public static final ResourceKey<DamageType> HOLY_WATER = key("holy_water");
    public static final ResourceKey<DamageType> GRACE = key("grace");
    public static final ResourceKey<DamageType> SPELL = key("spell");
    public static final ResourceKey<DamageType> ARENA_BARRIER = key("arena_barrier");
    /** The Darkness itself: her strikes, and what her void does to those it holds. */
    public static final ResourceKey<DamageType> VOID = key("void");
    /** The Broken Chorus's gaze and light, and the hand that catches those who fall from its summit. */
    public static final ResourceKey<DamageType> JUDGMENT = key("judgment");
    /** The Hymn of Unmaking: sound that goes straight through armour. */
    public static final ResourceKey<DamageType> HYMN = key("hymn");
    /** A consecrated round from the Colt. */
    public static final ResourceKey<DamageType> COLT = key("colt");
    /** Lilith's white light: it burns out whoever it falls on. */
    public static final ResourceKey<DamageType> WHITE_LIGHT = key("white_light");
    /** The Author's snap: unwritten from the page. */
    public static final ResourceKey<DamageType> ERASED = key("erased");
    /** The Author's ink: keys, lines, words, echoes. */
    public static final ResourceKey<DamageType> INK = key("ink");
    /** A rule he rewrote: water that burns, light that hurts, the floor that is lava. */
    public static final ResourceKey<DamageType> REWRITTEN = key("rewritten");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, SupernaturalCraft.asResource(name));
    }

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(SMITE, new DamageType("supernaturalcraft.smite", DamageScaling.NEVER, 0.1f));
        ctx.register(HELLFIRE, new DamageType("supernaturalcraft.hellfire", DamageScaling.NEVER, 0.1f));
        ctx.register(HOLY_WATER, new DamageType("supernaturalcraft.holy_water", DamageScaling.NEVER, 0.0f));
        ctx.register(GRACE, new DamageType("supernaturalcraft.grace", DamageScaling.ALWAYS, 0.1f));
        ctx.register(SPELL, new DamageType("supernaturalcraft.spell", DamageScaling.NEVER, 0.0f));
        ctx.register(ARENA_BARRIER, new DamageType("supernaturalcraft.arena_barrier", DamageScaling.NEVER, 0.0f));
        ctx.register(VOID, new DamageType("supernaturalcraft.void", DamageScaling.NEVER, 0.1f));
        ctx.register(JUDGMENT, new DamageType("supernaturalcraft.judgment", DamageScaling.NEVER, 0.1f));
        ctx.register(HYMN, new DamageType("supernaturalcraft.hymn", DamageScaling.NEVER, 0.0f));
        ctx.register(COLT, new DamageType("supernaturalcraft.colt", DamageScaling.NEVER, 0.1f));
        ctx.register(WHITE_LIGHT, new DamageType("supernaturalcraft.white_light", DamageScaling.NEVER, 0.1f));
        ctx.register(ERASED, new DamageType("supernaturalcraft.erased", DamageScaling.NEVER, 0.0f));
        ctx.register(INK, new DamageType("supernaturalcraft.ink", DamageScaling.NEVER, 0.1f));
        ctx.register(REWRITTEN, new DamageType("supernaturalcraft.rewritten", DamageScaling.NEVER, 0.0f));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity direct, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),
                direct, attacker);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity attacker) {
        return source(level, key, attacker, attacker);
    }
}
