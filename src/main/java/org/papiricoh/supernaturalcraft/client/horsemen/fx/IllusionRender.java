package org.papiricoh.supernaturalcraft.client.horsemen.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * War's illusion, on the marked hunter's screen: every other hunter is drawn as a black-eyed demon (a client-side puppet
 * that follows them). The innocents and the friends all look the same: that is the trick.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class IllusionRender {

    private static final Map<UUID, BlackEyedDemon> PUPPETS = new java.util.HashMap<>();
    private static final Map<Player, Boolean> DRAWING = new WeakHashMap<>();

    private IllusionRender() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = event.getEntity();
        if (!ClientHorsemen.illusion() || player == mc.player || mc.level == null || DRAWING.containsKey(player)) {
            if (!ClientHorsemen.illusion() && !PUPPETS.isEmpty()) PUPPETS.clear();
            return;
        }
        BlackEyedDemon puppet = PUPPETS.computeIfAbsent(player.getUUID(), id -> AllEntities.BLACK_EYED_DEMON.get().create(mc.level));
        if (puppet == null) return;
        event.setCanceled(true);
        follow(puppet, player);
        PoseStack pose = event.getPoseStack();
        float partial = event.getPartialTick();
        var renderer = mc.getEntityRenderDispatcher().getRenderer(puppet);
        DRAWING.put(player, true);
        try {
            renderer.render(puppet, player.getViewYRot(partial), partial, pose, event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            DRAWING.remove(player);
        }
    }

    private static void follow(BlackEyedDemon puppet, Player player) {
        puppet.setPos(player.getX(), player.getY(), player.getZ());
        puppet.xo = player.xo;
        puppet.yo = player.yo;
        puppet.zo = player.zo;
        puppet.setYRot(player.getYRot());
        puppet.yRotO = player.yRotO;
        puppet.setXRot(player.getXRot());
        puppet.xRotO = player.xRotO;
        puppet.yBodyRot = player.yBodyRot;
        puppet.yBodyRotO = player.yBodyRotO;
        puppet.yHeadRot = player.yHeadRot;
        puppet.yHeadRotO = player.yHeadRotO;
        puppet.walkAnimation.setSpeed(player.walkAnimation.speed());
        puppet.tickCount = player.tickCount;
        puppet.setDeltaMovement(player.getDeltaMovement());
        if (player instanceof AbstractClientPlayer) puppet.setOnGround(player.onGround());
    }
}
