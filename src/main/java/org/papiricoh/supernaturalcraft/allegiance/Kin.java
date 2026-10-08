package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Who counts as what, players included (a player can't be in an entity-type tag). Every weakness and every rule that
 * asked {@code getType().is(DEMONS)} asks this instead, so a demon player is held by salt and a devil's trap, burnt by
 * holy water and expelled by an exorcism; an angel player is an angel to the Host and to holy oil.
 */
public final class Kin {

    private Kin() {
    }

    public static boolean isDemon(Entity e) {
        if (e instanceof Player p) return Allegiances.get(p).isDemon();
        return e.getType().is(AllTags.Entities.DEMONS);
    }

    public static boolean isAngel(Entity e) {
        if (e instanceof Player p) return Allegiances.get(p).isAngel();
        return e.getType().is(AllTags.Entities.ANGELS);
    }

    /** A human player: free will (no possession, no Heaven's mark, Michael never asks). */
    public static boolean freeWill(Entity e) {
        return e instanceof Player p && Allegiances.get(p).isHuman();
    }

    /** A player sworn to Heaven or Hell. */
    public static boolean sworn(Entity e) {
        return e instanceof Player p && Allegiances.get(p).committed();
    }

    /** Whether two entities are on the same supernatural side (a demon and a demon player; the Host and an angel). */
    public static boolean sameSide(Entity a, Entity b) {
        return isDemon(a) && isDemon(b) || isAngel(a) && isAngel(b);
    }

    /** Whether they are on opposite supernatural sides. */
    public static boolean opposed(Entity a, Entity b) {
        return isDemon(a) && isAngel(b) || isAngel(a) && isDemon(b);
    }
}
