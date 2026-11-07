"""The demon blood and the Apocalypse's keepsakes (v0.40): the blood powers' wheel icons, the blood HUD's sheet, the withdrawal
vignette and effect icon, the Panic Room door, and the Cage Shackle. Contracts: BloodAssets, ApocalypseAssets (Ruby's flask is
ruby_art's).

  gui/allegiance/power/blood_push|blood_exorcism|blood_smite.png   24x24, the wheel's icon frame in the blood's colours (a dark
      crimson field, a rim of blackened iron): a palm with a shock ring; a mouth of black smoke torn out of a figure; a fist
      closing on a black heart.
  gui/demon_blood.png   128x64, read by client/apocalypse/blood/BloodHud (the constants there mirror these):
      vial     (0, 0)    16x48   a glass vial: cork and wax at the top, a cracked paper label, the glass empty (see-through)
      fill     (16, 0)   10x40   the blood inside it (drawn cropped from the bottom: the HUD shows the bottom `n` rows),
                                  placed at the vial's inside (3, 6)
      rings    (32, 0)   16x16 each: calm (x 32), hungry (x 48), withdrawal (x 64) -- the dependency's ring; the HUD draws the
                                  dependency itself as an arc of lit dots over it
      pips     (32, 16)  8x8 each: empty (x 32), lit (x 40) -- the three tiers of blood power
  misc/withdrawal_vignette.png   256x256: dark red edges with veins creeping in, the centre clear.
  mob_effect/withdrawal.png      18x18: a cracked vial, a drop falling.
  block/panic_room_door_top|bottom.png, item/panic_room_door.png   a heavy riveted iron door: a sliding peephole up top, a devil's
      trap welded on the lower half, a wheel lock.
  item/cage_shackle.png          a heavy black-iron cuff, a broken link hanging off it, Enochian marks glowing faint red.
"""

import math

from allegiance_art import Ink, lit
from chuck_art import h01
from common import art, save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

BLOOD = Ramp("#1c0204", "#360509", "#56080e", "#7a0f14", "#a01c1c", "#d0382c")
CRIMSON = Ramp("#120203", "#220407", "#3a060b", "#560a10", "#741418", "#9a2222")
IRON = Ramp("#0c0c0f", "#17171c", "#24242b", "#33333c", "#45454f", "#5c5c68")
GLASS = Ramp("#2c3438", "#46525a", "#66747c", "#8c9aa2", "#b6c2c8", "#e2eaee")
CORK = Ramp("#3a2614", "#56391e", "#74502c", "#93683c", "#b08450", "#c8a06a")
PAPER = Ramp("#5e4a2c", "#86704a", "#a8926a", "#c6b08a", "#ddcaa4", "#efe2c4")
SKIN = Ramp("#5e3d2c", "#8c604a", "#b07e63", "#c69579", "#d8ac90", "#e7c3a8")
SMOKE = Ramp("#020202", "#08080a", "#111114", "#1c1c20", "#2a2a30", "#3e3e46")
STEEL = Ramp("#24272c", "#454a52", "#6b717a", "#9198a2", "#bac0c8", "#e6eaee")
RUST = Ramp("#2a1208", "#45200e", "#643016", "#86421e", "#a65a2a", "#c47a40")
EMBER = Ramp("#3a0604", "#6e0d06", "#a8200a", "#e0430f", "#ff7a26", "#ffc46a")
OUT = hexc("#0c0b0d")
WHITE = hexc("#ffffff")


# =====================================================================================================
# Wheel icons
# =====================================================================================================

def frame():
    """The wheel's icon frame (allegiance_art.icon_frame's shape) in the blood's colours."""
    t = Tex(24, 24, 9920)
    for y in range(24):
        for x in range(24):
            e = min(x, y, 23 - x, 23 - y)
            corner = (x in (0, 23) and y in (0, 1, 22, 23)) or (y in (0, 23) and x in (0, 1, 22, 23))
            if corner:
                continue
            if e == 0:
                t.set(x, y, IRON[1])
            elif e == 1:
                t.set(x, y, IRON[4] if x + y < 23 else IRON[2])
            else:
                r = math.hypot(x - 11.5, y - 11.5) / 12
                c = mix(CRIMSON[3], CRIMSON[0], min(1.0, r * 0.9))
                if fbm(x * 3, y * 3, 9921, 24, 24, 2, 4.0) > 0.72:
                    c = mix(c, BLOOD[1], 0.5)                     # dark clots in the field
                t.set(x, y, c)
    return t


