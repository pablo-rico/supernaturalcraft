package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * The demon blood's clock (v0.40, game bus), once a second per drinker: the blood fades, the dependency fades a little each
 * day, withdrawal comes and goes (its marker effect, a human's weakness and slowness, a demon's or angel's draining essence),
 * the Panic Room's cure runs ({@link PanicRoom#tick}). Holy damage bites harder into a demon or angel in withdrawal.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class BloodEvents {

    private BloodEvents() {
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        BloodData d = Bloods.get(p);
        PanicRoom.tick(p);
        if (d.blood() <= 0 && d.dependency() <= 0) return;
        long day = WorldClock.day(p.server) / BloodRules.DAY_TICKS;
        Bloods.update(p, x -> {
            BloodData out = x.withBlood(BloodRules.fade(x.blood(), 1f / 60f));
            if (x.lastDay() >= 0 && day > x.lastDay()) out = out.withDependency(BloodRules.fadeDays(x.dependency(), day - x.lastDay())).withLastDay(day);
            else if (x.lastDay() < 0) out = out.withLastDay(day);
            return out;
        });
        d = Bloods.get(p);
        if (!BloodRules.withdrawal(d)) {
            if (p.hasEffect(AllMobEffects.WITHDRAWAL)) p.removeEffect(AllMobEffects.WITHDRAWAL);
            return;
        }
        p.addEffect(new MobEffectInstance(AllMobEffects.WITHDRAWAL, 60, BloodRules.severe(d) ? 1 : 0, true, false, true));
        Allegiance a = Allegiances.get(p);
        if (a.faction() == Faction.HUMAN) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, false));
            if (BloodRules.severe(d)) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, true, false, false));
                if (p.getRandom().nextInt(30) == 0) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, false));
            }
        } else {
            float drain = BloodRules.essenceDrainPerSecond(d, a.faction()) * a.maxEssence();
            if (drain > 0 && a.essence() > 0) Allegiances.update(p, a.addEssence(-drain));
        }
        if (p.getRandom().nextInt(45) == 0) {
            p.serverLevel().playSound(null, p.blockPosition(), AllSounds.apocalypse("blood.craving"), SoundSource.PLAYERS, 0.6f, 1f);
        }
    }

    @SubscribeEvent
    public static void holy(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !org.papiricoh.supernaturalcraft.weapon.Holy.isHoly(event.getSource())) return;
        float k = BloodRules.holyTaken(Bloods.get(p), Allegiances.get(p).faction());
        if (k > 1f) event.setAmount(event.getAmount() * k);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Bloods.sync(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Bloods.sync(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Bloods.sync(p);
    }
}
