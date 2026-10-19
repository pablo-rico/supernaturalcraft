package org.papiricoh.supernaturalcraft.entity.witch;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.bowl.witch.Maledictions;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.hex.HexBags;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.magic.spell.SpellContext;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.List;
import java.util.Optional;

/**
 * How the coven's creatures work their magic (v0.21), through the same pipeline as a hunter's: sigil bolts built from the
 * datapack's sigils ({@code hex}, {@code pain}, {@code frost}…) with their CAST_CIRCLE and BOLT_TRAIL effects; curses from
 * afar with the bowl's own curses ({@link CurseEffect}), turned by a Ward of the Name or a protection bag and sent back by a
 * Mirror Hex; curse bags slipped into a pocket ({@link HexBags}); and smoke teleports (TELEPORT).
 */
public final class WitchMagic {

    public static final ResourceLocation BOLT = SupernaturalCraft.asResource("bolt");
    /** How long a witch's cast circle and curse pulses play. */
    public static final int FX_TICKS = 20;

    private WitchMagic() {
    }

    /** Whether {@code e} is one of the coven's: a witch, a familiar or a hexed victim. */
    public static boolean kin(net.minecraft.world.entity.Entity e) {
        return e instanceof WitchEntity || e instanceof FamiliarEntity || e instanceof HexedVillagerEntity
                || e.getType().is(AllTags.Entities.WITCHES);
    }

