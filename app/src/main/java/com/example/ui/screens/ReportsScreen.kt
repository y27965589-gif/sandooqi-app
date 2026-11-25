package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.getTxYM
import com.example.data.FinanceUtils.shiftYM
import com.example.data.FinanceUtils.transactionAccount
import com.example.model.SandooqiDB
import com.example.ui.components.BadgeTag
import com.example.ui.components.EmptyHintText
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    db: SandooqiDB,
    buildCsv: (String) -> String,
    onDeleteTransaction: (Long) -> Unit,
    onOpenMonthClose: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current

    val availableMonths = remember(db.tx, db.monthArchive) {
        (listOf(currentYM()) + db.tx.map { getTxYM(it) } + db.monthArchive.keys)
            .distinct()
            .sortedDescending()
    }

    var selectedMonth by remember { mutableStateOf(currentYM()) }
    var logSearch by remember { mutableStateOf("") }
    var logAccount by remember { mutableStateOf("all") }
    var logType by remember { mutableStateOf("all") }

    val report = remember(db, selectedMonth) { FinanceUtils.computeCashReportForMonth(db, selectedMonth) }
    val prevReport = remember(db, selectedMonth) { FinanceUtils.computeCashReportForMonth(db, shiftYM(selectedMonth, -1)) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(buildCsv(selectedMonth).toByteArray(Charsets.UTF_8))
                }
                onNotify("تم تصدير التقرير بصيغة CSV")
            }.onFailure {
                onNotify("تعذر حفظ ملف CSV")
            }
        }
    }

    // Detailed Home Box Breakdown
    val detailed = remember(report) {
        var fac = 0.0
        var srv = 0.0
        var sav = 0.0
        var shop = 0.0
        var bills = 0.0
        var fam = 0.0
        var debtsPaid = 0.0
        for (t in report.arr) {
            if (t.type != "out" || t.cat == "transfer" || t.cat == "account_transfer") continue
            if (t.source == "credit-purchase" || t.cat == "credit_purchase") continue
            if (transactionAccount(t) != "family") continue
            when (t.cat) {
                "fac" -> fac += t.amount
                "srv" -> srv += t.amount
                "sav" -> sav += t.amount
                "bill", "rent" -> bills += t.amount
                "fam" -> fam += t.amount
                "debt_payment" -> debtsPaid += t.amount
                else -> shop += t.amount
            }
        }
        listOf(fac, srv, sav, shop, bills, fam, debtsPaid)
    }
    val dedSum = detailed[0] + detailed[1] + detailed[2]
    val expSum = detailed[3] + detailed[4] + detailed[5] + detailed[6]

    val breakdown = remember(detailed, report) {
        val raw = listOf(
            "مشتريات منزلية" to detailed[3],
            "فواتير وديون" to (detailed[4] + detailed[6]),
            "مصاريف العائلة" to detailed[5],
            "فاسيليتي وخدمات" to (detailed[0] + detailed[1]),
            "ادخار وأهداف" to detailed[2],
            "خياطة وشخصي" to (report.sewing.out + report.personal.out),
        )
        val total = max(1.0, raw.sumOf { it.second })
        raw.map { (lbl, v) -> Triple(lbl, v, ((v / total) * 100.0).roundToInt().coerceIn(0, 100)) }
    }
    val topCategory = remember(breakdown) { breakdown.maxByOrNull { it.second }?.takeIf { it.second > 0 } }

    val (daysInMonth, remDaysBase) = remember { FinanceUtils.daysInCurrentMonth() }
    val remDays = if (selectedMonth == currentYM()) remDaysBase else daysInMonth
    val openDebts = remember(db.homeDebts, db.rentDebts) {
        db.homeDebts.filter { it.direction != "in" }.sumOf { debtRemaining(it) } +
            db.rentDebts.filter { it.direction == "out" && !it.settled }.sumOf { max(0.0, it.amount - it.paid) }
    }
    val unpaidObl = remember(db.obligations, selectedMonth) {
        db.obligations.filter { selectedMonth !in it.paidMonths }.sumOf { it.amount }
    }
    val forecastAfterObl = report.home.balance - unpaidObl - openDebts
    val dailySafe = max(0.0, report.home.balance / remDays)

    val sixMonths = remember(db, selectedMonth) {
        (5 downTo 0).map { offset ->
            val m = shiftYM(selectedMonth, -offset)
            val r = FinanceUtils.computeCashReportForMonth(db, m)
            Triple(formatYM(m).substringBefore(" "), r.combined.income, r.combined.out)
        }
    }
    val maxChartVal = remember(sixMonths) {
        max(1.0, sixMonths.maxOf { max(it.second, it.third) })
    }

    val filteredTx = remember(report.arr, logAccount, logType, logSearch) {
        val q = logSearch.trim().lowercase()
        report.arr.filter { t ->
            val acc = transactionAccount(t)
            val isTransfer = t.cat == "transfer" || t.cat == "account_transfer"
            val accOk = logAccount == "all" || acc == logAccount
            val typeOk = when (logType) {
                "transfer" -> isTransfer
                "in" -> t.type == "in" && !isTransfer
                "out" -> t.type == "out" && !isTransfer
                else -> true
            }
            val qOk = q.isBlank() ||
                t.name.lowercase().contains(q) ||
                (t.person ?: "").lowercase().contains(q) ||
                (t.shop ?: "").lowercase().contains(q)
            accOk && typeOk && qOk
        }
    }

    val summaryText = remember(selectedMonth, report) {
        listOf(
            "📊 التقرير المالي — ${formatYM(selectedMonth)}",
            "• إجمالي الدخل: ${fmt(report.combined.income)}",
            "• إجمالي المصاريف: ${fmt(report.combined.out)}",
            "• رصيد العائلة: ${fmt(report.home.balance)}",
            "• رصيد الخياطة: ${fmt(report.sewing.balance)}",
            "• رصيد الشخصي: ${fmt(report.personal.balance)}",
            "• الرصيد التراكمي الكلي: ${fmt(report.combined.balance)}",
        ).joinToString("\n")
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header + Month Picker + Export Actions
        SandooqiCard {
            SectionHeader(title = "التقرير المالي الشهري — ${formatYM(selectedMonth)}")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                availableMonths.forEach { m ->
                    FilterChip(
                        selected = selectedMonth == m,
                        onClick = { selectedMonth = m },
                        label = { Text(formatYM(m)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Sandooqi Report", summaryText))
                        onNotify("تم نسخ ملخص التقرير الشهري")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نسخ")
                }
                OutlinedButton(
                    onClick = { csvLauncher.launch("sandooqi-report-$selectedMonth.csv") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV")
                }
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة التقرير"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة")
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("إجمالي الدخل", fmt(report.combined.income, privacy), valueColor = ext.emerald, modifier = Modifier.weight(1f))
                KpiMiniCard("إجمالي المصاريف", fmt(report.combined.out, privacy), valueColor = ext.rose, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard(
                    "صافي حركة الشهر",
                    fmt(report.combined.net, privacy),
                    valueColor = if (report.combined.net >= 0) ext.emerald else ext.rose,
                    modifier = Modifier.weight(1f),
                )
                KpiMiniCard("الرصيد التراكمي الكلي", fmt(report.combined.balance, privacy), modifier = Modifier.weight(1f))
            }

            if (report.home.balance < 0) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ext.roseContainer,
                    border = BorderStroke(1.dp, ext.rose.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = ext.rose, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تنبيه عجز في الصندوق العائلي", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ext.onRoseContainer)
                        }
                        Text(fmt(abs(report.home.balance), privacy), fontWeight = FontWeight.ExtraBold, color = ext.rose)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            // 3 Breakdown Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("الاقتطاعات والادخار", fmt(dedSum, privacy), valueColor = ext.rose, containerColor = ext.roseContainer, modifier = Modifier.weight(1f))
                KpiMiniCard("المصاريف والفواتير", fmt(expSum, privacy), valueColor = ext.amber, containerColor = ext.amberContainer, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))
            Text("توزيع المصاريف حسب الفئة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.forEach { (label, valAmt, pct) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("${fmt(valAmt, privacy)} ($pct%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        StatusProgressBar(percent = pct, barColor = ext.emerald)
                    }
                }
            }
        }

        // Smart Advisor Card
        SandooqiCard(
            containerColor = ext.emeraldContainer,
            borderColor = ext.emerald.copy(alpha = 0.35f),
        ) {
            SectionHeader(
                title = "المساعد المالي الذكي للشهر",
                icon = Icons.Filled.Psychology,
                iconTint = ext.emerald,
                trailing = {
                    BadgeTag(
                        text = if (forecastAfterObl >= 0) "متوازن" else "يحتاج ضبط",
                        containerColor = if (forecastAfterObl >= 0) ext.emerald else ext.rose,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("المسموح يومياً", fmt(dailySafe, privacy), containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f))
                KpiMiniCard("الأيام المتبقية", "$remDays يوم", containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("أعلى بند إنفاق", topCategory?.first ?: "—", containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f))
                KpiMiniCard(
                    "بعد الالتزامات",
                    fmt(forecastAfterObl, privacy),
                    valueColor = if (forecastAfterObl >= 0) ext.emerald else ext.rose,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (prevReport.combined.out > 0) {
                    val cmp = if (report.combined.out <= prevReport.combined.out) "أقل" else "أعلى"
                    "مقارنة بالشهر السابق: مصاريف هذا الشهر $cmp بفارق ${fmt(abs(report.combined.out - prevReport.combined.out), privacy)}."
                } else {
                    "تظهر المقارنة التلقائية مع الشهر السابق بمجرد تسجيل عمليات في شهرين."
                },
                style = MaterialTheme.typography.bodySmall,
                color = ext.onEmeraldContainer,
            )
        }

        // 6-Month Bar Chart via Compose Canvas
        SandooqiCard {
            SectionHeader(title = "تحليل الاتجاه المالي (آخر 6 أشهر)", icon = Icons.AutoMirrored.Filled.TrendingUp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BadgeTag("الدخل", ext.emeraldContainer, ext.onEmeraldContainer)
                BadgeTag("المصاريف", ext.roseContainer, ext.onRoseContainer)
            }
            Spacer(Modifier.height(12.dp))
            val incColor = ext.emerald
            val outColor = ext.rose
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                val count = sixMonths.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barW = (slotWidth * 0.28f).coerceAtLeast(8f)
                sixMonths.forEachIndexed { i, (_, inc, out) ->
                    val centerX = i * slotWidth + slotWidth / 2f
                    val incH = ((inc / maxChartVal) * (size.height - 8f)).toFloat().coerceAtLeast(4f)
                    val outH = ((out / maxChartVal) * (size.height - 8f)).toFloat().coerceAtLeast(4f)
                    drawRoundRect(
                        color = incColor,
                        topLeft = Offset(centerX - barW - 3f, size.height - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                    drawRoundRect(
                        color = outColor,
                        topLeft = Offset(centerX + 3f, size.height - outH),
                        size = Size(barW, outH),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                sixMonths.forEach { (label, _, _) ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Month Archive & Close Action
        SandooqiCard {
            SectionHeader(
                title = "أرشيف الأشهر المغلقة (${db.monthArchive.size})",
                icon = Icons.Filled.CalendarToday,
                trailing = {
                    OutlinedButton(onClick = onOpenMonthClose) {
                        Text("إغلاق الشهر")
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            if (db.monthArchive.isEmpty()) {
                EmptyHintText("لم يتم إغلاق أي شهر بعد.")
            } else {
                db.monthArchive.values.sortedByDescending { it.ym }.forEach { arc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(formatYM(arc.ym), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("ادخار: ${fmt(arc.savedToGoal, privacy)} · مرحّل: ${fmt(arc.carriedForward, privacy)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(fmt(arc.closingBalance, privacy), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = ext.emerald)
                    }
                }
            }
        }

        // Filterable Transaction Log
        SandooqiCard {
            SectionHeader(title = "سجل العمليات (${filteredTx.size})")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = logSearch,
                onValueChange = { logSearch = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("بحث في العمليات...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("all" to "كل الحسابات", "family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (k, lbl) ->
                    FilterChip(selected = logAccount == k, onClick = { logAccount = k }, label = { Text(lbl) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "الكل", "in" to "وارد", "out" to "صادر", "transfer" to "تحويل").forEach { (k, lbl) ->
                    FilterChip(selected = logType == k, onClick = { logType = k }, label = { Text(lbl) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filteredTx.isEmpty()) {
                EmptyHintText("لا توجد عمليات مطابقة.")
            } else {
                filteredTx.take(40).forEachIndexed { idx, t ->
                    if (idx > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val accLabel = FinanceUtils.ACCOUNT_LABELS[transactionAccount(t)] ?: "الحساب العائلي"
                            Text("$accLabel · ${t.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (t.type == "in") "+" else "−"}${fmt(t.amount, privacy)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (t.type == "in") ext.emerald else ext.rose,
                            )
                            IconButton(onClick = { onDeleteTransaction(t.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف العملية", tint = ext.rose)
                            }
                        }
                    }
                }
            }
        }
    }
}
