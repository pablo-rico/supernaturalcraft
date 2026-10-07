package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/**
 * War's illusions. He marks every hunter for ten seconds: on their screens the others are demons. Mirages walk the
 * field, hostile and innocent alike in a demon's face; the innocent kneel. A marked hunter who strikes an innocent or a
 * friend hurts themself instead, and feeds his fury.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class WarIllusions {

    /** Share of the mirages that are innocent. */
    public static final float INNOCENT_SHARE = 0.5f;

    private WarIllusions() {
    }

    public static void cast(ServerLevel level, WarEntity war, List<ServerPlayer> hunters) {
        for (ServerPlayer p : hunters) war.mark(p, WarEntity.ILLUSION_TICKS);
        int n = 3 + hunters.size();
        int innocents = Math.round(n * INNOCENT_SHARE);
        for (int i = 0; i < n; i++) {
            Vec3 at = LuciferAttacks.randomArenaPoint(war, 0.75);
            WarMirageEntity mirage = AllEntities.WAR_MIRAGE.get().create(level);
            if (mirage == null) continue;
            mirage.moveTo(at.x, at.y, at.z, war.getRandom().nextFloat() * 360, 0);
            mirage.setOwner(war.getUUID());
            mirage.setInnocent(i < innocents);
            mirage.finalizeSpawn(level, level.getCurrentDifficultyAt(mirage.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(mirage);
            war.minions().add(mirage.getUUID());
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 12, 0.3, 0.8, 0.3, 0.02);
        }
    }

    public static int mirages(ServerLevel level, WarEntity war) {
        return level.getEntitiesOfClass(WarMirageEntity.class, war.getBoundingBox().inflate(48), m -> m.isAlive()).size();
    }

    /** {@code striker} hit an innocent or a friend under the illusion: the blow comes back on them and War grows angrier. */
    public static void betrayed(ServerLevel level, @Nullable WarEntity war, ServerPlayer striker, float amount) {
        striker.hurt(striker.damageSources().magic(), Math.max(1, amount));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, striker.getX(), striker.getY() + 2, striker.getZ(), 3,
                0.3, 0.2, 0.3, 0);
        if (war != null) war.fury().betrayal();
    }

    /** The War who has marked {@code p}, if any. */
    public static @Nullable WarEntity markedBy(Player p) {
        for (WarEntity war : p.level().getEntitiesOfClass(WarEntity.class, new AABB(p.blockPosition()).inflate(64))) {
            if (war.isAlive() && war.isMarked(p)) return war;
        }
        return null;
    }

    /** A marked hunter striking another hunter: no harm to the friend, and it comes back on the striker. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || !(event.getSource().getEntity() instanceof ServerPlayer striker)
                || striker == victim || !(striker.level() instanceof ServerLevel level)) {
            return;
        }
        if (strikeFriend(level, striker, event.getAmount())) event.setCanceled(true);
    }

    /** A hunter struck another: if War has marked the striker, the friend takes nothing and the striker takes the blow. */
    public static boolean strikeFriend(ServerLevel level, ServerPlayer striker, float amount) {
        WarEntity war = markedBy(striker);
        if (war == null) return false;
        striker.displayClientMessage(Component.translatable("message.supernaturalcraft.war.betrayal").withStyle(ChatFormatting.RED), true);
        betrayed(level, war, striker, amount);
        return true;
    }
}