def icon(pid):
    t = frame()
    k = Ink(t)
    if pid == "blood_push":
        # A shock ring thrown off an open palm.
        for i, r in enumerate((9.0, 6.6)):
            k.ring(12, 12, r, r + 1.1, lambda x, y, i=i: BLOOD[5 - i] if (math.atan2(y - 12, x - 12) * 3 + i) % 2.0 < 1.4 else None)
        k.poly([(8, 19), (8, 11), (9, 9.5), (10, 10.5), (10, 6), (11.2, 5), (12.2, 6), (12.4, 9.5), (13, 5.4), (14.2, 5),
                (15, 6), (14.8, 10), (16, 7.6), (17, 8.4), (16, 13), (15, 19)], lit(SKIN, 8, 5, 17, 19))
        k.line(10.6, 11, 10.6, 15, 0.5, SKIN[1])
        k.line(13.2, 11, 13.2, 15, 0.5, SKIN[1])
        k.disc(12, 14.5, 1.6, BLOOD[4])                          # blood in the palm
    elif pid == "blood_exorcism":
        # A dark figure, its head thrown back, the black smoke torn out of its mouth and up out of the frame.
        k.poly([(8, 21), (9, 14), (11, 12.5), (13, 12.5), (15, 14), (16, 21)], lit(IRON, 8, 12, 16, 21))
        k.disc(12, 10.5, 2.6, lit(IRON, 9, 8, 15, 13))
        for i, (x, y, r) in enumerate(((12.4, 8.0, 1.6), (13.2, 6.0, 2.2), (14.6, 4.0, 2.6), (16.6, 3.0, 2.2), (11.6, 4.6, 1.8))):
            k.disc(x, y, r, lambda xx, yy, i=i: SMOKE[1 + (h01("sm", xx, yy) < 0.3) * 2])
        k.ring(12, 12, 9.4, 10.4, lambda x, y: BLOOD[4] if (x * 7 + y * 3) % 5 else None)
        k.disc(12.4, 9.5, 0.8, BLOOD[5])
    elif pid == "blood_smite":
        # A fist closing round a black heart that cracks with red light.
        k.disc(12, 9.5, 4.6, lambda x, y: SMOKE[2] if (x + y) % 5 else SMOKE[3])
        k.poly([(7.6, 9), (12, 15), (16.4, 9)], SMOKE[2])
        k.line(12, 6, 11, 9, 0.6, BLOOD[5]).line(11, 9, 13, 11, 0.6, BLOOD[5]).line(13, 11, 12, 13.5, 0.6, BLOOD[5])
        k.poly([(6, 21), (6, 14.5), (8, 12.5), (10, 13.5), (12, 12.5), (14, 13.5), (16, 12.5), (18, 14.5), (18, 21)], lit(SKIN, 6, 12, 18, 21))
        for x in (9, 12, 15):
            k.line(x, 13, x, 16, 0.5, SKIN[1])
        k.line(7, 17, 17, 17, 0.6, BLOOD[4])
    return t


# =====================================================================================================
# The HUD sheet
# =====================================================================================================

VIAL_IN = (3, 6, 10, 40)


def vial(t, ox, oy):
    """The vial (16x48): see-through glass, its edges lit; a cork and red wax at the neck; a cracked label."""
    for y in range(48):
        for x in range(16):
            # The body (x 2..13, y 5..47), rounded at the bottom; the neck (x 5..10, y 1..5).
            body = 2 <= x <= 13 and 5 <= y <= 46 and not (y >= 44 and (x - 7.5) ** 2 / 36 + (y - 41) ** 2 / 36 > 1)
            neck = 5 <= x <= 10 and 1 <= y <= 5
            if not (body or neck):
                continue
            edge = x in (2, 13) or y == 46 or (neck and x in (5, 10)) or (body and y >= 44 and
                                                                           (x - 7.5) ** 2 / 36 + (y - 41) ** 2 / 36 > 0.75)
            if neck and y <= 3:
                c = CORK[3 + (x == 6) - (x == 9)] if y > 1 else BLOOD[3]
                t.set(ox + x, oy + y, c)
                continue
            if edge:
                t.set(ox + x, oy + y, GLASS[4] if x < 8 else GLASS[2])
            elif x == 4 and 8 <= y <= 40:
                t.set(ox + x, oy + y, (230, 240, 245, 110))       # a highlight down the glass
            elif body:
                t.set(ox + x, oy + y, (120, 140, 150, 40))        # the empty glass, barely there
    # The wax drip over the cork.
    for y in range(1, 6):
        t.set(ox + 5, oy + y, BLOOD[4])
    t.set(ox + 6, oy + 4, BLOOD[3])
    # A paper label, cracked and stained, across the middle.
    for y in range(22, 30):
        for x in range(3, 13):
            c = PAPER[3 + ((x + y) % 4 == 0)]
            if (x - y + 30) % 9 == 0:
                c = PAPER[1]                                     # the crack
            if h01("lab", x, y) < 0.12:
                c = mix(c, BLOOD[2], 0.6)
            t.set(ox + x, oy + y, (c[0], c[1], c[2], 190))
    for x in range(5, 11):
        t.set(ox + x, oy + 25, (60, 30, 24, 210))               # a scrawl on the label


