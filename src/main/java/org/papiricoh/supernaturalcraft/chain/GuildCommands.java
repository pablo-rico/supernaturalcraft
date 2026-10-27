package org.papiricoh.supernaturalcraft.chain;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.network.GuildActionPayload;

/**
 * Test commands for the guilds (v0.29), under {@code /supernatural chain guild}: found one where you stand, act for your guild
 * as the book would, hire the wild one nearest you, win an officer over, force a stance, open or end an interregnum, and
 * dissolve a guild.
 */
public final class GuildCommands {
    private GuildCommands() {
    }

    private static final String OK = "chain.supernaturalcraft.command.life";

    static LiteralArgumentBuilder<CommandSourceStack> attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var guild = Commands.literal("guild");
        guild.then(Commands.literal("list").executes(GuildCommands::list));
        guild.then(Commands.literal("found").then(Commands.argument("dye", IntegerArgumentType.integer(0, GuildRules.DYES - 1))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    // A test command founds where you stand, however near the centre (the rite's standard asks where).
                    String why = GuildLife.foundRefusal(p, StringArgumentType.getString(c, "name"), p.blockPosition());
                    if (why != null && !why.startsWith("guild_place")) return fail(c, why);
                    p.level().setBlock(p.blockPosition(), org.papiricoh.supernaturalcraft.registry.AllBlocks.GUILD_STANDARD.get().defaultBlockState(), 3);
                    ChainFaction slot = GuildLife.establish(p, StringArgumentType.getString(c, "name"), IntegerArgumentType.getInteger(c, "dye"), p.blockPosition());
                    if (slot == null) return fail(c, "guild_bad_name");
                    GuildStandardBlock.refresh(p.server, ChainSavedData.get(p.server).guild(slot));
                    return ok(c, "founded " + slot.id());
                }))));
        guild.then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                .executes(c -> act(c, GuildActionPayload.Action.INVITE, "", EntityArgument.getPlayer(c, "player").getGameProfile().getName(), 0))));
        guild.then(Commands.literal("leave").executes(c -> act(c, GuildActionPayload.Action.LEAVE, "", "", 0)));
        guild.then(Commands.literal("proclaim").executes(c -> act(c, GuildActionPayload.Action.PROCLAIM, "", "", 0)));
        guild.then(Commands.literal("emblem").then(Commands.argument("n", IntegerArgumentType.integer(0, GuildRules.EMBLEMS - 1))
                .executes(c -> act(c, GuildActionPayload.Action.EMBLEM, "", "", IntegerArgumentType.getInteger(c, "n")))));
        guild.then(Commands.literal("hire").executes(GuildCommands::hire));
        var accept = Commands.literal("accept");
        var dissolve = Commands.literal("dissolve");
        var interregnum = Commands.literal("interregnum");
        var stance = Commands.literal("stance");
        for (ChainFaction g : ChainFaction.GUILDS) {
            accept.then(Commands.literal(g.id()).executes(c -> act(c, GuildActionPayload.Action.ACCEPT_INVITE, g.id(), "", 0)));
            dissolve.then(Commands.literal(g.id()).executes(c -> {
                GuildLife.dissolve(c.getSource().getServer(), g, ChainEvents.day(c.getSource().getServer()));
                return ok(c, "dissolved " + g.id());
            }));
            interregnum.then(Commands.literal(g.id())
                    .then(Commands.literal("start").executes(c -> {
                        var data = ChainSavedData.get(c.getSource().getServer());
                        Officer lord = data.hierarchy(g).lord();
                        if (lord != null) data.hierarchy(g).unseat(lord);
                        GuildLife.interregnum(c.getSource().getServer(), g, ChainEvents.day(c.getSource().getServer()));
                        return ok(c, "interregnum in " + g.id());
                    }))
                    .then(Commands.literal("end").executes(c -> {
                        GuildLife.crown(c.getSource().getServer(), g, ChainEvents.day(c.getSource().getServer()));
                        return ok(c, "interregnum of " + g.id() + " ended");
                    })));
            for (ChainFaction other : ChainFaction.values()) {
                if (other == g) continue;
                var to = Commands.literal(g.id() + "_" + other.id());
                for (Diplomacy.Stance s : Diplomacy.Stance.values())
                    to.then(Commands.literal(s.name().toLowerCase(java.util.Locale.ROOT)).executes(c -> {
                        var d = ChainSavedData.get(c.getSource().getServer()).diplomacy();
                        if (s == Diplomacy.Stance.HOSTILE) d.declare(g, other);
                        else if (d.propose(g, other, s) == Diplomacy.Outcome.PROPOSED) d.accept(other, g);
                        ChainSavedData.get(c.getSource().getServer()).changed();
                        return ok(c, g.id() + " / " + other.id() + ": " + d.stance(g, other));
                    }));
                stance.then(to);
            }
        }
        var win = Commands.literal("desert");
        var bind = Commands.literal("bind");
        for (ChainFaction f : ChainFaction.values()) {
            win.then(Commands.literal(f.id()).then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Officer o = ChainSavedData.get(p.server).hierarchy(f).get(IntegerArgumentType.getInteger(c, "id"));
                return o != null && GuildLife.desert(p.server, o, p.getUUID()) != null ? ok(c, "deserted") : fail(c, "guild_full");
            })));
            bind.then(Commands.literal(f.id()).then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Officer o = ChainSavedData.get(p.server).hierarchy(f).get(IntegerArgumentType.getInteger(c, "id"));
                return o != null && GuildLife.bind(p.server, o, p.getUUID()) != null ? ok(c, "bound") : fail(c, "guild_full");
            })));
        }
        guild.then(accept).then(dissolve).then(interregnum).then(stance).then(win).then(bind);
        guild.then(Commands.literal("standard").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Officer post = GuildLife.post(ChainSavedData.get(p.server), p.getUUID());
            if (post == null) return fail(c, "guild_not_here");
            Guild g = ChainSavedData.get(p.server).guild(post.faction);
            var level = GuildLife.standardLevel(p.server, g);
            if (level == null) return fail(c, "guild_not_here");
            p.teleportTo(level, g.standardX + .5, g.standardY + 1, g.standardZ + 2.5, p.getYRot(), p.getXRot());
            return ok(c, "at the standard of " + g.name);
        }));
        return root.then(guild);
    }

    private static int act(CommandContext<CommandSourceStack> c, GuildActionPayload.Action action, String slot, String target, int arg)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        boolean done = GuildLife.act(p, action, slot, target, arg);
        ChainSavedData.get(p.server).changed();
        return done ? ok(c, action.name().toLowerCase(java.util.Locale.ROOT)) : fail(c, "guild_no_right");
    }

    private static int hire(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Mob mob = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8), m -> GuildLife.kind(m) != null).stream()
                .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p))).orElse(null);
        if (mob == null) return fail(c, "guild_not_here");
        Officer o = GuildLife.hire(p, mob);
        return o == null ? 0 : ok(c, "hired " + ChainNames.name(o));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        var data = ChainSavedData.get(c.getSource().getServer());
        for (Guild g : data.guilds()) {
            String line = g.slot.id() + " \"" + g.name + "\" gen " + g.generation + " founder " + g.founderName + " members "
                    + data.hierarchy(g.slot).alive().size() + (g.hasStandard ? " standard " + g.standardX + " " + g.standardY + " " + g.standardZ : " homeless")
                    + (data.court(g.slot).vacant() ? " interregnum until " + data.court(g.slot).vacantUntil : "");
            c.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return data.guilds().size();
    }

    private static int ok(CommandContext<CommandSourceStack> c, String what) {
        c.getSource().sendSuccess(() -> Component.translatable(OK, what), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String key) {
        c.getSource().sendFailure(Component.translatable("chain.supernaturalcraft.event." + key));
        return 0;
    }
}
