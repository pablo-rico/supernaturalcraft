package org.papiricoh.supernaturalcraft.entity.boss.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.network.VesselAnswerPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "I need your yes." Michael asks one hunter at a time; the question waits here (who was asked, by which Michael, when)
 * until an answer comes in time ({@link MichaelBalance#answerInTime}) or the time runs out, which counts as no.
 * <ul>
 *   <li>Yes: he wears the hunter ({@code vessel}, {@link MichaelBalance#POSSESS_TICKS}). The server walks the body to the
 *   nearest ally and pulses light from it that burns allies as Michael's own damage (so it needs no PvP); alone, the body
 *   stands still. He heals while he wears them. When he lets go he leaves his grace's favour ({@code grace_favor}: double
 *   damage against him).</li>
 *   <li>No, or silence: Heaven's mark ({@code heavens_mark}): the Host hunts that hunter.</li>
 * </ul>
 */
public final class VesselPossession {

    /** A question in the air: asked by Michael {@code michael} (entity id) at game time {@code askedAt}. */
    public record Question(int michael, long askedAt, ServerPlayer hunter) {
    }

    /** {@code hunter}, worn by Michael {@code michael} until game time {@code until}. */
    public record Possession(int michael, long until, ServerPlayer hunter) {
    }

    /** Ticks between two pulses of light from a worn body. */
    public static final int PULSE_TICKS = 20;
    public static final float PULSE_DAMAGE = 6f;
    public static final double PULSE_RADIUS = 3.5;

    private static final Map<UUID, Question> ASKED = new HashMap<>();
    private static final Map<UUID, Possession> WORN = new HashMap<>();

    private VesselPossession() {
    }

    /** He asks {@code hunter}: the question goes to their screen and waits here. */
    public static void ask(ServerPlayer hunter, MichaelEntity michael) {
        ASKED.put(hunter.getUUID(), new Question(michael.getId(), hunter.serverLevel().getGameTime(), hunter));
        PacketDistributor.sendToPlayer(hunter, new MichaelFxPayload(michael.getId(), MichaelFxPayload.ASK_YES, 0, 0, michael.position(),
                MichaelBalance.YES_DECIDE_TICKS));
        hunter.serverLevel().playSound(null, hunter.blockPosition(), AllSounds.MICHAEL_ASK_YES.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.asked").withStyle(ChatFormatting.AQUA,
                ChatFormatting.ITALIC), false);
    }

    public static @Nullable Question pending(ServerPlayer hunter) {
        return ASKED.get(hunter.getUUID());
    }

    public static void forget(ServerPlayer hunter) {
        ASKED.remove(hunter.getUUID());
    }

    /** Whether Michael is wearing {@code hunter} now. */
    public static boolean worn(LivingEntity hunter) {
        return WORN.containsKey(hunter.getUUID());
    }

    /** A hunter's answer: ignored unless this Michael asked them and it comes in time. */
    public static void answer(VesselAnswerPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer hunter)) return;
        answer(hunter, payload.entity(), payload.yes());
    }

    /** {@link #answer(VesselAnswerPayload, IPayloadContext)} without the network (tests). Returns whether it counted. */
    public static boolean answer(ServerPlayer hunter, int michaelId, boolean yes) {
        Question q = ASKED.get(hunter.getUUID());
        if (q == null || q.michael() != michaelId) return false;
        if (!MichaelBalance.answerInTime(q.askedAt(), hunter.serverLevel().getGameTime())) return false;
        ASKED.remove(hunter.getUUID());
        if (!(hunter.serverLevel().getEntity(michaelId) instanceof MichaelEntity michael) || !michael.isAlive()) return false;
        resolve(michael, hunter, yes);
        return true;
    }

    /** What the answer does (also called with {@code yes = false} when the time runs out). */
    public static void resolve(MichaelEntity michael, ServerPlayer hunter, boolean yes) {
        ServerLevel level = hunter.serverLevel();
        ASKED.remove(hunter.getUUID());
        if (yes) {
            WORN.put(hunter.getUUID(), new Possession(michael.getId(), level.getGameTime() + MichaelBalance.POSSESS_TICKS, hunter));
            hunter.addEffect(new MobEffectInstance(AllMobEffects.VESSEL, MichaelBalance.POSSESS_TICKS, 0, false, true, true));
            PacketDistributor.sendToPlayer(hunter, new MichaelFxPayload(hunter.getId(), MichaelFxPayload.POSSESSED, 1, 0, hunter.position(),
                    MichaelBalance.POSSESS_TICKS));
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.possessed").withStyle(ChatFormatting.AQUA), false);
            level.playSound(null, hunter.blockPosition(), AllSounds.MICHAEL_CHOIR.get(), SoundSource.HOSTILE, 2f, 1.2f);
            level.sendParticles(ParticleTypes.END_ROD, hunter.getX(), hunter.getY() + 1, hunter.getZ(), 40, 0.4, 1, 0.4, 0.1);
        } else {
            hunter.addEffect(new MobEffectInstance(AllMobEffects.HEAVENS_MARK, MichaelBalance.MARK_TICKS, 0, false, true, true));
            michael.fx(level, new MichaelFxPayload(michael.getId(), MichaelFxPayload.MARK, hunter.getId(), 0, hunter.position(),
                    MichaelBalance.MARK_TICKS));
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.refused").withStyle(ChatFormatting.GOLD), false);
            level.playSound(null, hunter.blockPosition(), AllSounds.MICHAEL_TRUMPET.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
        }
    }

    /**
     * Each tick of Michael's fight: unanswered questions run out (no), worn bodies walk and burn, he heals, and the
     * ones he is done with are let go with his favour.
     */
    public static void tick(MichaelEntity michael, ServerLevel level) {
        long now = level.getGameTime();
        for (var e : List.copyOf(ASKED.entrySet())) {
            Question q = e.getValue();
            if (q.michael() != michael.getId()) continue;
            if (now - q.askedAt() > MichaelBalance.YES_DECIDE_TICKS + MichaelBalance.YES_GRACE_TICKS) {
                ASKED.remove(e.getKey());
                if (q.hunter().isAlive() && !q.hunter().isRemoved()) resolve(michael, q.hunter(), false);
            }
        }
        for (var e : List.copyOf(WORN.entrySet())) {
            Possession w = e.getValue();
            if (w.michael() != michael.getId()) continue;
            ServerPlayer hunter = w.hunter();
            if (!hunter.isAlive() || hunter.isRemoved()) {
                WORN.remove(e.getKey());
                continue;
            }
            if (now >= w.until()) release(michael, hunter, true);
            else wear(michael, hunter, level, w.until() - now);
        }
    }

    /** One tick of a worn body: it walks to the nearest ally and pulses light; he heals. */
    static void wear(MichaelEntity michael, ServerPlayer hunter, ServerLevel level, long left) {
        List<LivingEntity> allies = allies(michael, hunter);
        LivingEntity near = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity a : allies) {
            double d = a.distanceToSqr(hunter);
            if (d < best) {
                best = d;
                near = a;
            }
        }
        if (near != null && best > 1.5 * 1.5) {
            Vec3 to = near.position().subtract(hunter.position()).multiply(1, 0, 1).normalize().scale(0.22);
            hunter.setDeltaMovement(to.x, hunter.getDeltaMovement().y, to.z);
            hunter.hurtMarked = true;
            hunter.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, near.getEyePosition());
        } else if (near == null) {
            // Alone: the body stands still while he heals in it.
            hunter.setDeltaMovement(0, hunter.getDeltaMovement().y, 0);
            hunter.hurtMarked = true;
        }
        level.sendParticles(AllParticles.GRACE.get(), hunter.getX(), hunter.getY() + 1.2, hunter.getZ(), 2, 0.3, 0.6, 0.3, 0.02);
        if (left % PULSE_TICKS == 0) {
            level.sendParticles(ParticleTypes.END_ROD, hunter.getX(), hunter.getY() + 1, hunter.getZ(), 30, PULSE_RADIUS / 2, 0.4,
                    PULSE_RADIUS / 2, 0.05);
            for (LivingEntity a : allies) {
                if (a.distanceToSqr(hunter) <= PULSE_RADIUS * PULSE_RADIUS) LuciferAttacks.hit(michael, a, AllDamageTypes.SMITE, PULSE_DAMAGE);
            }
            michael.healTrue(MichaelBalance.POSSESS_HEAL_PER_SECOND * michael.trueMaxHealth() * PULSE_TICKS / 20f);
        }
    }

    /** The worn hunter's allies: his other challengers. */
    private static List<LivingEntity> allies(MichaelEntity michael, ServerPlayer hunter) {
        List<LivingEntity> out = new ArrayList<>();
        for (ServerPlayer p : michael.challengers()) if (p != hunter && p.isAlive() && !worn(p)) out.add(p);
        return out;
    }

    /** He lets go of {@code hunter}, leaving his favour if the possession ran its course. */
    public static void release(MichaelEntity michael, ServerPlayer hunter, boolean favour) {
        WORN.remove(hunter.getUUID());
        hunter.removeEffect(AllMobEffects.VESSEL);
        PacketDistributor.sendToPlayer(hunter, new MichaelFxPayload(hunter.getId(), MichaelFxPayload.POSSESSED, 0, 0, hunter.position(), 0));
        if (!favour) return;
        hunter.addEffect(new MobEffectInstance(AllMobEffects.GRACE_FAVOR, MichaelBalance.FAVOR_TICKS, 0, false, true, true));
        PacketDistributor.sendToPlayer(hunter, new MichaelFxPayload(michael.getId(), MichaelFxPayload.FAVOR, 0, 0, hunter.position(),
                MichaelBalance.FAVOR_TICKS));
        hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.michael.released").withStyle(ChatFormatting.GOLD), false);
        hunter.serverLevel().sendParticles(AllParticles.GRACE.get(), hunter.getX(), hunter.getY() + 1, hunter.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
    }

    /** Lets go of everyone he wears and forgets his questions (he died, or the fight ended). */
    public static void releaseAll(MichaelEntity michael, ServerLevel level) {
        ASKED.values().removeIf(q -> q.michael() == michael.getId());
        for (var e : List.copyOf(WORN.entrySet())) {
            if (e.getValue().michael() != michael.getId()) continue;
            release(michael, e.getValue().hunter(), false);
        }
    }

    /** Whether this Michael has a question waiting or a body on. */
    public static boolean busy(MichaelEntity michael) {
        return ASKED.values().stream().anyMatch(q -> q.michael() == michael.getId())
                || WORN.values().stream().anyMatch(w -> w.michael() == michael.getId());
    }
}
