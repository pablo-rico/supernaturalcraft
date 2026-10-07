package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.demon.DemonEntity;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The Concealment spell ({@link AllMobEffects#CONCEALED}): demons, angels, spirits and hellhounds
 * neither see nor target its bearer. Never bosses. A hound collecting a debt still finds its quarry,
 * and striking one of the creatures it hides you from breaks the spell.
 */
public final class Concealment {

    /** When the spell takes hold, creatures this close that were hunting the bearer lose them. */
    public static final double SCATTER_RANGE = 48;

    private Concealment() {
    }

    /** Whether Concealment works on {@code mob} at all: the mod's unholy and holy things, never a boss. */
    public static boolean affects(@Nullable Entity mob) {
        if (!(mob instanceof LivingEntity) || mob instanceof Player) return false;
        EntityType<?> type = mob.getType();
        if (type.is(AllTags.Entities.BOSSES) || mob instanceof LuciferEntity) return false;
        return type.is(AllTags.Entities.DEMONS) || type.is(AllTags.Entities.ANGELS) || type.is(AllTags.Entities.SPIRITS)
                || mob instanceof DemonEntity || mob instanceof HellhoundEntity || mob instanceof GhostEntity;
    }

    /** Whether {@code target} is hidden from {@code mob}. */
    public static boolean hides(@Nullable Entity mob, @Nullable LivingEntity target) {
        if (target == null || !target.hasEffect(AllMobEffects.CONCEALED) || !affects(mob)) return false;
        // The hounds that come to collect a debt see their quarry whatever it hides behind.
        return !(mob instanceof HellhoundEntity hound && target.getUUID().equals(hound.quarry()));
    }

    /** The spell just took hold on {@code bearer}: whatever it hides them from stops hunting them. */
    public static void scatter(LivingEntity bearer) {
        AABB box = bearer.getBoundingBox().inflate(SCATTER_RANGE);
        for (Mob mob : bearer.level().getEntitiesOfClass(Mob.class, box, m -> m.getTarget() == bearer)) {
            if (!affects(mob) || (mob instanceof HellhoundEntity h && bearer.getUUID().equals(h.quarry()))) continue;
            mob.setTarget(null);
            if (mob.getLastHurtByMob() == bearer) mob.setLastHurtByMob(null);
        }
    }

    /** {@code bearer} struck something the spell hides them from: the spell breaks. */
    public static void broken(LivingEntity bearer) {
        if (!bearer.hasEffect(AllMobEffects.CONCEALED)) return;
        bearer.removeEffect(AllMobEffects.CONCEALED);
        if (bearer instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.concealment.broken").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
