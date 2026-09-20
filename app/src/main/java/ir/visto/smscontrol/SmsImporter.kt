package ir.visto.smscontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class ImportedSms(
    val body: String,
    val outgoing: Boolean,
    val date: Long
)

/**
 * پیامک‌های ردوبدل‌شده با شمارهٔ دستگاه را از حافظهٔ خود گوشی می‌خواند
 * تا بتوان از روی پیام‌های واقعیِ اپ رسمی، قالب فرمان‌ها را ساخت.
 */
object SmsImporter {

    fun canRead(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun load(ctx: Context, deviceNumber: String, limit: Int = 200): List<ImportedSms> {
        if (!canRead(ctx) || deviceNumber.isBlank()) return emptyList()

        val tail = Parser.normalizeDigits(deviceNumber).filter { it.isDigit() }.takeLast(9)
        if (tail.isEmpty()) return emptyList()

        val out = mutableListOf<ImportedSms>()
        try {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%$tail"),
                "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                while (c.moveToNext() && out.size < limit) {
                    val body = c.getString(iBody) ?: continue
                    if (body.isBlank()) continue
                    out.add(
                        ImportedSms(
                            body = body,
                            outgoing = c.getInt(iType) == Telephony.Sms.MESSAGE_TYPE_SENT,
                            date = c.getLong(iDate)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    /**
     * یک پیامک واقعی را به قالب تبدیل می‌کند: عددِ کانال با {ch}،
     * عددِ دقیقه با {min} و رمز با {pw} جایگزین می‌شود.
     */
    fun toTemplate(body: String, channel: Int?, minutes: Int?, password: String): String {
        var t = Parser.normalizeDigits(body).trim()
        if (password.isNotBlank()) t = t.replace(password, "{pw}")
        if (minutes != null && minutes > 0) t = replaceWholeNumber(t, minutes, "{min}")
        if (channel != null && channel > 0) t = replaceWholeNumber(t, channel, "{ch}")
        return t
    }

    /** فقط عددِ کامل را جایگزین می‌کند تا مثلاً ۱ داخل ۱۲ خراب نشود. */
    private fun replaceWholeNumber(text: String, value: Int, token: String): String {
        val re = Regex("(?<!\\d)" + Regex.escape(value.toString()) + "(?!\\d)")
        return re.replaceFirst(text, Regex.escapeReplacement(token))
    }
}
