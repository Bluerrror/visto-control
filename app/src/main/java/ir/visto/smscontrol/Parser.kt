package ir.visto.smscontrol

/**
 * پیامک پاسخِ دستگاه را به وضعیت کانال‌ها تبدیل می‌کند.
 * چون قالب پاسخ هر دستگاه فرق می‌کند، چند حالت رایج پوشش داده شده و
 * کلیدواژه‌ها از تنظیمات خوانده می‌شود.
 */
object Parser {

    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            val fa = FA_DIGITS.indexOf(c)
            val ar = AR_DIGITS.indexOf(c)
            sb.append(
                when {
                    fa >= 0 -> ('0' + fa)
                    ar >= 0 -> ('0' + ar)
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** فقط ۱۰ رقم آخر شماره را مقایسه می‌کند تا +98 / 0098 / 09 فرقی نکند. */
    fun sameNumber(a: String, b: String): Boolean {
        val x = normalizeDigits(a).filter { it.isDigit() }.takeLast(10)
        val y = normalizeDigits(b).filter { it.isDigit() }.takeLast(10)
        return x.isNotEmpty() && x == y
    }

    fun parse(raw: String, cfg: Config): Map<Int, ChannelState> {
        val text = normalizeDigits(raw)
        val onWords = cfg.onWords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val offWords = cfg.offWords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val result = linkedMapOf<Int, ChannelState>()

        // --- گذر اول: هر سطر/بخش را جدا بررسی کن ---
        val segments = text.split('\n', '\r', ',', '،', ';', '؛', '|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (seg in segments) {
            val state = when {
                offWords.any { seg.contains(it, ignoreCase = true) } -> ChannelState.OFF
                onWords.any { seg.contains(it, ignoreCase = true) } -> ChannelState.ON
                else -> null
            } ?: continue

            var idx: Int? = null
            for (i in 1..cfg.channelCount) {
                val n = cfg.channelNames.getOrNull(i - 1)
                if (!n.isNullOrBlank() && seg.contains(n, ignoreCase = true)) { idx = i; break }
            }
            if (idx == null) {
                idx = Regex("\\d+").findAll(seg)
                    .mapNotNull { it.value.toIntOrNull() }
                    .firstOrNull { it in 1..cfg.channelCount }
            }
            if (idx != null) result[idx] = state
        }

        if (result.isNotEmpty()) return result

        // --- گذر دوم: اگر دقیقاً به تعداد کانال‌ها کلیدواژه پیدا شد، به ترتیب نسبت بده ---
        val hits = mutableListOf<Pair<Int, ChannelState>>()
        fun collect(words: List<String>, st: ChannelState) {
            for (w in words) {
                var i = text.indexOf(w, 0, ignoreCase = true)
                while (i >= 0) {
                    hits.add(i to st)
                    i = text.indexOf(w, i + 1, ignoreCase = true)
                }
            }
        }
        collect(onWords, ChannelState.ON)
        collect(offWords, ChannelState.OFF)
        hits.sortBy { it.first }
        // حذف هم‌پوشانی‌ها (مثلاً «روشن» داخل «روشن‌شد»)
        val unique = mutableListOf<Pair<Int, ChannelState>>()
        var lastPos = -100
        for (h in hits) {
            if (h.first - lastPos > 1) { unique.add(h); lastPos = h.first }
        }
        if (unique.size == cfg.channelCount) {
            unique.forEachIndexed { i, p -> result[i + 1] = p.second }
        }
        return result
    }
}
