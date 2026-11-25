package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.PdfExportHelper
import com.example.model.HomeDebt
import com.example.model.SandooqiDB
import com.example.ui.components.BadgeTag
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyHintText
import com.example.ui.components.InputAmountDialog
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.PhotoAttachmentButton
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.roundToInt

private val DEBT_ACTIVITIES = listOf(
    "مواد غذائية", "خضر", "دجاج ولحوم", "لباس", "دخان",
    "ماكياج", "كانكايري", "دواء", "توابل", "فواتير", "أخرى",
)

private data class CreditorGroup(
    val groupKey: String,
    val creditor: String,
    val direction: String,
    val activity: String,
    val total: Double,
    val paid: Double,
    val remaining: Double,
    val nearestDue: String?,
    val items: List<HomeDebt>,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillsDebtsScreen(
    db: SandooqiDB,
    onAddHomeDebt: (creditor: String, item: String, activity: String, amount: Double, due: String?, direction: String, photo: String?) -> Unit,
    onPayHomeDebt: (debt: HomeDebt, payAmt: Double, applyTreasury: Boolean) -> Unit,
    onPayCreditorAll: (creditor: String, amt: Double, direction: String, applyTreasury: Boolean) -> Unit,
    onDeleteHomeDebt: (Long) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onAddRentRecord: (direction: String, unit: String, party: String, amount: Double, due: String?, immediate: Boolean) -> Unit,
    onCollectRent: (Long) -> Unit,
    onDeleteRent: (Long) -> Unit,
    onAddObligation: (name: String, amount: Double, dueDay: Int, creditor: String?, photo: String?) -> Unit,
    onToggleObligationPaid: (Long) -> Unit,
    onDeleteObligation: (Long) -> Unit,
    onAddBillEntry: (name: String, amount: Double, creditor: String, status: String, due: String?, unpaidMonths: Int, recurring: Boolean, photo: String?) -> Unit,
    onAddSalaryDeduction: (facAmt: Double, facNote: String, srvAmt: Double, srvNote: String, srvMonth: String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddFacPlan: (name: String, monthly: Double, months: Int) -> Unit,
    onPayFacMonth: (Long) -> Unit,
    onDeleteFacPlan: (Long) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current
    val ym = remember(db.lastActiveYM) { currentYM() }

    var tab by remember { mutableStateOf("debts") } // "debts" | "bills" | "deductions"

    // Tab 1 State
    var showRentBox by remember { mutableStateOf(false) }
    var rentDir by remember { mutableStateOf("in") }
    var rentUnit by remember { mutableStateOf("كراء سكن") }
    var rentParty by remember { mutableStateOf("") }
    var rentAmt by remember { mutableStateOf("") }
    var rentDue by remember { mutableStateOf("") }
    var rentImmediate by remember { mutableStateOf(true) }
    var rentFilter by remember { mutableStateOf("all") }

    var debtDir by remember { mutableStateOf("out") }
    var creditor by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var debtAmt by remember { mutableStateOf("") }
    var debtDue by remember { mutableStateOf("") }
    var debtActivity by remember { mutableStateOf("مواد غذائية") }
    var debtPhoto by remember { mutableStateOf("") }

    var debtSearch by remember { mutableStateOf("") }
    var debtDirFilter by remember { mutableStateOf("all") }
    var showFilters by remember { mutableStateOf(false) }
    var filterMonth by remember { mutableStateOf("all") }
    var filterActivity by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("balance") }
    var expandedKey by remember { mutableStateOf<String?>(null) }

    var paySingleTarget by remember { mutableStateOf<HomeDebt?>(null) }
    var payAllTarget by remember { mutableStateOf<CreditorGroup?>(null) }
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    val creditorChips = remember(db.homeDebts) { db.homeDebts.map { it.creditor }.filter { it.isNotBlank() }.distinct() }
    val debtMonths = remember(db.homeDebts) { db.homeDebts.map { it.ym }.filter { it.isNotBlank() }.distinct().sortedDescending() }

    val groupedCreditors = remember(db.homeDebts, debtSearch, debtDirFilter, filterMonth, filterActivity, sortBy) {
        val q = debtSearch.trim().lowercase()
        val map = mutableMapOf<String, MutableList<HomeDebt>>()
        for (d in db.homeDebts) {
            val dir = if (d.direction == "in") "in" else "out"
            if (debtDirFilter != "all" && dir != debtDirFilter) continue
            if (filterMonth != "all" && d.ym != filterMonth) continue
            if (filterActivity != "all" && d.activity.ifBlank { "أخرى" } != filterActivity) continue
            if (q.isNotBlank() &&
                !d.creditor.lowercase().contains(q) &&
                !d.item.lowercase().contains(q) &&
                !d.activity.lowercase().contains(q)
            ) continue
            val nameKey = d.creditor.ifBlank { "غير محدد" }
            map.getOrPut("$dir::$nameKey") { mutableListOf() }.add(d)
        }
        val list = map.map { (gKey, items) ->
            val first = items.first()
            val dir = if (first.direction == "in") "in" else "out"
            val total = items.sumOf { it.amount }
            val paid = items.sumOf { it.paid }
            val rem = items.sumOf { debtRemaining(it) }
            val nearest = items.filter { debtRemaining(it) > 0 && !it.due.isNullOrBlank() }.minOfOrNull { it.due!! }
            CreditorGroup(gKey, first.creditor.ifBlank { "غير محدد" }, dir, first.activity.ifBlank { "مواد غذائية" }, total, paid, rem, nearest, items)
        }
        when (sortBy) {
            "name" -> list.sortedBy { it.creditor }
            "due" -> list.sortedWith(compareBy<CreditorGroup> { it.nearestDue ?: "9999-99-99" }.thenByDescending { it.remaining })
            else -> list.sortedByDescending { it.remaining }
        }
    }

    val totalOpenOut = remember(db.homeDebts) { db.homeDebts.filter { it.direction != "in" }.sumOf { debtRemaining(it) } }
    val totalOpenIn = remember(db.homeDebts) { db.homeDebts.filter { it.direction == "in" }.sumOf { debtRemaining(it) } }
    val rentInPending = remember(db.rentDebts) { db.rentDebts.filter { it.direction == "in" && !it.settled }.sumOf { it.amount } }
    val rentOutPending = remember(db.rentDebts) { db.rentDebts.filter { it.direction == "out" && !it.settled }.sumOf { it.amount } }
    val unpaidOblCount = remember(db.obligations, ym) { db.obligations.count { ym !in it.paidMonths } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 3 Sub-Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val openCount = db.homeDebts.count { debtRemaining(it) > 0 }
            listOf(
                "debts" to "الديون ($openCount)",
                "bills" to "الفواتير ($unpaidOblCount)",
                "deductions" to "الاقتطاعات (${db.facPlans.size})",
            ).forEach { (k, lbl) ->
                FilterChip(
                    selected = tab == k,
                    onClick = { tab = k },
                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (tab == "debts") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("ديون عليّ (مفتوحة)", fmt(totalOpenOut, privacy), valueColor = ext.rose, modifier = Modifier.weight(1f))
                KpiMiniCard("مستحقات لي (عند الغير)", fmt(totalOpenIn, privacy), valueColor = ext.emerald, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("إيجارات ومستحقات لي", fmt(rentInPending, privacy), valueColor = ext.sky, modifier = Modifier.weight(1f))
                KpiMiniCard("كراء والتزام عليّ", fmt(rentOutPending, privacy), valueColor = ext.amber, modifier = Modifier.weight(1f))
            }

            // Collapsible Rent Section
            SandooqiCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRentBox = !showRentBox },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = ext.sky, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تسيير الكراء والمستحقات (${db.rentDebts.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Icon(if (showRentBox) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                }

                if (showRentBox) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = rentDir == "in", onClick = { rentDir = "in" }, label = { Text("كراء / مستحق لي (+)") })
                        FilterChip(selected = rentDir == "out", onClick = { rentDir = "out" }, label = { Text("كراء / التزام عليّ (−)") })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("كراء سكن", "كراء محل", "كراء قاعة", "مستحق شخصي").forEach { u ->
                            FilterChip(selected = rentUnit == u, onClick = { rentUnit = u }, label = { Text(u) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rentParty,
                            onValueChange = { rentParty = it },
                            label = { Text("الطرف / المستأجر") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = rentAmt,
                            onValueChange = { rentAmt = FinanceUtils.normArDigits(it) },
                            label = { Text("المبلغ (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rentImmediate, onCheckedChange = { rentImmediate = it })
                        Text("إضافة / خصم فوري في الخزينة العائلية", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            val amt = rentAmt.toDoubleOrNull() ?: 0.0
                            if (rentParty.isNotBlank() && amt > 0) {
                                onAddRentRecord(rentDir, rentUnit, rentParty.trim(), amt, rentDue.trim().ifBlank { null }, rentImmediate)
                                rentParty = ""
                                rentAmt = ""
                                rentDue = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("تسجيل حركة الكراء", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("all" to "الكل", "in" to "لي (+)", "out" to "عليّ (−)").forEach { (k, lbl) ->
                            FilterChip(selected = rentFilter == k, onClick = { rentFilter = k }, label = { Text(lbl) })
                        }
                    }
                    db.rentDebts.filter { rentFilter == "all" || it.direction == rentFilter }.forEach { r ->
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${r.unit} — ${r.party}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${fmt(r.amount, privacy)} · ${if (r.settled) "مسدّد" else "غير مسدّد"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (r.settled) ext.emerald else ext.amber,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!r.settled) {
                                    TextButton(onClick = { onCollectRent(r.id) }) { Text("تحصيل") }
                                }
                                IconButton(onClick = { onDeleteRent(r.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                                }
                            }
                        }
                    }
                }
            }

            // Add Debt Form (له / عليه)
            SandooqiCard {
                SectionHeader(title = "تسجيل دين جديد (له / عليه)", icon = Icons.Filled.Add)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = debtDir == "out",
                        onClick = { debtDir = "out" },
                        label = { Text("دين عليّ (عليه)") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = debtDir == "in",
                        onClick = { debtDir = "in" },
                        label = { Text("مستحق لي (له)") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (creditorChips.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        creditorChips.take(10).forEach { c ->
                            FilterChip(selected = creditor == c, onClick = { creditor = c }, label = { Text(c) })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditor,
                        onValueChange = { creditor = it },
                        label = { Text("الطرف / المحل") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = debtAmt,
                        onValueChange = { debtAmt = FinanceUtils.normArDigits(it) },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = item,
                        onValueChange = { item = it },
                        label = { Text("التفاصيل / المشتريات") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = debtDue,
                        onValueChange = { debtDue = it },
                        label = { Text("الاستحقاق (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DEBT_ACTIVITIES.forEach { act ->
                        FilterChip(selected = debtActivity == act, onClick = { debtActivity = act }, label = { Text(act) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                PhotoAttachmentButton(
                    hasPhoto = debtPhoto.isNotBlank(),
                    labelIdle = "صورة الوصل",
                    onCompressUri = onCompressUri,
                    onPhotoReady = { debtPhoto = it },
                    onClearPhoto = { debtPhoto = "" },
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val amt = debtAmt.toDoubleOrNull() ?: 0.0
                        if (creditor.isNotBlank() && amt > 0) {
                            onAddHomeDebt(
                                creditor.trim(),
                                item.trim().ifBlank { debtActivity },
                                debtActivity,
                                amt,
                                debtDue.trim().ifBlank { null },
                                debtDir,
                                debtPhoto.ifBlank { null },
                            )
                            creditor = ""
                            item = ""
                            debtAmt = ""
                            debtDue = ""
                            debtPhoto = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_home_debt_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (debtDir == "in") "تسجيل مستحق لي" else "تسجيل دين عليّ", fontWeight = FontWeight.Bold)
                }
            }

            // Creditor Directory
            SandooqiCard {
                SectionHeader(
                    title = "دليل الديون حسب الطرف (${groupedCreditors.size})",
                    trailing = {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "فلترة")
                        }
                    },
                )
                OutlinedTextField(
                    value = debtSearch,
                    onValueChange = { debtSearch = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("بحث باسم الدائن أو الغرض...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "الكل", "out" to "عليّ (−)", "in" to "لي (+)").forEach { (k, lbl) ->
                        FilterChip(selected = debtDirFilter == k, onClick = { debtDirFilter = k }, label = { Text(lbl) })
                    }
                }
                if (showFilters) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("balance" to "الأكثر مبلغاً", "due" to "الأقرب استحقاقاً", "name" to "الأبجدي").forEach { (k, lbl) ->
                            FilterChip(selected = sortBy == k, onClick = { sortBy = k }, label = { Text(lbl) })
                        }
                    }
                    if (debtMonths.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilterChip(selected = filterMonth == "all", onClick = { filterMonth = "all" }, label = { Text("كل الأشهر") })
                            debtMonths.forEach { m ->
                                FilterChip(selected = filterMonth == m, onClick = { filterMonth = m }, label = { Text(formatYM(m)) })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (groupedCreditors.isEmpty()) {
                    EmptyHintText("لا توجد ديون مسجلة حالياً.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groupedCreditors.forEach { grp ->
                            val open = expandedKey == grp.groupKey
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ext.cardBorder),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedKey = if (open) null else grp.groupKey },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(grp.creditor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp))
                                                BadgeTag(
                                                    text = if (grp.direction == "in") "له" else "عليه",
                                                    containerColor = if (grp.direction == "in") ext.emeraldContainer else ext.roseContainer,
                                                    contentColor = if (grp.direction == "in") ext.onEmeraldContainer else ext.onRoseContainer,
                                                )
                                            }
                                            Text(
                                                text = "${grp.activity} · ${grp.items.size} سجلات · إجمالي ${fmt(grp.total, privacy)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (grp.remaining > 0) fmt(grp.remaining, privacy) else "خالص ✓",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (grp.remaining <= 0) ext.emerald else if (grp.direction == "in") ext.sky else ext.rose,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (grp.remaining > 0) {
                                            Button(
                                                onClick = { payAllTarget = grp },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (grp.direction == "in") ext.emerald else ext.amber,
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (grp.direction == "in") "تحصيل دفعة" else "تسديد دفعة")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val file = PdfExportHelper.generateDebtStatementPdf(
                                                    context = context,
                                                    creditor = grp.creditor,
                                                    direction = grp.direction,
                                                    activity = grp.activity,
                                                    items = grp.items,
                                                )
                                                PdfExportHelper.sharePdf(context, file, "كشف حساب ${grp.creditor} PDF")
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = if (grp.remaining > 0) Modifier else Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "كشف حساب PDF", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("كشف PDF")
                                        }
                                    }

                                    if (open) {
                                        Spacer(Modifier.height(8.dp))
                                        grp.items.forEach { d ->
                                            val rem = debtRemaining(d)
                                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(d.item, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        text = "المتبقي: ${fmt(rem, privacy)} / ${fmt(d.amount, privacy)} · ${d.date}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    if (!d.photo.isNullOrBlank()) {
                                                        TextButton(onClick = { onPreviewPhoto(d.photo, "${d.creditor} — ${d.item}") }) {
                                                            Text("عرض الصورة", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (rem > 0) {
                                                        TextButton(onClick = { paySingleTarget = d }) {
                                                            Text(if (grp.direction == "in") "تحصيل" else "تسديد")
                                                        }
                                                    }
                                                    IconButton(onClick = { deleteConfirmId = d.id }) {
                                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (tab == "bills") {
            BillsTabSection(
                db = db,
                onAddObligation = onAddObligation,
                onToggleObligationPaid = onToggleObligationPaid,
                onDeleteObligation = onDeleteObligation,
                onAddBillEntry = onAddBillEntry,
                onPayHomeDebt = { paySingleTarget = it },
                onDeleteHomeDebt = { deleteConfirmId = it },
                onPreviewPhoto = onPreviewPhoto,
                onCompressUri = onCompressUri,
            )
        } else {
            DeductionsTabSection(
                db = db,
                onAddSalaryDeduction = onAddSalaryDeduction,
                onDeleteTransaction = onDeleteTransaction,
                onAddFacPlan = onAddFacPlan,
                onPayFacMonth = onPayFacMonth,
                onDeleteFacPlan = onDeleteFacPlan,
            )
        }
    }

    paySingleTarget?.let { debt ->
        val rem = debtRemaining(debt)
        val isIn = debt.direction == "in"
        InputAmountDialog(
            title = "${if (isIn) "تحصيل مستحق" else "تسديد دين"} — ${debt.creditor}",
            message = "المتبقي حالياً: ${fmt(rem)}",
            initialValue = rem.roundToInt().toString(),
            checkboxLabel = if (isIn) "إضافة المبلغ المحصّل للخزينة العائلية" else "خصم المبلغ من الخزينة العائلية",
            checkboxDefault = true,
            confirmLabel = if (isIn) "تحصيل الدفعة" else "تسديد الدفعة",
            onDismiss = { paySingleTarget = null },
            onSubmit = { amt, apply -> onPayHomeDebt(debt, amt, apply) },
        )
    }

    payAllTarget?.let { grp ->
        val isIn = grp.direction == "in"
        InputAmountDialog(
            title = "${if (isIn) "تحصيل مستحقات" else "تسديد ديون"} — ${grp.creditor}",
            message = "إجمالي المتبقي: ${fmt(grp.remaining)} (يوزّع تلقائياً على الأقدم أولاً)",
            initialValue = grp.remaining.roundToInt().toString(),
            checkboxLabel = if (isIn) "إضافة المبلغ المحصّل للخزينة العائلية" else "خصم المبلغ من الخزينة العائلية",
            checkboxDefault = true,
            confirmLabel = if (isIn) "تحصيل" else "تسديد",
            onDismiss = { payAllTarget = null },
            onSubmit = { amt, apply -> onPayCreditorAll(grp.creditor, amt, grp.direction, apply) },
        )
    }

    deleteConfirmId?.let { id ->
        ConfirmActionDialog(
            title = "حذف الدين",
            message = "هل تريد حذف سجل هذا الدين؟ يمكنك التراجع بعد الحذف.",
            confirmLabel = "حذف",
            danger = true,
            onDismiss = { deleteConfirmId = null },
            onConfirm = { onDeleteHomeDebt(id) },
        )
    }
}
