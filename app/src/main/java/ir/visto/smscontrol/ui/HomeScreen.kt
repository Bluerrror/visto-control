package ir.visto.smscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.visto.smscontrol.Channel
import ir.visto.smscontrol.ChannelState
import ir.visto.smscontrol.Config
import ir.visto.smscontrol.SmsRepo
import ir.visto.smscontrol.Store
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class PendingAction(val title: String, val body: String, val run: () -> Unit)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState,
    goToSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg by Store.config.collectAsState()
    val channels by Store.channels.collectAsState()

    var confirm by remember { mutableStateOf<PendingAction?>(null) }
    var timerFor by remember { mutableStateOf<Int?>(null) }

    fun dispatch(title: String, body: String, action: () -> Unit) {
        if (cfg.confirm) confirm = PendingAction(title, body, action) else action()
    }

    fun fire(text: String, expect: Pair<Int, ChannelState>?) {
        val err = SmsRepo.send(ctx, text, expect)
        scope.launch { snackbar.showSnackbar(err ?: "فرمان ارسال شد") }
    }

    if (cfg.deviceNumber.isBlank()) {
        Column(
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("هنوز دستگاهی تنظیم نشده", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "شمارهٔ سیم‌کارت دستگاه و قالب فرمان‌ها را از روی دفترچهٔ راهنما در بخش تنظیمات وارد کنید.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = goToSettings) { Text("رفتن به تنظیمات") }
        }
        return
    }

    LazyColumn(
        modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { fire(SmsRepo.build(cfg.cmdStatus, cfg), null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.height(0.dp))
                    Text(" استعلام وضعیت")
                }
                OutlinedButton(
                    onClick = {
                        dispatch("خاموش کردن همه", "همهٔ خروجی‌ها خاموش شوند؟") {
                            if (cfg.cmdAllOff.isNotBlank()) {
                                fire(SmsRepo.build(cfg.cmdAllOff, cfg), null)
                            } else {
                                channels.forEach { c ->
                                    SmsRepo.send(
                                        ctx,
                                        SmsRepo.build(cfg.cmdOff, cfg, c.index),
                                        c.index to ChannelState.OFF
                                    )
                                }
                                scope.launch { snackbar.showSnackbar("فرمان خاموشی همهٔ کانال‌ها ارسال شد") }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, null)
                    Text(" خاموشی همه")
                }
            }
        }

        items(channels, key = { it.index }) { ch ->
            ChannelCard(
                ch = ch,
                cfg = cfg,
                onOn = {
                    dispatch("روشن کردن", "«${ch.name}» روشن شود؟") {
                        fire(SmsRepo.build(cfg.cmdOn, cfg, ch.index), ch.index to ChannelState.ON)
                    }
                },
                onOff = {
                    dispatch("خاموش کردن", "«${ch.name}» خاموش شود؟") {
                        fire(SmsRepo.build(cfg.cmdOff, cfg, ch.index), ch.index to ChannelState.OFF)
                    }
                },
                onTimer = { timerFor = ch.index }
            )
        }

        item {
            Text(
                "وضعیت نمایش‌داده‌شده «آخرین وضعیت شناخته‌شده» است و تا رسیدن پیامک پاسخ دستگاه قطعی نیست.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(action.title) },
            text = { Text(action.body) },
            confirmButton = {
                TextButton(onClick = { action.run(); confirm = null }) { Text("تأیید و ارسال") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("انصراف") } }
        )
    }

    timerFor?.let { ch ->
        var minutes by remember(ch) { mutableStateOf("20") }
        AlertDialog(
            onDismissRequest = { timerFor = null },
            title = { Text("اجرای زمان‌دار — ${cfg.nameOf(ch)}") },
            text = {
                Column {
                    Text("مدت روشن ماندن به دقیقه:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { v -> minutes = v.filter { it.isDigit() }.take(5) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val m = minutes.toIntOrNull()
                    if (m != null && m > 0) {
                        fire(SmsRepo.build(cfg.cmdTimer, cfg, ch, m), ch to ChannelState.ON)
                    }
                    timerFor = null
                }) { Text("ارسال") }
            },
            dismissButton = { TextButton(onClick = { timerFor = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun ChannelCard(
    ch: Channel,
    cfg: Config,
    onOn: () -> Unit,
    onOff: () -> Unit,
    onTimer: () -> Unit
) {
    val isOn = ch.state == ChannelState.ON
    val label = when (ch.state) {
        ChannelState.ON -> "روشن"
        ChannelState.OFF -> "خاموش"
        ChannelState.UNKNOWN -> "نامشخص"
    }
    val color = when (ch.state) {
        ChannelState.ON -> Color(0xFF1B6B4A)
        ChannelState.OFF -> Color(0xFF6B6B6B)
        ChannelState.UNKNOWN -> Color(0xFFB08900)
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ch.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { },
                    label = { Text(label) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = color)
                )
            }

            if (ch.updatedAt > 0) {
                Text(
                    "آخرین به‌روزرسانی: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                        .format(Date(ch.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (ch.pending) {
                Spacer(Modifier.height(6.dp))
                Text("در انتظار پاسخ دستگاه…", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOn, enabled = !isOn, modifier = Modifier.weight(1f)) {
                    Text("روشن")
                }
                OutlinedButton(onClick = onOff, modifier = Modifier.weight(1f)) {
                    Text("خاموش")
                }
                OutlinedButton(onClick = onTimer) { Icon(Icons.Filled.Timer, "زمان‌دار") }
            }

            if (cfg.cmdTimer.isBlank()) {
                Text(
                    "قالب فرمان زمان‌دار در تنظیمات خالی است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB00020)
                )
            }
        }
    }
}
