"""Lucifer Uncaged: the archangel out of his Cage. Geometry, six phase textures (+ glowmasks) and
every animation (his own, plus Lucifer's attacks re-used under his prefix).

  P1 Prisoner      -- burned skin split by red light, Enochian shackles and chains, no wings.
  P2 Hellfire      -- the first pair of wings, black with ember tips; the cracks blaze.
  P3 Cold          -- rime on skin and wings, the cracks run pale.
  P4 Legion        -- two pairs, charred black and blood red.
  P5 Morning Star  -- a broken halo, gold light in every crack.
  P6 Light-Bringer -- six white wings, the halo burning, the body almost all light.
"""

import random

import lucifer_art
from animkit import AnimFile
from common import ASSETS, save
from geomodel import box
from humanoid import humanoid
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
A = "animation.lucifer_uncaged."

CHAR = Ramp("#0e0706", "#1d0e0b", "#2e1611", "#412017", "#552b1f", "#6b3a2a")
SKIN = Ramp("#3a221c", "#5c372c", "#7d4e3f", "#9b6652", "#b58069", "#cc9c84")
ROBE = Ramp("#050405", "#0c0a0c", "#151216", "#1f1a20", "#2a232c", "#362d39")
IRON = Ramp("#070708", "#111114", "#1b1b20", "#26262d", "#33333b", "#43434d")
WINGS = {
    2: Ramp("#060505", "#0e0c0c", "#181414", "#231d1c", "#302725", "#3f3330"),
    3: Ramp("#1e2833", "#344457", "#526a82", "#7a94ac", "#a9c0d4", "#dcebf7"),
    4: Ramp("#140204", "#2a0508", "#430a0f", "#5e1218", "#7a1c22", "#97292e"),
    5: Ramp("#1a1006", "#33200c", "#5a3c14", "#8a6424", "#c09640", "#f0cf78"),
    6: Ramp("#8a7a5a", "#b8a57e", "#dccaa2", "#f0e4c8", "#fbf5e6", "#ffffff"),
}
CRACK = {1: hexc("#c41a12"), 2: hexc("#ff5a14"), 3: hexc("#bfefff"), 4: hexc("#ff2a2a"), 5: hexc("#ffe08a"), 6: hexc("#ffffff")}
EYE = {1: hexc("#ff2a12"), 2: hexc("#ff3b12"), 3: hexc("#bfefff"), 4: hexc("#ff1010"), 5: hexc("#ffd060"), 6: hexc("#ffffff")}
EMBER = [hexc("#ff4a0a"), hexc("#ff8a1e"), hexc("#ffc04a")]
CLEAR = (0, 0, 0, 0)


# --- geometry -------------------------------------------------------------------------------------

