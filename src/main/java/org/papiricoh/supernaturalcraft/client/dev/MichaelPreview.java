package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.client.michael.ClientMichael;
import org.papiricoh.supernaturalcraft.client.michael.VesselScreen;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.MichaelLanceEntity;
import org.papiricoh.supernaturalcraft.network.MichaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.michael.HeavenLedger;

import java.util.ArrayList;
import java.util.List;

/**
 * The Archangel Michael in the real renderer, well away from the origin.
 * <ul>
 *   <li>{@code SN_PREVIEW=michael_model}: the vessel, the shadow wings, the lance thrown, the true form, the broken halo,
 *   the Host (three vessels and a captain), the lance in the ground, the General's armour on the player, the trophy,
 *   hitboxes.</li>
 *   <li>{@code SN_PREVIEW=michael_fight}: a real fight over the shoulder (the player cannot be hurt and flies, but is no
 *   spectator), every phase forced along the way.</li>
 *   <li>{@code SN_PREVIEW=michael_arena}: the three Heavens written in turn, from above and from inside.</li>
 *   <li>{@code SN_PREVIEW=michael_hud}: the celestial boss bar, each title card, the "yes" screen, being worn, the mark,
 *   the wings' stamina.</li>
 * </ul>
 * Screenshots land in {@code runs/client/screenshots/sn_michael_*}.
 */
final class MichaelPreview {

    private static int t = -1;
    private static BlockPos altar;
    private static MichaelEntity boss;
    private static final List<Entity> shown = new ArrayList<>();

    private MichaelPreview() {
    }

