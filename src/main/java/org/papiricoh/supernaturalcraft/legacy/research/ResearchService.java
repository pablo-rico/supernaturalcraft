package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.LegacyOrder;
import org.papiricoh.supernaturalcraft.legacy.LegacyRules;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;
import org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;
import org.papiricoh.supernaturalcraft.network.ResearchBoardPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The research engine on the server (v0.17): gathers a hunter's {@link ResearchBoard.State} from the world, starts research
 * (paying from the inventory), cancels it, and finishes what is due ({@link ResearchRewards}). The desk's menu, the payload
 * handler, the ticker and the commands all come through here.
 */
public final class ResearchService {

    /** The Men of Letters Ring takes this much off research time. */
    public static final double RING_TIME = 0.9;

    private ResearchService() {
    }

    // --- The board ---------------------------------------------------------------------------------------------------------

    /** How fast research runs for {@code player}: the config, and the ring (−10 % time). */
    public static double speed(Player player) {
        double s = SNConfig.LEGACY_RESEARCH_SPEED.get();
        if (LegacyOrder.wears(player, AllItems.MEN_OF_LETTERS_RING.get())) s /= RING_TIME;
        s /= org.papiricoh.supernaturalcraft.memory.MemoryBonuses.researchTimeFactor(player);
        return s;
    }

    /** Research {@code player} can run at once. */
    public static int maxSlots(Player player) {
        return LegacyRules.slots(Legacies.rank(player), LegacyOrder.wears(player, AllItems.AQUARIAN_STAR.get()));
    }

    /** Whether a creature has a file: supernatural, or a vanilla hostile; never a boss. */
    public static boolean fileable(EntityType<?> type) {
        if (type.is(AllTags.Entities.BOSSES) || type.is(Tags.EntityTypes.BOSSES)) return false;
        if (type.is(AllTags.Entities.SUPERNATURAL)) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return "minecraft".equals(id.getNamespace()) && type.getCategory() == MobCategory.MONSTER;
    }

