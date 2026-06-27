package io.github.brainage04.fortniteinminecraft.server.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPreviewTickerTest {
    @Test
    void rendersOnlyOnPreviewInterval() {
        assertFalse(BuildPreviewTicker.shouldRender(1));
        assertFalse(BuildPreviewTicker.shouldRender(2));
        assertFalse(BuildPreviewTicker.shouldRender(3));
        assertTrue(BuildPreviewTicker.shouldRender(4));
        assertTrue(BuildPreviewTicker.shouldRender(8));
    }
}
