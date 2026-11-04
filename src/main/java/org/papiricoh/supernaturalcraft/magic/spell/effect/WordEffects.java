package org.papiricoh.supernaturalcraft.magic.spell.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Expulsion;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.balance.WeaponDamage;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehavior;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBehaviors;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The sigils of the Word (v0.36, tier {@link SigilComponent#WORD_TIER}): only the tablets teach them. None of them touches a
 * boss. The two "mass" ones reach every one of their kind within {@code radius} of what the spell struck.
 */
public final class WordEffects {

    private WordEffects() {
    }

    private interface Harm extends SpellBehavior.Effect {
        @Override
        default boolean beneficial() {
            return false;
        }
    }

    private interface Help extends SpellBehavior.Effect {
        @Override
        default boolean beneficial() {
            return true;
        }
    }

    private static boolean boss(Entity e) {
        return e.getType().is(AllTags.Entities.BOSSES);
    }

    /** The Angel Tablet: an angel struck from the sky — slammed down, hurt, and slowed as if its wings were gone. */
    public static final SpellBehavior.Effect THE_FALL = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity angel) || !Kin.isAngel(angel) || boss(angel)) return false;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 200f)));
        angel.setDeltaMovement(angel.getDeltaMovement().x * 0.2, -sigil.param("slam", 2.5f), angel.getDeltaMovement().z * 0.2);
        angel.hurtMarked = true;
        angel.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2), ctx.caster);
        angel.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 0), ctx.caster);
        angel.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), WeaponDamage.magic(sigil.param("damage", 10f) * ctx.potency));
        ctx.level.sendParticles(ParticleTypes.END_ROD, angel.getX(), angel.getY() + 1.5, angel.getZ(), 30, 0.6, 0.4, 0.6, 0.02);
        return true;
    };

    /** The Angel Tablet: every angel round the struck point sent away, as the banishing sigil does one. */
    public static final SpellBehavior.Effect MASS_EXPULSION = (Harm) (ctx, sigil, target) -> {
        double r = sigil.param("radius", 12f) * ctx.potency;
        int sent = 0;
        for (LivingEntity e : around(ctx, target.position(), r)) {
            if (Kin.isAngel(e) && e != ctx.caster && Expulsion.banish(e, ctx.caster)) sent++;
        }
        return sent > 0;
    };

    /** The Demon Tablet: every demon round the struck point cast out at once (a held one for good, as the exorcism). */
    public static final SpellBehavior.Effect MASS_EXORCISM = (Harm) (ctx, sigil, target) -> {
        double r = sigil.param("radius", 12f) * ctx.potency;
        int cast = 0;
        for (LivingEntity e : around(ctx, target.position(), r)) {
            if (e == ctx.caster || !Kin.isDemon(e)) continue;
            if (SpellEffects.EXORCISE.applyToEntity(ctx, sigil, e)) cast++;
        }
        return cast > 0;
    };

    /** The Leviathan Tablet: Purgatory's grip — held fast and dragged towards the caster. */
    public static final SpellBehavior.Effect PURGATORY_BIND = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living) || boss(living)) return false;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 100f)));
        living.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, ticks), ctx.caster);
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks, 0), ctx.caster);
        Vec3 pull = ctx.caster.position().subtract(living.position());
        if (pull.lengthSqr() > 4) living.setDeltaMovement(pull.normalize().scale(sigil.param("pull", 0.8f)));
        living.hurtMarked = true;
        ctx.level.sendParticles(ParticleTypes.ASH, living.getX(), living.getY() + 1, living.getZ(), 40, 0.4, 0.8, 0.4, 0.01);
        return true;
    };

    /**
     * The Leviathan Tablet: a monster (no boss, no player, no villager) shut in an empty Purgatory jar the caster carries; it
     * comes out where the full jar is used.
     */
    public static final SpellBehavior.Effect SEAL_IN_JAR = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof Mob mob) || boss(mob) || mob instanceof net.minecraft.world.entity.npc.Npc || !mob.isAlive()) return false;
        if (!(ctx.caster instanceof Player player)) return false;
        ItemStack jar = findJar(player);
        if (jar.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.purgatory_jar.none").withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        CompoundTag soul = new CompoundTag();
        if (!mob.save(soul)) return false;
        soul.remove("UUID");
        ItemStack full = new ItemStack(AllItems.PURGATORY_JAR_FULL.get());
        full.set(AllDataComponents.JAR_SOUL.get(), CustomData.of(soul));
        jar.shrink(1);
        if (!player.getInventory().add(full)) player.drop(full, false);
        ctx.level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
        ctx.level.playSound(null, mob.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
        mob.discard();
        return true;
    };

    // --- v0.36, the longer tablets ----------------------------------------------------------------------------------------

    /** Angel Tablet: the angels' radio — every angel within {@code radius} glows for a while, and you hear how many. */
    public static final SpellBehavior.Effect ANGEL_RADIO = (Help) (ctx, sigil, target) -> {
        double r = sigil.param("radius", 64f) * ctx.potency;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 200f)));
        int heard = 0;
        for (LivingEntity e : around(ctx, ctx.caster.position(), r)) {
            if (e == ctx.caster || !Kin.isAngel(e)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), ctx.caster);
            heard++;
        }
        if (ctx.caster instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.angel_radio", heard).withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    };

    /** Angel Tablet: grace torn out of an angel — it is hurt, and half of it heals you; twice as much if it is held. */
    public static final SpellBehavior.Effect STOLEN_GRACE = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity angel) || !Kin.isAngel(angel) || boss(angel)) return false;
        float dmg = WeaponDamage.magic(sigil.param("damage", 8f) * ctx.potency);
        if (angel.hasEffect(AllMobEffects.TRAPPED)) dmg *= 2;
        float before = angel.getHealth();
        angel.hurt(AllDamageTypes.source(ctx.level, AllDamageTypes.SMITE, ctx.caster), dmg);
        float taken = Math.max(0, before - angel.getHealth());
        ctx.caster.heal(taken * sigil.param("heal_share", 0.5f));
        ctx.level.sendParticles(ParticleTypes.END_ROD, angel.getX(), angel.getY() + 1.2, angel.getZ(), 25, 0.3, 0.5, 0.3, 0.08);
        return true;
    };

    /** Angel Tablet: Enoch's chain — a ring of holy fire round the angel struck, and it is held at once. */
    public static final SpellBehavior.Effect ENOCHS_CHAIN = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity angel) || !Kin.isAngel(angel) || boss(angel)) return false;
        org.papiricoh.supernaturalcraft.allegiance.item.HolyOilItem.pour(ctx.level, angel.blockPosition());
        angel.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, ctx.duration(Math.round(sigil.param("duration", 60f)))), ctx.caster);
        return true;
    };

    /** Demon Tablet: a devil's trap thrown where the spell lands, that fades after a while ({@link TempTraps}). */
    public static final SpellBehavior.Effect CAST_TRAP = new Harm() {
        @Override
        public boolean applyToEntity(SpellContext ctx, SigilComponent sigil, Entity target) {
            if (boss(target)) return false;
            return TempTraps.place(ctx.level, target.blockPosition(), ctx.duration(Math.round(sigil.param("duration", 200f))));
        }

        @Override
        public boolean applyToBlock(SpellContext ctx, SigilComponent sigil, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {
            return TempTraps.place(ctx.level, pos.above(), ctx.duration(Math.round(sigil.param("duration", 200f))));
        }
    };

    /** Demon Tablet: a crossroads deal broken clean — the contract void, the hounds and the demon sent away. */
    public static final SpellBehavior.Effect BROKEN_CONTRACT = (Help) (ctx, sigil, target) -> {
        if (!(target instanceof ServerPlayer sp)) return false;
        var state = org.papiricoh.supernaturalcraft.crossroads.Debts.get(sp).state();
        if (state != org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State.OPEN
                && state != org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State.COLLECTING
                && state != org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State.HUNTED) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.no_deal").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        org.papiricoh.supernaturalcraft.crossroads.Debts.free(sp, org.papiricoh.supernaturalcraft.crossroads.ContractTerms.VOID);
        return true;
    };

    /** Demon Tablet: no demon round you leaves in smoke for a while. */
    public static final SpellBehavior.Effect BOUND_SMOKE = (Help) (ctx, sigil, target) -> {
        double r = sigil.param("radius", 12f) * ctx.potency;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 400f)));
        for (LivingEntity e : around(ctx, ctx.caster.position(), r)) {
            if (e != ctx.caster && Kin.isDemon(e)) e.addEffect(new MobEffectInstance(AllMobEffects.SMOKEBOUND, ticks), ctx.caster);
        }
        ctx.level.sendParticles(ParticleTypes.LARGE_SMOKE, ctx.caster.getX(), ctx.caster.getY() + 0.2, ctx.caster.getZ(), 60, r / 3, 0.2, r / 3, 0.01);
        return true;
    };

    /** Leviathan Tablet: a black maw bites — heavy harm, and part of it feeds you. */
    public static final SpellBehavior.Effect HUNGER = (Harm) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        float before = living.getHealth();
        float dmg = WeaponDamage.magic(sigil.param("damage", 12f) * ctx.potency);
        living.hurt(ctx.level.damageSources().indirectMagic(ctx.caster, ctx.caster), boss(living) ? dmg * 0.5f : dmg);
        ctx.caster.heal(Math.max(0, before - living.getHealth()) * sigil.param("heal_share", 0.4f));
        if (ctx.caster instanceof Player p) p.getFoodData().eat(4, 0.5f);
        ctx.level.sendParticles(ParticleTypes.SQUID_INK, living.getX(), living.getY() + 1, living.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
        ctx.level.playSound(null, living.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    };

    /** Leviathan Tablet: a borrowed face — no monster knows you until you strike ({@code MIMICRY}, see {@code prophecy/Mimicry}). */
    public static final SpellBehavior.Effect MIMICRY = (Help) (ctx, sigil, target) -> {
        if (!(target instanceof LivingEntity living)) return false;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 1200f)));
        org.papiricoh.supernaturalcraft.prophecy.Mimicry.start(living, ticks);
        return true;
    };

    /** Leviathan Tablet: the black sea — everything round the struck point slowed, choked and dragged down. */
    public static final SpellBehavior.Effect BLACK_TIDE = (Harm) (ctx, sigil, target) -> {
        double r = sigil.param("radius", 5f) * ctx.potency;
        int ticks = ctx.duration(Math.round(sigil.param("duration", 120f)));
        int hit = 0;
        for (LivingEntity e : around(ctx, target.position(), r)) {
            if (e == ctx.caster) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3), ctx.caster);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(ticks, 60), 0), ctx.caster);
            e.setAirSupply(Math.max(-20, e.getAirSupply() - 150));
            e.hurt(ctx.level.damageSources().drown(), WeaponDamage.magic(sigil.param("damage", 4f) * ctx.potency));
            hit++;
        }
        ctx.level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 0.3, target.getZ(), 120, r / 2, 0.3, r / 2, 0.02);
        return hit > 0;
    };

    private static ItemStack findJar(Player player) {
        for (ItemStack s : player.getInventory().items) if (s.is(AllItems.PURGATORY_JAR.get())) return s;
        ItemStack off = player.getOffhandItem();
        return off.is(AllItems.PURGATORY_JAR.get()) ? off : ItemStack.EMPTY;
    }

    private static java.util.List<LivingEntity> around(SpellContext ctx, Vec3 at, double r) {
        return ctx.level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> e.isAlive() && !boss(e) && e.distanceToSqr(at) <= r * r);
    }

    public static void registerAll() {
        SpellBehaviors.register(SupernaturalCraft.asResource("the_fall"), THE_FALL);
        SpellBehaviors.register(SupernaturalCraft.asResource("mass_expulsion"), MASS_EXPULSION);
        SpellBehaviors.register(SupernaturalCraft.asResource("mass_exorcism"), MASS_EXORCISM);
        SpellBehaviors.register(SupernaturalCraft.asResource("purgatory_bind"), PURGATORY_BIND);
        SpellBehaviors.register(SupernaturalCraft.asResource("seal_in_jar"), SEAL_IN_JAR);
        SpellBehaviors.register(SupernaturalCraft.asResource("angel_radio"), ANGEL_RADIO);
        SpellBehaviors.register(SupernaturalCraft.asResource("stolen_grace"), STOLEN_GRACE);
        SpellBehaviors.register(SupernaturalCraft.asResource("enochs_chain"), ENOCHS_CHAIN);
        SpellBehaviors.register(SupernaturalCraft.asResource("cast_trap"), CAST_TRAP);
        SpellBehaviors.register(SupernaturalCraft.asResource("broken_contract"), BROKEN_CONTRACT);
        SpellBehaviors.register(SupernaturalCraft.asResource("bound_smoke"), BOUND_SMOKE);
        SpellBehaviors.register(SupernaturalCraft.asResource("hunger"), HUNGER);
        SpellBehaviors.register(SupernaturalCraft.asResource("mimicry"), MIMICRY);
        SpellBehaviors.register(SupernaturalCraft.asResource("black_tide"), BLACK_TIDE);
    }
}
