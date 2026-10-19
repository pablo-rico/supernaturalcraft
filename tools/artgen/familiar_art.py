"""A witch's familiar (v0.21), two rigs by variant (names in entity/witch/WitchAssets): `familiar_cat` and
`familiar_raven` -- GeckoLib geometry, texture, glowmask (the eyes) and clips.

  familiar_cat    a sleek black cat, long-legged and narrow-headed, amber eyes that glow, a fine silver collar with a
                  little charm (a moon in a ring); the tail held high with a crook at the tip.
  familiar_raven  a big black raven with a heavy beak and a shaggy throat, the black sheened blue-violet; magenta glints
                  in its eyes (glowmask).

Skeletons (Bedrock px; +X is the animal's LEFT; it faces north):
  cat:   root -- body -- head -- ear_r, ear_l, mouth -- held_item;  collar;  tail -- tail_tip;
                      leg_fr, leg_fl, leg_br, leg_bl
  raven: root -- body -- head -- beak -- held_item;  wings -- wing_r -- wing_r_tip, wing_l -- wing_l_tip;
                      tail;  leg_r, leg_l
`held_item` is empty: the renderer draws the carried item there (no clip keys it). `wings` is only a group: no clip keys
it either, so the renderer may fold (or hide) the pair as a whole; the clips spread and fold `wing_r`/`wing_l`.
"""

import math

from animkit import AnimFile
from chuck_art import Model, h01, none_on, solid
from common import save
from michael_art import ANIM, E_IN, E_IO, E_OUT, E_SNAP, GEO, Clip, P_, tone, write_compact
from pixelkit import Ramp, hexc, mix, shade

FUR = Ramp("#050407", "#0b090e", "#131017", "#1c1822", "#28232f", "#38323f")
AMBER = Ramp("#4a2a02", "#7a4604", "#b06e08", "#e09a14", "#ffc23a", "#fff0a0")
SILVER = Ramp("#2e333b", "#4f5661", "#7a828e", "#a6adb8", "#cfd5dc", "#f4f7fa")
NOSE = hexc("#3a2a30")
FEATHER = Ramp("#040406", "#0a0a10", "#12121c", "#1b1b2a", "#262a3c", "#353c55")
SHEEN = hexc("#4b3d78")
BEAK = Ramp("#050505", "#0d0d0f", "#17171a", "#232327", "#323238", "#46464e")
MAGENTA = Ramp("#3d0630", "#6e0c55", "#a3187e", "#d22aa6", "#f05bc8", "#ffb0ea")


def fur_m(seed, base=2.6, belly=None):
    """Short black fur: fine strands, a blue-grey sheen along the top."""
    def m(f, x, y, w, h):
        n = h01("fur", seed, f, x, y)
        v = base + (n - 0.5) * 0.8
        if f == "up":
            v += 0.9
        elif f == "down":
            v -= 0.8
        elif y == 0:
            v += 0.5
        return tone(FUR, v)
    return m


def eye_glow(f, x, y, w, h, c):
    return c if c[0] > 150 or c in (AMBER[3], AMBER[4], AMBER[5]) else None


# =====================================================================================================
# The cat
# =====================================================================================================

