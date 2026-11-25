package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.getTxYM
import com.example.model.AppPage
import com.example.model.BudgetAllocationItem
import com.example.model.SandooqiDB
import com.example.ui.components.BudgetOrganizerCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusProgressBar
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    db: SandooqiDB,
    onNavigate: (AppPage) -> Unit,
    onOpenVoice: () -> Unit,
    onQuickExpense: (String, Double) -> Unit,
    onOpenMonthClose: () -> Unit,
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
    val (_, remainingDays) = remember { FinanceUtils.daysInCurrentMonth() }

    val dailySafeBudget = max(0.0, report.home.balance / remainingDays)
    val netAvailable = max(0.0, report.home.opening + report.home.income + report.home.transfer)
    val spentPct = if (netAvailable > 0) {
        ((report.home.out / netAvailable) * 100.0).roundToInt().coerceIn(0, 100)
    } else if (report.home.out > 0) {
        100
    } else {
        0
    }

    val openDebts = remember(db.homeDebts) { db.homeDebts.filter { it.direction != "in" && debtRemaining(it) > 0 } }
    val totalOpenDebts = remember(openDebts) { openDebts.sumOf { debtRemaining(it) } }
    val overdueDebts = remember(openDebts) {
        openDebts.filter {
            val d = daysUntil(it.due)
            d != null && d <= 3
        }
    }
    val unpaidBills = remember(db.obligations, ym) { db.obligations.filter { ym !in it.paidMonths } }
    val expiringWarranties = remember(db.warranties) {
        db.warranties.filter {
            val d = daysUntil(it.warrantyEndDateIso)
            d != null && d in 0..30
        }
    }
    val activeOrders = remember(db.orders) { db.orders.filter { it.stage != "مسلّم" } }
    val totalGoalsSaved = remember(db.goals, db.seasonalBudgets) {
        db.goals.sumOf { it.saved } + db.seasonalBudgets.sumOf { it.savedAmount }
    }
    val pendingClosure = db.monthClosePending.firstOrNull()

    val familySpend = remember(db.family, db.tx, ym) {
        db.family.map { member ->
            val spent = db.tx.filter {
                getTxYM(it) == ym &&
                    it.type == "out" &&
                    it.person == member &&
                    it.cat != "transfer" &&
                    it.cat != "account_transfer" &&
                    it.cat != "sav" &&
                    it.source != "credit-purchase" &&
                    it.cat != "credit_purchase"
            }.sumOf { it.amount }
            member to spent
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Pending Month Close Banner
        if (pendingClosure != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.amberContainer,
                border = BorderStroke(1.dp, ext.amber.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = ext.amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "شهر ${formatYM(pendingClosure.month)} بانتظار الإغلاق والترحيل إلى ${formatYM(pendingClosure.target)}.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.onAmberContainer,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onOpenMonthClose,
                        colors = ButtonDefaults.buttonColors(containerColor = ext.amber),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("إغلاق الشهر", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hero Cash Balance Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(ext.heroStart, ext.heroEnd)))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رصيد الصندوق العائلي · ${formatYM(ym)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            Text(
                                text = "الراتب: ${fmt(report.salary.s1 + report.salary.s2, privacy)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = fmt(report.home.balance, privacy),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("hero_family_balance"),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "الحد اليومي الآمن ($remainingDays يوماً): ${fmt(dailySafeBudget, privacy)} / يوم",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.heroAccent,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenVoice,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_voice_button"),
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تسجيل بالصوت", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onNavigate(AppPage.EXPENSES) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.heroAccent,
                                contentColor = Color(0xFF0F172A),
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_add_expense_button"),
                        ) {
                            Icon(Icons.Filled.ShoppingBasket, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تسجيل مصروف", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "المصروف هذا الشهر: ${fmt(report.home.out, privacy)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f),
                            )
                            Text(
                                text = "$spentPct% من المتاح",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        StatusProgressBar(
                            percent = spentPct,
                            barColor = when {
                                spentPct >= 90 -> Color(0xFFFDA4AF)
                                spentPct >= 75 -> ext.heroAccent
                                else -> Color(0xFF6EE7B7)
                            },
                            trackColor = Color.Black.copy(alpha = 0.25f),
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            Triple("الحساب العائلي", report.home.balance, "family"),
                            Triple("الحساب الشخصي", report.personal.balance, "personal"),
                            Triple("حساب الخياطة", report.sewing.balance, "sewing"),
                        ).forEach { (label, bal, _) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigate(AppPage.TREASURY) },
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = fmt(bal, privacy),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Budget Organizer
        BudgetOrganizerCard(
            db = db,
            onUpdateBudgetTotal = onUpdateBudgetTotal,
            onUpdateBudgetItemAmount = onUpdateBudgetItemAmount,
            onAddBudgetItem = onAddBudgetItem,
            onDeleteBudgetItem = onDeleteBudgetItem,
            onRecordBudgetExpense = onRecordBudgetExpense,
        )

        // Alerts & Reminders
        if (overdueDebts.isNotEmpty() || unpaidBills.isNotEmpty() || expiringWarranties.isNotEmpty()) {
            SandooqiCard(
                containerColor = ext.roseContainer,
                borderColor = ext.rose.copy(alpha = 0.35f),
            ) {
                SectionHeader(
                    title = "تنبيهات مالية تستوجب المراجعة",
                    icon = Icons.Filled.Warning,
                    iconTint = ext.rose,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (overdueDebts.isNotEmpty()) {
                        AlertLinkRow(
                            text = "ديون محلات مستحقة أو قريبة الاستحقاق (${overdueDebts.size})",
                            actionLabel = "عرض",
                            color = ext.onRoseContainer,
                            onClick = { onNavigate(AppPage.BILLS) },
                        )
                    }
                    if (unpaidBills.isNotEmpty()) {
                        AlertLinkRow(
                            text = "فواتير والتزامات شهرية غير مدفوعة (${unpaidBills.size})",
                            actionLabel = "تسديد",
                            color = ext.onRoseContainer,
                            onClick = { onNavigate(AppPage.BILLS) },
                        )
                    }
                    if (expiringWarranties.isNotEmpty()) {
                        AlertLinkRow(
                            text = "ضمانات أجهزة تنتهي خلال 30 يوماً (${expiringWarranties.size})",
                            actionLabel = "الأرشيف",
                            color = ext.onRoseContainer,
                            onClick = { onNavigate(AppPage.TOOLS) },
                        )
                    }
                }
            }
        }

        // Quick 1-Tap Expenses
        SandooqiCard {
            SectionHeader(
                title = "مصاريف يومية بضغطة واحدة",
                trailing = {
                    TextButton(onClick = { onNavigate(AppPage.TOOLS) }) {
                        Text("تعديل الاختصارات", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            db.quickExpenses.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { q ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuickExpense(q.title, q.amount) }
                                .testTag("quick_expense_${q.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(q.emoji, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(6.dp))
                                    Text(q.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = fmt(q.amount, privacy),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.emerald,
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // 4 Domain Summary Cards (2x2)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DomainSummaryTile(
                title = "ديون المحلات المفتوحة",
                value = fmt(totalOpenDebts, privacy),
                subtitle = "${openDebts.size} دين مفتوح",
                valueColor = ext.rose,
                icon = Icons.Filled.AccountBalanceWallet,
                iconTint = ext.rose,
                onClick = { onNavigate(AppPage.BILLS) },
                modifier = Modifier.weight(1f),
            )
            DomainSummaryTile(
                title = "إجمالي المدخرات",
                value = fmt(totalGoalsSaved, privacy),
                subtitle = "${db.goals.size} حصّالة · ${db.seasonalBudgets.size} موسم",
                valueColor = ext.emerald,
                icon = Icons.Filled.Savings,
                iconTint = ext.emerald,
                onClick = { onNavigate(AppPage.GOALS) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DomainSummaryTile(
                title = "طلبيات الخياطة النشطة",
                value = "${activeOrders.size} طلبية",
                subtitle = "الورشة: ${fmt(report.sewing.balance, privacy)}",
                valueColor = ext.violet,
                icon = Icons.Filled.ContentCut,
                iconTint = ext.violet,
                onClick = { onNavigate(AppPage.TAILORING) },
                modifier = Modifier.weight(1f),
            )
            DomainSummaryTile(
                title = "صافي الشهر",
                value = fmt(report.combined.net, privacy),
                subtitle = "${report.arr.size} عملية مسجلة",
                valueColor = if (report.combined.net >= 0) ext.emerald else ext.rose,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = ext.sky,
                onClick = { onNavigate(AppPage.REPORTS) },
                modifier = Modifier.weight(1f),
            )
        }

        // Family Spending Breakdown
        SandooqiCard {
            SectionHeader(
                title = "مصاريف أفراد العائلة (${formatYM(ym)})",
                trailing = {
                    TextButton(onClick = { onNavigate(AppPage.TOOLS) }) {
                        Text("إدارة الأعضاء", style = MaterialTheme.typography.labelMedium)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            familySpend.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { (member, spent) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(member, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = fmt(spent, privacy),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ext.emerald,
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AlertLinkRow(
    text: String,
    actionLabel: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun DomainSummaryTile(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = SandooqiThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
