package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.getTxYM
import com.example.model.HomeDebt
import com.example.model.SandooqiDB
import com.example.ui.components.EmptyHintText
import com.example.ui.components.PhotoAttachmentButton
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

private val BILL_PRESETS = listOf(
    "فاتورة الغاز والكهرباء",
    "فاتورة الإنترنت",
    "فاتورة الكراء",
    "فاتورة الماء",
    "custom",
)

@Composable
fun BillsTabSection(
    db: SandooqiDB,
    onAddObligation: (name: String, amount: Double, dueDay: Int, creditor: String?, photo: String?) -> Unit,
    onToggleObligationPaid: (Long) -> Unit,
    onDeleteObligation: (Long) -> Unit,
    onAddBillEntry: (name: String, amount: Double, creditor: String, status: String, due: String?, unpaidMonths: Int, recurring: Boolean, photo: String?) -> Unit,
    onPayHomeDebt: (HomeDebt) -> Unit,
    onDeleteHomeDebt: (Long) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }

    var oblName by remember { mutableStateOf("") }
    var oblAmt by remember { mutableStateOf("") }
    var oblDay by remember { mutableStateOf("15") }
    var oblCreditor by remember { mutableStateOf("") }
    var oblPhoto by remember { mutableStateOf("") }

    var billType by remember { mutableStateOf("فاتورة الغاز والكهرباء") }
    var billCustomName by remember { mutableStateOf("") }
    var billAmt by remember { mutableStateOf("") }
    var billCreditor by remember { mutableStateOf("") }
    var billStatus by remember { mutableStateOf("pending") }
    var billDue by remember { mutableStateOf("") }
    var billUnpaidMonths by remember { mutableStateOf(1) }
    var billRecurring by remember { mutableStateOf(false) }
    var billPhoto by remember { mutableStateOf("") }

    val billDebts = remember(db.homeDebts) {
        db.homeDebts.filter { it.activity == "فواتير" || it.item.contains("فاتورة") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Recurring Monthly Obligations
        SandooqiCard {
            SectionHeader(title = "الالتزامات والفواتير الشهرية الثابتة", icon = Icons.Filled.ReceiptLong, iconTint = ext.emerald)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oblName,
                    onValueChange = { oblName = it },
                    label = { Text("الالتزام (كراء، أنترنت...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = oblAmt,
                    onValueChange = { oblAmt = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oblCreditor,
                    onValueChange = { oblCreditor = it },
                    label = { Text("الجهة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = oblDay,
                    onValueChange = { oblDay = FinanceUtils.normArDigits(it) },
                    label = { Text("يوم الاستحقاق") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                )
            }
            Spacer(Modifier.height(6.dp))
            PhotoAttachmentButton(
                hasPhoto = oblPhoto.isNotBlank(),
                labelIdle = "صورة العقد / الوصل",
                onCompressUri = onCompressUri,
                onPhotoReady = { oblPhoto = it },
                onClearPhoto = { oblPhoto = "" },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val amt = oblAmt.toDoubleOrNull() ?: 0.0
                    if (oblName.isNotBlank() && amt > 0) {
                        onAddObligation(
                            oblName.trim(),
                            amt,
                            oblDay.toIntOrNull() ?: 15,
                            oblCreditor.trim().ifBlank { null },
                            oblPhoto.ifBlank { null },
                        )
                        oblName = ""
                        oblAmt = ""
                        oblCreditor = ""
                        oblPhoto = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("إضافة التزام شهري", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            if (db.obligations.isEmpty()) {
                EmptyHintText("أضف فواتيرك الثابتة لمتابعة دفعها بضغطة واحدة كل شهر.")
            } else {
                db.obligations.forEachIndexed { idx, o ->
                    if (idx > 0) HorizontalDivider()
                    val paidThisMonth = ym in o.paidMonths
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(o.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${fmt(o.amount, privacy)} · يوم ${o.dueDay} من كل شهر",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!o.photo.isNullOrBlank()) {
                                TextButton(onClick = { onPreviewPhoto(o.photo, o.name) }) {
                                    Text("عرض صورة الوصل", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onToggleObligationPaid(o.id) },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (paidThisMonth) ext.emerald else ext.amber,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (paidThisMonth) "مدفوعة" else "تسديد", style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { onDeleteObligation(o.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                            }
                        }
                    }
                }
            }
        }

        // Quick Bill Entry Form
        SandooqiCard {
            SectionHeader(title = "تسجيل فاتورة (كهرباء، غاز، ماء، أنترنت، كراء)", icon = Icons.Filled.ReceiptLong, iconTint = ext.sky)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BILL_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = billType == preset,
                        onClick = { billType = preset },
                        label = { Text(if (preset == "custom") "✏️ فاتورة مخصصة" else preset) },
                    )
                }
            }
            if (billType == "custom") {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = billCustomName,
                    onValueChange = { billCustomName = it },
                    label = { Text("اسم الفاتورة المخصصة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = billAmt,
                    onValueChange = { billAmt = FinanceUtils.normArDigits(it) },
                    label = { Text("مبلغ الشهر الواحد (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = billCreditor,
                    onValueChange = { billCreditor = it },
                    label = { Text("الشخص / الجهة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = billStatus == "pending",
                    onClick = { billStatus = "pending" },
                    label = { Text("دين غير مسدّد") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = billStatus == "paid",
                    onClick = { billStatus = "paid" },
                    label = { Text("مدفوعة الآن") },
                    modifier = Modifier.weight(1f),
                )
            }
            if (billStatus == "pending") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(1, 2, 3, 4, 6).forEach { m ->
                        FilterChip(
                            selected = billUnpaidMonths == m,
                            onClick = { billUnpaidMonths = m },
                            label = { Text("$m شهر") },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = billDue,
                onValueChange = { billDue = it },
                label = { Text("تاريخ الاستحقاق (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = billRecurring, onCheckedChange = { billRecurring = it })
                Text("تكرار شهري تلقائي (التزام شهري ثابت)", style = MaterialTheme.typography.bodySmall)
            }
            PhotoAttachmentButton(
                hasPhoto = billPhoto.isNotBlank(),
                labelIdle = "صورة الفاتورة",
                onCompressUri = onCompressUri,
                onPhotoReady = { billPhoto = it },
                onClearPhoto = { billPhoto = "" },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val amt = billAmt.toDoubleOrNull() ?: 0.0
                    val finalName = if (billType == "custom") billCustomName.trim() else billType
                    if (finalName.isNotBlank() && amt > 0) {
                        onAddBillEntry(
                            finalName,
                            amt,
                            billCreditor.trim().ifBlank { finalName },
                            billStatus,
                            billDue.trim().ifBlank { null },
                            billUnpaidMonths,
                            billRecurring,
                            billPhoto.ifBlank { null },
                        )
                        billAmt = ""
                        billCustomName = ""
                        billCreditor = ""
                        billDue = ""
                        billPhoto = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ext.sky),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                val previewAmt = (billAmt.toDoubleOrNull() ?: 0.0) * max(1, billUnpaidMonths)
                val btnText = if (billStatus == "pending") {
                    if (billUnpaidMonths > 1 && previewAmt > 0) {
                        "تسجيل الفاتورة كدين (${fmt(previewAmt)})"
                    } else {
                        "تسجيل الفاتورة كدين"
                    }
                } else {
                    "تسجيل ودفع الفاتورة"
                }
                Text(btnText, fontWeight = FontWeight.Bold)
            }

            if (billDebts.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("ديون الفواتير المسجلة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                billDebts.forEach { b ->
                    val rem = debtRemaining(b)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${b.creditor} — ${b.item}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("المتبقي: ${fmt(rem, privacy)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            if (rem > 0) {
                                TextButton(onClick = { onPayHomeDebt(b) }) { Text("تسديد") }
                            }
                            IconButton(onClick = { onDeleteHomeDebt(b.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeductionsTabSection(
    db: SandooqiDB,
    onAddSalaryDeduction: (facAmt: Double, facNote: String, srvAmt: Double, srvNote: String, srvMonth: String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddFacPlan: (name: String, monthly: Double, months: Int) -> Unit,
    onPayFacMonth: (Long) -> Unit,
    onDeleteFacPlan: (Long) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }

    var facDedAmt by remember { mutableStateOf("") }
    var facDedNote by remember { mutableStateOf("") }
    var srvDedAmt by remember { mutableStateOf("") }
    var srvDedNote by remember { mutableStateOf("") }
    var srvDedMonth by remember { mutableStateOf(ym) }

    var facName by remember { mutableStateOf("") }
    var facMonthly by remember { mutableStateOf("") }
    var facMonths by remember { mutableStateOf("12") }

    val monthlyDeductions = remember(db.tx, ym) {
        db.tx.filter { getTxYM(it) == ym && (it.cat == "fac" || it.cat == "srv") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Direct Salary Deductions
        SandooqiCard {
            SectionHeader(title = "اقتطاعات الراتب الشهري (فاسيليتي وخدمات)", icon = Icons.Filled.CreditCard, iconTint = ext.rose)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = facDedAmt,
                    onValueChange = { facDedAmt = FinanceUtils.normArDigits(it) },
                    label = { Text("مبلغ فاسيليتي") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = facDedNote,
                    onValueChange = { facDedNote = it },
                    label = { Text("تفاصيل فاسيليتي") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = srvDedAmt,
                    onValueChange = { srvDedAmt = FinanceUtils.normArDigits(it) },
                    label = { Text("مبلغ خدمات/سلفة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = srvDedNote,
                    onValueChange = { srvDedNote = it },
                    label = { Text("نوع الخدمة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = srvDedMonth,
                onValueChange = { srvDedMonth = it },
                label = { Text("شهر الاقتطاع (YYYY-MM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val facA = max(0.0, facDedAmt.toDoubleOrNull() ?: 0.0)
                    val srvA = max(0.0, srvDedAmt.toDoubleOrNull() ?: 0.0)
                    if (facA > 0 || srvA > 0) {
                        onAddSalaryDeduction(facA, facDedNote.trim(), srvA, srvDedNote.trim(), srvDedMonth.ifBlank { ym })
                        facDedAmt = ""
                        facDedNote = ""
                        srvDedAmt = ""
                        srvDedNote = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ext.rose),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("خصم الاقتطاع من الراتب الشهري", fontWeight = FontWeight.Bold)
            }

            if (monthlyDeductions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                monthlyDeductions.forEach { t ->
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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

        // Installment Plans (FacPlans)
        SandooqiCard {
            SectionHeader(title = "متابعة خطط الأقساط (فاسيليتي)")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = facName,
                onValueChange = { facName = it },
                label = { Text("المنتج / الخطة (ثلاجة، غسالة...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = facMonthly,
                    onValueChange = { facMonthly = FinanceUtils.normArDigits(it) },
                    label = { Text("القسط الشهري (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = facMonths,
                    onValueChange = { facMonths = FinanceUtils.normArDigits(it) },
                    label = { Text("عدد الأشهر") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val mAmt = facMonthly.toDoubleOrNull() ?: 0.0
                    val mCnt = facMonths.toIntOrNull() ?: 0
                    if (facName.isNotBlank() && mAmt > 0 && mCnt > 0) {
                        onAddFacPlan(facName.trim(), mAmt, mCnt)
                        facName = ""
                        facMonthly = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_fac_plan_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("إضافة خطة قسط", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            if (db.facPlans.isEmpty()) {
                EmptyHintText("لا توجد خطط أقساط نشطة.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    db.facPlans.forEach { p ->
                        val remMonths = max(0, p.months - p.paidMonths)
                        val pct = ((p.paidMonths.toDouble() / max(1, p.months)) * 100.0).roundToInt().coerceIn(0, 100)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ext.cardBorder),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${fmt(p.monthly, privacy)}/شهر · المدفوع ${p.paidMonths}/${p.months} · المتبقي ${fmt(remMonths * p.monthly, privacy)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (remMonths > 0) {
                                            Button(
                                                onClick = { onPayFacMonth(p.id) },
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text("خصم شهر", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        IconButton(onClick = { onDeleteFacPlan(p.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                                        }
                                    }
                                }
                                StatusProgressBar(percent = pct, barColor = ext.emerald)
                            }
                        }
                    }
                }
            }
        }
    }
}