def cat_rig():
    m = Model("familiar_cat", 128, 128)
    m.bone("root", (0, 0, 0))
    body = m.bone("body", (0, 6.0, 0), "root")
    m.bone("head", (0, 7.6, -5.0), "body")
    m.bone("ear_r", (-1.2, 10.4, -6.4), "head")
    m.bone("ear_l", (1.2, 10.4, -6.4), "head")
    m.bone("mouth", (0, 7.1, -7.6), "head")
    m.bone("held_item", (0, 6.6, -9.4), "mouth")
    m.bone("collar", (0, 7.6, -5.4), "head")
    m.bone("tail", (0, 7.8, 5.0), "body", rotation=(30, 0, 0))
    m.bone("tail_tip", (0, 13.9, 5.0), "tail", rotation=(-40, 0, 0))
    for name, x, z in (("leg_fr", -1.1, -3.6), ("leg_fl", 1.1, -3.6), ("leg_br", -1.1, 3.6), ("leg_bl", 1.1, 3.6)):
        m.bone(name, (x, 5.6, z), "body")
    fur = fur_m(51001)
    # A long narrow body, the chest deeper than the haunches; a slight dip at the shoulders.
    m.cube("body", (-1.6, 4.6, -4.8), (3.2, 3.4, 5.4), fur, density=2, tag="chest")
    m.cube("body", (-1.5, 4.9, 0.4), (3.0, 3.0, 4.8), fur, density=2, tag="haunch")
    m.cube("body", (-1.7, 5.2, 2.6), (3.4, 2.8, 2.4), fur, density=2, tag="hip")
    # The head: a wedge (a skull block, a narrower muzzle), big ears, amber eyes.
    def face(f, x, y, w, h):
        c = fur(f, x, y, w, h)
        if f == "north":
            # 8 x 6 texels: two almond eyes on rows 2-3 (amber, a black slit in the middle), dark lids above.
            for ex in (0, 5):
                if ex <= x <= ex + 2 and y in (2, 3):
                    if x == ex + 1:
                        return hexc("#0a0604")
                    return AMBER[4] if y == 2 else AMBER[3]
                if ex <= x <= ex + 2 and y == 1:
                    return FUR[0]
        return c
    m.cube("head", (-2.0, 6.6, -8.0), (4.0, 3.2, 3.2), face, eye_glow, density=2, tag="skull")
    # Slit pupils need a texel of amber either side: widen the eye with the side rows.
    def muzzle(f, x, y, w, h):
        if f == "north":
            if y == 0 and x in (1, 2):
                return NOSE
            if y == 2 and x in (1, 2):
                return FUR[0]
            return fur(f, x, y, w, h)
        return fur(f, x, y, w, h)
    m.cube("head", (-1.0, 6.7, -9.0), (2.0, 1.4, 1.2), muzzle, density=2, tag="muzzle")
    # Whiskers (thin white planes at the muzzle's sides).
    for s in (-1, 1):
        m.cube("head", (s * 1.0 - (1.4 if s < 0 else 0), 7.4, -8.6), (1.4, 0.0, 0.6), lambda f, x, y, w, h: (220, 220, 225, 200) if x % 2 == 0 else None,
               faces=("up", "down"), density=4, tag="whiskers", rotation=(0, 0, -10 * s), pivot=(s * 1.0, 7.4, -8.4))
    # The lower jaw: opens to steal, and the stolen thing hangs from it (held_item).
    m.cube("mouth", (-0.8, 6.4, -8.8), (1.6, 0.6, 1.6), lambda f, x, y, w, h: hexc("#6a2a36") if f == "up" else fur(f, x, y, w, h), density=2,
           tag="jaw")
    for side, s in (("r", -1), ("l", 1)):
        def ear(f, x, y, w, h):
            if f == "north":
                return hexc("#4a2c34") if 0 < x < w - 1 and y > 0 else FUR[3]
            return fur(f, x, y, w, h)
        m.cube(f"ear_{side}", (s * 1.2 - 0.8, 9.6, -6.8), (1.6, 1.8, 0.6), ear, density=4, tag="ear", rotation=(0, 0, -12 * s),
               pivot=(s * 1.2, 9.6, -6.5))
        m.cube(f"ear_{side}", (s * 1.2 - 0.45, 11.2, -6.7), (0.9, 0.8, 0.4), fur, density=4, tag="ear_tip", rotation=(0, 0, -12 * s),
               pivot=(s * 1.2, 9.6, -6.5))
    # The collar: a fine silver chain, the charm (a crescent in a ring) under the chin.
    m.cube("collar", (-1.7, 6.2, -6.1), (3.4, 0.5, 2.0), none_on(("up", "down"), lambda f, x, y, w, h: SILVER[4] if x % 2 else SILVER[2]),
           inflate=0.1, density=4, tag="collar")
    m.cube("collar", (-0.5, 5.2, -6.45), (1.0, 1.0, 0.25), lambda f, x, y, w, h: SILVER[5] if (x, y) in ((1, 0), (0, 1), (0, 2), (1, 3)) else
           (MAGENTA[3] if (x, y) == (2, 2) else SILVER[2]), lambda f, x, y, w, h, c: MAGENTA[5] if c == MAGENTA[3] else None, density=4, tag="charm")
    # Legs: long and slender, white-less, small round paws.
    for name, x, z in (("leg_fr", -1.1, -3.6), ("leg_fl", 1.1, -3.6), ("leg_br", -1.1, 3.6), ("leg_bl", 1.1, 3.6)):
        back = z > 0
        m.cube(name, (x - 0.6, 0.6, z - 0.6), (1.2, 5.4, 1.2), fur, density=2, tag="leg")
        if back:
            m.cube(name, (x - 0.75, 3.4, z - 0.6), (1.5, 2.6, 2.0), fur, density=2, tag="thigh")
        m.cube(name, (x - 0.7, 0.0, z - 1.0), (1.4, 0.7, 1.6), fur_m(51002, 2.2), density=2, tag="paw")
    # The tail: up from the rump, a crook at the tip.
    m.cube("tail", (-0.5, 7.6, 4.5), (1.0, 6.6, 1.0), fur, density=2, tag="tail")
    m.cube("tail_tip", (-0.45, 13.6, 4.55), (0.9, 3.4, 0.9), fur, density=2, tag="tail_tip")
    return m


