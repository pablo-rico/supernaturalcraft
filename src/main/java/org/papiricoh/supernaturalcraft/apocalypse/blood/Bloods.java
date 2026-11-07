package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.network.BloodSyncPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.function.UnaryOperator;

/** A drinker's demon blood on the server (v0.40): the attachment, a dose, and the sync (to the owner only). */
public final class Bloods {

    private Bloods() {
    }

    public static BloodData get(ServerPlayer p) {
        return p.getData(AllAttachments.DEMON_BLOOD);
    }

    public static void update(ServerPlayer p, UnaryOperator<BloodData> f) {
        BloodData before = get(p);
        BloodData after = f.apply(before);
        if (after.equals(before)) return;
        p.setData(AllAttachments.DEMON_BLOOD, after);
        if (changedForClient(before, after)) sync(p);
    }

    /** Only what the HUD shows: blood in steps of 1 %, dependency in steps of 1 %, tier, withdrawal. */
    private static boolean changedForClient(BloodData a, BloodData b) {
        return Math.round(a.blood() * 100) != Math.round(b.blood() * 100) || Math.round(a.dependency() * 100) != Math.round(b.dependency() * 100)
                || BloodRules.tier(a.doses()) != BloodRules.tier(b.doses()) || BloodRules.withdrawal(a) != BloodRules.withdrawal(b)
                || (a.blood() > 0) != (b.blood() > 0);
    }

    public static void sync(ServerPlayer p) {
        BloodData d = get(p);
        Send.toPlayer(p, new BloodSyncPayload(d.blood(), d.dependency(), BloodRules.tier(d.doses()), BloodRules.withdrawal(d)));
    }

    /** One dose: the blood, the dependency, a demon's Corruption, the first-drink advancement. */
    public static void drink(ServerPlayer p) {
        Allegiance a = Allegiances.get(p);
        Faction side = a.faction();
        update(p, d -> {
            BloodData out = BloodRules.drink(d, side);
            return out.lastDay() < 0 ? out.withLastDay(WorldClock.day(p.server) / BloodRules.DAY_TICKS) : out;
        });
        if (side == Faction.DEMON && a.maxEssence() > 0) {
            Allegiances.update(p, Allegiances.get(p).addEssence(a.maxEssence() * BloodRules.DEMON_REFILL));
        }
        p.removeEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.WITHDRAWAL);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.apocalypse("blood.drink"), SoundSource.PLAYERS, 1f, 0.9f);
        p.serverLevel().sendParticles(AllParticles.BLOOD_MOTE.get(), p.getX(), p.getEyeY() - 0.2, p.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
        ChorusRewards.award(p, "main/when_the_levee_breaks");
    }

    /** Whether {@code p} has the blood (and the tier) for a power of tier {@code tier} costing {@code cost}. */
    public static boolean canPay(ServerPlayer p, int tier, int cost) {
        BloodData d = get(p);
        return BloodRules.activeTier(d) >= tier && d.blood() >= BloodRules.cost(cost);
    }

    public static void pay(ServerPlayer p, int cost) {
        update(p, d -> d.withBlood(BloodRules.clamp(d.blood() - BloodRules.cost(cost))));
    }

    /** The cure (the Panic Room): the blood and the dependency gone; the powers learnt (lifetime doses) are kept. */
    public static void cure(ServerPlayer p) {
        update(p, d -> d.withBlood(0).withDependency(0));
        p.removeEffect(org.papiricoh.supernaturalcraft.registry.AllMobEffects.WITHDRAWAL);
        ChorusRewards.award(p, "main/the_panic_room");
    }
}