def rig():
    r, p = humanoid("lucifer_uncaged", 128, 128)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 30, -4), (8, 2, 8), inflate=0.25, tag="hair")
    # A torn black robe from the waist: front and back panels, ragged.
    p["sash"] = box(body, (-4, 11, -2), (8, 2, 4), inflate=0.4, tag="sash")
    p["robe_r"] = box(r.get("right_leg"), (-4.1, 1, -2), (4, 11, 4), inflate=0.5, tag="robe")
    p["robe_l"] = box(r.get("left_leg"), (0.1, 1, -2), (4, 11, 4), inflate=0.5, tag="robe")
    # Pauldrons of black iron, the remains of the Cage's bonds.
    p["pauldron_r"] = box(r.get("right_arm"), (-8.5, 21, -2.5), (5, 3, 5), inflate=0.2, tag="iron")
    p["pauldron_l"] = box(r.get("left_arm"), (3.5, 21, -2.5), (5, 3, 5), inflate=0.2, tag="iron")

    # The broken halo: three of its four sides, and shards floating where the fourth was.
    halo = r.bone("halo", (0, 31, 5.5), "head")
    p["halo"] = [box(halo, (-5, 36, 5), (10, 1, 1)), box(halo, (4, 27, 5), (1, 9, 1)), box(halo, (-5, 30, 5), (1, 6, 1)),
                 box(halo, (-3, 25.5, 5), (2, 1, 1)), box(halo, (1, 25, 5), (1, 1, 1))]

    # Chains: shackles on both wrists and the neck, each with a length of broken chain.
    for side, s, arm in (("r", -1, "right_arm"), ("l", 1, "left_arm")):
        x0 = -8.5 if s < 0 else 3.5
        ch = r.bone(f"chain_{side}", (6 * s, 13, 0), arm)
        p[f"shackle_{side}"] = box(ch, (x0, 13, -2.5), (5, 2.5, 5), tag="iron")
        links = []
        for i in range(5):
            y = 11.5 - i * 2.2
            wide = i % 2 == 0
            links.append(box(ch, (6 * s - (1 if wide else 0.5), y, -0.5 if wide else -1), (2 if wide else 1, 2, 1 if wide else 2), tag="link"))
        p[f"links_{side}"] = links
    neck = r.bone("chain_neck", (0, 24, 0), "body")
    p["collar"] = box(neck, (-4.5, 23, -2.5), (9, 1.5, 5), inflate=0.2, tag="iron")
    p["links_neck"] = [box(neck, (-0.5 if i % 2 else -1, 21 - i * 2.2, -3.2), (1 if i % 2 else 2, 2, 1), tag="link") for i in range(4)]

    # Three pairs of great wings (same bones as Lucifer's, so his wing clips play here too), larger and torn.
    r.bone("wings", (0, 21, 2), "body")
    for side, s in (("r", -1), ("l", 1)):
        w1 = r.bone(f"wing_{side}1", (2 * s, 21, 2), "wings", rotation=(0, 14 * -s, 24 * -s))
        x0 = 2 * s if s > 0 else -20
        p[f"bar_{side}1"] = box(w1, (x0, 20, 2), (18, 2, 1), tag="bar")
        p[f"plume_{side}1"] = box(w1, (x0, 5, 2.5), (18, 15, 0), faces=("north", "south"), tag=f"plume_{side}")
        tip = r.bone(f"wing_{side}1_tip", (20 * s, 21, 2), f"wing_{side}1", rotation=(0, 10 * -s, -12 * -s))
        tx = 20 if s > 0 else -34
        p[f"bar_{side}1t"] = box(tip, (tx, 20, 2), (14, 2, 1), tag="bar")
        p[f"plume_{side}1t"] = box(tip, (tx, 1, 2.5), (14, 19, 0), faces=("north", "south"), tag=f"primary_{side}")
        w2 = r.bone(f"wing_{side}2", (2 * s, 18, 2.2), "wings", rotation=(0, 20 * -s, -4 * -s))
        x2 = 2 if s > 0 else -18
        p[f"bar_{side}2"] = box(w2, (x2, 17, 2.5), (16, 1, 1), tag="bar")
        p[f"plume_{side}2"] = box(w2, (x2, 4, 3), (16, 13, 0), faces=("north", "south"), tag=f"plume_{side}")
        tip2 = r.bone(f"wing_{side}2_tip", (18 * s, 18, 2.5), f"wing_{side}2", rotation=(0, 8 * -s, -10 * -s))
        x2t = 18 if s > 0 else -30
        p[f"plume_{side}2t"] = box(tip2, (x2t, 1, 3), (12, 17, 0), faces=("north", "south"), tag=f"primary_{side}")
        w3 = r.bone(f"wing_{side}3", (2 * s, 15, 2.4), "wings", rotation=(0, 26 * -s, -38 * -s))
        x3 = 2 if s > 0 else -16
        p[f"bar_{side}3"] = box(w3, (x3, 14, 3), (14, 1, 1), tag="bar")
        p[f"plume_{side}3"] = box(w3, (x3, 1, 3.5), (14, 13, 0), faces=("north", "south"), tag=f"primary_{side}")
    r.pack()
    return r, p


# --- texture ----------------------------------------------------------------------------------------

