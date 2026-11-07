"""The Shadow, the Empty's keeper (v0.41, cosmic entity): rig, texture + glowmask, clips (names and timings parsed from
ShadowAssets) and its trophy bust (`block/shadow_trophy`). Its bar is in s1214_gui_art.

A tall figure of black tar (2.6 blocks: 44 px to the crown, long-limbed, gaunt) in the rough shape of a person in a long coat
that hangs to the floor and runs into strands of tar at the hem (`drip`). A faint violet-black sheen runs down it in slow wet
streaks. Its head is a smooth featureless lump; a face can form on it (`face`: a plane on its front -- hollow eye sockets and
a split of a mouth, lit a dim violet; the renderer shows it while the face forms).

Tendrils of tar (`tendril_1..6`, the renderer shows them by phase): four from its back (1, 2 high, 3, 4 low) and one from each
forearm (5 right, 6 left). Each is an unkeyed container holding a chain of three segments (`tendril_n_a` -- `_b` -- `_c`),
thick to thin, which the clips whip and curl. No clip keys face, drip or tendril_n.
"""

import math

import s1214_kit as K
from animkit import AnimFile
from apoc_vessel import Body, E_BACK, E_IN, E_IO, E_OUT, E_SNAP, arms, coat, legs, neck, skeleton, tone, torso
from chuck_art import Model, h01, none_on, solid
from michael_art import Clip, P_
from pixelkit import Ramp, fbm, hexc, mix, shade

NAME = "shadow"
A = "animation.shadow."
C = K.Contract("entity/boss/shadow/ShadowAssets")

B = Body(HIP=17.6, NECK=35.0, HEAD=9.0, TW=4.8, TD=2.5, AW=3.8, SY=34.0, ELBOW=27.0, WRIST=20.4, PALM=18.0, LX=2.3, LW=4.2, KNEE=9.2,
         SHOE_H=0.8, HEM=0.6)

TAR = Ramp("#020203", "#060509", "#0b0910", "#120f19", "#1b1624", "#282034")
SHEEN = Ramp("#2a1f3e", "#3e2c5c", "#56407a", "#705898", "#8c74b4", "#b4a0d8")
GLOW_HOT, GLOW, GLOW_DIM = hexc("#e6d4ff"), hexc("#9a6ee0"), hexc("#4a2a80")


def tar(seed, base=2.4, streak=True):
    """Wet black tar: smooth, slow vertical streaks of violet sheen, darker toward the bottom of each face, a few drips."""
    def m(f, x, y, w, h):
        n = fbm(x * 1.2 + len(f) * 9, y * 0.5, seed, 64, 64, 2, 7.0)
        v = base + (n - 0.5) * 1.4 - 0.7 * y / max(1, h) + (0.8 if f == "up" else 0)
        c = tone(TAR, v)
        if streak and f in ("north", "south", "east", "west") and h01("col", seed, f, x) < 0.16:
            run = fbm(x * 3.0, y * 0.35, seed + 5, 64, 64, 2, 5.0)
            if run > 0.55:
                c = mix(c, tone(SHEEN, 0.8 + 3 * (run - 0.55)), min(0.6, (run - 0.55) * 2.5))
        if f == "up" and h01("hl", seed, x, y) < 0.08:
            c = mix(c, SHEEN[2], 0.45)
        return c
    return m


