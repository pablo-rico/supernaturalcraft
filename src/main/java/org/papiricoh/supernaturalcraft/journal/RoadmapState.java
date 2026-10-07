package org.papiricoh.supernaturalcraft.journal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** How far along the roadmap a hunter is (pure, tested in JUnit). */
public final class RoadmapState {

    public enum Status {
        /** Done: shown in full, with its seal. */
        DONE,
        /** Every parent done: its name and hint show, its picture is still a silhouette. */
        AVAILABLE,
        /** A silhouette and "???". */
        LOCKED
    }

    private RoadmapState() {
    }

    /** A node done out of order (a boss killed on someone else's road) still counts as done. */
    public static Map<String, Status> of(List<RoadmapNode> nodes, Progress progress) {
        Map<String, Boolean> done = new HashMap<>();
        for (RoadmapNode n : nodes) done.put(n.id(), n.done().test(progress));
        Map<String, Status> out = new HashMap<>();
        for (RoadmapNode n : nodes) {
            if (done.get(n.id())) out.put(n.id(), Status.DONE);
            else if (n.parents().stream().allMatch(p -> done.getOrDefault(p, false))) out.put(n.id(), Status.AVAILABLE);
            else out.put(n.id(), Status.LOCKED);
        }
        return out;
    }

    /** The next step on the main road: its first available node (in list order), or null at the end. */
    public static RoadmapNode next(List<RoadmapNode> nodes, Map<String, Status> state) {
        for (RoadmapNode n : nodes) if (n.main() && state.get(n.id()) == Status.AVAILABLE) return n;
        for (RoadmapNode n : nodes) if (state.get(n.id()) == Status.AVAILABLE) return n;
        return null;
    }
}