    public static ResearchBoard.State state(Player player) {
        Archive archive = Legacies.archive(player);
        HunterLog log = HunterLogs.get(player);
        List<ResearchBoard.Creature> creatures = new ArrayList<>();
        for (ResourceLocation id : log.seen()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type != null && fileable(type)) creatures.add(new ResearchBoard.Creature(id.toString(), type.is(AllTags.Entities.SUPERNATURAL)));
        }
        List<ResearchBoard.Boss> bosses = new ArrayList<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("supernaturalcraft", b.entity);
            if (log.hasSeen(id)) bosses.add(new ResearchBoard.Boss(id.toString(), ProgressionScale.of(b).tier()));
        }
        Map<Long, ResearchBoard.Artifact> artifacts = new LinkedHashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ArtifactData d = Artifacts.data(player.getInventory().getItem(i));
            if (d != null && !d.identified()) artifacts.putIfAbsent(d.seed(), new ResearchBoard.Artifact(d.seed(), d.form(), d.rarity()));
        }
        List<ResearchBoard.SolvedCase> cases = new ArrayList<>();
        for (CaseFile c : Legacies.get(player).cases()) {
            if (c.state() == CaseFile.SOLVED) cases.add(new ResearchBoard.SolvedCase(c.index(), c.monster().toString(), c.tier()));
        }
        Set<String> running = new HashSet<>();
        for (ResearchSlot s : archive.slots()) running.add(s.topic());
        return new ResearchBoard.State(Legacies.rank(player), archive.researched(), archive.files(), archive.counter("formula"),
                archive.counter("rite"), running, creatures, bosses, List.copyOf(artifacts.values()), cases);
    }

    public static List<ResearchBoard.Topic> board(Player player) {
        return ResearchBoard.board(state(player), speed(player));
    }

    public static @Nullable ResearchBoard.Topic find(Player player, String topic) {
        return ResearchBoard.find(state(player), topic, speed(player));
    }

    /** The cost as stacks, notes first. */
    public static List<ItemStack> stacks(ResearchBoard.Cost cost) {
        List<ItemStack> out = new ArrayList<>();
        if (cost.noteCount() > 0) out.add(FieldNotesItem.stack(cost.notes(), cost.noteCount()));
        if (cost.paper() > 0) out.add(new ItemStack(item(ResearchBoard.PAPER), cost.paper()));
        if (cost.ink() > 0) out.add(new ItemStack(item(ResearchBoard.INK), cost.ink()));
        if (!cost.reagent().isEmpty() && cost.reagentCount() > 0) out.add(new ItemStack(item(cost.reagent()), cost.reagentCount()));
        return out;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** Whether a stack pays for {@code want}: same item, and field notes on the same topic. */
    static boolean pays(ItemStack have, ItemStack want) {
        if (!have.is(want.getItem())) return false;
        if (want.is(AllItems.FIELD_NOTES.get())) return FieldNotesItem.topic(have).equals(FieldNotesItem.topic(want));
        return true;
    }

    public static boolean canPay(Player player, List<ItemStack> cost) {
        if (player.getAbilities().instabuild) return true;
        for (ItemStack want : cost) {
            int have = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (pays(s, want)) have += s.getCount();
            }
            if (have < want.getCount()) return false;
        }
        return true;
    }

    static void pay(Player player, List<ItemStack> cost) {
        if (player.getAbilities().instabuild) return;
        for (ItemStack want : cost) {
            int left = want.getCount();
            for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!pays(s, want)) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        player.getInventory().setChanged();
    }

    public static ResearchOffer offer(Player player, ResearchBoard.Topic t) {
        List<ItemStack> cost = stacks(t.cost());
        return new ResearchOffer(t.topic(), t.tier(), cost, t.ticks(), t.titleKey(), t.args(), canPay(player, cost));
    }

    /** Sends the board of the open desk (menu {@code container}) to its hunter. */
    public static void sendBoard(ServerPlayer player, int container) {
        if (player.connection == null) return;
        List<ResearchOffer> offers = board(player).stream().map(t -> offer(player, t)).toList();
        PacketDistributor.sendToPlayer(player, new ResearchBoardPayload(container, maxSlots(player), offers));
    }

    // --- Starting and cancelling -------------------------------------------------------------------------------------------

    /**
     * Starts research on {@code topic}: checks membership, that the board offers it, a free slot and the cost; pays and fills a
     * slot.
     *
     * @return null if it started, else a lang key saying why not
     */
    public static @Nullable String start(ServerPlayer player, String topic) {
        if (!Legacies.member(player)) return "message.supernaturalcraft.research.not_member";
        Archive archive = Legacies.archive(player);
        if (archive.slots().size() >= maxSlots(player)) return "message.supernaturalcraft.research.no_slot";
        ResearchBoard.Topic t = find(player, topic);
        if (t == null) return "message.supernaturalcraft.research.not_offered";
        List<ItemStack> cost = stacks(t.cost());
        if (!canPay(player, cost)) return "message.supernaturalcraft.research.cannot_pay";
        pay(player, cost);
        long now = player.level().getGameTime();
        List<ResearchSlot> slots = new ArrayList<>(archive.slots());
        slots.add(new ResearchSlot(t.topic(), now, now + t.ticks()));
        Legacies.setArchive(player, archive.withSlots(slots));
        player.level().playSound(null, player.blockPosition(), AllSounds.LEGACY_TYPEWRITER.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        return null;
    }

    /** Drops running research on {@code topic} (nothing is given back). */
    public static boolean cancel(ServerPlayer player, String topic) {
        Archive archive = Legacies.archive(player);
        List<ResearchSlot> slots = new ArrayList<>(archive.slots());
        if (!slots.removeIf(s -> s.topic().equals(topic))) return false;
        Legacies.setArchive(player, archive.withSlots(slots));
        player.level().playSound(null, player.blockPosition(), AllSounds.LEGACY_PAPER.get(), SoundSource.PLAYERS, 0.7f, 0.8f);
        return true;
    }

    // --- Finishing ---------------------------------------------------------------------------------------------------------

    /**
     * Finishes every slot that is due; also identifies any carried artifact whose research is done (it may have been elsewhere
     * when the research finished).
     *
     * @return how many research finished
     */
    public static int tick(ServerPlayer player) {
        identifyCarried(player);
        Archive archive = Legacies.archive(player);
        if (archive.slots().isEmpty()) return 0;
        long now = player.level().getGameTime();
        int done = 0;
        for (ResearchSlot s : archive.slots()) {
            if (s.done(now)) {
                finish(player, s.topic());
                done++;
            }
        }
        return done;
    }

    /** Ends every running research now (command). */
    public static int finishAll(ServerPlayer player) {
        int n = 0;
        for (ResearchSlot s : Legacies.archive(player).slots()) {
            finish(player, s.topic());
            n++;
        }
        return n;
    }

    /**
     * Finishes {@code topic} (running or not): its reward, the archive entry, the slot freed, the toast and a rank check.
     */
    public static void finish(ServerPlayer player, String topic) {
        Archive archive = Legacies.archive(player);
        List<ResearchSlot> slots = new ArrayList<>(archive.slots());
        slots.removeIf(s -> s.topic().equals(topic));
        archive = ResearchRewards.reward(player, archive.withSlots(slots), topic).finish(topic);
        Legacies.setArchive(player, archive);
        identifyCarried(player);
        player.level().playSound(null, player.blockPosition(), AllSounds.LEGACY_RESEARCH_DONE.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.research.done",
                ResearchRewards.title(archive, topic)).withStyle(ChatFormatting.DARK_AQUA), false);
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new LegacyFxPayload(LegacyFxPayload.RESEARCH_DONE, player.getId(), 0, topic));
        }
        LegacyOrder.checkRank(player);
    }

    /** Marks carried artifacts identified when their research is done. */
    static void identifyCarried(ServerPlayer player) {
        Archive archive = Legacies.archive(player);
        if (archive.finished().getOrDefault(TopicKind.ARTIFACT.id(), 0) == 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            ArtifactData d = Artifacts.data(s);
            if (d != null && !d.identified() && archive.knows(TopicKind.ARTIFACT.topic(String.valueOf(d.seed())))) {
                s.set(AllDataComponents.ARTIFACT.get(), d.identify());
            }
        }
    }
}