def build(m):
    # The head: a smooth featureless lump of tar.
    hs = B.HEAD / 2
    m.cube("head", (-hs, B.NECK, -hs), (B.HEAD, B.HEAD, B.HEAD), tar(54001, 2.6), density=2, tag="skull")
    m.cube("head", (-hs + 1, B.crown - 0.2, -hs + 1), (B.HEAD - 2, 1.2, B.HEAD - 2), tar(54002, 2.8), density=2, tag="crown")
    neck(m, B, tar(54003, 2.2), 4.4)
    # The face that forms: hollow sockets and a split of a mouth, lit violet.
    fc = m.bone("face", (0, B.NECK + B.HEAD * 0.5, -hs), "head")
    rows = [
        "..................",
        "..................",
        "...oooo....oooo...",
        "..oEEEEo..oEEEEo..",
        "..oEHHEo..oEHHEo..",
        "..oEEEEo..oEEEEo..",
        "...oooo....oooo...",
        "..................",
        "..................",
        "....ooooooooooo...",
        "...oMMMMMMMMMMMo..",
        "....oMMHHHHMMMo...",
        ".....ooooooooo....",
    ]
    pal = {"o": hexc("#000000"), "E": GLOW, "H": GLOW_HOT, "M": GLOW_DIM}

    def face(f, x, y, w, h):
        if f != "north":
            return None
        ch = rows[y][x] if y < len(rows) and x < len(rows[y]) else "."
        return pal.get(ch)
    m.cube(fc, (-hs, B.NECK + 1.2, -hs - 0.06), (B.HEAD, 6.5, 0), face, glow=lambda f, x, y, w, h, c: None if c == pal["o"] else c,
           faces=("north",), density=2, tag="face")
    # The body and long coat of tar, to the floor.
    torso(m, B, tar(54010, 2.3), tag="torso")
    coat(m, B, TAR, tar(54011, 2.5), 54011, lapel=True, collar_up=True, pockets=False, open_front=True)
    arms(m, B, tar(54012, 2.4), tar(54013, 2.6, streak=False), sleeve_inflate=0.4)
    legs(m, B, tar(54014, 2.2), TAR, toe=0.6)
    # The dripping hem: strands of tar hanging from the coat's edge to the floor, puddling.
    dr = m.bone("drip", (0, B.HEM + 1.0, 0), "body")
    for i in range(14):
        a = i / 14 * 2 * math.pi
        x = math.sin(a) * (B.TW + 1.2)
        z = -math.cos(a) * (B.TD + 1.2)
        L = 1.2 + 2.4 * h01("drip", i)
        m.cube(dr, (x - 0.35, B.HEM - L + 0.6, z - 0.35), (0.7, L, 0.7), tar(54020 + i, 2.0, streak=False), density=4, tag="drip")
    m.cube(dr, (-B.TW - 1.6, 0, -B.TD - 1.6), (2 * B.TW + 3.2, 0.3, 2 * B.TD + 3.2), lambda f, x, y, w, h: None if f not in ("up",) or
           h01("pool", x, y) < 0.25 else tone(TAR, 2.0 + (1.6 if h01("pl", x, y) < 0.06 else 0)), density=1, tag="puddle")
    # Tendrils: four from the back, one from each forearm; each a container and a chain of three segments.
    spots = [
        ("tendril_1", "body", (-2.2, B.NECK - 3.0, B.TD), (115, 0, 35)),
        ("tendril_2", "body", (2.2, B.NECK - 3.0, B.TD), (115, 0, -35)),
        ("tendril_3", "body", (-2.6, B.HIP + 5.0, B.TD), (75, 0, 60)),
        ("tendril_4", "body", (2.6, B.HIP + 5.0, B.TD), (75, 0, -60)),
        ("tendril_5", "right_forearm", (-B.AX, B.WRIST + 2.0, 1.0), (16, 0, 10)),
        ("tendril_6", "left_forearm", (B.AX, B.WRIST + 2.0, 1.0), (16, 0, -10)),
    ]
    for n, (name, parent, (x, y, z), rest) in enumerate(spots):
        t = m.bone(name, (x, y, z), parent, rotation=rest)
        prev, py = t.name, y
        for k, (seg, L, w) in enumerate((("a", 7.0, 1.8), ("b", 6.0, 1.3), ("c", 5.0, 0.8))):
            sb = m.bone(f"{name}_{seg}", (x, py, z), prev, rotation=(14 + 8 * k, 0, 0))
            m.cube(sb, (x - w / 2, py - L, z - w / 2), (w, L + 0.4, w), tar(54100 + n * 7 + k, 2.4 - 0.2 * k), density=2, tag="tendril")
            prev, py = sb.name, py - L
        m.cube(prev, (x - 0.25, py - 1.2, z - 0.25), (0.5, 1.2, 0.5), solid(TAR[3]), density=4, tag="tendril_tip")


def rig():
    m = Model(NAME, 256, 256)
    skeleton(m, B, coat=True)
    build(m)
    return m


# =====================================================================================================
# Clips
# =====================================================================================================

LOOSE = P_(K.relax("right"), K.relax("left"), {"right_arm": [-4, 0, 6], "left_arm": [-4, 0, -6], "right_forearm": [-10, 0, 0],
                                               "left_forearm": [-10, 0, 0], "head": [6, 0, 0], "body": [6, 0, 0]})
SEGS = [f"tendril_{i}_{s}" for i in range(1, 7) for s in "abc"]


