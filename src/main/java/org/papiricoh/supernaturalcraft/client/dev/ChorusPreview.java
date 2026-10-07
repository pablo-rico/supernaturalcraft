package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

/**
 * DevPreview scenes for the Broken Chorus (see {@link DevPreview}):
 * <ul>
 *   <li>{@code chorus_model}: its descent, every phase reached by breaking its parts, kneeling and
 *   resting, with hit boxes shown for a few frames at each stage (they must sit on the model).</li>
 * </ul>
 */
final class ChorusPreview {

    private static final String SCENES = System.getenv("SN_PREVIEW");
    private static int tick = -1;
    private static BlockPos centre;

    private ChorusPreview() {
    }

    static boolean tick(Minecraft mc) {
        if ("chorus".equals(SCENES)) return tickFight(mc);
        if ("spire".equals(SCENES)) return tickSpire(mc);
        if ("wings".equals(SCENES)) return tickWings(mc);
        if (SCENES != null && SCENES.startsWith("spire_real")) return tickRealSpire(mc);
        if (!"chorus_model".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                centre = p.blockPosition().offset(0, 0, -20);
                ArenaController arena = ChorusSummoning.openArena(level, centre);
                ChorusEntity c = AllEntities.BROKEN_CHORUS.get().create(level);
                c.moveTo(centre.getX() + 0.5, centre.getY() + 40, centre.getZ() + 0.5, 0, 0);
                c.bindArena(arena, centre, List.of());
                level.addFreshEntity(c);
                c.beginEmergence();
            });
            return true;
        }
        int t = ++tick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var all = p.serverLevel().getEntitiesOfClass(ChorusEntity.class, p.getBoundingBox().inflate(80));
            if (all.isEmpty()) return;
            ChorusEntity c = all.getFirst();
            // Walk the camera round it: front, three-quarter, side, a little higher in the later phases.
            double angle = t < 280 ? 0 : t < 300 ? 0.7 : t < 330 ? 1.5 : 0.35 * Math.sin(t * 0.004);
            double dist = c.phase() == 4 ? 14 : 21;
            Vec3 at = new Vec3(centre.getX() + 0.5 + Math.sin(angle) * dist, centre.getY(), centre.getZ() + 0.5 + Math.cos(angle) * dist);
            if (p.position().distanceToSqr(at) > 0.5) p.teleportTo(at.x, at.y, at.z);
            p.lookAt(EntityAnchorArgument.Anchor.EYES, c.position().add(0, ChorusGeometry.CORE_Y / ChorusGeometry.PX - 1, 0));
            var hit = p.serverLevel().damageSources().playerAttack(p);
            if (t == 330) c.kneel(ChorusEntity.KNEEL_TICKS);
            if (t == 440) breakAll(c, ChorusEntity.FIRST_FACE, 4, hit);
            if (t == 700) c.rest(90);
            if (t == 800) breakAll(c, ChorusEntity.FIRST_WING, 6, hit);
            if (t == 1020) c.forceEyes(0xFFF);
            if (t == 1100) breakAll(c, ChorusEntity.FIRST_EYE, 12, hit);
            if (t == 1101) c.forceEyes(0);
            if (t == 1420) breakAll(c, ChorusEntity.CORE, 1, hit);
        });
        for (int[] window : new int[][]{{236, 243}, {362, 369}, {646, 653}, {732, 739}, {1042, 1049}, {1382, 1389}}) {
            if (t == window[0]) mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
            if (t == window[1]) mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
        }
        for (int shot : new int[]{60, 150, 220, 240, 270, 300, 360, 366, 470, 525, 560, 630, 650, 730, 736, 850, 905, 980, 1040,
                1046, 1080, 1150, 1225, 1300, 1380, 1386, 1500, 1650, 1780}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_chorus_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 1830) mc.stop();
        return true;
    }

    private static void breakAll(ChorusEntity c, int from, int count, net.minecraft.world.damagesource.DamageSource hit) {
        for (int i = from; i < from + count; i++) {
            for (int n = 0; n < 40 && c.partAlive(i); n++) c.part(i).hurt(hit, 40f);
        }
    }

    private static int hymnsSeen;

    /**
     * {@code chorus}: a real fight on a small summit (altar, bells, a ring of pillars) with its AI
     * on. The player is invulnerable; parts break on a timer to walk every phase; one Hymn is broken
     * on the right bell. A screenshot every two seconds.
     */
    private static boolean tickFight(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                centre = p.blockPosition().offset(0, 0, -16);
                level.setBlock(centre, org.papiricoh.supernaturalcraft.registry.AllBlocks.CHOIR_ALTAR.get().defaultBlockState(), 3);
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2 / 7;
                    BlockPos b = centre.offset((int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7));
                    level.setBlock(b.below(), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
                    level.setBlock(b, org.papiricoh.supernaturalcraft.registry.AllBlocks.CHOIR_BELLS.get(i).get().defaultBlockState(), 3);
                }
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3 + 0.3;
                    BlockPos b = centre.offset((int) Math.round(Math.cos(a) * 10), 0, (int) Math.round(Math.sin(a) * 10));
                    for (int y = 0; y < 5; y++) level.setBlock(b.above(y), net.minecraft.world.level.block.Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
                }
                level.setWeatherParameters(0, 6000, true, true);
                ChorusSummoning.summon(level, centre, p);
            });
            return true;
        }
        int t = ++tick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var all = p.serverLevel().getEntitiesOfClass(ChorusEntity.class, p.getBoundingBox().inflate(80));
            if (all.isEmpty()) return;
            ChorusEntity c = all.getFirst();
            double angle = t * 0.0025;
            double dist = c.phase() == 4 ? 12 : 15;
            Vec3 at = new Vec3(centre.getX() + 0.5 + Math.sin(angle) * dist, centre.getY(), centre.getZ() + 0.5 + Math.cos(angle) * dist);
            if (p.position().distanceToSqr(at) > 1) p.teleportTo(at.x, at.y, at.z);
            p.lookAt(EntityAnchorArgument.Anchor.EYES, c.position().add(0, ChorusGeometry.CORE_Y / ChorusGeometry.PX - 2, 0));
            var hit = p.serverLevel().damageSources().playerAttack(p);
            if (t == 700) breakAll(c, ChorusEntity.FIRST_FACE, 4, hit);
            if (t == 1500) breakAll(c, ChorusEntity.FIRST_WING, 6, hit);
            if (t == 2300) {
                c.forceEyes(0xFFF);
                breakAll(c, ChorusEntity.FIRST_EYE, 12, hit);
                c.forceEyes(0);
            }
            if (t == 3000) breakAll(c, ChorusEntity.CORE, 1, hit);
            // Break the second Hymn on its second note, to see it kneel.
            if (c.hymnNote() >= 0 && c.scheduler().current() instanceof org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusAttacks.TheHymn h) {
                if (h.note() >= 0 && t % 10 == 0) {
                    if (hymnsSeen == 0 && !h.broken()) hymnsSeen = t;
                    if (hymnsSeen > 0 && t - hymnsSeen > 300 && !h.broken()) {
                        for (BlockPos b : c.bells()) {
                            if (p.serverLevel().getBlockState(b).getBlock() instanceof org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock bell
                                    && bell.note == h.note()) {
                                org.papiricoh.supernaturalcraft.chorus.ChoirBellBlock.ring(p.serverLevel(), b, p);
                                break;
                            }
                        }
                    }
                }
            }
        });
        if (t % 40 == 0) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_chorusfight_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 3480) mc.stop();
        return true;
    }

    private static org.papiricoh.supernaturalcraft.structure.SpireLayout.Plan spirePlan;

    /** A cone of a mountain on the flat preview world, the way the noise would never make one, but tall enough. */
    private static BlockPos mountain(ServerLevel level, BlockPos base) {
        int R = 64, H = 64;
        net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState snow = net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / R;
                if (d >= 1) continue;
                double ripple = Math.sin(dx * 0.21) * Math.cos(dz * 0.17) * 3 + Math.sin((dx + dz) * 0.09) * 2;
                int h = (int) (H * Math.pow(1 - d, 1.25) + ripple * (1 - d));
                for (int y = 0; y < h; y++) level.setBlock(m.set(base.getX() + dx, base.getY() + y, base.getZ() + dz), stone, 2);
                if (h > 0) level.setBlock(m.set(base.getX() + dx, base.getY() + h, base.getZ() + dz), h > 40 ? snow : grass, 2);
            }
        }
        return base.above(H);
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(from.x, from.y, from.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    /** {@code spire}: a Hymnal Spire on a made-up mountain, seen from the air, the summit, the stair and the temple. */
    private static boolean tickSpire(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
                level.setWeatherParameters(12000, 0, false, false);
                p.setGameMode(GameType.SPECTATOR);
                BlockPos base = p.blockPosition().offset(90, 0, 0);
                mountain(level, base);
                spirePlan = org.papiricoh.supernaturalcraft.structure.HymnalSpire.placeDirect(level, base, 20261007L);
            });
            return true;
        }
        int t = ++tick;
        if (spirePlan == null) return true;
        var plan = spirePlan;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            Vec3 altar = Vec3.atCenterOf(plan.altar());
            var path = plan.path();
            Vec3 mid = Vec3.atCenterOf(path.get(path.size() / 2));
            Vec3 door = Vec3.atBottomCenterOf(plan.templeOrigin());
            Vec3 f = Vec3.atLowerCornerOf(plan.templeFacing().getNormal());
            if (t == 10) view(p, altar.add(-75, 45, -75), altar.add(0, -25, 0));
            if (t == 130) view(p, altar.add(-18, 9, -18), altar);
            if (t == 230) view(p, altar.add(0, 3, 9), altar.add(0, 1, -10));
            if (t == 330) view(p, mid.add(0, 4, 0).subtract(Vec3.atLowerCornerOf(path.get(path.size() / 2 + 6).subtract(path.get(path.size() / 2))).scale(1.2)), mid);
            if (t == 430) view(p, door.add(f.scale(9)).add(0, 4, 0), door.add(0, 2, 0));
            if (t == 530) view(p, door.subtract(f.scale(2)).add(0, 1.6, 0), door.subtract(f.scale(12)).add(0, 3, 0));
            if (t == 630) {
                p.serverLevel().setDayTime(18000);
                p.serverLevel().setWeatherParameters(0, 6000, true, true);
                view(p, altar.add(-110, 20, -60), altar.add(0, 10, 0));
            }
        });
        for (int shot : new int[]{110, 210, 310, 410, 510, 610, 760}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_spire_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 780) mc.stop();
        return true;
    }

    /** {@code wings}: the Seraph Wings in the Curios back slot: resting, crouched, falling; from behind and in front. */
    private static boolean tickWings(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.serverLevel().setDayTime(6000);
                p.serverLevel().setWeatherParameters(12000, 0, false, false);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat.equip(p, "back",
                        new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.SERAPH_WINGS.get()));
            });
            return true;
        }
        int t = ++tick;
        if (t == 20) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        if (t == 90) mc.options.keyShift.setDown(true);
        if (t == 150) mc.options.keyShift.setDown(false);
        if (t == 180) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
        if (t == 250) {
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(p.getX(), p.getY() + 40, p.getZ());
            });
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        for (int shot : new int[]{80, 140, 240, 275}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_wings_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 300) mc.stop();
        return true;
    }

    private static BlockPos realAltar, realLectern;

    /**
     * {@code spire_real:x,z}: a spire the real world generator placed (find one with
     * {@code /locate structure supernaturalcraft:hymnal_spire} on a normal world copied into sn_preview).
     */
    private static boolean tickRealSpire(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        String[] xz = SCENES.substring("spire_real:".length()).split(",");
        int x = Integer.parseInt(xz[0].trim()), z = Integer.parseInt(xz[1].trim());
        if (tick < 0) {
            tick = 0;
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.serverLevel().setDayTime(6000);
                p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
                p.serverLevel().setWeatherParameters(12000, 0, false, false);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(x, 230, z);
            });
            return true;
        }
        int t = ++tick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            ServerLevel level = p.serverLevel();
            if (realAltar == null && t % 20 == 0) {
                for (int cx = (x >> 4) - 6; cx <= (x >> 4) + 6; cx++) {
                    for (int cz = (z >> 4) - 6; cz <= (z >> 4) + 6; cz++) {
                        for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                            if (be instanceof org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity) realAltar = be.getBlockPos();
                            if (be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity) realLectern = be.getBlockPos();
                        }
                    }
                }
                if (realAltar != null) org.papiricoh.supernaturalcraft.SupernaturalCraft.LOGGER.info("Real spire altar at {}, lectern at {}", realAltar, realLectern);
            }
            if (realAltar == null) return;
            Vec3 altar = Vec3.atCenterOf(realAltar);
            if (t == 100) view(p, altar.add(-70, 40, -70), altar.add(0, -20, 0));
            if (t == 200) view(p, altar.add(70, 25, 40), altar.add(0, -15, 0));
            if (t == 300) view(p, altar.add(-16, 8, -16), altar);
            if (t == 400 && realLectern != null) {
                Vec3 l = Vec3.atCenterOf(realLectern);
                view(p, l.add(l.subtract(altar).multiply(1, 0, 1).normalize().scale(-7)).add(0, 1, 0), l);
            }
            if (t == 500 && realLectern != null) {
                Vec3 l = Vec3.atCenterOf(realLectern);
                view(p, l.add(l.subtract(altar).multiply(1, 0, 1).normalize().scale(-22)).add(0, 14, 0), l);
            }
        });
        for (int shot : new int[]{180, 280, 380, 480, 580}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_realspire_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 600) mc.stop();
        return true;
    }
}
