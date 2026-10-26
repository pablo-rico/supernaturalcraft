"""The Chain of Command's rank and file, Hell's side (v0.26): four demons in human vessels, plus the projectile sprites of
the whole rank and file (the sulfur priest's ball, the herald's lance of light, the tormentor's hook).

  demon_brute       a hulking possessed labourer half again a man's height (128x128, its own 1.35x rig): torn work
                    clothes over swollen, veined grey flesh, a chain wrapped round the right fist, black eyes.
  sulfur_priest     a cult priest: a sulfur-stained black cassock and hood with yellow-ochre trim, a brass censer on a
                    chain in the left hand (bone `censer`).
  demon_flayer      a gaunt, stooped vessel (the stoop is the rig's rest pose): a filthy butcher's apron over bare,
                    scarred arms, two long flaying knives grown out of the fists, lank hair.
  demon_tormentor   the Pit's jailer: a broad executioner in a leather hood and apron, rusted chains crossed over the
                    chest, a meat hook in the right hand (locator `hook_hand`: where the thrown chain starts).

The contract (ids, clips, hit ticks, bones) is entity/chain/ChainMobAssets.java. Every rig keeps the humanoid's bone
names (body, head, right_arm, left_arm, right_leg, left_leg); every texture has a `_glowmask` that keeps the black eyes
black in the dark. Clips: idle, walk, trapped (loops), attack, smoke_out (held), and each one's role clips.
"""

import math

import palette as P
from animkit import AnimFile
from common import save
from demon_art import ANIM, GEO, SHOE, hair_cap, trouser_leg
from geomodel import Rig, box
from humanoid import humanoid
from paint import cloth, noise, paint_cube, stamp_face
from pixelkit import Ramp, Tex, fbm, hexc, mix, shade

K = P.VOID
GLINT = hexc("#3b3b44")
CLEAR = (0, 0, 0, 0)
E_IO, E_OUT, E_IN, E_BACK, E_SNAP = "easeInOutSine", "easeOutQuad", "easeInQuad", "easeOutBack", "easeInQuart"

# --- palettes -------------------------------------------------------------------------------------------------
GREY_FLESH = Ramp("#2a272d", "#433f48", "#5b5660", "#716c76", "#88838c", "#a19ca3")
VEIN = hexc("#231d29")
VEIN_HI = hexc("#3e3446")
WORK_SHIRT = Ramp("#1c232b", "#2a343f", "#3b4754", "#4d5a68", "#61707e", "#788795")
CANVAS = Ramp("#22180f", "#352617", "#4a3520", "#5e452b", "#735637", "#896844")
BOOT = Ramp("#0d0907", "#18110c", "#241a12", "#31241a", "#3e2e22", "#4e3b2c")
CHAIN = Ramp("#0e0d0c", "#1d1b19", "#2f2c29", "#46423d", "#5f5a53", "#7b756c")
RUST = Ramp("#200c06", "#3a170b", "#5a2410", "#7a3416", "#9a4a22", "#b86434")
STUBBLE = Ramp("#0d0b0b", "#171414", "#221d1d", "#2d2727", "#3a3232", "#473e3e")

CASSOCK = Ramp("#070607", "#0e0c0d", "#161314", "#1f1b1b", "#2a2424", "#362f2d")
OCHRE = Ramp("#3e2a06", "#664509", "#8f6510", "#b58818", "#d6aa2c", "#ecc94e")
SULFUR_STAIN = hexc("#7a6418")
BRASS = Ramp("#2e1f08", "#56390e", "#7e5716", "#a77a22", "#c99d3a", "#e6c463")
PRIEST_SKIN = Ramp("#3f2d26", "#68493d", "#8a6656", "#a78170", "#c09c8a", "#d4b4a2")

FLAYER_SKIN = Ramp("#3b3532", "#59514c", "#776d66", "#90857d", "#a59a92", "#b9afa7")
SCAR = hexc("#7d3f3a")
SCAR_HI = hexc("#b8857a")
APRON = Ramp("#57534a", "#7a7569", "#9c968a", "#b9b3a6", "#cfcabd", "#e2ded3")
BLOOD = P.BLOOD
GRIME = hexc("#2e261c")
LANK = Ramp("#0f0d0b", "#1a1612", "#25201a", "#302921", "#3c3329", "#493e32")
KNIFE = Ramp("#2a2c30", "#45484e", "#676b72", "#8d9199", "#b4b8bf", "#dde0e4")

LEATHER = Ramp("#120b07", "#21140c", "#321f12", "#442b19", "#573822", "#6c472c")
HOOD = Ramp("#0b0807", "#140f0c", "#1e1712", "#291f18", "#35291f", "#433428")
TORM_SKIN = Ramp("#3a2a22", "#5d4335", "#7c5a48", "#97705b", "#ad866f", "#c29c84")
TROUSER = Ramp("#0f0e0e", "#1a1818", "#252222", "#302c2c", "#3c3737", "#4a4444")


# --- shared painting -------------------------------------------------------------------------------------------

def veined(ramp, seed, base=2.6, amp=0.9, veins=0.022):
    """Swollen flesh: mottled, with dark veins running through it (ridges of the noise)."""
    def m(f, x, y, w, h):
        n = fbm(x * 2 + len(f) * 11, y * 2, seed, 64, 64, 3, 5.0)
        c = ramp[base + (n - 0.5) * amp * 2]
        v = fbm(x * 1.5 + len(f) * 5, y * 1.2, seed + 31, 64, 64, 3, 4.0)
        if abs(v - 0.5) < veins:
            return VEIN if abs(v - 0.5) < veins * 0.5 else mix(c, VEIN_HI, 0.6)
        return c
    return m


def glow_eyes(t, g, cube):
    """Copies the black eyes (and their glint) of a head's front face into the glowmask, so they stay black at night."""
    u, v, w, h = cube.rects["north"]
    for y in range(h):
        for x in range(w):
            c = t.rows[v + y][u + x]
            if c[3] and (c[:3] == K[:3] or c[:3] == GLINT[:3] or c[:3] == shade(K, 1.0)[:3]):
                g.set(u + x, v + y, c)


def chain_links(ramp, horizontal=False):
    """A chain on a thin cube: links alternating face-on (a ring with a dark hole) and edge-on (a bar)."""
    def m(f, x, y, w, h):
        a = x if horizontal else y
        if a % 2 == 0:
            return ramp[4] if (x + y) % 3 else ramp[3]
        return ramp[1] if f in ("north", "south") else ramp[3]
    return m


def boot_leg(trousers, boot, rows=3, laces=False):
    def m(f, x, y, w, h):
        if f == "down" or (f != "up" and y >= h - rows):
            c = boot[3 if f != "down" else 1]
            if f != "up" and y == h - rows:
                c = boot[4]
            if laces and f == "north" and y == h - rows + 1 and x % 2:
                c = boot[1]
            return c
        return trousers(f, x, y, w, h)
    return m


# =============================================================================================================
# demon_brute (128x128): its own rig, 1.35x a man
# =============================================================================================================

