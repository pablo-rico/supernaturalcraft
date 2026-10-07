package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "And the hunter grew light" / "and the sky became the floor": the Author's gravity rules on the hunters' own
 * {@link Attributes#GRAVITY}, as a transient modifier (never saved). Whoever holds it is remembered here so that it is
 * always taken off: when the rule ends, when he dies or his arena closes ({@link ChuckEntity#clearRules}), when the hunter
 * logs out, and by a sweep of anyone left holding it with no rule behind it. Falls do not hurt for a while after.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ChuckGravity {

    public static final ResourceLocation MODIFIER = SupernaturalCraft.asResource("author_gravity");

    /** Hunter → the Author whose rule they are under. */
    private static final Map<UUID, UUID> HELD = new HashMap<>();
    /** Hunter → game time until which falls are forgiven. */
    private static final Map<UUID, Long> NO_FALL = new HashMap<>();

    private ChuckGravity() {
    }

    /** The multiplier a gravity mode puts on the hunters' gravity. */
    public static double multiplier(byte mode) {
        return switch (mode) {
            case ChuckEntity.GRAVITY_LOW -> ChuckBalance.LOW_GRAVITY;
            case ChuckEntity.GRAVITY_INVERTED -> ChuckBalance.INVERTED_GRAVITY;
            default -> 0;
        };
    }

    /** Puts {@code who} under {@code mode} (replacing any rule they were under). */
    public static void apply(ChuckEntity author, LivingEntity who, byte mode) {
        AttributeInstance gravity = who.getAttribute(Attributes.GRAVITY);
        if (gravity == null) return;
        gravity.removeModifier(MODIFIER);
        if (mode == ChuckEntity.GRAVITY_NORMAL) {
            HELD.remove(who.getUUID());
            return;
        }
        gravity.addTransientModifier(new AttributeModifier(MODIFIER, multiplier(mode), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        HELD.put(who.getUUID(), author.getUUID());
    }

    /** Takes the rule off {@code who}; their next falls are forgiven for {@code forgiveTicks}. */
    public static void release(LivingEntity who, int forgiveTicks) {
        AttributeInstance gravity = who.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(MODIFIER);
        HELD.remove(who.getUUID());
        if (forgiveTicks > 0) NO_FALL.put(who.getUUID(), who.level().getGameTime() + forgiveTicks);
        who.fallDistance = 0;
    }

    /** Whether {@code who} carries the Author's gravity right now. */
    public static boolean affected(LivingEntity who) {
        AttributeInstance gravity = who.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(MODIFIER);
    }

    public static boolean forgiven(LivingEntity who) {
        Long until = NO_FALL.get(who.getUUID());
        return until != null && who.level().getGameTime() < until;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity who = event.getEntity();
        if (affected(who) || forgiven(who)) {
            event.setDistance(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        release(event.getEntity(), 0);
        NO_FALL.remove(event.getEntity().getUUID());
    }

    /** Once a second: nobody keeps a rule whose Author is gone or has taken it back. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (!affected(p)) continue;
            UUID by = HELD.get(p.getUUID());
            ChuckEntity author = by == null ? null : ChuckEntity.find(p.level(), by);
            if (author == null || !author.isAlive() || author.gravityMode() == ChuckEntity.GRAVITY_NORMAL) {
                release(p, ChuckBalance.NO_FALL_AFTER);
            }
        }
        long now = event.getServer().overworld().getGameTime();
        NO_FALL.values().removeIf(until -> until < now - 200);
    }
}
