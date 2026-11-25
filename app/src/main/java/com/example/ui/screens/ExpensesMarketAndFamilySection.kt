package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.getTxYM
import com.example.model.SandooqiDB
import com.example.ui.components.EmptyHintText
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

private val FAMILY_CATEGORIES = listOf(
    "غذاء ومأكولات",
    "ملابس وأحذية",
    "طبيب وعلاج وأدوية",
    "تعليم ودراسة",
    "نقل ومواصلات",
    "هاتف وإنترنت",
    "مستلزمات شخصية",
    "ترفيه ونزهات",
    "هدايا ومناسبات",
    "دخان",
    "أخرى",
)
private val MARKET_UNITS = listOf("حبة", "كغ", "غ", "لتر", "مل", "كيس", "علبة", "ربطة", "صندوق")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyMemberExpensesSection(
    db: SandooqiDB,
    onAddFamilyExpense: (person: String, category: String, amount: Double) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }

    var famPerson by remember(db.family) { mutableStateOf(db.family.firstOrNull() ?: "") }
    var famCat by remember { mutableStateOf("غذاء ومأكولات") }
    var famCatOther by remember { mutableStateOf("") }
    var famAmt by remember { mutableStateOf("") }
    var expandedPerson by remember { mutableStateOf<String?>(null) }

    val familyMonthGroups = remember(db.tx, ym) {
        db.tx
            .filter { getTxYM(it) == ym && it.type == "out" && it.cat == "fam" }
            .groupBy { (it.person ?: "فرد العائلة").ifBlank { "فرد العائلة" } }
            .map { (p, items) -> Triple(p, items.sumOf { it.amount }, items) }
    }
    val grandTotal = remember(familyMonthGroups) { familyMonthGroups.sumOf { it.second } }

    SandooqiCard {
        SectionHeader(title = "مصاريف أفراد العائلة", icon = Icons.Filled.Group, iconTint = ext.sky)
        Spacer(Modifier.height(8.dp))

        Text("اختر الفرد:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            db.family.forEach { member ->
                FilterChip(
                    selected = famPerson == member,
                    onClick = { famPerson = member },
                    label = { Text(member) },
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("نوع المصروف:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FAMILY_CATEGORIES.forEach { c ->
                FilterChip(selected = famCat == c, onClick = { famCat = c }, label = { Text(c) })
            }
        }

        if (famCat == "أخرى") {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = famCatOther,
                onValueChange = { famCatOther = it },
                label = { Text("اكتب نوع المصروف") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = famAmt,
            onValueChange = { famAmt = FinanceUtils.normArDigits(it) },
            label = { Text("المبلغ (دج)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val amt = famAmt.toDoubleOrNull() ?: 0.0
                val category = if (famCat == "أخرى" && famCatOther.isNotBlank()) famCatOther.trim() else famCat
                if (famPerson.isNotBlank() && amt > 0) {
                    onAddFamilyExpense(famPerson, category, amt)
                    famAmt = ""
                    famCatOther = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ext.sky),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_family_expense_button"),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Wallet, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("تسجيل مصروف الفرد", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ext.skyContainer,
            border = BorderStroke(1.dp, ext.sky.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("إجمالي مصاريف جميع أفراد العائلة", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(fmt(grandTotal, privacy), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = ext.rose)
            }
        }

        Spacer(Modifier.height(8.dp))
        if (familyMonthGroups.isEmpty()) {
            EmptyHintText("لا توجد مصاريف عائلية مسجلة لهذا الشهر.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                familyMonthGroups.forEach { (personName, total, items) ->
                    val open = expandedPerson == personName
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ext.cardBorder),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedPerson = if (open) null else personName }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(personName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${items.size} مصروفات · ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(fmt(total, privacy), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = ext.rose)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                    )
                                }
                            }
                            if (open) {
                                HorizontalDivider()
                                items.forEachIndexed { idx, t ->
                                    if (idx > 0) HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(t.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(t.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(fmt(t.amount, privacy), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ext.rose)
                                            IconButton(onClick = { onDeleteTransaction(t.id) }) {
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
}

@Composable
fun MarketListSection(
    db: SandooqiDB,
    onAddMarketItem: (name: String, quantity: Double, unit: String, price: Double) -> Unit,
    onUpdateMarketItem: (id: Long, quantity: Double?, unit: String?, price: Double?) -> Unit,
    onToggleMarketItem: (id: Long, recordExpense: Boolean) -> Unit,
    onDeleteMarketItem: (Long) -> Unit,
    onClearDoneMarket: () -> Unit,
    onUpdateMarketBudget: (Double) -> Unit,
    onNotify: (String) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current

    var mktName by remember { mutableStateOf("") }
    var mktQty by remember { mutableStateOf("1") }
    var mktUnit by remember { mutableStateOf("حبة") }
    var mktPrice by remember { mutableStateOf("") }
    var autoExpense by remember { mutableStateOf(true) }
    var budgetDraft by remember(db.marketBudget) { mutableStateOf(db.marketBudget.roundToInt().toString()) }

    val boughtTotal = remember(db.marketList) {
        FinanceUtils.round2(db.marketList.filter { it.done }.sumOf { it.price * max(0.0, it.quantity) })
    }
    val pendingTotal = remember(db.marketList) {
        FinanceUtils.round2(db.marketList.filter { !it.done }.sumOf { it.price * max(0.0, it.quantity) })
    }
    val marketTotal = FinanceUtils.round2(boughtTotal + pendingTotal)
    val remainingBudget = FinanceUtils.round2(max(0.0, db.marketBudget - boughtTotal))
    val overBudget = FinanceUtils.round2(max(0.0, boughtTotal - db.marketBudget))
    val marketPct = if (db.marketBudget > 0) ((boughtTotal / db.marketBudget) * 100.0).roundToInt().coerceIn(0, 100) else 0

    val formatShareText = {
        val lines = db.marketList.joinToString("\n") { m ->
            val q = max(0.0, m.quantity)
            "${if (m.done) "✅" else "⬜"} ${m.name} (${FinanceUtils.fmtQty(q)} ${m.unit})" +
                if (m.price > 0) " — ${fmt(FinanceUtils.round2(m.price * q))}" else ""
        }
        "🛍️ قائمة السوق:\n$lines\nالمشترى: ${fmt(boughtTotal)} | الإجمالي التقديري: ${fmt(marketTotal)}"
    }

    SandooqiCard {
        SectionHeader(
            title = "قائمة التسوق (السوق)",
            icon = Icons.Filled.ShoppingBag,
            iconTint = ext.amber,
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "المشترى فعلياً: ${fmt(boughtTotal, privacy)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ext.emerald,
            )
            Text(
                if (overBudget > 0) "تجاوز السقف بـ: ${fmt(overBudget, privacy)}"
                else "المتبقي من السقف: ${fmt(remainingBudget, privacy)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (overBudget > 0) ext.rose else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        StatusProgressBar(
            percent = marketPct,
            barColor = if (marketPct >= 90) ext.rose else ext.amber,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "السقف: ${fmt(db.marketBudget, privacy)} · في الانتظار (غير معلّم): ${fmt(pendingTotal, privacy)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = budgetDraft,
                onValueChange = { budgetDraft = FinanceUtils.normArDigits(it) },
                label = { Text("سقف السوق (دج)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { onUpdateMarketBudget(budgetDraft.toDoubleOrNull() ?: 0.0) }) {
                Text("حفظ السقف")
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = mktName,
            onValueChange = {
                mktName = it
                mktUnit = FinanceUtils.suggestMarketUnit(it)
            },
            label = { Text("غرض السوق (بطاطا، زيت...)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = mktQty,
                onValueChange = { mktQty = FinanceUtils.normArDigits(it) },
                label = { Text("الكمية") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(0.5f),
            )
            OutlinedTextField(
                value = mktPrice,
                onValueChange = { mktPrice = FinanceUtils.normArDigits(it) },
                label = { Text("سعر الوحدة (دج)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MARKET_UNITS.forEach { u ->
                FilterChip(selected = mktUnit == u, onClick = { mktUnit = u }, label = { Text(u) })
            }
        }

        Spacer(Modifier.height(6.dp))
        Button(
            onClick = {
                if (mktName.isNotBlank()) {
                    onAddMarketItem(
                        mktName.trim(),
                        max(0.1, mktQty.toDoubleOrNull() ?: 1.0),
                        mktUnit,
                        max(0.0, mktPrice.toDoubleOrNull() ?: 0.0),
                    )
                    mktName = ""
                    mktQty = "1"
                    mktPrice = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_market_item_button"),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("إضافة لقائمة السوق", fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = autoExpense, onCheckedChange = { autoExpense = it })
            Text("تسجيل كمصروف تلقائياً عند التحديد كـ مقتنى", style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (db.marketList.isNotEmpty()) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Sandooqi Market", formatShareText()))
                        onNotify("تم نسخ قائمة السوق")
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("نسخ")
            }
            OutlinedButton(
                onClick = {
                    if (db.marketList.isNotEmpty()) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, formatShareText())
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة قائمة السوق"))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("مشاركة")
            }
        }

        if (db.marketList.any { it.done }) {
            TextButton(onClick = onClearDoneMarket) {
                Text("مسح المقتناة ✓", color = ext.rose)
            }
        }

        Spacer(Modifier.height(6.dp))
        if (db.marketList.isEmpty()) {
            EmptyHintText("قائمة السوق فارغة.")
        } else {
            db.marketList.forEachIndexed { idx, item ->
                if (idx > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { onToggleMarketItem(item.id, autoExpense) },
                        )
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                            )
                            Text(
                                text = "${FinanceUtils.fmtQty(item.quantity)} ${item.unit}" +
                                    if (item.price > 0) " · ${fmt(FinanceUtils.round2(item.price * max(0.0, item.quantity)), privacy)}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            onUpdateMarketItem(item.id, FinanceUtils.round2(max(0.0, item.quantity) + 1.0), null, null)
                        }) {
                            Text("+1")
                        }
                        IconButton(onClick = { onDeleteMarketItem(item.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                        }
                    }
                }
            }
        }
    }
}