def cat_anims():
    m = cat_rig()
    A = "animation.familiar_cat."
    f = AnimFile()
    clips = []

    def clip(n, length, loop=False):
        clips.append(Clip(f, A + n, length, loop, forbid=("held_item",)))
        return clips[-1]

    # --- idle: standing, tail swaying, ears flicking, the head turning. ---
    L = 4.0
    c = clip("idle", L, loop=True)
    for t, look in ((0, 0), (1.2, 22), (2.4, -16), (4.0, 0)):
        c.key(t, {"head": [0, look, 0]}, E_IO if t else None)
    c.layer("tail", lambda t: [4 * math.sin(t * 2 * math.pi / L), 0, 10 * math.sin(t * 2 * math.pi * 2 / L)])
    c.layer("tail_tip", lambda t: [0, 0, 16 * math.sin(t * 2 * math.pi * 2 / L - 0.8)])
    c.layer("ear_l", lambda t: [0, 0, -18 if 2.9 < t < 3.05 else 0])
    c.layer("@body", lambda t: [0, 0.08 * math.sin(t * 2 * math.pi * 2 / L), 0])

    # --- walk: a slinking diagonal gait. ---
    L = 0.8
    c = clip("walk", L, loop=True)
    for i in range(9):
        ph = 2 * math.pi * i / 8
        s = math.sin(ph)
        c.key(L * i / 8, {"leg_fr": [-28 * s, 0, 0], "leg_bl": [-28 * s, 0, 0], "leg_fl": [28 * s, 0, 0], "leg_br": [28 * s, 0, 0],
                          "@body": [0, 0.2 * abs(math.cos(ph)), 0], "head": [4, 3 * s, 0], "body": [0, 3 * s, 0]}, E_IO if i else None)
    c.layer("tail", lambda t: [-10, 0, 8 * math.sin(t * 2 * math.pi / L)])
    c.layer("tail_tip", lambda t: [0, 0, 12 * math.sin(t * 2 * math.pi / L - 0.8)])

    # --- sit: haunches down, forelegs straight, tail curled round the paws; she watches, blinking slowly. ---
    L = 4.0
    c = clip("sit", L, loop=True)
    sit = {"body": [-34, 0, 0], "@body": [0, -1.6, 1.6], "head": [32, 0, 0], "leg_fr": [34, 0, 0], "leg_fl": [34, 0, 0],
           "leg_br": [-56, 0, 0], "leg_bl": [-56, 0, 0], "@leg_br": [0, 0.6, -1.0], "@leg_bl": [0, 0.6, -1.0], "tail": [86, 0, 0],
           "tail_tip": [30, 40, 0]}
    for t, look in ((0, 0), (2.0, 12), (4.0, 0)):
        c.key(t, P_(sit, {"head": [32, look, 0]}), E_IO if t else None)
    c.layer("tail_tip", lambda t: [0, 0, 10 * math.sin(t * 2 * math.pi / L)])

    # --- steal: a crouch, a dart of the head, the jaw snapping shut on it (tick 6), the head up proud. ---
    c = clip("steal", 0.8)
    c.key(0, {})
    c.key(0.18, {"body": [10, 0, 0], "@body": [0, -0.8, 0], "head": [26, 0, 0], "mouth": [30, 0, 0], "leg_fr": [-20, 0, 0], "leg_fl": [-20, 0, 0],
                 "ear_r": [0, 0, 20], "ear_l": [0, 0, -20]}, E_OUT)
    c.key(0.3, {"body": [12, 0, 0], "@body": [0, -1.0, -0.6], "head": [34, 0, 0], "mouth": [0, 0, 0], "leg_fr": [-26, 0, 0], "leg_fl": [-26, 0, 0]}, E_SNAP)
    c.key(0.5, {"head": [-14, 0, 0], "body": [-6, 0, 0], "tail": [-16, 0, 0]}, E_OUT)
    c.key(0.8, {}, E_IO)
    for cl in clips:
        cl.done(step=0.05)
    return f, clips


# =====================================================================================================
# The raven
# =====================================================================================================

