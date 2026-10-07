package org.papiricoh.supernaturalcraft.util;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs short delayed tasks on the server thread (echoed spells, staged effects). Not persisted. */
public final class ServerScheduler {

    private record Task(long dueTick, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();
    private static long tick;

    private ServerScheduler() {
    }

    public static void schedule(int delayTicks, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new Task(tick + Math.max(1, delayTicks), action));
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        tick++;
        synchronized (PENDING) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.dueTick <= tick) {
                it.remove();
                task.action.run();
            }
        }
    }

    public static void clear(MinecraftServer server) {
        TASKS.clear();
        synchronized (PENDING) {
            PENDING.clear();
        }
    }
}