class Painter(lucifer_art.Painter):
    """One phase of him. Reuses Lucifer's feather painting; everything else is his own."""

    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 140 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 150 + phase)

    def wing_ramp(self):
        return WINGS[max(2, self.phase)]

    def feathers(self, cube, ramp, primary, mirror, seed):
        # Lucifer's painter keys its glints on its own phase numbers (2 embers, 3 frost, 4 gold): map ours onto them.
        real = self.phase
        self.phase = {1: 2, 2: 2, 3: 3, 4: 2, 5: 4, 6: 4}[real]
        super().feathers(cube, ramp, primary, mirror, seed)
        self.phase = real
        # Torn wings: bite ragged holes out of the trailing edge.
        rng = random.Random(seed * 7 + real)
        for face, (u, v, w, h) in cube.rects.items():
            for _ in range((w * h) // (18 if real <= 4 else 40)):
                x, y = rng.randrange(w), rng.randrange(h // 2, h)
                self.t.set(u + x, v + y, CLEAR)
                self.g.set(u + x, v + y, CLEAR)

    def paint(self):
        p, ph = self.p, self.phase
        crack = CRACK[ph]
        skin_m = noise(SKIN if ph < 6 else Ramp("#d8c4a8", "#e6d4bc", "#efe2cf", "#f6eee2", "#fbf7ef", "#ffffff"), 4, 2.6, 0.6)
        char_m = noise(CHAR, 5, 2.4, 1.4, 6.0)
        rng = random.Random(90 + ph)

        def body_mat(face, x, y, w, h):
            # Burned skin: charred patches over the vessel, more as the phases go on (until the light takes it).
            n = fbm(x * 3 + sum(map(ord, face)) % 11, y * 3, 77, 48, 48, 2, 5.0)
            if ph < 6 and n > 0.6 - 0.03 * min(ph, 4):
                return char_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        for key in ("head", "body", "right_arm", "left_arm"):
            paint_cube(self.t, p[key], body_mat)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 0) else CHAR[1 + (x + y) % 2])
        robe = cloth(ROBE, 9, 2.3, 0.3)
        for key in ("right_leg", "left_leg"):
            paint_cube(self.t, p[key], robe)
        for key in ("robe_r", "robe_l"):
            def robe_m(face, x, y, w, h):
                if face in ("up", "down"):
                    return None
                if y >= h - 2 and (x * 5 + y + len(face)) % 3 == 0:
                    return None  # tattered hem
                return robe(face, x, y, w, h)
            paint_cube(self.t, p[key], robe_m)
        paint_cube(self.t, p["sash"], lambda f, x, y, w, h: None if f in ("up", "down") else ROBE[4] if y == 0 else ROBE[2])

        # Iron: pauldrons, shackles, collar, links, with Enochian marks that glow.
        iron = noise(IRON, 11, 2.6, 1.0)
        for key, cube in p.items():
            if isinstance(cube, list):
                for c in cube:
                    if c.tag == "link":
                        paint_cube(self.t, c, iron)
                continue
            if cube.tag == "iron":
                paint_cube(self.t, cube, iron)
                for face, (u, v, w, h) in cube.rects.items():
                    if face in ("north", "south", "east", "west") and w >= 3:
                        for x in range(1, w - 1, 2):
                            self.glow_px(u + x, v + h // 2, CRACK[1] if ph < 5 else crack)

        self.face()
        # Cracks of light through the skin (in every phase; brighter and denser as he burns).
        density = {1: 1, 2: 2, 3: 1, 4: 2, 5: 3, 6: 5}[ph]
        for key in ("head", "body", "right_arm", "left_arm"):
            self.cracks(p[key], crack, density, 170 + ph)
        if ph == 3:
            for key in ("body", "right_arm", "left_arm", "pauldron_r", "pauldron_l"):
                for face, (u, v, w, h) in p[key].rects.items():
                    for x in range(w):
                        c = self.t.rows[v][u + x]
                        if c[3] and x % 2 == 0:
                            self.t.set(u + x, v, mix(c, WINGS[3][5], 0.6))
        self.halo()
        if ph >= 2:
            self.wings()
        return self.t, self.g

    def face(self):
        ph = self.phase
        eye = EYE[ph]
        pal = {"b": CHAR[0], "m": hexc("#2a0606"), "u": CHAR[2]}
        stamp_face(self.t, self.p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            "........",
            "........",
            "...u....",
            "..mmmm..",
            "........",
        ], pal)
        for x in (1, 2, 5, 6):
            self.glow(self.p["head"], "north", x, 3, eye)
        if ph >= 4:
            for y in (4,):
                for x in (2, 5):
                    self.glow(self.p["head"], "north", x, y, mix(eye, hexc("#000000"), 0.3))

    def halo(self):
        ph = self.phase
        cols = [hexc("#7a5a1a"), hexc("#c09640"), hexc("#ffe08a")] if ph < 6 else [hexc("#fff3c4"), hexc("#ffffff"), hexc("#ffe8a0")]
        for c in self.p["halo"]:
            paint_cube(self.t, c, lambda f, x, y, w, h: cols[(x + y) % 3], shading=False)
            paint_cube(self.g, c, lambda f, x, y, w, h: cols[(x + y) % 3], shading=False)


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


