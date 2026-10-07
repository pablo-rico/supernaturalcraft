"""Azazel, the Yellow-Eyed Demon: GeckoLib geometry, two phase textures (+ glowmasks), every animation,
his trophy bust, Samuel Colt's rails, his blood and his yellow smoke.

  P1 The Yellow-Eyed Demon -- a man in a dark suit and tie; only the eyes give him away.
  P2 Smoke and Fire        -- the vessel cracks: yellow light through the skin, the suit scorched.
"""

import json
import os
import random

import palette as P
from animkit import AnimFile
from common import ASSETS, save
from geomodel import box
from humanoid import humanoid
from items import vial
from lucifer_art import BOOT, HAIR, SKIN, TROUSER, crack_field
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from particles import _blob
from pixelkit import Ramp, Tex, fbm, hexc, mix

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
MODELS = ASSETS + "/models/block"

SUIT = Ramp("#0e0d0c", "#181614", "#22201d", "#2d2a26", "#3a3631", "#48433d")
SHIRT = Ramp("#7d7a74", "#a29e96", "#bdb9b0", "#d2cec5", "#e3e0d8", "#f1eee8")
TIE = Ramp("#120707", "#240c0c", "#381313", "#4d1b1a", "#622422", "#782e2b")
SCORCH = Ramp("#0a0605", "#1c0f09", "#33190d", "#4f2610", "#6e3612", "#8f4a16")
YELLOW = [hexc("#c79a12"), hexc("#f2d22e"), hexc("#fff6a6")]
EYE = hexc("#ffd21a")


# --- geometry -------------------------------------------------------------------------------

def rig():
    r, p = humanoid("azazel", 128, 128)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 30, -4), (8, 2, 8), inflate=0.3, tag="hair")
    p["jacket"] = box(body, (-4, 12, -2), (8, 12, 4), inflate=0.3, tag="jacket")
    p["tails_r"] = box(r.get("right_leg"), (-4.1, 6, -2), (4, 6, 4), inflate=0.4, tag="tails")
    p["tails_l"] = box(r.get("left_leg"), (0.1, 6, -2), (4, 6, 4), inflate=0.4, tag="tails")
    p["cuff_r"] = box(r.get("right_arm"), (-8, 15, -2), (4, 1, 4), inflate=0.25, tag="cuff")
    p["cuff_l"] = box(r.get("left_arm"), (4, 15, -2), (4, 1, 4), inflate=0.25, tag="cuff")
    r.pack()
    return r, p


# --- texture --------------------------------------------------------------------------------

