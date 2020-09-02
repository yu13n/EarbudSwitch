package app.tuuure.earbudswitch

import android.app.Application

class EBSApp : Application() {
    companion object {
        private lateinit var _context: Application
        fun getContext(): Application {
            return _context
        }
    }

    override fun onCreate() {
        super.onCreate()
        _context = this;
    }
}