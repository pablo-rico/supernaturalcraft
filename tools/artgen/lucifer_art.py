"""Lucifer: GeckoLib geometry, four phase textures (+ glowmasks) and every animation.

Phases share one rig. Bones are shown/hidden per phase by the renderer:
  P1 Vessel     -- Nick's body in a long coat, red eyes, burn scars. No wings.
  P2 Fallen     -- the upper pair of wings, made of ash with ember edges; more burns.
  P3 Cage Wrath -- four wings, frost and cracks of cold light across the skin.
  P4 Archangel  -- six radiant wings, a halo, light pouring out of every crack.
"""

import math
import random

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import box
from humanoid import humanoid
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
CLEAR = (0, 0, 0, 0)

SKIN = Ramp("#4a3029", "#7a564a", "#a07a69", "#bd977f", "#d3b09a", "#e6cbb8")
HAIR = Ramp("#120d09", "#22180f", "#3a2a1b", "#4f3a26", "#664c32", "#7d5f40")
COAT = Ramp("#0d0d10", "#18181c", "#232328", "#2f2f35", "#3c3c43", "#4b4b53")
SHIRT = Ramp("#1a1612", "#29231d", "#3a3129", "#4a3f35", "#5c4f43", "#6e6052")
TROUSER = Ramp("#0b0b0d", "#141418", "#1d1d22", "#27272d", "#323239", "#3e3e46")
BOOT = Ramp("#070605", "#120e0b", "#1d1611", "#291f18", "#36291f", "#443428")
BURN = Ramp("#2a0d08", "#4d170f", "#712418", "#93372a", "#b25444", "#c97a68")
ASH_WING = Ramp("#0a0909", "#171515", "#252121", "#36302e", "#4a423e", "#5f5550")
FROST_WING = Ramp("#2a3542", "#46566a", "#6a7e95", "#93a8be", "#bfd0e0", "#e8f2fb")
GRACE_WING = Ramp("#8a7440", "#b89e5e", "#dcc68a", "#f0e2b4", "#fbf3da", "#ffffff")

EYE_RED = hexc("#ff2a12")
EYE_COLD = hexc("#b8f0ff")
EYE_WHITE = hexc("#ffffff")
EMBER = [hexc("#ff4a0a"), hexc("#ff8a1e"), hexc("#ffc04a")]
CRACK_COLD = hexc("#cdf6ff")
CRACK_GOLD = hexc("#fff3c4")


# --- geometry -------------------------------------------------------------------------------

WING_BONES = ("wing_r1", "wing_r1_tip", "wing_l1", "wing_l1_tip", "wing_r2", "wing_r2_tip", "wing_l2",
              "wing_l2_tip", "wing_r3", "wing_l3")


def rig():
    r, p = humanoid("lucifer", 128, 128)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 29, -4), (8, 3, 8), inflate=0.3, tag="hair")
    p["coat"] = box(body, (-4, 12, -2), (8, 12, 4), inflate=0.35, tag="coat")
    p["collar"] = box(body, (-4, 23, -2), (8, 2, 4), inflate=0.6, tag="collar")
    p["flap_r"] = box(r.get("right_leg"), (-4.1, 3, -2), (4, 9, 4), inflate=0.45, tag="flap")
    p["flap_l"] = box(r.get("left_leg"), (0.1, 3, -2), (4, 9, 4), inflate=0.45, tag="flap")
    p["cuff_r"] = box(r.get("right_arm"), (-8, 15, -2), (4, 1, 4), inflate=0.3, tag="cuff")
    p["cuff_l"] = box(r.get("left_arm"), (4, 15, -2), (4, 1, 4), inflate=0.3, tag="cuff")

    halo = r.bone("halo", (0, 30.5, 5.5), "head")
    p["halo"] = [box(halo, (-5, 35, 5), (10, 1, 1)), box(halo, (-5, 25, 5), (10, 1, 1)),
                 box(halo, (4, 26, 5), (1, 9, 1)), box(halo, (-5, 26, 5), (1, 9, 1))]

    r.bone("wings", (0, 21, 2), "body")
    # Upper pair: the great wings. Right wing reaches to -X (the entity's right).
    for side, s in (("r", -1), ("l", 1)):
        rot = (0, 14 * -s, 24 * -s)            # swept back and raised (see geomodel conventions)
        w1 = r.bone(f"wing_{side}1", (2 * s, 21, 2), "wings", rotation=rot)
        x0 = 2 * s if s > 0 else -18
        p[f"bar_{side}1"] = box(w1, (x0, 20, 2), (16, 2, 1), tag="bar")
        p[f"plume_{side}1"] = box(w1, (x0, 7, 2.5), (16, 13, 0), faces=("north", "south"), tag=f"plume_{side}")
        tip = r.bone(f"wing_{side}1_tip", (18 * s, 21, 2), f"wing_{side}1", rotation=(0, 10 * -s, -12 * -s))
        tx = 18 if s > 0 else -30
        p[f"bar_{side}1t"] = box(tip, (tx, 20, 2), (12, 2, 1), tag="bar")
        p[f"plume_{side}1t"] = box(tip, (tx, 3, 2.5), (12, 17, 0), faces=("north", "south"), tag=f"primary_{side}")

        w2 = r.bone(f"wing_{side}2", (2 * s, 18, 2.2), "wings", rotation=(0, 20 * -s, -4 * -s))
        x2 = 2 if s > 0 else -16
        p[f"bar_{side}2"] = box(w2, (x2, 17, 2.5), (14, 1, 1), tag="bar")
        p[f"plume_{side}2"] = box(w2, (x2, 5, 3), (14, 12, 0), faces=("north", "south"), tag=f"plume_{side}")
        tip2 = r.bone(f"wing_{side}2_tip", (16 * s, 18, 2.5), f"wing_{side}2", rotation=(0, 8 * -s, -10 * -s))
        x2t = 16 if s > 0 else -26
        p[f"plume_{side}2t"] = box(tip2, (x2t, 3, 3), (10, 15, 0), faces=("north", "south"), tag=f"primary_{side}")

        w3 = r.bone(f"wing_{side}3", (2 * s, 15, 2.4), "wings", rotation=(0, 26 * -s, -38 * -s))
        x3 = 2 if s > 0 else -14
        p[f"bar_{side}3"] = box(w3, (x3, 14, 3), (12, 1, 1), tag="bar")
        p[f"plume_{side}3"] = box(w3, (x3, 3, 3.5), (12, 11, 0), faces=("north", "south"), tag=f"primary_{side}")
    r.pack()
    return r, p


