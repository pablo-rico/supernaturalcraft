package org.papiricoh.supernaturalcraft.angel;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;

/**
 * When angels come to the earth (v0.22): angels once anyone in the world has beaten Lilith (the last seal broken), the fallen
 * once anyone has beaten Metatron ({@link TheFall}). Kept in {@link HostsSavedData}; opened by the bosses' advancements, so a
 * share of the credit, a debt paid on login or a command all count, and a world from before v0.22 opens on the first login of
 * a hunter who already beat them (without the Fall's night: it is long past). Eggs, spawners and commands always work.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AngelSpawns {

    private AngelSpawns() {
    }

    /** Whether a {@code fallen} (or plain) angel may spawn for {@code reason} now. */
    public static boolean allowed(ServerLevelAccessor level, boolean fallen, MobSpawnType reason) {
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        HostsSavedData data = HostsSavedData.get(level.getLevel().getServer());
        return fallen ? data.fallen() : data.angels();
    }

    private static ResourceLocation advancement(BossProgression.Boss boss) {
        return SupernaturalCraft.asResource(boss.advancement);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = event.getAdvancement().id();
        if (id.equals(advancement(BossProgression.Boss.LILITH))) HostsSavedData.get(p.server).setAngels(true);
        else if (id.equals(advancement(BossProgression.Boss.METATRON))) TheFall.open(p.server.overworld());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        HostsSavedData data = HostsSavedData.get(p.server);
        if (!data.angels() && done(p, BossProgression.Boss.LILITH)) data.setAngels(true);
        if (!data.fallen() && done(p, BossProgression.Boss.METATRON)) {
            data.setAngels(true);
            data.setFallen(true, false);
        }
    }

    private static boolean done(ServerPlayer p, BossProgression.Boss boss) {
        AdvancementHolder adv = p.server.getAdvancements().get(advancement(boss));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }
}
