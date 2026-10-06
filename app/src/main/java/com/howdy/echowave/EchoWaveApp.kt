package com.howdy.echowave

import android.app.Application

/**
 * Manual DI holder (Hilt avoided: Hilt Gradle plugin is incompatible with
 * AGP 9.4.1 — "Android BaseExtension not found". Same seams, zero magic.
 * Reintroduce Hilt later only after pinning an AGP/Hilt matrix.)
 */
class EchoWaveApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Eager cold-start work (BotGuard prewarm + visitor bootstrap).
        // Lazy init here cost ~20 s on first tap; now it amortizes at launch.
        container.startup()
    }
}
