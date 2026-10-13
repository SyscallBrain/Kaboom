package pt.tiago.kaboom

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
