package com.amirrezahadipoor.herodefense.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class IdleWhisperRendererTest {
    @Test
    void envelopeFadesInHoldsThenFadesOut() {
        assertEquals(0f, IdleWhisperRenderer.alphaFor(0f));
        assertEquals(1f, IdleWhisperRenderer.alphaFor(1f));
        assertEquals(1f, IdleWhisperRenderer.alphaFor(2f));
        assertEquals(0f, IdleWhisperRenderer.alphaFor(IdleWhisperRenderer.SHOW_SECONDS));
        assertEquals(0f, IdleWhisperRenderer.alphaFor(99f));
    }

    @Test
    void envelopeRampsLinearlyAtBothEnds() {
        assertEquals(0.5f, IdleWhisperRenderer.alphaFor(0.225f), 0.001f);
        assertEquals(
            0.5f,
            IdleWhisperRenderer.alphaFor(IdleWhisperRenderer.SHOW_SECONDS - 0.35f),
            0.001f
        );
    }
}
