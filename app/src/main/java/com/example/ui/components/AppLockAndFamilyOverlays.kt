package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.localDateISO
import com.example.model.SandooqiDB
import com.example.ui.theme.SandooqiThemeTokens

@Composable
fun AppLockOverlay(
    onUnlock: (String) -> Boolean,
) {
    val ext = SandooqiThemeTokens.colors
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        SandooqiCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = ext.emeraldContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "قفل التطبيق",
                            tint = ext.emerald,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "صندوقي محمي بالرمز السري",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "أدخل الرمز السري للوصول إلى البيانات المالية",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val count = maxOf(4, pinInput.length)
                    repeat(count) { idx ->
                        val filled = idx < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = if (filled) ext.emerald else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                )
                        )
                    }
                }

                if (!errorMsg.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.rose,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(18.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("مسح", "0", "⌫"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { key ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable {
                                            errorMsg = null
                                            when (key) {
                                                "مسح" -> pinInput = ""
                                                "⌫" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                else -> if (pinInput.length < 10) pinInput += key
                                            }
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "⌫") {
                                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "حذف")
                                        } else {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pinInput.isBlank()) {
                            errorMsg = "أدخل الرمز السري أولاً"
                        } else if (!onUnlock(pinInput)) {
                            errorMsg = "الرمز السري غير صحيح"
                            pinInput = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unlock_app_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("فتح التطبيق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyEntryOverlay(
    db: SandooqiDB,
    onSelectMember: (String) -> Unit,
    onAddExpense: (title: String, amount: Double, person: String, shop: String, payMode: String, creditor: String) -> Unit,
    onAddFamilyExpense: (person: String, category: String, amount: Double) -> Unit,
    onOpenVoice: () -> Unit,
    onExitFamilyMode: (String) -> Boolean,
) {
    val ext = SandooqiThemeTokens.colors
    var tab by remember { mutableStateOf("daily") } // "daily" | "personal"
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("cash") }
    var creditor by remember { mutableStateOf("") }

    var famCategory by remember { mutableStateOf("غذاء ومأكولات") }
    var famAmount by remember { mutableStateOf("") }

    var showAdminDialog by remember { mutableStateOf(false) }
    var pinAttempt by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val activeMember = db.activeFamilyMember.ifBlank { db.family.firstOrNull() ?: "فرد العائلة" }
    val today = remember { localDateISO() }
    val todayEntries = remember(db.tx, activeMember, today) {
        db.tx.filter { it.person == activeMember && it.dateISO == today }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ext.emerald,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "وضع العائلة — التسجيل المقيّد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "إدخال المصاريف فقط دون الاطلاع على الأرصدة",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                OutlinedButton(
                    onClick = {
                        pinAttempt = ""
                        pinError = false
                        showAdminDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("family_admin_unlock_button"),
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("المدير", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Member selector
        SandooqiCard {
            SectionHeader(title = "من يسجّل المصروف الآن؟", icon = Icons.Filled.Person)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                db.family.forEach { member ->
                    FilterChip(
                        selected = activeMember == member,
                        onClick = { onSelectMember(member) },
                        label = { Text(member, fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }

        // Action mode switch + voice trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = tab == "daily",
                onClick = { tab = "daily" },
                label = { Text("مصروف منزلي") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == "personal",
                onClick = { tab = "personal" },
                label = { Text("مصروف شخصي") },
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onOpenVoice,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "صوت", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("صوت")
            }
        }

        if (tab == "daily") {
            SandooqiCard {
                SectionHeader(title = "تسجيل مشتريات / مصروف ($activeMember)", icon = Icons.Filled.ShoppingCart)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("ماذا اشتريت؟ (خبز، خضر...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = FinanceUtils.normArDigits(it) },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = shop,
                        onValueChange = { shop = it },
                        label = { Text("المحل (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = payMode == "cash",
                            onClick = { payMode = "cash" },
                            label = { Text("نقداً") },
                        )
                        FilterChip(
                            selected = payMode == "debt",
                            onClick = { payMode = "debt" },
                            label = { Text("بالدين (كريدي)") },
                        )
                    }
                    if (payMode == "debt") {
                        OutlinedTextField(
                            value = creditor,
                            onValueChange = { creditor = it },
                            label = { Text("اسم البائع / المحل") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && title.isNotBlank()) {
                                onAddExpense(title.trim(), amt, activeMember, shop.trim(), payMode, creditor.trim())
                                title = ""
                                amount = ""
                                shop = ""
                                creditor = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("family_submit_expense_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("تسجيل المصروف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            SandooqiCard {
                SectionHeader(title = "مصروف شخصي لـ $activeMember", icon = Icons.Filled.Person, iconTint = ext.sky)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("غذاء ومأكولات", "نقل ومواصلات", "دراسة", "علاج", "ملابس", "أخرى").forEach { cat ->
                        FilterChip(
                            selected = famCategory == cat,
                            onClick = { famCategory = cat },
                            label = { Text(cat) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = famAmount,
                    onValueChange = { famAmount = FinanceUtils.normArDigits(it) },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val amt = famAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddFamilyExpense(activeMember, famCategory, amt)
                            famAmount = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("تسجيل المصروف الشخصي", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Today's entries for active member
        SandooqiCard {
            SectionHeader(title = "سجل اليوم لـ $activeMember")
            Spacer(Modifier.height(8.dp))
            if (todayEntries.isEmpty()) {
                EmptyHintText("لم تسجل أي مصاريف اليوم بعد.")
            } else {
                todayEntries.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(t.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = fmt(t.amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.rose,
                        )
                    }
                }
            }
        }
    }

    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            title = { Text("الدخول لوضع المدير") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل الرمز السري للمدير لإلغاء القفل والعودة للتطبيق الكامل:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pinAttempt,
                        onValueChange = {
                            pinAttempt = FinanceUtils.normArDigits(it)
                            pinError = false
                        },
                        label = { Text("الرمز السري") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (pinError) {
                        Text("الرمز السري غير صحيح", color = ext.rose, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (onExitFamilyMode(pinAttempt)) {
                        showAdminDialog = false
                    } else {
                        pinError = true
                    }
                }) {
                    Text("فتح المدير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = false }) {
                    Text("إلغاء")
                }
            },
        )
    }
}
