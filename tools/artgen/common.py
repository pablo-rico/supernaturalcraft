"""Helpers shared by every texture module."""

import os

from pixelkit import Tex

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "supernaturalcraft")
TEXTURES = os.path.join(ASSETS, "textures")

WRITTEN = []


def out(kind, name):
    return os.path.join(TEXTURES, kind, name + ".png")


def save(t, kind, name):
    path = out(kind, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    t.save(path)
    WRITTEN.append(path)
    return path


def art(rows, pal, w=None, h=None, seed=0):
    """Stamps string art (one char per pixel) onto a fresh canvas."""
    w = w or len(rows[0])
    h = h or len(rows)
    for i, r in enumerate(rows):
        if len(r) != w:
            raise ValueError(f"art row {i} is {len(r)} wide, expected {w}: {r!r}")
    t = Tex(w, h, seed)
    t.stamp(rows, pal)
    return t
