package org.papiricoh.supernaturalcraft.angel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.network.SmiteFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.util.ServerScheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * An angel struck dead by an angel blade (v0.22), as in the show: instead of dying it stands rigid for
 * {@link AngelAssets#SMITTEN_TICKS} while the light pours out of its eyes and mouth (the client's, {@link SmiteFxPayload#SMITE}),
 * untouchable and still, then the flash, and it falls — dying then of the very blow that smote it, so its spoils, its
 * experience, the journal's credit and a master's hooks (a Host captain, a Garrison angel) all follow as ever. Wild angels, the
 * fallen and the bosses' angels; never a boss. A smite cut short by an unload finishes the moment the angel is back.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class SmiteDeath {

    /** Persistent-data flag of an angel mid-smite. */
    public static final String TAG = "sn_smite";

    private static final Map<UUID, DamageSource> SMITING = new HashMap<>();
    private static final Set<UUID> FINISHING = new HashSet<>();

    private SmiteDeath() {
    }

    /** Whether {@code e} is an angel of any kind (a wild one, a fallen one, a boss's). */
    public static boolean angelKind(LivingEntity e) {
        return e.getType().is(AllTags.Entities.ANGELS) || e.getType().is(AllTags.Entities.FALLEN_ANGELS);
    }

    /** Whether {@code source} is a blow of an angel blade: a strike (or the blade's dash) by whoever holds one. */
    public static boolean angelicBlow(DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity holder) || source.getEntity() != holder) return false;
        boolean strike = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(AllDamageTypes.SMITE);
        return strike && holder.getMainHandItem().is(AllTags.Items.ANGELIC_BLADES);
    }

    public static boolean smiting(LivingEntity e) {
        return SMITING.containsKey(e.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel)) return;
        UUID id = victim.getUUID();
        if (FINISHING.contains(id)) return;
        if (SMITING.containsKey(id)) {
            // Killed outright mid-smite (a command, the void): let it go.
            SMITING.remove(id);
            victim.getPersistentData().remove(TAG);
            return;
        }
        DamageSource source = event.getSource();
        if (!AngelRules.smites(angelKind(victim), victim.getType().is(AllTags.Entities.BOSSES), angelicBlow(source))) return;
        event.setCanceled(true);
        LivingEntity holder = (LivingEntity) source.getDirectEntity();
        start(victim, source, holder.getMainHandItem());
    }

    /**
     * Smites {@code victim} now, as if {@code by} had struck it dead with {@code blade} (commands, previews).
     * @return whether it began (not for a boss, a non-angel or one already smitten)
     */
    public static boolean smite(LivingEntity victim, @Nullable LivingEntity by, ItemStack blade) {
        if (!(victim.level() instanceof ServerLevel level) || !angelKind(victim) || victim.getType().is(AllTags.Entities.BOSSES)
                || !victim.isAlive() || SMITING.containsKey(victim.getUUID())) return false;
        start(victim, by == null ? AllDamageTypes.source(level, AllDamageTypes.SMITE, null) : AllDamageTypes.source(level, AllDamageTypes.SMITE, by), blade);
        return true;
    }

    private static void start(LivingEntity victim, DamageSource source, ItemStack blade) {
        ServerLevel level = (ServerLevel) victim.level();
        SMITING.put(victim.getUUID(), source);
        victim.getPersistentData().putBoolean(TAG, true);
        victim.setHealth(1);
        victim.setDeltaMovement(Vec3.ZERO);
        if (victim instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (victim instanceof AngelEntity angel) {
            angel.flashWings();
            angel.triggerAnim("action", "smitten");
        }
        Send.trackingEntity(victim, new SmiteFxPayload(SmiteFxPayload.SMITE, victim.getId(), BuiltInRegistries.ITEM.getId(blade.getItem()),
                victim.position(), victim.getYRot(), AngelAssets.SMITTEN_TICKS));
        ServerScheduler.schedule(AngelAssets.SMITTEN_TICKS - 4, () -> {
            if (!victim.isRemoved()) TempLights.place(level, BlockPos.containing(victim.getEyePosition()), 15, 14);
        });
        ServerScheduler.schedule(AngelAssets.SMITTEN_TICKS, () -> finish(victim));
    }

    private static void finish(LivingEntity victim) {
        DamageSource source = SMITING.remove(victim.getUUID());
        if (source == null || victim.isRemoved() || victim.isDeadOrDying()) return;
        victim.getPersistentData().remove(TAG);
        FINISHING.add(victim.getUUID());
        try {
            victim.setHealth(0);
            victim.die(source);
        } finally {
            FINISHING.remove(victim.getUUID());
        }
    }

    /** Untouchable while it burns. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (SMITING.containsKey(victim.getUUID())) {
            if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) event.setCanceled(true);
            return;
        }
        // Only an angel blade truly bites a wild angel or a fallen one.
        if (victim instanceof AngelEntity) {
            DamageSource src = event.getSource();
            event.setAmount(AngelRules.incoming(event.getAmount(), angelicBlow(src),
                    src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(AllDamageTypes.COLT)));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SMITING.clear();
        FINISHING.clear();
    }

    /** One that was mid-smite when its chunk unloaded (or the server stopped) falls as soon as it is back. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity e)) return;
        if (!e.getPersistentData().getBoolean(TAG) || SMITING.containsKey(e.getUUID())) return;
        SMITING.put(e.getUUID(), AllDamageTypes.source(level, AllDamageTypes.SMITE, null));
        ServerScheduler.schedule(1, () -> finish(e));
    }
}