    static boolean tick(Minecraft mc) {
        String scene = System.getenv("SN_PREVIEW");
        if (scene == null || !scene.startsWith("michael_")) return false;
        int end = switch (scene) {
            case "michael_model" -> 560;
            case "michael_fight" -> 1700;
            case "michael_arena" -> 520;
            case "michael_hud" -> 800;
            default -> -1;
        };
        if (end < 0) return false;
        var server = mc.getSingleplayerServer();
        t++;
        int now = t;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            switch (scene) {
                case "michael_model" -> model(mc, p, now);
                case "michael_fight" -> fight(mc, p, now);
                case "michael_arena" -> arena(mc, p, now);
                default -> hud(mc, p, now);
            }
        });
        if ("michael_hud".equals(scene)) hudClient(mc, now);
        int[] shots = switch (scene) {
            case "michael_model" -> new int[]{50, 90, 130, 170, 210, 250, 290, 330, 370, 410, 450, 490, 530};
            case "michael_arena" -> new int[]{110, 150, 270, 310, 430, 470};
            case "michael_hud" -> new int[]{60, 140, 190, 250, 310, 370, 430, 470, 530, 590, 660, 720, 780};
            default -> null;
        };
        if (shots != null) {
            for (int s : shots) if (now == s) grab(mc, "sn_" + scene + "_%04d.png", now);
        } else if (now >= 60 && now % 40 == 0 && now < end - 10) {
            grab(mc, "sn_michael_fight_%04d.png", now);
        }
        if (now >= end) mc.stop();
        return true;
    }

    private static void grab(Minecraft mc, String pattern, int now) {
        Screenshot.grab(mc.gameDirectory, String.format(pattern, now), mc.getMainRenderTarget(), m -> {
        });
    }

    private static void view(ServerPlayer p, Vec3 from, Vec3 at) {
        p.teleportTo(p.serverLevel(), from.x, from.y, from.z, p.getYRot(), p.getXRot());
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void setUp(ServerLevel level, int x, int z) {
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
        ArenaSavedData.get(level).removeClosed();
        level.getChunk(x >> 4, z >> 4);
        altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    private static void clearShown() {
        for (Entity e : shown) e.discard();
        shown.clear();
    }

    // --- the models ------------------------------------------------------------------------------------------------

    private static void model(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            setUp(level, 620, 412);
            Vec3 c = Vec3.atBottomCenterOf(altar);
            MichaelEntity m = AllEntities.MICHAEL.get().create(level);
            m.setNoAi(true);
            m.moveTo(c.x, c.y, c.z, 180, 0);
            m.setYHeadRot(180);
            m.setYBodyRot(180);
            level.addFreshEntity(m);
            shown.add(m);
            boss = m;
            view(p, c.add(0, 1.5, -2.8), c.add(0, 1.1, 0));
        }
        if (altar == null) return;
        Vec3 c = Vec3.atBottomCenterOf(altar);
        if (boss != null) vessel(p, c, now);
        else host(mc, p, level, c, now);
    }

    /** Michael himself: the vessel, the shadow wings, the lance thrown, the true form, the broken halo. */
    private static void vessel(ServerPlayer p, Vec3 c, int now) {
        if (now == 70) view(p, c.add(2.2, 1.5, -2), c.add(0, 1.1, 0));
        if (now == 110) {
            // Phase III: shadow wings and the lance.
            boss.forceLook(3);
            boss.setWings(MichaelEntity.WINGS_SHADOW);
            view(p, c.add(0, 1.8, -3.8), c.add(0, 1.3, 0));
        }
        if (now == 150) view(p, c.add(0, 1.8, 3.8), c.add(0, 1.3, 0));
        if (now == 160) boss.setLanceHeld(false);
        if (now == 190) {
            // The true form.
            boss.setLanceHeld(true);
            boss.forceLook(5);
            boss.setArchangel(true);
            view(p, c.add(0, 3.0, -6.5), c.add(0, 2.6, 0));
        }
        if (now == 230) view(p, c.add(-4.5, 3.2, -4.5), c.add(0, 2.6, 0));
        if (now == 270) {
            boss.forceLook(6);
            boss.setHaloBroken(true);
            view(p, c.add(0, 5.5, 4), c.add(0, 4.2, 0));
        }
        if (now == 305) {
            clearShown();
            boss = null;
        }
    }

    /** The Host, the lance in the ground, the armour on the player and the trophy, the two hitboxes. */
    private static void host(Minecraft mc, ServerPlayer p, ServerLevel level, Vec3 c, int now) {
        if (now == 310) {
            // The Host: the three vessels and a captain.
            for (int i = 0; i < 4; i++) {
                HostAngelEntity h = AllEntities.HOST_ANGEL.get().create(level);
                h.finalizeSpawn(level, level.getCurrentDifficultyAt(altar), MobSpawnType.MOB_SUMMONED, null);
                h.setNoAi(true);
                h.setVessel(i);
                h.setCaptain(i == 3);
                h.moveTo(c.x - 3 + i * 2, c.y, c.z, 180, 0);
                h.setYHeadRot(180);
                h.setYBodyRot(180);
                level.addFreshEntity(h);
                shown.add(h);
            }
            view(p, c.add(0, 1.8, -4.5), c.add(0, 1.2, 0));
        }
        if (now == 350) view(p, c.add(3.5, 1.8, -2.5), c.add(0, 1.2, 0));
        if (now == 380) {
            clearShown();
            // The lance standing in the ground (Michael kept out of sight below the floor so it is his).
            MichaelEntity owner = AllEntities.MICHAEL.get().create(level);
            owner.setNoAi(true);
            owner.setInvisible(true);
            owner.moveTo(c.x, c.y - 6, c.z, 0, 0);
            level.addFreshEntity(owner);
            shown.add(owner);
            MichaelLanceEntity lance = MichaelLanceEntity.hurl(owner, c.add(0, 0, 0));
            lance.setPos(c.x, c.y + 3, c.z);
            lance.shoot(0, -1, 0.1, 1.5f, 0);
            level.addFreshEntity(lance);
            shown.add(lance);
            view(p, c.add(0, 1.6, -2.8), c.add(0, 0.8, 0));
        }
        if (now == 420) {
            // The General's armour on the player, from the front, the trophy beside.
            clearShown();
            p.setGameMode(GameType.CREATIVE);
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AllItems.GENERAL_HELMET.get()));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllItems.GENERAL_CHESTPLATE.get()));
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AllItems.GENERAL_LEGGINGS.get()));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(AllItems.GENERAL_BOOTS.get()));
            level.setBlockAndUpdate(altar.east(2), AllBlocks.MICHAEL_TROPHY.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH));
            p.teleportTo(level, c.x, c.y, c.z, 0, 0);
            mc.execute(() -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        }
        if (now == 470) {
            // Hitboxes: the vessel and the true form side by side.
            mc.execute(() -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            p.setGameMode(GameType.SPECTATOR);
            for (int i = 0; i < 2; i++) {
                MichaelEntity m = AllEntities.MICHAEL.get().create(level);
                m.setNoAi(true);
                m.moveTo(c.x - 2 + i * 4, c.y, c.z + 3, 180, 0);
                if (i == 1) {
                    m.forceLook(5);
                    m.setArchangel(true);
                }
                level.addFreshEntity(m);
                shown.add(m);
            }
            view(p, c.add(0, 2.6, -3.5), c.add(0, 2, 3));
            mc.execute(() -> mc.getEntityRenderDispatcher().setRenderHitBoxes(true));
        }
        if (now == 545) {
            mc.execute(() -> mc.getEntityRenderDispatcher().setRenderHitBoxes(false));
            clearShown();
            level.removeBlock(altar.east(2), false);
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                p.setItemSlot(s, ItemStack.EMPTY);
            }
        }
    }

    // --- a real fight ------------------------------------------------------------------------------------------------

    private static void fight(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            setUp(level, 720, 520);
            Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -11);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 10);
        }
        if (now == 10) boss = MichaelSummoning.summon(level, altar, p);
        if (boss == null || boss.isRemoved()) return;
        if (now % 10 == 0 && boss.arena() != null && (p.distanceTo(boss) > 16 || boss.arena().horizontalDistance(p.position()) > boss.arena().radius() - 3)) {
            Vec3 c = boss.arena().centerVec();
            Vec3 toward = c.subtract(boss.position()).multiply(1, 0, 1);
            Vec3 at = boss.position().add(toward.lengthSqr() < 1 ? new Vec3(0, 0, -9) : toward.normalize().scale(9)).add(0, 1.5, 0);
            p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        }
        if (now % 5 == 0) {
            Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 side = new Vec3(-to.z, 0, to.x).normalize().scale(Math.max(2.5, to.length() * 0.45));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(side).add(0, boss.getBbHeight() * 0.6, 0));
        }
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        // Phases II to VI, forced along the way.
        int[] at = {330, 560, 800, 1060, 1340};
        for (int i = 0; i < at.length; i++) {
            if (now == at[i]) {
                float share = MichaelBalance.threshold(i + 1) + 0.005f;
                boss.setHealth(boss.getMaxHealth() * share);
                boss.invulnerableTime = 0;
                boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 40f);
            }
        }
        if (now == 1690) {
            boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
    }

    // --- the three Heavens -----------------------------------------------------------------------------------------

    private static void arena(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = true;
            p.setGameMode(GameType.SPECTATOR);
            setUp(level, 820, 412);
        }
        if (now == 10) {
            boss = MichaelSummoning.summon(level, altar, null);
            if (boss != null) {
                boss.setNoAi(true);
                boss.setInvisible(true);
            }
        }
        if (boss == null) return;
        Vec3 c = Vec3.atBottomCenterOf(altar);
        int[] starts = {60, 220, 380};
        for (int which = 0; which < 3; which++) {
            int s = starts[which];
            if (now == s) {
                boss.layHeavenNow(which);
                int w = which;
                mc.execute(() -> ClientMichael.forceHeaven(w));
                view(p, c.add(0, 30, -30), c);
            }
            if (now == s + 70) view(p, c.add(-9, 3, -9), c.add(4, 1.5, 4));
        }
        if (now == 510) {
            boss.discard();
            mc.execute(() -> ClientMichael.forceHeaven(-1));
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
    }

    // --- the HUD -----------------------------------------------------------------------------------------------------

    private static void hud(Minecraft mc, ServerPlayer p, int now) {
        ServerLevel level = p.serverLevel();
        if (now == 1) {
            mc.options.hideGui = false;
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
            setUp(level, 920, 520);
            Vec3 stand = Vec3.atBottomCenterOf(altar).add(0, 0, -9);
            p.teleportTo(level, stand.x, stand.y, stand.z, 0, 0);
        }
        if (now == 10) boss = MichaelSummoning.summon(level, altar, p);
        if (boss != null && !boss.isRemoved() && now % 10 == 0) {
            p.lookAt(EntityAnchorArgument.Anchor.EYES, boss.position().add(0, 1.5, 0));
            p.setHealth(p.getMaxHealth());
        }
        // Struck now and then, so feathers fall off the bar.
        if (boss != null && now > 190 && now < 330 && now % 25 == 0) {
            boss.invulnerableTime = 0;
            boss.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, p), 30f);
        }
        if (now == 640) {
            // The wings on Michael's Grace: worn in the chest slot, flying.
            p.setData(AllAttachments.HEAVEN, new HeavenLedger(0, true, true));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AllItems.SERAPH_WINGS.get()));
            p.teleportTo(level, p.getX(), p.getY() + 4, p.getZ(), p.getYRot(), p.getXRot());
        }
        if (now == 645) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        if (now == 790) {
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            p.setData(AllAttachments.HEAVEN, HeavenLedger.NONE);
            if (boss != null) boss.discard();
            for (ArenaController a : List.copyOf(ArenaSavedData.get(level).all())) a.restoreNow(level);
            ArenaSavedData.get(level).removeClosed();
        }
    }

    /** The HUD's moments, played on the client as the server would send them. */
    private static void hudClient(Minecraft mc, int now) {
        if (mc.player == null) return;
        int id = boss != null ? boss.getId() : -1;
        // He comes down with his own title (phase I); then the others, one after another.
        if (now == 240) ClientMichael.handle(new MichaelFxPayload(id, MichaelFxPayload.TITLE, 5, 0, Vec3.ZERO, 140));
        if (now == 300) ClientMichael.handle(new MichaelFxPayload(id, MichaelFxPayload.TITLE, 6, 0, Vec3.ZERO, 140));
        if (now == 360) ClientMichael.handle(new MichaelFxPayload(id, MichaelFxPayload.TITLE, MichaelFxPayload.TITLE_DEATH, 0, Vec3.ZERO, 140));
        if (now == 420) mc.setScreen(new VesselScreen(id, MichaelBalance.YES_DECIDE_TICKS));
        if (now == 450 && mc.screen instanceof VesselScreen) mc.setScreen(null);
        if (now == 455) ClientMichael.forcePossessed(60);
        if (now == 520) ClientMichael.forceMark(mc.player.getId(), 120);
        if (now == 570) ClientMichael.handle(new MichaelFxPayload(id, MichaelFxPayload.FAVOR, 0, 0, Vec3.ZERO, 120));
        if (now == 580) ClientMichael.handle(new MichaelFxPayload(id, MichaelFxPayload.FEATHER_BURST, 20, 0, mc.player.position(), 20));
    }
}
