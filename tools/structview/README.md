# structview

Renders a dumped build layout (`build/layouts/<name>.json`, written by `LayoutDumpTest`) to PNGs with the real vanilla
1.21.1 block models and textures, so build quality can be judged without starting the game. Python 3 stdlib only.

```bash
./gradlew test --tests '*LayoutDumpTest'                          # writes build/layouts/*.json
python tools/structview/structview.py build/layouts/kit_cottage.json --out build/layouts/png
python tools/structview/structview.py build/layouts/kit_cottage.json --detail -8,-6,7,10 --ymin -2  # textured close-up
python tools/structview/structview.py build/layouts/kit_cottage.json --views front --target house  # eye height at a building
python tools/structview/structview.py build/layouts/kit_cottage.json --cutaway y=3 --views iso_sw  # see inside
python tools/structview/structview.py build/layouts/kit_cottage.json --zone ground_floor
python tools/structview/structview.py build/layouts/kit_cottage.json --eye 0,1.6,14 --look 0,2,0  # free camera
python tools/structview/structview.py --extract-ids --out src/test/resources/vanilla_blocks_1_21_1.txt
```

## Views and outputs

| view | camera |
|---|---|
| `iso_ne`, `iso_sw` (also `iso_nw`, `iso_se`) | orthographic, from that corner, 30° down |
| `top` | orthographic, straight down, north up; 8-block grid (red lines through 0), lower surfaces darker |
| `front` | perspective, 70° vertical FOV, from `--front-from` (south by default). Whole build: eye 1.6 above `anchors.entry` y (else y=0), outside its bounds. With `--target ZONE` (or `--zone`): framed on that zone's box, ~1.2× its width from its centre, standing on the terrain there (else on the zone's floor, min y + 1) |
| `--eye x,y,z --look x,y,z` | free perspective camera (`<name>_eye.png`) |

Files are `<name>_<view>[_cut<H>][_from<Y>][_<zone>][_at_<target>].png`; `--detail x0,z0,x1,z1` writes
`<name>_detail[...].png` (those columns, `--detail-view`, `--detail-scale`, default auto ~1400 px wide at 16..48 px/block,
always textured).

Options: `--views`, `--scale N` (px/block of orthographic views; default auto: ~1400 px wide, 2..32), `--mode
auto|flat|textured` (auto = textured from 8 px/block; flat = the texture's average colour per face, still alpha-tested
for plants and leaves), `--textures`, `--cutaway y=H` (drop cells above H), `--ymin H` (drop cells below H), `--zone NAME` (only that zone's
cells), `--target NAME` (front view framed on a zone, all cells kept), `--front-from`, `--distance`,
`--size WxH` (perspective images), `--no-ao`, `--no-shadow`, `--jar` (client jar; default
`~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`, else searched under `~/.gradle`).

## How it draws

- `models.py` resolves blockstate variants (missing properties take vanilla's defaults from the block report) and
  multipart cases, the model parent chain and `#texture` references, element rotations (with `rescale`), face UV
  rotations and the variant's x/y rotations, exactly as vanilla bakes them.
- `shapes.py` approximates what vanilla draws with block-entity renderers: chests, beds, signs (standing, wall,
  hanging), banners, skulls, decorated pots, shulker boxes, conduits, the bell's body; fluids (surface at 14/16 unless
  the same fluid is above, translucent tinted water). Invisible: air, `light`, `barrier`, `structure_void`.
  Unknown blocks are magenta cubes; `supernaturalcraft:` blocks are read from `src/main|generated/resources`.
- `structview.py`: z-buffered triangle rasteriser with perspective-correct UVs and near-plane clipping; faces culled
  against opaque full cubes (and same-block glass/ice); vanilla face shading (top 1, N/S 0.8, E/W 0.6, bottom 0.5),
  per-vertex ambient occlusion on cell-aligned faces, a cast sun shadow (sun toward +x, +y, +z), alpha cutout, and a
  back-to-front translucent pass (water, stained glass, ice, slime, honey).
- Decor: paintings (vanilla textures and sizes, centred as vanilla), item frames (with the item's sprite), armour
  stands (a stand, a helmet and a chestplate block when given); other kinds are ignored.
- `pngx.py`: PNG reader (colour types 0/2/3/4/6, bit depths 1-16, palette + tRNS, all filters; not interlaced) and writer.
- `blockcolors.py`: assets from the jar and the repo, textures (first frame of animated strips), average colours,
  fixed tints (plains grass/foliage, birch, spruce, water, lily pad, redstone power) and the block report.

## The dump format

```json
{"name": "kit_cottage", "bounds": {"min": [x, y, z], "max": [x, y, z]},
 "cells": [[x, y, z, "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"], ...],
 "decor": [{"kind": "PAINTING", "x": 0, "y": 1, "z": 0, "facing": "south", "data": "kebab", "extra": ""}],
 "anchors": {"entry": [x, y, z]},
 "zones": [{"name": "ground_floor", "min": [x, y, z], "max": [x, y, z], "anchors": {}}]}
```

X east, Y up, Z south. Unlisted cells and `minecraft:air` cells are drawn as nothing.

## `--extract-ids`

Writes the vanilla block list used by the Java `BlockIds` check: two `#` header lines, then one block per line, sorted,
`<id> <property>=<v1>|<v2>...` (properties and values sorted). Properties come from the data generator's
`reports/blocks.json` (`java -DbundlerMainClass=net.minecraft.data.Main -jar minecraft_1.21.1_server.jar --reports`,
run once and cached in the system temp dir as `structview_cache/blocks_1_21_1.json`); without Java or the server jar it
falls back to the ids of the client jar's blockstate files, without properties.

## Limitations

- Fixed plains tints (no biome colours); no block light, no emissive glow; the sun shadow is one value per face.
- `uvlock` is ignored (rotated stairs/slabs may show their texture turned, never their shape).
- Entity-rendered blocks are boxes in the right place and colour, not their real models; banner patterns, sign text
  and item-frame rotation are not drawn; waterlogged blocks are drawn without their water.
- Animated textures show their first frame; connected/random textures beyond weighted variants are not modelled.
