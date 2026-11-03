package com.liangji.bodymeasure

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w393dp-h852dp-xxhdpi")
class AppLaunchTest {

    @Test
    fun launchesIntoHome() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()

        val activity = controller.get()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.window.decorView.width > 0)
        assertTrue(activity.window.decorView.height > 0)

        activity.window.decorView.captureRoboImage("build/screenshots/app-launch.png")

        controller.pause().stop().destroy()
    }
}