def brute_rig():
    r = Rig("demon_brute", 128, 128)
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, 16, 0), "root")
    head = r.bone("head", (0, 31, -1.5), "body")
    ra = r.bone("right_arm", (-9.5, 29, 0), "body")
    la = r.bone("left_arm", (9.5, 29, 0), "body")
    rl = r.bone("right_leg", (-3.3, 16, 0), "root")
    ll = r.bone("left_leg", (3.3, 16, 0), "root")
    p = {
        "body": box(body, (-7, 16, -4), (14, 15, 8), tag="body"),
        "gut": box(body, (-6, 17, -5), (12, 8, 1), tag="gut"),
        "traps": box(body, (-6, 30, -3), (12, 3, 6), tag="traps"),
        "head": box(head, (-4.5, 30.5, -6.5), (9, 9, 9), tag="head"),
        "jaw": box(head, (-4, 30, -7), (8, 3, 2), tag="jaw"),
        "right_arm": box(ra, (-13, 15, -3), (6, 16, 6), tag="right_arm"),
        "left_arm": box(la, (7, 15, -3), (6, 16, 6), tag="left_arm"),
        "right_fist": box(ra, (-13.5, 9, -3.5), (7, 6, 7), tag="fist"),
        "left_fist": box(la, (6.5, 9, -3.5), (7, 6, 7), tag="fist"),
        "chain_wrap": box(ra, (-13.5, 10, -3.5), (7, 4, 7), inflate=0.45, tag="chain_wrap"),
        "chain_end": box(ra, (-11, 2, -0.5), (1, 8, 1), tag="chain_end"),
        "right_leg": box(rl, (-6.4, 0, -3), (6, 16, 6), tag="right_leg"),
        "left_leg": box(ll, (0.4, 0, -3), (6, 16, 6), tag="left_leg"),
        "shirt_r": box(ra, (-13, 25, -3), (6, 6, 6), inflate=0.3, tag="sleeve"),
        "shirt_l": box(la, (7, 25, -3), (6, 6, 6), inflate=0.3, tag="sleeve"),
    }
    r.pack(gutter=1)
    return r, p


def brute_paint():
    r, p = brute_rig()
    t = Tex(128, 128, 2601)
    g = Tex(128, 128, 2602)
    flesh = veined(GREY_FLESH, 2603)
    shirt = cloth(WORK_SHIRT, 2604, 2.6, 0.4)

    def torn(seed, top_rows=0):
        """The work shirt, ripped open down the front and shredded: flesh shows through the tears."""
        def m(f, x, y, w, h):
            if f == "up":
                return shirt(f, x, y, w, h)
            if f == "down":
                return flesh(f, x, y, w, h)
            n = fbm(x * 1.6 + len(f) * 13, y * 1.4, seed, 64, 64, 2, 4.0)
            mid = (w - 1) / 2
            if f == "north" and abs(x - mid) < 1.5 + y * 0.28:
                return flesh(f, x, y, w, h)       # torn open down the chest
            if n > 0.68:
                return flesh(f, x, y, w, h)
            if n > 0.64:
                return WORK_SHIRT[0]               # frayed rim of a tear
            if f in ("north", "south") and y > h - 3 and (x * 5 + seed) % 4 == 0:
                return flesh(f, x, y, w, h)
            c = shirt(f, x, y, w, h)
            if fbm(x + 3, y * 2, seed + 4, 64, 64, 2, 6.0) > 0.7:
                c = mix(c, GRIME, 0.45)            # sweat and dirt
            return c
        return m
    paint_cube(t, p["body"], torn(2605))
    # The bulging gut: flesh through the tear, the shirt's ragged edges either side.
    paint_cube(t, p["gut"], lambda f, x, y, w, h: (WORK_SHIRT[2] if f == "north" and (x < 2 or x > w - 3) else flesh(f, x, y, w, h)))
    paint_cube(t, p["traps"], lambda f, x, y, w, h: shirt(f, x, y, w, h) if f in ("east", "west") else flesh(f, x, y, w, h))
    # A belt of rope round the trousers' top, painted on the body's last rows.
    u, v, w, h = p["body"].rects["north"]
    for x in range(w):
        for f in ("north", "south", "east", "west"):
            uu, vv, ww, hh = p["body"].rects[f]
            if x < ww:
                t.set(uu + x, vv + hh - 1, CANVAS[2] if x % 3 else CANVAS[4])
                t.set(uu + x, vv + hh - 2, CANVAS[1] if (x + 1) % 3 else CANVAS[3])

    # The head: a buzz cut, a heavy brow, swollen veins at the temple, a slack grimacing jaw; black eyes.
    hair = noise(STUBBLE, 2606, 2.4, 1.2, 9.0)

    def head_m(f, x, y, w, h):
        if f == "up" or (f == "south" and y < 4) or (f in ("east", "west") and y < 2):
            return hair(f, x, y, w, h)
        return flesh(f, x, y, w, h)
    paint_cube(t, p["head"], head_m)
    pal = {"h": STUBBLE[2], "H": STUBBLE[4], "s": GREY_FLESH[3], "S": GREY_FLESH[4], "b": GREY_FLESH[1], "B": GREY_FLESH[2],
           "k": K, "g": GLINT, "n": GREY_FLESH[4], "N": GREY_FLESH[1], "v": VEIN, "m": hexc("#1d1214"), "t": hexc("#b4a98a")}
    stamp_face(t, p["head"], "north", [
        "hHhhhhhHh",
        "svsssssss",
        "sBbbsbbBs",
        "bkkgSgkkb",
        "skkkSkkks",
        "sNssnssNs",
        "sNsnnnsNs",
        "sssmmmsss",
        "ssNNNNNss",
    ], pal)
    paint_cube(t, p["jaw"], lambda f, x, y, w, h: (STUBBLE[2] if (x + y) % 2 else GREY_FLESH[2]) if f in ("north", "down")
               else flesh(f, x, y, w, h))
    stamp_face(t, p["jaw"], "north", ["sNmtmmNs", "ssNNNNss", "jsjsjsjs"], {"N": GREY_FLESH[1], "m": hexc("#1d1214"),
                                                                           "t": hexc("#9c916f"), "s": GREY_FLESH[2],
                                                                           "j": STUBBLE[2]})
    glow_eyes(t, g, p["head"])

    # Bare, veined arms; the shirt's torn short sleeves; fists like hams, one wrapped in chain.
    for side in ("right", "left"):
        paint_cube(t, p[f"{side}_arm"], veined(GREY_FLESH, 2610 + (side == "left")))
        knuckles = veined(GREY_FLESH, 2612, 2.8, 0.7)
        paint_cube(t, p[f"{side}_fist"], lambda f, x, y, w, h, kn=knuckles: (GREY_FLESH[1] if f == "down" and x % 2 == 0
                                                                                else kn(f, x, y, w, h)))

        def sleeve(f, x, y, w, h, s=side):
            if f in ("up", "down"):
                return None if f == "down" else shirt(f, x, y, w, h)
            if y >= h - 2 and (x * 7 + y * 3 + (s == "left")) % 3:
                return None                    # ragged hem
            return shirt(f, x, y, w, h)
        paint_cube(t, p[f"shirt_{side[0]}"], sleeve)
    paint_cube(t, p["chain_wrap"], lambda f, x, y, w, h: (None if f in ("up", "down") else
                                                          (CHAIN[4] if (x + 2 * y) % 4 == 0 else CHAIN[2] if (x + 2 * y) % 4 == 1
                                                           else (CHAIN[3] if y % 2 else None))))
    paint_cube(t, p["chain_end"], chain_links(CHAIN))
    for side in ("right", "left"):
        canvas = cloth(CANVAS, 2620 + (side == "left"), 2.5, 0.5)

        def legm(f, x, y, w, h, c=canvas):
            col = c(f, x, y, w, h)
            if f == "north" and 6 <= y <= 9 and 1 <= x <= 4:
                col = mix(col, GRIME, 0.5) if fbm(x, y, 2622, 16, 16, 2, 4) > 0.45 else CANVAS[1]  # a worn patch
            return col
        paint_cube(t, p[f"{side}_leg"], boot_leg(legm, BOOT, 4, laces=True))
    return r, t, g


