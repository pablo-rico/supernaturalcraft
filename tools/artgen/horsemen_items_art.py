"""The Horsemen's smaller art (v0.11): War's standard (GeckoLib), the four trophies (block models on the trophies'
pedestal, banded in each ring's colour), the antidote vial, the `plague` effect icon and the fly / plague_spore /
soul_wisp particles. Spawn eggs use vanilla's template model (datagen)."""

import math
import random

from animkit import AnimFile
from azazel_art import write_model
from chuck_art import Model, h01, none_on, solid
from common import ASSETS, art, save
from hell_art import DEATH_GEM, FAMINE_GEM, PESTILENCE_GEM, WAR_GEM
from horsemen_art import Clip, glow_faint
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

import death_art
import famine_art
import pestilence_art
import war_art

GEO = ASSETS + "/geo/entity/"
ANIM = ASSETS + "/animations/entity/"
IRON = Ramp("#0e0e10", "#1c1c20", "#2c2c32", "#3e3e46", "#55555e", "#70707a")
BANNER = Ramp("#2a0405", "#480709", "#650c0e", "#821416", "#9c1f1f", "#b83030")
O = hexc("#14100f")


# --- War's standard ---------------------------------------------------------------------------------

def standard():
    m = Model("war_standard", 128, 128)
    m.bone("root", (0, 0, 0))
    m.bone("pole", (0, 0, 0), "root")
    m.bone("banner", (0, 37, 0), "pole")
    m.bone("banner_tail", (0, 25, 0), "banner")
    iron = lambda f, x, y, w, h: IRON[2.8 + (1.2 if f in ("north", "west") and x == 0 else 0) + (fbm(x, y, 3601, 32, 64, 2, 8.0) - 0.5)]
    g = glow_faint(0.08)
    m.cube("pole", (-0.75, 0, -0.75), (1.5, 40, 1.5), iron, glow=g, density=2, tag="pole")
    m.cube("pole", (-1.25, 40, -1.25), (2.5, 1, 2.5), iron, glow=g, density=2, tag="collar")
    m.cube("pole", (-0.5, 41, -0.5), (1, 4, 1), iron, glow=g, density=2, tag="spike", rotation=(0, 45, 0), pivot=(0, 41, 0))
    m.cube("pole", (-0.5, 37, -7), (1, 1, 14), iron, glow=g, density=2, tag="crossbar")
    for z in (-7.5, 6.5):
        m.cube("pole", (-0.5, 36.5, z), (1, 2, 1), iron, glow=g, density=2, tag="finial")
    rng = random.Random(3602)
    for i in range(6):
        a = i * math.pi / 3 + rng.uniform(-0.3, 0.3)
        m.cube("pole", (math.cos(a) * 2.2 - 1, 0, math.sin(a) * 2.2 - 1), (2, 1 + rng.random() * 1.5, 2),
               lambda f, x, y, w, h: shade(hexc("#4a4646"), 0.8 + 0.4 * h01("st", x, y, f)), glow=g, density=2, tag="stone")

    # The banner: blood red, a black sword-and-crown sigil, the bottom torn into tongues and holes.
    def cloth(face, x, y, w, h, top=True):
        n = fbm(x * 2, y * 2 + len(face), 3603, 64, 64, 2, 9.0)
        c = BANNER[2.7 + (n - 0.5) * 1.6 + (0.4 if (x + y) % 5 == 0 else 0)]
        if face in ("east", "west"):
            fx = x if face == "west" else w - 1 - x  # 0 at the north end
            yy = y if top else y + 24
            if top and y < 2:
                return BANNER[1]
            # The sigil: a sword point-down crossed by a bar, in black.
            ring = abs(math.hypot(fx - 13, yy - 13) - 7.5) < 1.0
            blade = 12 <= fx <= 14 and 4 <= yy <= 34 - max(0, yy - 30)
            guard = 7 <= fx <= 19 and 9 <= yy <= 10
            if ring or blade or guard:
                return mix(c, hexc("#0a0303"), 0.85)
            if not top and h01("tear", fx // 3) < 0.75 and y >= h - (2 + int(16 * h01("len", fx // 3))):
                return None
            if h01("hole", face, x // 2, yy // 2) < 0.04:
                return None
        return c
    m.cube("banner", (-0.25, 25, -6.5), (0.5, 12, 13), lambda f, x, y, w, h: cloth(f, x, y, w, h, True), glow=g, density=2, tag="cloth_top")
    m.cube("banner_tail", (-0.25, 11, -6.5), (0.5, 14, 13), lambda f, x, y, w, h: cloth(f, x, y, w, h, False), glow=g, density=2,
           tag="cloth_tail")
    t, gl = m.build(gutter=1, seed=3604)
    f = AnimFile()
    c = Clip(f, "animation.war_standard.idle", 3.0, loop=True)
    for i in range(13):
        t_ = 3.0 * i / 12
        k = math.sin(2 * math.pi * t_ / 3.0)
        k2 = math.sin(4 * math.pi * t_ / 3.0 + 0.7)
        c.key(t_, {"banner": [0, 0, 6 * k + 2 * k2], "banner_tail": [0, 8 * k2, 10 * math.sin(2 * math.pi * t_ / 3.0 - 0.9)]})
    c.done()
    return m, t, gl, f


# --- trophies ---------------------------------------------------------------------------------------

def _fill(ramp, seed, base, amp, scale=4.0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            t.set(x, y, ramp[base + (fbm(x, y, seed + 1, 16, 16, 2, scale) - 0.5) * amp])
    return t


def _face(skin, hair_rows, legend, seed):
    """A 16x16 face for a 6-px trophy head: the Horseman's own face rows, downsampled by taking every texel."""
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            ch = hair_rows[y][x]
            v = legend.get(ch, legend["s"])
            t.set(x, y, v if not callable(v) else v("north", x, y, 16, 16))
    return t


def trophies():
    specs = {
        "war": (war_art, war_art.SUIT, war_art.SKIN, war_art.HAIR, WAR_GEM, "sword"),
        "famine": (famine_art, famine_art.KNIT, famine_art.SKIN, famine_art.WISP, FAMINE_GEM, "scales"),
        "pestilence": (pestilence_art, pestilence_art.TWEED, pestilence_art.SKIN, pestilence_art.HAIR, PESTILENCE_GEM, "cane"),
        "death": (death_art, death_art.COAT, death_art.SKIN, death_art.HAIR, DEATH_GEM, "scythe"),
    }
    for name, (mod, cloth_r, skin_r, hair_r, gem, emblem) in specs.items():
        cloth_t = _fill(cloth_r, 3700 + len(name), 2.8, 1.2)
        skin_t = _fill(skin_r, 3710 + len(name), 3.2, 0.8)
        hair_t = _fill(hair_r, 3720 + len(name), 2.6, 1.6, 6.0)
        if name in ("famine",):
            hair_t = skin_t
        gem_t = _fill(gem, 3730 + len(name), 3.0, 2.0, 6.0)
        for x in range(16):
            gem_t.set(x, 0, gem[5])
        face = Tex(16, 16, 3740)
        rows = mod.FACE
        sk = lambda f, x, y, w, h: skin_r[3]
        for y in range(16):
            for x in range(16):
                ch = rows[y][x]
                col = {"h": hair_t.get(x, y), "c": hair_t.get(x, y), "w": hexc("#e6ddd2"), "p": hexc("#101010"), "i": hexc("#202020"),
                       "b": hair_r[1], "B": hair_r[4], "m": hexc("#4a2a26"), "g": hexc("#c8b070"), "d": shade(skin_r[2], 0.75),
                       "u": skin_r[2], "S": mix(skin_r[3], hair_r[1], 0.4), "R": mix(skin_r[3], hexc("#c45050"), 0.5),
                       "o": hexc("#8a3a26"), "l": hexc("#8a6248"), "N": skin_r[1], "n": skin_r[2]}.get(ch, skin_t.get(x, y))
                face.set(x, y, col)
        metal = _fill(Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff"), 3750, 3.2, 1.4)
        dark = _fill(Ramp("#060607", "#0d0d10", "#151519", "#1e1e23", "#28282e", "#33333a"), 3751, 2.6, 1.2)
        for tname, t in (("cloth", cloth_t), ("face", face), ("hair", hair_t), ("skin", skin_t), ("gem", gem_t), ("metal", metal),
                         ("dark", dark)):
            save(t, "block", f"{name}_trophy_{tname}")

        def el(n, frm, to, tex, uv=None, per=None, rot=None):
            faces = {fc: {"uv": uv or [0, 0, 16, 16], "texture": "#" + ((per or {}).get(fc, tex))}
                     for fc in ("north", "south", "east", "west", "up", "down")}
            e = {"name": n, "from": frm, "to": to, "faces": faces}
            if rot:
                e["rotation"] = rot
            return e
        elements = [
            el("pedestal", [4, 0, 4], [12, 3, 12], "base", [4, 4, 12, 12]),
            el("gem_band", [4.5, 2.6, 4.5], [11.5, 3.4, 11.5], "gem", [0, 0, 7, 1]),
            el("gem", [7.25, 0.8, 3.7], [8.75, 2.2, 4.1], "gem", [6, 6, 8, 8]),
            el("shoulders", [4.5, 3, 6], [11.5, 7, 10], "cloth", [2, 2, 9, 6]),
            el("neck", [7, 7, 7], [9, 8, 9], "skin", [4, 4, 6, 6]),
            el("head", [5, 8, 5], [11, 14, 11], "skin", [0, 0, 16, 16],
               {"north": "face", "up": "hair", "south": "hair", "east": "skin", "west": "skin"}),
        ]
        # The Horseman's emblem behind the bust.
        if emblem == "sword":
            elements += [el("blade", [7.6, 3, 11.2], [8.4, 18, 11.6], "metal", [7, 0, 8, 15]),
                         el("guard", [5.0, 15.5, 11.0], [11.0, 16.3, 11.8], "dark", [0, 0, 6, 1]),
                         el("hilt", [7.5, 16.3, 11.1], [8.5, 19, 11.7], "dark", [0, 0, 1, 3]),
                         el("pommel", [7.3, 19, 10.9], [8.7, 19.8, 11.9], "gem", [0, 0, 2, 1])]
        elif emblem == "scales":
            elements += [el("post", [7.6, 3, 11.2], [8.4, 18, 11.6], "metal", [7, 0, 8, 15]),
                         el("beam", [2.5, 17.5, 11.1], [13.5, 18.1, 11.7], "metal", [0, 0, 11, 1]),
                         el("pan_r", [1.8, 14.5, 10.2], [4.8, 15, 12.6], "metal", [0, 0, 3, 2]),
                         el("pan_l", [11.2, 14.5, 10.2], [14.2, 15, 12.6], "metal", [0, 0, 3, 2]),
                         el("cord_r", [3.1, 15, 11.3], [3.5, 17.5, 11.5], "dark", [0, 0, 1, 2]),
                         el("cord_l", [12.5, 15, 11.3], [12.9, 17.5, 11.5], "dark", [0, 0, 1, 2])]
        elif emblem == "cane":
            elements += [el("cane", [12.0, 3, 7.6], [12.8, 15, 8.4], "dark", [7, 0, 8, 12]),
                         el("crook", [12.0, 14.6, 5.8], [12.8, 15.4, 8.4], "dark", [0, 0, 2, 1]),
                         el("crook_tip", [12.0, 13.4, 5.8], [12.8, 14.6, 6.6], "dark", [0, 0, 1, 1]),
                         el("glasses", [5.5, 11.2, 4.85], [10.5, 11.5, 4.95], "metal", [0, 0, 5, 1])]
        else:
            elements += [el("snath", [7.6, 3, 11.2], [8.4, 20, 11.6], "dark", [7, 0, 8, 16]),
                         el("blade", [1.0, 18.2, 11.15], [8, 19.6, 11.65], "metal", [0, 0, 7, 1],
                            rot={"angle": -22.5, "axis": "z", "origin": [8, 19.5, 11.4]})]
        tex = {"particle": "supernaturalcraft:block/trophy_base", "base": "supernaturalcraft:block/trophy_base"}
        for tname in ("cloth", "face", "hair", "skin", "gem", "metal", "dark"):
            tex[tname] = f"supernaturalcraft:block/{name}_trophy_{tname}"
        write_model(f"{name}_trophy", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                       "textures": tex, "elements": elements})


# --- the antidote, the plague icon, particles -------------------------------------------------------

def antidote_vial():
    """A stoppered phial of clear, bright blue-green cure with a paper label and a red cross."""
    liquid = Ramp("#0c4a4a", "#137070", "#1e9a94", "#3cc4b4", "#7ae6d4", "#c8fff2")
    pal = {"o": O, "C": hexc("#c9b48a"), "c": hexc("#9c845a"), "w": hexc("#efe8d6"), "x": hexc("#c02a24"),
           "G": hexc("#eef8fb"), "a": hexc("#bcd4de"), "R": liquid[5], "r": liquid[3], "m": liquid[2], "D": liquid[1], "s": liquid[4]}
    return art([
        "................",
        "......oooo......",
        "......oCco......",
        "......occo......",
        ".....oooooo.....",
        "......oGao......",
        ".....oGaaao.....",
        "....oGRsrrmo....",
        "...oGRwwwwrmo...",
        "...oGrwxxwrmo...",
        "...oRrwxxwmDo...",
        "...ormwwwwmDo...",
        "...ormmrrmmDo...",
        "....oDmmmmDo....",
        ".....oooooo.....",
        "................",
    ], pal)


def plague_icon():
    """18x18: a sickly green droplet of contagion with dark sores, two flies circling it."""
    K = hexc("#1E1410")
    pal = {"k": K, "g": hexc("#6f9a2a"), "G": hexc("#a8d048"), "h": hexc("#e2f5a0"), "d": hexc("#3e5a14"), "s": hexc("#6a2a1a"),
           "f": hexc("#101010"), "w": hexc("#c8d0d8")}
    return art([
        "..................",
        "...fw.............",
        "..fwf.......k.....",
        "...........kgk....",
        "..........kgGgk...",
        ".........kgGhGgk..",
        "........kgGhhGggk.",
        "........kgGGGgsgk.",
        ".......kggGgggggk.",
        ".......kgsggdggdk.",
        ".......kgggggsgdk.",
        "........kgdgggdk..",
        "........kkgdddkk..",
        "...wf.....kkkk....",
        "..fwf.............",
        "...f..............",
        "..................",
        "..................",
    ], pal)


def particles():
    for i in range(4):
        # fly: a black speck with blurred wings that flick between frames.
        t = Tex(8, 8, 3800 + i)
        t.set(3, 4, hexc("#0c0c0c"))
        t.set(4, 4, hexc("#141414"))
        t.set(4, 3, hexc("#202020"))
        wing = (hexc("#d0d8e0")[:3] + (150,))
        if i % 2 == 0:
            t.set(2, 3, wing)
            t.set(5, 2, wing)
        else:
            t.set(2, 4, wing)
            t.set(5, 4, wing)
        save(t, "particle", f"fly_{i}")
        # plague_spore: a puff of sickly green with darker motes, shrinking.
        t = Tex(8, 8, 3810 + i)
        r = 3.3 - i * 0.6
        rng = random.Random(3811 + i)
        for y in range(8):
            for x in range(8):
                d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
                if d <= r + rng.uniform(-0.5, 0.3):
                    k = 1 - d / (r + 0.5)
                    c = mix(hexc("#4a6a1a"), hexc("#c8e870"), k)
                    if rng.random() < 0.15:
                        c = hexc("#3a3a12")
                    t.set(x, y, c[:3] + (int(120 + 120 * k),))
        save(t, "particle", f"plague_spore_{i}")
        # soul_wisp: a pale blue-white flame-shaped wisp, curling.
        t = Tex(8, 8, 3820 + i)
        for y in range(8):
            for x in range(8):
                cx = 3.5 + math.sin((y + i * 2) * 0.9) * (1.0 - y / 10)
                w = 2.6 * (y / 7) ** 0.7
                d = abs(x + 0.5 - cx - 0.5)
                if d <= w and y > i * 0.4:
                    k = 1 - d / max(0.01, w)
                    c = mix(hexc("#6a8ab8"), hexc("#f2f8ff"), min(1.0, k * 1.2))
                    t.set(x, y, c[:3] + (int(90 + 160 * k),))
        save(t, "particle", f"soul_wisp_{i}")


def generate():
    m, t, g, f = standard()
    m.rig.write(GEO + "war_standard.geo.json")
    save(t, "entity", "war_standard")
    save(g, "entity", "war_standard_glowmask")
    f.write(ANIM + "war_standard.animation.json")
    trophies()
    save(antidote_vial(), "item", "antidote_vial")
    save(plague_icon(), "mob_effect", "plague")
    particles()