def plumage(seed, base=2.0, sheen=0.3):
    """Black feathers with a blue-violet sheen: rows of scalloped feather edges catching the light."""
    def m(f, x, y, w, h):
        row = (y + (x % 2)) % 3
        v = base + (0.8 if row == 0 else 0.0) + (h01("pl", seed, f, x, y) - 0.5) * 0.3
        if f == "up":
            v += 0.6
        elif f == "down":
            v -= 0.6
        c = tone(FEATHER, v)
        if row == 0 and h01("sh", seed, f, x, y) < sheen:
            c = mix(c, SHEEN, 0.25)
        return c
    return m


def raven_rig():
    m = Model("familiar_raven", 128, 128)
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 4.0, 0), "root")
    m.bone("head", (0, 6.6, -2.6), "body")
    m.bone("beak", (0, 7.4, -4.2), "head")
    m.bone("held_item", (0, 6.4, -6.6), "beak")
    m.bone("wings", (0, 6.2, -1.6), "body")
    m.bone("wing_r", (-1.6, 6.2, -1.6), "wings")
    m.bone("wing_r_tip", (-1.75, 6.0, 2.4), "wing_r")
    m.bone("wing_l", (1.6, 6.2, -1.6), "wings")
    m.bone("wing_l_tip", (1.75, 6.0, 2.4), "wing_l")
    m.bone("tail", (0, 4.6, 2.6), "body")
    m.bone("leg_r", (-0.8, 3.2, 0), "body")
    m.bone("leg_l", (0.8, 3.2, 0), "body")
    pl = plumage(52001)
    # A heavy body, tapering behind; a shaggy throat.
    m.cube("body", (-1.6, 3.0, -2.4), (3.2, 3.6, 4.0), pl, density=4, tag="body")
    m.cube("body", (-1.3, 3.4, 1.4), (2.6, 2.6, 1.8), pl, density=4, tag="rump")
    m.cube("head", (-1.3, 6.0, -4.1), (2.6, 2.6, 2.8), pl, density=4, tag="head")

    def throat(f, x, y, w, h):
        if f == "up":
            return None
        return tone(FEATHER, 2.8 + (0.8 if (x + y) % 2 == 0 else 0)) if not (f != "down" and y == h - 1 and x % 2) else None
    m.cube("head", (-1.0, 4.9, -3.9), (2.0, 1.6, 1.4), throat, density=4, tag="hackles")
    # Eyes: a dark bead with a magenta glint.
    for s in (-1, 1):
        m.cube("head", (s * 1.3 - 0.15 + (0 if s > 0 else -0.0), 7.4, -3.7), (0.3, 0.6, 0.6),
               lambda f, x, y, w, h: MAGENTA[4] if (x + y) % 3 == 0 and f in ("east", "west") else hexc("#08060a"),
               lambda f, x, y, w, h, c: MAGENTA[5] if c == MAGENTA[4] else MAGENTA[2], density=4, tag="eye")
    # The beak: heavy, curved at the tip.
    beakm = lambda f, x, y, w, h: BEAK[4] if f == "up" else BEAK[2] if y < h / 2 else BEAK[1]
    m.cube("beak", (-0.55, 6.9, -6.1), (1.1, 0.9, 2.2), beakm, density=4, tag="beak")
    m.cube("beak", (-0.4, 6.6, -6.5), (0.8, 0.8, 0.6), beakm, density=4, tag="beak_hook", rotation=(-30, 0, 0), pivot=(0, 7.2, -6.1))
    m.cube("beak", (-0.6, 7.3, -4.4), (1.2, 0.5, 1.0), lambda f, x, y, w, h: tone(FEATHER, 2.4), density=4, tag="nasal_bristles")
    # Wings folded along the sides: the coverts and secondaries, then the long primaries to the tail.
    for side, s in (("r", -1), ("l", 1)):
        x0 = s * 1.75 - 0.25
        wing = plumage(52010 + s, 2.6)
        m.cube(f"wing_{side}", (x0, 3.6, -1.9), (0.5, 2.8, 4.4), wing, density=4, tag="wing")
        def primaries(f, x, y, w, h, wing=wing):
            if f not in ("up", "down") and y >= h - 1 and x % 2:
                return None
            return wing(f, x, y, w, h)
        m.cube(f"wing_{side}_tip", (s * 1.75 - 0.2, 3.9, 2.2), (0.4, 2.2, 4.2), primaries, density=4, tag="primaries", rotation=(-8, 0, 0),
               pivot=(s * 1.75, 6.0, 2.4))
    # A wedge tail.
    m.cube("tail", (-1.1, 4.1, 2.6), (2.2, 0.5, 4.2), lambda f, x, y, w, h: None if f in ("north",) else
           (tone(FEATHER, 3.0 + (x % 2)) if f in ("up", "down") else FEATHER[2]), density=4, tag="tail", rotation=(-16, 0, 0), pivot=(0, 4.6, 2.6))
    # Legs and feet.
    for side, s in (("r", -1), ("l", 1)):
        x = s * 0.8
        m.cube(f"leg_{side}", (x - 0.25, 0.4, -0.25), (0.5, 3.0, 0.5), solid(BEAK[2]), density=4, tag="leg")
        for k, a in enumerate((-25, 0, 25)):
            m.cube(f"leg_{side}", (x - 0.15, 0.0, -1.4), (0.3, 0.4, 1.4), solid(BEAK[3]), density=4, tag="toe", rotation=(0, a, 0),
                   pivot=(x, 0.2, 0))
        m.cube(f"leg_{side}", (x - 0.15, 0.0, 0.0), (0.3, 0.4, 0.9), solid(BEAK[3]), density=4, tag="toe")
    return m


