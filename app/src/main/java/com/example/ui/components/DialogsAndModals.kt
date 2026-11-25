package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.shiftYM
import com.example.model.AppPage
import com.example.model.SandooqiDB
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceEntryDialog(
    creditorNames: List<String>,
    familyEntryOnlyMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (mode: String, title: String, amount: Double, party: String, applyNow: Boolean) -> Unit,
) {
    var transcript by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("cash") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("") }
    var applyNow by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val applyParsed: (String) -> Unit = { raw ->
        val res = FinanceUtils.parseArabicVoiceEntry(raw)
        mode = res.mode
        if (res.title.isNotBlank()) title = res.title
        if (res.amount.isNotBlank()) amount = res.amount
        if (res.party.isNotBlank()) party = res.party
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                transcript = spoken
                speechError = null
                applyParsed(spoken)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("التسجيل الصوتي الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Button(
                    onClick = {
                        runCatching {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-DZ")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن (مثال: خبز 100 أو بطاطا 500 دين عند علي)")
                            }
                            speechLauncher.launch(intent)
                        }.onFailure {
                            speechError = "التعرف الصوتي غير متاح، يمكنك كتابة الجملة مباشرة أدناه"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_voice_recognition_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("اضغط للتحدث بالعربية", fontWeight = FontWeight.Bold)
                }

                if (!speechError.isNullOrBlank()) {
                    Text(
                        text = speechError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = SandooqiThemeTokens.colors.amber,
                    )
                }

                OutlinedTextField(
                    value = transcript,
                    onValueChange = {
                        transcript = it
                        applyParsed(it)
                    },
                    label = { Text("نص الجملة (أو اكتبها مباشرة)") },
                    placeholder = { Text("مثال: خبز وحليب 300 أو سكر 400 دين عند كريم") },
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == "cash", onClick = { mode = "cash" }, label = { Text("مصروف نقدي") })
                    FilterChip(selected = mode == "out", onClick = { mode = "out" }, label = { Text("دين عليّ") })
                    FilterChip(selected = mode == "in", onClick = { mode = "in" }, label = { Text("مستحق لي") })
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("البيان / المادة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (mode != "cash") {
                    OutlinedTextField(
                        value = party,
                        onValueChange = { party = it },
                        label = { Text(if (mode == "in") "اسم الشخص" else "اسم المحل / الدائن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (creditorNames.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            creditorNames.take(5).forEach { name ->
                                FilterChip(
                                    selected = party == name,
                                    onClick = { party = name },
                                    label = { Text(name) },
                                )
                            }
                        }
                    }
                    if (!familyEntryOnlyMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = applyNow, onCheckedChange = { applyNow = it })
                            Text(
                                if (mode == "in") "تحصيل المبلغ مباشرة للخزينة" else "تسديد المبلغ مباشرة من الخزينة",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                onSave(mode, title.trim(), amt, party.trim(), applyNow)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_voice_entry_button"),
                    ) {
                        Text("حفظ العملية")
                    }
                }
            }
        }
    }
}

private data class SearchResultRow(
    val title: String,
    val subtitle: String,
    val badge: String,
    val page: AppPage,
)