def brute_anims():
    f = AnimFile()
    a = "animation.demon_brute."
    idle = f.new(a + "idle", 3.2, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (1.6, [0, -0.5, 0], E_IO), (3.2, [0, 0, 0], E_IO))
    idle.rot("body", (0, [6, 0, 0]), (1.6, [9, 0, 0], E_IO), (3.2, [6, 0, 0], E_IO))
    idle.rot("head", (0, [-4, 0, 0]), (1.0, [-6, 10, -3], E_IO), (2.2, [-2, -8, 3], E_IO), (3.2, [-4, 0, 0], E_IO))
    idle.rot("right_arm", (0, [-6, 0, 6]), (1.6, [-2, 0, 9], E_IO), (3.2, [-6, 0, 6], E_IO))
    idle.rot("left_arm", (0, [-6, 0, -6]), (1.6, [-2, 0, -9], E_IO), (3.2, [-6, 0, -6], E_IO))

    walk = f.new(a + "walk", 1.4, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [24 * s, 0, 0]), (0.7, [-24 * s, 0, 0], E_IO), (1.4, [24 * s, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        z = 6 if bone == "right_arm" else -6
        walk.rot(bone, (0, [18 * s, 0, z]), (0.7, [-18 * s, 0, z], E_IO), (1.4, [18 * s, 0, z], E_IO))
    walk.rot("body", (0, [8, 6, 4]), (0.7, [8, -6, -4], E_IO), (1.4, [8, 6, 4], E_IO))
    walk.rot("head", (0, [-6, -4, 0]), (0.7, [-6, 4, 0], E_IO), (1.4, [-6, -4, 0], E_IO))
    walk.pos("body", (0, [0, 0, 0]), (0.35, [0, -0.9, 0], E_IN), (0.7, [0, 0, 0], E_OUT), (1.05, [0, -0.9, 0], E_IN),
             (1.4, [0, 0, 0], E_OUT))

    trapped = f.new(a + "trapped", 0.9, loop=True)
    trapped.rot("right_arm", (0, [-40, 0, 30]), (0.45, [-60, 0, 40], E_IO), (0.9, [-40, 0, 30], E_IO))
    trapped.rot("left_arm", (0, [-60, 0, -40]), (0.45, [-40, 0, -30], E_IO), (0.9, [-60, 0, -40], E_IO))
    trapped.rot("body", (0, [4, -8, 0]), (0.225, [4, 8, 0]), (0.45, [4, -8, 0]), (0.675, [4, 8, 0]), (0.9, [4, -8, 0]))
    trapped.rot("head", (0, [-12, 0, 0]), (0.45, [-20, 0, 0], E_IO), (0.9, [-12, 0, 0], E_IO))

    # attack (hit 0.3 s = 6 ticks): the chained fist raised and hammered down.
    at = f.new(a + "attack", 0.6)
    at.rot("right_arm", (0, [0, 0, 0]), (0.18, [-150, 10, 14], E_OUT), (0.3, [-30, -12, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    at.rot("body", (0, [0, 0, 0]), (0.18, [-6, 20, 0], E_OUT), (0.3, [16, -14, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    at.rot("left_arm", (0, [0, 0, 0]), (0.18, [-30, 0, -14]), (0.6, [0, 0, 0], E_IO))
    at.rot("head", (0, [0, 0, 0]), (0.18, [-10, -10, 0]), (0.3, [10, 8, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))

    smoke = f.new(a + "smoke_out", 1.2, hold=True)
    smoke.rot("head", (0, [0, 0, 0]), (0.4, [-55, 0, 0], E_BACK), (1.2, [-60, 0, 0]))
    smoke.rot("right_arm", (0, [0, 0, 0]), (0.4, [-10, 0, 40], E_BACK), (1.2, [-10, 0, 45]))
    smoke.rot("left_arm", (0, [0, 0, 0]), (0.4, [-10, 0, -40], E_BACK), (1.2, [-10, 0, -45]))
    smoke.rot("body", (0, [0, 0, 0]), (0.4, [-12, 0, 0], E_BACK), (0.8, [-12, 0, 2]), (0.9, [-12, 0, -2]), (1.2, [-14, 0, 0]))

    # charge (loops while charging): head down, shoulders forward, a lumbering run.
    ch = f.new(a + "charge", 0.7, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        ch.rot(bone, (0, [40 * s, 0, 0]), (0.35, [-40 * s, 0, 0], E_IO), (0.7, [40 * s, 0, 0], E_IO))
    ch.rot("body", (0, [34, 4, 0]), (0.35, [34, -4, 0], E_IO), (0.7, [34, 4, 0], E_IO))
    ch.rot("head", (0, [-20, 0, 0]), (0.35, [-18, 0, 0], E_IO), (0.7, [-20, 0, 0], E_IO))
    ch.rot("right_arm", (0, [30, 0, 18]), (0.35, [-10, 0, 14], E_IO), (0.7, [30, 0, 18], E_IO))
    ch.rot("left_arm", (0, [-10, 0, -14]), (0.35, [30, 0, -18], E_IO), (0.7, [-10, 0, -14], E_IO))
    ch.pos("body", (0, [0, -1.5, -1]), (0.175, [0, -0.3, -1], E_OUT), (0.35, [0, -1.5, -1], E_IN), (0.525, [0, -0.3, -1], E_OUT),
           (0.7, [0, -1.5, -1], E_IN))

    # slam (hit 0.7 s = 14 ticks): both fists raised overhead, brought down to the ground.
    sl = f.new(a + "slam", 1.2)
    for bone, z in (("right_arm", 10), ("left_arm", -10)):
        sl.rot(bone, (0, [0, 0, 0]), (0.45, [-175, 0, z], E_OUT), (0.58, [-178, 0, z * 0.6]), (0.7, [-62, 0, z * 0.4], E_SNAP),
               (0.9, [-60, 0, z * 0.4]), (1.2, [0, 0, 0], E_IO))
    sl.rot("body", (0, [0, 0, 0]), (0.45, [-14, 0, 0], E_OUT), (0.58, [-16, 0, 0]), (0.7, [42, 0, 0], E_SNAP), (0.9, [40, 0, 0]),
           (1.2, [0, 0, 0], E_IO))
    sl.rot("head", (0, [0, 0, 0]), (0.45, [-14, 0, 0], E_OUT), (0.7, [-24, 0, 0], E_SNAP), (0.9, [-22, 0, 0]), (1.2, [0, 0, 0], E_IO))
    sl.pos("body", (0, [0, 0, 0]), (0.45, [0, 1.0, 0], E_OUT), (0.7, [0, -2.5, -1.5], E_SNAP), (0.9, [0, -2.4, -1.5]),
           (1.2, [0, 0, 0], E_IO))
    for bone in ("right_leg", "left_leg"):
        sl.rot(bone, (0, [0, 0, 0]), (0.7, [-24, 0, 0], E_SNAP), (0.9, [-24, 0, 0]), (1.2, [0, 0, 0], E_IO))
    return f


# =============================================================================================================
# The humanoid demons (64x64): shared clips
# =============================================================================================================

def demon_anims(prefix, attack_hit, hunch=0.0, walk_len=1.0, swing=24, censer=False):
    """The lesser demons' clips (demon_art.humanoid_anims), with the attack's hit moved to `attack_hit` seconds."""
    f = AnimFile()
    a = f"animation.{prefix}."
    idle = f.new(a + "idle", 3.0, loop=True)
    idle.pos("body", (0, [0, 0, 0]), (1.5, [0, -0.3, 0], E_IO), (3.0, [0, 0, 0], E_IO))
    idle.rot("head", (0, [0, 0, 0]), (1.0, [4, 6, -4], E_IO), (2.2, [-2, -4, 3], E_IO), (3.0, [0, 0, 0], E_IO))
    idle.rot("right_arm", (0, [0, 0, 2]), (1.5, [2, 0, 4], E_IO), (3.0, [0, 0, 2], E_IO))
    if censer:
        idle.rot("left_arm", (0, [-14, 0, -4]), (1.5, [-12, 0, -6], E_IO), (3.0, [-14, 0, -4], E_IO))
        idle.rot("censer", (0, [14, 0, 0]), (0.75, [-14, 0, 0], E_IO), (1.5, [14, 0, 0], E_IO), (2.25, [-14, 0, 0], E_IO),
                 (3.0, [14, 0, 0], E_IO))
    else:
        idle.rot("left_arm", (0, [0, 0, -2]), (1.5, [2, 0, -4], E_IO), (3.0, [0, 0, -2], E_IO))

    walk = f.new(a + "walk", walk_len, loop=True)
    h = walk_len / 2
    for bone, sign in (("right_leg", 1), ("left_leg", -1)):
        walk.rot(bone, (0, [28 * sign, 0, 0]), (h, [-28 * sign, 0, 0], E_IO), (walk_len, [28 * sign, 0, 0], E_IO))
    for bone, sign in (("right_arm", -1), ("left_arm", 1)):
        if censer and bone == "left_arm":
            walk.rot(bone, (0, [-14 + 6 * sign, 0, -4]), (h, [-14 - 6 * sign, 0, -4], E_IO), (walk_len, [-14 + 6 * sign, 0, -4], E_IO))
            continue
        walk.rot(bone, (0, [swing * sign, 0, 0]), (h, [-swing * sign, 0, 0], E_IO), (walk_len, [swing * sign, 0, 0], E_IO))
    if censer:
        walk.rot("censer", (0, [-24, 0, 0]), (h, [24, 0, 0], E_IO), (walk_len, [-24, 0, 0], E_IO))
    walk.pos("body", (0, [0, 0, 0]), (h / 2, [0, 0.6 - hunch, 0], E_OUT), (h, [0, 0, 0], E_IN),
             (h * 1.5, [0, 0.6 - hunch, 0], E_OUT), (walk_len, [0, 0, 0], E_IN))
    walk.rot("body", (0, [0, 4, 0]), (h, [0, -4, 0], E_IO), (walk_len, [0, 4, 0], E_IO))

    t1, t2 = attack_hit * 0.58, attack_hit
    end = attack_hit + 0.28
    attack = f.new(a + "attack", round(end, 2))
    attack.rot("right_arm", (0, [0, 0, 0]), (t1, [-125, 10, 10], E_OUT), (t2, [15, -10, 0], E_SNAP), (end, [0, 0, 0], E_IO))
    attack.rot("body", (0, [0, 0, 0]), (t1, [0, 18, 0], E_OUT), (t2, [6, -16, 0], E_SNAP), (end, [0, 0, 0], E_IO))
    attack.rot("left_arm", (0, [0, 0, 0]), (t1, [-20, 0, -10]), (end, [0, 0, 0], E_IO))

    smoke = f.new(a + "smoke_out", 1.2, hold=True)
    smoke.rot("head", (0, [0, 0, 0]), (0.4, [-55, 0, 0], E_BACK), (1.2, [-60, 0, 0]))
    smoke.rot("right_arm", (0, [0, 0, 0]), (0.4, [-10, 0, 40], E_BACK), (1.2, [-10, 0, 45]))
    smoke.rot("left_arm", (0, [0, 0, 0]), (0.4, [-10, 0, -40], E_BACK), (1.2, [-10, 0, -45]))
    smoke.rot("body", (0, [0, 0, 0]), (0.4, [-12 - hunch * 10, 0, 0], E_BACK), (0.8, [-12 - hunch * 10, 0, 2]),
              (0.9, [-12 - hunch * 10, 0, -2]), (1.2, [-14 - hunch * 10, 0, 0]))

    trapped = f.new(a + "trapped", 0.8, loop=True)
    trapped.rot("right_arm", (0, [-20, 0, 25]), (0.4, [-30, 0, 35], E_IO), (0.8, [-20, 0, 25], E_IO))
    trapped.rot("left_arm", (0, [-20, 0, -25]), (0.4, [-30, 0, -35], E_IO), (0.8, [-20, 0, -25], E_IO))
    trapped.rot("body", (0, [0, -6, 0]), (0.2, [0, 6, 0]), (0.4, [0, -6, 0]), (0.6, [0, 6, 0]), (0.8, [0, -6, 0]))
    trapped.rot("head", (0, [8, 0, 0]), (0.4, [14, 0, 0], E_IO), (0.8, [8, 0, 0], E_IO))
    return f


def replace(f, name, length, loop=False, hold=False):
    f.anims = [x for x in f.anims if x.name != name]
    return f.new(name, length, loop, hold)


# =============================================================================================================
# sulfur_priest
# =============================================================================================================

def priest_rig():
    r, p = humanoid("sulfur_priest")
    head, body = r.get("head"), r.get("body")
    p["hood"] = box(head, (-4, 24, -4), (8, 9, 8), inflate=0.55, tag="hood")
    p["robe"] = box(body, (-4, 1, -2), (8, 11, 4), inflate=0.5, tag="robe")
    la = r.get("left_arm")
    cn = r.bone("censer", (6, 12, -0.5), "left_arm")
    p["censer_chain"] = box(cn, (5.5, 7, -1), (1, 5, 1), tag="censer_chain")
    p["censer_lid"] = box(cn, (4.5, 6, -2), (3, 1, 3), tag="censer_lid")
    p["censer"] = box(cn, (4, 3, -2.5), (4, 3, 4), tag="censer")
    p["sleeve_r"] = box(r.get("right_arm"), (-8, 12, -2), (4, 4, 4), inflate=0.45, tag="sleeve")
    p["sleeve_l"] = box(la, (4, 12, -2), (4, 4, 4), inflate=0.45, tag="sleeve")
    r.pack()
    return r, p


def priest_paint():
    r, p = priest_rig()
    t = Tex(64, 64, 2701)
    g = Tex(64, 64, 2702)
    robe_base = cloth(CASSOCK, 2703, 2.4, 0.3)

    def cassock(seed):
        def m(f, x, y, w, h):
            c = robe_base(f, x, y, w, h)
            n = fbm(x * 2 + len(f) * 7, y * 2, seed, 64, 64, 3, 5.0)
            if n > 0.66:
                c = mix(c, SULFUR_STAIN, 0.25 + 0.9 * (n - 0.66))   # yellow sulfur stains
            return c
        return m
    skin = noise(PRIEST_SKIN, 2704, 2.6, 0.6)
    paint_cube(t, p["head"], skin)
    pal = {"s": PRIEST_SKIN[3], "S": PRIEST_SKIN[4], "d": PRIEST_SKIN[1], "D": PRIEST_SKIN[0], "k": K, "g": GLINT,
           "n": PRIEST_SKIN[4], "m": hexc("#2a1414"), "o": OCHRE[3]}
    stamp_face(t, p["head"], "north", [
        "dddddddd",
        "dsssssDd",
        "dsddsdsd",
        "skkssgks",
        "skgSSkks",
        "sssnnsss",
        "ssmmmmss",
        "ddsssssd",
    ], pal)
    glow_eyes(t, g, p["head"])
    trim = OCHRE

    def hood(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north" and 1 <= x <= w - 2 and y >= 2:
            return None                          # the face opening
        if f == "north" and (x in (0, w - 1) or y == 1):
            return trim[3] if (x + y) % 4 else trim[2]
        if f in ("east", "west") and y >= h - 1:
            return trim[2]
        return cassock(2705)(f, x, y, w, h)
    paint_cube(t, p["hood"], hood)
    stole = cassock(2706)

    def body_m(f, x, y, w, h):
        if f == "north" and x in (3, 4):
            return trim[3] if y % 3 else trim[1]
        if f == "north" and y < 2 and 2 <= x <= 5:
            return trim[2]
        return stole(f, x, y, w, h)
    paint_cube(t, p["body"], body_m)

    def skirt(f, x, y, w, h):
        if f == "up":
            return None
        if f in ("north", "south", "east", "west") and y >= h - 1:
            return trim[2] if (x % 2) else trim[3]
        if f == "north" and x in (3, 4):
            return trim[3] if y % 3 else trim[1]
        if f in ("north", "south", "east", "west") and y >= h - 3 and fbm(x * 3, y, 2707, 32, 32, 2, 6) > 0.55:
            return mix(OCHRE[2], CASSOCK[2], 0.4)   # the hem caked in yellow
        return cassock(2708)(f, x, y, w, h)
    paint_cube(t, p["robe"], skirt)
    # An inverted sigil in ochre thread on the back.
    stamp_face(t, p["body"], "south", ["...tt...", ".t.tt.t.", "..tttt..", "...tt...", "..t..t..", ".t....t."], {"t": trim[3]}, 0, 2)
    for side in ("right", "left"):
        hand = noise(PRIEST_SKIN, 2709, 2.8, 0.5)
        sl = cassock(2710 + (side == "left"))
        paint_cube(t, p[f"{side}_arm"], lambda f, x, y, w, h, s=sl, hd=hand:
                   hd(f, x, y, w, h) if (f == "down" or (f != "up" and y >= h - 3)) else s(f, x, y, w, h))
        paint_cube(t, p[f"sleeve_{side[0]}"], lambda f, x, y, w, h: None if f in ("up", "down") else
                   (trim[3] if y == h - 1 else robe_base(f, x, y, w, h)))
        trouser_leg(t, p[f"{side}_leg"], CASSOCK, SHOE, 2712 + (side == "left"), 2)
    paint_cube(t, p["censer_chain"], chain_links(BRASS))
    paint_cube(t, p["censer_lid"], lambda f, x, y, w, h: BRASS[4] if f == "up" and (x + y) % 2 == 0 else BRASS[3])

    def censer(f, x, y, w, h):
        if f == "up":
            return OCHRE[5] if 1 <= x <= 2 and 1 <= y <= 2 else BRASS[2]    # embers of sulfur inside
        if f == "down":
            return BRASS[1]
        if y == 1 and x % 2 == 0:
            return hexc("#2a1804")                # pierced vents
        return BRASS[4] if y == 0 else BRASS[3] if x % 3 else BRASS[2]
    paint_cube(t, p["censer"], censer)
    for f, (u, v, w, h) in p["censer"].rects.items():
        for y in range(h):
            for x in range(w):
                c = t.rows[v + y][u + x]
                if c[3] and (f == "up" and 1 <= x <= 2 and 1 <= y <= 2):
                    g.set(u + x, v + y, OCHRE[5])
                elif c[3] and f not in ("up", "down") and y == 1 and x % 2 == 0:
                    g.set(u + x, v + y, OCHRE[3])
    return r, t, g


def priest_anims():
    f = demon_anims("sulfur_priest", 0.25, censer=True)
    a = "animation.sulfur_priest."
    # cast (hit 0.5 s = 10 ticks): the right hand drawn back, then thrust forward palm out.
    c = f.new(a + "cast", 0.9)
    c.rot("right_arm", (0, [0, 0, 0]), (0.3, [-40, 20, 30], E_OUT), (0.5, [-92, -6, 0], E_SNAP), (0.65, [-90, -4, 0]),
          (0.9, [0, 0, 0], E_IO))
    c.rot("body", (0, [0, 0, 0]), (0.3, [-4, 20, 0], E_OUT), (0.5, [8, -10, 0], E_SNAP), (0.65, [8, -10, 0]), (0.9, [0, 0, 0], E_IO))
    c.rot("head", (0, [0, 0, 0]), (0.3, [-8, -16, 0], E_OUT), (0.5, [4, 8, 0], E_SNAP), (0.9, [0, 0, 0], E_IO))
    c.rot("left_arm", (0, [-14, 0, -4]), (0.3, [10, 0, -12], E_OUT), (0.5, [16, 0, -10]), (0.9, [-14, 0, -4], E_IO))
    c.rot("censer", (0, [0, 0, 0]), (0.5, [-20, 0, 0], E_OUT), (0.7, [16, 0, 0], E_IO), (0.9, [0, 0, 0], E_IO))
    # smoke (hit 0.4 s = 8 ticks): the censer whirled overhead on its chain, a cloud of yellow smoke.
    s = f.new(a + "smoke", 1.0)
    s.rot("left_arm", (0, [-14, 0, -4]), (0.2, [-165, 0, -8], E_OUT), (0.8, [-170, 0, -6]), (1.0, [-14, 0, -4], E_IO))
    s.rot("censer", (0, [0, 0, 0]), (0.2, [60, 0, 0], E_OUT), (0.3, [60, 120, 0]), (0.4, [60, 240, 0]), (0.5, [60, 360, 0]),
          (0.6, [60, 480, 0]), (0.7, [60, 600, 0]), (0.8, [40, 720, 0]), (1.0, [0, 720, 0], E_IO))
    s.rot("head", (0, [0, 0, 0]), (0.2, [-24, 0, 0], E_OUT), (0.8, [-26, 0, 0]), (1.0, [0, 0, 0], E_IO))
    s.rot("right_arm", (0, [0, 0, 0]), (0.2, [-30, 0, 24], E_OUT), (0.8, [-30, 0, 26]), (1.0, [0, 0, 0], E_IO))
    s.rot("body", (0, [0, 0, 0]), (0.2, [-6, 0, 0], E_OUT), (0.8, [-6, 0, 0]), (1.0, [0, 0, 0], E_IO))
    return f


# =============================================================================================================
# demon_flayer
# =============================================================================================================

HUNCH = 30


def flayer_rig():
    r = Rig("demon_flayer", 64, 64)
    r.bone("root", (0, 0, 0))
    # The stoop lives in the rest pose: the torso bent forward, the head craned back up, the arms hanging in front.
    body = r.bone("body", (0, 12, 0), "root", rotation=(HUNCH, 0, 0))
    head = r.bone("head", (0, 24, -0.5), "body", rotation=(-HUNCH - 4, 0, 0))
    ra = r.bone("right_arm", (-5, 22.5, 0), "body", rotation=(-HUNCH - 8, 0, 4))
    la = r.bone("left_arm", (5, 22.5, 0), "body", rotation=(-HUNCH - 8, 0, -4))
    rl = r.bone("right_leg", (-2, 12, 0), "root", rotation=(-10, 0, 3))
    ll = r.bone("left_leg", (2, 12, 0), "root", rotation=(6, 0, -3))
    p = {
        "body": box(body, (-3.5, 12, -1.5), (7, 12, 3), tag="body"),
        "head": box(head, (-4, 24, -4.5), (8, 8, 8), tag="head"),
        "hair": box(head, (-4, 26, -4.5), (8, 6, 8), inflate=0.35, tag="hair"),
        "right_arm": box(ra, (-6.5, 11, -1.5), (3, 13, 3), tag="right_arm"),
        "left_arm": box(la, (3.5, 11, -1.5), (3, 13, 3), tag="left_arm"),
        "right_leg": box(rl, (-3.6, 0, -1.5), (3, 12, 3), tag="right_leg"),
        "left_leg": box(ll, (0.6, 0, -1.5), (3, 12, 3), tag="left_leg"),
        "apron": box(body, (-3.5, 4, -1.9), (7, 18, 0), faces=("north",), tag="apron"),
        "apron_strap": box(body, (-3.5, 21, -1.5), (7, 1, 3), inflate=0.25, tag="apron_strap"),
    }
    for side, cx in (("right", -5.0), ("left", 5.0)):
        b = r.get(f"{side}_arm")
        p[f"{side}_handle"] = box(b, (cx - 0.5, 9, -1), (1, 3, 2), tag="handle")
        p[f"{side}_knife"] = box(b, (cx - 0.5, 1, -1.5), (1, 8, 2), tag="knife", rotation=(-12, 0, 0), pivot=(cx, 10, 0))
    r.pack()
    return r, p


def flayer_paint():
    r, p = flayer_rig()
    t = Tex(64, 64, 2801)
    g = Tex(64, 64, 2802)
    skin = noise(FLAYER_SKIN, 2803, 2.5, 0.7)
    hair = noise(LANK, 2804, 2.4, 1.0, 9.0)
    paint_cube(t, p["head"], lambda f, x, y, w, h: hair(f, x, y, w, h) if (f in ("up", "south") or (f in ("east", "west") and y < 5))
               else skin(f, x, y, w, h))
    pal = {"s": FLAYER_SKIN[3], "S": FLAYER_SKIN[4], "h": LANK[2], "H": LANK[4], "d": FLAYER_SKIN[1], "D": FLAYER_SKIN[0],
           "k": K, "g": GLINT, "n": FLAYER_SKIN[4], "m": hexc("#2a1010"), "t": hexc("#a39a7a"), "r": SCAR}
    stamp_face(t, p["head"], "north", [
        "hhhhhhhh",
        "hhshhhsh",
        "hdddsddh",
        "hkkgskkh",
        "hkkSSkks",
        "sdssnsds",
        "sdtmmtdr",
        "ssDsssDs",
    ], pal)
    glow_eyes(t, g, p["head"])

    def lank(f, x, y, w, h):
        if f == "down":
            return None
        if f == "north":
            return hair(f, x, y, w, h) if (y < 1 or (y < 3 and x in (0, 1, 6, 7)) or (x in (0, 7) and y < 6)) else None
        if y > h - 2 and (x * 5 + len(f)) % 3 == 0:
            return None                          # lank strands
        return hair(f, x, y, w, h)
    paint_cube(t, p["hair"], lank)
    # The bare torso under the apron: grey-pale, ribs showing, scars.
    def torso(f, x, y, w, h):
        c = skin(f, x, y, w, h)
        if f in ("north", "south") and 2 <= y <= 8 and y % 2 == 0 and 0 < x < w - 1:
            c = shade(c, 0.82)                    # ribs
        if fbm(x * 3, y * 2 + len(f), 2805, 64, 64, 2, 7.0) > 0.72:
            c = SCAR
        if f in ("north", "south", "east", "west") and y >= h - 2:
            c = TROUSER[2]
        return c
    paint_cube(t, p["body"], torso)

    def arm(f, x, y, w, h, seed):
        if f == "down":
            return skin(f, x, y, w, h)
        c = skin(f, x, y, w, h)
        # Scars across the forearms: short pale-edged slashes.
        if f != "up" and (y + x * 2 + seed) % 5 == 0 and 3 < y < h - 2:
            return SCAR if (x + seed) % 2 else SCAR_HI
        if f != "up" and fbm(x * 4, y * 2 + len(f), seed, 64, 64, 2, 6.0) > 0.7:
            return mix(c, BLOOD[2], 0.6)          # dried blood up to the elbows
        if f != "up" and y >= h - 4:
            return mix(c, BLOOD[3], 0.45)
        return c
    paint_cube(t, p["right_arm"], lambda f, x, y, w, h: arm(f, x, y, w, h, 2806))
    paint_cube(t, p["left_arm"], lambda f, x, y, w, h: arm(f, x, y, w, h, 2807))
    for side in ("right", "left"):
        paint_cube(t, p[f"{side}_leg"], boot_leg(cloth(TROUSER, 2808 + (side == "left"), 2.4, 0.4), BOOT, 2))
        paint_cube(t, p[f"{side}_handle"], lambda f, x, y, w, h: P.WOOD[1 + (y % 2)])

        def blade(f, x, y, w, h):
            if f in ("east", "west"):
                c = KNIFE[4] if x == 0 else KNIFE[3]      # the flat; the edge forward
                if fbm(x * 5, y * 3 + len(f), 2810, 32, 32, 2, 5.0) > 0.62:
                    c = mix(c, BLOOD[3], 0.6)
                return c
            if f == "north":
                return KNIFE[5]
            return KNIFE[2]
        paint_cube(t, p[f"{side}_knife"], blade)
    apron = cloth(APRON, 2811, 2.6, 0.3)

    def apron_m(f, x, y, w, h):
        if y >= h - 1 and (x * 3) % 4 == 1:
            return None
        c = apron(f, x, y, w, h)
        n = fbm(x * 2, y * 2, 2812, 32, 32, 3, 5.0)
        if n > 0.64:
            c = mix(c, BLOOD[2] if n > 0.7 else BLOOD[4], 0.7)       # old and fresh blood
        elif n < 0.3:
            c = mix(c, GRIME, 0.5)
        if 4 <= y <= 6 and x in (0, w - 1):
            c = APRON[1]
        return c
    paint_cube(t, p["apron"], apron_m)
    paint_cube(t, p["apron_strap"], lambda f, x, y, w, h: None if f in ("up", "down") or (f == "north" and 2 <= x <= 4)
               else APRON[1 + (x % 2)])
    return r, t, g


def flayer_anims():
    f = demon_anims("demon_flayer", 0.2, hunch=0.3, walk_len=0.8, swing=14)
    a = "animation.demon_flayer."
    # A crouching gait: knees bent, the stoop deepened, the knives swinging low.
    w = replace(f, a + "walk", 0.8, loop=True)
    for bone, s in (("right_leg", 1), ("left_leg", -1)):
        w.rot(bone, (0, [22 * s - 10, 0, 0]), (0.4, [-22 * s - 10, 0, 0], E_IO), (0.8, [22 * s - 10, 0, 0], E_IO))
    for bone, s in (("right_arm", -1), ("left_arm", 1)):
        w.rot(bone, (0, [14 * s - 6, 0, 0]), (0.4, [-14 * s - 6, 0, 0], E_IO), (0.8, [14 * s - 6, 0, 0], E_IO))
    w.rot("body", (0, [8, 6, 0]), (0.4, [8, -6, 0], E_IO), (0.8, [8, 6, 0], E_IO))
    w.rot("head", (0, [-8, -6, 0]), (0.4, [-8, 6, 0], E_IO), (0.8, [-8, -6, 0], E_IO))
    w.pos("body", (0, [0, -1.0, 0]), (0.2, [0, -0.4, 0], E_OUT), (0.4, [0, -1.0, 0], E_IN), (0.6, [0, -0.4, 0], E_OUT),
          (0.8, [0, -1.0, 0], E_IN))
    # attack (hit 0.2 s = 4 ticks): a quick backhand rip with the right knife.
    at = replace(f, a + "attack", 0.5)
    at.rot("right_arm", (0, [0, 0, 0]), (0.1, [-50, 40, 40], E_OUT), (0.2, [-70, -40, -10], E_SNAP), (0.5, [0, 0, 0], E_IO))
    at.rot("body", (0, [0, 0, 0]), (0.1, [0, 20, 0], E_OUT), (0.2, [10, -20, 0], E_SNAP), (0.5, [0, 0, 0], E_IO))
    at.rot("head", (0, [0, 0, 0]), (0.2, [-6, 12, 0], E_SNAP), (0.5, [0, 0, 0], E_IO))
    # vanish (gone on 0.4 s = 8 ticks): crouch and fold in on itself.
    v = f.new(a + "vanish", 0.6)
    v.rot("body", (0, [0, 0, 0]), (0.3, [40, 0, 0], E_OUT), (0.4, [52, 0, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    v.rot("head", (0, [0, 0, 0]), (0.3, [20, 0, 0], E_OUT), (0.4, [30, 0, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    v.rot("right_arm", (0, [0, 0, 0]), (0.3, [-40, -30, -40], E_OUT), (0.4, [-50, -40, -60], E_SNAP), (0.6, [0, 0, 0], E_IO))
    v.rot("left_arm", (0, [0, 0, 0]), (0.3, [-40, 30, 40], E_OUT), (0.4, [-50, 40, 60], E_SNAP), (0.6, [0, 0, 0], E_IO))
    for bone in ("right_leg", "left_leg"):
        v.rot(bone, (0, [0, 0, 0]), (0.3, [-50, 0, 0], E_OUT), (0.4, [-60, 0, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    v.pos("body", (0, [0, 0, 0]), (0.3, [0, -4, 0], E_OUT), (0.4, [0, -6, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    v.scale("body", (0, [1, 1, 1]), (0.3, [1, 1, 1]), (0.4, [0.7, 0.6, 0.7], E_SNAP), (0.6, [1, 1, 1], E_IO))
    # strike (hit 0.2 s = 4 ticks): a fast lunge, both knives crossing in front.
    s = f.new(a + "strike", 0.55)
    s.rot("right_arm", (0, [0, 0, 0]), (0.1, [-100, 30, 50], E_OUT), (0.2, [-80, -40, -30], E_SNAP), (0.55, [0, 0, 0], E_IO))
    s.rot("left_arm", (0, [0, 0, 0]), (0.1, [-100, -30, -50], E_OUT), (0.2, [-80, 40, 30], E_SNAP), (0.55, [0, 0, 0], E_IO))
    s.rot("body", (0, [0, 0, 0]), (0.1, [-6, 0, 0], E_OUT), (0.2, [14, 0, 0], E_SNAP), (0.55, [0, 0, 0], E_IO))
    s.pos("root", (0, [0, 0, 0]), (0.1, [0, 0, 1], E_OUT), (0.2, [0, 0, -3], E_SNAP), (0.55, [0, 0, 0], E_IO))
    s.rot("right_leg", (0, [0, 0, 0]), (0.2, [-34, 0, 0], E_SNAP), (0.55, [0, 0, 0], E_IO))
    s.rot("left_leg", (0, [0, 0, 0]), (0.2, [26, 0, 0], E_SNAP), (0.55, [0, 0, 0], E_IO))
    return f


# =============================================================================================================
# demon_tormentor
# =============================================================================================================

def tormentor_rig():
    r = Rig("demon_tormentor", 64, 64)
    r.bone("root", (0, 0, 0))
    body = r.bone("body", (0, 12, 0), "root")
    head = r.bone("head", (0, 24, 0), "body")
    ra = r.bone("right_arm", (-6, 22.5, 0), "body")
    la = r.bone("left_arm", (6, 22.5, 0), "body")
    rl = r.bone("right_leg", (-2.5, 12, 0), "root")
    ll = r.bone("left_leg", (2.5, 12, 0), "root")
    p = {
        "body": box(body, (-5, 12, -2.5), (10, 12, 5), tag="body"),
        "head": box(head, (-4, 24, -4), (8, 8, 8), tag="head"),
        "peak": box(head, (-2, 32, -2), (4, 2, 4), tag="peak"),
        "right_arm": box(ra, (-10, 11, -2), (4, 13, 4), tag="right_arm"),
        "left_arm": box(la, (6, 11, -2), (4, 13, 4), tag="left_arm"),
        "right_leg": box(rl, (-5.1, 0, -2.5), (5, 12, 5), tag="right_leg"),
        "left_leg": box(ll, (0.1, 0, -2.5), (5, 12, 5), tag="left_leg"),
        "apron": box(body, (-4.5, 3, -2.9), (9, 18, 0), faces=("north",), tag="apron"),
        "hook_grip": box(ra, (-8.5, 9, -1), (1, 3, 2), tag="hook_grip"),
        "hook_shaft": box(ra, (-8.5, 4, -0.5), (1, 5, 1), tag="hook_shaft"),
        "hook_bend": box(ra, (-8.5, 3, -3.5), (1, 1, 4), tag="hook_bend"),
        "hook_tip": box(ra, (-8.5, 4, -3.5), (1, 2, 1), tag="hook_tip"),
        "hook_chain": box(ra, (-8.5, 12, 1.5), (1, 1, 1), tag="hook_chain"),
    }
    r.bone("hook_hand", (-8, 10.5, 0), "right_arm")
    r.pack()
    return r, p


def tormentor_paint():
    r, p = tormentor_rig()
    t = Tex(64, 64, 2901)
    g = Tex(64, 64, 2902)
    hood = noise(HOOD, 2903, 2.4, 0.9, 7.0)

    def hood_m(f, x, y, w, h):
        c = hood(f, x, y, w, h)
        if f in ("east", "west", "south") and y == h - 1:
            return HOOD[1]
        if f == "south" and x == w // 2 and y > 1:
            return LEATHER[1] if y % 2 else LEATHER[3]       # laced up the back
        return c
    paint_cube(t, p["head"], hood_m)
    pal = {"o": HOOD[0], "s": TORM_SKIN[2], "k": K, "g": GLINT, "e": HOOD[4], "m": hexc("#1b0f0d"), "j": TORM_SKIN[3]}
    # The hood's eye holes: a stitched rim, the black eyes behind.
    stamp_face(t, p["head"], "north", [
        "........",
        "........",
        ".eoo.ooe",
        "eokgookg",
        ".okkookk",
        "..oo..oo",
        "........",
        "....e...",
    ], {"o": HOOD[0], "k": K, "g": GLINT, "e": HOOD[4]})
    glow_eyes(t, g, p["head"])
    paint_cube(t, p["peak"], lambda f, x, y, w, h: None if f == "down" else HOOD[3 if f == "up" else 2])
    skin = noise(TORM_SKIN, 2904, 2.5, 0.7)
    leather = cloth(LEATHER, 2905, 2.5, 0.3)

    def torso(f, x, y, w, h):
        # Bare, broad and hairy; a leather jerkin open down the front; trousers at the hips.
        if f in ("north", "south", "east", "west") and y >= h - 2:
            return TROUSER[2] if y == h - 2 else TROUSER[1]
        if f == "north" and 2 <= x <= w - 3:
            c = skin(f, x, y, w, h)
            if (x * 3 + y * 5) % 7 == 0:
                c = shade(c, 0.75)
            return c
        return leather(f, x, y, w, h)

    def chains(f, x, y, w, h):
        if f in ("up", "down"):
            return torso(f, x, y, w, h)
        if f in ("north", "south"):
            d1, d2 = abs(x - y * (w - 1) / (h - 1)), abs((w - 1 - x) - y * (w - 1) / (h - 1))
            d = min(d1, d2)
            if d < 0.9 and y < h - 2:
                k = (y + (x if d1 < d2 else 0)) % 2
                return RUST[4] if k else CHAIN[2]
            return torso(f, x, y, w, h)
        # Over the shoulders and round the sides.
        if y < 1:
            return RUST[3] if x % 2 else CHAIN[2]
        return torso(f, x, y, w, h)
    paint_cube(t, p["body"], chains)
    for side in ("right", "left"):
        hand = noise(TORM_SKIN, 2906, 2.7, 0.6)
        bracer = cloth(LEATHER, 2907, 2.2, 0.3)

        def arm(f, x, y, w, h, hd=hand, br=bracer):
            if f == "down" or (f != "up" and y >= h - 3):
                return hd(f, x, y, w, h)
            if f != "up" and 5 <= y < h - 3:
                c = br(f, x, y, w, h)
                return CHAIN[3] if y in (6, h - 5) and x % 2 else c
            return hd(f, x, y, w, h)
        paint_cube(t, p[f"{side}_arm"], arm)
        paint_cube(t, p[f"{side}_leg"], boot_leg(cloth(TROUSER, 2908 + (side == "left"), 2.4, 0.4), BOOT, 4))
    apron = cloth(LEATHER, 2909, 2.9, 0.3)

    def apron_m(f, x, y, w, h):
        if y < 9 and (x < 1 + (8 - y) // 3 or x > w - 2 - (8 - y) // 3):
            return None                            # the bib narrows to the chest
        c = apron(f, x, y, w, h)
        if x in (0, w - 1) or y == h - 1:
            c = LEATHER[1]
        n = fbm(x * 2, y * 2, 2910, 32, 32, 3, 5.0)
        if n > 0.62:
            c = mix(c, BLOOD[1], 0.65)
        if y == 9:
            c = LEATHER[0]
        return c
    paint_cube(t, p["apron"], apron_m)
    paint_cube(t, p["hook_grip"], lambda f, x, y, w, h: P.WOOD[1 + (y % 2)])
    rusty = lambda f, x, y, w, h: RUST[3] if (x + y) % 3 else (CHAIN[3] if f in ("north", "up") else RUST[2])
    for k in ("hook_shaft", "hook_bend", "hook_tip"):
        paint_cube(t, p[k], rusty)
    paint_cube(t, p["hook_chain"], lambda f, x, y, w, h: CHAIN[3])
    return r, t, g


def tormentor_anims():
    f = demon_anims("demon_tormentor", 0.3, walk_len=1.2, swing=18)
    a = "animation.demon_tormentor."
    at = replace(f, a + "attack", 0.6)
    at.rot("right_arm", (0, [0, 0, 0]), (0.17, [-150, 0, 30], E_OUT), (0.3, [-40, 0, -10], E_SNAP), (0.6, [0, 0, 0], E_IO))
    at.rot("body", (0, [0, 0, 0]), (0.17, [-4, 22, 0], E_OUT), (0.3, [14, -14, 0], E_SNAP), (0.6, [0, 0, 0], E_IO))
    at.rot("left_arm", (0, [0, 0, 0]), (0.17, [-24, 0, -18]), (0.6, [0, 0, 0], E_IO))
    # hook (thrown at 0.4 s = 8 ticks): wind back, then throw the right arm forward.
    hk = f.new(a + "hook", 0.75)
    hk.rot("right_arm", (0, [0, 0, 0]), (0.25, [40, 30, 50], E_OUT), (0.32, [30, 40, 60]), (0.4, [-100, -10, 0], E_SNAP),
           (0.5, [-96, -8, 0]), (0.75, [0, 0, 0], E_IO))
    hk.rot("body", (0, [0, 0, 0]), (0.25, [-4, 34, 0], E_OUT), (0.32, [-4, 38, 0]), (0.4, [12, -18, 0], E_SNAP), (0.75, [0, 0, 0], E_IO))
    hk.rot("head", (0, [0, 0, 0]), (0.25, [0, -28, 0], E_OUT), (0.4, [4, 14, 0], E_SNAP), (0.75, [0, 0, 0], E_IO))
    hk.rot("left_arm", (0, [0, 0, 0]), (0.25, [-50, 0, -10], E_OUT), (0.4, [10, 0, -16], E_SNAP), (0.75, [0, 0, 0], E_IO))
    hk.rot("right_leg", (0, [0, 0, 0]), (0.25, [14, 0, 0], E_OUT), (0.4, [-22, 0, 0], E_SNAP), (0.75, [0, 0, 0], E_IO))
    hk.rot("left_leg", (0, [0, 0, 0]), (0.25, [-10, 0, 0], E_OUT), (0.4, [18, 0, 0], E_SNAP), (0.75, [0, 0, 0], E_IO))
    return f


# =============================================================================================================
# projectiles (16x16)
# =============================================================================================================

def sulfur_ball():
    t = Tex(16, 16, 3001)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            n = fbm(x * 4, y * 4, 3002, 64, 64, 3, 6.0)
            edge = 6.2 + (n - 0.5) * 2.2
            if r > edge:
                continue
            k = r / edge
            if k < 0.35:
                c = P.SULFUR[5]
            elif k < 0.6:
                c = mix(P.SULFUR[4], P.HELLFIRE[4], (k - 0.35) * 2)
            elif k < 0.85:
                c = mix(P.HELLFIRE[4], P.HELLFIRE[3], n)
            else:
                c = mix(P.HELLFIRE[2], hexc("#3a2a08"), n)
            if 0.4 < k < 0.8 and n > 0.62:
                c = mix(c, P.SULFUR[2], 0.6)            # crusted sulfur
            t.set(x, y, c)
    return t


def tormentor_hook():
    t2 = Tex(16, 16, 3101)
    # the bend: a half circle below the shank, open to the left, ending in an upward point
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 5.5, y - 10.5)
            if 2.0 <= r <= 3.4 and y >= 10:
                t2.set(x, y, RUST[3] if (x + y) % 3 else RUST[4])
    for y in range(4, 11):
        t2.set(8, y, RUST[3] if y % 3 else CHAIN[3])
        t2.set(9, y, RUST[2])
    for y in (7, 8, 9, 10):
        t2.set(2, y, RUST[3])
    t2.set(2, 6, CHAIN[4])                          # the barb's point
    t2.set(3, 7, RUST[2])
    for y in range(0, 4):
        c = CHAIN[4] if y % 2 == 0 else CHAIN[2]
        t2.set(8, y, c)
        if y % 2 == 0:
            t2.set(7, y, CHAIN[2])
            t2.set(9, y, CHAIN[2])
    t2.set(8, 4, CHAIN[1])
    # blood on the point
    t2.set(2, 7, BLOOD[3])
    t2.set(3, 12, BLOOD[2])
    return t2


def light_lance():
    """A lance of light drawn along +x (the point at the right): a thin shaft fading to the tail, a leaf-shaped head of
    white light rimmed gold, a soft golden halo."""
    t = Tex(16, 16, 3201)
    gold = P.GRACE
    for y in range(16):
        for x in range(16):
            px = x + 0.5
            d = abs(y - 7)
            if px < 10:
                half = 0.3 + 0.3 * px / 10            # the shaft
                fade = 0.35 + 0.65 * px / 10
            else:
                u = (px - 10) / 6.0                    # the head: widest a third of the way, then to a point
                half = 3.0 * (u / 0.3) if u < 0.3 else 3.0 * (1 - u) / 0.7
                half = max(half, 0.3)
                fade = 1.0
            if d <= half * 0.5 + 0.05:
                c = mix(hexc("#ffffff"), gold[5], 0.0 if px >= 10 else 0.2)
                a = int(255 * fade)
            elif d <= half + 0.05:
                c, a = gold[4], int(235 * fade)
            elif d <= half + 1.2:
                c, a = gold[3], int(110 * fade * (1 - (d - half) / 1.2))
            else:
                continue
            if a > 12:
                t.set(x, y, (c[0], c[1], c[2], a))
    return t


# =============================================================================================================

def generate():
    r, t, g = brute_paint()
    r.write(GEO + "demon_brute.geo.json")
    save(t, "entity", "demon_brute")
    save(g, "entity", "demon_brute_glowmask")
    brute_anims().write(ANIM + "demon_brute.animation.json")
    for name, painter, anims in (("sulfur_priest", priest_paint, priest_anims), ("demon_flayer", flayer_paint, flayer_anims),
                                 ("demon_tormentor", tormentor_paint, tormentor_anims)):
        r, t, g = painter()
        r.write(GEO + name + ".geo.json")
        save(t, "entity", name)
        save(g, "entity", name + "_glowmask")
        anims().write(ANIM + name + ".animation.json")
    save(sulfur_ball(), "entity/projectile", "sulfur_ball")
    save(light_lance(), "entity/projectile", "light_lance")
    save(tormentor_hook(), "entity/projectile", "tormentor_hook")


PAINTERS = {"demon_brute": (brute_paint, brute_anims), "sulfur_priest": (priest_paint, priest_anims),
            "demon_flayer": (flayer_paint, flayer_anims), "demon_tormentor": (tormentor_paint, tormentor_anims)}


def preview(out_dir, clips=(("idle", 0.0),)):
    import json
    import os
    import preview3d
    os.makedirs(out_dir, exist_ok=True)
    for name, (painter, anims) in PAINTERS.items():
        r, t, g = painter()
        f = anims()
        j = {"animations": {a.name: a.to_json() for a in f.anims}}
        for clip, tm in clips:
            full = f"animation.{name}.{clip}"
            if full not in j["animations"]:
                continue
            preview3d.render(r, t, os.path.join(out_dir, f"{name}_{clip}.png"), pose=preview3d.pose_from(j, full, tm), scale=5)


if __name__ == "__main__":
    generate()
