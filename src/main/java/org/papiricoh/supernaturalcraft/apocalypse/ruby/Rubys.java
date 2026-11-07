package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.apocalypse.warehouse.WarehouseWorld;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.Optional;
import java.util.function.UnaryOperator;

/** Ruby on the server (v0.40): where a hunter stands with her, her gifts, and what her talks' answers do. */
public final class Rubys {

    private Rubys() {
    }

    public static RubyData get(ServerPlayer p) {
        return p.getData(AllAttachments.RUBY);
    }

    public static RubyRules.Step step(ServerPlayer p) {
        return RubyRules.Step.of(get(p).step());
    }

    public static void update(ServerPlayer p, UnaryOperator<RubyData> f) {
        p.setData(AllAttachments.RUBY, f.apply(get(p)));
    }

    /** Moves {@code p} on to {@code s} (never back). */
    public static void advance(ServerPlayer p, RubyRules.Step s) {
        if (step(p).ordinal() < s.ordinal()) update(p, d -> d.withStep(s.ordinal()));
    }

    public static long today(ServerPlayer p) {
        return WorldClock.day(p.server) / 24000L;
    }

    /** The side words, for her opening line. */
    public static String side(ServerPlayer p) {
        Faction f = Allegiances.get(p).faction();
        return f == Faction.ANGEL ? "angel" : f == Faction.DEMON ? "demon" : "human";
    }

    /** At mod construction: her talk comes back through the shared dialogue. */
    public static void registerDialogue() {
        AllegianceDialogue.register(RubyRules.DIALOGUE, (player, speaker, node, option) -> {
            if (speaker instanceof RubyNpcEntity r && player.getUUID().equals(r.hunter()) && !r.leaving()) answer(player, r, node, option);
        });
    }

    /** Her answer handler (public for tests: fake players answer by hand). */
    public static void answer(ServerPlayer p, RubyNpcEntity ruby, String node, String option) {
        RubyRules.Turn t = RubyRules.answer(node, option);
        update(p, d -> d.withVisit(today(p)));
        switch (t.act()) {
            case LEAVE -> {
                ruby.triggerAnim("action", "vanish");
                ruby.leave(20);
                return;
            }
            case TAKE_FLASK, NO_FLASK -> {
                advance(p, RubyRules.Step.ALASTAIR);
                if (t.act() == RubyRules.Act.TAKE_FLASK) give(p, new ItemStack(AllItems.RUBYS_FLASK.get()));
                giveCompass(p);
                ruby.triggerAnim("action", "give");
                ruby.leave(30);
                return;
            }
            case LILITHS_WAY -> {
                advance(p, RubyRules.Step.LILITHS_WAY);
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.ruby.liliths_way").withStyle(ChatFormatting.DARK_RED));
            }
            default -> {
                if (node.startsWith("meet")) {
                    advance(p, RubyRules.Step.MET);
                    ChorusRewards.award(p, "main/i_know_what_you_did");
                }
            }
        }
        if (t.next() != null) ruby.say(p, new org.papiricoh.supernaturalcraft.pact.PactDialogue.Line(t.next().node(), t.next().options()));
        else ruby.leave(16);
    }

    /** A lodestone compass at the warehouse where the angels hold Alastair (it is raised if it is not yet). */
    public static void giveCompass(ServerPlayer p) {
        WarehouseWorld.ensure(p.server.overworld());
        @Nullable BlockPos at = WarehouseWorld.trap(p.server);
        if (at == null) return;
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, at)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.supernaturalcraft.rubys_compass").withStyle(ChatFormatting.DARK_RED));
        give(p, compass);
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.ruby.warehouse", at.getX(), at.getZ()).withStyle(ChatFormatting.GOLD));
    }

    static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
}