def raven_anims():
    m = raven_rig()
    A = "animation.familiar_raven."
    f = AnimFile()
    clips = []

    def clip(n, length, loop=False):
        clips.append(Clip(f, A + n, length, loop, forbid=("held_item", "wings")))
        return clips[-1]

    # --- idle: perched, the head cocking side to side, a ruffle, a little hop. ---
    L = 4.0
    c = clip("idle", L, loop=True)
    for t, look, tilt in ((0, 0, 0), (0.8, 30, 16), (1.6, 30, 16), (2.0, -24, -20), (2.9, -24, -20), (3.4, 0, 0), (4.0, 0, 0)):
        c.key(t, {"head": [0, look, tilt]}, E_SNAP if t else None)
    c.layer("@root", lambda t: [0, 1.2 * max(0, math.sin((t - 3.4) * math.pi / 0.3)) if 3.4 < t < 3.7 else 0, 0])
    c.layer("wing_r", lambda t: [0, 0, 8 * max(0, math.sin((t - 3.35) * math.pi / 0.35)) if 3.35 < t < 3.7 else 0])
    c.layer("wing_l", lambda t: [0, 0, -8 * max(0, math.sin((t - 3.35) * math.pi / 0.35)) if 3.35 < t < 3.7 else 0])
    c.layer("tail", lambda t: [3 * math.sin(t * 2 * math.pi * 2 / L), 0, 0])

    # --- fly: wings spread wide and beating, body level, legs tucked, the tail fanned. ---
    L = 0.6
    c = clip("fly", L, loop=True)
    for i in range(7):
        ph = 2 * math.pi * i / 6
        k = math.sin(ph)
        flap = 34 * k
        pose = {"body": [6, 0, 0], "head": [-8, 0, 0], "leg_r": [70, 0, 0], "leg_l": [70, 0, 0], "tail": [-8, 0, 0],
                "wing_r": [0, 88, -6 - flap], "wing_l": [0, -88, 6 + flap], "wing_r_tip": [0, 0, -18 * math.sin(ph - 0.9)],
                "wing_l_tip": [0, 0, 18 * math.sin(ph - 0.9)], "@body": [0, -0.6 * k, 0]}
        c.key(L * i / 6, pose, E_IO if i else None)

    # --- steal: a lunge, the beak stabbing down and snapping it up (tick 6), the head back. ---
    c = clip("steal", 0.8)
    c.key(0, {})
    c.key(0.15, {"body": [-10, 0, 0], "head": [-20, 0, 0], "wing_r": [0, 0, 20], "wing_l": [0, 0, -20]}, E_OUT)
    c.key(0.3, {"body": [26, 0, 0], "head": [40, 0, 0], "@root": [0, 0, -0.8], "wing_r": [0, 0, 30], "wing_l": [0, 0, -30]}, E_SNAP)
    c.key(0.5, {"body": [-6, 0, 0], "head": [-24, 0, 0]}, E_OUT)
    c.key(0.8, {}, E_IO)
    for cl in clips:
        cl.done(step=0.05)
    return f, clips


ANIMS = {"cat_rig": cat_anims, "raven_rig": raven_anims}


def generate():
    for name, rigf, animf in (("familiar_cat", cat_rig, cat_anims), ("familiar_raven", raven_rig, raven_anims)):
        m = rigf()
        t, g = m.build(gutter=1, seed=sum(map(ord, name)))
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        f, _ = animf()
        write_compact(f, ANIM + name + ".animation.json")


if __name__ == "__main__":
    generate()
