package ir.visto.smscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.visto.smscontrol.Config
import ir.visto.smscontrol.SmsRepo
import ir.visto.smscontrol.Store
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, snackbar: SnackbarHostState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by Store.config.collectAsState()
    var c by remember(saved) { mutableStateOf(saved) }

    val sims = remember { SmsRepo.sims(ctx) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Section("دستگاه")

        OutlinedTextField(
            value = c.deviceNumber,
            onValueChange = { c = c.copy(deviceNumber = it.trim()) },
            label = { Text("شمارهٔ سیم‌کارت دستگاه") },
            placeholder = { Text("مثلاً 09123456789") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = c.password,
            onValueChange = { c = c.copy(password = it.trim()) },
            label = { Text("رمز دستگاه (اگر دارد)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("تعداد کانال: ${c.channelCount}", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                if (c.channelCount > 1) c = c.copy(
                    channelCount = c.channelCount - 1,
                    channelNames = c.channelNames.take(c.channelCount - 1)
                )
            }) { Text("−") }
            Spacer(Modifier.height(0.dp))
            OutlinedButton(onClick = {
                if (c.channelCount < 10) c = c.copy(
                    channelCount = c.channelCount + 1,
                    channelNames = c.channelNames + "کانال ${c.channelCount + 1}"
                )
            }) { Text("+") }
        }

        for (i in 1..c.channelCount) {
            OutlinedTextField(
                value = c.channelNames.getOrNull(i - 1) ?: "",
                onValueChange = { v ->
                    val list = c.channelNames.toMutableList()
                    while (list.size < c.channelCount) list.add("")
                    list[i - 1] = v
                    c = c.copy(channelNames = list)
                },
                label = { Text("نام کانال $i") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider()
        Section("قالب فرمان‌ها")
        Card(Modifier.fillMaxWidth()) {
            Text(
                "پیش‌فرض‌ها روی سینتکس رسمی ویستو تنظیم شده‌اند. اگر مدل شما فرق داشت، اصلاح کنید.\n" +
                        "جانگهدارها: {ch} شمارهٔ کانال، {min} دقیقه، {pw} رمز، {name} نام کانال.\n" +
                        "نمونهٔ ویستو: ({ch})On روشن، ({ch})Off خاموش، ({ch}){min} روشنِ زمان‌دار",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }

        Cmd("فرمان روشن کردن", c.cmdOn) { c = c.copy(cmdOn = it) }
        Cmd("فرمان خاموش کردن", c.cmdOff) { c = c.copy(cmdOff = it) }
        Cmd("فرمان استعلام وضعیت", c.cmdStatus) { c = c.copy(cmdStatus = it) }
        Cmd("فرمان زمان‌دار", c.cmdTimer) { c = c.copy(cmdTimer = it) }
        Cmd("فرمان خاموشی همه (اختیاری)", c.cmdAllOff) { c = c.copy(cmdAllOff = it) }

        Divider()
        Section("تشخیص پاسخ دستگاه")
        Cmd("کلیدواژه‌های «روشن» (با ویرگول)", c.onWords) { c = c.copy(onWords = it) }
        Cmd("کلیدواژه‌های «خاموش» (با ویرگول)", c.offWords) { c = c.copy(offWords = it) }

        if (sims.isNotEmpty()) {
            Divider()
            Section("سیم‌کارت ارسال")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = c.subscriptionId < 0,
                    onClick = { c = c.copy(subscriptionId = -1) },
                    label = { Text("پیش‌فرض") }
                )
                sims.forEach { s ->
                    FilterChip(
                        selected = c.subscriptionId == s.subscriptionId,
                        onClick = { c = c.copy(subscriptionId = s.subscriptionId) },
                        label = { Text(s.displayName?.toString() ?: "سیم ${s.simSlotIndex + 1}") }
                    )
                }
            }
        }

        Divider()
        Section("ایمنی")
        SwitchRow("تأیید گرفتن قبل از هر فرمان", c.confirm) { c = c.copy(confirm = it) }
        SwitchRow("حالت شبیه‌ساز (بدون ارسال پیامک واقعی)", c.simulator) { c = c.copy(simulator = it) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                Store.saveConfig(c)
                scope.launch { snackbar.showSnackbar("تنظیمات ذخیره شد") }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ذخیرهٔ تنظیمات") }

        OutlinedButton(
            onClick = {
                Store.saveConfig(c)
                SmsRepo.sendViaDefaultApp(ctx, SmsRepo.build(c.cmdStatus, c))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ارسال آزمایشی با اپ پیام‌رسان گوشی") }

        Text(
            "اگر مجوز ارسال پیامک را ندادید یا نصب از گوگل‌پلی محدودیت داشت، از دکمهٔ بالا استفاده کنید؛ " +
                    "متن فرمان آماده می‌شود و خودتان دکمهٔ ارسال را می‌زنید.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Cmd(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Suppress("unused")
private fun defaults() = Config()
