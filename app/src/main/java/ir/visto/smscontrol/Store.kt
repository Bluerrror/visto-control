package ir.visto.smscontrol

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** ذخیرهٔ ساده روی SharedPreferences با JSON — بدون وابستگی اضافه. */
object Store {

    private const val PREFS = "visto_store"
    private const val K_CONFIG = "config"
    private const val K_CHANNELS = "channels"
    private const val K_LOGS = "logs"
    private const val MAX_LOGS = 300

    private lateinit var prefs: SharedPreferences

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _config.value = readConfig()
        _channels.value = readChannels(_config.value)
        _logs.value = readLogs()
    }

    // ---------- config ----------

    private fun readConfig(): Config {
        val raw = prefs.getString(K_CONFIG, null) ?: return Config()
        return try {
            val o = JSONObject(raw)
            val d = Config()
            val names = mutableListOf<String>()
            val arr = o.optJSONArray("channelNames")
            if (arr != null) for (i in 0 until arr.length()) names.add(arr.optString(i))
            Config(
                deviceNumber = o.optString("deviceNumber", d.deviceNumber),
                password = o.optString("password", d.password),
                channelCount = o.optInt("channelCount", d.channelCount),
                channelNames = if (names.isEmpty()) d.channelNames else names,
                cmdOn = o.optString("cmdOn", d.cmdOn),
                cmdOff = o.optString("cmdOff", d.cmdOff),
                cmdStatus = o.optString("cmdStatus", d.cmdStatus),
                cmdTimer = o.optString("cmdTimer", d.cmdTimer),
                cmdAllOff = o.optString("cmdAllOff", d.cmdAllOff),
                onWords = o.optString("onWords", d.onWords),
                offWords = o.optString("offWords", d.offWords),
                subscriptionId = o.optInt("subscriptionId", d.subscriptionId),
                confirm = o.optBoolean("confirm", d.confirm),
                simulator = o.optBoolean("simulator", d.simulator)
            )
        } catch (e: Exception) {
            Config()
        }
    }

    fun saveConfig(c: Config) {
        val o = JSONObject()
        o.put("deviceNumber", c.deviceNumber)
        o.put("password", c.password)
        o.put("channelCount", c.channelCount)
        o.put("channelNames", JSONArray(c.channelNames))
        o.put("cmdOn", c.cmdOn)
        o.put("cmdOff", c.cmdOff)
        o.put("cmdStatus", c.cmdStatus)
        o.put("cmdTimer", c.cmdTimer)
        o.put("cmdAllOff", c.cmdAllOff)
        o.put("onWords", c.onWords)
        o.put("offWords", c.offWords)
        o.put("subscriptionId", c.subscriptionId)
        o.put("confirm", c.confirm)
        o.put("simulator", c.simulator)
        prefs.edit().putString(K_CONFIG, o.toString()).apply()
        _config.value = c
        // تعداد/نام کانال‌ها ممکن است عوض شده باشد
        _channels.value = reconcile(_channels.value, c)
        writeChannels(_channels.value)
    }

    // ---------- channels ----------

    private fun reconcile(old: List<Channel>, c: Config): List<Channel> =
        (1..c.channelCount).map { i ->
            val prev = old.firstOrNull { it.index == i }
            Channel(
                index = i,
                name = c.nameOf(i),
                state = prev?.state ?: ChannelState.UNKNOWN,
                updatedAt = prev?.updatedAt ?: 0L,
                pending = prev?.pending ?: false
            )
        }

    private fun readChannels(c: Config): List<Channel> {
        val raw = prefs.getString(K_CHANNELS, null)
        val old = mutableListOf<Channel>()
        if (raw != null) try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                old.add(
                    Channel(
                        index = o.getInt("index"),
                        name = o.optString("name"),
                        state = runCatching { ChannelState.valueOf(o.optString("state")) }
                            .getOrDefault(ChannelState.UNKNOWN),
                        updatedAt = o.optLong("updatedAt")
                    )
                )
            }
        } catch (_: Exception) { }
        return reconcile(old, c)
    }

    private fun writeChannels(list: List<Channel>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("index", it.index)
                    .put("name", it.name)
                    .put("state", it.state.name)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs.edit().putString(K_CHANNELS, arr.toString()).apply()
    }

    fun setState(index: Int, state: ChannelState) {
        _channels.value = _channels.value.map {
            if (it.index == index) it.copy(state = state, updatedAt = System.currentTimeMillis(), pending = false)
            else it
        }
        writeChannels(_channels.value)
    }

    fun setPending(index: Int, pending: Boolean) {
        _channels.value = _channels.value.map { if (it.index == index) it.copy(pending = pending) else it }
    }

    fun clearPending() {
        _channels.value = _channels.value.map { it.copy(pending = false) }
    }

    // ---------- logs ----------

    private fun readLogs(): List<LogEntry> {
        val raw = prefs.getString(K_LOGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LogEntry(o.getLong("time"), o.getBoolean("out"), o.getString("text"), o.optString("status"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLog(e: LogEntry) {
        val list = (listOf(e) + _logs.value).take(MAX_LOGS)
        _logs.value = list
        persistLogs(list)
    }

    /** وضعیت آخرین پیام ارسالی را به‌روز می‌کند (مثلاً «ارسال شد» یا خطا). */
    fun updateLastOutgoingStatus(status: String) {
        val list = _logs.value.toMutableList()
        val i = list.indexOfFirst { it.outgoing }
        if (i >= 0) {
            list[i] = list[i].copy(status = status)
            _logs.value = list
            persistLogs(list)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        persistLogs(emptyList())
    }

    private fun persistLogs(list: List<LogEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("time", it.time)
                    .put("out", it.outgoing)
                    .put("text", it.text)
                    .put("status", it.status)
            )
        }
        prefs.edit().putString(K_LOGS, arr.toString()).apply()
    }
}
