package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.ConsumptionPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How far the Darkness has seeped into each challenger. It rises in the dark and in her void, and
 * light drives it back: past 30 the world closes in, past 60 it drags at you and drinks your
 * mana, and at 100 it eats you.
 */
public final class Consumption {

    /** Updated twice a second. */
    public static final int INTERVAL = 10;
    private static final Map<UUID, Float> VALUES = new HashMap<>();
    private static final Map<UUID, Float> SENT = new HashMap<>();

    private Consumption() {
    }

    public static float get(ServerPlayer p) {
        return VALUES.getOrDefault(p.getUUID(), 0f);
    }

    public static void set(ServerPlayer p, float value) {
        VALUES.put(p.getUUID(), AmaraBalance.clampConsumption(value));
        sync(p, false);
    }

    public static int lightAt(ServerPlayer p) {
        return p.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(p.getEyePosition()));
    }

    /** One step for one challenger; {@code inVoid} when they stand in one of her void zones. */
    public static void tick(AmaraEntity boss, ServerPlayer p, boolean inVoid) {
        float delta = org.papiricoh.supernaturalcraft.allegiance.BossTwists.consumption(p, AmaraBalance.consumptionPerSecond(lightAt(p), inVoid) * INTERVAL / 20f);
        org.papiricoh.supernaturalcraft.allegiance.BossTwists.amaraDrain(p, INTERVAL);
        float c = AmaraBalance.clampConsumption(get(p) + delta);
        VALUES.put(p.getUUID(), c);
        if (c >= AmaraBalance.DARKNESS_AT) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        if (c >= AmaraBalance.SLOW_AT) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            ArcanaData arcana = ManaManager.get(p);
            arcana.setMana(Math.max(0, arcana.mana() - 0.5f * INTERVAL / 20f));
        }
        if (c >= AmaraBalance.HARM_AT) p.hurt(AllDamageTypes.source(p.level(), AllDamageTypes.VOID, boss), 4f * INTERVAL / 20f);
        sync(p, false);
    }

    private static void sync(ServerPlayer p, boolean force) {
        float now = get(p), last = SENT.getOrDefault(p.getUUID(), -100f);
        boolean crossed = band(now) != band(last);
        if (force || crossed || Math.abs(now - last) >= 5 || (now == 0 && last != 0)) {
            SENT.put(p.getUUID(), now);
            PacketDistributor.sendToPlayer(p, new ConsumptionPayload(now));
        }
    }

    private static int band(float c) {
        return c >= AmaraBalance.HARM_AT ? 3 : c >= AmaraBalance.SLOW_AT ? 2 : c >= AmaraBalance.DARKNESS_AT ? 1 : 0;
    }

    /** The fight is over for this player: the Darkness lets go. */
    public static void release(ServerPlayer p) {
        VALUES.remove(p.getUUID());
        SENT.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new ConsumptionPayload(0));
    }
}
