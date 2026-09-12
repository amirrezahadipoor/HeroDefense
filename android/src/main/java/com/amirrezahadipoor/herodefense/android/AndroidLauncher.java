package com.amirrezahadipoor.herodefense.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.amirrezahadipoor.herodefense.HeroDefenseGame;

public final class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        configuration.useAccelerometer = false;
        configuration.useCompass = false;
        initialize(new HeroDefenseGame(), configuration);
    }
}
