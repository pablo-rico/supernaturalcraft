package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlockEntity;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import org.papiricoh.supernaturalcraft.grave.GraveBonesBlockEntity;
import org.papiricoh.supernaturalcraft.grave.GraveBuilder;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.network.SmokeTrailPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

import java.util.List;
import java.util.UUID;

/**
 * {@code SN_PREVIEW=bowl}: the spell bowl and what came with it, well away from the origin. A full bowl
 * on the ground, lit, the recitation screen and the smoke of a spell that took; the bowl held in first
 * person, from the front and in the inventory; a backlash; a ghost unseen, flickering and seen with Second
 * Sight; its grave; a crossroads demon; a locating spell's smoke trail.
 */
final class BowlPreview {

    private static int t = -1;
    private static BlockPos ground;
    private static Vec3 camFrom, camAt;
    private static GhostEntity ghost;

    private BowlPreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"bowl".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        t++;
        int now = t + 30;
        // Client-side camera and screens.
        if (now == 150) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (now == 210) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if (now == 226) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (now == 240) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if (now == 256) {
            mc.setScreen(null);
            mc.options.hideGui = true;
        }
        if (now == 122) mc.setScreen(null);
        server.execute(() -> scene(mc, server.getPlayerList().getPlayers().getFirst(), now));
        for (int s : new int[]{60, 75, 110, 135, 185, 200, 222, 236, 252, 290, 330, 350, 375, 400, 450, 480, 500, 515}) {
            if (now == s) Screenshot.grab(mc.gameDirectory, String.format("sn_bowl_%04d.png", now), mc.getMainRenderTarget(), m -> {
            });
        }
        if (now >= 520) mc.stop();
        return true;
    }

    private static void scene(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.server.overworld();
        if (now == 31) {
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(level, 400.5, 120, 400.5, 0, 0);
            mc.options.hideGui = true;
            level.setDayTime(18000);
            level.getChunk(400 >> 4, 412 >> 4);
            ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(400, 0, 412));
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    level.setBlockAndUpdate(ground.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int dy = 0; dy < 4; dy++) level.setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(ground, AllBlocks.SPELL_BOWL.get().defaultBlockState());
            bowl(level).setContents(BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(AllItems.ECTOPLASM.get()),
                    new ItemStack(Items.AMETHYST_SHARD)), List.of(Dose.of(BowlLiquid.HOLY_WATER))));
            ManaManager.get(p).learnRite(org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("second_sight"));
            ManaManager.get(p).setMana(ManaManager.get(p).maxMana());
        }
        if (now < 50) return;
        // A teleport right after the change of dimension can be lost: keep re-applying the held view.
        if (camFrom != null && now % 5 == 0 && p.position().distanceToSqr(camFrom) > 0.25) view(p, camFrom, camAt);
        Vec3 c = Vec3.atBottomCenterOf(ground);
        if (now == 52) view(p, c.add(1.1, 1.1, -1.1), c.add(0, 0.2, 0));
        if (now == 70) {
            bowl(level).setContents(BowlContents.of(List.of(new ItemStack(Items.BONE), new ItemStack(Items.COMPASS),
                    new ItemStack(AllItems.SALT.get())), List.of(Dose.of(BowlLiquid.WATER), Dose.blood(UUID.randomUUID(), "Dean"))));
        }
        if (now == 90) {
            bowl(level).setContents(BowlContents.of(List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(AllItems.ECTOPLASM.get()),
                    new ItemStack(Items.AMETHYST_SHARD)), List.of(Dose.of(BowlLiquid.HOLY_WATER))));
            view(p, c.add(1.6, 1.6, -1.6), c.add(0, 0.3, 0));
            bowl(level).tryLight(p, new ItemStack(Items.FLINT_AND_STEEL), InteractionHand.MAIN_HAND);
        }
        if (now == 120) bowl(level).resolveRecitation(p, true, 0);
        if (now == 125) view(p, c.add(3, 2.5, -3), c.add(0, 1.2, 0));
        if (now == 145) {
            // The bowl in hand, full of something red.
            ItemStack held = new ItemStack(AllItems.SPELL_BOWL.get());
            held.set(AllDataComponents.BOWL_CONTENTS.get(), BowlContents.of(List.of(new ItemStack(Items.BONE), new ItemStack(Items.COMPASS),
                    new ItemStack(AllItems.SALT.get())), List.of(Dose.of(BowlLiquid.WATER), Dose.blood(UUID.randomUUID(), "Sam"))));
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            camFrom = null;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.teleportTo(level, c.x + 3, c.y, c.z, 90, 25);
        }
        if (now == 192) p.teleportTo(level, c.x + 3, c.y, c.z, 90, 45);
        if (now == 205) p.teleportTo(level, c.x + 3, c.y, c.z, 90, 0);
        if (now == 262) {
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            level.setBlockAndUpdate(ground, AllBlocks.SPELL_BOWL.get().defaultBlockState());
            bowl(level).setContents(BowlContents.of(List.of(new ItemStack(Items.DIRT), new ItemStack(Items.ROTTEN_FLESH)),
                    List.of(Dose.of(BowlLiquid.DEMON_BLOOD))));
            view(p, c.add(2.5, 2, -2.5), c.add(0, 0.6, 0));
        }
        if (now == 285) bowl(level).tryLight(p, new ItemStack(Items.FLINT_AND_STEEL), InteractionHand.MAIN_HAND);
        if (now == 310) {
            level.setBlockAndUpdate(ground, Blocks.AIR.defaultBlockState());
            var plan = GraveBuilder.placeDirect(level, ground.offset(0, 0, 0), 7);
            if (level.getBlockEntity(plan.bones()) instanceof GraveBonesBlockEntity bones) ghost = bones.raiseGhost(level);
            if (ghost != null) {
                ghost.setNoAi(true);
                ghost.moveTo(c.x, c.y + 0.5, c.z, 180, 0);
            }
            p.removeEffect(AllMobEffects.SECOND_SIGHT);
            view(p, c.add(0, 1.8, -3.2), c.add(0, 1.4, 0));
        }
        if (now == 340 && ghost != null) ghost.manifest(40);
        if (now == 365) p.addEffect(new MobEffectInstance(AllMobEffects.SECOND_SIGHT, 2000));
        if (now == 390) {
            if (ghost != null) ghost.discard();
            view(p, c.add(4, 4, -6), c);
        }
        if (now == 415) {
            CrossroadsDemonEntity demon = CrossroadsDemonEntity.summon(level, c.add(0, 0, 3), p);
            if (demon != null) demon.setNoAi(true);
            view(p, c.add(0, 1.6, -0.5), c.add(0, 1.6, 3));
        }
        if (now == 455) {
            for (var d : level.getEntitiesOfClass(CrossroadsDemonEntity.class, new AABB(ground).inflate(8))) d.discard();
            view(p, c.add(-4, 3, -6), c.add(10, 2, 20));
            p.removeEffect(AllMobEffects.SECOND_SIGHT);
        }
        if (now == 460) {
            PacketDistributor.sendToPlayer(p, new SmokeTrailPayload(c.add(0, 0.5, 0), c.add(60, 0, 120), 0x9FB7FF, 600));
        }
        if (now == 515) p.removeEffect(AllMobEffects.SECOND_SIGHT);
    }

    private static SpellBowlBlockEntity bowl(ServerLevel level) {
        return (SpellBowlBlockEntity) level.getBlockEntity(ground);
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        camFrom = from;
        camAt = at;
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }
}
