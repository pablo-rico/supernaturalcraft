package org.papiricoh.supernaturalcraft.coven.ending;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlCast;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The bowl spell {@code unmark} (v0.21, P6): the Book of the Damned's cure for the Mark of Cain. The bowl must name the Marked
 * (a vial of their bound blood: the recipe's ingredient), the survivor of their witch boss fight must stand within
 * {@link Unmarking#SURVIVOR_REACH} of the bowl (Rowena, or the Grand Witch), and — for a hunter who kept the Book — the Book must
 * be at hand (the caster's or the Marked's pack; it is never consumed). See {@link Unmarking}.
 */
public record UnmarkEffect() implements BowlSpellEffect {

    public static final ResourceLocation ID = SupernaturalCraft.asResource("unmark");
    public static final MapCodec<UnmarkEffect> CODEC = MapCodec.unit(new UnmarkEffect());

    @Override
    public ResourceLocation type() {
        return ID;
    }

    /** The Marked the bowl names, if they are online (the caster, a fake player in tests, counts). */
    static @Nullable ServerPlayer marked(BowlCast cast) {
        Taglock t = cast.target().orElse(null);
        if (t == null) return null;
        if (t.owner().equals(cast.caster().getUUID())) return cast.caster();
        return cast.level().getServer().getPlayerList().getPlayer(t.owner());
    }

    @Override
    public @Nullable String precheck(BowlCast cast) {
        ServerPlayer target = marked(cast);
        if (target == null) return "message.supernaturalcraft.bowl.needs_target";
        String why = Unmarking.why(target, Unmarking.holdsBook(cast.caster()) || Unmarking.holdsBook(target), false);
        if (why != null) return why;
        Vec3 at = Vec3.atCenterOf(cast.bowl());
        return Unmarking.survivorNear(cast.level(), at, target) == null ? "message.supernaturalcraft.unmark.no_survivor_near" : null;
    }

    @Override
    public boolean perform(BowlCast cast) {
        if (precheck(cast) != null) return false;
        ServerPlayer target = marked(cast);
        LivingEntity survivor = Unmarking.survivorNear(cast.level(), Vec3.atCenterOf(cast.bowl()), target);
        if (!Unmarking.perform(target, survivor, null, false)) return false;
        cast.sendTo(target.position().add(0, 1, 0), target);
        return true;
    }
}