class Painter:
    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 70 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 80 + phase)

    def glow(self, cube, face, x, y, c):
        set_face_px(self.g, cube, face, x, y, c)
        set_face_px(self.t, cube, face, x, y, c)

    def cracks(self, cube, density, seed, faces=None):
        rng = random.Random(seed)
        for face, (u, v, w, h) in cube.rects.items():
            if faces and face not in faces:
                continue
            for (x, y) in crack_field(seed + u * 7 + v, w, h, density):
                self.glow(cube, face, x, y, YELLOW[1] if rng.random() < 0.75 else YELLOW[2])

    def paint(self):
        p, ph = self.p, self.phase
        hair_m, skin_m = noise(HAIR, 21, 2.2, 1.0, 7.0), noise(SKIN, 22, 3.2, 0.4)

        def head(face, x, y, w, h):
            if face == "up" or (face == "south" and y < 5) or (face in ("east", "west") and y < 2) or (face == "north" and y < 1):
                return hair_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        paint_cube(self.t, p["head"], head)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 0)
                   else hair_m(f, x, y, w, h))
        self.face()

        # Shirt and tie under a buttoned jacket: the shirt shows in a V at the collar.
        shirt_m = cloth(SHIRT, 23, 2.6, 0.2)
        paint_cube(self.t, p["body"], shirt_m)
        suit_m = cloth(SUIT, 24, 2.6, 0.3)

        def jacket(face, x, y, w, h):
            if face == "down":
                return None
            if face == "north":
                v = 3 - min(3, y)  # the V narrows down to the button at y = 4
                if y < 4 and 3 - v <= x <= 4 + v and not (3 - v == x or 4 + v == x):
                    return None
                if y < 4 and (x == 3 - v or x == 4 + v):
                    return SUIT[4]  # lapel edge
                if x in (3, 4) and y in (5, 8):
                    return hexc("#0a0908")  # buttons
            if face == "up" and 3 <= x <= 4 and y < 2:
                return None
            return suit_m(face, x, y, w, h)
        paint_cube(self.t, p["jacket"], jacket)
        stamp_face(self.t, p["body"], "north", [
            "...tt...",
            "...TT...",
            "...tt...",
            "...TT...",
        ], {"t": TIE[3], "T": TIE[2]})

        sleeve, hand = cloth(SUIT, 25, 2.6, 0.3), noise(SKIN, 26, 3.2, 0.4)
        for side in ("right_arm", "left_arm"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, s=sleeve, hd=hand:
                       hd(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 3)) else s(f, x, y, w, h))
        for c in ("cuff_r", "cuff_l"):
            paint_cube(self.t, p[c], lambda f, x, y, w, h: None if f in ("up", "down") else SHIRT[4])

        legs, shoes = cloth(TROUSER, 27, 2.3, 0.2), noise(BOOT, 28, 2.0, 0.5)
        for side in ("right_leg", "left_leg"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, l=legs, b=shoes:
                       b(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 2)) else l(f, x, y, w, h))
        for tail in ("tails_r", "tails_l"):
            paint_cube(self.t, p[tail], lambda f, x, y, w, h: None if f in ("up", "down")
                       else suit_m(f, x, y, w, h))

        if ph >= 2:
            self.vessel_cracks()
        return self.t, self.g

    def face(self):
        pal = {"b": HAIR[1], "n": SKIN[2], "m": hexc("#4a2a24"), "s": SKIN[1], "w": hexc("#e8e2da")}
        stamp_face(self.t, self.p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            ".w....w.",
            "........",
            "...nn...",
            "..mmmm..",
            "...ss...",
        ], pal)
        for x in (2, 5):
            self.glow(self.p["head"], "north", x, 3, EYE)
        if self.phase >= 2:
            for x in (1, 6):
                self.glow(self.p["head"], "north", x, 3, YELLOW[0])

    def vessel_cracks(self):
        """Phase 2: the vessel splits, yellow light through the cracks; the suit is scorched at the edges."""
        p = self.p
        self.cracks(p["head"], 1, 301, faces=("north", "east", "west"))
        self.cracks(p["body"], 1, 302, faces=("north",))
        self.cracks(p["jacket"], 1, 307, faces=("north", "south"))
        for side, seed in (("right_arm", 303), ("left_arm", 304)):
            self.cracks(p[side], 1, seed, faces=("north", "east", "west"))
        scorch = noise(SCORCH, 305, 2.5, 1.4, 5.0)
        for name in ("jacket", "tails_r", "tails_l", "right_arm", "left_arm"):
            cube = p[name]
            for face, (u, v, w, h) in cube.rects.items():
                for y in range(h):
                    for x in range(w):
                        n = fbm(x + u, y + v, 306, 128, 128, 3, 6.0)
                        if n > 0.66:
                            set_face_px(self.t, cube, face, x, y, scorch(face, x, y, w, h))


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


# --- animations -----------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
E_SNAP = "easeInQuart"
A = "animation.azazel."


