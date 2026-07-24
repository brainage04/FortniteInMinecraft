# FortniteInMinecraft TODO

## Visual follow-ups

- [x] Tile the wall hologram preview across a 5×5 grid of 25 block-sized texture cells instead of stretching one block texture across the wall.
  - Kept the preview client-only with vanilla `BlockDisplay` entities. Wall footprint cells now produce 25 unscaled one-block displays; valid/invalid states and non-wall face-merged previews are unchanged.
  - Evidence: `xvfb-run -a --server-args="-screen 0 1280x720x24" ./gradlew --no-daemon :fabric:runClientGameTest` passed on 2026-07-20, including valid and invalid WALL recordings and the assertion that exactly 25 client displays render.
  - Fresh showcase recording `build/recordings/client-gametest-20260720-050303.mp4` completed with Gradle status 0 and no recording failures. Native-size frame `build/recordings/wall-preview-valid-crop.png` visibly shows the wall as a 5×5 grid rather than one stretched texture; `wall-preview-invalid.png` confirms the invalid-wall path.

- [x] Replace the custom Fortnite HUD element implementations with HudRendererLib.
  - Migrated the current resource-wallet/Fortnite hotbar (`ClientFortniteHud`), `ClientBuildPieceHud`, `ClientLootContainerProgressHud`, and `ClientBuildHooks` hotbar-selection suppression to loader-neutral HudRendererLib layers. Fabric and NeoForge initialize their respective HudRendererLib platforms before shared client hooks; direct `HudElementRegistry` and `RegisterGuiLayersEvent` registrations were removed.
  - Evidence: Fabric and NeoForge client compilation passed, and the focused Fabric client GameTest passed with assertions for all four HudRendererLib layers, their vanilla-relative order, synchronized resource/progress state, and build-selection visibility.
  - Fresh runtime frame `build/recordings/hudrenderer-parity.png` shows the migrated resource, build-selection, loot-progress, and hotbar layers together during the passing `hud.layer_parity` scenario.
