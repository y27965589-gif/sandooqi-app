package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.absVal
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.getTxYM
import com.example.data.FinanceUtils.shiftYM
import com.example.model.SandooqiDB
import com.example.ui.components.EmptyHintText
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsSettingsScreen(
    db: SandooqiDB,
    onAddQuickExpensePreset: (title: String, amount: Double, emoji: String) -> Unit,
    onDeleteQuickExpensePreset: (String) -> Unit,
    onAddAccountBook: (kind: String, name: String, phone: String?, note: String?) -> Unit,
    onAddAccountBookEntry: (accountId: Long, sign: Int, defaultLabel: String, amount: Double) -> Unit,
    onDeleteAccountBook: (Long) -> Unit,
    onAddWarranty: (
        deviceName: String,
        category: String,
        shopOrBrand: String,
        purchaseDateIso: String,
        warrantyEndDateIso: String,
        cost: Double,
        photoData: String,
        notes: String?,
    ) -> Unit,
    onDeleteWarranty: (Long) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onAddFamilyMember: (String) -> Unit,
    onRemoveFamilyMember: (String) -> Unit,
    onEnableFamilyMode: () -> Unit,
    onUpdateSewingRates: (Double, Double) -> Unit,
    onAddCatalogProduct: (group: String, name: String, emoji: String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleAppLock: () -> Unit,
    onLockNow: () -> Unit,
    onChangePin: (String) -> Unit,
    exportBackupJson: () -> String,
    onImportBackupJson: (String) -> Unit,
    onSelectiveReset: (salary: Boolean, homeTx: Boolean, sewing: Boolean, homeDebts: Boolean, sewingDebts: Boolean) -> Unit,
    onResetAll: () -> Unit,
    onNotify: (String) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }

    var tab by remember { mutableStateOf("sim") } // sim | analytics | accounts | settings

    // Simulator
    var simName by remember { mutableStateOf("") }
    var simPrice by remember { mutableStateOf("") }
    var simSource by remember { mutableStateOf("home") }

    // Quick expense presets
    var qeEmoji by remember { mutableStateOf("🛍️") }
    var qeTitle by remember { mutableStateOf("") }
    var qeAmt by remember { mutableStateOf("") }

    // Analytics
    var faPerson by remember(db.family) { mutableStateOf(db.family.firstOrNull() ?: "") }
    var faMonth by remember { mutableStateOf(ym) }
    var miMonth by remember { mutableStateOf(ym) }

    val curReport = remember(db, ym) { FinanceUtils.computeCashReportForMonth(db, ym) }
    val totalSaved = remember(db.goals, db.seasonalBudgets) {
        db.goals.sumOf { it.saved } + db.seasonalBudgets.sumOf { it.savedAmount }
    }
    val simPool = when (simSource) {
        "combined" -> curReport.home.balance + curReport.sewing.balance
        "savings" -> curReport.home.balance + curReport.sewing.balance + totalSaved
        else -> curReport.home.balance
    }
    val simCost = max(0.0, simPrice.toDoubleOrNull() ?: 0.0)
    val simAfter = simPool - simCost

    val memberStats = remember(db.tx, faMonth, faPerson) {
        val curTx = db.tx.filter {
            getTxYM(it) == faMonth &&
                it.type == "out" &&
                it.person == faPerson &&
                it.cat != "transfer" &&
                it.cat != "account_transfer" &&
                it.cat != "sav" &&
                it.source != "credit-purchase" &&
                it.cat != "credit_purchase"
        }
        val prevM = shiftYM(faMonth, -1)
        val prevSmoke = db.tx
            .filter { getTxYM(it) == prevM && it.type == "out" && it.person == faPerson && it.name.contains("دخان") }
            .sumOf { it.amount }
        val total = curTx.sumOf { it.amount }
        val smoke = curTx.filter { it.name.contains("دخان") }.sumOf { it.amount }
        listOf(total, smoke, total - smoke, prevSmoke)
    }

    val whereMoneyWent = remember(db, miMonth) {
        val rep = FinanceUtils.computeCashReportForMonth(db, miMonth)
        val catMap = mutableMapOf<String, Double>()
        val dayMap = mutableMapOf<String, Double>()
        for (t in rep.arr) {
            if (t.type != "out" || t.cat == "transfer" || t.cat == "account_transfer") continue
            if (t.source == "credit-purchase" || t.cat == "credit_purchase") continue
            val key = when {
                t.cat == "bill" || t.cat == "rent" -> "الفواتير والكراء"
                t.cat == "fam" -> "العائلة"
                t.cat == "fac" -> "فاسيليتي"
                t.cat == "srv" -> "خدمات"
                t.cat == "sav" -> "الادخار"
                t.cat == "debt_payment" -> "تسديد الديون"
                FinanceUtils.transactionAccount(t) == "sewing" -> "ورشة الخياطة"
                else -> "المشتريات المنزلية"
            }
            catMap[key] = (catMap[key] ?: 0.0) + t.amount
            val dKey = t.dateISO.ifBlank { t.date }
            dayMap[dKey] = (dayMap[dKey] ?: 0.0) + t.amount
        }
        val top3 = catMap.entries.sortedByDescending { it.value }.take(3).map { it.key to it.value }
        val daily = dayMap.entries.sortedByDescending { it.key }.map { it.key to it.value }
        top3 to daily
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 4 Sub-tabs
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple("sim", "المحاكي والسريع", Icons.Filled.Calculate),
                Triple("analytics", "التحليلات", Icons.Filled.BarChart),
                Triple("accounts", "الحسابات والضمانات", Icons.Filled.People),
                Triple("settings", "الإعدادات والأمان", Icons.Filled.Settings),
            ).forEach { (k, lbl, icon) ->
                FilterChip(
                    selected = tab == k,
                    onClick = { tab = k },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(lbl, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tools_tab_$k"),
                )
            }
        }

        when (tab) {
            "sim" -> {
                // Affordability Simulator
                SandooqiCard {
                    SectionHeader("محاكي: هل أقدر أشتريه الآن؟", Icons.Filled.Calculate, ext.sky)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = simName,
                        onValueChange = { simName = it },
                        label = { Text("الشيء الذي تفكر في شرائه...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sim_name_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = simPrice,
                        onValueChange = { simPrice = FinanceUtils.normArDigits(it) },
                        label = { Text("السعر المتوقع (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("sim_price_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "home" to "صندوق البيت فقط",
                            "combined" to "البيت + الخياطة",
                            "savings" to "البيت + الخياطة + الادخار",
                        ).forEach { (k, lbl) ->
                            FilterChip(selected = simSource == k, onClick = { simSource = k }, label = { Text(lbl) })
                        }
                    }

                    if (simCost > 0) {
                        Spacer(Modifier.height(10.dp))
                        val safe = simAfter >= simPool * 0.25
                        val tight = simAfter >= 0
                        val bg = if (safe) ext.emeraldContainer else if (tight) ext.amberContainer else ext.roseContainer
                        val fg = if (safe) ext.onEmeraldContainer else if (tight) ext.onAmberContainer else ext.onRoseContainer
                        val verdict = when {
                            safe -> "نعم، يمكنك شراء «${simName.ifBlank { "الغرض" }}» بأمان."
                            tight -> "يمكنك شراؤه، لكن سيتبقى مبلغ محدود جداً في الصندوق."
                            else -> "غير مناسب الآن — المبلغ يفوق المتاح بـ ${fmt(absVal(simAfter))}."
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = bg) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(verdict, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = fg)
                                Spacer(Modifier.height(4.dp))
                                Text("المتاح: ${fmt(simPool, privacy)} · المتبقي بعد الشراء: ${fmt(simAfter, privacy)}", style = MaterialTheme.typography.bodySmall, color = fg)
                            }
                        }
                    }
                }

                // Quick Expense Presets Editor
                SandooqiCard {
                    SectionHeader("تخصيص أزرار المصروف السريع", Icons.Filled.Bolt, ext.amber, subtitle = "تظهر في الشاشة الرئيسية للتسجيل بضغطة واحدة")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qeEmoji, onValueChange = { qeEmoji = it }, label = { Text("الأيقونة") }, singleLine = true, modifier = Modifier.weight(0.6f))
                        OutlinedTextField(value = qeTitle, onValueChange = { qeTitle = it }, label = { Text("الاسم *") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        OutlinedTextField(value = qeAmt, onValueChange = { qeAmt = FinanceUtils.normArDigits(it) }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.9f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val a = qeAmt.toDoubleOrNull() ?: 0.0
                            if (qeTitle.isNotBlank() && a > 0) {
                                onAddQuickExpensePreset(qeTitle.trim(), a, qeEmoji.trim().ifBlank { "🛍️" })
                                qeTitle = ""; qeAmt = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_quick_preset_btn"),
                    ) { Text("+ إضافة زر سريع") }

                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        db.quickExpenses.forEach { q ->
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                                    Text("${q.emoji} ${q.title} (${fmt(q.amount, privacy)})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { onDeleteQuickExpensePreset(q.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "حذف ${q.title}", tint = ext.rose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "analytics" -> {
                // Member & Smoke Analytics
                SandooqiCard {
                    SectionHeader("مصاريف كل فرد ومتابعة الدخان", Icons.Filled.People, ext.emerald)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        db.family.forEach { m ->
                            FilterChip(selected = faPerson == m, onClick = { faPerson = m }, label = { Text(m) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("الشهر: ${formatYM(faMonth)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row {
                            TextButton(onClick = { faMonth = shiftYM(faMonth, -1) }) { Text("السابق") }
                            TextButton(onClick = { faMonth = shiftYM(faMonth, 1) }) { Text("التالي") }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiMiniCard("مجموع الفرد", fmt(memberStats[0], privacy), Modifier.weight(1f))
                        KpiMiniCard("مصروف الدخان", fmt(memberStats[1], privacy), Modifier.weight(1f), valueColor = ext.rose)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiMiniCard("باقي المصاريف", fmt(memberStats[2], privacy), Modifier.weight(1f))
                        KpiMiniCard("الدخان (السابق)", fmt(memberStats[3], privacy), Modifier.weight(1f), valueColor = ext.amber)
                    }
                }

                // Where did my money go?
                SandooqiCard {
                    SectionHeader(
                        title = "أين ذهبت أموالي؟ (${formatYM(miMonth)})",
                        icon = Icons.Filled.ReceiptLong,
                        iconTint = ext.sky,
                        trailing = {
                            Row {
                                TextButton(onClick = { miMonth = shiftYM(miMonth, -1) }) { Text("السابق") }
                                TextButton(onClick = { miMonth = shiftYM(miMonth, 1) }) { Text("التالي") }
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("أكثر ثلاثة أبواب استهلاكاً", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    if (whereMoneyWent.first.isEmpty()) {
                        EmptyHintText("لا توجد مصاريف مسجلة لهذا الشهر.")
                    } else {
                        whereMoneyWent.first.forEachIndexed { idx, (label, amount) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${idx + 1}. $label", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(fmt(amount, privacy), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = ext.rose)
                            }
                            HorizontalDivider()
                        }
                    }
                    if (whereMoneyWent.second.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("ملخص المصاريف اليومية", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        whereMoneyWent.second.take(20).forEach { (day, amt) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(day, style = MaterialTheme.typography.bodySmall)
                                Text(fmt(amt, privacy), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ext.rose)
                            }
                        }
                    }
                }
            }

            "accounts" -> ToolsAccountsAndWarrantiesTab(
                db = db,
                onAddAccountBook = onAddAccountBook,
                onAddAccountBookEntry = onAddAccountBookEntry,
                onDeleteAccountBook = onDeleteAccountBook,
                onAddWarranty = onAddWarranty,
                onDeleteWarranty = onDeleteWarranty,
                onPreviewPhoto = onPreviewPhoto,
                onCompressUri = onCompressUri,
            )

            else -> ToolsSettingsTab(
                db = db,
                onAddFamilyMember = onAddFamilyMember,
                onRemoveFamilyMember = onRemoveFamilyMember,
                onEnableFamilyMode = onEnableFamilyMode,
                onUpdateSewingRates = onUpdateSewingRates,
                onAddCatalogProduct = onAddCatalogProduct,
                onToggleDarkMode = onToggleDarkMode,
                onToggleSound = onToggleSound,
                onToggleAppLock = onToggleAppLock,
                onLockNow = onLockNow,
                onChangePin = onChangePin,
                exportBackupJson = exportBackupJson,
                onImportBackupJson = onImportBackupJson,
                onSelectiveReset = onSelectiveReset,
                onResetAll = onResetAll,
                onNotify = onNotify,
            )
        }
    }
}