def fill(t, ox, oy, w=10, h=40):
    for y in range(h):
        for x in range(w):
            n = fbm(x * 3, y * 2, 9930, 16, 64, 2, 5.0)
            v = 2.6 + (n - 0.5) * 1.4 - (0.6 if x >= w - 2 else 0) + (0.8 if x <= 1 else 0)
            c = BLOOD[v]
            if y == 0:
                c = BLOOD[5]                                     # the surface catching the light
            t.set(ox + x, oy + y, c)


def ring(t, ox, oy, state):
    """The dependency's ring (16x16): 0 calm (dull iron), 1 hungry (lit red), 2 withdrawal (cracked, black and burning)."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.0 <= d <= 7.4:
                rim = d > 6.6 or d < 5.6
                if state == 0:
                    c = IRON[4] if rim and (x + y) < 15 else IRON[2 + rim]
                elif state == 1:
                    c = BLOOD[5] if rim and (x + y) < 15 else BLOOD[3 + rim]
                else:
                    c = SMOKE[2] if h01("cr", x, y) < 0.6 else EMBER[3]
                    if abs(math.atan2(y - 7.5, x - 7.5) - 0.7) < 0.12 or abs(math.atan2(y - 7.5, x - 7.5) + 2.1) < 0.12:
                        continue                                  # the cracks right through it
                t.set(ox + x, oy + y, c)
            elif d < 3.4:
                c = (BLOOD[2], BLOOD[4], EMBER[3])[state]
                if d < 1.6:
                    c = (BLOOD[3], BLOOD[5], EMBER[5])[state]
                t.set(ox + x, oy + y, c)


def pip(t, ox, oy, lit_):
    for y in range(8):
        for x in range(8):
            # A small diamond.
            d = abs(x - 3.5) + abs(y - 3.5)
            if d <= 3.6:
                if d > 2.6:
                    c = IRON[1]
                else:
                    c = (BLOOD[5] if d < 1.2 else BLOOD[3]) if lit_ else IRON[3 if d < 1.2 else 2]
                t.set(ox + x, oy + y, c)


def hud_sheet():
    t = Tex(128, 64, 9930)
    vial(t, 0, 0)
    fill(t, 16, 0)
    for s in range(3):
        ring(t, 32 + 16 * s, 0, s)
    pip(t, 32, 16, False)
    pip(t, 40, 16, True)
    return t


# =====================================================================================================
# The vignette and the effect icon
# =====================================================================================================

def vignette():
    n = 256
    t = Tex(n, n, 9940)
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            d = math.hypot(dx * 0.92, dy)
            a = max(0.0, (d - 0.5) / 0.7)
            # Veins creeping in from the edge.
            ang = math.atan2(dy, dx)
            vein = abs(math.sin(ang * 9 + fbm(x, y, 9941, 256, 256, 3, 24.0) * 6))
            if vein < 0.07 and d > 0.38:
                a = max(a, 0.55 * min(1.0, (d - 0.38) / 0.3))
            if a <= 0:
                continue
            a = min(1.0, a) ** 1.4
            c = mix(BLOOD[3], hexc("#050000"), min(1.0, a * 0.9 + 0.1))
            t.set(x, y, (c[0], c[1], c[2], int(235 * a)))
    return t


def effect_icon():
    pal = {"k": hexc("#1E1410"), "g": GLASS[4], "G": GLASS[2], "r": BLOOD[3], "R": BLOOD[5], "c": CORK[3], "d": BLOOD[2]}
    return art([
        "..................",
        ".......kkkk.......",
        ".......kcck.......",
        "......kgkkGk......",
        "......kg..Gk......",
        ".....kg....Gk.....",
        "....kg......Gk....",
        "....kg..k...Gk....",
        "....kg...k..Gk....",
        "....kgrrrkrrGk....",
        "....kgRrrrkrGk....",
        "....kgrrrrrkGk....",
        ".....kgrrrrGk.....",
        "......kkkkkk......",
        "........r.........",
        "........R.........",
        ".......drd........",
        "..................",
    ], pal)


# =====================================================================================================
# The Panic Room door and the Cage Shackle
# =====================================================================================================

def door_half(top):
    t = Tex(16, 16, 9950 + top)
    for y in range(16):
        for x in range(16):
            n = fbm(x * 2, y * 2 + top * 16, 9951, 32, 32, 2, 6.0)
            c = IRON[2.6 + (n - 0.5) * 1.2]
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                c = IRON[1]                                       # the frame
            elif x in (1, 14) or (top and y == 1) or (not top and y == 14):
                c = IRON[4] if (x == 1 or y == 1) else IRON[2]
            if fbm(x * 3 + 5, y * 3, 9952 + top, 32, 32, 2, 5.0) > 0.78:
                c = mix(c, RUST[2], 0.5)
            t.set(x, y, c)
    # Rivets down both sides.
    for y in range(3, 15, 4):
        for x in (3, 12):
            t.set(x, y, IRON[5])
            t.set(x + 1, y + 1, IRON[0])
    if top:
        # The sliding peephole: a dark slot with a lit lip.
        for x in range(5, 11):
            t.set(x, 5, IRON[0])
            t.set(x, 6, hexc("#020203"))
            t.set(x, 7, IRON[5])
        t.set(5, 4, IRON[4])
        t.set(10, 4, IRON[4])
        # The wheel lock's spindle.
        t.set(7, 12, IRON[5]), t.set(8, 12, IRON[5]), t.set(7, 13, IRON[1]), t.set(8, 13, IRON[1])
    else:
        # A devil's trap welded on in red paint: a ring and the star.
        k = Ink(t)
        k.ring(7.75, 7.0, 4.6, 5.4, BLOOD[3])
        pts = [(7.75 + 4.6 * math.cos(math.radians(-90 + 144 * i)), 7.0 + 4.6 * math.sin(math.radians(-90 + 144 * i))) for i in range(6)]
        for (a, b) in zip(pts, pts[1:]):
            k.line(a[0], a[1], b[0], b[1], 0.7, BLOOD[4])
        # The wheel lock: a handwheel on the spindle.
        k.ring(7.75, 1.4, 1.6, 2.3, STEEL[3])
    return t


def door_item():
    t = Tex(16, 16, 9960)
    for y in range(1, 16):
        for x in range(4, 12):
            c = IRON[2.6 + 0.6 * math.sin(x + y * 0.5)]
            if x in (4, 11):
                c = IRON[4] if x == 4 else IRON[1]
            t.set(x, y, c)
    for x in range(6, 10):
        t.set(x, 4, hexc("#020203"))
    for y in (3, 7, 11, 14):
        t.set(5, y, IRON[5])
        t.set(10, y, IRON[5])
    k = Ink(t)
    k.ring(8, 10.5, 1.6, 2.4, BLOOD[4])
    item_outline(t, OUT)
    return t


def shackle():
    """A heavy black-iron cuff seen at an angle, its hinge and a broken link hanging off it; Enochian marks in faint red."""
    t = Tex(16, 16, 9970)
    k = Ink(t)
    # The cuff: a thick ellipse band.
    k.fill(lambda x, y: 0.42 <= ((x - 7.5) / 6.2) ** 2 + ((y - 7.0) / 4.6) ** 2 <= 1.0, lambda x, y: IRON[3.4 - 2.2 * (y - 2) / 10 + (0.8 if x < 6 else 0)])
    # The inside's shadow.
    k.fill(lambda x, y: ((x - 7.5) / 3.9) ** 2 + ((y - 7.2) / 2.6) ** 2 < 1.0, (0, 0, 0, 0))
    # The hinge bolt on the right; the broken link hanging below.
    k.disc(13.2, 7.0, 1.3, IRON[5])
    k.ring(10.5, 13.0, 1.3, 2.2, IRON[4])
    k.line(12.0, 14.6, 13.8, 15.4, 0.8, IRON[2])
    # Enochian marks along the band, faint red.
    for x, y in ((3, 5), (4, 9), (6, 11), (9, 11), (11, 10), (5, 3), (9, 2), (12, 4)):
        if t.get(x, y)[3]:
            t.set(x, y, EMBER[2] if (x + y) % 2 else BLOOD[4])
    item_outline(t, OUT)
    return t


def generate():
    for pid in ("blood_push", "blood_exorcism", "blood_smite"):
        save(icon(pid), "gui/allegiance/power", pid)
    save(hud_sheet(), "gui", "demon_blood")
    save(vignette(), "misc", "withdrawal_vignette")
    save(effect_icon(), "mob_effect", "withdrawal")
    save(door_half(True), "block", "panic_room_door_top")
    save(door_half(False), "block", "panic_room_door_bottom")
    save(door_item(), "item", "panic_room_door")
    save(shackle(), "item", "cage_shackle")


if __name__ == "__main__":
    generate()
