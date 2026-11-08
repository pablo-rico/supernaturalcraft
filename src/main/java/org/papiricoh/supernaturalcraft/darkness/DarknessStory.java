package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity;
import org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity;
import org.papiricoh.supernaturalcraft.darkness.superior.Superior;
import org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.hell.cage.CageLayout;

import java.util.Optional;

/**
 * Season 11's road told (v0.42): the server side of each step of {@link DarknessRules}. {@link DarknessEvents} calls these at
 * dawn and as the hunter moves; commands and tests call them directly (the GameTest server has no Hell: {@link #visitCage}
 * takes the hunter as they are).
 */
public final class DarknessStory {

    /** Scene bits ({@link Darkness#scenes}). */
    public static final int GOT_COMPASS = 1, CASTIEL_SENT = 2;
    /** How near the Cage's altar the hunter must stand. */
    public static final double CAGE_REACH = 16;

    private DarknessStory() {
    }

    private static void say(ServerPlayer p, String key, ChatFormatting colour, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(colour, ChatFormatting.ITALIC));
    }

    /** After the release: Castiel's compass to Superior (once). */
    public static void giveCompass(ServerPlayer p) {
        if (Darknesses.get(p).seen(GOT_COMPASS)) return;
        Darknesses.update(p, d -> d.withSeen(GOT_COMPASS));
        BlockPos square = Superior.ensure(p.server.overworld());
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, square)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.supernaturalcraft.superior_compass").withStyle(ChatFormatting.DARK_PURPLE));
        if (!p.getInventory().add(compass)) p.drop(compass, false);
        say(p, "message.supernaturalcraft.darkness.superior_compass", ChatFormatting.GRAY, square.getX(), square.getZ());
    }

    /** The hunter walks into Superior. */
    public static boolean reachSuperior(ServerPlayer p) {
        if (!Darknesses.advance(p, DarknessRules.Step.SUPERIOR)) return false;
        say(p, "message.supernaturalcraft.darkness.superior", ChatFormatting.DARK_PURPLE);
        return true;
    }

    /** At dawn after Superior: Castiel says the only one who can face her is in the Cage. */
    public static void sendToCage(ServerPlayer p) {
        if (Darknesses.get(p).seen(CASTIEL_SENT)) return;
        Darknesses.update(p, d -> d.withSeen(CASTIEL_SENT));
        say(p, "message.supernaturalcraft.darkness.castiel_cage", ChatFormatting.AQUA);
    }

    /** Whether {@code p} stands at the Cage (in Hell, by its altar). */
    public static boolean atCage(ServerPlayer p) {
        if (p.level().dimension() != HellDimension.LEVEL) return false;
        Vec3 altar = Vec3.atCenterOf(CageLayout.ALTAR);
        return p.position().distanceTo(altar) <= CAGE_REACH;
    }

    /** The hunter stands before the Cage: Lucifer bargains, and Castiel says yes. Casifer walks out with them. */
    public static boolean visitCage(ServerPlayer p) {
        if (!Darknesses.advance(p, DarknessRules.Step.CAGE_VISITED)) return false;
        say(p, "message.supernaturalcraft.darkness.cage.0", ChatFormatting.RED);
        say(p, "message.supernaturalcraft.darkness.cage.1", ChatFormatting.AQUA);
        Darknesses.advance(p, DarknessRules.Step.CASIFER);
        say(p, "message.supernaturalcraft.darkness.cage.2", ChatFormatting.RED);
        CasiferEntity.arrive(p);
        return true;
    }

    /** At dawn with three Hands: Chuck comes, and shows who he is ("Don't Call Me Shurley"). */
    public static boolean reveal(ServerPlayer p) {
        if (!Darknesses.advance(p, DarknessRules.Step.REVEALED)) return false;
        ServerLevel level = p.serverLevel();
        ChuckAllyEntity chuck = DarknessRegistry.CHUCK_ALLY.get().create(level);
        if (chuck != null) {
            Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(4));
            chuck.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
            chuck.setRole(ChuckAllyEntity.ENVOY);
            level.addFreshEntity(chuck);
            DarknessEvents.leaveLater(chuck, 600);
        }
        for (int i = 0; i < 4; i++) say(p, "message.supernaturalcraft.darkness.chuck." + i, ChatFormatting.WHITE);
        return true;
    }

    /** At dawn after the reveal: Amara comes for Casifer, and takes him. */
    public static boolean take(ServerPlayer p) {
        if (!Darknesses.advance(p, DarknessRules.Step.TAKEN)) return false;
        CasiferEntity c = CasiferEntity.with(p);
        AmaraVisitorEntity.visit(p, 0, org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets.Age.WOMAN);
        if (c != null) {
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, c.getX(), c.getY() + 1, c.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
            c.discard();
        }
        say(p, "message.supernaturalcraft.darkness.taken.0", ChatFormatting.DARK_PURPLE);
        say(p, "message.supernaturalcraft.darkness.taken.1", ChatFormatting.GRAY);
        return true;
    }

    /** Amara's visit for the hunter's milestone, if one is due today. @return whether she came */
    public static boolean visit(ServerPlayer p, long day) {
        Darkness d = Darknesses.get(p);
        DarknessRules.Step step = d.stage();
        if (!DarknessRules.visitDue(step, d.handCount(), d.visited(), d.lastVisit(), day)) return false;
        int m = DarknessRules.milestone(step, d.handCount());
        Darknesses.update(p, x -> x.withVisit(m, day));
        return AmaraVisitorEntity.visit(p, m, DarknessRules.ageFor(step, d.handCount())) != null;
    }
}
