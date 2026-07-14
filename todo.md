# FortniteInMinecraft TODO

## Visual follow-ups

- [ ] Tile the wall hologram preview across a 5×5 grid of 25 block-sized texture cells instead of stretching one block texture across the wall.
  - The current preview uses client-only vanilla `BlockDisplay` entities, not block entities. Do not convert it to block entities solely for this task; a single-display solution is optional only if it still produces the requested 25-tile visual.

- [ ] Replace the custom Fortnite HUD element implementations with HudRendererLib.
  - Migrate `ClientResourceWalletHud`, `ClientBuildPieceHud`, `ClientLootContainerProgressHud`, and the custom hotbar-selection HUD hook in `ClientBuildHooks`.
  - Preserve the current vanilla-element ordering, visibility rules, layout, rendering, and network-driven state behavior; remove the direct `HudElementRegistry` registrations after parity is verified.
