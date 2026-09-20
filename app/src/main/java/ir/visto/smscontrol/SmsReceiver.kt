package ir.visto.smscontrol

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** پیامک‌های ورودی را می‌گیرد و فقط پیام‌های شمارهٔ دستگاه را پردازش می‌کند. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Store.init(context)

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val from = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return

        SmsRepo.handleIncoming(context, from, body)
    }
}

/** نتیجهٔ ارسال پیامک (تحویل به اپراتور) را در لاگ ثبت می‌کند. */
class SendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        val status = when (resultCode) {
            android.app.Activity.RESULT_OK -> "ارسال شد"
            android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "خطا: آنتن/سرویس نیست"
            android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "خطا: رادیو خاموش است"
            android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "خطا: PDU نامعتبر"
            else -> "خطا در ارسال (کد $resultCode)"
        }
        Store.updateLastOutgoingStatus(status)
    }
}

object Notifier {
    private const val CHANNEL_ID = "visto_replies"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "پاسخ دستگاه", NotificationManager.IMPORTANCE_DEFAULT
            )
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    fun show(ctx: Context, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("پاسخ دستگاه")
            .setContentText(text.take(80))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n)
        } catch (_: SecurityException) { }
    }
}
