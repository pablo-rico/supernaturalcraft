package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.colt.ColtClient;
import org.papiricoh.supernaturalcraft.network.ColtInputPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ColtItem;

import java.util.HashMap;
import java.util.Map;

/**
 * "colt": the Colt in the real renderer. First person at rest, a shot that executes a demon
 * (frame by frame), a shot into stone, a reload, an inspection; then third person from behind
 * and in front, and a shot at night. SN_COLT_FROM=&lt;tick&gt; skips ahead.
 */
final class ColtPreview {

    private static int t = -1;
    private static final Map<String, Integer> TARGETS = new HashMap<>();

    private ColtPreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"colt".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        if (t < 0) {
            server.execute(() -> setup(server.getPlayerList().getPlayers().getFirst()));
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            t = 0;
            return true;
        }
        t++;
        if (t == 1 && System.getenv("SN_COLT_FROM") != null) t = Integer.parseInt(System.getenv("SN_COLT_FROM"));
        switch (t) {
            case 30 -> look(mc, null);
            case 50 -> shot(mc, "fp_idle");
            case 51 -> turn(mc, 35, -20);
            case 52 -> shot(mc, "fp_turn_1");
            case 53 -> turn(mc, -70, 70);
            case 54 -> shot(mc, "fp_turn_2");
            case 55 -> look(mc, "demon");
            case 60 -> fire(mc);
            case 61 -> shot(mc, "fp_fire_1");
            case 62 -> shot(mc, "fp_fire_2");
            case 64 -> shot(mc, "fp_fire_4");
            case 68 -> shot(mc, "fp_fire_8");
            case 72 -> shot(mc, "fp_fire_12");
            case 80 -> look(mc, "wall");
            case 85 -> fire(mc);
            case 87 -> shot(mc, "fp_impact");
            case 110 -> look(mc, null);
            case 115 -> ColtClient.key(ColtInputPayload.RELOAD);
            case 121 -> shot(mc, "fp_reload_intro");
            case 133 -> shot(mc, "fp_reload_insert");
            case 145 -> shot(mc, "fp_reload_end");
            case 170 -> ColtClient.key(ColtInputPayload.INSPECT);
            case 180 -> shot(mc, "fp_inspect_show");
            case 196 -> shot(mc, "fp_inspect_engraving");
            case 205 -> shot(mc, "fp_inspect_spin");
            case 230 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 240 -> look(mc, "golem");
            case 250 -> shot(mc, "tp_back_idle");
            case 252 -> fire(mc);
            case 254 -> shot(mc, "tp_back_fire");
            case 270 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 300 -> shot(mc, "tp_front_idle");
            case 302 -> fire(mc);
            case 304 -> shot(mc, "tp_front_fire");
            case 312 -> ColtClient.key(ColtInputPayload.RELOAD);
            case 325 -> shot(mc, "tp_front_reload");
            case 332 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> server.getPlayerList().getPlayers().getFirst().serverLevel().setDayTime(18000));
            }
            case 345 -> look(mc, "golem");
            case 350 -> fire(mc);
            case 351 -> shot(mc, "fp_night_fire");
            case 365 -> mc.stop();
            default -> {
            }
        }
        return true;
    }

    private static void setup(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true);
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.lock(level, false);
        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
        level.setWeatherParameters(6000, 0, false, false);
        level.setDayTime(6000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        BlockPos base = p.blockPosition();
        p.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
        ItemStack gun = new ItemStack(AllItems.THE_COLT.get());
        gun.set(AllDataComponents.COLT_AMMO, ColtItem.CAPACITY);
        p.getInventory().clearContent();
        p.getInventory().setItem(0, gun);
        p.getInventory().setItem(1, new ItemStack(AllItems.COLT_BULLET.get(), 16));
        p.getInventory().selected = 0;
        // Facing south: a demon ahead and to the left, a golem ahead right, a stone wall beyond.
        TARGETS.put("demon", place(level, AllEntities.BLACK_EYED_DEMON.get(), base.offset(-2, 0, 6)).getId());
        TARGETS.put("golem", place(level, EntityType.IRON_GOLEM, base.offset(3, 0, 9)).getId());
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, 14), Blocks.STONE_BRICKS.defaultBlockState());
        }
        TARGETS.put("wall", Integer.MIN_VALUE);
    }

    private static Entity place(ServerLevel level, EntityType<? extends Mob> type, BlockPos at) {
        Mob m = type.create(level);
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        m.setNoAi(true);
        m.setYHeadRot(180);
        level.addFreshEntity(m);
        return m;
    }

    /** Turns the player toward a target (or straight ahead), client side; the server hears it next tick. */
    private static void look(Minecraft mc, String target) {
        var p = mc.player;
        Vec3 eye = p.getEyePosition(), at;
        if (target == null) {
            at = eye.add(0, 0, 10);
        } else if ("wall".equals(target)) {
            at = new Vec3(p.getX() - 1.5, p.getY() + 1.5, p.getZ() + 13.5);
        } else {
            Entity e = mc.level.getEntity(TARGETS.get(target));
            at = e != null ? e.getBoundingBox().getCenter().add(0, 0.2, 0) : eye.add(0, 0, 10);
        }
        Vec3 d = at.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
        p.setYBodyRot(yaw);
    }

    /** Swings the view by a yaw and pitch (to check the hands hold steady as it moves). */
    private static void turn(Minecraft mc, float yaw, float pitch) {
        var p = mc.player;
        p.setYRot(p.getYRot() + yaw);
        p.setXRot(p.getXRot() + pitch);
    }

    private static void fire(Minecraft mc) {
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sn_colt_" + name + ".png", mc.getMainRenderTarget(), m -> {
        });
    }
}
