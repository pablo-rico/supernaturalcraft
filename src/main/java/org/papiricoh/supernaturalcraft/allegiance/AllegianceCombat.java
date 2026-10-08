package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.papiricoh.supernaturalcraft.allegiance.power.ActivePowers;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity;
import org.papiricoh.supernaturalcraft.hunter.DemonBane;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Blows given and taken by the sworn (v0.13), on {@link LivingIncomingDamageEvent}:
 * <ul>
 *   <li>untouchable while pouring as smoke or riding a possessed mob;</li>
 *   <li>a demon with fire immunity takes no fire;</li>
 *   <li>the Angel Blade does ×{@link #SWORN_BLADE} to an angel player (a demon player already takes every demon-bane weapon's
 *   bonus, Ruby's knife and the blade included, through {@code hunter.CombatEvents});</li>
 *   <li>a Knight of Hell's First Blade ×{@link #FIRST_BLADE}; a Legend's hunter weapons ×{@link #HUNTER_EDGE} against the
 *   supernatural; a General's lance of light ×{@link #LIGHT_LANCE};</li>
 *   <li>dominion: whatever strikes a King of Hell has every demon near after it.</li>
 * </ul>
 */
public final class AllegianceCombat {

    public static final float SWORN_BLADE = 3f, FIRST_BLADE = 1.5f, HUNTER_EDGE = 1.2f, LIGHT_LANCE = 2.5f;
    public static final double DOMINION_RANGE = 16;

    private AllegianceCombat() {
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();
        if (victim instanceof ServerPlayer p) {
            if (ActivePowers.untouchable(p) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
                return;
            }
            Allegiance a = Allegiances.get(p);
            if (src.is(DamageTypeTags.IS_FIRE) && PowerRules.passiveActive(a, Power.FIRE_IMMUNITY, PowerCaster.suppressed(p))) {
                event.setCanceled(true);
                return;
            }
            if (a.isAngel() && src.getDirectEntity() instanceof LivingEntity att && att.getMainHandItem().is(AllItems.ANGEL_BLADE.get())) {
                event.setAmount(event.getAmount() * SWORN_BLADE);
                AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, Passives.WHISPER_SWORN_BLADE, 0, p.position(), 40);
            }
            if (a.committed() && Kin.isDemon(p) && src.getDirectEntity() instanceof LivingEntity att && att.getMainHandItem().getItem() instanceof DemonBane bane) {
                // Every demon-bane weapon already bites a demon player (CombatEvents); the two killing blades bite ×3 in all.
                if (att.getMainHandItem().is(AllItems.RUBYS_KNIFE.get()) || att.getMainHandItem().is(AllItems.ANGEL_BLADE.get())) {
                    event.setAmount(event.getAmount() * SWORN_BLADE / Math.max(1f, bane.demonDamageMultiplier()));
                }
                AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, Passives.WHISPER_SWORN_BLADE, 0, p.position(), 40);
            }
            if (src.getEntity() instanceof LivingEntity attacker && attacker != p
                    && PowerRules.passiveActive(a, Power.DOMINION, PowerCaster.suppressed(p))) {
                dominion(p, attacker);
            }
        }
        Entity attacker = src.getEntity();
        if (attacker instanceof ServerPlayer ap && src.getDirectEntity() == ap) {
            Allegiance aa = Allegiances.get(ap);
            boolean suppressed = PowerCaster.suppressed(ap);
            if (ap.getMainHandItem().is(AllItems.FIRST_BLADE.get()) && PowerRules.passiveActive(aa, Power.FIRST_BLADE, suppressed)) {
                event.setAmount(event.getAmount() * FIRST_BLADE);
            }
            if (ap.getMainHandItem().getItem() instanceof DemonBane && supernatural(victim)
                    && PowerRules.passiveActive(aa, Power.HUNTER_EDGE, suppressed)) {
                event.setAmount(event.getAmount() * HUNTER_EDGE);
            }
        }
        if (src.getDirectEntity() instanceof LightSpearEntity spear && spear.getOwner() instanceof Player owner) {
            // Against a great enemy the lance also grows with its thrower's tier (v0.15).
            event.setAmount(org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.vsBoss(owner, victim, event.getAmount() * LIGHT_LANCE));
        }
    }

    /** The First Blade in a Knight of Hell's hands: it answers him, and its curse no longer climbs. */
    public static boolean knightsBlade(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        return stack.is(AllItems.FIRST_BLADE.get())
                && PowerRules.passiveActive(Allegiances.get(player), Power.FIRST_BLADE, PowerCaster.suppressed(player));
    }

    static boolean supernatural(LivingEntity e) {
        return e.getType().is(AllTags.Entities.SUPERNATURAL) || Kin.isDemon(e) || Kin.isAngel(e);
    }

    /** Every demon near the King turns on whoever struck him. @return how many */
    public static int dominion(ServerPlayer king, LivingEntity attacker) {
        int n = 0;
        for (Mob m : king.serverLevel().getEntitiesOfClass(Mob.class, king.getBoundingBox().inflate(DOMINION_RANGE),
                m -> m.isAlive() && m != attacker && Kin.isDemon(m) && !m.getType().is(AllTags.Entities.BOSSES))) {
            m.setTarget(attacker);
            n++;
        }
        return n;
    }
}
