package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.nextId
import com.example.data.PdfExportHelper
import com.example.model.MeasurementRecord
import com.example.model.OrderFabricLine
import com.example.model.OrderMeasurements
import com.example.model.OrderSupplyLine
import com.example.model.SandooqiDB
import com.example.model.SewDebtRecord
import com.example.model.SewMaterial
import com.example.model.SewProjectRecord
import com.example.model.SewServiceRecord
import com.example.model.SewingOrder
import com.example.ui.components.BadgeTag
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyHintText
import com.example.ui.components.InputAmountDialog
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.PhotoAttachmentButton
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val STAGES = listOf("جديد", "قيد التفصيل", "جاهز", "مسلّم")
private val SUPPLY_OPTIONS = listOf(
    "خيط", "سحاب", "أزرار", "دانتيل", "طرز", "بطانة", "مطاط", "إبرة", "لوازم أخرى",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TailoringScreen(
    db: SandooqiDB,
    onAddOrder: (SewingOrder, List<Pair<Long, Double>>) -> Unit,
    onUpdateOrderStage: (Long, String) -> Unit,
    onCollectOrderPayment: (SewingOrder, Double) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onAddMeasurement: (MeasurementRecord) -> Unit,
    onDeleteMeasurement: (Long) -> Unit,
    onAddSewService: (SewServiceRecord) -> Unit,
    onDeleteSewService: (Long) -> Unit,
    onAddSewMaterial: (name: String, unit: String, qty: Double, cost: Double, minQty: Double) -> Unit,
    onUseSewMaterial: (SewMaterial, Double) -> Unit,
    onDeleteSewMaterial: (Long) -> Unit,
    onAddSewProject: (SewProjectRecord) -> Unit,
    onDeleteSewProject: (Long) -> Unit,
    onSewCashMovement: (type: String, name: String, amount: Double) -> Unit,
    onTransferSewingToFamily: (Double) -> Unit,
    onAddSewDebt: (type: String, name: String, amount: Double, due: String?) -> Unit,
    onPaySewDebt: (SewDebtRecord, Double, Boolean) -> Unit,
    onDeleteSewDebt: (Long) -> Unit,
    onNotify: (String) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current

    var sewTab by remember { mutableStateOf("orders") } // orders | calendar | clients | workshop | finance
    var showOrderForm by remember { mutableStateOf(false) }

    // Order form state
    var client by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var fittingDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var meterRate by remember(db.meterPrice) { mutableStateOf(db.meterPrice.roundToInt().toString()) }
    var flatLabor by remember { mutableStateOf("0") }
    var adjustment by remember { mutableStateOf("0") }
    var paid by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    var fabrics by remember { mutableStateOf(listOf<OrderFabricLine>()) }
    var fabSource by remember { mutableStateOf("customer") }
    var fabName by remember { mutableStateOf("") }
    var fabMeters by remember { mutableStateOf("") }
    var fabRate by remember { mutableStateOf("") }
    var fabBuyRate by remember { mutableStateOf("") }

    var supplies by remember { mutableStateOf(listOf<OrderSupplyLine>()) }
    var supName by remember { mutableStateOf("خيط") }
    var supCustom by remember { mutableStateOf("") }
    var supCost by remember { mutableStateOf("") }
    var stockPickId by remember { mutableStateOf<Long?>(null) }
    var stockPickQty by remember { mutableStateOf("1") }

    var ordMeas by remember { mutableStateOf(OrderMeasurements()) }
    var showMeasSection by remember { mutableStateOf(false) }
    var showEstSection by remember { mutableStateOf(false) }

    var estMat by remember { mutableStateOf("0") }
    var estHours by remember { mutableStateOf("0") }
    var estHourlyRate by remember { mutableStateOf("0") }
    var estExtra by remember { mutableStateOf("0") }
    var estProfitPct by remember { mutableStateOf("25") }

    var filterStage by remember { mutableStateOf("الكل") }
    var sortBy by remember { mutableStateOf("new") }
    var search by remember { mutableStateOf("") }

    var receiptOrder by remember { mutableStateOf<SewingOrder?>(null) }
    var collectTarget by remember { mutableStateOf<SewingOrder?>(null) }
    var deleteTarget by remember { mutableStateOf<SewingOrder?>(null) }

    // Order computed values
    val totalMeters = fabrics.sumOf { it.meters }
    val parsedRate = max(0.0, meterRate.toDoubleOrNull() ?: 0.0)
    val computedLabor = FinanceUtils.round2(
        if (totalMeters > 0) totalMeters * parsedRate else max(0.0, flatLabor.toDoubleOrNull() ?: 0.0)
    )
    val fabricsCharge = FinanceUtils.round2(fabrics.sumOf { it.charge })
    val fabricsCost = FinanceUtils.round2(fabrics.sumOf { it.cost })
    val suppliesCost = FinanceUtils.round2(supplies.sumOf { it.cost })
    val parsedAdj = adjustment.toDoubleOrNull() ?: 0.0
    val orderGrandTotal = FinanceUtils.round2(max(0.0, computedLabor + fabricsCharge + suppliesCost + parsedAdj))
    val parsedDeposit = FinanceUtils.round2(min(orderGrandTotal, max(0.0, paid.toDoubleOrNull() ?: 0.0)))

    val estMatVal = if (suppliesCost > 0) suppliesCost else max(0.0, estMat.toDoubleOrNull() ?: 0.0)
    val estBaseCost = estMatVal +
        max(0.0, estHours.toDoubleOrNull() ?: 0.0) * max(0.0, estHourlyRate.toDoubleOrNull() ?: 0.0) +
        max(0.0, estExtra.toDoubleOrNull() ?: 0.0)
    val estSuggested = (estBaseCost * (1.0 + max(0.0, estProfitPct.toDoubleOrNull() ?: 0.0) / 100.0)).roundToLong()

    val stats = remember(db.orders) {
        val sales = FinanceUtils.round2(db.orders.sumOf { it.price })
        val collected = FinanceUtils.round2(db.orders.sumOf { it.paid })
        val costs = FinanceUtils.round2(db.orders.sumOf { it.fabricCost + it.threadCost })
        val remaining = FinanceUtils.round2(db.orders.sumOf { max(0.0, it.price - it.paid) })
        val expectedProfit = FinanceUtils.round2(sales - costs)
        Triple(sales, collected, expectedProfit) to remaining
    }

    val filteredOrders = remember(db.orders, filterStage, sortBy, search) {
        val q = search.trim().lowercase()
        val list = db.orders.filter { o ->
            val stageOk = when (filterStage) {
                "متأخرة" -> {
                    val du = daysUntil(o.dueDate)
                    o.stage != "مسلّم" && du != null && du < 0
                }
                "الكل" -> true
                else -> o.stage == filterStage
            }
            val searchOk = q.isBlank() ||
                o.client.lowercase().contains(q) ||
                o.model.lowercase().contains(q) ||
                (o.phone ?: "").contains(q)
            stageOk && searchOk
        }
        if (sortBy == "due") list.sortedBy { it.dueDate ?: "9999-12-31" } else list
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 5-Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple("orders", "الطلبيات", Icons.Filled.ContentCut),
                Triple("calendar", "المواعيد", Icons.Filled.CalendarMonth),
                Triple("clients", "الزبونات", Icons.Filled.People),
                Triple("workshop", "الورشة", Icons.Filled.Inventory2),
                Triple("finance", "المالية", Icons.Filled.Payments),
            ).forEach { (id, label, icon) ->
                FilterChip(
                    selected = sewTab == id,
                    onClick = { sewTab = id },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("sew_tab_$id"),
                )
            }
        }

        when (sewTab) {
            "orders" -> {
                // KPIs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMiniCard("إجمالي الطلبيات", fmt(stats.first.first, privacy), Modifier.weight(1f))
                    KpiMiniCard("المحصّل نقداً", fmt(stats.first.second, privacy), Modifier.weight(1f), valueColor = ext.emerald)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMiniCard("عند الزبونات", fmt(stats.second, privacy), Modifier.weight(1f), valueColor = ext.amber)
                    KpiMiniCard("الربح المتوقع", fmt(stats.first.third, privacy), Modifier.weight(1f), valueColor = ext.violet)
                }

                // Header + Form toggle
                SandooqiCard {
                    SectionHeader(
                        title = "دفتر الطلبيات (${db.orders.count { it.stage != "مسلّم" }} جارية)",
                        icon = Icons.Filled.ContentCut,
                        iconTint = ext.violet,
                        subtitle = "الأقمشة واللوازم والمقاسات وحاسبة التسعير",
                        trailing = {
                            Button(
                                onClick = { showOrderForm = !showOrderForm },
                                colors = ButtonDefaults.buttonColors(containerColor = ext.violet),
                                modifier = Modifier.testTag("toggle_order_form_btn"),
                            ) {
                                Icon(if (showOrderForm) Icons.Filled.Close else Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (showOrderForm) "إغلاق" else "طلبية جديدة")
                            }
                        },
                    )

                    if (showOrderForm) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("1. الزبونة والمواعيد", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.violet)
                            OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("اسم الزبونة *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("order_client_input"))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("نوع اللباس / الموديل *") }, singleLine = true, modifier = Modifier.weight(1f).testTag("order_model_input"))
                                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(0.9f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fittingDate, onValueChange = { fittingDate = it }, label = { Text("موعد التجربة") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("موعد التسليم") }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            PhotoAttachmentButton(
                                hasPhoto = photo.isNotBlank(),
                                labelIdle = "صورة الموديل (اختياري)",
                                labelAttached = "تم إرفاق صورة الموديل ✓",
                                testTag = "order_photo_button",
                                onCompressUri = onCompressUri,
                                onPhotoReady = { photo = it },
                                onClearPhoto = { photo = "" },
                            )

                            HorizontalDivider()
                            Text("2. أقمشة الطلبية (${fabrics.size}) — ${FinanceUtils.fmtQty(totalMeters)} متر", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.violet)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = fabSource == "customer", onClick = { fabSource = "customer" }, label = { Text("قماش الزبونة") })
                                FilterChip(selected = fabSource == "workshop", onClick = { fabSource = "workshop" }, label = { Text("قماش الورشة") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fabName, onValueChange = { fabName = it }, label = { Text("نوع القماش") }, singleLine = true, modifier = Modifier.weight(1.3f))
                                OutlinedTextField(value = fabMeters, onValueChange = { fabMeters = FinanceUtils.normArDigits(it) }, label = { Text("الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
                            }
                            val addFabricLine = {
                                val m = fabMeters.toDoubleOrNull() ?: 0.0
                                if (fabName.isNotBlank() && m > 0) {
                                    val buy = if (fabSource == "workshop") max(0.0, fabBuyRate.toDoubleOrNull() ?: 0.0) else 0.0
                                    val sell = fabRate.toDoubleOrNull()?.let { max(0.0, it) } ?: buy
                                    fabrics = fabrics + OrderFabricLine(
                                        id = "fab_${nextId()}",
                                        name = fabName.trim(),
                                        source = fabSource,
                                        meters = FinanceUtils.round2(m),
                                        rate = sell,
                                        buyRate = buy,
                                        charge = if (fabSource == "workshop") FinanceUtils.round2(m * sell) else 0.0,
                                        cost = if (fabSource == "workshop") FinanceUtils.round2(m * buy) else 0.0,
                                    )
                                    fabName = ""; fabMeters = ""; fabRate = ""; fabBuyRate = ""
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = fabRate,
                                    onValueChange = { fabRate = FinanceUtils.normArDigits(it) },
                                    label = { Text(if (fabSource == "workshop") "بيع/م" else "قيمة/م (اختياري)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                                if (fabSource == "workshop") {
                                    OutlinedTextField(
                                        value = fabBuyRate,
                                        onValueChange = { fabBuyRate = FinanceUtils.normArDigits(it) },
                                        label = { Text("شراء/م") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Button(onClick = addFabricLine) { Text("+ قماش") }
                                }
                            }
                            if (fabSource == "workshop") {
                                OutlinedButton(
                                    onClick = addFabricLine,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("+ إضافة القماش للطلبية") }
                            }
                            fabrics.forEach { f ->
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("${f.name} · ${f.meters}م (${if (f.source == "customer") "زبونة" else "ورشة: ${fmt(f.charge)}"})", style = MaterialTheme.typography.bodySmall)
                                        TextButton(onClick = { fabrics = fabrics.filter { it.id != f.id } }) { Text("حذف", color = ext.rose) }
                                    }
                                }
                            }

                            HorizontalDivider()
                            Text("3. اللوازم ومخزون الورشة (${supplies.size}) — ${fmt(suppliesCost)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.violet)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SUPPLY_OPTIONS.forEach { opt ->
                                    FilterChip(selected = supName == opt, onClick = { supName = opt }, label = { Text(opt) })
                                }
                                FilterChip(selected = supName == "__custom", onClick = { supName = "__custom" }, label = { Text("مادة أخرى") })
                            }
                            if (supName == "__custom") {
                                OutlinedTextField(value = supCustom, onValueChange = { supCustom = it }, label = { Text("اسم المادة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = supCost, onValueChange = { supCost = FinanceUtils.normArDigits(it) }, label = { Text("التكلفة (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = {
                                        val finalSup = if (supName == "__custom") supCustom.trim() else supName
                                        val c = max(0.0, supCost.toDoubleOrNull() ?: 0.0)
                                        if (finalSup.isNotBlank()) {
                                            supplies = supplies + OrderSupplyLine("sup_${nextId()}", finalSup, c)
                                            supCustom = ""; supCost = ""
                                        }
                                    },
                                ) { Text("+ إضافة للاستهلاك") }
                            }
                            val availableStock = db.sewMaterials.filter { it.qty > 0 }
                            if (availableStock.isNotEmpty()) {
                                Text("سحب مباشر من مخزون الورشة:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    availableStock.forEach { mat ->
                                        FilterChip(
                                            selected = stockPickId == mat.id,
                                            onClick = { stockPickId = if (stockPickId == mat.id) null else mat.id },
                                            label = { Text("${mat.name} (${mat.qty} ${mat.unit})") },
                                        )
                                    }
                                }
                                if (stockPickId != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(value = stockPickQty, onValueChange = { stockPickQty = FinanceUtils.normArDigits(it) }, label = { Text("الكمية المسحوبة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = {
                                                val mat = availableStock.find { it.id == stockPickId }
                                                val q = max(0.01, stockPickQty.toDoubleOrNull() ?: 0.0)
                                                if (mat != null && q <= mat.qty) {
                                                    supplies = supplies + OrderSupplyLine("stk_${nextId()}", mat.name, q * mat.cost, mat.id, q, mat.unit)
                                                    stockPickId = null; stockPickQty = "1"
                                                }
                                            },
                                        ) { Text("سحب") }
                                    }
                                }
                            }
                            supplies.forEach { s ->
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("${s.name} ${if (s.qty != null) "(${s.qty} ${s.unit})" else ""} — ${fmt(s.cost)}", style = MaterialTheme.typography.bodySmall)
                                        TextButton(onClick = { supplies = supplies.filter { it.id != s.id } }) { Text("حذف", color = ext.rose) }
                                    }
                                }
                            }

                            // 4. Measurements collapsible
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showMeasSection = !showMeasSection }.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("4. مقاسات الطلبية (اختياري)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.violet)
                                Icon(if (showMeasSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                            }
                            if (showMeasSection) {
                                if (db.measurements.isNotEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        db.measurements.forEach { m ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    ordMeas = OrderMeasurements(m.len, m.sh, m.chest, m.waist, m.hip, m.sleeve, m.note)
                                                },
                                                label = { Text("نسخ: ${m.name}") },
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(value = ordMeas.len.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(len = it) }, label = { Text("الطول") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = ordMeas.sh.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(sh = it) }, label = { Text("الكتف") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = ordMeas.chest.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(chest = it) }, label = { Text("الصدر") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(value = ordMeas.waist.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(waist = it) }, label = { Text("الخصر") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = ordMeas.hip.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(hip = it) }, label = { Text("الأرداف") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = ordMeas.sleeve.orEmpty(), onValueChange = { ordMeas = ordMeas.copy(sleeve = it) }, label = { Text("الكم") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                            }

                            // 5. Smart Pricing Calculator collapsible
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showEstSection = !showEstSection }.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("5. حاسبة التسعير الذكية", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ext.violet)
                                Icon(if (showEstSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                            }
                            if (showEstSection) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(value = estMat, onValueChange = { estMat = FinanceUtils.normArDigits(it) }, label = { Text("المواد") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = estHours, onValueChange = { estHours = FinanceUtils.normArDigits(it) }, label = { Text("ساعات") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = estHourlyRate, onValueChange = { estHourlyRate = FinanceUtils.normArDigits(it) }, label = { Text("أجرة/س") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(value = estExtra, onValueChange = { estExtra = FinanceUtils.normArDigits(it) }, label = { Text("طرز/إضافي") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = estProfitPct, onValueChange = { estProfitPct = FinanceUtils.normArDigits(it) }, label = { Text("الربح %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                Surface(shape = RoundedCornerShape(12.dp), color = ext.violetContainer) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("المقترح: ${fmt(estSuggested.toDouble())} (تكلفة ${fmt(estBaseCost)})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ext.onVioletContainer)
                                        TextButton(
                                            onClick = {
                                                if (totalMeters == 0.0) {
                                                    flatLabor = max(0.0, estSuggested - suppliesCost - fabricsCharge).roundToInt().toString()
                                                } else {
                                                    adjustment = (estSuggested - (computedLabor + fabricsCharge + suppliesCost)).roundToInt().toString()
                                                }
                                            },
                                        ) { Text("اعتماد السعر") }
                                    }
                                }
                            }

                            // 6. Final price & deposit
                            HorizontalDivider()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = meterRate, onValueChange = { meterRate = FinanceUtils.normArDigits(it) }, label = { Text("أجرة المتر") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                if (totalMeters == 0.0) {
                                    OutlinedTextField(value = flatLabor, onValueChange = { flatLabor = FinanceUtils.normArDigits(it) }, label = { Text("أتعاب مقطوعة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                }
                                OutlinedTextField(value = adjustment, onValueChange = { adjustment = FinanceUtils.normArDigits(it) }, label = { Text("تعديل ±") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = paid, onValueChange = { paid = FinanceUtils.normArDigits(it) }, label = { Text("العربون المدفوع") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, singleLine = true, modifier = Modifier.weight(1.2f))
                            }

                            Surface(shape = RoundedCornerShape(14.dp), color = ext.violetContainer) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("الإجمالي النهائي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ext.onVioletContainer)
                                    Text(fmt(orderGrandTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ext.onVioletContainer)
                                }
                            }

                            Button(
                                onClick = {
                                    if (client.isNotBlank() && model.isNotBlank() && orderGrandTotal > 0) {
                                        val deductions = supplies.filter { it.stockId != null && it.qty != null }.map { it.stockId!! to it.qty!! }
                                        onAddOrder(
                                            SewingOrder(
                                                client = client.trim(),
                                                phone = phone.trim().takeIf { it.isNotBlank() },
                                                model = model.trim(),
                                                meters = totalMeters,
                                                meterRate = parsedRate,
                                                flatLabor = if (totalMeters > 0) 0.0 else max(0.0, flatLabor.toDoubleOrNull() ?: 0.0),
                                                adjustment = parsedAdj,
                                                price = orderGrandTotal,
                                                paid = parsedDeposit,
                                                fabricCost = fabricsCost,
                                                threadCost = suppliesCost,
                                                stage = "جديد",
                                                fittingDate = fittingDate.trim().takeIf { it.isNotBlank() },
                                                dueDate = dueDate.trim().takeIf { it.isNotBlank() },
                                                notes = notes.trim().takeIf { it.isNotBlank() },
                                                photo = photo.takeIf { it.isNotBlank() },
                                                fabrics = fabrics.takeIf { it.isNotEmpty() },
                                                supplies = supplies.takeIf { it.isNotEmpty() },
                                                measurements = ordMeas.takeIf {
                                                    listOf(it.len, it.sh, it.chest, it.waist, it.hip, it.sleeve, it.note).any { v -> !v.isNullOrBlank() }
                                                },
                                            ),
                                            deductions,
                                        )
                                        client = ""; phone = ""; model = ""; photo = ""
                                        fittingDate = ""; dueDate = ""; flatLabor = "0"; adjustment = "0"; paid = "0"; notes = ""
                                        fabrics = emptyList(); supplies = emptyList(); ordMeas = OrderMeasurements()
                                        showOrderForm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ext.violet),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_order_button"),
                            ) { Text("حفظ الطلبية في الدفتر", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // Filters + Sort + Search
                SandooqiCard {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("ابحث بالزبونة أو الموديل أو الهاتف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf("الكل", "متأخرة") + STAGES).forEach { st ->
                            FilterChip(selected = filterStage == st, onClick = { filterStage = st }, label = { Text(st) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = sortBy == "new", onClick = { sortBy = "new" }, label = { Text("الأحدث") })
                        FilterChip(selected = sortBy == "due", onClick = { sortBy = "due" }, label = { Text("الأقرب تسليماً") })
                    }
                }

                // Orders list
                if (filteredOrders.isEmpty()) {
                    SandooqiCard { EmptyHintText("لا توجد طلبيات مطابقة للعرض الحالي.") }
                } else {
                    filteredOrders.forEach { order ->
                        val rawRem = order.price - order.paid
                        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
                        val pct = FinanceUtils.formatPercent(order.paid, order.price)
                        val du = daysUntil(order.dueDate)
                        val isOverdue = order.stage != "مسلّم" && du != null && du < 0

                        SandooqiCard(borderColor = if (isOverdue) ext.rose else ext.cardBorder) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${order.client} — ${order.model}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${order.date}${if (!order.phone.isNullOrBlank()) " · ${order.phone}" else ""}${if (!order.dueDate.isNullOrBlank()) " · التسليم: ${order.dueDate}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                BadgeTag(
                                    text = if (isOverdue) "متأخرة" else order.stage,
                                    containerColor = if (isOverdue) ext.roseContainer else if (order.stage == "مسلّم") ext.emeraldContainer else ext.violetContainer,
                                    contentColor = if (isOverdue) ext.onRoseContainer else if (order.stage == "مسلّم") ext.onEmeraldContainer else ext.onVioletContainer,
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            StatusProgressBar(
                                percent = pct,
                                barColor = if (rem <= 0.0) ext.emerald else ext.violet,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("السعر: ${fmt(order.price, privacy)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("المدفوع: ${fmt(order.paid, privacy)}", style = MaterialTheme.typography.labelMedium, color = ext.emerald, modifier = Modifier.weight(1f))
                                Text("المتبقي: ${fmt(rem, privacy)}", style = MaterialTheme.typography.labelMedium, color = if (rem > 0) ext.rose else ext.emerald, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                STAGES.forEach { st ->
                                    FilterChip(selected = order.stage == st, onClick = { onUpdateOrderStage(order.id, st) }, label = { Text(st) })
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (rem > 0) {
                                        Button(onClick = { collectTarget = order }) { Text("تحصيل") }
                                    }
                                    OutlinedButton(onClick = { receiptOrder = order }) {
                                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("وصل")
                                    }
                                    if (!order.photo.isNullOrBlank()) {
                                        TextButton(onClick = { onPreviewPhoto(order.photo, "${order.client} — ${order.model}") }) {
                                            Text("الصورة")
                                        }
                                    }
                                }
                                IconButton(onClick = { deleteTarget = order }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الطلبية", tint = ext.rose)
                                }
                            }
                        }
                    }
                }
            }

            "calendar" -> TailoringCalendarTab(db = db)

            "clients" -> TailoringClientsTab(
                db = db,
                onAddMeasurement = onAddMeasurement,
                onDeleteMeasurement = onDeleteMeasurement,
                onAddSewService = onAddSewService,
                onDeleteSewService = onDeleteSewService,
            )

            "workshop" -> TailoringWorkshopTab(
                db = db,
                onAddSewMaterial = onAddSewMaterial,
                onUseSewMaterial = onUseSewMaterial,
                onDeleteSewMaterial = onDeleteSewMaterial,
                onAddSewProject = onAddSewProject,
                onDeleteSewProject = onDeleteSewProject,
            )

            else -> TailoringFinanceTab(
                db = db,
                onCollectOrderPayment = onCollectOrderPayment,
                onSewCashMovement = onSewCashMovement,
                onTransferSewingToFamily = onTransferSewingToFamily,
                onAddSewDebt = onAddSewDebt,
                onPaySewDebt = onPaySewDebt,
                onDeleteSewDebt = onDeleteSewDebt,
                onNotify = onNotify,
            )
        }
    }

    collectTarget?.let { order ->
        val rawRem = order.price - order.paid
        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
        InputAmountDialog(
            title = "تحصيل دفعة — ${order.client}",
            message = "المتبقي من طلبية ${order.model}: ${fmt(rem)}",
            initialValue = rem.roundToInt().toString(),
            confirmLabel = "تحصيل لحساب الخياطة",
            onDismiss = { collectTarget = null },
            onSubmit = { amt, _ -> onCollectOrderPayment(order, amt) },
        )
    }

    deleteTarget?.let { order ->
        ConfirmActionDialog(
            title = "حذف الطلبية",
            message = "هل تريد حذف طلبية ${order.client} (${order.model})؟",
            confirmLabel = "حذف",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = { onDeleteOrder(order.id) },
        )
    }

    receiptOrder?.let { order ->
        val rawRem = order.price - order.paid
        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
        val shareText = buildString {
            appendLine("وصل طلبية خياطة — صندوقي")
            appendLine("الزبونة: ${order.client}")
            if (!order.phone.isNullOrBlank()) appendLine("الهاتف: ${order.phone}")
            appendLine("الموديل: ${order.model}")
            appendLine("المرحلة: ${order.stage}")
            if (!order.dueDate.isNullOrBlank()) appendLine("موعد التسليم: ${order.dueDate}")
            appendLine("السعر الإجمالي: ${fmt(order.price)}")
            appendLine("المدفوع: ${fmt(order.paid)}")
            appendLine("المتبقي: ${fmt(rem)}")
        }
        AlertDialog(
            onDismissRequest = { receiptOrder = null },
            title = { Text("وصل الطلبية #${order.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(shareText, style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val file = PdfExportHelper.generateOrderReceiptPdf(context, order)
                                PdfExportHelper.sharePdf(context, file, "مشاركة وصل الخياطة PDF")
                                onNotify("تم تجهيز وصل الطلبية بصيغة PDF")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ext.violet),
                            modifier = Modifier.weight(1f).testTag("share_order_pdf_btn"),
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("مشاركة PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = PdfExportHelper.generateOrderReceiptPdf(context, order)
                                PdfExportHelper.openPdf(context, file)
                            },
                            modifier = Modifier.weight(1f).testTag("open_order_pdf_btn"),
                        ) {
                            Text("معاينة / طباعة")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "مشاركة وصل الخياطة"))
                    },
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة نص")
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptOrder = null }) { Text("إغلاق") }
            },
        )
    }
}
