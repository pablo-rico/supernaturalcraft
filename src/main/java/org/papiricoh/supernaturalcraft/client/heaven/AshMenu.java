package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * What Ash's menu shows (v0.18), read from {@code AshMenuPayload.data} in the format of
 * {@link org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu} (greeting, hints with arguments, the Heavens to visit, the
 * visitors toggle, whether visits are allowed, whether the hunter has a plot). Missing keys read as empty / false.
 *
 * @param lines         the greeting, then each hint, as text
 * @param heavens       where Ash can send them
 * @param welcome       whether their own Heaven welcomes visitors
 * @param visitsEnabled whether the server allows visits at all
 * @param home          whether "take me home" is offered (they have a plot)
 */
public record AshMenu(List<Component> lines, List<Heaven> heavens, boolean welcome, boolean visitsEnabled, boolean home) {

    /** A Heaven Ash can open the way to. */
    public record Heaven(String name, String uuid, boolean online) {
    }

    public static AshMenu read(CompoundTag tag) {
        List<Component> lines = new ArrayList<>();
        if (tag.contains(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.GREETING, Tag.TAG_STRING)) {
            lines.add(Component.translatable(tag.getString(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.GREETING)));
        }
        ListTag hints = tag.getList(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.HINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < hints.size(); i++) {
            CompoundTag h = hints.getCompound(i);
            ListTag args = h.getList(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.HINT_ARGS, Tag.TAG_STRING);
            Object[] a = new Object[args.size()];
            for (int k = 0; k < a.length; k++) {
                String s = args.getString(k);
                a[k] = s.startsWith("@") ? Component.translatable(s.substring(1)) : s;
            }
            lines.add(Component.translatable(h.getString(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.HINT_KEY), a));
        }
        List<Heaven> heavens = new ArrayList<>();
        ListTag visits = tag.getList(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.VISITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < visits.size(); i++) {
            CompoundTag v = visits.getCompound(i);
            String key = org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.VISIT_UUID;
            String uuid = v.contains(key, Tag.TAG_INT_ARRAY) ? v.getUUID(key).toString() : v.getString(key);
            if (!uuid.isEmpty()) {
                heavens.add(new Heaven(v.getString(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.VISIT_NAME), uuid,
                        v.getBoolean(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.VISIT_ONLINE)));
            }
        }
        String enabled = org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.VISITS_ENABLED;
        return new AshMenu(List.copyOf(lines), List.copyOf(heavens),
                tag.getBoolean(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.WELCOME),
                !tag.contains(enabled) || tag.getBoolean(enabled),
                tag.getBoolean(org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu.HAS_PLOT));
    }

    public AshMenu withWelcome(boolean value) {
        return new AshMenu(lines, heavens, value, visitsEnabled, home);
    }
}
