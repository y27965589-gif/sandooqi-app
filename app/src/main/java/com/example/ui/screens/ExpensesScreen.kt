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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.getTxYM
import com.example.data.FinanceUtils.localDateISO
import com.example.model.SandooqiDB
import com.example.ui.components.BadgeTag
import com.example.ui.components.EmptyHintText
import com.example.ui.components.PhotoAttachmentButton
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

private val EXPENSE_CATEGORIES = listOf(
    "مواد غذائية", "خضر وفواكه", "لحوم وأسماك", "نقل ومواصلات",
    "صحة ودواء", "لوازم المنزل", "تعليم", "ملابس", "ترفيه", "أخرى",
)
private val UNITS = listOf("حبة", "كغ", "غ", "لتر", "مل", "كيس", "علبة", "ربطة", "صندوق")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    db: SandooqiDB,
    onAddExpense: (
        title: String,
        amount: Double,
        person: String,
        shop: String,
        category: String?,
        dateISO: String?,
        payMode: String,
        creditor: String,
        due: String?,
        photo: String?,
    ) -> Unit,
    onAddFamilyExpense: (person: String, category: String, amount: Double) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddMarketItem: (name: String, quantity: Double, unit: String, price: Double) -> Unit,
    onUpdateMarketItem: (id: Long, quantity: Double?, unit: String?, price: Double?) -> Unit,
    onToggleMarketItem: (id: Long, recordExpense: Boolean) -> Unit,
    onDeleteMarketItem: (Long) -> Unit,
    onClearDoneMarket: () -> Unit,
    onUpdateMarketBudget: (Double) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onNotify: (String) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }

    var subTab by remember { mutableStateOf("products") } // "products" | "market" | "history"
    var activeGroup by remember { mutableStateOf("الكل") }
    var search by remember { mutableStateOf("") }
    var selectedItems by remember { mutableStateOf(listOf<String>()) }

    var customTitle by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("حبة") }
    var showQtyBox by remember { mutableStateOf(false) }
    var expenseCategory by remember { mutableStateOf("مواد غذائية") }
    var expenseDate by remember { mutableStateOf(localDateISO()) }
    var person by remember(db.family) { mutableStateOf(db.family.firstOrNull() ?: "") }
    var shop by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("cash") }
    var creditor by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }

    var historySearch by remember { mutableStateOf("") }

    val groups = remember(db.catalog) { listOf("الكل") + db.catalog.map { it.group }.distinct() }
    val filteredCatalog = remember(db.catalog, activeGroup, search) {
        val q = search.trim().lowercase()
        db.catalog.filter { c ->
            (activeGroup == "الكل" || c.group == activeGroup) &&
                (q.isBlank() || c.name.lowercase().contains(q))
        }
    }

    val monthExpenses = remember(db.tx, ym) {
        db.tx.filter {
            getTxYM(it) == ym &&
                it.type == "out" &&
                FinanceUtils.transactionAccount(it) == "family" &&
                it.cat != "transfer" &&
                it.cat != "account_transfer" &&
                it.cat != "sav" &&
                it.source != "credit-purchase" &&
                it.cat != "credit_purchase"
        }
    }
    val monthExpenseTotal = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val groceryPct = if (db.groceryBudget > 0) ((monthExpenseTotal / db.groceryBudget) * 100.0).roundToInt().coerceIn(0, 100) else 0

    val existingCreditors = remember(db.homeDebts) {
        db.homeDebts.map { it.creditor }.filter { it.isNotBlank() }.distinct()
    }

    val computedTotal = remember(unitPrice, qty, showQtyBox) {
        val p = max(0.0, unitPrice.toDoubleOrNull() ?: 0.0)
        val q = if (showQtyBox) max(0.01, qty.toDoubleOrNull() ?: 1.0) else 1.0
        FinanceUtils.round2(p * q)
    }

    val filteredHistory = remember(monthExpenses, historySearch) {
        val q = historySearch.trim().lowercase()
        if (q.isBlank()) monthExpenses
        else monthExpenses.filter {
            it.name.lowercase().contains(q) ||
                (it.shop ?: "").lowercase().contains(q) ||
                (it.person ?: "").lowercase().contains(q)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 3 Sub-Tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "products" to "المنتجات والتسجيل",
                "market" to "قائمة السوق (${db.marketList.count { !it.done }})",
                "history" to "السجل (${monthExpenses.size})",
            ).forEach { (k, lbl) ->
                FilterChip(
                    selected = subTab == k,
                    onClick = { subTab = k },
                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (subTab == "products") {
            // Grocery Budget Meter
            SandooqiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("مصاريف هذا الشهر مقابل السقف", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${fmt(monthExpenseTotal, privacy)} / ${fmt(db.groceryBudget, privacy)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (groceryPct >= 90) ext.rose else ext.emerald,
                    )
                }
                Spacer(Modifier.height(6.dp))
                StatusProgressBar(
                    percent = groceryPct,
                    barColor = when {
                        groceryPct >= 90 -> ext.rose
                        groceryPct >= 75 -> ext.amber
                        else -> ext.emerald
                    },
                )
            }

            // Catalog Picker + Expense Form
            SandooqiCard {
                SectionHeader(title = "الكتالوج السريع وتفاصيل المصروف")
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("بحث سريع عن منتج...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    groups.forEach { g ->
                        FilterChip(
                            selected = activeGroup == g,
                            onClick = { activeGroup = g },
                            label = { Text(g) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Catalog Grid (2 columns compact chips)
                val shownCatalog = filteredCatalog.take(36)
                shownCatalog.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pair.forEach { item ->
                            val picked = item.name in selectedItems
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (picked) ext.emeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (picked) ext.emerald else ext.cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedItems = if (picked) selectedItems - item.name else selectedItems + item.name
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${item.emoji} ${item.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (picked) ext.onEmeraldContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (picked) {
                                        Icon(Icons.Filled.Check, contentDescription = "محدد", tint = ext.emerald, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                if (selectedItems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedItems.forEach { name ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedItems = selectedItems - name },
                                label = { Text("$name ✕") },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("منتج إضافي / اسم المشتريات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = FinanceUtils.normArDigits(it) },
                        label = { Text(if (showQtyBox) "سعر الوحدة (دج)" else "المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showQtyBox = !showQtyBox }) {
                        Text(if (showQtyBox) "إخفاء الكمية" else "+ كمية ووحدة")
                    }
                }

                if (showQtyBox) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = FinanceUtils.normArDigits(it) },
                            label = { Text("الكمية") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.5f),
                        )
                        Row(
                            modifier = Modifier
                                .weight(1.5f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            UNITS.forEach { u ->
                                FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("الفئة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        FilterChip(selected = expenseCategory == cat, onClick = { expenseCategory = cat }, label = { Text(cat) })
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("المشتري:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    db.family.forEach { m ->
                        FilterChip(selected = person == m, onClick = { person = m }, label = { Text(m) })
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shop,
                        onValueChange = { shop = it },
                        label = { Text("المحل (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        label = { Text("التاريخ (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payMode == "cash",
                        onClick = { payMode = "cash" },
                        label = { Text("دفع نقدي (كاش)") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = payMode == "debt",
                        onClick = { payMode = "debt" },
                        label = { Text("دين على المحل (كريدي)") },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (payMode == "debt") {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = creditor,
                            onValueChange = { creditor = it },
                            label = { Text("اسم الدائن / البائع") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = due,
                            onValueChange = { due = it },
                            label = { Text("الاستحقاق (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (existingCreditors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            existingCreditors.take(8).forEach { c ->
                                FilterChip(selected = creditor == c, onClick = { creditor = c }, label = { Text(c) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                PhotoAttachmentButton(
                    hasPhoto = photo.isNotBlank(),
                    labelIdle = "إرفاق صورة الوصل",
                    onCompressUri = onCompressUri,
                    onPhotoReady = { photo = it },
                    onClearPhoto = { photo = "" },
                )

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (computedTotal <= 0) return@Button
                        val baseTitle = (selectedItems + listOf(customTitle.trim()))
                            .filter { it.isNotBlank() }
                            .joinToString("، ")
                            .ifBlank { "مشتريات منزلية" }
                        val qtyVal = max(0.01, qty.toDoubleOrNull() ?: 1.0)
                        val suffix = if (showQtyBox) " (${FinanceUtils.fmtQty(qtyVal)} $unit)" else ""
                        onAddExpense(
                            "$baseTitle$suffix",
                            computedTotal,
                            person.ifBlank { db.family.firstOrNull() ?: "" },
                            shop.trim(),
                            expenseCategory,
                            expenseDate,
                            payMode,
                            creditor.trim().ifBlank { shop.trim().ifBlank { "محل" } },
                            due.trim().ifBlank { null },
                            photo.ifBlank { null },
                        )
                        selectedItems = emptyList()
                        customTitle = ""
                        unitPrice = ""
                        qty = "1"
                        shop = ""
                        creditor = ""
                        due = ""
                        photo = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (payMode == "debt") ext.amber else ext.emerald,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_expense_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (payMode == "debt") {
                            "تسجيل دين (${fmt(computedTotal)})"
                        } else {
                            "تأكيد وخصم (${fmt(computedTotal)})"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Family Expenses Card + Market List Card directly visible on Products tab
            FamilyMemberExpensesSection(
                db = db,
                onAddFamilyExpense = onAddFamilyExpense,
                onDeleteTransaction = onDeleteTransaction,
            )

            MarketListSection(
                db = db,
                onAddMarketItem = onAddMarketItem,
                onUpdateMarketItem = onUpdateMarketItem,
                onToggleMarketItem = onToggleMarketItem,
                onDeleteMarketItem = onDeleteMarketItem,
                onClearDoneMarket = onClearDoneMarket,
                onUpdateMarketBudget = onUpdateMarketBudget,
                onNotify = onNotify,
            )
        } else if (subTab == "market") {
            MarketListSection(
                db = db,
                onAddMarketItem = onAddMarketItem,
                onUpdateMarketItem = onUpdateMarketItem,
                onToggleMarketItem = onToggleMarketItem,
                onDeleteMarketItem = onDeleteMarketItem,
                onClearDoneMarket = onClearDoneMarket,
                onUpdateMarketBudget = onUpdateMarketBudget,
                onNotify = onNotify,
            )
        } else {
            // History Sub-tab
            SandooqiCard {
                SectionHeader(
                    title = "مشتريات ومصاريف هذا الشهر",
                    trailing = {
                        Text(
                            text = fmt(monthExpenseTotal, privacy),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ext.rose,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = historySearch,
                    onValueChange = { historySearch = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("بحث في المشتريات أو المحل أو الفرد...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (filteredHistory.isEmpty()) {
                    EmptyHintText("لا توجد مصاريف منزلية مطابقة في هذا الشهر.")
                } else {
                    filteredHistory.forEachIndexed { idx, t ->
                        if (idx > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = buildString {
                                        append(t.date)
                                        if (!t.person.isNullOrBlank()) append(" · ${t.person}")
                                        if (!t.shop.isNullOrBlank()) append(" · ${t.shop}")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!t.photo.isNullOrBlank()) {
                                    TextButton(onClick = { onPreviewPhoto(t.photo, t.name) }) {
                                        Text("عرض صورة الوصل", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = fmt(t.amount, privacy),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ext.rose,
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
}
