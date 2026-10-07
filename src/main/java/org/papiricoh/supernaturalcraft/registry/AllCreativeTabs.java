package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;

import java.util.Comparator;

public class AllCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SupernaturalCraft.MODID);

    /** Lists every registered item, so new content never needs a second edit here. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.supernaturalcraft"))
                    .icon(() -> AllItems.GRIMOIRE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        AllItems.ITEMS.getEntries().stream().map(DeferredHolder::get)
                                .filter(item -> item != AllItems.SIGIL_PAGE.get() && item != AllItems.SPELL_SCROLL.get())
                                .forEach(output::accept);
                        // One page per sigil the loaded datapacks define.
                        params.holders().lookup(SNRegistries.SIGIL).ifPresent(sigils -> sigils.listElementIds()
                                .sorted(Comparator.comparing(k -> k.location().toString()))
                                .forEach(k -> output.accept(SigilPageItem.of(AllItems.SIGIL_PAGE.get(), k.location()))));
                    })
                    .build());

    public static void init() {
    }
}
