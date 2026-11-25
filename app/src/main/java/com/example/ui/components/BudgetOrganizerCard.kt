package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import com.example.data.FinanceUtils.fmt
import com.example.model.BudgetAllocationItem
import com.example.model.SandooqiDB
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BudgetOrganizerCard(
    db: SandooqiDB,
    onUpdateBudgetTotal: (Double?) -> Unit,
    onUpdateBudgetItemAmount: (String, Double) -> Unit,
    onAddBudgetItem: (String, Double) -> Unit,
    onDeleteBudgetItem: (String) -> Unit,
    onRecordBudgetExpense: (BudgetAllocationItem) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val ym = remember(db.lastActiveYM) { currentYM() }
    val report = remember(db, ym) { FinanceUtils.computeCashReportForMonth(db, ym) }
    val autoMonthlyBudget = max(0.0, report.home.opening + report.home.income + report.home.transfer)

    val override = db.budgetOrganizer.totalBudgetOverride
    val effectiveBudget = override ?: autoMonthlyBudget
    val items = db.budgetOrganizer.items
    val totalAllocated = items.sumOf { it.amount }
    val remainingBudget = effectiveBudget - totalAllocated
    val usedPct = if (effectiveBudget > 0) ((totalAllocated / effectiveBudget) * 100.0).roundToInt().coerceIn(0, 100) else 0

    var editingTotal by remember { mutableStateOf(false) }
    var totalDraft by remember { mutableStateOf("") }
    var newLabel by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }

    SandooqiCard {
        SectionHeader(
            title = "تنظيم الميزانية الشهرية",
            icon = Icons.Filled.Calculate,
            iconTint = ext.emerald,
            subtitle = "وزّع ميزانيتك على الكراء والمصروف وأي بنود إضافية، ويتم الخصم من المتبقي تلقائياً.",
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (override != null) {
                OutlinedButton(
                    onClick = {
                        onUpdateBudgetTotal(null)
                        editingTotal = false
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ربط بدخل الشهر", style = MaterialTheme.typography.labelMedium)
                }
            }
            OutlinedButton(
                onClick = {
                    totalDraft = effectiveBudget.roundToInt().toString()
                    editingTotal = !editingTotal
                },
                modifier = Modifier.testTag("toggle_budget_total_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (editingTotal) "إغلاق التعديل" else "تعديل مبلغ الميزانية",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (editingTotal) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ext.emeraldContainer,
                border = BorderStroke(1.dp, ext.emerald.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = totalDraft,
                        onValueChange = { totalDraft = FinanceUtils.normArDigits(it) },
                        label = { Text("إجمالي الميزانية (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onUpdateBudgetTotal(max(0.0, totalDraft.toDoubleOrNull() ?: 0.0))
                            editingTotal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("اعتماد")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KpiMiniCard(
                label = if (override == null) "إجمالي الميزانية (تلقائي)" else "الميزانية (مخصص)",
                value = fmt(effectiveBudget, privacy),
                modifier = Modifier.weight(1f),
            )
            KpiMiniCard(
                label = "مجموع المخصّص",
                value = "− ${fmt(totalAllocated, privacy)}",
                valueColor = ext.amber,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        KpiMiniCard(
            label = if (remainingBudget < 0) "تجاوز الميزانية بمقدار" else "المتبقي من الميزانية",
            value = fmt(abs(remainingBudget), privacy),
            valueColor = if (remainingBudget < 0) ext.rose else ext.emerald,
            containerColor = if (remainingBudget < 0) ext.roseContainer else ext.emeraldContainer,
            borderColor = if (remainingBudget < 0) ext.rose.copy(alpha = 0.35f) else ext.emerald.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "نسبة التوزيع من الميزانية",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$usedPct%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        StatusProgressBar(
            percent = usedPct,
            barColor = when {
                remainingBudget < 0 -> ext.rose
                usedPct >= 85 -> ext.amber
                else -> ext.emerald
            },
        )

        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val sharePct = if (effectiveBudget > 0) ((item.amount / effectiveBudget) * 100.0).roundToInt() else 0
                var amtDraft by remember(item.id, item.amount) {
                    mutableStateOf(if (item.amount == 0.0) "" else item.amount.roundToInt().toString())
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (effectiveBudget > 0 && item.amount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    BadgeTag(
                                        text = "$sharePct%",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (item.amount > 0) {
                                TextButton(onClick = { onRecordBudgetExpense(item) }) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("خصم من الخزينة", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            IconButton(onClick = { onDeleteBudgetItem(item.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "حذف ${item.label}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedTextField(
                            value = amtDraft,
                            onValueChange = { raw ->
                                val clean = FinanceUtils.normArDigits(raw)
                                amtDraft = clean
                                onUpdateBudgetItemAmount(item.id, max(0.0, clean.toDoubleOrNull() ?: 0.0))
                            },
                            label = { Text("المبلغ المخصّص (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                label = { Text("بند جديد (نقل، دراسة...)") },
                singleLine = true,
                modifier = Modifier.weight(1.2f),
            )
            OutlinedTextField(
                value = newAmount,
                onValueChange = { newAmount = FinanceUtils.normArDigits(it) },
                label = { Text("المبلغ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.8f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val amt = newAmount.toDoubleOrNull() ?: 0.0
                if (newLabel.isNotBlank() && amt > 0) {
                    onAddBudgetItem(newLabel.trim(), amt)
                    newLabel = ""
                    newAmount = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_budget_item_button"),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("إضافة واحتساب من الميزانية", fontWeight = FontWeight.Bold)
        }
    }
}
