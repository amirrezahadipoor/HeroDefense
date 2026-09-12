package com.amirrezahadipoor.herodefense;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import org.junit.jupiter.api.Test;

final class ProjectSkeletonTest {
    @Test
    void gameEntryPointUsesLibGdxApplicationAdapter() {
        assertTrue(ApplicationAdapter.class.isAssignableFrom(HeroDefenseGame.class));
    }
}
