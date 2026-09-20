package ir.visto.smscontrol

/**
 * الگوهای رایجِ فرمان در کنترلرهای پیامکی ایرانی.
 * این‌ها «حدس» هستند، نه دفترچهٔ رسمی ویستو. برای پیدا کردن قالب درست،
 * اول فرمان «وضعیت» هر الگو را امتحان کنید چون خروجی را تغییر نمی‌دهد.
 */
data class Preset(
    val title: String,
    val on: String,
    val off: String,
    val status: String,
    val timer: String
)

object Presets {
    val all = listOf(
        Preset("ON1 / OFF1", "ON{ch}", "OFF{ch}", "STATUS", "ON{ch}#{min}"),
        Preset("1ON / 1OFF", "{ch}ON", "{ch}OFF", "STATUS", "{ch}ON{min}"),
        Preset("با رمز — 1234*ON*1", "{pw}*ON*{ch}", "{pw}*OFF*{ch}", "{pw}*STATUS", "{pw}*ON*{ch}*{min}"),
        Preset("با # — #ON1#", "#ON{ch}#", "#OFF{ch}#", "#STATUS#", "#ON{ch}#{min}#"),
        Preset("RELAY1ON", "RELAY{ch}ON", "RELAY{ch}OFF", "RELAYSTATUS", "RELAY{ch}ON{min}"),
        Preset("فارسی — روشن 1", "روشن {ch}", "خاموش {ch}", "وضعیت", "روشن {ch} {min}"),
        Preset("عددی — 1*1", "{ch}*1", "{ch}*0", "0*0", "{ch}*1*{min}")
    )
}
