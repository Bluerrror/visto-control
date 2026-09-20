package ir.visto.smscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.visto.smscontrol.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val logs by Store.logs.collectAsState()
    val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.US)

    Column(modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "تاریخچهٔ پیام‌ها",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { Store.clearLogs() }) { Text("پاک کردن") }
        }

        if (logs.isEmpty()) {
            Text(
                "هنوز پیامی رد و بدل نشده است.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { e ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (e.outgoing) Color(0xFFE8F1EC) else Color(0xFFF3F1E8)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            (if (e.outgoing) "ارسالی ← " else "دریافتی → ") + fmt.format(Date(e.time)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(e.text, style = MaterialTheme.typography.bodyMedium)
                        if (e.status.isNotBlank()) {
                            Text(
                                e.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (e.status.startsWith("خطا")) Color(0xFFB00020) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
