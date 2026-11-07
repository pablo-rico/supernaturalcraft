package org.papiricoh.supernaturalcraft.nephilim.michael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.41: the yes. Michael wears the hunter for {@link #WEAR_TICKS} (his strength, his speed, his light round them, his voice in
 * their ear); then he takes the wheel, and the hunter wakes inside their own head ({@link Mindscape}) with Michael's projection
 * between them and the way out. Throwing him out ({@link #expelled}) puts them back where he took them. Memory only: after a
 * restart the step is still {@code OFFERED}, and Michael comes at the next dawn to ask again.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class MichaelPossession {

    public static final int WEAR_TICKS = 20 * 60;
    /** Lines of his voice while he wears them ({@code message.supernaturalcraft.michael_vessel.wear.<n>}). */
    public static final int LINES = 4;
    private static final int VOICE_EVERY = 20 * 12;

    private record Worn(long until, GlobalPos from) {
    }

    private static final Map<UUID, Worn> WORN = new ConcurrentHashMap<>();
    /** Where each hunter in the mindscape came from (to put them back). */
    private static final Map<UUID, GlobalPos> TAKEN = new ConcurrentHashMap<>();

    private MichaelPossession() {
    }

    public static boolean worn(ServerPlayer p) {
        return WORN.containsKey(p.getUUID());
    }

    public static boolean busy(ServerPlayer p) {
        return worn(p) || TAKEN.containsKey(p.getUUID());
    }

    public static void begin(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        WORN.put(p.getUUID(), new Worn(now + WEAR_TICKS, GlobalPos.of(p.level().dimension(), p.blockPosition())));
        int t = WEAR_TICKS + 20;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, t, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, t, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, t, 1));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, t, 1));
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, t, 0));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael_vessel.yes").withStyle(ChatFormatting.AQUA), false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (WORN.isEmpty() || server.overworld().getGameTime() % 20 != 3) return;
        long now = server.overworld().getGameTime();
        for (var e : Map.copyOf(WORN).entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                WORN.remove(e.getKey());
                continue;
            }
            ServerLevel level = p.serverLevel();
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.6, 0.4, 0.02);
            long left = e.getValue().until() - now;
            if ((WEAR_TICKS - left) % VOICE_EVERY < 20) {
                int n = (int) ((WEAR_TICKS - left) / VOICE_EVERY) % LINES;
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael_vessel.wear." + n)
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);
            }
            if (left > 0) continue;
            if (Mindscape.enter(p)) {
                WORN.remove(e.getKey());
                TAKEN.put(p.getUUID(), e.getValue().from());
            } else {
                // Another fight holds the world: he keeps them a little longer.
                WORN.put(e.getKey(), new Worn(now + 20 * 30, e.getValue().from()));
            }
        }
    }

    /** They threw him out: back where he took them. */
    public static void expelled(MinecraftServer server, Collection<ServerPlayer> hunters) {
        for (ServerPlayer p : hunters) {
            GlobalPos from = TAKEN.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.michael_vessel.expelled").withStyle(ChatFormatting.GOLD), false);
            if (from == null) continue;
            ServerLevel level = server.getLevel(from.dimension());
            if (level == null) level = server.overworld();
            Vec3 at = Vec3.atBottomCenterOf(from.pos());
            p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        }
    }

    /** Forgets {@code p} (left the mindscape any other way: death, logout). */
    public static void release(UUID p) {
        WORN.remove(p);
        TAKEN.remove(p);
    }

    static ResourceKey<Level> home() {
        return Level.OVERWORLD;
    }

    static BlockPos origin() {
        return Mindscape.ORIGIN;
    }
}
