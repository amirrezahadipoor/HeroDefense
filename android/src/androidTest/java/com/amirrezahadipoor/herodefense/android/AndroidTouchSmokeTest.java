package com.amirrezahadipoor.herodefense.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Launches the real libGDX activity and drives it exclusively with touch gestures. */
@RunWith(AndroidJUnit4.class)
public final class AndroidTouchSmokeTest {
    @Test
    public void appLaunchesAndAcceptsTapAndDrag() {
        try (ActivityScenario<AndroidLauncher> ignored = ActivityScenario.launch(AndroidLauncher.class)) {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            device.waitForIdle();

            int width = device.getDisplayWidth();
            int height = device.getDisplayHeight();
            assertTrue(device.click(width / 2, height * 3 / 4));
            assertTrue(device.swipe(width / 3, height / 2, width * 2 / 3, height / 2, 12));

            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(
                "com.amirrezahadipoor.herodefense.debug",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()
            );
        }
    }
}
