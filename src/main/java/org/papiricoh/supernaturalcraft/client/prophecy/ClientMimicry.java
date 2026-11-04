package org.papiricoh.supernaturalcraft.client.prophecy;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.render.DisguiseMask;
import org.papiricoh.supernaturalcraft.network.MimicryPayload;

import java.util.HashSet;
import java.util.Set;

/** Players wearing a borrowed face (v0.36, Mimicry) are drawn as a villager, in third person and to everyone else. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientMimicry {

    public static final String DISGUISE = "villager:farmer";
    private static final Set<Integer> MASKED = new HashSet<>();

    private ClientMimicry() {
    }

    public static void handle(MimicryPayload p) {
        if (p.active()) MASKED.add(p.entity());
        else MASKED.remove(p.entity());
    }

    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        if (!MASKED.contains(player.getId())) return;
        if (Minecraft.getInstance().level == null || player.isRemoved()) {
            MASKED.remove(player.getId());
            return;
        }
        if (DisguiseMask.render(player, DISGUISE, player.getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight())) {
            event.setCanceled(true);
        }
    }
}
