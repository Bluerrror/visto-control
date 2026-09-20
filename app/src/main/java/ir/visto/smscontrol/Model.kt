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
    val password: String = "",
    val channelCount: Int = 3,
    val channelNames: List<String> = listOf("کانال ۱", "کانال ۲", "کانال ۳"),
    val cmdOn: String = "ON{ch}",
    val cmdOff: String = "OFF{ch}",
    val cmdStatus: String = "STATUS",
    val cmdTimer: String = "ON{ch}#{min}",
    val cmdAllOff: String = "",           // خالی = تک‌تک کانال‌ها خاموش می‌شوند
    val onWords: String = "ON,روشن,وصل,باز",
    val offWords: String = "OFF,خاموش,قطع,بسته",
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
