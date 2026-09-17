# FortniteInMinecraft icon

## What this is

The mod's icon: `icon.png` — 1024x1024 RGB PNG, 828 726 bytes,
sha256 `370a124e797a00580b81ae8df3939249ade069720c58ccbb4e5bb0909976dcb2`.

It shows a half-built Fortnite-style corner floating in a void: wood, brick and copper
structures with this mod's hologram blocks. It is an **offline CPU path trace of a
Minecraft world**, not an in-game screenshot and not a Blender render.

## How it was made

**Method: headless Chunky render from client-jar assets.** The world geometry is real
Minecraft 26.2 build data; the image is a physically based path trace computed on the CPU.

| | |
|---|---|
| Renderer | Chunky `2.5.0-SNAPSHOT.478.g527cb4a` = commit `527cb4a`, standalone, `PathTracingRenderer`, headless (`-Djava.awt.headless=true`), 4 threads, `-Xmx4G` |
| Projection | `PARALLEL` (orthographic), field of view 11.5 (blocks), `dof=Infinity`, no depth of field |
| Camera | position `(1.5, -58.5, 1.5)`, yaw `2.356194490192345 rad` (135°), pitch `-0.9553166181245093 rad` (≈ -54.74°, the isometric angle), `shift = (0, 0)` |
| Sun | altitude `0.8726646259971648 rad` (50°), azimuth `3.9269908169872414 rad` (225°), intensity 1.25, white |
| Sky | `SOLID_COLOR` with gradient, clouds **off**, uniform fog density 0, water opacity 0.42 |
| Sampling | `sppTarget 128` (rendered 130 batches), `rayDepth 12`, `pathTrace true`, `additionalData` empty, BVH `SAH_MA`, octree `PACKED` |
| Canvas | 1600x1600, `yClip -64..320`, output PNG, gamma post-process, render time 53 779 ms |
| Assets | Minecraft 26.2 solid textures plus this mod's own hologram block textures (`textures/real-fortnite.zip`, mip 4) |
| Mod code | the six materials `fortniteinminecraft:build_hologram_{stone,wood,metal}` are imported by a scene-local adapter (`tools/FortniteChunky.java`) that reuses Chunky's real `PathTracer` with one local editing branch (`tools/PathTracer.java`); `evidence/renderer-local.patch` is the complete diff against the upstream file kept in `evidence/PathTracer.original.java` (`chunky-dev/chunky@527cb4a`) |
| World | a void world (no ground) holding a half-built corner: 12 oak planks + 13 wood hologram, 10 bricks + 10 stone hologram, 8 waxed copper + 8 metal hologram cells → wood 48 %, brick 50 %, copper 50 % built; ground/terrain: none |
| Shader pack | none — the "shader" is the scene-local Chunky material branch described above, not an OptiFine/Iris pack |

