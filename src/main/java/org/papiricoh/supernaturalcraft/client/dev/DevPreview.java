package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Development aid, inert unless the {@code SN_PREVIEW} environment variable is set. Stages each
 * scene in the loaded world, takes an in-game screenshot ("sn_&lt;scene&gt;.png" in the screenshots
 * folder) and quits when done. Lets the art pipeline be checked against the real renderer.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DevPreview {

    private static final String SCENES = System.getenv("SN_PREVIEW");
    private static final int SETTLE = 70;

    private record Scene(String name, double camDist, double camHeight, float camYawOffset, float pitch, Consumer<ServerLevel> setup) {
    }

    private static List<Scene> queue;
    private static int timer = -1, warmup = 200;
    private static final List<Entity> staged = new ArrayList<>();
    private static BlockPos origin;

    private DevPreview() {
    }

    private static int fightTick = -1;

    /**
     * "fight": a real duel. The player turns survival (but invulnerable) inside a fresh Cage and
     * Lucifer fights with his AI on; a screenshot every three seconds, phases forced on a timer.
     */
    private static boolean tickFight(Minecraft mc) {
        if (!"fight".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (fightTick < 0) {
            fightTick = 0;
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                BlockPos at = p.blockPosition();
                org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning.summon(level, at.offset(0, -1, -8), p);
            });
            return true;
        }
        fightTick++;
        int t = fightTick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var bosses = p.serverLevel().getEntitiesOfClass(LuciferEntity.class, p.getBoundingBox().inflate(60));
            if (bosses.isEmpty()) return;
            LuciferEntity l = bosses.getFirst();
            // Keep the camera on him.
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, l.getEyePosition());
            if (t == 700) l.setHealth(l.getMaxHealth() * 0.751f);
            if (t == 1300) l.setHealth(l.getMaxHealth() * 0.501f);
            if (t == 1900) l.setHealth(l.getMaxHealth() * 0.251f);
            if (t == 701 || t == 1301 || t == 1901) {
                l.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(p.serverLevel(),
                        org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SMITE, p), 5f);
            }
            if (t == 2600) {
                l.setAbsorptionAmount(0);
                l.setHealth(3f);
            }
            if (t == 2601) {
                l.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(p.serverLevel(),
                        org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SMITE, p), 10f);
            }
        });
        if (t % 60 == 0 && t > 0) {
            Screenshot.grab(mc.gameDirectory, String.format("sn_fight_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 2900) mc.stop();
        return true;
    }

    private static int cineTick = -1;

    /** "cinematic": Lucifer's camera sequences one after another (intro, phases 2-4, death). */
    private static boolean tickCinematic(Minecraft mc) {
        if (!"cinematic".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (cineTick < 0) {
            cineTick = 0;
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.serverLevel().setDayTime(6000);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning.summon(p.serverLevel(), p.blockPosition().offset(0, -1, -8), p);
            });
            return true;
        }
        int t = ++cineTick;
        // Threshold hits just after each phase may begin: P2 at 200, P3 at 400, P4 at 600, death at 900.
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var bosses = p.serverLevel().getEntitiesOfClass(LuciferEntity.class, p.getBoundingBox().inflate(60));
            if (bosses.isEmpty()) return;
            LuciferEntity l = bosses.getFirst();
            float[] at = {0.751f, 0.501f, 0.251f};
            for (int i = 0; i < 3; i++) {
                if (t == 200 + 200 * i) l.setHealth(l.getMaxHealth() * at[i]);
                if (t == 201 + 200 * i) l.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(p.serverLevel(),
                        org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SMITE, p), 5f);
            }
            if (t == 900) {
                l.setAbsorptionAmount(0);
                l.setHealth(3f);
            }
            if (t == 901) l.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(p.serverLevel(),
                    org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SMITE, p), 10f);
        });
        for (int shot : new int[]{15, 45, 75, 105, 225, 265, 425, 465, 630, 690, 760, 950, 1040, 1120}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_cine_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 1140) mc.stop();
        return true;
    }

    private static int amaraTick = -1;

    /** "amara": the Darkness under her eclipse, through every phase, with a hitbox check of her parts. */
    private static boolean tickAmara(Minecraft mc) {
        if (!"amara".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (amaraTick < 0) {
            amaraTick = 0;
            mc.options.hideGui = false;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
                org.papiricoh.supernaturalcraft.eclipse.Eclipses.begin(level, p.blockPosition(), 24000);
                org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning.summon(level, p.blockPosition().offset(0, 0, -14), p);
                p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
            });
            return true;
        }
        int t = ++amaraTick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var all = p.serverLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.class, p.getBoundingBox().inflate(60));
            if (all.isEmpty()) return;
            var a = all.getFirst();
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, a.position().add(0, 2.4, 0));
            var hit = p.serverLevel().damageSources().playerAttack(p);
            if (t == 420 || t == 790) {
                int from = a.phase() == 1 ? org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.FIRST_ANCHOR
                        : org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.FIRST_TENTACLE;
                int n = a.phase() == 1 ? 4 : 3;
                for (int i = 0; i < n; i++) a.hurtPart(a.part(from + i), hit, 1000f);
            }
            float[] at = {0, 0.701f, 0.401f, 0.101f};
            int[] when = {0, 500, 800, 1080};
            for (int ph = 1; ph <= 3; ph++) {
                if (t == when[ph]) a.setHealth(a.getMaxHealth() * at[ph]);
                if (t == when[ph] + 1) {
                    a.invulnerableTime = 0;
                    a.hurtPart(a.part(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.CORE), hit, 30f);
                }
            }
            if (t == 1420) a.setHealth(2f);
            if (t == 1421) {
                a.invulnerableTime = 0;
                a.hurt(hit, 30f);
            }
        });
        if (t == 395) mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
        if (t == 402) mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
        for (int shot : new int[]{60, 200, 330, 380, 400, 445, 560, 760, 870, 1060, 1150, 1400, 1500, 1700}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_amara_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 1860) mc.stop();
        return true;
    }

    private static int fxTick = -1;
    private static final java.util.Map<String, org.papiricoh.supernaturalcraft.entity.boss.BossAttack<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity>> FX_ATTACKS = new java.util.HashMap<>();

    /** "amara_fx": each of her later attacks fired by hand, for a look at the effects. */
    private static boolean tickAmaraFx(Minecraft mc) {
        if (!"amara_fx".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (fxTick < 0) {
            fxTick = 0;
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = p.serverLevel();
                level.setDayTime(6000);
                level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                p.setGameMode(GameType.CREATIVE);
                org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
                org.papiricoh.supernaturalcraft.eclipse.Eclipses.begin(level, p.blockPosition(), 24000);
                org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraSummoning.summon(level, p.blockPosition().offset(0, 0, -16), p);
            });
            return true;
        }
        int t = ++fxTick;
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            var all = p.serverLevel().getEntitiesOfClass(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity.class, p.getBoundingBox().inflate(60));
            if (all.isEmpty()) return;
            var a = all.getFirst();
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, a.position().add(0, 3.5, 0));
            if (t == 5 || t == 12 || t == 302 || t == 622) a.skipToFight();
            if (t == 10) a.beginTransition(2);
            if (t == 300) a.beginTransition(3);
            if (t == 620) a.beginTransition(4);
            fire(a, p, t, 30, "slam", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.TentacleSlam::new);
            fire(a, p, t, 90, "sweep", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.TentacleSweep::new);
            fire(a, p, t, 170, "grasp", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.Grasp::new);
            fire(a, p, t, 220, "spikes", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.BurrowingSpikes::new);
            fire(a, p, t, 320, "flare", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.CoronaFlare::new);
            fire(a, p, t, 440, "totality", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.Totality::new);
            fire(a, p, t, 540, "collapse", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.BlackSunCollapse::new);
            fire(a, p, t, 640, "unmake", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.Unmaking::new);
            fire(a, p, t, 760, "shades", org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAttacks.SpawnShades::new);
        });
        for (int shot : new int[]{50, 56, 135, 200, 262, 370, 400, 470, 505, 560, 710, 800}) {
            if (t == shot) Screenshot.grab(mc.gameDirectory, String.format("sn_fx_%04d.png", t), mc.getMainRenderTarget(), msg -> {
            });
        }
        if (t >= 830) mc.stop();
        return true;
    }

    /** Runs one attack's stages by hand: windup at {@code at}, active after it, ticking both. */
    private static void fire(org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity a, ServerPlayer p, int t, int at, String key,
                             java.util.function.Supplier<org.papiricoh.supernaturalcraft.entity.boss.BossAttack<org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity>> make) {
        var atk = FX_ATTACKS.get(key);
        if (t == at) {
            atk = make.get();
            FX_ATTACKS.put(key, atk);
            a.triggerAnim("action", atk.animation);
            atk.onWindup(a, p);
        }
        if (atk == null || t < at) return;
        int since = t - at;
        if (since > 0 && since < atk.windup) atk.tickWindup(a, p, since);
        if (since == atk.windup) atk.onActive(a, p);
        if (since > atk.windup && since < atk.windup + atk.active) atk.tickActive(a, p, since - atk.windup);
    }

    private static int guiTick = -1;

    /** "gui": the grimoire composer with a spell on the page, the HUD, then a journal page. */
    private static boolean tickGui(Minecraft mc) {
        if (!"gui".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        guiTick++;
        if (guiTick == 0) {
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                var reg = p.registryAccess().registryOrThrow(org.papiricoh.supernaturalcraft.registry.SNRegistries.SIGIL);
                reg.keySet().forEach(id -> org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).learn(id));
                org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).setMana(64);
                var stack = new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.GRIMOIRE.get());
                var spell = new org.papiricoh.supernaturalcraft.magic.spell.Spell(
                        java.util.Optional.of(SupernaturalCraft.asResource("bolt")),
                        List.of(SupernaturalCraft.asResource("smite"), SupernaturalCraft.asResource("hellfire")),
                        List.of(SupernaturalCraft.asResource("empower")), "Holy Lance");
                stack.set(org.papiricoh.supernaturalcraft.registry.AllDataComponents.SPELL_BOOK,
                        org.papiricoh.supernaturalcraft.magic.spell.SpellBook.EMPTY.withPage(0, spell));
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                p.getInventory().add(new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.SULFUR.get(), 5));
                org.papiricoh.supernaturalcraft.network.SNNetworking.syncArcana(p);
            });
        }
        if (guiTick == 60) Screenshot.grab(mc.gameDirectory, "sn_gui_hud.png", mc.getMainRenderTarget(), m -> {
        });
        if (guiTick == 70) mc.setScreen(new org.papiricoh.supernaturalcraft.client.screen.SpellComposerScreen());
        if (guiTick == 100) Screenshot.grab(mc.gameDirectory, "sn_gui_composer.png", mc.getMainRenderTarget(), m -> {
        });
        if (guiTick == 110) mc.setScreen(new org.papiricoh.supernaturalcraft.client.screen.JournalScreen(null));
        if (guiTick == 140) Screenshot.grab(mc.gameDirectory, "sn_gui_journal.png", mc.getMainRenderTarget(), m -> {
        });
        if (guiTick == 160) mc.stop();
        return true;
    }

    private static int weaponTick = -1;
    private static final List<net.minecraft.world.item.Item> SHOWCASE = new ArrayList<>();

    /** "weapons": each 3D weapon in first person and third person, then the inventory and the Hellforge. */
    private static boolean tickWeapons(Minecraft mc) {
        if (!"weapons".equals(SCENES)) return false;
        var server = mc.getSingleplayerServer();
        if (weaponTick < 0) {
            SHOWCASE.addAll(List.of(org.papiricoh.supernaturalcraft.registry.AllItems.SOUL_SCYTHE.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.HELLFIRE_GREATSWORD.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.CENSER_OF_GRACE.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.EXORCISTS_MACE.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.FIRST_BLADE.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.WHISPERING_CODEX.get(),
                    org.papiricoh.supernaturalcraft.registry.AllItems.PENUMBRA.get()));
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.serverLevel().setDayTime(6000);
                for (int i = 0; i < SHOWCASE.size(); i++) p.getInventory().setItem(i, new net.minecraft.world.item.ItemStack(SHOWCASE.get(i)));
                for (int i = 0; i < 9; i++) p.getInventory().setItem(9 + i, new net.minecraft.world.item.ItemStack(
                        org.papiricoh.supernaturalcraft.registry.AllItems.RUNES.get(org.papiricoh.supernaturalcraft.weapon.Rune.values()[i]).get()));
                p.getInventory().setItem(7, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.EMBER_STAFF.get()));
                p.getInventory().setItem(8, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.ENOCHIAN_ORB.get()));
                p.getInventory().setItem(22, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.SILVER_MACHETE.get()));
                p.experienceLevel = 30;
                p.getInventory().setItem(18, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.VOID_ESSENCE.get(), 5));
                p.getInventory().setItem(19, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.ECLIPSE_SIGHT.get()));
                p.getInventory().setItem(20, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.ECLIPSE_TROPHY.get()));
                p.getInventory().setItem(21, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.RUNES
                        .get(org.papiricoh.supernaturalcraft.weapon.Rune.VOID).get()));
                BlockPos trophy = p.blockPosition().relative(p.getDirection(), 3).relative(p.getDirection().getClockWise(), 1);
                p.serverLevel().setBlockAndUpdate(trophy, org.papiricoh.supernaturalcraft.registry.AllBlocks.ECLIPSE_TROPHY.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, p.getDirection().getOpposite()));
                p.serverLevel().setBlockAndUpdate(trophy.relative(p.getDirection().getCounterClockWise(), 2),
                        org.papiricoh.supernaturalcraft.registry.AllBlocks.MORNINGSTAR_TROPHY.get().defaultBlockState()
                                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, p.getDirection().getOpposite()));
                // Low sanity, so the HUD column and the hallucinations show up.
                org.papiricoh.supernaturalcraft.magic.mana.ManaManager.get(p).setSanity(20);
                org.papiricoh.supernaturalcraft.network.SNNetworking.syncArcana(p);
            });
            weaponTick = 0;
            return true;
        }
        weaponTick++;
        int per = 90, n = SHOWCASE.size();
        if (weaponTick == 1 && System.getenv("SN_WEAPON_FROM") != null) weaponTick = Integer.parseInt(System.getenv("SN_WEAPON_FROM")) * per;
        int idx = weaponTick / per, phase = weaponTick % per;
        if (idx < n) {
            if (phase == 1) {
                int slot = idx;
                server.execute(() -> server.getPlayerList().getPlayers().getFirst().getInventory().selected = slot);
                mc.player.getInventory().selected = idx;
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            if (phase == 55) Screenshot.grab(mc.gameDirectory, "sn_weapon_" + idx + "_fp.png", mc.getMainRenderTarget(), m -> {
            });
            if (phase == 57) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            if (phase == 85) Screenshot.grab(mc.gameDirectory, "sn_weapon_" + idx + "_tp.png", mc.getMainRenderTarget(), m -> {
            });
            return true;
        }
        int t = weaponTick - n * per;
        if (t == 1) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if (t == 20) Screenshot.grab(mc.gameDirectory, "sn_weapon_inventory.png", mc.getMainRenderTarget(), m -> {
        });
        if (t == 25) {
            mc.setScreen(null);
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                BlockPos at = p.blockPosition().relative(p.getDirection(), 2);
                p.serverLevel().setBlockAndUpdate(at, org.papiricoh.supernaturalcraft.registry.AllBlocks.HELLFORGE.get().defaultBlockState());
                p.openMenu(org.papiricoh.supernaturalcraft.registry.AllBlocks.HELLFORGE.get().defaultBlockState()
                        .getMenuProvider(p.serverLevel(), at));
                if (p.containerMenu instanceof org.papiricoh.supernaturalcraft.weapon.forge.HellforgeMenu menu) {
                    menu.container().setItem(0, new net.minecraft.world.item.ItemStack(org.papiricoh.supernaturalcraft.registry.AllItems.SOUL_SCYTHE.get()));
                    menu.container().setItem(1, p.getInventory().getItem(9).copy());
                    menu.container().setItem(2, p.getInventory().getItem(12).copy());
                    menu.broadcastChanges();
                }
            });
        }
        if (t == 50) Screenshot.grab(mc.gameDirectory, "sn_weapon_hellforge.png", mc.getMainRenderTarget(), m -> {
        });
        if (t == 60) mc.stop();
        return true;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (SCENES == null || SCENES.isBlank()) return;
        Minecraft mc = Minecraft.getInstance();
        // Worlds with datapack dimensions (Hell) ask for a backup first: load without one.
        if (mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen screen) {
            for (var child : screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button b
                        && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().equals("selectWorld.backupJoinSkipButton")) {
                    b.onPress();
                    return;
                }
            }
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        // The preview window is rarely focused; a paused game would freeze every scene.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (warmup-- > 0) return;
        if (tickFight(mc) || tickCinematic(mc) || tickAmara(mc) || tickAmaraFx(mc) || tickGui(mc) || tickWeapons(mc)
                || ChorusPreview.tick(mc) || ColtPreview.tick(mc) || HellPreview.tick(mc)
                || AzazelPreview.tick(mc) || LilithPreview.tick(mc) || MetatronPreview.tick(mc)
                || BowlPreview.tick(mc)) return;
        if (queue == null) {
            queue = new ArrayList<>(scenes(SCENES));
            mc.options.hideGui = true;
            origin = mc.player.blockPosition();
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SPECTATOR);
                p.serverLevel().setDayTime(6000);
                p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
                p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, mc.getSingleplayerServer());
            });
        }
        if (timer < 0) {
            if (queue.isEmpty()) {
                mc.stop();
                return;
            }
            Scene scene = queue.getFirst();
            mc.getSingleplayerServer().execute(() -> stage(mc, scene));
            timer = SETTLE;
            return;
        }
        if (--timer == 0) {
            Scene scene = queue.removeFirst();
            Screenshot.grab(mc.gameDirectory, "sn_" + scene.name + ".png", mc.getMainRenderTarget(), msg -> {
            });
            timer = -1;
        }
    }

    private static void stage(Minecraft mc, Scene scene) {
        ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        ServerLevel level = p.serverLevel();
        staged.forEach(Entity::discard);
        staged.clear();
        scene.setup.accept(level);
        double yaw = Math.toRadians(180 + scene.camYawOffset);
        double x = origin.getX() + 0.5 - Math.sin(yaw) * scene.camDist, z = origin.getZ() + 0.5 + Math.cos(yaw) * scene.camDist;
        float look = (float) Math.toDegrees(Math.atan2(-(origin.getX() + 0.5 - x), origin.getZ() + 0.5 - z));
        p.teleportTo(level, x, origin.getY() + scene.camHeight, z, look, scene.pitch);
    }

    private static <T extends Mob> T place(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, double dx, double dz, float yaw) {
        T e = type.create(level);
        e.moveTo(origin.getX() + 0.5 + dx, origin.getY(), origin.getZ() + 0.5 + dz, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setNoAi(true);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        staged.add(e);
        return e;
    }

    private static List<Scene> scenes(String spec) {
        List<Scene> out = new ArrayList<>();
        for (String name : spec.split(",")) {
            switch (name.trim()) {
                case "lucifer1", "lucifer2", "lucifer3", "lucifer4" -> {
                    int phase = name.trim().charAt(7) - '0';
                    out.add(new Scene(name.trim(), phase == 4 ? 9 : 6, 1.6, 0, 5,
                            level -> place(level, AllEntities.LUCIFER.get(), 0, 0, 180).forceLook(phase)));
                    out.add(new Scene(name.trim() + "_side", phase == 4 ? 9 : 6, 2.0, 140, 10,
                            level -> place(level, AllEntities.LUCIFER.get(), 0, 0, 180).forceLook(phase)));
                }
                case "arena" -> out.add(new Scene("arena", 16, 9, 30, 25, level -> {
                    LuciferEntity l = AllEntities.LUCIFER.get().create(level);
                    l.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 180, 0);
                    level.addFreshEntity(l);
                    staged.add(l);
                    var c = net.minecraft.world.phys.Vec3.atBottomCenterOf(origin);
                    staged.add(org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.circle(level, c.add(-5, 0, 3), 2.2f,
                            org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.RED, 400));
                    staged.add(org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.ring(level, c.add(5, 0, 3), 3f,
                            org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.BLUE, 400));
                    staged.add(org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.line(level, c.add(0, 0, 2), 0, 2.4f, 10,
                            org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.VIOLET, 400));
                    staged.add(org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.cone(level, c.add(0, 0, -2), 180, 6f,
                            org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker.GOLD, 400));
                }));
                case "demons" -> out.add(new Scene("demons", 5, 1.5, 0, 5, level -> {
                    BlackEyedDemon a = place(level, AllEntities.BLACK_EYED_DEMON.get(), -1.6, 0, 180);
                    BlackEyedDemon b = place(level, AllEntities.BLACK_EYED_DEMON.get(), 0, 0, 180);
                    var nbt = new net.minecraft.nbt.CompoundTag();
                    b.addAdditionalSaveData(nbt);
                    nbt.putInt("Variant", 1);
                    b.readAdditionalSaveData(nbt);
                    place(level, AllEntities.DEMON_OCCULTIST.get(), 1.6, 0, 180);
                }));
                case "ritual" -> out.add(new Scene("ritual", 7, 7, 0, 55, level -> {
                    BlockPos c = origin;
                    level.setBlockAndUpdate(c, AllBlocks.RITUAL_ALTAR.get().defaultBlockState());
                    List<String> great = List.of("  #####  ", " #  c  # ", "# c   c #", "#       #", "#   A   #", "#       #",
                            "# c   c #", " #  c  # ", "  #####  ");
                    for (PatternGeometry.Cell cell : PatternGeometry.cells(great)) {
                        level.setBlockAndUpdate(c.offset(cell.dx(), 0, cell.dz()), cell.symbol() == 'c'
                                ? Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)
                                : AllBlocks.BLOOD_CHALK_LINE.get().defaultBlockState());
                    }
                    for (int part = 0; part < 9; part++) {
                        level.setBlockAndUpdate(c.offset(6 + org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.dx(part), 0,
                                        org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.dz(part)),
                                AllBlocks.DEVILS_TRAP.get().defaultBlockState().setValue(org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock.PART, part));
                    }
                }));
                case "eclipse" -> {
                    // A lit camp at noon, then the eclipse rising over it (it fades in over 200 ticks).
                    out.add(new Scene("eclipse_0_clear", 9, 2.5, 20, -8, level -> {
                        org.papiricoh.supernaturalcraft.eclipse.Eclipses.end(level);
                        for (int i = 0; i < 4; i++) {
                            level.setBlockAndUpdate(origin.offset(i < 2 ? -3 : 3, 0, i % 2 == 0 ? -3 : 3), Blocks.TORCH.defaultBlockState());
                        }
                        level.setBlockAndUpdate(origin.offset(0, 0, 2), Blocks.CAMPFIRE.defaultBlockState());
                        place(level, net.minecraft.world.entity.EntityType.ZOMBIE, 1.5, 0, 180);
                        place(level, AllEntities.BLACK_EYED_DEMON.get(), -1.5, 0, 180);
                    }));
                    out.add(new Scene("eclipse_1_rising", 9, 2.5, 20, -8, level -> {
                        org.papiricoh.supernaturalcraft.eclipse.Eclipses.begin(level, origin, 6000);
                        place(level, net.minecraft.world.entity.EntityType.ZOMBIE, 1.5, 0, 180);
                        place(level, AllEntities.BLACK_EYED_DEMON.get(), -1.5, 0, 180);
                    }));
                    out.add(new Scene("eclipse_2_half", 9, 2.5, 20, -8, level -> {
                        place(level, net.minecraft.world.entity.EntityType.ZOMBIE, 1.5, 0, 180);
                        place(level, AllEntities.BLACK_EYED_DEMON.get(), -1.5, 0, 180);
                    }));
                    out.add(new Scene("eclipse_3_full", 9, 2.5, 20, -8, level -> {
                        place(level, net.minecraft.world.entity.EntityType.ZOMBIE, 1.5, 0, 180);
                        place(level, AllEntities.BLACK_EYED_DEMON.get(), -1.5, 0, 180);
                    }));
                    out.add(new Scene("eclipse_4_zenith", 9, 2.5, 20, -89, level -> {
                    }));
                    out.add(new Scene("eclipse_5_sky", 9, 2.5, 20, -50, level -> {
                    }));
                }
                default -> {
                }
            }
        }
        return out;
    }
}
