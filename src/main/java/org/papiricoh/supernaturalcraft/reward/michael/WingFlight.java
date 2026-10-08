package org.papiricoh.supernaturalcraft.reward.michael;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Flight on the Seraph Wings for a hunter who took in Michael's Grace: while the wings are worn (Curios back slot, or the
 * chest slot) they may fly as in creative, for as long as their {@link WingStamina} lasts; on the ground it comes back.
 * Run dry in the air and the wings fold (slow falling carries you down). The first flight earns {@code main/wings_of_heaven}.
 * Only flight this grants is ever taken away again: creative flight and other mods' are left alone.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class WingFlight {

    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");
    private static final Map<UUID, WingStamina> STAMINA = new HashMap<>();
    private static final Set<UUID> GRANTED = new HashSet<>();

    private WingFlight() {
    }

    /** Whether {@code player} wears the Seraph Wings: on their back (Curios) or in the chest slot. */
    public static boolean wearingWings(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(AllItems.SERAPH_WINGS.get())) return true;
        return CURIOS && CuriosCompat.isWearing(player, AllItems.SERAPH_WINGS.get());
    }

    /** Whether {@code player} took in Michael's Grace (server side: the attachment is not synced). */
    public static boolean blessed(Player player) {
        return player.getData(AllAttachments.HEAVEN).grace();
    }

    public static WingStamina stamina(Player player) {
        return STAMINA.computeIfAbsent(player.getUUID(), k -> new WingStamina(SNConfig.GRACE_FLIGHT_SECONDS.get()));
    }

    /** Whether the wings are what lets {@code player} fly now. */
    public static boolean granted(Player player) {
        return GRANTED.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STAMINA.remove(event.getEntity().getUUID());
        GRANTED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        GRANTED.remove(event.getEntity().getUUID());
        STAMINA.remove(event.getEntity().getUUID());
    }

    /** One tick of a hunter's wings (also called directly by tests). */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (player.isCreative() || player.isSpectator()) {
            GRANTED.remove(id);
            return;
        }
        var abilities = player.getAbilities();
        if (!(blessed(player) && wearingWings(player) || org.papiricoh.supernaturalcraft.allegiance.Allegiances.wingsGranted(player))) {
            if (GRANTED.remove(id)) {
                abilities.mayfly = false;
                abilities.flying = false;
                player.onUpdateAbilities();
            }
            return;
        }
        WingStamina stamina = stamina(player);
        boolean flying = GRANTED.contains(id) && abilities.flying;
        if (stamina.tick(flying, player.onGround())) {
            // The wings give out: fold them and let the light carry you down.
            abilities.flying = false;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
            player.serverLevel().playSound(null, player.blockPosition(), AllSounds.MICHAEL_WINGS.get(), SoundSource.PLAYERS, 0.8f, 0.6f);
        }
        boolean may = stamina.canFly();
        if (abilities.mayfly != may || (!may && abilities.flying)) {
            abilities.mayfly = may;
            if (!may) abilities.flying = false;
            player.onUpdateAbilities();
        }
        GRANTED.add(id);
        if (flying) {
            HeavenLedger ledger = player.getData(AllAttachments.HEAVEN);
            if (!ledger.flown()) {
                player.setData(AllAttachments.HEAVEN, ledger.withFlown(true));
                ChorusRewards.award(player, "main/wings_of_heaven");
                player.serverLevel().playSound(null, player.blockPosition(), AllSounds.GRACE_FLIGHT.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            }
            if (player.tickCount % 30 == 0) {
                player.serverLevel().playSound(null, player.blockPosition(), AllSounds.MICHAEL_WINGS.get(), SoundSource.PLAYERS, 0.25f, 1.4f);
            }
            if (player.tickCount % 3 == 0) {
                player.serverLevel().sendParticles(AllParticles.GRACE.get(), player.getX(), player.getY() + 1.1, player.getZ(), 1, 0.4, 0.3, 0.4, 0.01);
            }
        }
    }
}
