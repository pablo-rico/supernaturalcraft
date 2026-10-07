"""Lilith, the First Demon: GeckoLib geometry, three phase textures (+ glowmasks), every animation, her
trophy bust, the headstones of her arena, the last seal, her whistle and her white light.

  P1 The First Demon     -- a fair-haired woman in a pale dress; white eyes.
  P2 Holder of Contracts -- cracks of white light through the skin.
  P3 The Last Seal       -- the light pours out of her: almost all of her glows.
"""

import json
import os
import random

import palette as P
from animkit import AnimFile
from common import art, save
from geomodel import box
from humanoid import humanoid
from lucifer_art import crack_field
from paint import cloth, noise, paint_cube, set_face_px, stamp_face
from particles import _blob
from pixelkit import Ramp, Tex, fbm, hexc, mix
from azazel_art import ANIM, GEO, MODELS, write_model

SKIN = Ramp("#7a5a4c", "#a47e6c", "#c49d88", "#d9b6a0", "#e8cbb6", "#f3ddcc")
HAIR = Ramp("#6b5228", "#8f7036", "#b39047", "#cdac5e", "#e0c77d", "#efdea0")
DRESS = Ramp("#8f8b84", "#b2ada4", "#cbc6bc", "#ddd8cf", "#ebe7df", "#f7f4ee")
SHOE = Ramp("#2a2420", "#3a322c", "#4a4038", "#5b4f45", "#6c5f53", "#7d6f62")
LIGHT = [hexc("#d8e6ff"), hexc("#f2f6ff"), hexc("#ffffff")]
EYE = hexc("#ffffff")


# --- geometry -------------------------------------------------------------------------------

def rig():
    r, p = humanoid("lilith", 128, 128, slim_arms=True)
    head, body = r.get("head"), r.get("body")
    p["hair"] = box(head, (-4, 30, -4), (8, 2, 8), inflate=0.35, tag="hair")
    p["hair_back"] = box(head, (-4, 18, 2.2), (8, 12, 2), inflate=0.2, tag="hair_back")
    p["dress"] = box(body, (-4, 12, -2), (8, 12, 4), inflate=0.3, tag="dress")
    p["skirt_r"] = box(r.get("right_leg"), (-4.1, 3, -2), (4, 9, 4), inflate=0.55, tag="skirt")
    p["skirt_l"] = box(r.get("left_leg"), (0.1, 3, -2), (4, 9, 4), inflate=0.55, tag="skirt")
    r.pack()
    return r, p


# --- texture --------------------------------------------------------------------------------

