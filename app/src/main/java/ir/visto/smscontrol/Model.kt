package ir.visto.smscontrol

/** وضعیت هر کانال (خروجی) دستگاه */
enum class ChannelState { UNKNOWN, ON, OFF }

data class Channel(
    val index: Int,
    val name: String,
    val state: ChannelState = ChannelState.UNKNOWN,
    val updatedAt: Long = 0L,
    val pending: Boolean = false
)

/**
 * تمام تنظیمات دستگاه. قالب فرمان‌ها (cmd*) را باید از روی دفترچهٔ راهنمای
 * دستگاه خودتان پر کنید. جانگهدارها:
 *   {ch}   شمارهٔ کانال (۱ تا n)
 *   {min}  مدت زمان به دقیقه
 *   {pw}   رمز دستگاه
 *   {name} نام کانال
 */
data class Config(
    val deviceNumber: String = "",
    val password: String = "9865",        // رمز پیش‌فرض کارخانهٔ ویستو
    val channelCount: Int = 3,
    val channelNames: List<String> = listOf("کانال ۱", "کانال ۲", "کانال ۳"),
    // قالب‌های تأییدشدهٔ ویستو/تکنواسمارت (منبع: بلاگ رسمی ویستو).
    // نمونه: (۱)On روشن، (۳)Off خاموش، (۱)۲۰ روشن به‌مدت ۲۰ دقیقه، Off خاموشی همه.
    val cmdOn: String = "({ch})On",
    val cmdOff: String = "({ch})Off",
    val cmdStatus: String = "9865",       // استعلام وضعیت در دفترچهٔ رسمی نیامده — این یک حدس است
    val cmdTimer: String = "({ch}){min}",
    val cmdAllOff: String = "Off",        // خاموش کردن همهٔ خروجی‌ها با یک فرمان
    val onWords: String = "On,روشن,وصل,باز,فعال",
    val offWords: String = "Off,خاموش,قطع,بسته,غیرفعال",
    val subscriptionId: Int = -1,          // -1 یعنی سیم‌کارت پیش‌فرض
    val confirm: Boolean = true,
    val simulator: Boolean = false
) {
    fun nameOf(ch: Int): String = channelNames.getOrNull(ch - 1)?.takeIf { it.isNotBlank() } ?: "کانال $ch"
}

data class LogEntry(
    val time: Long,
    val outgoing: Boolean,
    val text: String,
    val status: String = ""
)
