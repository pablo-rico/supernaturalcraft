package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * What an Enochian ward does to angels (v0.36). Every {@link #CHECK_TICKS} a wild angel (never a boss) inside a ward is
 * thrown back out of it — burnt too by a supreme one — and one hunting someone who shelters in a ward forgets them. Angel
 * players are not pushed, but their grace answers nothing inside ({@link #silences}). The blink of the angels in the world
 * asks {@link #blocks} before it lands.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class WardEvents {

    public static final int CHECK_TICKS = 5;
    public static final double PUSH = 0.9;
    public static final float SUPREME_BURN = 2f;

    private WardEvents() {
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        if ((mob.tickCount + mob.getId()) % CHECK_TICKS != 0 || !Kin.isAngel(mob) || mob.getType().is(AllTags.Entities.BOSSES)) return;
        WardIndex index = WardIndex.get(level);
        if (index.isEmpty()) return;
        WardIndex.Ward ward = index.holding(level, mob.position());
        if (ward != null) {
            double[] out = WardGeometry.outward(mob.getX(), mob.getZ(), ward.pos().getX(), ward.pos().getZ());
            mob.setDeltaMovement(out[0] * PUSH, 0.25, out[1] * PUSH);
            mob.hurtMarked = true;
            mob.getNavigation().stop();
            if (ward.radius() > WardGeometry.RADIUS) {
                mob.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), SUPREME_BURN);
            }
            if (mob.tickCount % (CHECK_TICKS * 4) == 0) {
                MagicFx.ward(level, School.ENOCHIAN, mob, mob.position().add(0, 1, 0), 1.2f, MagicFxPayload.WARD_HIT, 12);
            }
        }
        LivingEntity target = mob.getTarget();
        if (target != null && index.holding(level, target.position()) != null) mob.setTarget(null);
    }

    /** Whether a ward stops an angel landing at {@code at}. */
    public static boolean blocks(ServerLevel level, LivingEntity angel, Vec3 at) {
        return !angel.getType().is(AllTags.Entities.BOSSES) && WardIndex.warded(level, at);
    }

    /** Whether an angel player's powers are stilled by a ward where they stand. */
    public static boolean silences(Player player) {
        return player.level() instanceof ServerLevel level && Kin.isAngel(player) && WardIndex.warded(level, player.position());
    }
}