# --- texture --------------------------------------------------------------------------------

def crack_field(seed, w, h, density):
    """A set of pixels forming branching cracks, for light seeping through skin and cloth."""
    rng = random.Random(seed)
    pts = set()
    for _ in range(density):
        x, y = rng.randrange(w), rng.randrange(h)
        length = rng.randint(3, 7)
        dx, dy = rng.choice([(1, 0), (0, 1), (1, 1), (-1, 1)])
        for _ in range(length):
            pts.add((x % w, y % h))
            if rng.random() < 0.35:
                dx, dy = rng.choice([(1, 0), (0, 1), (1, 1), (-1, 1), (1, -1)])
            x, y = x + dx, y + dy
    return pts


class Painter:
    """Paints one phase: the albedo texture and its glowmask together."""

    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 40 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 50 + phase)

    def glow(self, cube, face, x, y, c):
        set_face_px(self.g, cube, face, x, y, c)
        set_face_px(self.t, cube, face, x, y, c)

    def cracks(self, cube, color, density, seed):
        """Cracks of light across every face of a cube (P3/P4)."""
        for face, (u, v, w, h) in cube.rects.items():
            for (x, y) in crack_field(seed + u * 7 + v, w, h, density):
                self.glow(cube, face, x, y, color)

    def paint(self):
        p, ph = self.p, self.phase
        # Head: skin, hair, burns.
        hair_m, skin_m = noise(HAIR, 3, 2.5, 1.2, 7.0), noise(SKIN, 4, 3.0, 0.45)
        burn_m = noise(BURN, 5, 2.6, 1.6, 6.0)
        burn_spots = self.burns()

        def head(face, x, y, w, h):
            if face == "up" or (face == "south" and y < 6) or (face in ("east", "west") and y < 3) or (face == "north" and y < 1):
                return hair_m(face, x, y, w, h)
            if (face, x, y) in burn_spots:
                return burn_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        paint_cube(self.t, p["head"], head)
        self.face()
        paint_cube(self.t, p["hair"], self.hair_cap())

        # Torso: dark henley under an open coat.
        paint_cube(self.t, p["body"], cloth(SHIRT, 8, 2.4, 0.3))
        stamp_face(self.t, p["body"], "north", ["...bb...", "...b....", "...b....", "........"], {"b": SHIRT[4]}, 0, 0)
        coat_m = cloth(COAT, 9, 2.5, 0.25)

        def coat(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north" and 2 <= x <= 5:
                return None  # open front
            if face == "north" and x in (1, 6):
                return COAT[4]  # lapels
            if face == "up" and 2 <= x <= 5 and y < 2:
                return None
            return coat_m(face, x, y, w, h)
        paint_cube(self.t, p["coat"], coat)
        paint_cube(self.t, p["collar"], lambda f, x, y, w, h: None if (f in ("down", "up") or (f == "north" and 2 <= x <= 5))
                   else COAT[3] if y == 0 else COAT[2])

        sleeve, hand = cloth(COAT, 10, 2.5, 0.25), noise(SKIN, 6, 3.0, 0.45)
        for side in ("right_arm", "left_arm"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, s=sleeve, hd=hand:
                       hd(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 3)) else s(f, x, y, w, h))
        for c in ("cuff_r", "cuff_l"):
            paint_cube(self.t, p[c], lambda f, x, y, w, h: None if f in ("up", "down") else COAT[4])

        legs, boots = cloth(TROUSER, 11, 2.3, 0.2), noise(BOOT, 12, 2.5, 0.8)
        for side in ("right_leg", "left_leg"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, l=legs, b=boots:
                       b(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 3)) else l(f, x, y, w, h))
        for flap in ("flap_r", "flap_l"):
            def flap_m(face, x, y, w, h):
                if face in ("up", "down"):
                    return None
                if y == h - 1:
                    return COAT[0] if (x * 5 + y) % 4 else None  # ragged hem
                return coat_m(face, x, y, w, h)
            paint_cube(self.t, p[flap], flap_m)

        self.halo()
        self.wings()
        self.phase_overlays()
        return self.t, self.g

    def burns(self):
        """Face-local burn scars on Nick's failing vessel; they spread with every phase."""
        spots = {("north", 6, 1), ("north", 6, 2), ("north", 7, 2), ("north", 7, 4), ("north", 6, 4), ("north", 7, 5)}
        if self.phase >= 2:
            spots |= {("north", 0, 5), ("north", 1, 6), ("north", 0, 6), ("north", 5, 1), ("west", 1, 4), ("west", 2, 5)}
        if self.phase >= 3:
            spots |= {("north", 1, 1), ("north", 2, 7), ("east", 5, 4), ("east", 6, 5), ("east", 6, 6)}
        return spots

    def face(self):
        ph = self.phase
        eye = {1: EYE_RED, 2: EYE_RED, 3: EYE_COLD, 4: EYE_WHITE}[ph]
        pal = {"b": HAIR[1], "s": SKIN[3], "n": SKIN[2], "m": hexc("#5a2c28"), "u": SKIN[2], "w": hexc("#e8e2da")}
        stamp_face(self.t, self.p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            ".we..ew.",
            "........",
            "...nn...",
            "..mmmm.m",
            ".u.u.u..",
        ], pal)
        for x in (2, 5):
            self.glow(self.p["head"], "north", x, 3, eye)
        if ph >= 4:
            for x in (1, 6):
                self.glow(self.p["head"], "north", x, 3, eye)

    def hair_cap(self):
        base = noise(HAIR, 13, 2.6, 1.3, 8.0)

        def m(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north":
                return base(face, x, y, w, h) if y == 0 or (y == 1 and x in (0, 3, 7)) else None
            if face != "up" and y == h - 1 and (x * 3) % 4 == 0:
                return None
            return base(face, x, y, w, h)
        return m

    def halo(self):
        gold = [P.GOLD[4], P.GOLD[5], CRACK_GOLD]
        for c in self.p["halo"]:
            paint_cube(self.t, c, lambda f, x, y, w, h: gold[(x + y) % 3], shading=False)
            paint_cube(self.g, c, lambda f, x, y, w, h: gold[(x + y) % 3], shading=False)

    def wing_ramp(self):
        return {1: ASH_WING, 2: ASH_WING, 3: FROST_WING, 4: GRACE_WING}[self.phase]

    def wings(self):
        ramp = self.wing_ramp()
        for key, cube in self.p.items():
            if not isinstance(cube, list) and cube.tag == "bar":
                paint_cube(self.t, cube, noise(ramp, 20, 1.6, 0.8))
        for key, cube in self.p.items():
            if isinstance(cube, list) or not cube.tag or not cube.tag.startswith(("plume", "primary")):
                continue
            primary = cube.tag.startswith("primary")
            mirror = cube.tag.endswith("_l")
            self.feathers(cube, ramp, primary, mirror, sum(map(ord, key)))

    def feathers(self, cube, ramp, primary, mirror, seed):
        """Two layers: long flight feathers, overlapped near the bar by a staggered row of
        shorter coverts. Each feather has a rounded tip, a darker leading edge and a pale shaft."""
        ph = self.phase
        for face, (u, v, w, h) in cube.rects.items():
            rng = random.Random(seed * 31 + len(face))
            fw = 3 if primary else 4
            ncol = w // fw + 2
            # Flight feathers lengthen toward the wing tip (higher lx) on primaries.
            lengths, tones = [], []
            for col in range(ncol):
                base = h - 1 - rng.randint(0, 2)
                if primary:
                    base -= int(4 * (1 - col / max(1, ncol - 1)))
                lengths.append(max(3, base))
                tones.append(rng.uniform(-0.6, 0.6))
            cov_h = int(h * (0.3 if primary else 0.45))
            cov_len = [cov_h + rng.randint(-1, 1) for _ in range(ncol + 1)]
            cov_tone = [rng.uniform(-0.4, 0.5) for _ in range(ncol + 1)]

            def put(x, y, value, tip):
                c = ramp[max(0, min(5, int(round(value))))]
                self.t.set(u + x, v + y, c)
                if tip:
                    if ph == 2 and rng.random() < 0.75:
                        self.glow_px(u + x, v + y, EMBER[rng.randrange(3)])
                    elif ph == 3 and rng.random() < 0.4:
                        self.glow_px(u + x, v + y, CRACK_COLD)
                    elif ph == 4 and rng.random() < 0.85:
                        self.glow_px(u + x, v + y, CRACK_GOLD)

            for y in range(h):
                for x in range(w):
                    lx = (w - 1 - x) if (mirror ^ (face == "south")) else x
                    # Covert layer on top, offset half a feather.
                    cc = (lx + fw // 2) // fw
                    cx = (lx + fw // 2) % fw
                    if y < cov_len[cc] and not (y == cov_len[cc] - 1 and cx in (0, fw - 1)):
                        val = 3.6 + cov_tone[cc] - (0.9 if cx == 0 else 0) + (0.5 if cx == fw // 2 else 0)
                        put(x, y, val, False)
                        if ph == 4 and y == cov_len[cc] - 1:
                            self.glow_px(u + x, v + y, GRACE_WING[4])
                        continue
                    col, fx = lx // fw, lx % fw
                    length = lengths[col]
                    if y >= length:
                        continue
                    if y >= length - 2 and fx in (0, fw - 1) and fw > 2:
                        continue  # rounded tip
                    if ph == 2 and y > cov_h and rng.random() < 0.04:
                        continue  # ash wings are full of holes
                    val = 3.0 + tones[col] - (y / h) * 1.2
                    if fx == 0:
                        val -= 1.3
                    elif fx == fw // 2:
                        val += 0.6
                    put(x, y, val, y >= length - 2)

    def glow_px(self, x, y, c):
        self.t.set(x, y, c)
        self.g.set(x, y, c)

    def phase_overlays(self):
        ph, p = self.phase, self.p
        if ph == 2:
            # Scorched coat hems.
            for key in ("flap_r", "flap_l", "coat"):
                for face, (u, v, w, h) in p[key].rects.items():
                    for x in range(w):
                        if (x * 7 + u) % 3 == 0 and face != "up":
                            self.glow_px(u + x, v + h - 2, EMBER[x % 3]) if self.t.rows[v + h - 2][u + x][3] else None
        if ph >= 3:
            color = CRACK_COLD if ph == 3 else CRACK_GOLD
            density = 2 if ph == 3 else 4
            for key in ("head", "body", "right_arm", "left_arm", "coat", "right_leg", "left_leg"):
                self.cracks(p[key], color, density, 70 + ph)
            if ph == 3:
                # Rime on the shoulders and hems.
                for key in ("coat", "flap_r", "flap_l", "collar"):
                    for face, (u, v, w, h) in p[key].rects.items():
                        for x in range(w):
                            for y in (0, h - 1):
                                c = self.t.rows[v + y][u + x]
                                if c[3] and (x + y) % 2 == 0:
                                    self.t.set(u + x, v + y, mix(c, FROST_WING[4], 0.6))


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


# --- animations -----------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
E_SNAP = "easeInQuart"


def anims(A="animation.lucifer."):
    """Every clip; {@code A} is the clip-name prefix (Lucifer Uncaged reuses the attacks under its own)."""
    f = AnimFile()

    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.25, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [0, 0, 0]), (1.4, [2, -8, 3], E_IO), (2.8, [-3, 6, -2], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("right_arm", (0, [0, 0, 3]), (2.0, [-4, 0, 5], E_IO), (4.0, [0, 0, 3], E_IO))
    idle.rot("left_arm", (0, [0, 0, -3]), (2.0, [-4, 0, -5], E_IO), (4.0, [0, 0, -3], E_IO))

    fly = f.new(A + "idle_fly", 3.0, loop=True)
    fly.pos("root", (0, [0, 0, 0]), (1.5, [0, 1.5, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    fly.rot("right_leg", (0, [12, 0, 4]), (1.5, [16, 0, 6], E_IO), (3.0, [12, 0, 4], E_IO))
    fly.rot("left_leg", (0, [6, 0, -4]), (1.5, [10, 0, -6], E_IO), (3.0, [6, 0, -4], E_IO))
    fly.rot("right_arm", (0, [-10, 0, 25]), (1.5, [-14, 0, 30], E_IO), (3.0, [-10, 0, 25], E_IO))
    fly.rot("left_arm", (0, [-10, 0, -25]), (1.5, [-14, 0, -30], E_IO), (3.0, [-10, 0, -25], E_IO))
    fly.rot("head", (0, [-6, 0, 0]), (3.0, [-6, 0, 0]))

    walk = f.new(A + "walk", 1.2, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [24 * s, 0, 0]), (0.6, [-24 * s, 0, 0], E_IO), (1.2, [24 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [16 * s, 0, 0]), (0.6, [-16 * s, 0, 0], E_IO), (1.2, [16 * s, 0, 0], E_IO))
    walk.pos("body", (0, [0, 0, 0]), (0.3, [0, 0.4, 0], E_OUT), (0.6, [0, 0, 0], E_IN), (0.9, [0, 0.4, 0], E_OUT), (1.2, [0, 0, 0], E_IN))
    walk.rot("body", (0, [0, 5, 0]), (0.6, [0, -5, 0], E_IO), (1.2, [0, 5, 0], E_IO))

    wings_idle = f.new(A + "wings_idle", 4.0, loop=True)
    for i, (r_, amp) in enumerate((("1", 5), ("2", 4), ("3", 3))):
        wings_idle.rot("wing_r" + r_, (0, [0, 0, 0]), (2.0, [0, 3, amp], E_IO), (4.0, [0, 0, 0], E_IO))
        wings_idle.rot("wing_l" + r_, (0, [0, 0, 0]), (2.0, [0, -3, -amp], E_IO), (4.0, [0, 0, 0], E_IO))

    beat = f.new(A + "wings_fly", 1.0, loop=True)
    for r_, amp, lag in (("1", 34, 0.0), ("2", 26, 0.06), ("3", 18, 0.12)):
        beat.rot("wing_r" + r_, (0, [0, 0, -14]), (0.45 + lag, [0, -6, amp], E_OUT), (1.0, [0, 0, -14], E_IN))
        beat.rot("wing_l" + r_, (0, [0, 0, 14]), (0.45 + lag, [0, 6, -amp], E_OUT), (1.0, [0, 0, 14], E_IN))
    beat.rot("wing_r1_tip", (0, [0, 0, -10]), (0.5, [0, 0, 18], E_OUT), (1.0, [0, 0, -10], E_IN))
    beat.rot("wing_l1_tip", (0, [0, 0, 10]), (0.5, [0, 0, -18], E_OUT), (1.0, [0, 0, 10], E_IN))
    beat.rot("halo", (0, [0, 0, 0]), (1.0, [0, 0, 36]))

    # --- set pieces ---------------------------------------------------------------------
    emerge = f.new(A + "emerge", 6.0, hold=True)
    emerge.pos("root", (0, [0, -36, 0]), (4.5, [0, -2, 0], E_IO), (5.2, [0, 0, 0], E_OUT), (6.0, [0, 0, 0]))
    emerge.rot("head", (0, [40, 0, 0]), (3.5, [30, 0, 0]), (4.6, [-30, 0, 0], E_BACK), (5.4, [-10, 0, 0], E_IO), (6.0, [0, 0, 0], E_IO))
    emerge.rot("right_arm", (0, [0, 0, 10]), (3.5, [-20, 0, 50], E_IO), (4.8, [-30, 0, 75], E_BACK), (6.0, [0, 0, 3], E_IO))
    emerge.rot("left_arm", (0, [0, 0, -10]), (3.5, [-20, 0, -50], E_IO), (4.8, [-30, 0, -75], E_BACK), (6.0, [0, 0, -3], E_IO))
    emerge.rot("body", (0, [25, 0, 0]), (3.5, [15, 0, 0]), (4.6, [-8, 0, 0], E_BACK), (6.0, [0, 0, 0], E_IO))

    def attack(name, length):
        return f.new(A + name, length)

    # Snap: raise the right hand, a flick of the fingers, pillars of hellfire.
    snap = attack("snap", 3.0)
    snap.rot("right_arm", (0, [0, 0, 0]), (1.2, [-95, 15, 0], E_OUT), (1.5, [-88, 15, 0]), (1.55, [-104, 18, 0], E_SNAP),
             (2.2, [-95, 15, 0]), (3.0, [0, 0, 0], E_IO))
    snap.rot("head", (0, [0, 0, 0]), (1.2, [6, -8, 0], E_OUT), (1.55, [10, -10, 0]), (3.0, [0, 0, 0], E_IO))
    snap.rot("body", (0, [0, 0, 0]), (1.2, [0, -10, 0], E_OUT), (3.0, [0, 0, 0], E_IO))

    fling = attack("fling", 2.25)
    fling.rot("right_arm", (0, [0, 0, 0]), (0.75, [-90, -10, 0], E_OUT), (1.0, [-80, 50, 30], E_SNAP), (2.25, [0, 0, 0], E_IO))
    fling.rot("body", (0, [0, 0, 0]), (0.75, [0, -15, 0], E_OUT), (1.0, [0, 25, 0], E_SNAP), (2.25, [0, 0, 0], E_IO))

    grasp = attack("grasp", 3.35)
    grasp.pos("body", (0, [0, 0, 0]), (0.6, [0, -2, 0], E_OUT), (0.8, [0, 0, 0]), (3.35, [0, 0, 0]))
    grasp.rot("body", (0, [0, 0, 0]), (0.6, [20, 0, 0], E_OUT), (1.0, [10, 20, 0]), (1.4, [10, -25, 0], E_SNAP),
              (1.8, [10, 25, 0], E_SNAP), (2.2, [5, 0, 0], E_SNAP), (3.35, [0, 0, 0], E_IO))
    grasp.rot("right_arm", (0, [0, 0, 0]), (0.6, [30, 0, 10]), (1.0, [-100, 0, 20], E_OUT), (1.4, [-40, 0, 0], E_SNAP),
              (2.2, [-110, 0, 0], E_OUT), (2.4, [-20, 0, 0], E_SNAP), (3.35, [0, 0, 0], E_IO))
    grasp.rot("left_arm", (0, [0, 0, 0]), (0.6, [30, 0, -10]), (1.4, [-60, 0, -20], E_OUT), (1.8, [-20, 0, 0], E_SNAP),
              (3.35, [0, 0, 0], E_IO))

    summon = attack("summon", 3.0)
    summon.rot("right_arm", (0, [0, 0, 0]), (1.2, [-40, 0, 70], E_OUT), (1.6, [-60, 0, 80]), (3.0, [0, 0, 0], E_IO))
    summon.rot("left_arm", (0, [0, 0, 0]), (1.2, [-40, 0, -70], E_OUT), (1.6, [-60, 0, -80]), (3.0, [0, 0, 0], E_IO))
    summon.rot("head", (0, [0, 0, 0]), (1.2, [25, 0, 0], E_OUT), (1.6, [-20, 0, 0], E_BACK), (3.0, [0, 0, 0], E_IO))

    sweep = attack("wing_sweep", 2.3)
    for s, side in ((1, "r"), (-1, "l")):
        sweep.rot(f"wing_{side}1", (0, [0, 0, 0]), (0.9, [0, 40 * s, 20 * s], E_OUT), (1.3, [0, -80 * s, 5 * s], E_SNAP),
                  (2.3, [0, 0, 0], E_IO))
        sweep.rot(f"wing_{side}1_tip", (0, [0, 0, 0]), (0.9, [0, 20 * s, 0]), (1.3, [0, -40 * s, 0], E_SNAP), (2.3, [0, 0, 0], E_IO))
    sweep.rot("body", (0, [0, 0, 0]), (0.9, [-10, 0, 0], E_OUT), (1.3, [20, 0, 0], E_SNAP), (2.3, [0, 0, 0], E_IO))

    rain = attack("rain", 4.5)
    rain.rot("right_arm", (0, [0, 0, 0]), (1.5, [-170, 0, 20], E_OUT), (3.5, [-175, 0, 25]), (4.5, [0, 0, 0], E_IO))
    rain.rot("left_arm", (0, [0, 0, 0]), (1.5, [-170, 0, -20], E_OUT), (3.5, [-175, 0, -25]), (4.5, [0, 0, 0], E_IO))
    rain.rot("head", (0, [0, 0, 0]), (1.5, [-35, 0, 0], E_OUT), (3.5, [-35, 0, 0]), (4.5, [0, 0, 0], E_IO))

    fissure = attack("fissure", 3.5)
    fissure.rot("right_arm", (0, [0, 0, 0]), (1.0, [-160, 0, 10], E_OUT), (1.2, [-30, 0, 0], E_SNAP), (2.5, [-30, 0, 0]),
                (3.5, [0, 0, 0], E_IO))
    fissure.rot("body", (0, [0, 0, 0]), (1.0, [-10, 0, 0], E_OUT), (1.2, [30, 0, 0], E_SNAP), (2.5, [25, 0, 0]),
                (3.5, [0, 0, 0], E_IO))
    fissure.pos("body", (0, [0, 0, 0]), (1.2, [0, -2, 0], E_SNAP), (2.5, [0, -2, 0]), (3.5, [0, 0, 0], E_IO))

    leap = attack("leap", 3.25)
    leap.pos("root", (0, [0, 0, 0]), (0.8, [0, -3, 0], E_OUT), (1.0, [0, 0, 0], E_SNAP), (1.8, [0, 0, 0]), (3.25, [0, 0, 0]))
    leap.rot("right_leg", (0, [0, 0, 0]), (0.8, [-40, 0, 0], E_OUT), (1.0, [20, 0, 0]), (1.8, [-50, 0, 0]), (3.25, [0, 0, 0], E_IO))
    leap.rot("left_leg", (0, [0, 0, 0]), (0.8, [-40, 0, 0], E_OUT), (1.0, [20, 0, 0]), (1.8, [0, 0, 0]), (3.25, [0, 0, 0], E_IO))
    leap.rot("right_arm", (0, [0, 0, 0]), (0.8, [40, 0, 30]), (1.2, [-170, 0, 10], E_OUT), (1.9, [-20, 0, 10], E_SNAP),
             (3.25, [0, 0, 0], E_IO))
    leap.rot("left_arm", (0, [0, 0, 0]), (0.8, [40, 0, -30]), (1.2, [-170, 0, -10], E_OUT), (1.9, [-20, 0, -10], E_SNAP),
             (3.25, [0, 0, 0], E_IO))

    cage = attack("cage", 2.25)
    cage.rot("right_arm", (0, [0, 0, 0]), (1.0, [-90, 10, 0], E_OUT), (1.25, [-85, 10, -10], E_SNAP), (2.25, [0, 0, 0], E_IO))
    cage.rot("head", (0, [0, 0, 0]), (1.0, [5, 10, 0], E_OUT), (2.25, [0, 0, 0], E_IO))

    beam = attack("beam", 6.0)
    beam.rot("right_arm", (0, [0, 0, 0]), (1.5, [-20, 0, 95], E_OUT), (4.5, [-20, 0, 95]), (6.0, [0, 0, 0], E_IO))
    beam.rot("left_arm", (0, [0, 0, 0]), (1.5, [-20, 0, -95], E_OUT), (4.5, [-20, 0, -95]), (6.0, [0, 0, 0], E_IO))
    beam.rot("body", (0, [0, 0, 0]), (1.5, [-15, 0, 0], E_OUT), (4.5, [-15, 0, 0]), (6.0, [0, 0, 0], E_IO))
    beam.rot("head", (0, [0, 0, 0]), (1.5, [-30, 0, 0], E_OUT), (4.5, [-30, 0, 0]), (6.0, [0, 0, 0], E_IO))

    illusion = attack("illusion", 2.25)
    illusion.rot("right_arm", (0, [0, 0, 0]), (0.8, [-60, -40, -30], E_OUT), (1.1, [-30, 30, 80], E_SNAP), (2.25, [0, 0, 0], E_IO))
    illusion.rot("left_arm", (0, [0, 0, 0]), (0.8, [-60, 40, 30], E_OUT), (1.1, [-30, -30, -80], E_SNAP), (2.25, [0, 0, 0], E_IO))

    collapse = attack("collapse", 3.5)
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        collapse.rot(side, (0, [0, 0, 0]), (1.25, [-175, 0, 10 * s], E_OUT), (1.45, [-10, 0, 5 * s], E_SNAP), (3.5, [0, 0, 0], E_IO))
    collapse.rot("body", (0, [0, 0, 0]), (1.25, [-12, 0, 0], E_OUT), (1.45, [25, 0, 0], E_SNAP), (2.5, [20, 0, 0]), (3.5, [0, 0, 0], E_IO))

    storm = attack("storm", 4.5)
    for s, side in ((1, "r"), (-1, "l")):
        keys = [(0, [0, 0, 0])]
        for k in range(6):
            t0 = 1.0 + k * 0.5
            keys.append((t0, [0, 30 * s, 25 * s], E_OUT))
            keys.append((t0 + 0.25, [0, -50 * s, -5 * s], E_SNAP))
        keys.append((4.5, [0, 0, 0], E_IO))
        storm.rot(f"wing_{side}1", *keys)
    storm.rot("body", (0, [0, 0, 0]), (1.0, [-12, 0, 0], E_OUT), (4.0, [-12, 0, 0]), (4.5, [0, 0, 0], E_IO))

    judgement = attack("judgement", 5.5)
    judgement.rot("right_arm", (0, [0, 0, 0]), (1.5, [-178, 0, 0], E_OUT), (4.5, [-178, 0, 0]), (5.5, [0, 0, 0], E_IO))
    judgement.rot("head", (0, [0, 0, 0]), (1.5, [-40, 0, 0], E_OUT), (4.5, [-40, 0, 0]), (5.5, [0, 0, 0], E_IO))

    smite = attack("smite_charge", 6.0)
    smite.pos("root", (0, [0, 0, 0]), (3.5, [0, 6, 0], E_IO), (3.6, [0, 4, 0], E_SNAP), (6.0, [0, 0, 0], E_IO))
    smite.rot("right_arm", (0, [0, 0, 0]), (2.0, [-60, 0, 110], E_OUT), (3.5, [-70, 0, 120]), (3.6, [-20, 0, 40], E_SNAP),
              (6.0, [0, 0, 0], E_IO))
    smite.rot("left_arm", (0, [0, 0, 0]), (2.0, [-60, 0, -110], E_OUT), (3.5, [-70, 0, -120]), (3.6, [-20, 0, -40], E_SNAP),
              (6.0, [0, 0, 0], E_IO))
    smite.rot("head", (0, [0, 0, 0]), (2.0, [-35, 0, 0], E_OUT), (3.5, [-40, 0, 0]), (3.6, [20, 0, 0], E_SNAP), (6.0, [0, 0, 0], E_IO))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            smite.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (3.5, [0, -15 * s, 40 * s], E_OUT), (3.6, [0, 20 * s, -10 * s], E_SNAP),
                      (6.0, [0, 0, 0], E_IO))

    drain = attack("drain", 5.75)
    drain.rot("right_arm", (0, [0, 0, 0]), (0.75, [-90, 0, 0], E_OUT), (4.75, [-85, 0, 0]), (5.75, [0, 0, 0], E_IO))
    drain.rot("left_arm", (0, [0, 0, 0]), (0.75, [-30, 0, -40], E_OUT), (4.75, [-30, 0, -40]), (5.75, [0, 0, 0], E_IO))
    drain.rot("head", (0, [0, 0, 0]), (0.75, [-10, 0, 0], E_OUT), (4.75, [-15, 0, 0]), (5.75, [0, 0, 0], E_IO))

    tp = attack("teleport", 1.0)
    tp.scale("root", (0, [1, 1, 1]), (0.45, [0.1, 1.6, 0.1], E_IN), (0.55, [0.1, 1.6, 0.1]), (1.0, [1, 1, 1], E_BACK))

    # --- transformations ------------------------------------------------------------------
    t2 = f.new(A + "transform_2", 4.0)
    t2.rot("body", (0, [0, 0, 0]), (1.0, [30, 0, 0], E_OUT), (2.4, [30, 0, 0]), (2.8, [-15, 0, 0], E_BACK), (4.0, [0, 0, 0], E_IO))
    t2.rot("right_arm", (0, [0, 0, 0]), (1.0, [-50, 0, -20], E_OUT), (2.4, [-50, 0, -20]), (2.8, [-20, 0, 80], E_BACK), (4.0, [0, 0, 3], E_IO))
    t2.rot("left_arm", (0, [0, 0, 0]), (1.0, [-50, 0, 20], E_OUT), (2.4, [-50, 0, 20]), (2.8, [-20, 0, -80], E_BACK), (4.0, [0, 0, -3], E_IO))
    t2.rot("head", (0, [0, 0, 0]), (1.0, [35, 0, 0], E_OUT), (2.4, [35, 0, 0]), (2.8, [-40, 0, 0], E_BACK), (4.0, [0, 0, 0], E_IO))
    for side in ("wing_r1", "wing_l1"):
        t2.scale(side, (0, [0.05, 0.05, 0.05]), (2.4, [0.05, 0.05, 0.05]), (3.0, [1.15, 1.15, 1.15], E_BACK), (4.0, [1, 1, 1], E_IO))

    t3 = f.new(A + "transform_3", 4.0)
    t3.rot("body", (0, [0, 0, 0]), (1.2, [-20, 0, 0], E_OUT), (2.6, [-25, 0, 0]), (4.0, [0, 0, 0], E_IO))
    t3.rot("right_arm", (0, [0, 0, 0]), (1.2, [-30, 0, 100], E_OUT), (2.6, [-35, 0, 110]), (4.0, [0, 0, 3], E_IO))
    t3.rot("left_arm", (0, [0, 0, 0]), (1.2, [-30, 0, -100], E_OUT), (2.6, [-35, 0, -110]), (4.0, [0, 0, -3], E_IO))
    t3.rot("head", (0, [0, 0, 0]), (1.2, [-45, 0, 0], E_OUT), (2.6, [-50, 0, 0]), (4.0, [0, 0, 0], E_IO))
    for side in ("wing_r2", "wing_l2"):
        t3.scale(side, (0, [0.05, 0.05, 0.05]), (1.6, [0.05, 0.05, 0.05]), (2.4, [1.15, 1.15, 1.15], E_BACK), (3.2, [1, 1, 1], E_IO))

    t4 = f.new(A + "transform_4", 8.0)
    t4.pos("root", (0, [0, 0, 0]), (1.5, [0, -2, 0], E_OUT), (5.5, [0, 18, 0], E_IO), (6.2, [0, 16, 0], E_IO), (8.0, [0, 0, 0], E_IO))
    t4.rot("body", (0, [0, 0, 0]), (1.5, [35, 0, 0], E_OUT), (4.5, [30, 0, 0]), (5.6, [-25, 0, 0], E_BACK), (8.0, [0, 0, 0], E_IO))
    t4.rot("head", (0, [0, 0, 0]), (1.5, [40, 0, 0], E_OUT), (4.5, [35, 0, 0]), (5.6, [-55, 0, 0], E_BACK), (8.0, [-6, 0, 0], E_IO))
    t4.rot("right_arm", (0, [0, 0, 0]), (1.5, [-40, 0, -10], E_OUT), (4.5, [-45, 0, -10]), (5.6, [-30, 0, 125], E_BACK),
           (8.0, [-10, 0, 25], E_IO))
    t4.rot("left_arm", (0, [0, 0, 0]), (1.5, [-40, 0, 10], E_OUT), (4.5, [-45, 0, 10]), (5.6, [-30, 0, -125], E_BACK),
           (8.0, [-10, 0, -25], E_IO))
    for side in ("wing_r3", "wing_l3"):
        t4.scale(side, (0, [0.05, 0.05, 0.05]), (4.5, [0.05, 0.05, 0.05]), (5.4, [1.2, 1.2, 1.2], E_BACK), (6.5, [1, 1, 1], E_IO))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2"):
            t4.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (1.5, [0, 50 * s, -20 * s], E_OUT), (4.5, [0, 55 * s, -25 * s]),
                   (5.6, [0, -20 * s, 45 * s], E_BACK), (8.0, [0, 0, 0], E_IO))
    t4.scale("halo", (0, [0, 0, 0]), (5.5, [0, 0, 0]), (6.3, [1.3, 1.3, 1.3], E_BACK), (7.0, [1, 1, 1], E_IO))

    death = f.new(A + "death", 10.0, hold=True)
    death.pos("root", (0, [0, 0, 0]), (2.0, [0, -4, 0], E_IN), (10.0, [0, -4, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (2.0, [-85, 0, 0], E_OUT), (10.0, [-85, 0, 0]))
    death.rot("left_leg", (0, [0, 0, 0]), (2.0, [10, 0, 0], E_OUT), (10.0, [10, 0, 0]))
    death.rot("body", (0, [0, 0, 0]), (2.0, [15, 0, 0], E_OUT), (5.0, [-30, 0, 0], E_IO), (8.0, [-35, 0, 0]), (10.0, [10, 0, 0], E_IN))
    death.rot("head", (0, [0, 0, 0]), (2.0, [30, 0, 0], E_OUT), (5.0, [-60, 0, 0], E_IO), (10.0, [-50, 0, 0]))
    death.rot("right_arm", (0, [0, 0, 0]), (2.0, [-10, 0, 10]), (5.0, [-40, 0, 110], E_IO), (8.0, [-45, 0, 115]), (10.0, [0, 0, 20], E_IN))
    death.rot("left_arm", (0, [0, 0, 0]), (2.0, [-10, 0, -10]), (5.0, [-40, 0, -110], E_IO), (8.0, [-45, 0, -115]), (10.0, [0, 0, -20], E_IN))
    for s, side in ((1, "r"), (-1, "l")):
        for n in ("1", "2", "3"):
            death.rot(f"wing_{side}{n}", (0, [0, 0, 0]), (5.0, [0, -30 * s, 50 * s], E_IO), (8.0, [0, -35 * s, 55 * s]),
                      (10.0, [0, 60 * s, -60 * s], E_IN))
            death.scale(f"wing_{side}{n}", (0, [1, 1, 1]), (8.0, [1, 1, 1]), (10.0, [0.05, 0.05, 0.05], E_IN))
    death.scale("halo", (0, [1, 1, 1]), (8.0, [1.2, 1.2, 1.2]), (10.0, [0, 0, 0], E_IN))
    return f


ATTACK_ANIMS = ["snap", "fling", "grasp", "summon", "wing_sweep", "rain", "fissure", "leap", "cage", "beam", "illusion",
                "collapse", "storm", "judgement", "smite_charge", "drain", "teleport"]


def generate():
    r, _ = rig()
    r.write(GEO + "lucifer.geo.json")
    for phase in (1, 2, 3, 4):
        t, g = paint_phase(phase)
        save(t, "entity", f"lucifer_p{phase}")
        save(g, "entity", f"lucifer_p{phase}_glowmask")
    a = anims()
    a.write(ANIM + "lucifer.animation.json")
    # The Java side names these too; tests compare the two lists.
    with open(ASSETS + "/animations/entity/lucifer.names.txt", "w") as fh:
        fh.write("\n".join(a.names()) + "\n")
