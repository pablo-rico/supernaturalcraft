package org.papiricoh.supernaturalcraft.entity.boss.uriel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.seal.CastielEnvoyEntity;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Uriel's end (v0.40): every hunter who fought him gets his bust ({@code uriel_trophy}), {@link #FEATHERS} Scorched Feathers off
 * his burnt wings and the page of {@link #SIGIL}; then Castiel comes to the one who struck last (or the nearest) to say his word.
 * The boss's credit (shards, hearts, lore) comes with his kill advancement, as for every boss.
 */
public final class UrielSpoils {

    public static final int FEATHERS = 4;
    /** The sigil his page teaches: the radiance he blinded them with. */
    public static final ResourceLocation SIGIL = SupernaturalCraft.asResource("radiance");

    private UrielSpoils() {
    }

    /** What every hunter's share is. */
    public static List<ItemStack> share() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.URIEL_TROPHY.get()));
        out.add(new ItemStack(AllItems.SCORCHED_FEATHER.get(), FEATHERS));
        out.add(SigilPageItem.of(AllItems.SIGIL_PAGE.get(), SIGIL));
        return out;
    }

    public static void defeated(ServerLevel level, UrielEntity boss, List<ServerPlayer> hunters) {
        Vec3 at = boss.position().add(0, 1, 0);
        if (hunters.isEmpty()) {
            for (ItemStack s : share()) {
                ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, s);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
            return;
        }
        for (ServerPlayer p : hunters) Stakes.give(level.getServer(), p.getUUID(), share(), false);
        ServerPlayer witness = boss.getLastHurtByMob() instanceof ServerPlayer p && p.isAlive() ? p : hunters.getFirst();
        CastielEnvoyEntity.after(witness);
    }
}
