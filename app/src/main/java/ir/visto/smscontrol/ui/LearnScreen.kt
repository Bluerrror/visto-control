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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.visto.smscontrol.ImportedSms
import ir.visto.smscontrol.Presets
import ir.visto.smscontrol.SmsImporter
import ir.visto.smscontrol.SmsRepo
import ir.visto.smscontrol.Store
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LearnScreen(modifier: Modifier = Modifier, snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg by Store.config.collectAsState()

    var messages by remember { mutableStateOf<List<ImportedSms>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<ImportedSms?>(null) }
    val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

    LazyColumn(
        modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("یادگیری فرمان‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "اگر قبلاً با اپ رسمی یا دستی به دستگاه پیامک داده‌اید، همان پیام‌ها روی گوشی هستند. " +
                        "آن‌ها را بخوانید و با یک ضربه به قالب فرمان تبدیل کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        item {
            Button(
                onClick = {
                    if (cfg.deviceNumber.isBlank()) {
                        scope.launch { snackbar.showSnackbar("اول شمارهٔ دستگاه را در تنظیمات وارد کنید") }
                    } else if (!SmsImporter.canRead(ctx)) {
                        scope.launch { snackbar.showSnackbar("اجازهٔ خواندن پیامک‌ها داده نشده است") }
                    } else {
                        messages = SmsImporter.load(ctx, cfg.deviceNumber)
                        loaded = true
                        scope.launch { snackbar.showSnackbar("${messages.size} پیام پیدا شد") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("خواندن پیامک‌های ردوبدل‌شده با دستگاه") }
        }

        if (loaded && messages.isEmpty()) {
            item {
                Text(
                    "هیچ پیامی با این شماره پیدا نشد. یا هنوز با دستگاه پیامکی ردوبدل نشده، یا شمارهٔ دستگاه اشتباه است.",
                    color = Color(0xFFB00020),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(messages) { m ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (m.outgoing) Color(0xFFE8F1EC) else Color(0xFFF3F1E8)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        (if (m.outgoing) "ارسالی ← " else "دریافتی → ") + fmt.format(Date(m.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(m.body, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    if (m.outgoing) {
                        OutlinedButton(onClick = { picking = m }) { Text("تبدیل به قالب فرمان") }
                    } else {
                        val parsed = ir.visto.smscontrol.Parser.parse(m.body, cfg)
                        Text(
                            if (parsed.isEmpty()) "تشخیص وضعیت: ناموفق — کلیدواژه‌ها را در تنظیمات اصلاح کنید"
                            else "تشخیص وضعیت: " + parsed.entries.joinToString("، ") {
                                "${cfg.nameOf(it.key)}=${if (it.value.name == "ON") "روشن" else "خاموش"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (parsed.isEmpty()) Color(0xFFB00020) else Color(0xFF1B6B4A)
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("الگوهای رایج برای امتحان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "این‌ها حدس‌های رایج در کنترلرهای ایرانی‌اند، نه دفترچهٔ رسمی ویستو. " +
                        "اول «وضعیت» را بفرستید چون خروجی را تغییر نمی‌دهد؛ اگر دستگاه پاسخ داد، الگو درست است.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        items(Presets.all) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(p.title, fontWeight = FontWeight.Bold)
                    Text(
                        "روشن: ${p.on}   خاموش: ${p.off}   وضعیت: ${p.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val err = SmsRepo.send(ctx, SmsRepo.build(p.status, cfg), null)
                            scope.launch { snackbar.showSnackbar(err ?: "فرمان «وضعیت» این الگو ارسال شد") }
                        }) { Text("فقط وضعیت را بفرست") }

                        OutlinedButton(onClick = {
                            Store.saveConfig(
                                cfg.copy(cmdOn = p.on, cmdOff = p.off, cmdStatus = p.status, cmdTimer = p.timer)
                            )
                            scope.launch { snackbar.showSnackbar("الگو روی تنظیمات اعمال شد") }
                        }) { Text("اعمال این الگو") }
                    }
                }
            }
        }
    }

    picking?.let { m ->
        var target by remember(m) { mutableStateOf("on") }
        var ch by remember(m) { mutableStateOf("1") }
        var min by remember(m) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text("تبدیل به قالب فرمان") },
            text = {
                Column {
                    Text(m.body, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("این پیام کدام فرمان بود؟")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("on" to "روشن", "off" to "خاموش", "status" to "وضعیت", "timer" to "زمان‌دار")
                            .forEach { (k, label) ->
                                TextButton(onClick = { target = k }) {
                                    Text(if (target == k) "● $label" else label)
                                }
                            }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ch,
                        onValueChange = { v -> ch = v.filter { it.isDigit() }.take(2) },
                        label = { Text("شمارهٔ کانال در این پیام") },
                        singleLine = true
                    )
                    if (target == "timer") {
                        OutlinedTextField(
                            value = min,
                            onValueChange = { v -> min = v.filter { it.isDigit() }.take(5) },
                            label = { Text("عدد دقیقه در این پیام") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val tpl = SmsImporter.toTemplate(
                        m.body,
                        if (target == "status") null else ch.toIntOrNull(),
                        min.toIntOrNull(),
                        cfg.password
                    )
                    val next = when (target) {
                        "on" -> cfg.copy(cmdOn = tpl)
                        "off" -> cfg.copy(cmdOff = tpl)
                        "status" -> cfg.copy(cmdStatus = tpl)
                        else -> cfg.copy(cmdTimer = tpl)
                    }
                    Store.saveConfig(next)
                    picking = null
                    scope.launch { snackbar.showSnackbar("قالب ذخیره شد: $tpl") }
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("انصراف") } }
        )
    }
}
