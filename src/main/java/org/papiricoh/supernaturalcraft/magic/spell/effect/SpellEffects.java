package org.papiricoh.supernaturalcraft.magic.spell.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehavior;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehaviors;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** The eight v1 effects. Numbers come from the sigil's params; defaults here mirror the shipped JSON. */
public final class SpellEffects {

    private SpellEffects() {
    }

    private interface Harm extends SpellBehavior.Effect {
        @Override
        default boolean beneficial() {
            return false;
        }
    }

    private static boolean unholy(Entity e) {
        return e.getType().is(AllTags.Entities.DEMONS) || e.getType().is(EntityTypeTags.UNDEAD);
    }

    private static void burst(SpellContext ctx, Entity target, net.minecraft.core.particles.ParticleOptions particle, int count) {
        ctx.level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                count, target.getBbWidth() / 2, target.getBbHeight() / 3, target.getBbWidth() / 2, 0.04);
    }

    public static final SpellBehavior.Effect SMITE = (Harm) (ctx, sigil, target) -> {
        float dmg = sigil.param("damage", 6f) * ctx.potency;
        if (unholy(target)) dmg *= sigil.param("vs_unholy", 2f);
        boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), dmg);
        burst(ctx, target, AllParticles.GRACE.get(), 10);
        return hit;
    };

    public static final SpellBehavior.Effect HELLFIRE = new Harm() {
        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.HELLFIRE, ctx.caster),
                    sigil.param("damage", 4f) * ctx.potency);
            target.igniteForSeconds(sigil.param("burn_seconds", 4f) * ctx.durationScale);
            burst(ctx, target, AllParticles.HELLFIRE.get(), 14);
            return hit;
        }

        @Override
        public boolean applyToBlock(SpellContext ctx, SigilComponent sigil, BlockPos pos, Direction face) {
            BlockPos at = pos.relative(face);
            ctx.level.sendParticles(AllParticles.HELLFIRE.get(), at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.03);
            return false;
        }
    };

    public static final SpellBehavior.Effect FROST = new Harm() {
        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SPELL, ctx.caster),
                    sigil.param("damage", 2f) * ctx.potency);
            if (target instanceof LivingEntity living) {
                int ticks = ctx.duration(Math.round(sigil.param("duration", 100f)));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), ctx.caster);
                living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + ticks / 2));
            }
            burst(ctx, target, AllParticles.FROST.get(), 14);
            return hit;
        }

        /** Freezes still water into ice that melts again on its own. */
        @Override
        public boolean applyToBlock(SpellContext ctx, SigilComponent sigil, BlockPos pos, Direction face) {
            int r = Math.max(1, Math.round(1 + ctx.area));
            boolean any = false;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
                if (ctx.level.getBlockState(p).getBlock() instanceof LiquidBlock
                        && ctx.level.getFluidState(p).is(Fluids.WATER) && ctx.level.getFluidState(p).isSource()
                        && ctx.level.getBlockState(p.above()).isAir()) {
                    ctx.level.setBlockAndUpdate(p, Blocks.FROSTED_ICE.defaultBlockState().setValue(FrostedIceBlock.AGE, 0));
                    ctx.level.scheduleTick(p, Blocks.FROSTED_ICE, 60 + ctx.level.random.nextInt(60));
                    any = true;
                }
            }
            return any;
        }
    };

    /** Unbearable to demons; a trapped demon is cast out entirely, leaving a hellfire ember behind. */
    public static final SpellBehavior.Effect EXORCISE = (Harm) (ctx, sigil, target) -> {
        if (target instanceof SpellHooks.Exorcisable ex) {
            return ex.onExorcised(ctx.potency);
        }
        if (!target.getType().is(AllTags.Entities.DEMONS) || !(target instanceof LivingEntity demon)) return false;
        burst(ctx, target, AllParticles.DEMON_SMOKE.get(), 30);
        if (demon.hasEffect(AllMobEffects.TRAPPED)) {
            ctx.level.sendParticles(ParticleTypes.LARGE_SMOKE, demon.getX(), demon.getEyeY(), demon.getZ(), 40, 0.2, 1.2, 0.2, 0.05);
            ctx.level.playSound(null, demon.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
            ItemEntity ember = new ItemEntity(ctx.level, demon.getX(), demon.getY() + 0.5, demon.getZ(),
                    new ItemStack(AllItems.HELLFIRE_EMBER.get()));
            ctx.level.addFreshEntity(ember);
            demon.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), Float.MAX_VALUE);
            return true;
        }
        return demon.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), sigil.param("damage", 10f) * ctx.potency);
    };

    public static final SpellBehavior.Effect BIND = (Harm) (ctx, sigil, target) -> {
        int ticks = ctx.duration(Math.round(sigil.param("duration", 80f)));
        if (target instanceof SpellHooks.Bindable b) {
            return b.onBound(ticks);
        }
        if (!(target instanceof LivingEntity living)) return false;
        if (living.getType().is(AllTags.Entities.DEMONS)) {
            living.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, ticks), ctx.caster);
        } else {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks / 2, 4), ctx.caster);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks / 2, 1), ctx.caster);
        }
        burst(ctx, target, AllParticles.SIGIL.get(), 8);
        return true;
    };

    public static final SpellBehavior.Effect MEND = new SpellBehavior.Effect() {
        @Override
        public boolean beneficial() {
            return true;
        }

        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            if (!(target instanceof LivingEntity living)) return false;
            living.heal(sigil.param("heal", 4f) * ctx.potency);
            living.clearFire();
            burst(ctx, target, ParticleTypes.HAPPY_VILLAGER, 8);
            burst(ctx, target, AllParticles.GRACE.get(), 6);
            return true;
        }
    };

    public static final SpellBehavior.Effect REPEL = (Harm) (ctx, sigil, target) -> {
        Vec3 away = target.position().subtract(ctx.caster.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = ctx.caster.getLookAngle().multiply(1, 0, 1);
        away = away.normalize().scale(sigil.param("strength", 1.4f) * ctx.potency);
        target.push(away.x, sigil.param("lift", 0.35f), away.z);
        target.hurtMarked = true;
        burst(ctx, target, ParticleTypes.CLOUD, 6);
        return true;
    };

    /** Lights up the unseen: strips invisibility, outlines the target, unmasks illusions. */
    public static final SpellBehavior.Effect REVEAL = (Harm) (ctx, sigil, target) -> {
        if (target instanceof SpellHooks.Revealable r) {
            r.onRevealed();
        }
        if (!(target instanceof LivingEntity living)) return false;
        living.removeEffect(MobEffects.INVISIBILITY);
        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, ctx.duration(Math.round(sigil.param("duration", 200f)))), ctx.caster);
        burst(ctx, target, AllParticles.GRACE.get(), 6);
        return true;
    };

    public static void registerAll() {
        SpellBehaviors.register(SupernaturalCraft.asResource("smite"), SMITE);
        SpellBehaviors.register(SupernaturalCraft.asResource("hellfire"), HELLFIRE);
        SpellBehaviors.register(SupernaturalCraft.asResource("frost"), FROST);
        SpellBehaviors.register(SupernaturalCraft.asResource("exorcise"), EXORCISE);
        SpellBehaviors.register(SupernaturalCraft.asResource("bind"), BIND);
        SpellBehaviors.register(SupernaturalCraft.asResource("mend"), MEND);
        SpellBehaviors.register(SupernaturalCraft.asResource("repel"), REPEL);
        SpellBehaviors.register(SupernaturalCraft.asResource("reveal"), REVEAL);
    }
}