def writhe(u, amp=1.0, phase=0.0):
    """The tendrils' slow writhing (u in 0..1 over the clip)."""
    p = {}
    for i in range(1, 7):
        for k, s in enumerate("abc"):
            a = u * 2 * math.pi + i * 1.3 + k * 0.9 + phase
            p[f"tendril_{i}_{s}"] = [amp * (8 + 4 * k) * math.sin(a), 0, amp * (6 + 4 * k) * math.cos(a * 0.7)]
    return p


def ripple(u, amp=1.0):
    return {"coat_tail": [3 * amp * math.sin(u * 6.28), 0, 1.5 * amp * math.sin(u * 3.14)], "coat_r": [-2 * amp * math.sin(u * 6.28 + 0.6), 0, 0],
            "coat_l": [-2 * amp * math.sin(u * 6.28 + 1.2), 0, 0]}


def anims():
    f = AnimFile()
    clips = []

    def clip(name, length=None, loop=False, hold=False):
        clips.append(Clip(f, A + name, length if length is not None else C.length(name), loop, hold, forbid=C.optional))
        return clips[-1]

    h, L = C.hit, C.length
    # --- loops ---------------------------------------------------------------------------------------------------
    c = clip("idle", 4.0, loop=True)
    for i in range(9):
        u = i / 8
        b = math.sin(u * 2 * math.pi)
        c.key(4.0 * u, P_(LOOSE, writhe(u), ripple(u, 0.6), {"body": [6 + 2 * b, 3 * math.sin(u * 6.28 + 1), 2 * b],
                                                             "head": [6 - 3 * b, 6 * math.sin(u * 6.28 + 2), -3 * b],
                                                             "%body": [1 + 0.02 * b, 1 - 0.02 * b, 1 + 0.02 * b]}), E_IO if i else None)
    c = clip("walk", 2.0, loop=True)
    for i in range(5):
        u = i / 4
        s = 1 if i % 2 == 0 else -1
        c.key(2.0 * u, P_(LOOSE, writhe(u, 1.2), ripple(u, 1.4), {"right_leg": [10 * s, 0, 0], "left_leg": [-10 * s, 0, 0],
                                                                  "right_shin": [6, 0, 0], "left_shin": [6, 0, 0], "body": [12, 4 * s, 0],
                                                                  "@root": [0, 0.3 * (i % 2), 0], "coat_tail": [14, 0, 2 * s]}),
              E_IO if i else None)

    # --- once ----------------------------------------------------------------------------------------------------
    # Lash (8): tendril 1 drawn back, then whipped forward over its shoulder.
    c = clip("lash")
    back = P_(LOOSE, writhe(0.1), {"tendril_1_a": [70, 0, 10], "tendril_1_b": [40, 0, 0], "tendril_1_c": [30, 0, 0], "body": [-6, -14, 0],
                                   "head": [0, 10, 0], "right_arm": [-30, 0, 30]})
    whip = P_(LOOSE, writhe(0.2), {"tendril_1_a": [-150, 0, -10], "tendril_1_b": [-30, 0, 0], "tendril_1_c": [-20, 0, 0], "body": [18, 14, 0],
                                   "head": [10, -10, 0], "right_arm": [-60, 0, 10], "@root": [0, 0, -1.0]})
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.28, back, E_IO)
    c.key(h("lash"), whip, E_SNAP)
    c.key(0.55, P_(whip, {"tendril_1_c": [10, 0, 0]}), E_OUT)
    c.key(L("lash"), P_(LOOSE, writhe(0)), E_IO)

    # Engulf (18): both arms open wide, the tar surging -- every tendril flung forward.
    c = clip("engulf")
    gather = P_(LOOSE, {"body": [20, 0, 0], "head": [20, 0, 0], "right_arm": [-30, 0, -10], "left_arm": [-30, 0, 10],
                        "%body": [0.94, 1.04, 0.94]}, {k: [40, 0, 0] for k in SEGS})
    surge = P_(K.spread("right"), K.spread("left"), {"body": [-10, 0, 0], "head": [-20, 0, 0], "right_arm": [-40, 0, 90], "left_arm": [-40, 0, -90],
                                                     "%body": [1.15, 1.05, 1.15], "@root": [0, 0.6, -1.0]},
               {f"tendril_{i}_a": [-110, 0, 0] for i in range(1, 7)}, {f"tendril_{i}_b": [-20, 0, 0] for i in range(1, 7)},
               ripple(0.25, 3))
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.65, gather, E_IO)
    c.key(h("engulf"), surge, E_SNAP)
    c.key(1.2, P_(surge, writhe(0.3, 0.6)), E_OUT)
    c.key(L("engulf"), P_(LOOSE, writhe(0)), E_IO)

    # Sink (10): it melts into the floor.
    c = clip("sink", hold=True)
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.2, P_(LOOSE, writhe(0.1), {"%root": [1.1, 0.9, 1.1]}), E_OUT)
    c.key(h("sink"), P_(LOOSE, writhe(0.2), {"%root": [1.4, 0.45, 1.4], "@root": [0, -8, 0]}), E_IN)
    c.key(L("sink"), P_(LOOSE, {"%root": [1.8, 0.05, 1.8], "@root": [0, -2, 0]}), E_IN)

    # Rise (10): out of the floor.
    c = clip("rise")
    c.key(0, P_(LOOSE, {"%root": [1.8, 0.05, 1.8], "@root": [0, -2, 0]}))
    c.key(h("rise"), P_(LOOSE, writhe(0.2), {"%root": [1.3, 0.6, 1.3], "@root": [0, -4, 0]}), E_OUT)
    c.key(0.9, P_(LOOSE, writhe(0.3), {"%root": [0.95, 1.08, 0.95]}), E_OUT)
    c.key(L("rise"), P_(LOOSE, writhe(0)), E_IO)

    # Lull (20): a slow sweep of a hand -- sleep falls.
    c = clip("lull")
    start = P_(LOOSE, writhe(0), K.spread("right"), {"right_arm": [-80, 0, -30], "right_forearm": [-30, 0, 0], "right_hand": [0, 0, -30],
                                                     "body": [10, 20, 0], "head": [10, 10, 0]})
    end = P_(LOOSE, writhe(0.5), K.spread("right"), {"right_arm": [-80, 0, 60], "right_forearm": [-10, 0, 0], "right_hand": [0, 0, 20],
                                                    "body": [10, -20, 0], "head": [14, -10, 0]})
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.5, start, E_IO)
    c.key(h("lull"), P_(start, end, {"right_arm": [-84, 0, 14], "body": [12, 0, 0]}), E_IO)
    c.key(1.5, end, E_IO)
    c.key(L("lull"), P_(LOOSE, writhe(0)), E_IO)

    # Grab (10): a forearm tendril seizes and drags.
    c = clip("grab")
    reach = P_(LOOSE, writhe(0.1), {"right_arm": [-90, 0, 0], "right_forearm": [-6, 0, 0], "tendril_5_a": [-80, 0, -10], "tendril_5_b": [-10, 0, 0],
                                    "tendril_5_c": [0, 0, 0], "body": [14, 0, 0], "@root": [0, 0, -1.4]})
    drag = P_(LOOSE, writhe(0.2), K.fist("right"), {"right_arm": [-40, 0, 10], "right_forearm": [-60, 0, 0], "tendril_5_a": [-30, 0, 0],
                                                   "tendril_5_b": [60, 0, 0], "tendril_5_c": [70, 0, 0], "body": [-4, 10, 0], "@root": [0, 0, 0.6]})
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.3, P_(reach, {"right_arm": [-70, 0, 10]}), E_IO)
    c.key(h("grab"), reach, E_SNAP)
    c.key(0.8, drag, E_IO)
    c.key(L("grab"), P_(LOOSE, writhe(0)), E_IO)

    # Roar (14): the face splits open, head thrown back, a soundless scream; the tendrils flare.
    c = clip("roar")
    gather = P_(LOOSE, writhe(0.1), {"body": [26, 0, 0], "head": [24, 0, 0], "%body": [0.95, 0.96, 0.95]})
    roar = P_(K.spread("right"), K.spread("left"), {"body": [-14, 0, 0], "head": [-40, 0, 0], "right_arm": [10, 0, 50], "left_arm": [10, 0, -50],
                                                    "%head": [1.1, 1.25, 1.1], "%body": [1.08, 1.04, 1.08]},
              {f"tendril_{i}_a": [-60, 0, 40 * (1 if i % 2 else -1)] for i in range(1, 7)}, ripple(0.25, 2.5))
    c.key(0, P_(LOOSE, writhe(0)))
    c.key(0.5, gather, E_IO)
    c.key(h("roar"), roar, E_SNAP)
    for i in range(1, 5):
        j = K.jit("shadow-roar", i, 3)
        c.key(h("roar") + 0.15 * i, P_(roar, {"head": [-40 + j[0], j[1], j[2]], "body": [-14 + j[2], j[0], 0]}))
    c.key(L("roar"), P_(LOOSE, writhe(0)), E_IO)

    # Hurt (2): a ripple through the tar.
    c = clip("hurt")
    K.recoil(c, P_(LOOSE, writhe(0)), L("hurt"), h("hurt"), P_(ripple(0.25, 2), {"body": [-10, 8, 0], "head": [-16, 10, 0],
                                                                                "%body": [1.08, 0.92, 1.08], "@root": [0, 0, 0.8]}))

    # Death (50): it boils -- shudders and swells -- and collapses into a pool.
    c = clip("death", hold=True)
    c.key(0, P_(LOOSE, writhe(0)))
    for i in range(1, 11):
        t = 0.25 * i
        j = K.jit("shadow-death", i, 4 + i)
        k = i / 10
        c.key(t, P_(LOOSE, writhe(k * 2, 1 + k), {"body": [6 + j[0], j[1], j[2]], "head": [6 + j[2], j[0] * 2, j[1]],
                                                  "%body": [1 + 0.12 * k * (1 + math.sin(i * 2.1)), 1 + 0.06 * math.sin(i * 1.7), 1 + 0.12 * k],
                                                  "%head": [1 + 0.1 * k, 1 - 0.1 * k, 1 + 0.1 * k]}))
    c.key(h("death"), P_(LOOSE, writhe(1.2, 2.0), K.spread("right"), K.spread("left"),
                         {"body": [-12, 0, 0], "head": [-30, 0, 0], "right_arm": [-20, 0, 70], "left_arm": [-20, 0, -70],
                          "%body": [1.2, 1.1, 1.2], "%head": [1.15, 1.2, 1.15]}), E_BACK)
    c.key(3.4, P_(LOOSE, {"%root": [1.6, 0.4, 1.6], "@root": [0, -3, 0]}), E_IN)
    c.key(L("death"), P_(LOOSE, {"%root": [2.2, 0.06, 2.2], "@root": [0, -1, 0]}), E_IN)

    for cl in clips:
        cl.done(step=0.1)
    return f, clips