The hologram materials are rendered with emissive, **flat-alpha** shading: hologram gain
1.0 (this icon), ior 1.0002930164337158 (= Chunky's air), specular 0, metalness 0. Reflected,
scattered and refracted hologram paths are switched off; the renderer asserts that
`hologramFresnel = hologramRefractions = hologramDiffuse = 0` for the final scene. That is the
point of this deliverable: the previously approved render still showed Fresnel reflections,
caused by the material IOR of 1.0 not matching Chunky's air IOR — see `diagnosis.json`.

The delivered image is `renders/fortnite-a-full.png` (the native 1600x1600 render) cropped
to the approved box `(182, 182, 1418, 1418)` and Lanczos-resampled to 1024x1024. Verified
while creating this provenance: that crop+resample reproduces `icon.png` byte for byte.

## Provenance files

| Path | What it is |
|---|---|
| `manifest.json` | Round-3 delivery record for `fortnite-a` and `fortnite-b`: method, source, gains, margins, and the 1024 re-output note |
| `scenes/fortnite-a.json` | **The scene actually rendered** — camera, sun, sky, materials, chunk list, sampling |
| `scenes/fortnite-empty.json` | The identical scene with an empty world, used as the crop control |
| `source-renders/fortnite-a-full.png` | The native 1600x1600 path-trace output the icon is cropped from |
| `source-renders/fortnite-empty-full.png` | The matching empty-world render (used by `crop()` as the difference reference) |
| `tools/render.py` | **The author script**: prepares and verifies the byte-identical inputs, compiles the adapter, renders both gains, crops, and writes `evidence/` |
| `tools/FortniteChunky.java` | Chunky entry point that registers the mod's materials from the imported texture pack |
| `tools/PathTracer.java`, `evidence/renderer-local.patch`, `evidence/PathTracer.original.java` | The scene-local hologram-only shading branch and its exact diff against Chunky `527cb4a` |
| `tools/MaterialProbe.java`, `evidence/material-probe-commands.json`, `evidence/renderer-bytecode.txt` | The material probe that found the real air IOR and the bytecode evidence for it |
| `tools/finalize.py` | Verification pass: re-checks every input hash, shader counters, CPU caps and crop margins, and writes the manifest and diagnosis |
| `tools/nbt.py`, `tools/anvil.py` | Minimal NBT/anvil readers used to census the world blocks and assert the void |
| `textures/real-fortnite.zip`, `textures/build_hologram_*.png` | The mod's hologram textures as imported by the renderer, plus the three corrected PNGs |
| `chunky-home/chunky.json` | The Chunky settings file of the render |
| `evidence/unchanged-inputs.json` | Proof that world, textures and libs are byte-identical to the approved inputs, plus the block census |
| `evidence/fortnite-a-input.json`, `evidence/fortnite-empty-input.json` | The exact scene files handed to the renderer |
| `evidence/margins.json` | Content bounds and crop box of the intermediate 1236 px crop |
| `evidence/commands-final.json` | Every command of the final render run with exit code, cgroup, `cpu.max`, `cpu.weight` and the shader counters |
| `diagnosis.json`, `evidence/root-cause-before-edit.json` | The Fresnel/refraction root cause and the pre-edit evidence |
| `cleanup.json`, `blockers.json` | Resource teardown record and the (empty) blocker list |

Excluded on purpose, with reasons in Notes: the Chunky library jars (`lib/`, 24 MB), the
compiled adapter (`classes/`), the 18 MB `.dump` scene caches, the probe run logs and scene
snapshots, the contact-sheet PNGs (`evidence/candidates-contact.png`, 1.9 MB;
`evidence/approved-flat-closeups.png`, 392 KB), the Minecraft world saves (`worlds/`), and
the session `logs/`.

## How to regenerate

The session ran headless in `<round3>/captures-fortnite-final`, and its `tools/render.py`
reads its approved baseline scene from a sibling directory `<round3>/captures-fortnite-fix`
(previous-round session, not shipped). After restoring `provenance/` as
`captures-fortnite-final` and providing `lib/` + `classes/` + `worlds/`:

```sh
cd captures-fortnite-final
python3 tools/render.py prepare   # verifies every copied input hash, censuses the world, compiles the adapter
python3 tools/render.py final     # renders fortnite-empty, fortnite-a (gain 1.0) and fortnite-b (gain 0.8), crops
python3 tools/finalize.py         # re-verifies and rewrites manifest.json / diagnosis.json
```

The 1024x1024 re-output of `fortnite-a` (the delivered file) is not scripted; it is one
Pillow crop+resample of the native render, recorded in the manifest `notes`:

```sh
python3 - <<'PY'
from PIL import Image
Image.open('renders/fortnite-a-full.png').convert('RGB') \
     .crop((182, 182, 1418, 1418)).resize((1024, 1024), Image.Resampling.LANCZOS) \
     .save('renders/fortnite-a.png')
PY
```

Prerequisites not shipped: the Chunky `2.5.0-SNAPSHOT.478.g527cb4a` jars and their
dependencies, the compiled `classes/`, the two void world saves, the Minecraft 26.2 client
assets, and the `captures-fortnite-fix` baseline scene. The scenes in `scenes/` and the
inputs in `evidence/` contain every setting needed to reconstitute them.

## Notes

* **This is a render, not an in-game capture.** The imagery comes from Chunky tracing the
  loaded Minecraft world with Minecraft's own block textures (extracted from the client jar)
  and this mod's hologram textures. No Minecraft client was running for it, no display, no
  GPU and no audio device were used.
* **`evidence/margins.json` describes the intermediate 1236x1236 crop**, not the delivered
  file (`sha256 97e79dc1…`, `dimensions [1236,1236]`). The delivered icon is the later
  1024x1024 re-output of the same box, recorded only in the manifest `notes`
  (`sha256 370a124e…`). Copying the stale hash over this file would be wrong.
* The mod's own textures were corrected for this render (the earlier brick hologram texture
  was invalid); `evidence/unchanged-inputs.json` proves they match the approved fix session.
* The adapter deliberately runs under a CPU cap (`cpu.max = 400000 100000`,
  `cpu.weight = 20`, `ActiveProcessorCount=4`); the checks in `tools/render.py` abort a run
  that exceeds it. This is why `evidence/commands-final.json` records those values per run.
* The 1600x1600 render is kept, but the intermediate 1236 px crop image itself
  (`renders/fortnite-a.png` at that size) was overwritten by the 1024 re-output and no
  longer exists.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
