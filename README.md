# FortniteInMinecraft

Server-authoritative Minecraft mechanics prototype generated from `brainage04/FabricModdingTemplate` with `--side=server`.

Current slice:
- Java 25 / Minecraft 26.2 Fabric server-side scaffold.
- No `src/client` source set.
- Core build data/state records.
- Old-mod-style build grid: 4-block stride, `4n + 1` centers, 5-block footprints with one-block overlaps.
- Canonical wall slots: north/west approach-side walls normalize to the same south/east build plane, so opposite-side placement cannot create duplicate parallel walls.
- Old-mod piece footprints: 5x5 walls/floors, one-block-thick diagonal stair surfaces, and roof-as-cone perimeter pyramids.
- Placement validation for occupied slots, mostly obstructed footprints, support, creative bypass, and survival resource spend.
- Overlap-aware block ownership: shared seam blocks keep owner sets; clearing one piece only removes blocks whose last owner is gone.
- Per-player build sessions for selected piece/shared material, preview mode, old-mod-style facing/raycast targeting, and placement/turbo bookkeeping.
- Polymer server-side build piece items for wall/floor/stair/roof in the `FortniteInMinecraft` creative tab; item appearance follows the player's shared selected material.
- Debug server command adapter:
  - `/fim select <wall|floor|stair|roof> <wood|stone|metal>`
  - `/fim preview-mode <particles|glass>`
  - `/fim preview <block-pos>`
  - `/fim place <block-pos>`
  - `/fim session`
  - `/fim clear`
- `/fim preview` stores a preview candidate using the player's current horizontal facing and renders either blue/red dust particles or non-colliding light-blue/red stained-glass block-display holograms. Glass mode insets displays slightly to avoid coplanar z-fighting, batches wall/floor previews into one stretched display, and batches stair previews into five row displays.
- Polymer build items select their piece when held; right-click places the current raycast/facing target and keeps a short server-side turbo-build window alive so held right-click packets can drive faster-than-vanilla placement cadence. Left-clicking a block cycles the shared material (`wood -> stone -> metal -> wood`) for every build piece. Server-only Fabric still cannot receive vanilla left-click-air packets, so the swap makes air placement viable through right-click/use-item packets instead.
- `/fim place` commits authoritative build state with the player's current facing and materializes visible blocks (wood = oak planks, stone = cobblestone, metal = copper blocks). `/fim clear` removes tracked blocks owned by the player in the current dimension. Use creative mode for command/item smoke tests: it bypasses resource/support checks, but still rejects occupied slots and mostly obstructed footprints.
- Movement speed, jump height, and step height are not modified yet; the current priority is validating placement/input feel before applying player attribute changes to compensate for the 4-block stride and 5-block footprints.

Run:

```shell
./gradlew test
./gradlew build
```
