package org.papiricoh.supernaturalcraft.bowl.spell.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * Second Sight's own touches (ghosts and hellhounds draw themselves): motes of white light outline
 * invisible creatures near you, and hidden curse bags glimmer.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class SecondSightFx {

    static final double CREATURE_RANGE = 24;
    static final int BAG_RANGE = 16;
    static final int BAG_SCAN_INTERVAL = 20;

    private static final List<BlockPos> BAGS = new ArrayList<>();

    private SecondSightFx() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        if (!player.hasEffect(AllMobEffects.SECOND_SIGHT)) {
            BAGS.clear();
            return;
        }
        long time = level.getGameTime();
        RandomSource r = level.random;
        if (time % 3 == 0) outlineInvisible(level, player, r);
        if (time % BAG_SCAN_INTERVAL == 0) scanBags(level, player.blockPosition());
        if (time % 4 == 0) {
            for (BlockPos p : BAGS) {
                if (!level.getBlockState(p).is(AllBlocks.CURSE_BAG.get())) continue;
                level.addParticle(ParticleTypes.WITCH, p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.5, p.getY() + 0.3 + r.nextDouble() * 0.3,
                        p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.5, 0, 0.01, 0);
                if (r.nextInt(3) == 0) {
                    level.addParticle(AllParticles.WHITE_LIGHT.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0.01, 0);
                }
            }
        }
    }

    /** White motes on the surface of each invisible creature's bounds: its silhouette. */
    private static void outlineInvisible(ClientLevel level, LocalPlayer player, RandomSource r) {
        AABB around = player.getBoundingBox().inflate(CREATURE_RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, around,
                e -> e != player && e.isAlive() && e.isInvisible() && !(e instanceof GhostEntity) && !(e instanceof HellhoundEntity))) {
            AABB b = e.getBoundingBox();
            int motes = 2 + (int) (b.getSize() * 2);
            for (int i = 0; i < motes; i++) {
                double x = b.minX + r.nextDouble() * b.getXsize();
                double y = b.minY + r.nextDouble() * b.getYsize();
                double z = b.minZ + r.nextDouble() * b.getZsize();
                // Snap one horizontal coordinate to a face, so the motes trace the outline.
                if (r.nextBoolean()) x = r.nextBoolean() ? b.minX : b.maxX;
                else z = r.nextBoolean() ? b.minZ : b.maxZ;
                level.addParticle(AllParticles.WHITE_LIGHT.get(), x, y, z, 0, 0.005, 0);
            }
        }
    }

    /** Curse bags in the loaded chunks around {@code center} (they all carry a block entity). */
    private static void scanBags(ClientLevel level, BlockPos center) {
        BAGS.clear();
        int cx = SectionPos.blockToSectionCoord(center.getX()), cz = SectionPos.blockToSectionCoord(center.getZ());
        int reach = (BAG_RANGE >> 4) + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx + dx, cz + dz, false);
                if (chunk == null) continue;
                for (BlockPos p : chunk.getBlockEntities().keySet()) {
                    if (p.distSqr(center) <= BAG_RANGE * BAG_RANGE && level.getBlockState(p).is(AllBlocks.CURSE_BAG.get())) {
                        BAGS.add(p.immutable());
                    }
                }
            }
        }
    }
}
