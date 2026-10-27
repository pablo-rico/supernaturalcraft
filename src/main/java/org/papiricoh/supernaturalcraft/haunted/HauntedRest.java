package org.papiricoh.supernaturalcraft.haunted;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlock;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A haunted house's family at peace (v0.31): when the last of its bones are burned, the study's chest appears where the
 * plan left its spot clear. The bones only know their house by that spot ({@link GraveBonesBlockEntity#house()}), so the
 * family is found by looking round it; if part of the house is not loaded the reckoning waits for another burning.
 */
public final class HauntedRest {

    /** How far round the study's chest spot the family's bones may lie. */
    static final int REACH = HauntedHouseLayout.RADIUS + 4;

    private HauntedRest() {
    }

    /** Whether a family is at rest: the house's bones were found and none is restless. */
    public static boolean atRest(List<Boolean> restedBones) {
        return !restedBones.isEmpty() && !restedBones.contains(Boolean.FALSE);
    }

    /** Called once bones have been laid to rest: opens the study if they were the last of their house's. */
    public static void onRested(ServerLevel level, GraveBonesBlockEntity bones, @Nullable Player player) {
        BlockPos chest = bones.house();
        if (chest == null) return;
        List<Boolean> family = family(level, chest);
        if (family == null || !atRest(family)) return;
        if (!level.getBlockState(chest).isAir()) return;
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity be) {
            be.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(HauntedHouseLayout.STUDY_LOOT)), level.random.nextLong());
        }
        double x = chest.getX() + 0.5, y = chest.getY() + 0.6, z = chest.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 30, 0.4, 0.5, 0.4, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.5, z, 12, 0.3, 0.6, 0.3, 0.01);
        level.playSound(null, chest, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2f, 0.7f);
        for (ServerPlayer p : level.getPlayers(p -> p.blockPosition().closerThan(chest, REACH * 2))) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.haunted.at_rest").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
        if (player instanceof ServerPlayer sp) org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(sp, "main/family_at_rest");
    }

    /** Whether each bones of the house round {@code chest} rests; null if any chunk of the house is not loaded. */
    @Nullable
    static List<Boolean> family(ServerLevel level, BlockPos chest) {
        List<Boolean> out = new ArrayList<>();
        for (int cx = (chest.getX() - REACH) >> 4; cx <= (chest.getX() + REACH) >> 4; cx++) {
            for (int cz = (chest.getZ() - REACH) >> 4; cz <= (chest.getZ() + REACH) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) return null;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof GraveBonesBlockEntity b && chest.equals(b.house())) {
                        out.add(b.getBlockState().getValue(GraveBonesBlock.RESTED));
                    }
                }
            }
        }
        return out;
    }
}
