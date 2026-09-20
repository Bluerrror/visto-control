package ir.visto.smscontrol

import android.app.Application
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifier.ensureChannel(this)
        ContextCompat.registerReceiver(
            this,
            SendResultReceiver(),
            IntentFilter(SmsRepo.ACTION_SENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
