package org.papiricoh.supernaturalcraft.bowl.witch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.spell.Bindings;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;

import java.util.Optional;

/**
 * Witchcraft by name (v0.21): finding whom a bowl spell is aimed at from the personal component in the bowl
 * ({@link BowlCast#target()}), and the rules every such spell keeps. A target must be loaded somewhere on the server; spells with
 * a {@code reach} also need it in the bowl's world and within that many blocks. Bosses are never fully moved by a curse or a
 * control spell (each spell says what, if anything, it does to one), and another player can only be cursed or controlled when
 * the server allows PvP ({@link #mayAfflict}).
 */
public final class NameMagic {

    /** How far (blocks, same world) the spells that seize a body reach: binding, banishing, compelling. */
    public static final double REACH = 256;

    private NameMagic() {
    }

    /** What finding the target came to. */
    public enum Found { FOUND, NO_TARGET, LOST, ELSEWHERE, TOO_FAR }

    public record Lookup(Found found, @Nullable LivingEntity entity, String name) {
        public boolean ok() {
            return found == Found.FOUND && entity != null;
        }
    }

    /** The entity with this id in any loaded world, or null. */
    public static @Nullable Entity anywhere(ServerLevel from, java.util.UUID id) {
        Entity here = from.getEntity(id);
        if (here != null) return here;
        for (ServerLevel l : from.getServer().getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /**
     * Whom the bowl names. With {@code reach > 0} the target must be in the bowl's world and that close to it; with
     * {@code reach == 0} it only needs to be in the bowl's world; with {@code reach < 0}, anywhere.
     */
    public static Lookup find(BowlCast cast, double reach) {
        Optional<Taglock> tag = cast.target();
        if (tag.isEmpty()) return new Lookup(Found.NO_TARGET, null, "");
        String name = tag.get().name();
        Entity e = anywhere(cast.level(), tag.get().owner());
        if (!(e instanceof LivingEntity living) || !living.isAlive()) return new Lookup(Found.LOST, null, name);
        if (reach >= 0 && living.level() != cast.level()) return new Lookup(Found.ELSEWHERE, living, name);
        if (reach > 0 && living.distanceToSqr(Vec3.atCenterOf(cast.bowl())) > reach * reach) return new Lookup(Found.TOO_FAR, living, name);
        return new Lookup(Found.FOUND, living, living.getName().getString());
    }

    /** Tells the caster why the target could not be reached. */
    public static void explain(BowlCast cast, Lookup l) {
        String key = switch (l.found()) {
            case NO_TARGET -> "message.supernaturalcraft.bowl.needs_target";
            case LOST -> "message.supernaturalcraft.witchcraft.lost";
            case ELSEWHERE -> "message.supernaturalcraft.witchcraft.elsewhere";
            case TOO_FAR -> "message.supernaturalcraft.witchcraft.too_far";
            case FOUND -> null;
        };
        if (key != null) cast.caster().displayClientMessage(Component.translatable(key, l.name()).withStyle(ChatFormatting.GRAY), false);
    }

    /** Whether {@code victim} is a boss (never fully taken by a curse or a control spell). */
    public static boolean boss(Entity victim) {
        return Bindings.isBoss(victim);
    }

    /** Whether {@code caster} may curse or control {@code victim}: never a boss; another player only with PvP on. */
    public static boolean mayAfflict(ServerPlayer caster, LivingEntity victim) {
        if (boss(victim)) return false;
        if (victim instanceof Player && victim != caster) return caster.server.isPvpAllowed();
        return true;
    }

    /** The message for a victim that may not be afflicted. */
    public static void refuse(BowlCast cast, LivingEntity victim) {
        String key = boss(victim) ? "message.supernaturalcraft.witchcraft.boss" : "message.supernaturalcraft.witchcraft.no_pvp";
        cast.caster().displayClientMessage(Component.translatable(key, victim.getName()).withStyle(ChatFormatting.GRAY), false);
    }

    /** Where the seal flies to: the target's chest. */
    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }
}
