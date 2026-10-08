package org.papiricoh.supernaturalcraft.trickster;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Trickster's marks on a creature (v0.14), all temporary: a party hat (attachment {@code PARTY_HAT} = the game time it
 * comes off, shown with {@link GabrielFxPayload#HAT}), a shrink (a transient {@code SCALE} modifier, so a reload undoes it
 * too) and a TV name (a custom name, remembered in the creature's persistent data so it is taken off even after a reload).
 * Expiries are looked at every second ({@link #tick}).
 */
public final class TricksterMarks {

    public static final ResourceLocation SHRINK = SupernaturalCraft.asResource("trickster_shrink");
    /** The shrink: the creature at half its size. */
    public static final double SHRINK_AMOUNT = -0.5;
    /** Persistent data: the game time a TV name comes off, and the name itself (so a real rename is never touched). */
    public static final String NAME_UNTIL = "supernaturalcraft_tv_name_until", NAME_TEXT = "supernaturalcraft_tv_name";

    private record Mark(ResourceKey<Level> level, long until) {
    }

    private static final Map<UUID, Mark> HATS = new ConcurrentHashMap<>();
    private static final Map<UUID, Mark> SHRUNK = new ConcurrentHashMap<>();
    private static final Map<UUID, Mark> NAMED = new ConcurrentHashMap<>();

    private TricksterMarks() {
    }

    // --- the party hat -------------------------------------------------------------------------------------------------

    /** Puts a party hat on {@code e} for {@code ticks}, for everyone who can see it. */
    public static void hat(LivingEntity e, int ticks) {
        long until = e.level().getGameTime() + ticks;
        e.setData(AllAttachments.PARTY_HAT, until);
        HATS.put(e.getUUID(), new Mark(e.level().dimension(), until));
        PacketDistributor.sendToPlayersTrackingEntity(e, new GabrielFxPayload(e.getId(), GabrielFxPayload.HAT, 0, 0, e.position(), ticks));
    }

    /** Whether {@code e} wears a hat now. */
    public static boolean hatted(Entity e) {
        long until = e.getData(AllAttachments.PARTY_HAT);
        return until > e.level().getGameTime();
    }

    private static void unhat(Entity e) {
        e.setData(AllAttachments.PARTY_HAT, 0L);
        PacketDistributor.sendToPlayersTrackingEntity(e, new GabrielFxPayload(e.getId(), GabrielFxPayload.HAT, 0, 0, e.position(), 0));
    }

    /** Someone new sees {@code target}: show them its hat, if it wears one. */
    public static void onStartTracking(Entity target, ServerPlayer viewer) {
        if (!target.hasData(AllAttachments.PARTY_HAT)) return;
        long left = target.getData(AllAttachments.PARTY_HAT) - target.level().getGameTime();
        if (left > 0) {
            PacketDistributor.sendToPlayer(viewer, new GabrielFxPayload(target.getId(), GabrielFxPayload.HAT, 0, 0, target.position(), (int) left));
        }
    }

    // --- the shrink -----------------------------------------------------------------------------------------------------

    /** Shrinks {@code e} to half its size for {@code ticks}. @return whether it took (no scale attribute, no shrink) */
    public static boolean shrink(LivingEntity e, int ticks) {
        AttributeInstance scale = e.getAttribute(Attributes.SCALE);
        if (scale == null) return false;
        scale.addOrUpdateTransientModifier(new AttributeModifier(SHRINK, SHRINK_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        SHRUNK.put(e.getUUID(), new Mark(e.level().dimension(), e.level().getGameTime() + ticks));
        return true;
    }

    public static boolean shrunk(LivingEntity e) {
        AttributeInstance scale = e.getAttribute(Attributes.SCALE);
        return scale != null && scale.hasModifier(SHRINK);
    }

    private static void unshrink(LivingEntity e) {
        AttributeInstance scale = e.getAttribute(Attributes.SCALE);
        if (scale != null) scale.removeModifier(SHRINK);
    }

    // --- the TV name ----------------------------------------------------------------------------------------------------

    /** Names {@code e} for {@code ticks}, as a character off the TV. */
    public static void tvName(LivingEntity e, String name, int ticks) {
        long until = e.level().getGameTime() + ticks;
        e.setCustomName(Component.literal(name));
        e.getPersistentData().putLong(NAME_UNTIL, until);
        e.getPersistentData().putString(NAME_TEXT, name);
        NAMED.put(e.getUUID(), new Mark(e.level().dimension(), until));
    }

    private static void unname(Entity e) {
        String mine = e.getPersistentData().getString(NAME_TEXT);
        if (e.getCustomName() != null && e.getCustomName().getString().equals(mine)) e.setCustomName(null);
        e.getPersistentData().remove(NAME_UNTIL);
        e.getPersistentData().remove(NAME_TEXT);
    }

    /** A creature comes (back) into the world: its marks are looked after again, or taken off if their time is up. */
    public static void onJoin(Entity e) {
        if (e.level().isClientSide) return;
        long now = e.level().getGameTime();
        if (e.getPersistentData().contains(NAME_UNTIL)) {
            long until = e.getPersistentData().getLong(NAME_UNTIL);
            if (until <= now) unname(e);
            else NAMED.put(e.getUUID(), new Mark(e.level().dimension(), until));
        }
        if (e.hasData(AllAttachments.PARTY_HAT)) {
            long until = e.getData(AllAttachments.PARTY_HAT);
            if (until > now) HATS.put(e.getUUID(), new Mark(e.level().dimension(), until));
            else if (until != 0) e.setData(AllAttachments.PARTY_HAT, 0L);
        }
    }

    /** Every second: whatever's time is up comes off. */
    public static void tick(MinecraftServer server) {
        expire(server, HATS, TricksterMarks::unhat);
        expire(server, SHRUNK, e -> {
            if (e instanceof LivingEntity l) unshrink(l);
        });
        expire(server, NAMED, TricksterMarks::unname);
    }

    private static void expire(MinecraftServer server, Map<UUID, Mark> marks, java.util.function.Consumer<Entity> off) {
        for (var it = marks.entrySet().iterator(); it.hasNext(); ) {
            var m = it.next();
            ServerLevel level = server.getLevel(m.getValue().level());
            if (level == null) {
                it.remove();
                continue;
            }
            if (level.getGameTime() < m.getValue().until()) continue;
            Entity e = level.getEntity(m.getKey());
            // Not loaded: its marks are looked at again when it comes back (onJoin); a shrink is gone on its own by then.
            if (e != null) off.accept(e);
            it.remove();
        }
    }
}
