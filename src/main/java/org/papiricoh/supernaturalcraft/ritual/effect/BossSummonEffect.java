package org.papiricoh.supernaturalcraft.ritual.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * A ritual that opens a fight (v0.19): refused before it starts while another fight holds the dimension. Just before it is
 * performed the altar calls {@link #openRift} (v0.21): a summoning rift over the altar.
 */
public interface BossSummonEffect extends RitualEffect {

    /** How tall and wide the summoning rift is (blocks), and how long it stays open (ticks). */
    float RIFT_HEIGHT = 5f, RIFT_WIDTH = 2.5f;
    int RIFT_TICKS = 60;

    @Override
    default @Nullable Component precheck(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        return LuciferSummoning.refusal(level);
    }

    /** SUMMON_RIFT over {@code altar}, in the rite's colours. Override for a rift of another size or place. */
    default void openRift(ServerLevel level, BlockPos altar, School school) {
        Vec3 base = Vec3.atBottomCenterOf(altar).add(0, 1, 0);
        MagicFx.rift(level, school, base, base.add(0, 0, -1), null, RIFT_HEIGHT, RIFT_WIDTH, RIFT_TICKS);
    }
}
