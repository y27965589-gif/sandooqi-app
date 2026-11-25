package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.localDateISO
import com.example.data.FinanceUtils.shiftYM
import com.example.data.PdfExportHelper
import com.example.model.MeasurementRecord
import com.example.model.SandooqiDB
import com.example.model.SewDebtRecord
import com.example.model.SewMaterial
import com.example.model.SewProjectRecord
import com.example.model.SewServiceRecord
import com.example.model.SewingOrder
import com.example.ui.components.BadgeTag
import com.example.ui.components.EmptyHintText
import com.example.ui.components.InputAmountDialog
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TailoringCalendarTab(db: SandooqiDB) {
    val ext = SandooqiThemeTokens.colors
    var weekOffset by remember { mutableIntStateOf(0) }

    val weekDays = remember(db.orders, weekOffset) {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        val diffToSat = (day % 7)
        cal.add(Calendar.DAY_OF_MONTH, -diffToSat + weekOffset * 7)
        val names = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        List(7) { i ->
            val iso = localDateISO(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val events = db.orders.filter { o ->
                o.stage != "مسلّم" && (o.dueDate == iso || o.fittingDate == iso)
            }
            Triple(iso, names[i], events)
        }
    }

    val dueSoon = remember(db.orders) {
        db.orders.filter { o ->
            if (o.stage == "مسلّم") false
            else {
                val du = daysUntil(o.dueDate)
                du != null && du <= 3
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (dueSoon.isNotEmpty()) {
            SandooqiCard(borderColor = ext.amber, containerColor = ext.amberContainer) {
                SectionHeader("تنبيهات التسليم القريبة (${dueSoon.size})", Icons.Filled.Warning, ext.amber)
                Spacer(Modifier.height(6.dp))
                dueSoon.forEach { o ->
                    val du = daysUntil(o.dueDate) ?: 0
                    Text(
                        "• ${o.client} (${o.model}) — ${if (du < 0) "متأخر بـ ${-du} يوم" else if (du == 0) "اليوم" else "خلال $du أيام"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ext.onAmberContainer,
                    )
                }
            }
        }

        SandooqiCard {
            SectionHeader(
                title = "جدول الأسبوع",
                icon = Icons.Filled.CalendarMonth,
                iconTint = ext.violet,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { weekOffset -= 1 }, modifier = Modifier.weight(1f)) { Text("السابق") }
                TextButton(onClick = { weekOffset = 0 }, modifier = Modifier.weight(1f)) { Text("اليوم") }
                OutlinedButton(onClick = { weekOffset += 1 }, modifier = Modifier.weight(1f)) { Text("التالي") }
            }
            Spacer(Modifier.height(10.dp))
            weekDays.forEach { (iso, label, events) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(iso, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (events.isEmpty()) {
                            Text("لا توجد مواعيد مسجلة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Spacer(Modifier.height(4.dp))
                            events.forEach { ev ->
                                val kind = if (ev.fittingDate == iso && ev.dueDate == iso) "تجربة + تسليم"
                                else if (ev.fittingDate == iso) "موعد تجربة"
                                else "موعد تسليم"
                                Text(
                                    "• ${ev.client} (${ev.model}) — $kind",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ext.violet,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TailoringClientsTab(
    db: SandooqiDB,
    onAddMeasurement: (MeasurementRecord) -> Unit,
    onDeleteMeasurement: (Long) -> Unit,
    onAddSewService: (SewServiceRecord) -> Unit,
    onDeleteSewService: (Long) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode

    var measName by remember { mutableStateOf("") }
    var measPhone by remember { mutableStateOf("") }
    var measLen by remember { mutableStateOf("") }
    var measSh by remember { mutableStateOf("") }
    var measChest by remember { mutableStateOf("") }
    var measWaist by remember { mutableStateOf("") }
    var measHip by remember { mutableStateOf("") }
    var measSleeve by remember { mutableStateOf("") }
    var measNote by remember { mutableStateOf("") }
    var measSearch by remember { mutableStateOf("") }

    var svcClient by remember { mutableStateOf("") }
    var svcItem by remember { mutableStateOf("") }
    var svcMeters by remember { mutableStateOf("") }
    var svcRate by remember(db.meterPrice) { mutableStateOf(db.meterPrice.roundToInt().toString()) }
    var svcThread by remember { mutableStateOf("") }
    var svcSupplies by remember { mutableStateOf("") }
    var svcPaid by remember { mutableStateOf("") }

    val svcPrice = max(0.0, svcMeters.toDoubleOrNull() ?: 0.0) * max(0.0, svcRate.toDoubleOrNull() ?: 0.0)
    val svcProfit = svcPrice - (max(0.0, svcThread.toDoubleOrNull() ?: 0.0) + max(0.0, svcSupplies.toDoubleOrNull() ?: 0.0))

    val filteredMeas = remember(db.measurements, measSearch) {
        val q = measSearch.trim().lowercase()
        if (q.isBlank()) db.measurements
        else db.measurements.filter { it.name.lowercase().contains(q) || (it.phone ?: "").contains(q) }
    }

    val clientsDirectory = remember(db.orders, db.measurements) {
        val map = linkedMapOf<String, Triple<Int, Double, Int>>()
        db.orders.forEach { o ->
            val k = o.client.trim()
            val cur = map[k] ?: Triple(0, 0.0, 0)
            map[k] = Triple(cur.first + 1, cur.second + o.price, cur.third)
        }
        db.measurements.forEach { m ->
            val k = m.name.trim()
            val cur = map[k] ?: Triple(0, 0.0, 0)
            map[k] = Triple(cur.first, cur.second, cur.third + 1)
        }
        map.entries.toList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Measurements Book
        SandooqiCard {
            SectionHeader("دفتر مقاسات الزبونات", Icons.Filled.Straighten, ext.violet)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = measName, onValueChange = { measName = it }, label = { Text("اسم الزبونة *") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = measPhone, onValueChange = { measPhone = it }, label = { Text("الهاتف") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(0.9f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = measLen, onValueChange = { measLen = it }, label = { Text("الطول") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = measSh, onValueChange = { measSh = it }, label = { Text("الكتف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = measChest, onValueChange = { measChest = it }, label = { Text("الصدر") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = measWaist, onValueChange = { measWaist = it }, label = { Text("الخصر") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = measHip, onValueChange = { measHip = it }, label = { Text("الأرداف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = measSleeve, onValueChange = { measSleeve = it }, label = { Text("الكم") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = measNote, onValueChange = { measNote = it }, label = { Text("ملاحظات") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (measName.isNotBlank()) {
                            onAddMeasurement(
                                MeasurementRecord(
                                    name = measName.trim(),
                                    phone = measPhone.trim().takeIf { it.isNotBlank() },
                                    len = measLen.trim(), sh = measSh.trim(), chest = measChest.trim(),
                                    waist = measWaist.trim(), hip = measHip.trim(), sleeve = measSleeve.trim(),
                                    note = measNote.trim(),
                                )
                            )
                            measName = ""; measPhone = ""; measLen = ""; measSh = ""; measChest = ""
                            measWaist = ""; measHip = ""; measSleeve = ""; measNote = ""
                        }
                    },
                    modifier = Modifier.height(48.dp).testTag("add_measurement_button"),
                ) { Text("حفظ") }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = measSearch, onValueChange = { measSearch = it }, label = { Text("بحث في المقاسات...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            if (filteredMeas.isEmpty()) {
                EmptyHintText("لا توجد مقاسات مسجلة.")
            } else {
                filteredMeas.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${m.name}${if (!m.phone.isNullOrBlank()) " (${m.phone})" else ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "طول:${m.len.ifBlank { "-" }} · كتف:${m.sh.ifBlank { "-" }} · صدر:${m.chest.ifBlank { "-" }} · خصر:${m.waist.ifBlank { "-" }} · أرداف:${m.hip.ifBlank { "-" }} · كم:${m.sleeve.ifBlank { "-" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteMeasurement(m.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف المقاس", tint = ext.rose)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Quick Sewing Services
        SandooqiCard {
            SectionHeader("خدمات الخياطة السريعة", Icons.Filled.People, ext.emerald)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = svcClient, onValueChange = { svcClient = it }, label = { Text("اسم الزبونة *") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcItem, onValueChange = { svcItem = it }, label = { Text("الخدمة (تصليح، كفافة...)") }, singleLine = true, modifier = Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = svcMeters, onValueChange = { svcMeters = FinanceUtils.normArDigits(it) }, label = { Text("الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcRate, onValueChange = { svcRate = FinanceUtils.normArDigits(it) }, label = { Text("سعر/م") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcPaid, onValueChange = { svcPaid = FinanceUtils.normArDigits(it) }, label = { Text("المدفوع") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = svcThread, onValueChange = { svcThread = FinanceUtils.normArDigits(it) }, label = { Text("تكلفة خيط") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcSupplies, onValueChange = { svcSupplies = FinanceUtils.normArDigits(it) }, label = { Text("لوازم أخرى") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("الإجمالي: ${fmt(svcPrice)} · الربح: ${fmt(svcProfit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ext.emerald)
                Button(
                    onClick = {
                        if (svcClient.isNotBlank() && svcItem.isNotBlank() && svcPrice > 0) {
                            onAddSewService(
                                SewServiceRecord(
                                    client = svcClient.trim(),
                                    item = svcItem.trim(),
                                    meters = max(0.0, svcMeters.toDoubleOrNull() ?: 0.0),
                                    rate = max(0.0, svcRate.toDoubleOrNull() ?: 0.0),
                                    price = svcPrice,
                                    thread = max(0.0, svcThread.toDoubleOrNull() ?: 0.0),
                                    supplies = max(0.0, svcSupplies.toDoubleOrNull() ?: 0.0),
                                    paid = max(0.0, svcPaid.toDoubleOrNull() ?: svcPrice),
                                )
                            )
                            svcClient = ""; svcItem = ""; svcMeters = ""; svcThread = ""; svcSupplies = ""; svcPaid = ""
                        }
                    },
                ) { Text("تسجيل الخدمة") }
            }
            if (db.sewServices.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                db.sewServices.take(15).forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${s.client} — ${s.item}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${s.date} · مدفوع: ${fmt(s.paid, privacy)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteSewService(s.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الخدمة", tint = ext.rose)
                        }
                    }
                }
            }
        }

        // Unified directory
        SandooqiCard {
            SectionHeader("دليل الزبونات الموحّد (${clientsDirectory.size})", Icons.Filled.People, ext.sky)
            Spacer(Modifier.height(6.dp))
            if (clientsDirectory.isEmpty()) {
                EmptyHintText("سيتم تجميع بيانات الزبونات تلقائياً من الطلبيات والمقاسات.")
            } else {
                clientsDirectory.forEach { (name, info) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${info.first} طلبية · ${info.third} مقاس · ${fmt(info.second, privacy)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun TailoringWorkshopTab(
    db: SandooqiDB,
    onAddSewMaterial: (name: String, unit: String, qty: Double, cost: Double, minQty: Double) -> Unit,
    onUseSewMaterial: (SewMaterial, Double) -> Unit,
    onDeleteSewMaterial: (Long) -> Unit,
    onAddSewProject: (SewProjectRecord) -> Unit,
    onDeleteSewProject: (Long) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode

    var matName by remember { mutableStateOf("") }
    var matUnit by remember { mutableStateOf("قطعة") }
    var matQty by remember { mutableStateOf("") }
    var matCost by remember { mutableStateOf("") }
    var matMin by remember { mutableStateOf("2") }
    var useMatTarget by remember { mutableStateOf<SewMaterial?>(null) }

    var prjName by remember { mutableStateOf("") }
    var prjMeters by remember { mutableStateOf("") }
    var prjFabRate by remember { mutableStateOf("") }
    var prjSupplies by remember { mutableStateOf("") }
    var prjOther by remember { mutableStateOf("") }
    var prjSale by remember { mutableStateOf("") }
    var prjPaid by remember { mutableStateOf("") }
    var prjStatus by remember { mutableStateOf("work") }

    val prjFabricCost = max(0.0, prjMeters.toDoubleOrNull() ?: 0.0) * max(0.0, prjFabRate.toDoubleOrNull() ?: 0.0)
    val prjTotalCost = prjFabricCost + max(0.0, prjSupplies.toDoubleOrNull() ?: 0.0) + max(0.0, prjOther.toDoubleOrNull() ?: 0.0)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Materials stock
        SandooqiCard {
            SectionHeader("مخزون مواد الورشة", Icons.Filled.Inventory2, ext.emerald)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = matName, onValueChange = { matName = it }, label = { Text("اسم المادة (خيط، أزرار...)") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = matUnit, onValueChange = { matUnit = it }, label = { Text("الوحدة") }, singleLine = true, modifier = Modifier.weight(0.7f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = matQty, onValueChange = { matQty = FinanceUtils.normArDigits(it) }, label = { Text("الكمية") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = matCost, onValueChange = { matCost = FinanceUtils.normArDigits(it) }, label = { Text("سعر الوحدة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = matMin, onValueChange = { matMin = FinanceUtils.normArDigits(it) }, label = { Text("حد التنبيه") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.9f))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val q = matQty.toDoubleOrNull() ?: 0.0
                    val c = max(0.0, matCost.toDoubleOrNull() ?: 0.0)
                    val mn = max(0.0, matMin.toDoubleOrNull() ?: 2.0)
                    if (matName.isNotBlank() && q > 0) {
                        onAddSewMaterial(matName.trim(), matUnit.trim().ifBlank { "قطعة" }, q, c, mn)
                        matName = ""; matQty = ""; matCost = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_material_button"),
            ) { Text("+ إضافة للمخزون") }

            Spacer(Modifier.height(10.dp))
            if (db.sewMaterials.isEmpty()) {
                EmptyHintText("مخزون الورشة فارغ حالياً.")
            } else {
                db.sewMaterials.forEach { m ->
                    val low = m.qty <= m.min
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(m.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (low) BadgeTag("قارب على النفاد", ext.roseContainer, ext.onRoseContainer)
                            }
                            Text("المتوفر: ${m.qty} ${m.unit} · ${fmt(m.cost, privacy)} للوحدة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            if (m.qty > 0) {
                                TextButton(onClick = { useMatTarget = m }) { Text("سحب") }
                            }
                            IconButton(onClick = { onDeleteSewMaterial(m.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف المادة", tint = ext.rose)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Own workshop projects
        SandooqiCard {
            SectionHeader("مشاريع الورشة الخاصة (تفصيل جاهز للبيع)", Icons.Filled.Inventory2, ext.violet)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = prjName, onValueChange = { prjName = it }, label = { Text("اسم المشروع / القطعة *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjMeters, onValueChange = { prjMeters = FinanceUtils.normArDigits(it) }, label = { Text("أمتار القماش") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjFabRate, onValueChange = { prjFabRate = FinanceUtils.normArDigits(it) }, label = { Text("سعر متر القماش") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjSupplies, onValueChange = { prjSupplies = FinanceUtils.normArDigits(it) }, label = { Text("لوازم") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjOther, onValueChange = { prjOther = FinanceUtils.normArDigits(it) }, label = { Text("مصاريف أخرى") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjSale, onValueChange = { prjSale = FinanceUtils.normArDigits(it) }, label = { Text("سعر البيع المتوقع") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjPaid, onValueChange = { prjPaid = FinanceUtils.normArDigits(it) }, label = { Text("المحصل") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("work" to "قيد العمل", "ready" to "جاهز للبيع", "sold" to "تم البيع").forEach { (k, lbl) ->
                    FilterChip(selected = prjStatus == k, onClick = { prjStatus = k }, label = { Text(lbl) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("التكلفة: ${fmt(prjTotalCost)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        if (prjName.isNotBlank()) {
                            onAddSewProject(
                                SewProjectRecord(
                                    name = prjName.trim(),
                                    meters = max(0.0, prjMeters.toDoubleOrNull() ?: 0.0),
                                    fabricRate = max(0.0, prjFabRate.toDoubleOrNull() ?: 0.0),
                                    fabric = prjFabricCost,
                                    supplies = max(0.0, prjSupplies.toDoubleOrNull() ?: 0.0),
                                    other = max(0.0, prjOther.toDoubleOrNull() ?: 0.0),
                                    sale = max(0.0, prjSale.toDoubleOrNull() ?: 0.0),
                                    paid = max(0.0, prjPaid.toDoubleOrNull() ?: 0.0),
                                    status = prjStatus,
                                )
                            )
                            prjName = ""; prjMeters = ""; prjFabRate = ""; prjSupplies = ""; prjOther = ""; prjSale = ""; prjPaid = ""
                        }
                    },
                ) { Text("حفظ المشروع") }
            }

            if (db.sewProjects.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                db.sewProjects.forEach { p ->
                    val cost = p.fabric + p.supplies + p.other
                    val statusLbl = when (p.status) { "ready" -> "جاهز"; "sold" -> "مباع"; else -> "قيد العمل" }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.name} ($statusLbl)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("التكلفة: ${fmt(cost, privacy)} · البيع: ${fmt(p.sale, privacy)} · محصل: ${fmt(p.paid, privacy)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteSewProject(p.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف المشروع", tint = ext.rose)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    useMatTarget?.let { mat ->
        InputAmountDialog(
            title = "سحب من المخزون — ${mat.name}",
            message = "المتوفر حالياً: ${mat.qty} ${mat.unit}",
            initialValue = "1",
            placeholder = "الكمية (${mat.unit})",
            confirmLabel = "تسجيل السحب",
            onDismiss = { useMatTarget = null },
            onSubmit = { qty, _ -> onUseSewMaterial(mat, qty) },
        )
    }
}

@Composable
fun TailoringFinanceTab(
    db: SandooqiDB,
    onCollectOrderPayment: (SewingOrder, Double) -> Unit,
    onSewCashMovement: (type: String, name: String, amount: Double) -> Unit,
    onTransferSewingToFamily: (Double) -> Unit,
    onAddSewDebt: (type: String, name: String, amount: Double, due: String?) -> Unit,
    onPaySewDebt: (SewDebtRecord, Double, Boolean) -> Unit,
    onDeleteSewDebt: (Long) -> Unit,
    onNotify: (String) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current

    var repMonth by remember { mutableStateOf(currentYM()) }
    var meterInCnt by remember { mutableStateOf("1") }
    var meterInPrice by remember(db.meterPrice) { mutableStateOf(db.meterPrice.roundToInt().toString()) }
    var meterInNote by remember { mutableStateOf("") }
    var threadCnt by remember { mutableStateOf("1") }
    var threadUnitCost by remember(db.threadPrice) { mutableStateOf(db.threadPrice.roundToInt().toString()) }
    var manualName by remember { mutableStateOf("") }
    var manualAmt by remember { mutableStateOf("") }
    var trHomeAmt by remember { mutableStateOf("") }

    var debtType by remember { mutableStateOf("in") }
    var debtName by remember { mutableStateOf("") }
    var debtAmt by remember { mutableStateOf("") }
    var debtDue by remember { mutableStateOf("") }

    var collectTarget by remember { mutableStateOf<SewingOrder?>(null) }
    var payDebtTarget by remember { mutableStateOf<SewDebtRecord?>(null) }

    val unpaidOrders = remember(db.orders) {
        db.orders.filter { o ->
            val raw = o.price - o.paid
            raw >= 0.005
        }
    }
    val sewingReport = remember(db.orders, db.tx, repMonth) {
        val rows = db.orders.filter { it.ym == repMonth }
        val sales = FinanceUtils.round2(rows.sumOf { it.price })
        val orderCosts = FinanceUtils.round2(rows.sumOf { it.fabricCost + it.threadCost })
        val orderRemaining = FinanceUtils.round2(rows.sumOf { max(0.0, it.price - it.paid) })
        val cashBox = FinanceUtils.computeCashReportForMonth(db, repMonth).sewing
        val cashIn = cashBox.income
        val cashOut = cashBox.out
        val cashNet = FinanceUtils.round2(cashIn - cashOut)
        val expectedOrderProfit = FinanceUtils.round2(sales - orderCosts)
        object {
            val ordersCount = rows.size
            val ordersSales = sales
            val ordersRemaining = orderRemaining
            val cashInVal = cashIn
            val cashOutVal = cashOut
            val cashNetVal = cashNet
            val ordersMatCosts = orderCosts
            val expectedProfit = expectedOrderProfit
        }
    }
    val ordersCount = sewingReport.ordersCount
    val ordersSales = sewingReport.ordersSales
    val ordersRemaining = sewingReport.ordersRemaining
    val cashIn = sewingReport.cashInVal
    val cashOut = sewingReport.cashOutVal
    val cashNet = sewingReport.cashNetVal
    val ordersMatCosts = sewingReport.ordersMatCosts
    val expectedOrderProfit = sewingReport.expectedProfit

    val meterPreview = FinanceUtils.round2(max(0.0, meterInCnt.toDoubleOrNull() ?: 0.0) * max(0.0, meterInPrice.toDoubleOrNull() ?: 0.0))
    val threadPreview = FinanceUtils.round2(max(0.0, threadCnt.toDoubleOrNull() ?: 0.0) * max(0.0, threadUnitCost.toDoubleOrNull() ?: 0.0))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Monthly report
        SandooqiCard {
            SectionHeader(
                title = "التقرير الشهري للورشة — ${formatYM(repMonth)}",
                icon = Icons.Filled.AccountBalanceWallet,
                iconTint = ext.violet,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { repMonth = shiftYM(repMonth, -1) }, modifier = Modifier.weight(1f)) { Text("الشهر السابق") }
                OutlinedButton(onClick = { repMonth = shiftYM(repMonth, 1) }, modifier = Modifier.weight(1f)) { Text("الشهر التالي") }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("الطلبيات", "$ordersCount", Modifier.weight(1f))
                KpiMiniCard("مبيعات الطلبيات", fmt(ordersSales, privacy), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard("المحصّل بالصندوق", fmt(cashIn, privacy), Modifier.weight(1f), valueColor = ext.emerald)
                KpiMiniCard("مصاريف الصندوق", fmt(cashOut, privacy), Modifier.weight(1f), valueColor = ext.rose)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiMiniCard(
                    "صافي الصندوق الفعلي",
                    fmt(cashNet, privacy),
                    Modifier.weight(1f),
                    valueColor = if (cashNet >= 0) ext.emerald else ext.rose,
                )
                KpiMiniCard("متبقي عند الزبونات", fmt(ordersRemaining, privacy), Modifier.weight(1f), valueColor = ext.amber)
            }
            if (ordersMatCosts > 0 || ordersRemaining > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "تكاليف مواد الطلبيات: ${fmt(ordersMatCosts, privacy)} · الربح التقديري عند التحصيل: ${fmt(expectedOrderProfit, privacy)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val txt = "ملخص ورشة الخياطة (${formatYM(repMonth)}): $ordersCount طلبية | مبيعات ${fmt(ordersSales)} | محصل فعلي ${fmt(cashIn)} | مصاريف ${fmt(cashOut)} | صافي فعلي ${fmt(cashNet)} | متبقي عند الزبونات ${fmt(ordersRemaining)}"
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, txt)
                        }, "مشاركة تقرير الخياطة"))
                        onNotify("تم تجهيز تقرير الورشة للمشاركة")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مشاركة نص")
                }
                Button(
                    onClick = {
                        val monthOrders = db.orders.filter { it.ym == repMonth }
                        val file = PdfExportHelper.generateMonthlyWorkshopReportPdf(
                            context = context,
                            ym = repMonth,
                            orders = monthOrders,
                            cashIn = cashIn,
                            cashOut = cashOut,
                            cashNet = cashNet,
                        )
                        PdfExportHelper.sharePdf(context, file, "مشاركة تقرير الورشة PDF")
                        onNotify("تم تصدير تقرير الورشة بصيغة PDF")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.violet),
                    modifier = Modifier.weight(1f).testTag("export_workshop_pdf_btn"),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تصدير PDF")
                }
            }
        }

        // Unpaid Orders
        SandooqiCard {
            SectionHeader("مستحقات الطلبيات عند الزبونات (${unpaidOrders.size})", Icons.Filled.AccountBalanceWallet, ext.amber)
            Spacer(Modifier.height(8.dp))
            if (unpaidOrders.isEmpty()) {
                EmptyHintText("جميع الطلبيات خالصة الدفع.")
            } else {
                unpaidOrders.forEach { o ->
                    val rawRem = o.price - o.paid
                    val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${o.client} — ${o.model}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("المتبقي: ${fmt(rem, privacy)}", style = MaterialTheme.typography.bodySmall, color = ext.rose)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { collectTarget = o }) { Text("تحصيل") }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Quick Sewing Cashbox actions
        SandooqiCard {
            SectionHeader("عمليات صندوق الخياطة السريعة", Icons.Filled.AccountBalanceWallet, ext.emerald)
            Spacer(Modifier.height(8.dp))
            Text("تسجيل مدخول بالمتر", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ext.emerald)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = meterInCnt, onValueChange = { meterInCnt = FinanceUtils.normArDigits(it) }, label = { Text("عدد الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = meterInPrice, onValueChange = { meterInPrice = FinanceUtils.normArDigits(it) }, label = { Text("سعر/م") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = meterInNote, onValueChange = { meterInNote = it }, label = { Text("ملاحظة / اسم الزبونة") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (meterPreview > 0) {
                            val qtyVal = meterInCnt.toDoubleOrNull() ?: 0.0
                            onSewCashMovement("in", "مدخول خياطة (${FinanceUtils.fmtQty(qtyVal)}م)${if (meterInNote.isNotBlank()) " — ${meterInNote.trim()}" else ""}", meterPreview)
                            meterInNote = ""
                        }
                    },
                ) { Text("+ دخل (${fmt(meterPreview)})") }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = threadCnt, onValueChange = { threadCnt = FinanceUtils.normArDigits(it) }, label = { Text("بكرة خيط") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = threadUnitCost, onValueChange = { threadUnitCost = FinanceUtils.normArDigits(it) }, label = { Text("السعر") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (threadPreview > 0) {
                            val qtyVal = threadCnt.toDoubleOrNull() ?: 0.0
                            onSewCashMovement("out", "شراء خيط (${FinanceUtils.fmtQty(qtyVal)})", threadPreview)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.rose),
                ) { Text("- خيط (${fmt(threadPreview)})") }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("مصروف ورشة") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = manualAmt, onValueChange = { manualAmt = FinanceUtils.normArDigits(it) }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.9f))
                Button(
                    onClick = {
                        val a = FinanceUtils.round2(manualAmt.toDoubleOrNull() ?: 0.0)
                        if (manualName.isNotBlank() && a > 0) {
                            onSewCashMovement("out", manualName.trim(), a)
                            manualName = ""; manualAmt = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.rose),
                ) { Text("صرف") }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = trHomeAmt, onValueChange = { trHomeAmt = FinanceUtils.normArDigits(it) }, label = { Text("تحويل لصندوق البيت") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val a = FinanceUtils.round2(trHomeAmt.toDoubleOrNull() ?: 0.0)
                        if (a > 0) {
                            onTransferSewingToFamily(a)
                            trHomeAmt = ""
                        }
                    },
                ) { Text("تحويل") }
            }
        }

        // Workshop Debts
        SandooqiCard {
            SectionHeader("ديون ومستحقات الورشة (${db.debts.size})", Icons.Filled.AccountBalanceWallet, ext.rose)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = debtType == "in", onClick = { debtType = "in" }, label = { Text("لي (عند زبونة)") })
                FilterChip(selected = debtType == "out", onClick = { debtType = "out" }, label = { Text("عليّ (لورشة/محل)") })
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = debtName, onValueChange = { debtName = it }, label = { Text("الاسم *") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = debtAmt, onValueChange = { debtAmt = FinanceUtils.normArDigits(it) }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.9f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = debtDue, onValueChange = { debtDue = it }, label = { Text("تاريخ الاستحقاق (اختياري)") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val a = debtAmt.toDoubleOrNull() ?: 0.0
                        if (debtName.isNotBlank() && a > 0) {
                            onAddSewDebt(debtType, debtName.trim(), a, debtDue.trim().takeIf { it.isNotBlank() })
                            debtName = ""; debtAmt = ""; debtDue = ""
                        }
                    },
                ) { Text("حفظ") }
            }

            if (db.debts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                db.debts.forEach { d ->
                    val rem = max(0.0, d.amount - d.paid)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${d.name} (${if (d.type == "in") "مستحق لي" else "دين عليّ"})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("المتبقي: ${fmt(rem, privacy)} من ${fmt(d.amount, privacy)}", style = MaterialTheme.typography.bodySmall, color = if (d.type == "in") ext.emerald else ext.rose)
                        }
                        Row {
                            if (rem > 0) TextButton(onClick = { payDebtTarget = d }) { Text("تسديد") }
                            IconButton(onClick = { onDeleteSewDebt(d.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف دين الورشة", tint = ext.rose)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    collectTarget?.let { o ->
        val rawRem = o.price - o.paid
        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
        InputAmountDialog(
            title = "تحصيل طلبية — ${o.client}",
            message = "المتبقي: ${fmt(rem)}",
            initialValue = rem.roundToInt().toString(),
            onDismiss = { collectTarget = null },
            onSubmit = { amt, _ -> onCollectOrderPayment(o, amt) },
        )
    }

    payDebtTarget?.let { d ->
        val rawRem = d.amount - d.paid
        val rem = if (rawRem < 0.005) 0.0 else FinanceUtils.round2(rawRem)
        InputAmountDialog(
            title = if (d.type == "in") "تحصيل مستحق — ${d.name}" else "تسديد دين — ${d.name}",
            message = "المتبقي: ${fmt(rem)}",
            initialValue = rem.roundToInt().toString(),
            checkboxLabel = "تسجيل الحركة في صندوق الخياطة",
            checkboxDefault = true,
            onDismiss = { payDebtTarget = null },
            onSubmit = { amt, apply -> onPaySewDebt(d, amt, apply) },
        )
    }
}
