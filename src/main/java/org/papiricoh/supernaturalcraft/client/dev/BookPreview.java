package org.papiricoh.supernaturalcraft.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code SN_PREVIEW=book}: the Hunter's Book on a hunter halfway to the Cage (Azazel and Lilith
 * beaten, a deal open, a few creatures in the bestiary, a design in the library). Photographs every
 * {@link BookSection#previewShots() preview shot} of every tab ({@code SN_BOOK_TABS=home,roadmap}
 * picks some) at each GUI scale in {@code SN_BOOK_SCALES} (default {@code 2,3}): files
 * {@code sn_book_<tab>_<shot>_s<scale>.png}.
 */
final class BookPreview {

    private record Shot(HunterBookScreen.Tab tab, BookSection.PreviewShot shot, int scale) {
    }

    private static int t = -1;
    private static List<Shot> shots;
    private static int index, shotAt;

    private BookPreview() {
    }

    static boolean tick(Minecraft mc) {
        if (!"book".equals(System.getenv("SN_PREVIEW"))) return false;
        var server = mc.getSingleplayerServer();
        t++;
        if (t == 0) {
            mc.options.hideGui = false;
            server.execute(() -> setUp(server.getPlayerList().getPlayers().getFirst()));
            return true;
        }
        if (t == 50) server.execute(() -> {
            // Late, so nothing that runs on joining clears it: a couple of the mod's effects for the dashboard.
            ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(org.papiricoh.supernaturalcraft.registry.AllMobEffects.SECOND_SIGHT, 3600));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(org.papiricoh.supernaturalcraft.registry.AllMobEffects.CONCEALED, 2400));
        });
        if (t < 60) return true;
        if (shots == null) {
            shots = plan();
            shotAt = t - 1;
        }
        if (index >= shots.size()) {
            if (t > shotAt + 10) mc.stop();
            return true;
        }
        Shot s = shots.get(index);
        int local = t - shotAt;
        if (local == 1) {
            mc.options.guiScale().set(s.scale());
            mc.resizeDisplay();
            HunterBookScreen book = new HunterBookScreen(s.tab());
            mc.setScreen(book);
            s.shot().setup().accept(book);
        }
        if (local == 28) mc.getToasts().clear();
        if (local == 30) {
            String name = String.format("sn_book_%s_%s_s%d.png", s.tab().id(), s.shot().name(), s.scale());
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), m -> {
            });
        }
        if (local >= 32) {
            index++;
            shotAt = t;
        }
        return true;
    }

    private static List<Shot> plan() {
        String tabs = Optional.ofNullable(System.getenv("SN_BOOK_TABS")).orElse("home,journal,scriptorium,roadmap");
        String scales = Optional.ofNullable(System.getenv("SN_BOOK_SCALES")).orElse("2,3");
        List<Shot> out = new ArrayList<>();
        for (String scale : scales.split(",")) {
            for (String tab : tabs.split(",")) {
                HunterBookScreen.Tab which = HunterBookScreen.Tab.valueOf(tab.trim().toUpperCase(java.util.Locale.ROOT));
                // A throwaway book just to ask the section for its shots.
                HunterBookScreen probe = new HunterBookScreen(which);
                for (BookSection.PreviewShot shot : probe.section().previewShots()) {
                    out.add(new Shot(which, shot, Integer.parseInt(scale.trim())));
                }
            }
        }
        return out;
    }

    /** A hunter halfway along: two bosses down, a deal open, a little of everything seen. */
    private static void setUp(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true);
        p.setHealth(p.getMaxHealth());
        p.teleportTo(p.serverLevel(), 400.5, 100, 400.5, 0, 0);
        p.getAbilities().flying = true;
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
        for (String adv : List.of("main/root", "main/black_eyes", "main/fine_print", "main/christo", "main/caught_in_the_trap",
                "main/yellow_eyed", "main/lock_and_key", "main/lucifer_rising", "main/first_spell", "main/deal_with_the_devil",
                "main/hymnal_spire")) {
            ChorusRewards.award(p, adv);
        }
        var arcana = ManaManager.get(p);
        for (String sigil : List.of("touch", "bolt", "burst", "smite", "mend", "frost", "hellfire", "empower", "extend")) {
            arcana.learn(SupernaturalCraft.asResource(sigil));
        }
        arcana.learnRite(SupernaturalCraft.asResource("second_sight"));
        arcana.learnRite(SupernaturalCraft.asResource("locate"));
        arcana.setMana(72);
        arcana.setSanity(64);
        HunterLog log = HunterLogs.get(p);
        for (EntityType<?> type : List.<EntityType<?>>of(AllEntities.BLACK_EYED_DEMON.get(), AllEntities.DEMON_OCCULTIST.get(),
                AllEntities.HELLHOUND.get(), AllEntities.AZAZEL.get(), AllEntities.LILITH.get(), AllEntities.GHOST.get())) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
            for (int i = 0; i < 3; i++) log.kill(id);
        }
        for (var item : List.of(AllItems.SALT.get(), AllItems.HOLY_WATER.get(), AllItems.GRIMOIRE.get(), AllItems.DEMON_BLOOD.get(),
                AllItems.KEY_TO_THE_CAGE.get(), AllItems.LAST_SEAL.get(), AllItems.SPELL_BOWL.get())) {
            log.obtain(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
        }
        log.markRead(SupernaturalCraft.asResource("family_business"));
        if (!log.bookmarks().contains(SupernaturalCraft.asResource("family_business"))) {
            log.toggleBookmark(SupernaturalCraft.asResource("family_business"));
        }
        log.setDesign(0, new Spell(Optional.of(SupernaturalCraft.asResource("bolt")),
                List.of(SupernaturalCraft.asResource("smite"), SupernaturalCraft.asResource("hellfire")),
                List.of(SupernaturalCraft.asResource("empower")), "Holy Lance"));
        log.setDesign(1, new Spell(Optional.of(SupernaturalCraft.asResource("burst")),
                List.of(SupernaturalCraft.asResource("frost")), List.of(SupernaturalCraft.asResource("extend")), "Cold Snap"));
        // A side (v0.13): a Prince of Hell by default, or SN_BOOK_FACTION=angel|human; its ranks on the allegiance road.
        String side = Optional.ofNullable(System.getenv("SN_BOOK_FACTION")).orElse("demon");
        var faction = switch (side) {
            case "angel" -> org.papiricoh.supernaturalcraft.allegiance.Faction.ANGEL;
            case "human" -> org.papiricoh.supernaturalcraft.allegiance.Faction.HUMAN;
            default -> org.papiricoh.supernaturalcraft.allegiance.Faction.DEMON;
        };
        var allegiance = org.papiricoh.supernaturalcraft.allegiance.Allegiance.HUMAN.convert(faction).withRank(2);
        org.papiricoh.supernaturalcraft.allegiance.Allegiances.set(p, allegiance.withEssence(allegiance.maxEssence() * 0.64f));
        List<String> ranks = switch (faction) {
            case ANGEL -> List.of("main/heeded_the_call", "main/angel_1", "main/angel_2");
            case DEMON -> List.of("main/soul_bound", "main/demon_1", "main/demon_2");
            case HUMAN -> List.of("main/hunter_1", "main/hunter_2");
        };
        for (String adv : ranks) ChorusRewards.award(p, adv);
        long now = Debts.now(p);
        Debts.set(p, CrossroadsDeal.sealed(DealTerms.Wish.UPGRADE, 0, now).withDueAt(now + 24000L * 3 + 6000));
        p.getInventory().clearContent();
        p.getInventory().add(new net.minecraft.world.item.ItemStack(AllItems.GRIMOIRE.get()));
        p.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER, 12));
        p.getInventory().add(new net.minecraft.world.item.ItemStack(AllItems.ENOCHIAN_INK.get(), 5));
        SNNetworking.syncArcana(p);
        HunterLogs.sync(p);
        HunterLogs.syncLibrary(p);
    }
}
