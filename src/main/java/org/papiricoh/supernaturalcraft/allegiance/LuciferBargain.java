package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lucifer's offer to a demon (v0.13): "Serve me." It is a trap. Saying yes ({@code serve}) lets him wear the demon for
 * {@link #POSSESS_TICKS} (the movement and pulses of Michael's {@code VesselPossession.wear}): the body walks at the other
 * hunters and burns them with hellfire, he heals while he wears it, and then lets go; the fight goes on. Saying no
 * ({@code refuse}), or nothing, only earns a line. Dialogue {@code "lucifer_offer"}, node {@code offer}.
 */
public final class LuciferBargain {

    public static final String DIALOGUE = "lucifer_offer";
    public static final int POSSESS_TICKS = 120, ANSWER_TICKS = 200;
    public static final int PULSE_TICKS = 20;
    public static final float PULSE_DAMAGE = 6f;
    public static final double PULSE_RADIUS = 3.5;
    /** Share of his max health he heals each pulse while he wears the demon. */
    public static final float HEAL_PER_PULSE = 0.01f;

    /** {@code player}, worn by Lucifer {@code lucifer} (entity id) until game time {@code until}. */
    public record Possession(int lucifer, long until, ServerPlayer player) {
    }

    private static final Map<UUID, Possession> WORN = new ConcurrentHashMap<>();

    private LuciferBargain() {
    }

    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, (player, speaker, node, option) -> {
            if (speaker instanceof LuciferEntity lucifer && lucifer.isAlive() && "serve".equals(option)) wear(player, lucifer);
            else refused(player);
        });
    }

    /** He makes the offer. */
    public static void offer(ServerPlayer player, LuciferEntity lucifer) {
        AllegianceDialogue.ask(player, lucifer, DIALOGUE, "offer", List.of("serve", "refuse"), ANSWER_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.LUCIFER_WINGS.get(), SoundSource.HOSTILE, 1f, 0.6f);
    }

    static void refused(ServerPlayer player) {
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.boss.lucifer.refused", false, ChatFormatting.RED);
    }

    /** "Yes": he wears the demon. */
    public static void wear(ServerPlayer player, LuciferEntity lucifer) {
        ServerLevel level = player.serverLevel();
        WORN.put(player.getUUID(), new Possession(lucifer.getId(), level.getGameTime() + POSSESS_TICKS, player));
        player.addEffect(new MobEffectInstance(AllMobEffects.VESSEL, POSSESS_TICKS, 0, false, true, true));
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new MichaelFxPayload(player.getId(), MichaelFxPayload.POSSESSED, 1, 0, player.position(), POSSESS_TICKS));
        }
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.boss.lucifer.trap", false, ChatFormatting.DARK_RED);
        level.playSound(null, player.blockPosition(), AllSounds.ALLEGIANCE_ASCEND_DEMON.get(), SoundSource.HOSTILE, 1.5f, 0.6f);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 1, 0.4, 0.05);
    }

    public static boolean worn(Entity player) {
        return WORN.containsKey(player.getUUID());
    }

    public static @Nullable Possession possession(Entity player) {
        return WORN.get(player.getUUID());
    }

    /** Every tick: worn bodies walk and burn, he heals, the time runs out. */
    public static void tick(ServerLevel level) {
        if (WORN.isEmpty()) return;
        long now = level.getGameTime();
        for (var e : List.copyOf(WORN.entrySet())) {
            Possession w = e.getValue();
            ServerPlayer p = w.player();
            if (p.serverLevel() != level) continue;
            if (!p.isAlive() || p.isRemoved() || !(level.getEntity(w.lucifer()) instanceof LuciferEntity lucifer) || !lucifer.isAlive()) {
                release(p);
                continue;
            }
            if (now >= w.until()) release(p);
            else step(lucifer, p, level, w.until() - now);
        }
    }

    /** One tick of a worn body (as {@code VesselPossession.wear}): to the nearest ally, a pulse each second, he heals. */
    static void step(LuciferEntity lucifer, ServerPlayer player, ServerLevel level, long left) {
        List<LivingEntity> allies = new ArrayList<>();
        for (ServerPlayer p : lucifer.challengers()) if (p != player && p.isAlive() && !worn(p)) allies.add(p);
        LivingEntity near = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity a : allies) {
            double d = a.distanceToSqr(player);
            if (d < best) {
                best = d;
                near = a;
            }
        }
        if (near != null && best > 1.5 * 1.5) {
            Vec3 to = near.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.22);
            player.setDeltaMovement(to.x, player.getDeltaMovement().y, to.z);
            player.hurtMarked = true;
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, near.getEyePosition());
        } else if (near == null) {
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            player.hurtMarked = true;
        }
        level.sendParticles(AllParticles.HELLFIRE.get(), player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.3, 0.6, 0.3, 0.02);
        if (left % PULSE_TICKS == 0) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, PULSE_RADIUS / 2, 0.4, PULSE_RADIUS / 2, 0.05);
            for (LivingEntity a : allies) {
                if (a.distanceToSqr(player) <= PULSE_RADIUS * PULSE_RADIUS) LuciferAttacks.hit(lucifer, a, AllDamageTypes.HELLFIRE, PULSE_DAMAGE);
            }
            lucifer.setHealth(Math.min(lucifer.getMaxHealth(), lucifer.getHealth() + lucifer.getMaxHealth() * HEAL_PER_PULSE));
        }
    }

    /** He lets go. */
    public static void release(ServerPlayer player) {
        if (WORN.remove(player.getUUID()) == null) return;
        player.removeEffect(AllMobEffects.VESSEL);
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new MichaelFxPayload(player.getId(), MichaelFxPayload.POSSESSED, 0, 0, player.position(), 0));
        }
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.boss.lucifer.released", false, ChatFormatting.DARK_RED);
    }
}