class Painter:
    def __init__(self, rig_, parts, phase):
        self.r, self.p, self.phase = rig_, parts, phase
        self.t = Tex(rig_.tex_w, rig_.tex_h, 90 + phase)
        self.g = Tex(rig_.tex_w, rig_.tex_h, 95 + phase)

    def glow(self, cube, face, x, y, c):
        set_face_px(self.g, cube, face, x, y, c)
        set_face_px(self.t, cube, face, x, y, c)

    def cracks(self, cube, density, seed, faces=None):
        rng = random.Random(seed)
        for face, (u, v, w, h) in cube.rects.items():
            if faces and face not in faces:
                continue
            for (x, y) in crack_field(seed + u * 7 + v, w, h, density):
                self.glow(cube, face, x, y, LIGHT[1] if rng.random() < 0.7 else LIGHT[2])

    def paint(self):
        p, ph = self.p, self.phase
        hair_m, skin_m = noise(HAIR, 31, 3.0, 1.0, 6.0), noise(SKIN, 32, 3.4, 0.4)

        def head(face, x, y, w, h):
            if face == "up" or face == "south" or (face in ("east", "west") and (y < 3 or x > 4)) or (face == "north" and y < 1):
                return hair_m(face, x, y, w, h)
            return skin_m(face, x, y, w, h)
        paint_cube(self.t, p["head"], head)
        paint_cube(self.t, p["hair"], lambda f, x, y, w, h: None if f == "down" or (f == "north" and y > 0) else hair_m(f, x, y, w, h))
        paint_cube(self.t, p["hair_back"], lambda f, x, y, w, h: None if f == "up" else hair_m(f, x, y, w, h))
        self.face()

        dress_m = cloth(DRESS, 33, 3.0, 0.25)
        paint_cube(self.t, p["body"], skin_m)
        paint_cube(self.t, p["dress"], lambda f, x, y, w, h: None if f == "down" or (f == "up" and 2 <= x <= 5)
                   or (f == "north" and y == 0 and 2 <= x <= 5) else dress_m(f, x, y, w, h))
        arm_skin = noise(SKIN, 34, 3.4, 0.4)
        for side in ("right_arm", "left_arm"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, s=arm_skin: s(f, x, y, w, h))
        shoes = noise(SHOE, 35, 2.2, 0.5)
        for side in ("right_leg", "left_leg"):
            paint_cube(self.t, p[side], lambda f, x, y, w, h, s=arm_skin, b=shoes:
                       b(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 2)) else s(f, x, y, w, h))
        for skirt in ("skirt_r", "skirt_l"):
            def skirt_m(face, x, y, w, h):
                if face in ("up", "down"):
                    return None
                if y == h - 1 and (x * 3 + y) % 4 == 0:
                    return None  # a soft, uneven hem
                return dress_m(face, x, y, w, h)
            paint_cube(self.t, p[skirt], skirt_m)

        self.inner_light({1: 0.30, 2: 0.42, 3: 0.55}[ph])
        if ph >= 2:
            self.vessel_cracks(ph)
        if ph >= 3:
            self.blaze()
        return self.t, self.g

    def inner_light(self, k):
        """She is never quite in the dark: a faint glow of her own, stronger each phase (the glowmask adds it)."""
        for y in range(self.t.h):
            for x in range(self.t.w):
                c = self.t.rows[y][x]
                if c[3] and not self.g.rows[y][x][3]:
                    self.g.rows[y][x] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)

    def face(self):
        pal = {"b": HAIR[2], "n": SKIN[2], "m": hexc("#a8605c"), "l": SKIN[4]}
        stamp_face(self.t, self.p["head"], "north", [
            "........",
            "........",
            ".bb..bb.",
            "........",
            "........",
            "...nn...",
            "..mmmm..",
            "...ll...",
        ], pal)
        for x in (1, 2, 5, 6):
            self.glow(self.p["head"], "north", x, 3, EYE)

    def vessel_cracks(self, ph):
        p = self.p
        density = 1 if ph == 2 else 2
        self.cracks(p["head"], density, 401, faces=("north", "east", "west"))
        self.cracks(p["body"], density, 402)
        self.cracks(p["dress"], density, 403, faces=("north", "south"))
        for side, seed in (("right_arm", 404), ("left_arm", 405)):
            self.cracks(p[side], 1, seed, faces=("north", "east", "west"))

    def blaze(self):
        """Phase 3: the light pours out of the whole dress, pale gold at the edges."""
        for name in ("dress", "skirt_r", "skirt_l"):
            cube = self.p[name]
            for face, (u, v, w, h) in cube.rects.items():
                for y in range(h):
                    for x in range(w):
                        n = fbm(x + u, y + v, 406, 128, 128, 3, 5.0)
                        if n > 0.52:
                            set_face_px(self.g, cube, face, x, y, mix(LIGHT[0], LIGHT[2], min(1.0, (n - 0.52) * 3)))


def paint_phase(phase):
    r, p = rig()
    return Painter(r, p, phase).paint()


# --- animations -----------------------------------------------------------------------------

E_IO = "easeInOutSine"
E_OUT = "easeOutQuad"
E_IN = "easeInQuad"
E_BACK = "easeOutBack"
E_SNAP = "easeInQuart"
A = "animation.lilith."


