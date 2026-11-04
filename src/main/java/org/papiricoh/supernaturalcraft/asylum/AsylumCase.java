package org.papiricoh.supernaturalcraft.asylum;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleySavedData;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseFile;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseOffice;
import org.papiricoh.supernaturalcraft.legacy.cases.CaseSites;

/**
 * The Men of Letters' case that finds Crowley (v0.35): once a member has beaten Dick Roman, and while nobody in the world has
 * decided Crowley's fate, their next case is this one — a file and a map to the asylum ({@link AsylumWorld}, raised as the case
 * is handed out). It never materializes as an ordinary case site: the asylum itself is the case, and it closes, solved, when
 * Crowley's fate is decided (or lost, if it is given up).
 */
public final class AsylumCase {

    public static final String SCENARIO = "asylum";
    public static final ResourceLocation MONSTER = SupernaturalCraft.asResource("crowley");
    public static final int TIER = 4;

    private AsylumCase() {
    }

    /** Whether {@code player}'s next case is the asylum's. */
    public static boolean due(ServerPlayer player) {
        return Legacies.get(player).member() && AuthorWorld.done(player, BossProgression.Boss.DICK_ROMAN.advancement)
                && !CrowleySavedData.get(player.server).decided();
    }

    /** The case's site: the foot of the asylum's front stairs (the asylum is raised now if it is not yet). */
    public static BlockPos site(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        AsylumWorld.ensure(overworld);
        AsylumFight f = AsylumWorld.fight(level.getServer());
        BlockPos entry = f != null ? f.point(AsylumAnchors.ENTRY) : null;
        return entry != null ? entry : CrowleySavedData.get(level.getServer()).asylum();
    }

    public static boolean is(@Nullable CaseFile file) {
        return file != null && SCENARIO.equals(file.scenario());
    }

    /** {@code player}'s open asylum case, if they hold one. */
    public static @Nullable CaseFile open(ServerPlayer player) {
        CaseFile c = CaseOffice.open(Legacies.get(player));
        return is(c) ? c : null;
    }

    /** Crowley's fate is decided: the owner's case is solved. */
    public static void solve(ServerPlayer owner) {
        CaseFile c = open(owner);
        if (c != null) CaseSites.closeFile(owner, c.index(), CaseFile.SOLVED, false);
    }
}
