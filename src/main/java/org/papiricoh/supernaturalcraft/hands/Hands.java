package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.darkness.Darknesses;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.altar.HandAltarBlockEntity;
import org.papiricoh.supernaturalcraft.hands.mob.ThuleSoldierEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hands of God on the server (v0.42): a hunter's claimed Hands ({@link HandClaims}, this slice's record), taking one from
 * its reliquary's altar (the Hand, the record, the core's {@code Darknesses.handClaimed} hook, the locator's cache) and the
 * reliquary's guardians, who wake when its Hand is taken.
 */
public final class Hands {

    /** Guardians come at most this far from the altar, and only into open air. */
    static final int GUARD_REACH = 6;

    private Hands() {
    }

    public static HandClaims claims(ServerPlayer player) {
        return player.getData(HandsRegistry.HAND_CLAIMS);
    }

    public static void set(ServerPlayer player, HandClaims claims) {
        player.setData(HandsRegistry.HAND_CLAIMS, claims);
        HandLocator.forget(player);
    }

    /** The block's touch: claims at the altar for {@code player}, with the core's word on whether the Darkness is out for them. */
    public static HandAltarBlockEntity.Result touch(ServerPlayer player, HandAltarBlockEntity altar) {
        HandAltarBlockEntity.Result r = altar.claim(player, Darknesses.huntsHands(player));
        S11Assets.Hand kind = altar.kind();
        switch (r) {
            case GIVEN -> {
                if (player.level() instanceof ServerLevel level) guardians(level, altar.getBlockPos(), kind, player);
            }
            case ALREADY -> player.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.altar.already",
                    Component.translatable("item.supernaturalcraft." + kind.item)).withStyle(ChatFormatting.GRAY), true);
            case NOT_YET -> player.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.altar.not_yet")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            case EMPTY -> player.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.altar.empty")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return r;
    }

    /** {@code player} takes {@code hand}: the item, the record, the core's hook. Called once per hunter and kind (the altar checks). */
    public static void take(ServerPlayer player, S11Assets.Hand hand) {
        set(player, claims(player).with(hand));
        give(player, hand);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.hands.taken",
                Component.translatable("item.supernaturalcraft." + hand.item)).withStyle(ChatFormatting.GOLD), false);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        Darknesses.handClaimed(player, hand);
    }

    /** Puts {@code hand} in the hunter's inventory (or at their feet). */
    public static void give(ServerPlayer player, S11Assets.Hand hand) {
        ItemStack stack = new ItemStack(HandsRegistry.hand(hand));
        if (!player.getInventory().add(stack)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
        }
    }

    /** Forgets every Hand the hunter took (the reset command; their items stay where they are). */
    public static void reset(ServerPlayer player) {
        set(player, HandClaims.EMPTY);
    }

    // --- guardians ------------------------------------------------------------------------------------------------------------

    /** Who wakes when each kind's Hand is taken. */
    public static List<EntityType<? extends Mob>> guardians(S11Assets.Hand kind) {
        return switch (kind) {
            // The drowned crew of the U-boat.
            case DELPHINES_HAND -> List.of(HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_SOLDIER.get());
            // The bunker's garrison and the officer who keeps them standing.
            case NAIL_OF_THE_CROSS -> List.of(HandsRegistry.THULE_OFFICER.get(), HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_SOLDIER.get(),
                    HandsRegistry.THULE_SOLDIER.get());
            // The crypt's restless dead and the wailing woman over them.
            case APOSTLES_CHALICE -> List.of(HandsRegistry.BANSHEE.get(), AllEntities.GHOST.get(), AllEntities.GHOST.get());
            // The Society's dig party, come for the Ark as they once did.
            case ROD_OF_AARON -> List.of(HandsRegistry.THULE_OFFICER.get(), HandsRegistry.THULE_SOLDIER.get(), HandsRegistry.THULE_SOLDIER.get());
            // The monks who kept the trumpet, hollowed out by what lives in the mountain.
            case TRUMPET_OF_JERICHO -> List.of(HandsRegistry.BANSHEE.get(), HandsRegistry.CHITTER.get(), HandsRegistry.CHITTER.get());
        };
    }

    /** Wakes the guardians of {@code kind} round the altar at {@code altar}, set on {@code thief}. */
    public static List<Mob> guardians(ServerLevel level, BlockPos altar, S11Assets.Hand kind, ServerPlayer thief) {
        List<Mob> out = new ArrayList<>();
        List<EntityType<? extends Mob>> types = guardians(kind);
        for (int i = 0; i < types.size(); i++) {
            BlockPos at = openSpot(level, altar, i);
            Mob m = types.get(i).create(level);
            if (m == null) continue;
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null);
            m.setPersistenceRequired();
            m.setTarget(thief);
            if (m instanceof ThuleSoldierEntity s) s.rise();
            level.addFreshEntity(m);
            level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0.02);
            out.add(m);
        }
        return out;
    }

    /** An open two-high spot near the altar for the {@code i}-th guardian (a ring round it; the altar's own column at worst). */
    static BlockPos openSpot(ServerLevel level, BlockPos altar, int i) {
        double a = i * 2.399963; // the golden angle: guardians spread round the altar
        for (int r = 3; r <= GUARD_REACH; r++) {
            int dx = (int) Math.round(Math.cos(a) * r), dz = (int) Math.round(Math.sin(a) * r);
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos p = altar.offset(dx, dy, dz);
                if (open(level, p) && open(level, p.above()) && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) return p;
            }
        }
        return altar.above();
    }

    private static boolean open(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }
}
