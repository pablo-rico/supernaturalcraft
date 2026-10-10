package org.papiricoh.supernaturalcraft.heaven.roadhouse;

/**
 * The format of {@code AshMenuPayload.data} (v0.18): what Ash's menu ({@code client/heaven/AshMenuScreen}) shows. Written by
 * {@link Roadhouse#menu}; the choices go back as {@code AshChoicePayload} ({@code VISIT} with the visited hunter's UUID as
 * {@code arg}, {@code GO_HOME}, {@code TOGGLE_VISITORS}, {@code HINT} for other hints, {@code CLOSE}).
 * <pre>
 * {
 *   "greeting": "ash.supernaturalcraft.greet.first",          // translation key
 *   "hints": [ { "key": "ash.supernaturalcraft.hint.wing_sealed", "args": ["2"] }, ... ],   // 1-3 lines
 *   "visits": [ { "uuid": "...", "name": "Dean", "trusted": true, "online": false }, ... ],  // may be empty, at most 24
 *   "welcome": false,          // the hunter's own Heaven welcomes visitors (the toggle's state)
 *   "visits_enabled": true,    // the server allows visits at all (SNConfig heaven.visits); hide the list and toggle if false
 *   "has_plot": true           // the hunter has a Heaven of their own (GO_HOME takes them there)
 * }
 * </pre>
 * Hint arguments are literal text, except one starting with {@code @}: the rest is a translation key (a boss's name).
 */
public final class AshMenu {

    public static final String GREETING = "greeting";
    public static final String HINTS = "hints";
    public static final String HINT_KEY = "key";
    public static final String HINT_ARGS = "args";
    public static final String VISITS = "visits";
    public static final String VISIT_UUID = "uuid";
    public static final String VISIT_NAME = "name";
    public static final String VISIT_TRUSTED = "trusted";
    public static final String VISIT_ONLINE = "online";
    public static final String WELCOME = "welcome";
    public static final String VISITS_ENABLED = "visits_enabled";
    public static final String HAS_PLOT = "has_plot";
    /** Most Heavens listed. */
    public static final int MAX_VISITS = 24;
    /** How close to Ash a hunter must stand for their choices to count. */
    public static final double REACH = 8.0;

    private AshMenu() {
    }
}
