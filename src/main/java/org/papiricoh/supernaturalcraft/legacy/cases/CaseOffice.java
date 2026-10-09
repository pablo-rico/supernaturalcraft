package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMapDecorations;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Handing out cases (v0.17): Henry or the war room's map table rolls the hunter's next case ({@link CaseGenerator}), files it in
 * their {@link Legacy}, and hands over the case file and a map marked with the site. One open case at a time.
 */
public final class CaseOffice {

    /** Closed cases kept in the record (the research board offers {@code case:<index>} for solved ones). */
    public static final int KEEP_CLOSED = 8;
    public static final int MAP_COLOR = 0x7A2E22;
    public static final String MAP_NAME = "item.supernaturalcraft.case_map";

    private CaseOffice() {
    }

    /** The hunter's open (or active) case, or null. */
    public static @Nullable CaseFile open(Legacy legacy) {
        for (CaseFile c : legacy.cases()) if (!c.closed()) return c;
        return null;
    }

    public static @Nullable CaseFile find(Legacy legacy, int index) {
        for (CaseFile c : legacy.cases()) if (c.index() == index) return c;
        return null;
    }

    /** The index the hunter's next case gets. */
    public static int nextIndex(Legacy legacy) {
        int next = legacy.casesSolved();
        for (CaseFile c : legacy.cases()) next = Math.max(next, c.index() + 1);
        return next;
    }

    /** The case ring in blocks, from the config. */
    public static int[] ring() {
        int min = SNConfig.CASE_MIN_DISTANCE.get(), max = SNConfig.CASE_MAX_DISTANCE.get();
        return new int[]{Math.min(min, max), Math.max(min, max)};
    }

    /**
     * Rolls and hands out a new case, its site measured from {@code from}.
     *
     * @return the case, or null if the hunter is not a member or already has one open
     */
    public static @Nullable CaseFile issue(ServerPlayer player, BlockPos from) {
        Legacy legacy = Legacies.get(player);
        if (!legacy.member() || open(legacy) != null) return null;
        ServerLevel level = player.serverLevel();
        int index = nextIndex(legacy);
        int[] ring = ring();
        CaseGenerator.Plan plan = CaseGenerator.roll(level.getSeed(), player.getUUID().getMostSignificantBits(),
                player.getUUID().getLeastSignificantBits(), index, legacy.rank(), ring[0], ring[1]);
        BlockPos site = new BlockPos(from.getX() + plan.dx(), level.getSeaLevel(), from.getZ() + plan.dz());
        CaseFile file = new CaseFile(index, site, plan.scenario(), ResourceLocation.parse(plan.monster()), plan.twist(), plan.tier(),
                level.getGameTime(), CaseFile.OPEN);
        Legacies.update(player, l -> l.withCases(list -> trimmed(list, file)));
        give(player, fileItem(file));
        give(player, map(level, file));
        level.playSound(null, player.blockPosition(), AllSounds.LEGACY_PAPER.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case.new",
                Component.translatable("legacy.supernaturalcraft.case.scenario." + file.scenario())).withStyle(ChatFormatting.GOLD), false);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new LegacyFxPayload(LegacyFxPayload.CASE_NEW, player.getId(), index, ""));
        return file;
    }

    private static List<CaseFile> trimmed(List<CaseFile> list, CaseFile added) {
        List<CaseFile> out = new ArrayList<>(list);
        out.add(added);
        int closed = (int) out.stream().filter(CaseFile::closed).count();
        for (int i = 0; i < out.size() && closed > KEEP_CLOSED; i++) {
            if (out.get(i).closed()) {
                out.remove(i--);
                closed--;
            }
        }
        return out;
    }

    /** The case file item for {@code file}. */
    public static ItemStack fileItem(CaseFile file) {
        ItemStack stack = new ItemStack(AllItems.CASE_FILE.get());
        stack.set(AllDataComponents.CASE_INDEX.get(), file.index());
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.supernaturalcraft.case_file.named",
                Component.translatable("legacy.supernaturalcraft.case.scenario." + file.scenario())));
        return stack;
    }

    /** A map of the case's site, marked with its sign. */
    public static ItemStack map(ServerLevel level, CaseFile file) {
        BlockPos target = file.site();
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 1, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "case_" + file.index(), AllMapDecorations.CASE_SITE);
        map.set(DataComponents.ITEM_NAME, Component.translatable(MAP_NAME));
        map.set(DataComponents.MAP_COLOR, new MapItemColor(MAP_COLOR));
        return map;
    }

    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }
}
