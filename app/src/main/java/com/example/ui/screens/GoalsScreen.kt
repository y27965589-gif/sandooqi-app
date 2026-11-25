package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.fmt
import com.example.model.BudgetAllocationItem
import com.example.model.Goal
import com.example.model.SandooqiDB
import com.example.model.SeasonalBudget
import com.example.ui.components.BudgetOrganizerCard
import com.example.ui.components.EmptyHintText
import com.example.ui.components.InputAmountDialog
import com.example.ui.components.KpiMiniCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private val GOAL_PRESETS = listOf(
    "💰 التوفير العام",
    "📱 شراء هاتف",
    "💻 شراء حاسوب",
    "🚗 شراء سيارة",
    "🛋️ شراء أثاث للمنزل",
    "🏠 تجهيز المنزل",
    "🎉 العيد والمناسبات",
    "✈️ السفر والعطلة",
    "📚 الدراسة والتعليم",
    "🧰 صندوق الطوارئ",
    "💍 الزواج",
    "🧵 مشروع خاص",
    "custom",
)

private val SEASON_PRESETS = listOf(
    "رمضان" to "🌙",
    "عيد الفطر" to "✨",
    "عيد الأضحى" to "🐑",
    "الدخول المدرسي" to "🎒",
    "أعراس ومناسبات" to "💐",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(
    db: SandooqiDB,
    onUpdateBudgetTotal: (Double?) -> Unit,
    onUpdateBudgetItemAmount: (String, Double) -> Unit,
    onAddBudgetItem: (String, Double) -> Unit,
    onDeleteBudgetItem: (String) -> Unit,
    onRecordBudgetExpense: (BudgetAllocationItem) -> Unit,
    onAddGoal: (name: String, target: Double) -> Unit,
    onDepositGoal: (goal: Goal, amount: Double, deductTreasury: Boolean) -> Unit,
    onWithdrawGoal: (goal: Goal, amount: Double, creditTreasury: Boolean) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddSeasonal: (name: String, occasionType: String, emoji: String, targetAmount: Double, targetDateIso: String, notes: String?) -> Unit,
    onDepositSeasonal: (budget: SeasonalBudget, amount: Double, deductTreasury: Boolean) -> Unit,
    onDeleteSeasonal: (Long) -> Unit,
    onUpdateBudgets: (groceryBudget: Double, marketBudget: Double) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val totalSaved = remember(db.goals) { db.goals.sumOf { it.saved } }

    var goalPreset by remember { mutableStateOf("💰 التوفير العام") }
    var goalNameCustom by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }

    var seasonType by remember { mutableStateOf("رمضان") }
    var seasonEmoji by remember { mutableStateOf("🌙") }
    var seasonName by remember { mutableStateOf("") }
    var seasonTarget by remember { mutableStateOf("") }
    var seasonDate by remember { mutableStateOf("") }
    var seasonNotes by remember { mutableStateOf("") }

    var gBudget by remember(db.groceryBudget) { mutableStateOf(db.groceryBudget.roundToInt().toString()) }
    var mBudget by remember(db.marketBudget) { mutableStateOf(db.marketBudget.roundToInt().toString()) }

    var depositGoalTarget by remember { mutableStateOf<Pair<Goal, Double?>?>(null) }
    var withdrawGoalTarget by remember { mutableStateOf<Goal?>(null) }
    var depositSeasonalTarget by remember { mutableStateOf<SeasonalBudget?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BudgetOrganizerCard(
            db = db,
            onUpdateBudgetTotal = onUpdateBudgetTotal,
            onUpdateBudgetItemAmount = onUpdateBudgetItemAmount,
            onAddBudgetItem = onAddBudgetItem,
            onDeleteBudgetItem = onDeleteBudgetItem,
            onRecordBudgetExpense = onRecordBudgetExpense,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMiniCard(
                label = "إجمالي المدخرات في الحصّالات",
                value = fmt(totalSaved, privacy),
                valueColor = ext.emerald,
                containerColor = ext.emeraldContainer,
                modifier = Modifier.weight(1f),
            )
            KpiMiniCard(
                label = "عدد أهداف الادخار",
                value = "${db.goals.size} هدف",
                modifier = Modifier.weight(1f),
            )
        }

        // Savings Goals Card
        SandooqiCard {
            SectionHeader(title = "حصّالات وأهداف الادخار", icon = Icons.Filled.Savings, iconTint = ext.emerald)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GOAL_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = goalPreset == preset,
                        onClick = { goalPreset = preset },
                        label = { Text(if (preset == "custom") "✏️ هدف مخصص" else preset) },
                    )
                }
            }
            if (goalPreset == "custom") {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = goalNameCustom,
                    onValueChange = { goalNameCustom = it },
                    label = { Text("اكتب اسم هدفك...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = goalTarget,
                    onValueChange = { goalTarget = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ المستهدف (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val t = goalTarget.toDoubleOrNull() ?: 0.0
                        val finalName = if (goalPreset == "custom") {
                            goalNameCustom.trim()
                        } else {
                            goalPreset.substringAfter(" ").trim().ifBlank { goalPreset }
                        }
                        if (finalName.isNotBlank() && t > 0) {
                            onAddGoal(finalName, t)
                            goalNameCustom = ""
                            goalTarget = ""
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("add_goal_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إنشاء هدف", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            if (db.goals.isEmpty()) {
                EmptyHintText("أضف حصّالة ادخار لبدء متابعة أهدافك.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    db.goals.forEach { g ->
                        val pct = if (g.target > 0) ((g.saved / g.target) * 100.0).roundToInt().coerceIn(0, 100) else 0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ext.cardBorder),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(g.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "المدخر: ${fmt(g.saved, privacy)} / ${fmt(g.target, privacy)} ($pct%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { onDeleteGoal(g.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الحصّالة", tint = ext.rose)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { depositGoalTarget = g to null },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("+ إيداع")
                                    }
                                    if (g.saved > 0) {
                                        OutlinedButton(
                                            onClick = { withdrawGoalTarget = g },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("سحب", color = ext.rose)
                                        }
                                    }
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(500.0, 1000.0, 2000.0, 5000.0).forEach { amt ->
                                        OutlinedButton(
                                            onClick = { depositGoalTarget = g to amt },
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Text("+${amt.roundToInt()} دج", style = MaterialTheme.typography.labelSmall)
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

        // Seasonal & Occasion Budgets
        SandooqiCard {
            SectionHeader(title = "ميزانيات المواسم والمناسبات", icon = Icons.Filled.AutoAwesome, iconTint = ext.amber)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SEASON_PRESETS.forEach { (type, emoji) ->
                    FilterChip(
                        selected = seasonType == type,
                        onClick = {
                            seasonType = type
                            seasonEmoji = emoji
                        },
                        label = { Text("$emoji $type") },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = seasonName,
                    onValueChange = { seasonName = it },
                    label = { Text("الاسم (مثال: رمضان 2026)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = seasonTarget,
                    onValueChange = { seasonTarget = FinanceUtils.normArDigits(it) },
                    label = { Text("الميزانية (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = seasonDate,
                    onValueChange = { seasonDate = it },
                    label = { Text("الموعد (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = seasonNotes,
                    onValueChange = { seasonNotes = it },
                    label = { Text("ملاحظة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val t = seasonTarget.toDoubleOrNull() ?: 0.0
                    val name = seasonName.trim().ifBlank { seasonType }
                    if (t > 0) {
                        onAddSeasonal(name, seasonType, seasonEmoji, t, seasonDate.trim(), seasonNotes.trim().ifBlank { null })
                        seasonName = ""
                        seasonTarget = ""
                        seasonDate = ""
                        seasonNotes = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ext.amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("إنشاء ميزانية موسم", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            if (db.seasonalBudgets.isEmpty()) {
                EmptyHintText("لا توجد ميزانيات موسمية مضافة بعد.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    db.seasonalBudgets.forEach { sb ->
                        val pct = if (sb.targetAmount > 0) ((sb.savedAmount / sb.targetAmount) * 100.0).roundToInt().coerceIn(0, 100) else 0
                        val remAmt = max(0.0, sb.targetAmount - sb.savedAmount)
                        val days = daysUntil(sb.targetDateIso)
                        val monthsLeft = if (days != null && days > 0) max(1, ceil(days / 30.0).toInt()) else null
                        val monthlySuggested = if (monthsLeft != null && remAmt > 0) kotlin.math.round(remAmt / monthsLeft) else null

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
                                        Text("${sb.emoji} ${sb.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${fmt(sb.savedAmount, privacy)} / ${fmt(sb.targetAmount, privacy)} ($pct%)" +
                                                if (monthlySuggested != null) " · ادّخر شهرياً ${fmt(monthlySuggested, privacy)}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row {
                                        Button(
                                            onClick = { depositSeasonalTarget = sb },
                                            shape = RoundedCornerShape(10.dp),
                                        ) { Text("+ ادخار") }
                                        IconButton(onClick = { onDeleteSeasonal(sb.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = ext.rose)
                                        }
                                    }
                                }
                                StatusProgressBar(percent = pct, barColor = ext.amber)
                            }
                        }
                    }
                }
            }
        }

        // Monthly Spending Limits
        SandooqiCard {
            SectionHeader(title = "حدود الميزانية الشهرية")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gBudget,
                    onValueChange = { gBudget = FinanceUtils.normArDigits(it) },
                    label = { Text("سقف المصروف الشهري (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = mBudget,
                    onValueChange = { mBudget = FinanceUtils.normArDigits(it) },
                    label = { Text("سقف قائمة السوق (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onUpdateBudgets(gBudget.toDoubleOrNull() ?: 0.0, mBudget.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("حفظ حدود الميزانية", fontWeight = FontWeight.Bold)
            }
        }
    }

    depositGoalTarget?.let { (goal, preset) ->
        InputAmountDialog(
            title = "إيداع في حصّالة: ${goal.name}",
            message = "المدخر حالياً ${fmt(goal.saved)} من أصل ${fmt(goal.target)}",
            initialValue = preset?.roundToInt()?.toString() ?: "",
            checkboxLabel = "خصم المبلغ من الحساب العائلي",
            checkboxDefault = true,
            confirmLabel = "إيداع",
            onDismiss = { depositGoalTarget = null },
            onSubmit = { amt, deduct -> onDepositGoal(goal, amt, deduct) },
        )
    }

    withdrawGoalTarget?.let { goal ->
        InputAmountDialog(
            title = "سحب من حصّالة: ${goal.name}",
            message = "الرصيد المتاح للسحب: ${fmt(goal.saved)}",
            initialValue = goal.saved.roundToInt().toString(),
            checkboxLabel = "إضافة المبلغ المسحوب إلى الحساب العائلي",
            checkboxDefault = true,
            confirmLabel = "تأكيد السحب",
            onDismiss = { withdrawGoalTarget = null },
            onSubmit = { amt, credit -> onWithdrawGoal(goal, amt, credit) },
        )
    }

    depositSeasonalTarget?.let { sb ->
        InputAmountDialog(
            title = "ادخار لموسم: ${sb.name}",
            message = "المجموع حتى الآن: ${fmt(sb.savedAmount)} / ${fmt(sb.targetAmount)}",
            checkboxLabel = "خصم المبلغ من الحساب العائلي",
            checkboxDefault = true,
            confirmLabel = "إيداع",
            onDismiss = { depositSeasonalTarget = null },
            onSubmit = { amt, deduct -> onDepositSeasonal(sb, amt, deduct) },
        )
    }
}