@Composable
fun GlobalSearchDialog(
    db: SandooqiDB,
    onDismiss: () -> Unit,
    onNavigate: (AppPage) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, db) {
        val q = query.trim().lowercase()
        if (q.isBlank()) emptyList()
        else buildList {
            db.tx.filter {
                it.name.lowercase().contains(q) || (it.person ?: "").lowercase().contains(q) || (it.shop ?: "").lowercase().contains(q)
            }.take(10).forEach {
                add(SearchResultRow(it.name, "${it.date} · ${fmt(it.amount, db.privacyMode)}", "عملية", AppPage.REPORTS))
            }
            db.homeDebts.filter {
                it.creditor.lowercase().contains(q) || it.item.lowercase().contains(q)
            }.take(8).forEach {
                add(SearchResultRow("${it.creditor} — ${it.item}", "المتبقي: ${fmt(debtRemaining(it), db.privacyMode)}", "دين", AppPage.BILLS))
            }
            db.orders.filter {
                it.client.lowercase().contains(q) || it.model.lowercase().contains(q)
            }.take(8).forEach {
                add(SearchResultRow("${it.client} — ${it.model}", "${it.stage} · ${fmt(it.price, db.privacyMode)}", "خياطة", AppPage.TAILORING))
            }
            db.marketList.filter { it.name.lowercase().contains(q) }.take(6).forEach {
                add(SearchResultRow(it.name, "${it.quantity} ${it.unit}", "سوق", AppPage.EXPENSES))
            }
            db.goals.filter { it.name.lowercase().contains(q) }.take(5).forEach {
                add(SearchResultRow(it.name, "${fmt(it.saved, db.privacyMode)} / ${fmt(it.target, db.privacyMode)}", "هدف", AppPage.GOALS))
            }
            db.warranties.filter {
                it.deviceName.lowercase().contains(q) || it.shopOrBrand.lowercase().contains(q)
            }.take(5).forEach {
                add(SearchResultRow(it.deviceName, "ينتهي: ${it.warrantyEndDateIso}", "ضمان", AppPage.TOOLS))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("بحث شامل في صندوقي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "إغلاق") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("ابحث عن مصروف، دين، زبونة خياطة، ضمان...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                )
                Spacer(Modifier.height(10.dp))
                if (query.isBlank()) {
                    EmptyHintText("اكتب أي كلمة للبحث السريع في جميع أقسام التطبيق.")
                } else if (results.isEmpty()) {
                    EmptyHintText("لا توجد نتائج مطابقة لـ «$query».")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results) { row ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigate(row.page)
                                        onDismiss()
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    BadgeTag(
                                        text = row.badge,
                                        containerColor = SandooqiThemeTokens.colors.emeraldContainer,
                                        contentColor = SandooqiThemeTokens.colors.onEmeraldContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCloseDialog(
    db: SandooqiDB,
    onDismiss: () -> Unit,
    onConfirmClose: (sourceMonth: String, targetMonth: String, savePct: Int, targetGoalId: Long?) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val pending = db.monthClosePending.firstOrNull()
    val sourceMonth = pending?.month ?: currentYM()
    val targetMonth = pending?.target ?: shiftYM(sourceMonth, 1)
    val report = remember(db, sourceMonth) { FinanceUtils.computeCashReportForMonth(db, sourceMonth) }
    val closing = max(0.0, report.home.balance)

    var savePct by remember { mutableIntStateOf(20) }
    var selectedGoalId by remember(db.goals) { mutableStateOf(db.goals.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = ext.emerald, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("إغلاق شهر ${formatYM(sourceMonth)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMiniCard(
                    label = "فائض الحساب العائلي",
                    value = fmt(closing),
                    valueColor = ext.emerald,
                    containerColor = ext.emeraldContainer,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("الشهر المستهدف للترحيل: ${formatYM(targetMonth)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (closing > 0 && db.goals.isNotEmpty()) {
                    val savedToGoal = kotlin.math.round((closing * savePct) / 100.0)
                    Text(
                        text = "تحويل للادخار: $savePct% (${fmt(savedToGoal)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Slider(
                        value = savePct.toFloat(),
                        onValueChange = { savePct = (it / 5f).roundToInt() * 5 },
                        valueRange = 0f..100f,
                        steps = 19,
                    )
                    Text("إلى حصّالة:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        db.goals.forEach { g ->
                            FilterChip(
                                selected = selectedGoalId == g.id,
                                onClick = { selectedGoalId = g.id },
                                label = { Text(g.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmClose(sourceMonth, targetMonth, savePct, selectedGoalId) },
                modifier = Modifier.testTag("confirm_month_close_button"),
            ) {
                Text("تأكيد إغلاق الشهر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddFabDialog(
    db: SandooqiDB,
    onDismiss: () -> Unit,
    onNavigate: (AppPage) -> Unit,
    onQuickCash: (title: String, amount: Double, person: String?) -> Unit,
    onQuickDebt: (creditor: String, item: String, amount: Double) -> Unit,
) {
    var mode by remember { mutableStateOf("cash") } // "cash" | "debt"
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var creditor by remember { mutableStateOf("") }
    var selectedPerson by remember(db.family) { mutableStateOf(db.family.firstOrNull() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("إضافة سريعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "إغلاق") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "cash", onClick = { mode = "cash" }, label = { Text("مصروف فوري") })
                    FilterChip(selected = mode == "debt", onClick = { mode = "debt" }, label = { Text("دين محل") })
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (mode == "cash") "البيان (خبز، حليب...)" else "المشتريات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (mode == "cash") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        db.family.forEach { p ->
                            FilterChip(selected = selectedPerson == p, onClick = { selectedPerson = p }, label = { Text(p) })
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = creditor,
                        onValueChange = { creditor = it },
                        label = { Text("اسم المحل / الدائن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            if (mode == "cash") {
                                onQuickCash(title.trim(), amt, selectedPerson)
                            } else if (creditor.isNotBlank()) {
                                onQuickDebt(creditor.trim(), title.trim(), amt)
                            } else return@Button
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quick_add_submit_button"),
                ) {
                    Text("حفظ الآن", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigate(AppPage.TREASURY)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("حركة خزينة") }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigate(AppPage.TAILORING)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("طلبية خياطة") }
                }
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    title: String,
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "إغلاق") }
                }
                Spacer(Modifier.height(8.dp))
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                } else {
                    EmptyHintText("تعذر عرض الصورة المرفقة.")
                }
            }
        }
    }
}