def anims():
    f = AnimFile()
    idle = f.new(A + "idle", 4.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (2.0, [0, -0.15, 0], E_IO), (4.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [0, 0, -5]), (2.0, [-3, -8, -7], E_IO), (4.0, [0, 0, -5], E_IO))
    idle.rot("right_arm", (0, [0, 0, 6]), (2.0, [-3, 0, 8], E_IO), (4.0, [0, 0, 6], E_IO))
    idle.rot("left_arm", (0, [0, 0, -6]), (2.0, [-3, 0, -8], E_IO), (4.0, [0, 0, -6], E_IO))

    walk = f.new(A + "walk", 1.4, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [18 * s, 0, 0]), (0.7, [-18 * s, 0, 0], E_IO), (1.4, [18 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        walk.rot(bone, (0, [8 * s, 0, 0]), (0.7, [-8 * s, 0, 0], E_IO), (1.4, [8 * s, 0, 0], E_IO))

    for name in ("idle_fly", "wings_idle", "wings_fly"):
        f.new(A + name, 1.0, loop=True).const_rot("root", [0, 0, 0])

    emerge = f.new(A + "emerge", 4.0, hold=True)
    emerge.scale("root", (0, [0.05, 1.3, 0.05]), (0.4, [1.1, 1.05, 1.1], E_BACK), (0.8, [1, 1, 1], E_IO), (4.0, [1, 1, 1]))
    emerge.rot("head", (0, [-40, 0, 0]), (1.5, [-35, 0, 0]), (2.6, [10, 0, -8], E_BACK), (4.0, [0, 0, -5], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        emerge.rot(side, (0, [-20, 0, 70 * s]), (1.5, [-20, 0, 75 * s]), (2.6, [0, 0, 8 * s], E_IO), (4.0, [0, 0, 6 * s], E_IO))

    def attack(name, length):
        return f.new(A + name, length)

    contract = attack("contract", 2.0)
    contract.rot("right_arm", (0, [0, 0, 0]), (0.8, [-95, -15, 0], E_OUT), (1.0, [-90, 10, 0]), (1.1, [-80, -20, 0], E_SNAP),
                 (1.2, [-90, 15, 0], E_SNAP), (2.0, [0, 0, 0], E_IO))
    contract.rot("head", (0, [0, 0, 0]), (0.8, [5, 10, -6], E_OUT), (2.0, [0, 0, -5], E_IO))

    light = attack("white_light", 6.0)
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        light.rot(side, (0, [0, 0, 0]), (2.0, [-30, 0, 90 * s], E_OUT), (2.5, [-35, 0, 100 * s]), (2.6, [-50, 0, 120 * s], E_SNAP),
                  (4.6, [-50, 0, 120 * s]), (6.0, [0, 0, 0], E_IO))
    light.rot("head", (0, [0, 0, 0]), (2.0, [-35, 0, 0], E_OUT), (2.6, [-45, 0, 0], E_SNAP), (4.6, [-40, 0, 0]), (6.0, [0, 0, -5], E_IO))
    light.rot("body", (0, [0, 0, 0]), (2.0, [-10, 0, 0], E_OUT), (4.6, [-12, 0, 0]), (6.0, [0, 0, 0], E_IO))
    light.pos("root", (0, [0, 0, 0]), (2.5, [0, 2, 0], E_IO), (4.6, [0, 2, 0]), (6.0, [0, 0, 0], E_IO))

    shards = attack("shards", 2.0)
    shards.rot("right_arm", (0, [0, 0, 0]), (0.9, [-60, 0, 40], E_OUT), (1.05, [-90, 0, -20], E_SNAP), (2.0, [0, 0, 0], E_IO))
    shards.rot("left_arm", (0, [0, 0, 0]), (0.9, [-60, 0, -40], E_OUT), (1.05, [-90, 0, 20], E_SNAP), (2.0, [0, 0, 0], E_IO))

    smother = attack("smother", 2.3)
    smother.rot("right_arm", (0, [0, 0, 0]), (1.0, [-80, 0, 0], E_OUT), (1.2, [-80, 0, 0]), (1.3, [-60, 0, 0], E_SNAP), (2.3, [0, 0, 0], E_IO))
    smother.rot("head", (0, [0, 0, 0]), (1.0, [8, 0, 10], E_OUT), (2.3, [0, 0, -5], E_IO))

    rend = attack("rend", 2.6)
    rend.rot("body", (0, [0, 0, 0]), (0.7, [20, 0, 0], E_OUT), (1.1, [10, 25, 0], E_SNAP), (1.45, [10, -25, 0], E_SNAP), (2.6, [0, 0, 0], E_IO))
    rend.rot("right_arm", (0, [0, 0, 0]), (0.7, [30, 0, 10]), (1.1, [-110, 0, 30], E_SNAP), (1.45, [-30, 0, 0], E_SNAP), (2.6, [0, 0, 0], E_IO))
    rend.rot("left_arm", (0, [0, 0, 0]), (0.7, [30, 0, -10]), (1.1, [-30, 0, 0]), (1.45, [-110, 0, -30], E_SNAP), (2.6, [0, 0, 0], E_IO))

    flash = attack("flash", 2.1)
    flash.rot("head", (0, [0, 0, 0]), (1.0, [12, 0, 0], E_OUT), (1.1, [-15, 0, 0], E_SNAP), (2.1, [0, 0, -5], E_IO))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        flash.rot(side, (0, [0, 0, 0]), (1.0, [-40, 0, 20 * s], E_OUT), (1.1, [-10, 0, 60 * s], E_SNAP), (2.1, [0, 0, 0], E_IO))

    hounds = attack("hounds", 3.0)
    hounds.rot("right_arm", (0, [0, 0, 0]), (1.2, [-30, 0, 80], E_OUT), (1.5, [-10, 0, 40], E_SNAP), (3.0, [0, 0, 0], E_IO))
    hounds.rot("head", (0, [0, 0, 0]), (1.2, [15, 0, 15], E_OUT), (3.0, [0, 0, -5], E_IO))

    judgement = attack("judgement", 4.5)
    judgement.rot("right_arm", (0, [0, 0, 0]), (1.5, [-175, 0, 0], E_OUT), (3.5, [-175, 0, 0]), (4.5, [0, 0, 0], E_IO))
    judgement.rot("left_arm", (0, [0, 0, 0]), (1.5, [-175, 0, 0], E_OUT), (3.5, [-175, 0, 0]), (4.5, [0, 0, 0], E_IO))
    judgement.rot("head", (0, [0, 0, 0]), (1.5, [-40, 0, 0], E_OUT), (3.5, [-40, 0, 0]), (4.5, [0, 0, -5], E_IO))

    tp = attack("teleport", 1.1)
    tp.scale("root", (0, [1, 1, 1]), (0.5, [0.1, 1.5, 0.1], E_IN), (0.6, [0.1, 1.5, 0.1]), (1.1, [1, 1, 1], E_BACK))

    for n, tilt in ((2, 30), (3, 45)):
        t = f.new(A + f"transform_{n}", 4.0)
        t.rot("head", (0, [0, 0, 0]), (1.0, [25, 0, 0], E_OUT), (1.8, [25, 0, 0]), (2.2, [-tilt - 15, 0, 0], E_BACK), (3.4, [-tilt, 0, 0]),
              (4.0, [0, 0, -5], E_IO))
        for side, s in (("right_arm", 1), ("left_arm", -1)):
            t.rot(side, (0, [0, 0, 0]), (1.0, [-10, 0, -5 * s], E_OUT), (1.8, [-10, 0, -5 * s]), (2.2, [-40, 0, (70 + tilt) * s], E_BACK),
                  (3.4, [-40, 0, (75 + tilt) * s]), (4.0, [0, 0, 6 * s], E_IO))
        t.pos("root", (0, [0, 0, 0]), (2.2, [0, n - 1, 0], E_OUT), (3.4, [0, n - 1, 0]), (4.0, [0, 0, 0], E_IO))

    death = f.new(A + "death", 6.0, hold=True)
    death.pos("root", (0, [0, 0, 0]), (2.0, [0, 1.5, 0], E_OUT), (4.0, [0, 1.5, 0]), (5.0, [0, -5, 0], E_IN), (6.0, [0, -5, 0]))
    death.rot("head", (0, [0, 0, 0]), (2.0, [-60, 0, 0], E_OUT), (4.0, [-65, 0, 0]), (5.0, [30, 0, 0], E_IN), (6.0, [30, 0, 0]))
    death.rot("body", (0, [0, 0, 0]), (2.0, [-15, 0, 0], E_OUT), (4.0, [-15, 0, 0]), (5.0, [40, 0, 0], E_IN), (6.0, [40, 0, 0]))
    for side, s in (("right_arm", 1), ("left_arm", -1)):
        death.rot(side, (0, [0, 0, 0]), (2.0, [-30, 0, 100 * s], E_OUT), (4.0, [-35, 0, 110 * s]), (5.0, [10, 0, 10 * s], E_IN), (6.0, [10, 0, 10 * s]))
    death.rot("right_leg", (0, [0, 0, 0]), (4.0, [0, 0, 0]), (5.0, [-85, 0, 0], E_IN), (6.0, [-85, 0, 0]))
    death.rot("left_leg", (0, [0, 0, 0]), (4.0, [0, 0, 0]), (5.0, [-80, 0, 0], E_IN), (6.0, [-80, 0, 0]))
    return f


# --- the trophy, the headstones, the seal, the whistle, the light --------------------------------

def _tex(ramp, seed, base, amp, scale=4.0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            t.set(x, y, ramp[max(0, min(5, int(round(base + (fbm(x, y, seed + 1, 16, 16, 2, scale) - 0.5) * amp))))])
    return t


def trophy():
    """A bust on the trophies' pedestal: fair hair, pale dress, white eyes."""
    dress, skin, hair = _tex(DRESS, 701, 3.5, 1.4), _tex(SKIN, 703, 3.4, 1.0), _tex(HAIR, 705, 3.0, 2.0)
    face = Tex(16, 16, 707)
    for y in range(16):
        for x in range(16):
            face.set(x, y, hair.get(x, y) if (y < 4 or x < 2 or x > 13) else skin.get(x, y))
    for x in (4, 5, 10, 11):
        face.set(x, 7, EYE)
    for x in (7, 8):
        face.set(x, 9, SKIN[2])
    for x in range(6, 10):
        face.set(x, 11, hexc("#a8605c"))
    for name, t in (("dress", dress), ("face", face), ("hair", hair), ("skin", skin)):
        save(t, "block", "lilith_trophy_" + name)

    def el(name, frm, to, tex, uv=None, per=None):
        faces = {fc: {"uv": uv or [0, 0, 16, 16], "texture": "#" + ((per or {}).get(fc, tex))}
                 for fc in ("north", "south", "east", "west", "up", "down")}
        return {"name": name, "from": frm, "to": to, "faces": faces}
    elements = [
        el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
        el("shoulders", [4.5, 3, 6], [11.5, 7, 10], "dress", [2, 2, 9, 6]),
        el("neck", [7, 7, 7], [9, 8, 9], "skin", [4, 4, 6, 6]),
        el("head", [5, 8, 5], [11, 14, 11], "skin", [0, 0, 16, 16], {"north": "face", "up": "hair", "south": "hair", "east": "hair", "west": "hair"}),
        el("hair_back", [5, 5, 10], [11, 12, 11.5], "hair", [0, 0, 6, 7]),
    ]
    write_model("lilith_trophy", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                  "textures": {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base",
                                               "dress": "supernaturalcraft:block/lilith_trophy_dress", "face": "supernaturalcraft:block/lilith_trophy_face",
                                               "hair": "supernaturalcraft:block/lilith_trophy_hair", "skin": "supernaturalcraft:block/lilith_trophy_skin"},
                                  "elements": elements})


def headstones():
    """Weathered grey headstones, two blocks tall; three stages of cracking, the cracks lit faintly white."""
    for cracks in range(3):
        stone = _tex(P.STONE, 801, 2.6, 1.6, 3.0)
        rng = random.Random(810 + cracks)
        for _ in range(cracks * 3):
            x, y = rng.randrange(2, 14), rng.randrange(0, 16)
            for _ in range(rng.randint(4, 8)):
                if 0 <= x < 16 and 0 <= y < 16:
                    stone.set(x, y, P.STONE[0])
                x += rng.choice((-1, 0, 1))
                y += 1
        top = Tex(16, 16, 820)
        for y in range(16):
            for x in range(16):
                top.set(x, y, stone.get(x, y))
        # A cross carved in the upper half's face.
        for y in range(3, 11):
            top.set(7, y, P.STONE[1])
            top.set(8, y, P.STONE[1])
        for x in range(5, 11):
            top.set(x, 5, P.STONE[1])
        save(stone, "block", f"cracked_headstone_{cracks}")
        save(top, "block", f"cracked_headstone_top_{cracks}")
        for half, tex, (frm, to) in (("lower", f"cracked_headstone_{cracks}", ([1, 0, 5], [15, 16, 11])),
                                     ("upper", f"cracked_headstone_top_{cracks}", ([2, 0, 5], [14, 13, 11]))):
            faces = {}
            for fc in ("north", "south", "east", "west", "up", "down"):
                faces[fc] = {"uv": [frm[0], 16 - to[1], to[0], 16 - frm[1]] if fc in ("north", "south") else [5, 0, 11, to[1] - frm[1]],
                             "texture": "#stone"}
            write_model(f"cracked_headstone_{half}_{cracks}", {
                "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                "textures": {"particle": f"supernaturalcraft:block/cracked_headstone_{cracks}", "stone": f"supernaturalcraft:block/{tex}"},
                "elements": [{"from": frm, "to": to, "faces": faces}]})


def last_seal():
    pal = {"o": P.OUTLINE, "w": hexc("#f4f1ea"), "g": hexc("#cfc9bd"), "s": hexc("#9b958a"), "r": hexc("#7a0d10"),
           "R": hexc("#b3171c"), "l": LIGHT[2], "c": hexc("#3b2a1c")}
    return art([
        "................",
        "......oooo......",
        "....oogwwgoo....",
        "...ogwwwwwwgo...",
        "..ogwwRRRRwwgo..",
        "..owwRrllrRwwo..",
        ".ogwRrlwwlrRwgo.",
        ".owwRlwccwlRwwo.",
        ".owwRlwccwlRwwo.",
        ".ogwRrlwwlrRwgo.",
        "..owwRrllrRwwo..",
        "..ogwwRRRRwwgo..",
        "...ogwwwwwwgo...",
        "....oogsssoo....",
        "......oooo......",
        "................",
    ], pal)


def whistle():
    pal = {"o": P.OUTLINE, "b": hexc("#d8cfbd"), "B": hexc("#f0e8d6"), "d": hexc("#a39780"), "r": hexc("#7a0d10"), "k": hexc("#2a1a12")}
    return art([
        "................",
        "................",
        "............oo..",
        "..........oorko.",
        ".........orrooo.",
        "........obo.....",
        ".......oBbo.....",
        "......oBbdo.....",
        ".....oBbdo......",
        "....oBbdo.......",
        "...oBbbdo.......",
        "..oBbkbdo.......",
        "..obbbddo.......",
        "...oddoo........",
        "....oo..........",
        "................",
    ], pal)


def white_light():
    for i in range(4):
        t = _blob(Ramp("#9fb4d8", "#c4d4f0", "#dfe8fb", "#eef3ff", "#f8faff", "#ffffff"), 3.4 - i * 0.6, 900 + i, core=True)
        for y in range(8):
            for x in range(8):
                c = t.rows[y][x]
                if c[3]:
                    t.rows[y][x] = (c[0], c[1], c[2], 200)
        save(t, "particle", f"white_light_{i}")


def generate():
    r, _ = rig()
    r.write(GEO + "lilith.geo.json")
    for phase in (1, 2, 3):
        t, g = paint_phase(phase)
        save(t, "entity", f"lilith_p{phase}")
        save(g, "entity", f"lilith_p{phase}_glowmask")
    anims().write(ANIM + "lilith.animation.json")
    trophy()
    headstones()
    save(last_seal(), "item", "last_seal")
    save(whistle(), "item", "hound_whistle")
    white_light()


if __name__ == "__main__":
    generate()
