# FortniteInMinecraft

Server-authoritative Minecraft mechanics prototype generated from `brainage04/FabricModdingTemplate`, now using the template's server+client layout: common/server code in `src/main`, client-only code in `src/client`, and Fabric GameTests in `src/gametest`.

Current slice:
- Java 25 / Minecraft 26.2 Fabric server+client scaffold.
- Client entrypoint in `src/client`; server authority remains in the common `ModInitializer`.
- Core build data/state records.
- Old-mod-style build grid: 4-block stride, `4n + 1` centers, 5-block footprints with one-block overlaps.
- Canonical wall slots: north/west approach-side walls normalize to the same south/east build plane, so opposite-side placement cannot create duplicate parallel walls.
- Old-mod piece footprints: 5x5 walls/floors, one-block-thick diagonal stair surfaces, and roof-as-cone perimeter pyramids.
- Placement validation for occupied grid cells, any solid untracked world-block intersection, support, creative bypass, and survival resource spend.
- Overlap-aware block ownership: shared seam blocks keep owner sets; clearing one piece only removes blocks whose last owner is gone.
- Per-player build sessions for selected piece/shared material, old-mod-style facing/raycast targeting, and placement/turbo bookkeeping.
- Server+client build piece items for wall/floor/stair/roof in the `FortniteInMinecraft` creative tab; custom item model files keep tabs/icons textured without Polymer remapping.
- Client-only build previews render automatically while a build piece is held; no server preview fallback/debug renderer is registered.
- Naturally generated resource node clusters register wood/stone/metal harvest targets in newly generated chunks, with `/fim resource locate`, `/fim resource debug [radius]`, and `/fim resource clear_debug` for debug search/see-through markers.
- Debug server command adapter:
  - `/fim select <wall|floor|stair|roof> <wood|stone|metal>`
  - `/fim preview <block-pos>`
  - `/fim place <block-pos>`
  - `/fim session`
  - `/fim clear`
- `/fim preview` validates the candidate using the player's current horizontal facing and reports the authoritative result without rendering server-side preview particles or display entities.
- Client input packets drive gameplay intents: left-click shoots or places/holds turbo-build, right-click aims weapons or cycles build material, `R` reloads, and `G` toggles the targeted build between base and edited footprints.
- The client resource wallet HUD shows synchronized wood/stone/metal/gold icon counts on the right side, offset around the expected 17-line scoreboard area.
- `/fim place` commits authoritative build state with the player's current facing and materializes visible blocks (wood = oak planks, stone = cobblestone, metal = copper blocks). `/fim clear` removes tracked blocks owned by the player in the current dimension. Use creative mode for command/item smoke tests: it bypasses resource/support checks, but still rejects occupied grid cells and any solid untracked world-block intersection.
- Holding a build item applies experimental Fortnite-like movement tuning (higher step height, higher jump strength, faster horizontal movement). After a successful placement, if the new footprint intersects the player's lower or upper body AABB, the server phases the player upward to the first collision-free position above the placed blocks.

Recorded client GameTests require `ffmpeg`, `ffprobe`, Xvfb/`xdpyinfo`, and PipeWire tools (`pw-cli`, `wpctl`).

Run:

```shell
./gradlew test
./gradlew build
ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 \
  xvfb-run -a --server-args="-screen 0 1280x720x24" \
  ./gradlew --no-daemon runClientGameTest

ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1 \
  ./gradlew --no-daemon recordClientGameTest
```