def bust():
    import blockmodel as BM
    skin = lambda x, y: tone(TAR, 2.4 + 0.6 * math.sin(x * 0.7 + y * 0.3) - 0.3 * y / 15)
    sheen = lambda x, y: mix(skin(x, y), SHEEN[3], 0.45) if math.sin(x * 1.3 + 2.0 * math.sin(y * 0.3)) > 0.86 else skin(x, y)
    rows = ["................", "................", "................", "................", "................",
            "..oooo....oooo..", ".oEHEo....oEHEo.", "..oooo....oooo..", "................", "................",
            "................", "...oooooooooo...", "..oMMMHHMMMMo...", "...oooooooooo...", "................", "................"]

    def face(x, y):
        ch = rows[y][x]
        return {"o": hexc("#000000"), "E": GLOW, "H": GLOW_HOT, "M": GLOW_DIM}.get(ch) or sheen(x, y)
    K.bust("shadow", face, sheen, sheen, sheen, sheen, hair_cap=False,
           extra=[BM.cube("drip_l", (10.0, 4.2, 6.0), (10.6, 6.4, 6.6), {"*": "skin"}),
                  BM.cube("drip_r", (5.6, 3.6, 6.4), (6.2, 6.4, 7.0), {"*": "skin"})],
           shoulders_w=8.2, head_w=4.6)


def build_all():
    m = rig()
    t, g = m.build(gutter=1, seed=54000)
    f, clips = anims()
    K.verify_contract(m, f, C)
    return m, t, g, f, clips


def generate():
    m, t, g, f, _ = build_all()
    K.write_entity(m, f, NAME, t, g)
    bust()


def preview(out_dir, poses=(("idle", 0.0), ("lash", 0.4), ("engulf", 0.9), ("roar", 0.7), ("death", 3.4)), scale=4):
    m, t, g, f, clips = build_all()
    return K.render(m, t, clips, out_dir, NAME, poses, scale=scale, views=("front", "three_quarter", "side", "back"))


if __name__ == "__main__":
    generate()