    /**
     * {@code caster} hurls a sigil bolt of {@code sigil} (an effect sigil's path) at {@code target}, at {@code potency}.
     *
     * @return whether it went off (false if the sigil is missing from the datapack)
     */
    public static boolean bolt(Mob caster, LivingEntity target, String sigil, float potency) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        Spell spell = new Spell(Optional.of(BOLT), List.of(SupernaturalCraft.asResource(sigil)), List.of(), "");
        ResolvedSpell resolved = ResolvedSpell.resolve(level.registryAccess(), spell);
        if (resolved == null) return false;
        SpellContext ctx = new SpellContext(level, caster);
        ctx.color = resolved.effects().getFirst().sigil().color();
        resolved.formBehavior().setup(ctx, resolved.form());
        ctx.potency = potency;
        Vec3 eye = new Vec3(caster.getX(), caster.getEyeY() - 0.15, caster.getZ());
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 dir = aim.subtract(eye).normalize();
        Vec3 from = eye.add(dir.x * 0.6, 0, dir.z * 0.6);
        MagicFx.cast(level, caster, resolved.school(), from.add(0, -0.2, 0), aim, resolved.tier(), false, SpellCaster.CIRCLE_TICKS);
        SigilBolt bolt = new SigilBolt(level, caster, spell, ctx);
        bolt.setPos(from.x, from.y, from.z);
        bolt.shoot(dir.x, dir.y, dir.z, ctx.range, 1.5f);
        level.addFreshEntity(bolt);
        MagicFx.boltTrail(level, bolt, resolved.school(), bolt.position(), bolt.position().add(dir), resolved.impactIndex(), ctx.range,
                SigilBolt.MAX_AGE);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), AllSounds.coven("witch.cast"), SoundSource.HOSTILE, 1f,
                0.9f + caster.getRandom().nextFloat() * 0.2f);
        return true;
    }

    /** What became of a curse from afar. */
    public enum Outcome {CURSED, WARDED, MIRRORED, REFUSED}

    /**
     * {@code witch} lays {@code curse} on {@code victim} from wherever she is, for {@code duration} ticks: never on a boss, a
     * creative or spectating player or one of her own; a Ward of the Name (or a charged protection bag) turns it away and hurts
     * her; a Mirror Hex sends it back to her.
     */
    public static Outcome curse(Mob witch, LivingEntity victim, CurseEffect.Curse curse, int duration) {
        if (!(witch.level() instanceof ServerLevel level) || !victim.isAlive() || kin(victim)) return Outcome.REFUSED;
        if (victim.getType().is(AllTags.Entities.BOSSES)) return Outcome.REFUSED;
        if (victim instanceof Player p && (p.isCreative() || p.isSpectator())) return Outcome.REFUSED;
        CurseEffect effect = new CurseEffect(curse, duration);
        if (victim.hasEffect(AllMobEffects.NAME_WARDED) || HexBags.isProtected(victim)) {
            pulse(level, victim, MagicFxPayload.HEX_PROTECT);
            witch.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, null), Maledictions.BACKLASH);
            return Outcome.WARDED;
        }
        if (victim.hasEffect(AllMobEffects.MIRRORED)) {
            victim.removeEffect(AllMobEffects.MIRRORED);
            pulse(level, victim, MagicFxPayload.HEX_PROTECT);
            effect.lay(witch);
            pulse(level, witch, MagicFxPayload.HEX_CURSE);
            return Outcome.MIRRORED;
        }
        effect.lay(victim);
        pulse(level, victim, MagicFxPayload.HEX_CURSE);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), AllSounds.HEX_CURSE.get(), SoundSource.HOSTILE, 0.8f, 0.9f);
        return Outcome.CURSED;
    }

    private static void pulse(ServerLevel level, LivingEntity e, int mode) {
        MagicFx.hex(level, e, e.position().add(0, e.getBbHeight() / 2, 0), mode, 1.2f, Maledictions.FX_TICKS);
    }

    /** {@code witch} slips one of her curse bags into {@code victim}'s pack; a protection bag turns it away. */
    public static boolean slipBag(Mob witch, Player victim) {
        if (victim.isCreative() || victim.isSpectator() || !victim.isAlive()) return false;
        if (HexBags.isProtected(victim)) {
            HexBags.protectFx(victim);
            return false;
        }
        return victim.getInventory().add(HexBags.curseBag(witch.getUUID()));
    }

    /**
     * A smoke teleport: {@code mob} blinks to a safe spot {@code min}–{@code max} blocks away (away from {@code from} when
     * given). @return whether it found one
     */
    public static boolean teleport(Mob mob, Vec3 awayFrom, double min, double max, int smoke) {
        if (!(mob.level() instanceof ServerLevel level)) return false;
        Vec3 start = mob.position();
        var random = mob.getRandom();
        for (int i = 0; i < 16; i++) {
            double angle;
            if (awayFrom != null && i < 10) {
                double base = Math.atan2(start.z - awayFrom.z, start.x - awayFrom.x);
                angle = base + (random.nextDouble() - 0.5) * Math.PI * 0.9;
            } else {
                angle = random.nextDouble() * Math.PI * 2;
            }
            double d = min + random.nextDouble() * (max - min);
            double x = start.x + Math.cos(angle) * d, z = start.z + Math.sin(angle) * d;
            double y = start.y + random.nextInt(5) - 2;
            if (mob.randomTeleport(x, y, z, false)) {
                Vec3 end = mob.position();
                MagicFx.teleport(level, mob, School.WITCHCRAFT, start, end, smoke, FX_TICKS * 2);
                level.playSound(null, start.x, start.y, start.z, AllSounds.coven("witch.teleport"), SoundSource.HOSTILE, 1f, 1f);
                level.playSound(null, end.x, end.y, end.z, AllSounds.coven("witch.teleport"), SoundSource.HOSTILE, 0.8f, 1.1f);
                mob.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    /** A sip of her own brew: health back and a short regeneration. */
    public static void heal(Mob witch, float amount) {
        witch.heal(amount);
        witch.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0));
        witch.level().playSound(null, witch.getX(), witch.getY(), witch.getZ(), net.minecraft.sounds.SoundEvents.WITCH_DRINK,
                SoundSource.HOSTILE, 1f, 1f);
        if (witch.level() instanceof ServerLevel level) pulse(level, witch, MagicFxPayload.HEX_PROTECT);
    }
}
