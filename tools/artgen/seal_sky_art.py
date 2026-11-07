"""The seals' omens in the sky (v0.40). Contract: SealAssets.RED_MOON / BLOOD_SKY.

  textures/environment/red_moon.png   128x64, vanilla's moon_phases.png layout: 4 columns x 2 rows of 32x32 cells, phase p at
                                      cell (p % 4, p / 4), 0 = full, 4 = new (vanilla's order), so the code draws it like the
                                      vanilla moon (and with its additive blend: black/clear outside the disc). A blood-red
                                      disc of radius 11 with darker maria, a hot rim; its unlit side a faint red earthshine.
  textures/environment/blood_sky.png  256x256 RGBA, seamless left-to-right (and top-to-bottom): long streaks of dark red
                                      running down the sky, thinning and fading, alpha up to ~150 -- an overlay drawn over the
                                      sky dome (translucent) from the third stage of broken seals.
"""

import math

from chuck_art import h01
from common import save
from pixelkit import Ramp, Tex, fbm, hexc, mix

MOON = Ramp("#3a0604", "#62090a", "#8c140e", "#b42416", "#d84028", "#f87a50")


def red_moon():
    t = Tex(128, 64, 43001)
    R = 11.0
    for p in range(8):
        ox, oy = (p % 4) * 32, (p // 4) * 32
        for y in range(32):
            for x in range(32):
                dx, dy = x + 0.5 - 16, y + 0.5 - 16
                r = math.hypot(dx, dy)
                if r > R + 0.5:
                    continue
                half = math.sqrt(max(0.01, R * R - dy * dy))
                nx = dx / half
                if p <= 4:
                    lit = nx < math.cos(p * math.pi / 4)
                else:
                    lit = nx > -math.cos((8 - p) * math.pi / 4)
                n = fbm(x * 2, y * 2, 43002, 64, 64, 3, 6.0)       # the same maria in every phase
                v = 3.3 + (n - 0.5) * 2.0 - 1.2 * (r / R) ** 3
                if fbm(x + 7, y + 3, 43003, 32, 32, 2, 4.0) > 0.62:
                    v -= 1.0                                        # a mare
                c = MOON[max(0, min(5, int(round(v))))]
                if r > R - 0.8:
                    c = mix(c, MOON[5], 0.5)                        # the hot rim
                if not lit:
                    c = mix(hexc("#000000"), c, 0.12)               # earthshine
                t.set(ox + x, oy + y, c)
    return t


def blood_sky():
    W = 256
    t = Tex(W, W, 43101)
    for x in range(W):
        # Each column belongs to a streak: its strength, where it starts and how far it runs (seamless: periodic noise).
        s = fbm(x, 0, 43102, W, W, 2, 64.0)
        s2 = fbm(x, 7, 43103, W, W, 2, 64.0)
        strength = max(0.0, (s - 0.42) * 2.4) * (0.6 + 0.8 * s2)
        start = (fbm(x, 11, 43104, W, W, 1, 32.0) * 2.0 % 1.0) * W
        run = 60 + 180 * fbm(x, 13, 43105, W, W, 2, 16.0)
        for y in range(W):
            u = ((y - start) % W) / run
            if u > 1 or strength <= 0:
                haze = fbm(x, y, 43106, W, W, 3, 8.0)
                if haze > 0.6:
                    a = int(40 * (haze - 0.6) / 0.4)
                    if a > 2:
                        t.set(x, y, (90, 6, 8, a))
                continue
            drip = (1 - u) ** 1.3 * (1.0 if u > 0.04 else u / 0.04)
            n = fbm(x, y, 43107, W, W, 2, 32.0)
            a = 150 * strength * drip * (0.75 + 0.5 * n)
            if a < 3:
                continue
            c = mix(hexc("#5a0406"), hexc("#a8140e"), min(1.0, strength * drip))
            if u > 0.97 and h01("bead", x) < 0.5:
                c, a = hexc("#c41c14"), min(200, a * 2 + 40)       # the bead at a streak's end
            t.set(x, y, (c[0], c[1], c[2], int(min(160, a))))
    return t


def generate():
    save(red_moon(), "environment", "red_moon")
    save(blood_sky(), "environment", "blood_sky")


if __name__ == "__main__":
    generate()
