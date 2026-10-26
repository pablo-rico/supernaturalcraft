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

/** The sigils' effects. Numbers come from the sigil's params; defaults here mirror the shipped JSON. */
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
        return org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(e) || e.getType().is(EntityTypeTags.UNDEAD);
    }

    public static final SpellBehavior.Effect SMITE = (Harm) (ctx, sigil, target) -> {
        float dmg = org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 6f) * ctx.potency);
        if (unholy(target)) dmg *= sigil.param("vs_unholy", 2f);
        boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), vsBoss(ctx, sigil, target, dmg));
        return hit;
    };

    public static final SpellBehavior.Effect HELLFIRE = new Harm() {
        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.HELLFIRE, ctx.caster),
                    vsBoss(ctx, sigil, target, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 4f) * ctx.potency)));
            target.igniteForSeconds(sigil.param("burn_seconds", 4f) * ctx.durationScale);
            return hit;
        }

        @Override
        public boolean applyToBlock(SpellContext ctx, SigilComponent sigil, BlockPos pos, Direction face) {
            // The client draws the generic burst (v0.21): nothing here takes hold.
            return false;
        }
    };

    public static final SpellBehavior.Effect FROST = new Harm() {
        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SPELL, ctx.caster),
                    vsBoss(ctx, sigil, target, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 2f) * ctx.potency)));
            if (target instanceof LivingEntity living) {
                int ticks = ctx.duration(Math.round(sigil.param("duration", 100f)));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), ctx.caster);
                living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + ticks / 2));
            }
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
        if (target instanceof SpellHooks.Expellable x && x.onExpelled(ctx.caster, SpellHooks.Expellable.EXORCISE)) return true;
        if (target instanceof SpellHooks.Exorcisable ex) {
            return ex.onExorcised(ctx.potency);
        }
        if (!org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(target) || !(target instanceof LivingEntity demon)) return false;
        // A demon player is expelled, never cast out (v0.13).
        if (demon instanceof net.minecraft.server.level.ServerPlayer sp) return org.papiricoh.supernaturalcraft.allegiance.Expulsion.expel(sp, ctx.caster, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 10f) * ctx.potency) * 1.5f);
        if (!boss(demon) && demon.hasEffect(AllMobEffects.TRAPPED)) {
            ctx.level.playSound(null, demon.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0f, 0.5f);
            ItemEntity ember = new ItemEntity(ctx.level, demon.getX(), demon.getY() + 0.5, demon.getZ(),
                    new ItemStack(AllItems.HELLFIRE_EMBER.get()));
            ctx.level.addFreshEntity(ember);
            demon.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), Float.MAX_VALUE);
            return true;
        }
        return demon.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster),
                vsBoss(ctx, sigil, demon, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 10f) * ctx.potency)));
    };

    /**
     * The hunters' Snare (v0.21, once Bind): a short root. A demon is held as a devil's trap would hold it, but only for a breath
     * (the trap is the real trapping); anything else is rooted to the spot. Entities with their own answer
     * ({@link SpellHooks.Bindable}) give it.
     */
    public static final SpellBehavior.Effect SNARE = (Harm) (ctx, sigil, target) -> {
        int ticks = ctx.duration(Math.round(sigil.param("duration", 40f)));
        if (target instanceof SpellHooks.Bindable b) {
            return b.onBound(ticks);
        }
        if (!(target instanceof LivingEntity living)) return false;
        if (org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(living)) {
            living.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, ticks), ctx.caster);
        } else {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 7), ctx.caster);
        }
        return true;
    };

    /** @deprecated the Bind sigil is the Snare since v0.21 ({@link #SNARE}). */
    @Deprecated
    public static final SpellBehavior.Effect BIND = SNARE;

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
            return true;
        }
    };

    public static final SpellBehavior.Effect REPEL = (Harm) (ctx, sigil, target) -> {
        Vec3 away = target.position().subtract(ctx.caster.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = ctx.caster.getLookAngle().multiply(1, 0, 1);
        away = away.normalize().scale(sigil.param("strength", 1.4f) * ctx.potency);
        target.push(away.x, sigil.param("lift", 0.35f), away.z);
        target.hurtMarked = true;
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
        return true;
    };

    /** The Enochian banishing sigil (v0.13): an angel is flung far away, a lesser one sent back to Heaven. */
    public static final SpellBehavior.Effect BANISHING = (Harm) (ctx, sigil, target) ->
            org.papiricoh.supernaturalcraft.allegiance.Expulsion.banish(target, ctx.caster);

    // --- v0.21 ----------------------------------------------------------------------------------------------------------

    private interface Help extends SpellBehavior.Effect {
        @Override
        default boolean beneficial() {
            return true;
        }
    }

    private static boolean boss(Entity e) {
        return org.papiricoh.supernaturalcraft.bowl.spell.Bindings.isBoss(e);
    }

    private static float vsBoss(SpellContext ctx, SigilComponent sigil, Entity target, float dmg) {
        return vsBoss(ctx, sigil, ctx.caster, target, dmg);
    }

    private static float vsBoss(SpellContext ctx, SigilComponent sigil, LivingEntity caster, Entity target, float dmg) {
        if (org.papiricoh.supernaturalcraft.entity.boss.BossDamage.isBoss(target)) {
            dmg *= ctx.bossDamageScale * org.papiricoh.supernaturalcraft.balance.SpellBossDamage.effectScale(sigil.behavior().getPath());
        }
        return org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.vsBoss(caster, target, dmg);
    }

    /** Harms every foe of {@code caster} within {@code radius} of {@code at} (a delayed or lingering effect's blow). */
    private static int harmAround(net.minecraft.server.level.ServerLevel level, LivingEntity caster, Vec3 at, double radius,
                                  java.util.function.ToDoubleFunction<LivingEntity> damage,
                                  java.util.function.Function<LivingEntity, net.minecraft.world.damagesource.DamageSource> source) {
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius, 3, radius),
                e -> e.isAlive() && !e.isSpectator() && e != caster && e.position().distanceToSqr(at) <= radius * radius
                        && !org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell.isFriend(caster, e))) {
            if (e.hurt(source.apply(e), (float) damage.applyAsDouble(e))) n++;
        }
        return n;
    }

    /** Radiance (Enochian): a blinding flash. Blinds and outlines; burns the unholy hardest. Bosses only take the burn. */
    public static final SpellBehavior.Effect RADIANCE = (Harm) (ctx, sigil, target) -> {
        float dmg = org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 3f) * ctx.potency);
        if (unholy(target)) dmg *= sigil.param("vs_unholy", 2.5f);
        boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), vsBoss(ctx, sigil, target, dmg));
        if (target instanceof LivingEntity living && !boss(target)) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ctx.duration(Math.round(sigil.param("blind", 60f)))), ctx.caster);
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, ctx.duration(Math.round(sigil.param("glow", 100f)))), ctx.caster);
            hit = true;
        }
        return hit;
    };

    /**
     * Halo (Enochian): a shield of light on you and your allies (absorption, {@code shield} points times the potency, 4 per
     * level, up to five levels). Never touches a foe: it is a helpful sigil.
     */
    public static final SpellBehavior.Effect HALO = (Help) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        float shield = sigil.param("shield", 6f) * ctx.potency;
        int amp = Math.max(0, Math.min(4, Math.round(shield / 4f) - 1));
        living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ctx.duration(Math.round(sigil.param("duration", 300f))), amp), ctx.caster);
        return true;
    };

    /**
     * Judgement (Enochian, a Master's sigil): marks the ground under the target; {@code delay} ticks later a column of light
     * falls there, smiting every foe within {@code radius} ({@code damage} times the potency, the unholy {@code vs_unholy} more).
     * A player's spell: no telegraph, but its impact (30 ticks, the column's radius) shows where it will fall.
     */
    public static final SpellBehavior.Effect JUDGEMENT = (Harm) (ctx, sigil, target) -> {
        Vec3 at = target.position();
        LivingEntity caster = ctx.caster;
        net.minecraft.server.level.ServerLevel level = ctx.level;
        float base = org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 16f) * ctx.potency), vsUnholy = sigil.param("vs_unholy", 1.5f);
        double radius = sigil.param("radius", 2.5f) + ctx.area * 0.5;
        org.papiricoh.supernaturalcraft.util.ServerScheduler.schedule(Math.round(sigil.param("delay", 30f)), () -> {
            if (!caster.isAlive() || caster.level() != level) return;
            harmAround(level, caster, at, radius, e -> {
                float d = unholy(e) ? base * vsUnholy : base;
                return vsBoss(ctx, sigil, caster, e, d);
            }, e -> AllDamageTypes.source(level, AllDamageTypes.SMITE, caster));
        });
        return true;
    };

    /**
     * Brimstone (Infernal): a cloud of burning sulphur where it lands: {@code pulses} times, once a second, every foe within
     * {@code radius} of that spot takes {@code damage} times the potency in hellfire.
     */
    public static final SpellBehavior.Effect BRIMSTONE = (Harm) (ctx, sigil, target) -> {
        Vec3 at = target.position();
        LivingEntity caster = ctx.caster;
        net.minecraft.server.level.ServerLevel level = ctx.level;
        float dmg = org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 1.5f) * ctx.potency);
        double radius = sigil.param("radius", 2f) + ctx.area * 0.5;
        int pulses = Math.max(1, Math.round(sigil.param("pulses", 5f) * ctx.durationScale));
        boolean hit = target.hurt(AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, caster), vsBoss(ctx, sigil, target, dmg));
        for (int i = 1; i < pulses; i++) {
            org.papiricoh.supernaturalcraft.util.ServerScheduler.schedule(20 * i, () -> {
                if (caster.isAlive() && caster.level() == level) {
                    harmAround(level, caster, at, radius, e -> vsBoss(ctx, sigil, caster, e, dmg),
                            e -> AllDamageTypes.source(level, AllDamageTypes.HELLFIRE, caster));
                }
            });
        }
        return hit || target instanceof LivingEntity;
    };

    /** Soul Rend (Infernal): tears at the soul; the caster drinks {@code lifesteal} of the harm actually done. */
    public static final SpellBehavior.Effect SOUL_REND = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        float before = living.getHealth() + living.getAbsorptionAmount();
        boolean hit = living.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SPELL, ctx.caster), vsBoss(ctx, sigil, target, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 5f) * ctx.potency)));
        float dealt = Math.max(0, before - (living.isAlive() ? living.getHealth() + living.getAbsorptionAmount() : 0));
        if (dealt > 0) ctx.caster.heal(dealt * sigil.param("lifesteal", 0.5f));
        return hit;
    };

    /** Hellchain (Infernal): a chain of hellfire drags the target to the caster. Bosses are too heavy: they only burn. */
    public static final SpellBehavior.Effect HELLCHAIN = (Harm) (ctx, sigil, target) -> {
        boolean hit = target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.HELLFIRE, ctx.caster), vsBoss(ctx, sigil, target, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 2f) * ctx.potency)));
        if (boss(target) || !(target instanceof LivingEntity)) return hit;
        Vec3 to = ctx.caster.position().subtract(target.position());
        double dist = to.length();
        if (dist > 1.5) {
            Vec3 pull = to.multiply(1, 0, 1).normalize().scale(Math.min(sigil.param("strength", 1.6f) * ctx.potency, dist * 0.3));
            target.setDeltaMovement(pull.x, Math.min(0.6, 0.25 + Math.max(0, to.y) * 0.1), pull.z);
            target.hurtMarked = true;
        }
        return true;
    };

    /** Hex (Witchcraft): lays a jinx (JINXED) on the target. Bosses shrug it off; a protection bag or a Ward of the Name turns it. */
    public static final SpellBehavior.Effect HEX = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living) || boss(target)) return false;
        return living.addEffect(new MobEffectInstance(AllMobEffects.JINXED, ctx.duration(Math.round(sigil.param("duration", 300f))),
                Math.round(sigil.param("amplifier", 0f))), ctx.caster);
    };

    /**
     * Pain, Ignis Dolor (Witchcraft): magic that goes through armour, and a limp (Slowness). A boss takes the harm and no more.
     */
    public static final SpellBehavior.Effect PAIN = (Harm) (ctx, sigil, target) -> {
        boolean hit = target.hurt(ctx.level.damageSources().indirectMagic(ctx.caster, ctx.caster), vsBoss(ctx, sigil, target, org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 4f) * ctx.potency)));
        if (target instanceof LivingEntity living && !boss(target)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ctx.duration(Math.round(sigil.param("duration", 60f))), 1), ctx.caster);
            hit = true;
        }
        return hit;
    };

    /**
     * Glamour (Witchcraft): the target (you, or an ally) turns unseen for a moment, and the creatures hunting them within
     * {@code radius} see a foe in each other instead (each turns on the nearest other creature near it, or loses the scent).
     * Bosses are never fooled.
     */
    public static final SpellBehavior.Effect GLAMOUR = (Help) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ctx.duration(Math.round(sigil.param("duration", 100f))), 0, false, false, true), ctx.caster);
        double r = sigil.param("radius", 16f);
        java.util.List<net.minecraft.world.entity.Mob> hunters = ctx.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                living.getBoundingBox().inflate(r), m -> m.getTarget() == living && !boss(m));
        for (net.minecraft.world.entity.Mob m : hunters) {
            LivingEntity other = ctx.level.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(8),
                            e -> e != m && e != living && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
                                    && !org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell.isFriend(ctx.caster, e) && !boss(e))
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(m))).orElse(null);
            m.setTarget(other);
        }
        return true;
    };

    /**
     * Salt Shot (Hunter): a blast of rock salt. A ghost is scattered, a possession (POSSESSED) broken; a demon takes it hard
     * ({@code vs_unholy}) and is knocked back; anything else is only stung.
     */
    public static final SpellBehavior.Effect SALT_SHOT = (Harm) (ctx, sigil, target) -> {
        // v0.21 (P5): salt drives a riding demon out of its vessel (Abaddon in a knight).
        if (target instanceof SpellHooks.Expellable x && x.onExpelled(ctx.caster, SpellHooks.Expellable.SALT)) return true;
        boolean any = false;
        if (target instanceof org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity ghost) {
            if (!ghost.isFading() && !ghost.isDispersed()) {
                ghost.disperse();
                return true;
            }
            return false;
        }
        if (target instanceof LivingEntity living && living.hasEffect(AllMobEffects.POSSESSED) && !boss(target)) {
            living.removeEffect(AllMobEffects.POSSESSED);
            any = true;
        }
        // v0.21 (P3): salt frees a hexed villager (no harm done) and cracks a coven witch's shield.
        if (target instanceof LivingEntity living && !boss(target)
                && org.papiricoh.supernaturalcraft.hex.HexHooks.saltStruck(living, ctx.caster)) {
            if (!living.isAlive() || living instanceof org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity) return true;
            any = true;
        }
        // A hexed villager already being freed (or one salt cannot free) takes no harm from it.
        if (target instanceof org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity) return any;
        float dmg = org.papiricoh.supernaturalcraft.balance.WeaponDamage.magic(sigil.param("damage", 1.5f) * ctx.potency);
        boolean demon = org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(target);
        if (demon) dmg *= sigil.param("vs_unholy", 2.5f);
        any |= target.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SPELL, ctx.caster), vsBoss(ctx, sigil, target, dmg));
        if (demon && !boss(target)) {
            Vec3 away = target.position().subtract(ctx.caster.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize().scale(0.6);
                target.push(away.x, 0.2, away.z);
                target.hurtMarked = true;
            }
        }
        return any;
    };

    /**
     * Silver Edge (Hunter): the next {@code charges} blows of the target's weapon (any weapon, even a fist) count as silver
     * against werewolves and shapeshifters ({@code entity.legacy.LegacyWeapons#silver}); one charge goes with each blow that
     * lands ({@code hex.CurseEvents}).
     */
    public static final SpellBehavior.Effect SILVER_EDGE = (Help) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        int charges = Math.max(1, Math.min(8, Math.round(sigil.param("charges", 3f) * ctx.potency)));
        living.addEffect(new MobEffectInstance(AllMobEffects.SILVER_EDGE, ctx.duration(Math.round(sigil.param("duration", 1200f))), charges - 1), ctx.caster);
        return true;
    };

    public static void registerAll() {
        SpellBehaviors.register(SupernaturalCraft.asResource("smite"), SMITE);
        SpellBehaviors.register(SupernaturalCraft.asResource("hellfire"), HELLFIRE);
        SpellBehaviors.register(SupernaturalCraft.asResource("frost"), FROST);
        SpellBehaviors.register(SupernaturalCraft.asResource("exorcise"), EXORCISE);
        SpellBehaviors.register(SupernaturalCraft.asResource("snare"), SNARE);
        // Formulas worked from the old Bind sigil keep its behaviour id (v0.21).
        SpellBehaviors.register(SupernaturalCraft.asResource("bind"), SNARE);
        SpellBehaviors.register(SupernaturalCraft.asResource("mend"), MEND);
        SpellBehaviors.register(SupernaturalCraft.asResource("repel"), REPEL);
        SpellBehaviors.register(SupernaturalCraft.asResource("reveal"), REVEAL);
        SpellBehaviors.register(SupernaturalCraft.asResource("banishing"), BANISHING);
        SpellBehaviors.register(SupernaturalCraft.asResource("radiance"), RADIANCE);
        SpellBehaviors.register(SupernaturalCraft.asResource("halo"), HALO);
        SpellBehaviors.register(SupernaturalCraft.asResource("judgement"), JUDGEMENT);
        SpellBehaviors.register(SupernaturalCraft.asResource("brimstone"), BRIMSTONE);
        SpellBehaviors.register(SupernaturalCraft.asResource("soul_rend"), SOUL_REND);
        SpellBehaviors.register(SupernaturalCraft.asResource("hellchain"), HELLCHAIN);
        SpellBehaviors.register(SupernaturalCraft.asResource("hex"), HEX);
        SpellBehaviors.register(SupernaturalCraft.asResource("pain"), PAIN);
        SpellBehaviors.register(SupernaturalCraft.asResource("glamour"), GLAMOUR);
        SpellBehaviors.register(SupernaturalCraft.asResource("salt_shot"), SALT_SHOT);
        SpellBehaviors.register(SupernaturalCraft.asResource("silver_edge"), SILVER_EDGE);
    }
}