def anims():
    f = AnimFile()

    # A loose, unhurried stance: he is never in a hurry.
    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.2, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [-4, 0, 4]), (2.0, [-2, 10, 6], E_IO), (4.0, [-4, 0, 4], E_IO))
    idle.rot("right_arm", (0, [6, 0, 4]), (2.0, [4, 0, 6], E_IO), (4.0, [6, 0, 4], E_IO))
    idle.rot("left_arm", (0, [6, 0, -4]), (2.0, [4, 0, -6], E_IO), (4.0, [6, 0, -4], E_IO))

    walk = f.new(A + "walk", 1.3, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [22 * s, 0, 0]), (0.65, [-22 * s, 0, 0], E_IO), (1.3, [22 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [12 * s, 0, 0]), (0.65, [-12 * s, 0, 0], E_IO), (1.3, [12 * s, 0, 0], E_IO))
    walk.rot("head", (0, [-4, 0, 0]), (1.3, [-4, 0, 0]))

    # The shared controllers ask for these; he has no wings and never flies.
    for name in ("idle_fly", "wings_idle", "wings_fly"):
        f.new(A + name, 1.0, loop=True).const_rot("root", [0, 0, 0])

    emerge = f.new(A + "emerge", 4.0, hold=True)
    emerge.pos("root", (0, [0, -10, 0]), (2.5, [0, -1, 0], E_IO), (3.0, [0, 0, 0], E_OUT), (4.0, [0, 0, 0]))
    emerge.scale("root", (0, [0.2, 0.2, 0.2]), (2.5, [0.95, 0.95, 0.95], E_IO), (3.0, [1, 1, 1], E_OUT), (4.0, [1, 1, 1]))
    emerge.rot("head", (0, [50, 0, 0]), (2.6, [40, 0, 0]), (3.2, [-15, 0, 0], E_BACK), (4.0, [-4, 0, 4], E_IO))
    emerge.rot("body", (0, [30, 0, 0]), (2.6, [20, 0, 0]), (3.2, [-5, 0, 0], E_BACK), (4.0, [0, 0, 0], E_IO))

    def attack(name, length):
        return f.new(A + name, length)

    shove = attack("shove", 1.9)
    shove.rot("right_arm", (0, [0, 0, 0]), (0.7, [-40, 0, -10], E_OUT), (0.85, [-90, 0, 0], E_SNAP), (1.3, [-88, 0, 0]),
              (1.9, [0, 0, 0], E_IO))
    shove.rot("body", (0, [0, 0, 0]), (0.7, [0, -20, 0], E_OUT), (0.85, [8, 10, 0], E_SNAP), (1.9, [0, 0, 0], E_IO))
    shove.rot("head", (0, [0, 0, 0]), (0.7, [10, 0, 0], E_OUT), (0.85, [-5, 0, 0], E_SNAP), (1.9, [0, 0, 0], E_IO))

    hurl = attack("hurl", 3.0)
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        hurl.rot(side, (0, [0, 0, 0]), (1.2, [-150, 0, 25 * s], E_OUT), (2.0, [-155, 0, 30 * s]), (2.3, [-70, 0, 10 * s], E_SNAP),
                 (3.0, [0, 0, 0], E_IO))
    hurl.rot("head", (0, [0, 0, 0]), (1.2, [-20, 0, 0], E_OUT), (2.0, [-25, 0, 0]), (2.3, [5, 0, 0], E_SNAP), (3.0, [0, 0, 0], E_IO))

    drag = attack("drag", 2.5)
    drag.rot("right_arm", (0, [0, 0, 0]), (1.0, [-90, 10, 0], E_OUT), (1.1, [-85, 10, 0]), (1.3, [-40, -10, 20], E_SNAP),
             (1.5, [-70, 40, -30], E_SNAP), (2.5, [0, 0, 0], E_IO))
    drag.rot("body", (0, [0, 0, 0]), (1.0, [0, -10, 0], E_OUT), (1.3, [-8, 15, 0], E_SNAP), (1.5, [5, -25, 0], E_SNAP),
             (2.5, [0, 0, 0], E_IO))

    gaze = attack("gaze", 2.6)
    gaze.rot("head", (0, [0, 0, 0]), (1.0, [-8, 0, 14], E_OUT), (1.5, [-12, 0, 18]), (1.6, [6, 0, 0], E_SNAP), (2.6, [0, 0, 0], E_IO))
    gaze.rot("body", (0, [0, 0, 0]), (1.0, [-6, 0, 0], E_OUT), (1.6, [4, 0, 0], E_SNAP), (2.6, [0, 0, 0], E_IO))
    gaze.rot("right_arm", (0, [0, 0, 0]), (1.0, [0, 0, 12], E_OUT), (2.6, [0, 0, 0], E_IO))
    gaze.rot("left_arm", (0, [0, 0, 0]), (1.0, [0, 0, -12], E_OUT), (2.6, [0, 0, 0], E_IO))

    fire = attack("fire", 3.5)
    fire.rot("right_arm", (0, [0, 0, 0]), (1.2, [-175, 0, 10], E_OUT), (1.4, [-170, 0, 10]), (1.55, [-20, 0, 10], E_SNAP),
             (2.8, [-20, 0, 10]), (3.5, [0, 0, 0], E_IO))
    fire.rot("head", (0, [0, 0, 0]), (1.2, [-35, 0, 0], E_OUT), (1.55, [10, 0, 0], E_SNAP), (3.5, [0, 0, 0], E_IO))
    fire.rot("body", (0, [0, 0, 0]), (1.2, [-8, 0, 0], E_OUT), (1.55, [12, 0, 0], E_SNAP), (2.8, [8, 0, 0]), (3.5, [0, 0, 0], E_IO))

    possess = attack("possess", 3.0)
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        possess.rot(side, (0, [0, 0, 0]), (1.3, [-50, 0, 60 * s], E_OUT), (2.0, [-55, 0, 70 * s]), (3.0, [0, 0, 0], E_IO))
    possess.rot("head", (0, [0, 0, 0]), (1.3, [-45, 0, 0], E_OUT), (2.0, [-50, 0, 0]), (3.0, [0, 0, 0], E_IO))
    possess.rot("body", (0, [0, 0, 0]), (1.3, [-12, 0, 0], E_OUT), (3.0, [0, 0, 0], E_IO))

    dash = attack("smoke_dash", 4.4)
    dash.rot("body", (0, [0, 0, 0]), (1.1, [30, 0, 0], E_OUT), (1.2, [35, 0, 0]), (3.2, [35, 0, 0]), (4.4, [0, 0, 0], E_IO))
    dash.rot("head", (0, [0, 0, 0]), (1.1, [-30, 0, 0], E_OUT), (3.2, [-30, 0, 0]), (4.4, [0, 0, 0], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        dash.rot(side, (0, [0, 0, 0]), (1.1, [40, 0, 20 * s], E_OUT), (3.2, [40, 0, 20 * s]), (4.4, [0, 0, 0], E_IO))
    dash.pos("root", (0, [0, 0, 0]), (1.1, [0, -2, 0], E_OUT), (3.2, [0, -2, 0]), (4.4, [0, 0, 0], E_IO))

    blink = attack("blink", 1.1)
    blink.scale("root", (0, [1, 1, 1]), (0.5, [0.1, 1.5, 0.1], E_IN), (0.6, [0.1, 1.5, 0.1]), (1.1, [1, 1, 1], E_BACK))

    # Held in the iron: he strains against it, shoulders jerking, the head thrown about.
    trapped = attack("trapped", 6.0)
    keys_r, keys_l, keys_h = [(0, [0, 0, 0])], [(0, [0, 0, 0])], [(0, [0, 0, 0])]
    for k in range(10):
        t = 0.5 + k * 0.5
        keys_r.append((t, [-30 - (k % 3) * 15, 0, 30 + (k % 2) * 25], E_SNAP))
        keys_l.append((t, [-25 - ((k + 1) % 3) * 15, 0, -30 - ((k + 1) % 2) * 25], E_SNAP))
        keys_h.append((t, [(-20 if k % 2 else 15), (k % 3 - 1) * 20, 0], E_SNAP))
    keys_r.append((6.0, [0, 0, 0], E_IO))
    keys_l.append((6.0, [0, 0, 0], E_IO))
    keys_h.append((6.0, [0, 0, 0], E_IO))
    trapped.rot("right_arm", *keys_r)
    trapped.rot("left_arm", *keys_l)
    trapped.rot("head", *keys_h)
    trapped.rot("body", (0, [0, 0, 0]), (0.4, [15, 0, 0], E_OUT), (5.6, [15, 0, 0]), (6.0, [0, 0, 0], E_IO))
    trapped.pos("body", (0, [0, 0, 0]), (0.4, [0, -1, 0], E_OUT), (5.6, [0, -1, 0]), (6.0, [0, 0, 0], E_IO))

    t2 = f.new(A + "transform_2", 4.0)
    t2.rot("head", (0, [0, 0, 0]), (1.0, [30, 0, 0], E_OUT), (1.8, [30, 0, 0]), (2.2, [-55, 0, 0], E_BACK), (3.4, [-50, 0, 0]),
           (4.0, [0, 0, 0], E_IO))
    t2.rot("body", (0, [0, 0, 0]), (1.0, [25, 0, 0], E_OUT), (1.8, [25, 0, 0]), (2.2, [-15, 0, 0], E_BACK), (4.0, [0, 0, 0], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        t2.rot(side, (0, [0, 0, 0]), (1.0, [-20, 0, -10 * s], E_OUT), (1.8, [-20, 0, -10 * s]), (2.2, [-30, 0, 80 * s], E_BACK),
               (3.4, [-30, 0, 85 * s]), (4.0, [0, 0, 0], E_IO))

    death = f.new(A + "death", 6.0, hold=True)
    death.pos("root", (0, [0, 0, 0]), (1.0, [0, -5, 0], E_IN), (6.0, [0, -5, 0]))
    death.rot("right_leg", (0, [0, 0, 0]), (1.0, [-85, 0, 0], E_OUT), (6.0, [-85, 0, 0]))
    death.rot("left_leg", (0, [0, 0, 0]), (1.0, [10, 0, 0], E_OUT), (6.0, [10, 0, 0]))
    death.rot("head", (0, [0, 0, 0]), (1.0, [20, 0, 0], E_OUT), (2.5, [-70, 0, 0], E_IO), (5.0, [-70, 0, 0]), (6.0, [30, 0, 0], E_IN))
    death.rot("body", (0, [0, 0, 0]), (1.0, [10, 0, 0], E_OUT), (2.5, [-25, 0, 0], E_IO), (5.0, [-25, 0, 0]), (6.0, [40, 0, 0], E_IN))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        death.rot(side, (0, [0, 0, 0]), (2.5, [-30, 0, 90 * s], E_IO), (5.0, [-35, 0, 95 * s]), (6.0, [10, 0, 10 * s], E_IN))
    return f


TRIGGERED = ["emerge", "death", "transform_2", "trapped", "shove", "hurl", "drag", "gaze", "fire", "possess", "smoke_dash", "blink"]


# --- the trophy, the rails, the blood, the smoke ----------------------------------------------

def write_model(name, model):
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + ".json"), "w") as f:
        json.dump(model, f, indent=1)
        f.write("\n")


def trophy():
    """A bust on the trophies' pedestal: dark suit, slicked hair, yellow eyes."""
    suit = Tex(16, 16, 401)
    for y in range(16):
        for x in range(16):
            suit.set(x, y, SUIT[int(round(2.5 + (fbm(x, y, 402, 16, 16, 2, 4.0) - 0.5) * 2))])
    skin = Tex(16, 16, 403)
    for y in range(16):
        for x in range(16):
            skin.set(x, y, SKIN[int(round(3.2 + (fbm(x, y, 404, 16, 16, 2, 4.0) - 0.5) * 1.4))])
    hair = Tex(16, 16, 405)
    for y in range(16):
        for x in range(16):
            hair.set(x, y, HAIR[int(round(1.6 + (fbm(x, y, 406, 16, 16, 2, 3.0) - 0.5) * 2))])
    face = Tex(16, 16, 407)
    for y in range(16):
        for x in range(16):
            face.set(x, y, hair.get(x, y) if y < 4 else skin.get(x, y))
    for x in (4, 5, 10, 11):
        face.set(x, 7, EYE)
    for x in (4, 11):
        face.set(x, 6, HAIR[1])
    face.set(5, 6, HAIR[1])
    face.set(10, 6, HAIR[1])
    for x in (7, 8):
        face.set(x, 9, SKIN[2])
    for x in range(6, 10):
        face.set(x, 11, hexc("#4a2a24"))
    shirt = Tex(16, 16, 408)
    for y in range(16):
        for x in range(16):
            shirt.set(x, y, SHIRT[4] if not (7 <= x <= 8) else TIE[3])
    for name, t in (("suit", suit), ("face", face), ("hair", hair), ("skin", skin), ("shirt", shirt)):
        save(t, "block", "azazel_trophy_" + name)

    def el(name, frm, to, tex, uv=None, per=None):
        faces = {}
        for fc in ("north", "south", "east", "west", "up", "down"):
            faces[fc] = {"uv": uv or [0, 0, 16, 16], "texture": "#" + ((per or {}).get(fc, tex))}
        return {"name": name, "from": frm, "to": to, "faces": faces}
    elements = [
        el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
        el("shoulders", [4, 3, 5.5], [12, 7, 10.5], "suit", [2, 2, 10, 6]),
        el("collar", [7, 5.5, 5.3], [9, 7, 5.5], "shirt", [6, 0, 10, 3]),
        el("neck", [7, 7, 7], [9, 8, 9], "skin", [4, 4, 6, 6]),
        el("head", [5, 8, 5], [11, 14, 11], "skin", [0, 0, 16, 16], {"north": "face", "up": "hair", "south": "hair"}),
    ]
    write_model("azazel_trophy", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                  "textures": {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base",
                                               "suit": "supernaturalcraft:block/azazel_trophy_suit",
                                               "face": "supernaturalcraft:block/azazel_trophy_face",
                                               "hair": "supernaturalcraft:block/azazel_trophy_hair",
                                               "skin": "supernaturalcraft:block/azazel_trophy_skin",
                                               "shirt": "supernaturalcraft:block/azazel_trophy_shirt"},
                                  "elements": elements})


def rail_textures():
    """Samuel Colt's track laid flat: two iron rails on dark ties, cold or glowing with heat and runes."""
    iron = P.STEEL
    hot = [hexc("#7a2a08"), hexc("#d1521a"), hexc("#ff9a26"), hexc("#ffd96a")]
    tie = P.WOOD

    def track(t, along_x, charged, seed):
        rng = random.Random(seed)
        for i in range(16):
            for k in range(2, 14):
                if i % 4 == 1 or i % 4 == 2:
                    x, y = (i, k) if along_x else (k, i)
                    t.set(x, y, tie[1 + rng.randrange(2)])
        for rail in (4, 11):
            for i in range(16):
                x, y = (i, rail) if along_x else (rail, i)
                c = hot[2 if i % 5 else 3] if charged else iron[2 + (i * 7 + rail) % 2]
                t.set(x, y, c)
                x2, y2 = (i, rail + 1) if along_x else (rail + 1, i)
                t.set(x2, y2, hot[1] if charged else iron[1])

    def diagonal(t, charged, seed):
        rng = random.Random(seed)
        # Ties run across the track (x + z constant), every few pixels along it.
        for c in range(1, 31, 4):
            for x in range(16):
                for k in (0, 1):
                    y = c + k - x
                    if 0 <= y < 16 and abs(x - y) <= 5:
                        t.set(x, y, tie[1 + rng.randrange(2)])
        # The two rails run along x = z, either side of the middle.
        for off in (-3, 3):
            for i in range(16):
                for j in (0, 1):
                    x, y = i + off + j, i
                    if 0 <= x < 16 and 0 <= y < 16:
                        t.set(x, y, (hot[2 if i % 5 else 3] if j == 0 else hot[1]) if charged else iron[2 if j == 0 else 1])

    for charged in (False, True):
        suffix = "_charged" if charged else ""
        s = Tex(16, 16, 501)
        track(s, False, charged, 502)
        save(s, "block", "colt_rail_straight" + suffix)
        d = Tex(16, 16, 503)
        diagonal(d, charged, 504)
        save(d, "block", "colt_rail_diagonal" + suffix)
        c = Tex(16, 16, 505)
        track(c, False, charged, 506)
        track(c, True, charged, 507)
        save(c, "block", "colt_rail_cross" + suffix)


def blood():
    t = vial(Ramp("#1a0503", "#360907", "#561009", "#7a1a0b", "#9e2e0d", "#c9561a"), YELLOW[2])
    t.set(7, 10, YELLOW[1])
    return t


def yellow_smoke():
    for i in range(4):
        t = _blob(P.SULFUR, 3.6 - i * 0.5, 600 + i, core=False)
        for y in range(8):
            for x in range(8):
                c = t.rows[y][x]
                if c[3]:
                    t.rows[y][x] = (int(c[0] * 0.85), int(c[1] * 0.75), c[2] // 3, 165)
        save(t, "particle", f"yellow_smoke_{i}")


def generate():
    r, _ = rig()
    r.write(GEO + "azazel.geo.json")
    for phase in (1, 2):
        t, g = paint_phase(phase)
        save(t, "entity", f"azazel_p{phase}")
        save(g, "entity", f"azazel_p{phase}_glowmask")
    a = anims()
    a.write(ANIM + "azazel.animation.json")
    trophy()
    rail_textures()
    save(blood(), "item", "azazel_blood")
    yellow_smoke()


if __name__ == "__main__":
    generate()
