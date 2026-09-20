package ir.visto.smscontrol

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

object SmsRepo {

    const val ACTION_SENT = "ir.visto.smscontrol.SMS_SENT"

    /** جانگهدارها را با مقادیر واقعی جایگزین می‌کند. */
    fun build(template: String, cfg: Config, ch: Int? = null, minutes: Int? = null): String =
        template
            .replace("{ch}", ch?.toString() ?: "")
            .replace("{min}", minutes?.toString() ?: "")
            .replace("{pw}", cfg.password)
            .replace("{name}", ch?.let { cfg.nameOf(it) } ?: "")
            .trim()

    fun hasSendPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * ارسال فرمان. اگر حالت شبیه‌ساز روشن باشد هیچ پیامکی ارسال نمی‌شود و
     * فقط یک پاسخ ساختگی بعد از ۱.۵ ثانیه تولید می‌گردد.
     * @return پیام خطا در صورت شکست، وگرنه null
     */
    fun send(ctx: Context, text: String, expect: Pair<Int, ChannelState>? = null): String? {
        val cfg = Store.config.value
        if (cfg.deviceNumber.isBlank()) return "شمارهٔ دستگاه تنظیم نشده است."
        if (text.isBlank()) return "متن فرمان خالی است. قالب فرمان را در تنظیمات پر کنید."

        Store.addLog(LogEntry(System.currentTimeMillis(), true, text, "در حال ارسال…"))
        expect?.let { Store.setPending(it.first, true) }

        if (cfg.simulator) {
            Store.updateLastOutgoingStatus("شبیه‌ساز")
            Handler(Looper.getMainLooper()).postDelayed({
                val reply = expect?.let {
                    "${cfg.nameOf(it.first)} ${if (it.second == ChannelState.ON) "روشن" else "خاموش"} شد"
                } ?: Store.channels.value.joinToString("\n") { c ->
                    "${c.name} ${if (c.state == ChannelState.ON) "روشن" else "خاموش"}"
                }
                handleIncoming(ctx, cfg.deviceNumber, reply)
            }, 1500)
            return null
        }

        if (!hasSendPermission(ctx)) {
            Store.updateLastOutgoingStatus("مجوز ارسال پیامک داده نشده")
            Store.clearPending()
            return "اجازهٔ ارسال پیامک داده نشده است."
        }

        return try {
            val sm = managerFor(ctx, cfg.subscriptionId)
            val parts = sm.divideMessage(text)
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            repeat(parts.size) { i ->
                val intent = Intent(ACTION_SENT).setPackage(ctx.packageName)
                sentIntents.add(
                    PendingIntent.getBroadcast(
                        ctx, i, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            sm.sendMultipartTextMessage(cfg.deviceNumber, null, parts, sentIntents, null)
            // اگر تا ۱۲۰ ثانیه پاسخی نیامد، حالت «در انتظار» را پاک کن
            Handler(Looper.getMainLooper()).postDelayed({ Store.clearPending() }, 120_000)
            null
        } catch (e: Exception) {
            Store.updateLastOutgoingStatus("خطا: ${e.message}")
            Store.clearPending()
            "ارسال ناموفق بود: ${e.message}"
        }
    }

    /** مسیر جایگزین بدون مجوز: باز کردن اپ پیام‌رسان با متن آماده. */
    fun sendViaDefaultApp(ctx: Context, text: String) {
        val cfg = Store.config.value
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${cfg.deviceNumber}"))
        i.putExtra("sms_body", text)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        Store.addLog(LogEntry(System.currentTimeMillis(), true, text, "به اپ پیام‌رسان سپرده شد"))
    }

    /** ورودی مشترک برای پیامک واقعی و پاسخ شبیه‌سازی‌شده. */
    fun handleIncoming(ctx: Context, from: String, body: String) {
        val cfg = Store.config.value
        if (!Parser.sameNumber(from, cfg.deviceNumber)) return
        Store.addLog(LogEntry(System.currentTimeMillis(), false, body))
        val parsed = Parser.parse(body, cfg)
        parsed.forEach { (ch, st) -> Store.setState(ch, st) }
        if (parsed.isEmpty()) Store.clearPending()
        Notifier.show(ctx, body)
    }

    @Suppress("DEPRECATION")
    private fun managerFor(ctx: Context, subId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = ctx.getSystemService(SmsManager::class.java)
            if (subId >= 0) base.createForSubscriptionId(subId) else base
        } else {
            if (subId >= 0) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }

    /** فهرست سیم‌کارت‌های فعال برای انتخاب در تنظیمات. */
    fun sims(ctx: Context): List<SubscriptionInfo> = try {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val sm = ctx.getSystemService(SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList ?: emptyList()
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