# --- animations --------------------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
E_SNAP = "easeInQuart"

# Lucifer's clips he uses as they are (under his own prefix).
SHARED = ["idle", "idle_fly", "walk", "wings_idle", "wings_fly", "transform_2",
          "snap", "fling", "grasp", "summon", "wing_sweep", "rain", "fissure", "leap", "cage", "beam", "illusion",
          "collapse", "storm", "judgement", "smite_charge", "drain", "teleport"]
OWN_ATTACKS = ["chain_lash", "shackle_pull", "pillars", "hounds", "frozen_prison", "abyssal_pull", "falling_stars", "tempest", "supernova"]


def anims():
    base = lucifer_art.anims(A)
    f = AnimFile()
    for a in base.anims:
        if a.name[len(A):] in SHARED:
            f.add(a)

    # In chains, waiting: arms hauled up and out, head bowed, a slow sway.
    chained = f.new(A + "chained", 6.0, loop=True)
    chained.rot("right_arm", (0, [-150, 0, 28]), (3.0, [-155, 0, 32], E_IO), (6.0, [-150, 0, 28], E_IO))
    chained.rot("left_arm", (0, [-150, 0, -28]), (3.0, [-155, 0, -32], E_IO), (6.0, [-150, 0, -28], E_IO))
    chained.rot("head", (0, [38, 0, 0]), (2.5, [34, 6, 0], E_IO), (4.0, [10, -4, 0], E_IO), (4.6, [36, 0, 0], E_IO), (6.0, [38, 0, 0], E_IO))
    chained.rot("body", (0, [8, 0, 2]), (3.0, [10, 0, -2], E_IO), (6.0, [8, 0, 2], E_IO))
    chained.rot("right_leg", (0, [6, 0, 0]), (6.0, [6, 0, 0]))
    chained.rot("left_leg", (0, [-2, 0, 0]), (6.0, [-2, 0, 0]))

    # Let down from the Cage: hanging in his chains, then he tears his arms down and lifts his head.
    emerge = f.new(A + "emerge", 8.0, hold=True)
    emerge.rot("right_arm", (0, [-150, 0, 28]), (5.0, [-155, 0, 30]), (5.4, [-30, 0, 60], E_SNAP), (6.4, [-20, 0, 70], E_BACK),
               (8.0, [0, 0, 3], E_IO))
    emerge.rot("left_arm", (0, [-150, 0, -28]), (5.0, [-155, 0, -30]), (5.4, [-30, 0, -60], E_SNAP), (6.4, [-20, 0, -70], E_BACK),
               (8.0, [0, 0, -3], E_IO))
    emerge.rot("head", (0, [38, 0, 0]), (5.0, [36, 0, 0]), (5.6, [-35, 0, 0], E_BACK), (6.6, [-15, 0, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    emerge.rot("body", (0, [10, 0, 0]), (5.0, [10, 0, 0]), (5.5, [-12, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    for side in ("chain_r", "chain_l"):
        emerge.rot(side, (0, [0, 0, 0]), (5.4, [0, 0, 0]), (5.6, [40, 0, 0], E_SNAP), (6.4, [-20, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))

    def roar(clip, start, end):
        clip.rot("body", (0, [0, 0, 0]), (start, [25, 0, 0], E_OUT), (start + 1.2, [25, 0, 0]), (start + 1.6, [-20, 0, 0], E_BACK),
                 (end, [0, 0, 0], E_IO))
        clip.rot("head", (0, [0, 0, 0]), (start, [30, 0, 0], E_OUT), (start + 1.2, [30, 0, 0]), (start + 1.6, [-45, 0, 0], E_BACK),
                 (end, [0, 0, 0], E_IO))
        clip.rot("right_arm", (0, [0, 0, 0]), (start, [-40, 0, -15], E_OUT), (start + 1.2, [-40, 0, -15]), (start + 1.6, [-25, 0, 95], E_BACK),
                 (end, [0, 0, 3], E_IO))
        clip.rot("left_arm", (0, [0, 0, 0]), (start, [-40, 0, 15], E_OUT), (start + 1.2, [-40, 0, 15]), (start + 1.6, [-25, 0, -95], E_BACK),
                 (end, [0, 0, -3], E_IO))

    t3 = f.new(A + "transform_3", 4.0)
    roar(t3, 1.0, 4.0)
    t4 = f.new(A + "transform_4", 4.0)
    roar(t4, 1.0, 4.0)
    for side in ("wing_r2", "wing_l2"):
        t4.scale(side, (0, [0.05, 0.05, 0.05]), (2.2, [0.05, 0.05, 0.05]), (2.9, [1.2, 1.2, 1.2], E_BACK), (3.6, [1, 1, 1], E_IO))
    t5 = f.new(A + "transform_5", 4.0)
    roar(t5, 1.0, 4.0)
    t5.scale("halo", (0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.9, [1.4, 1.4, 1.4], E_BACK), (3.6, [1, 1, 1], E_IO))
    t6 = f.new(A + "transform_6", 8.0)
    t6.pos("root", (0, [0, 0, 0]), (1.5, [0, -2, 0], E_OUT), (5.5, [0, 18, 0], E_IO), (6.2, [0, 16, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    t6.rot("body", (0, [0, 0, 0]), (1.5, [35, 0, 0], E_OUT), (4.5, [30, 0, 0]), (5.6, [-25, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    t6.rot("head", (0, [0, 0, 0]), (1.5, [40, 0, 0], E_OUT), (4.5, [35, 0, 0]), (5.6, [-55, 0, 0], E_BACK), (8.0, [-6, 0, 0], E_IO))
    t6.rot("right_arm", (0, [0, 0, 0]), (1.5, [-40, 0, -10], E_OUT), (4.5, [-45, 0, -10]), (5.6, [-30, 0, 125], E_BACK), (8.0, [-10, 0, 25], E_IO))
    t6.rot("left_arm", (0, [0, 0, 0]), (1.5, [-40, 0, 10], E_OUT), (4.5, [-45, 0, 10]), (5.6, [-30, 0, -125], E_BACK), (8.0, [-10, 0, -25], E_IO))
    for side in ("wing_r3", "wing_l3"):
        t6.scale(side, (0, [0.05, 0.05, 0.05]), (4.5, [0.05, 0.05, 0.05]), (5.4, [1.2, 1.2, 1.2], E_BACK), (6.5, [1, 1, 1], E_IO))
    t6.scale("halo", (0, [1, 1, 1]), (5.4, [1, 1, 1]), (5.9, [1.6, 1.6, 1.6], E_BACK), (7.0, [1, 1, 1], E_IO))

    # Dragged back into the Cage: arms hauled up by the chains, wings folding away.
    death = f.new(A + "death", 10.0, hold=True)
    death.rot("right_arm", (0, [0, 0, 0]), (1.5, [-60, 0, 40], E_OUT), (3.0, [-155, 0, 30], E_SNAP), (10.0, [-160, 0, 28]))
    death.rot("left_arm", (0, [0, 0, 0]), (1.5, [-60, 0, -40], E_OUT), (3.0, [-155, 0, -30], E_SNAP), (10.0, [-160, 0, -28]))
    death.rot("head", (0, [0, 0, 0]), (1.5, [-40, 0, 0], E_OUT), (3.0, [-30, 0, 0]), (6.0, [40, 0, 0], E_IO), (10.0, [45, 0, 0]))
    death.rot("body", (0, [0, 0, 0]), (1.5, [-15, 0, 0], E_OUT), (3.0, [12, 0, 0], E_SNAP), (10.0, [10, 0, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (3.0, [15, 0, 4], E_IO), (10.0, [8, 0, 2]))
    death.rot("left_leg", (0, [0, 0, 0]), (3.0, [-8, 0, -4], E_IO), (10.0, [-4, 0, -2]))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            death.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (3.0, [0, -30 * s, 50 * s], E_IO), (7.0, [0, 60 * s, -40 * s], E_IN))
            death.scale(f"wing_{side}{n}", (0, [1, 1, 1]), (6.0, [1, 1, 1]), (9.0, [0.05, 0.05, 0.05], E_IN), (10.0, [0.05, 0.05, 0.05]))
    death.scale("halo", (0, [1, 1, 1]), (6.0, [1.3, 1.3, 1.3]), (9.0, [0, 0, 0], E_IN), (10.0, [0, 0, 0]))

    def attack(name, length):
        return f.new(A + name, length)

    lash = attack("chain_lash", 2.7)
    lash.rot("right_arm", (0, [0, 0, 0]), (1.0, [-120, 0, -40], E_OUT), (1.25, [-20, 0, 60], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("left_arm", (0, [0, 0, 0]), (1.0, [-120, 0, 40], E_OUT), (1.25, [-20, 0, -60], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("chain_r", (0, [0, 0, 0]), (1.0, [60, 0, 0], E_OUT), (1.3, [-90, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("chain_l", (0, [0, 0, 0]), (1.0, [60, 0, 0], E_OUT), (1.3, [-90, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))
    lash.rot("body", (0, [0, 0, 0]), (1.0, [-12, 0, 0], E_OUT), (1.25, [18, 0, 0], E_SNAP), (2.7, [0, 0, 0], E_IO))

    pull = attack("shackle_pull", 3.4)
    pull.rot("right_arm", (0, [0, 0, 0]), (0.8, [-90, 0, 40], E_OUT), (2.2, [-60, 0, 10], E_IO), (2.4, [-150, 0, 20], E_SNAP), (3.4, [0, 0, 0], E_IO))
    pull.rot("left_arm", (0, [0, 0, 0]), (0.8, [-90, 0, -40], E_OUT), (2.2, [-60, 0, -10], E_IO), (2.4, [-150, 0, -20], E_SNAP), (3.4, [0, 0, 0], E_IO))
    pull.rot("body", (0, [0, 0, 0]), (0.8, [10, 0, 0], E_OUT), (2.2, [-15, 0, 0], E_IO), (2.4, [25, 0, 0], E_SNAP), (3.4, [0, 0, 0], E_IO))

    pillars = attack("pillars", 3.7)
    pillars.rot("right_arm", (0, [0, 0, 0]), (1.4, [-170, 0, 15], E_OUT), (1.6, [-20, 0, 10], E_SNAP), (3.7, [0, 0, 0], E_IO))
    pillars.rot("left_arm", (0, [0, 0, 0]), (1.4, [-170, 0, -15], E_OUT), (1.6, [-20, 0, -10], E_SNAP), (3.7, [0, 0, 0], E_IO))
    pillars.rot("body", (0, [0, 0, 0]), (1.4, [-15, 0, 0], E_OUT), (1.6, [30, 0, 0], E_SNAP), (2.6, [25, 0, 0]), (3.7, [0, 0, 0], E_IO))
    pillars.pos("body", (0, [0, 0, 0]), (1.6, [0, -2.5, 0], E_SNAP), (2.6, [0, -2.5, 0]), (3.7, [0, 0, 0], E_IO))

    hounds = attack("hounds", 3.0)
    hounds.rot("right_arm", (0, [0, 0, 0]), (1.0, [-80, 0, 20], E_OUT), (1.5, [-30, 0, 70], E_BACK), (3.0, [0, 0, 0], E_IO))
    hounds.rot("head", (0, [0, 0, 0]), (1.0, [20, -20, 0], E_OUT), (1.5, [-25, 0, 0], E_BACK), (3.0, [0, 0, 0], E_IO))

    prison = attack("frozen_prison", 6.0)
    prison.rot("right_arm", (0, [0, 0, 0]), (1.0, [-95, -10, 0], E_OUT), (1.1, [-90, -10, -15], E_SNAP), (5.0, [-90, -10, -10]), (6.0, [0, 0, 0], E_IO))
    prison.rot("head", (0, [0, 0, 0]), (1.0, [5, -12, 0], E_OUT), (5.0, [5, -12, 0]), (6.0, [0, 0, 0], E_IO))

    abyss = attack("abyssal_pull", 6.0)
    abyss.rot("right_arm", (0, [0, 0, 0]), (1.5, [-40, 0, 100], E_OUT), (5.0, [-45, 0, 105]), (6.0, [0, 0, 0], E_IO))
    abyss.rot("left_arm", (0, [0, 0, 0]), (1.5, [-40, 0, -100], E_OUT), (5.0, [-45, 0, -105]), (6.0, [0, 0, 0], E_IO))
    abyss.rot("head", (0, [0, 0, 0]), (1.5, [30, 0, 0], E_OUT), (5.0, [30, 0, 0]), (6.0, [0, 0, 0], E_IO))

    stars = attack("falling_stars", 5.0)
    stars.rot("right_arm", (0, [0, 0, 0]), (1.8, [-178, 0, 10], E_OUT), (2.0, [-60, 0, 30], E_SNAP), (4.0, [-60, 0, 30]), (5.0, [0, 0, 0], E_IO))
    stars.rot("left_arm", (0, [0, 0, 0]), (1.8, [-178, 0, -10], E_OUT), (2.0, [-60, 0, -30], E_SNAP), (4.0, [-60, 0, -30]), (5.0, [0, 0, 0], E_IO))
    stars.rot("head", (0, [0, 0, 0]), (1.8, [-45, 0, 0], E_OUT), (2.0, [10, 0, 0], E_SNAP), (5.0, [0, 0, 0], E_IO))

    tempest = attack("tempest", 3.5)
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            tempest.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (1.6, [0, 45 * s, 30 * s], E_OUT), (1.85, [0, -85 * s, -10 * s], E_SNAP),
                        (3.5, [0, 0, 0], E_IO))
    tempest.rot("body", (0, [0, 0, 0]), (1.6, [-14, 0, 0], E_OUT), (1.85, [18, 0, 0], E_SNAP), (3.5, [0, 0, 0], E_IO))

    nova = attack("supernova", 7.5)
    nova.pos("root", (0, [0, 0, 0]), (4.5, [0, 8, 0], E_IO), (4.6, [0, 6, 0], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("right_arm", (0, [0, 0, 0]), (3.0, [-60, 0, 120], E_OUT), (4.5, [-70, 0, 135]), (4.6, [-20, 0, 30], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("left_arm", (0, [0, 0, 0]), (3.0, [-60, 0, -120], E_OUT), (4.5, [-70, 0, -135]), (4.6, [-20, 0, -30], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.rot("head", (0, [0, 0, 0]), (3.0, [-45, 0, 0], E_OUT), (4.5, [-50, 0, 0]), (4.6, [25, 0, 0], E_SNAP), (7.5, [0, 0, 0], E_IO))
    nova.scale("halo", (0, [1, 1, 1]), (4.5, [1.8, 1.8, 1.8], E_IO), (4.6, [1, 1, 1], E_SNAP), (7.5, [1, 1, 1]))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            nova.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (4.5, [0, -20 * s, 50 * s], E_OUT), (4.6, [0, 25 * s, -15 * s], E_SNAP),
                     (7.5, [0, 0, 0], E_IO))
    return f


def generate():
    r, _ = rig()
    r.write(GEO + "lucifer_uncaged.geo.json")
    for phase in range(1, 7):
        t, g = paint_phase(phase)
        save(t, "entity", f"lucifer_uncaged_p{phase}")
        save(g, "entity", f"lucifer_uncaged_p{phase}_glowmask")
    a = anims()
    a.write(ANIM + "lucifer_uncaged.animation.json")
    with open(ASSETS + "/animations/entity/lucifer_uncaged.names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")
