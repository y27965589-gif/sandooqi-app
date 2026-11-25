package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.transactionAccount
import com.example.model.SandooqiDB
import com.example.ui.components.EmptyHintText
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.roundToInt

@Composable
fun TreasuryScreen(
    db: SandooqiDB,
    onSaveSalaries: (Double, Double) -> Unit,
    onAddMovement: (accountId: String, type: String, name: String, amount: Double) -> Unit,
    onTransfer: (from: String, to: String, amount: Double, note: String) -> Unit,
    onOpenMonthClose: () -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }
    val report = remember(db, ym) { FinanceUtils.computeCashReportForMonth(db, ym) }

    var s1Input by remember(report.salary.s1) { mutableStateOf(report.salary.s1.roundToInt().toString()) }
    var s2Input by remember(report.salary.s2) { mutableStateOf(report.salary.s2.roundToInt().toString()) }

    var movAccount by remember { mutableStateOf("family") }
    var movType by remember { mutableStateOf("in") }
    var movName by remember { mutableStateOf("") }
    var movAmount by remember { mutableStateOf("") }

    var fromAcc by remember { mutableStateOf("sewing") }
    var toAcc by remember { mutableStateOf("family") }
    var trAmount by remember { mutableStateOf("") }
    var trNote by remember { mutableStateOf("") }

    var historyFilter by remember { mutableStateOf("all") }
    val recentMovements = remember(db.tx, historyFilter) {
        db.tx.filter { historyFilter == "all" || transactionAccount(it) == historyFilter }.take(25)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Three Accounts Overview
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMiniCard(
                label = "الحساب العائلي",
                value = fmt(report.home.balance, privacy),
                valueColor = ext.emerald,
                modifier = Modifier.weight(1f),
            )
            KpiMiniCard(
                label = "الحساب الشخصي",
                value = fmt(report.personal.balance, privacy),
                valueColor = ext.sky,
                modifier = Modifier.weight(1f),
            )
        }
        KpiMiniCard(
            label = "حساب الخياطة (الورشة) · دخل ${fmt(report.sewing.income, privacy)} · خروج ${fmt(report.sewing.out, privacy)}",
            value = fmt(report.sewing.balance, privacy),
            valueColor = ext.violet,
            modifier = Modifier.fillMaxWidth(),
        )

        // Salaries & Month Close
        SandooqiCard {
            SectionHeader(
                title = "رواتب ${formatYM(ym)} وإغلاق الشهر",
                subtitle = "الرصيد المرحّل للشهر الحالي: ${fmt(report.home.opening, privacy)}",
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s1Input,
                    onValueChange = { s1Input = FinanceUtils.normArDigits(it) },
                    label = { Text("الراتب الأول (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = s2Input,
                    onValueChange = { s2Input = FinanceUtils.normArDigits(it) },
                    label = { Text("الراتب الثاني (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSaveSalaries(s1Input.toDoubleOrNull() ?: 0.0, s2Input.toDoubleOrNull() ?: 0.0)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_salaries_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("حفظ الرواتب", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenMonthClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إغلاق الشهر", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Direct Income / Withdrawal
        SandooqiCard {
            SectionHeader(title = "تسجيل دخل إضافي أو سحب مباشر", icon = Icons.Filled.AddCircleOutline)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (id, lbl) ->
                    FilterChip(selected = movAccount == id, onClick = { movAccount = id }, label = { Text(lbl) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = movType == "in", onClick = { movType = "in" }, label = { Text("إيداع / دخل (+)") })
                FilterChip(selected = movType == "out", onClick = { movType = "out" }, label = { Text("سحب / مصروف (−)") })
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = movName,
                    onValueChange = { movName = it },
                    label = { Text("البيان (منحة، أرباح...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = movAmount,
                    onValueChange = { movAmount = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val amt = movAmount.toDoubleOrNull() ?: 0.0
                    if (movName.isNotBlank() && amt > 0) {
                        onAddMovement(movAccount, movType, movName.trim(), amt)
                        movName = ""
                        movAmount = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("treasury_movement_submit"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("تنفيذ الحركة", fontWeight = FontWeight.Bold)
            }
        }

        // Internal Transfer
        SandooqiCard {
            SectionHeader(title = "تحويل بين الحسابات", icon = Icons.Filled.CompareArrows, iconTint = ext.sky)
            Spacer(Modifier.height(8.dp))
            Text("من:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (id, lbl) ->
                    FilterChip(selected = fromAcc == id, onClick = { fromAcc = id }, label = { Text(lbl) })
                }
            }
            Text("إلى:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (id, lbl) ->
                    FilterChip(selected = toAcc == id, onClick = { toAcc = id }, label = { Text(lbl) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trAmount,
                    onValueChange = { trAmount = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                )
                OutlinedTextField(
                    value = trNote,
                    onValueChange = { trNote = it },
                    label = { Text("ملاحظة التحويل") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val amt = trAmount.toDoubleOrNull() ?: 0.0
                    if (fromAcc != toAcc && amt > 0) {
                        onTransfer(fromAcc, toAcc, amt, trNote.trim())
                        trAmount = ""
                        trNote = ""
                    }
                },
                enabled = fromAcc != toAcc,
                colors = ButtonDefaults.buttonColors(containerColor = ext.sky),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("treasury_transfer_submit"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("تحويل المبلغ", fontWeight = FontWeight.Bold)
            }
        }

        // Movements History
        SandooqiCard {
            SectionHeader(title = "سجل حركات الخزينة")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "الكل", "family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (k, lbl) ->
                    FilterChip(selected = historyFilter == k, onClick = { historyFilter = k }, label = { Text(lbl) })
                }
            }
            Spacer(Modifier.height(6.dp))
            if (recentMovements.isEmpty()) {
                EmptyHintText("لا توجد حركات مسجلة بعد.")
            } else {
                recentMovements.forEachIndexed { idx, t ->
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
                            Text(
                                text = "$accLabel · ${t.date}${if (!t.person.isNullOrBlank()) " · ${t.person}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${if (t.type == "in") "+" else "−"}${fmt(t.amount, privacy)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (t.type == "in") ext.emerald else ext.rose,
                        )
                    }
                }
            }
        }
    }
}
