package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.localDateISO
import com.example.data.PdfExportHelper
import com.example.model.AccountBookEntry
import com.example.model.SandooqiDB
import com.example.ui.components.BadgeTag
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyHintText
import com.example.ui.components.InputAmountDialog
import com.example.ui.components.PhotoAttachmentButton
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SandooqiThemeTokens
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsAccountsAndWarrantiesTab(
    db: SandooqiDB,
    onAddAccountBook: (kind: String, name: String, phone: String?, note: String?) -> Unit,
    onAddAccountBookEntry: (accountId: Long, sign: Int, defaultLabel: String, amount: Double) -> Unit,
    onDeleteAccountBook: (Long) -> Unit,
    onAddWarranty: (
        deviceName: String,
        category: String,
        shopOrBrand: String,
        purchaseDateIso: String,
        warrantyEndDateIso: String,
        cost: Double,
        photoData: String,
        notes: String?,
    ) -> Unit,
    onDeleteWarranty: (Long) -> Unit,
    onPreviewPhoto: (String, String) -> Unit,
    onCompressUri: suspend (android.net.Uri) -> String?,
) {
    val ext = SandooqiThemeTokens.colors
    val privacy = db.privacyMode
    val context = LocalContext.current

    var accKind by remember { mutableStateOf("customer") }
    var accName by remember { mutableStateOf("") }
    var accPhone by remember { mutableStateOf("") }
    var accNote by remember { mutableStateOf("") }
    var accSearch by remember { mutableStateOf("") }

    var entryDialogTarget by remember { mutableStateOf<Pair<AccountBookEntry, Int>?>(null) }

    var devName by remember { mutableStateOf("") }
    var warCat by remember { mutableStateOf("أجهزة منزلية") }
    var warFilter by remember { mutableStateOf("الكل") }
    var brand by remember { mutableStateOf("") }
    var buyDate by remember { mutableStateOf(localDateISO()) }
    var endDate by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var photoData by remember { mutableStateOf("") }

    val filteredAccounts = remember(db.accountsBook, accKind, accSearch) {
        val q = accSearch.trim().lowercase()
        db.accountsBook.filter {
            it.kind == accKind && (q.isBlank() || it.name.lowercase().contains(q))
        }
    }

    val filteredWarranties = remember(db.warranties, warFilter) {
        db.warranties.filter { warFilter == "الكل" || it.category == warFilter }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Customer / Supplier Ledger
        SandooqiCard {
            SectionHeader("دفتر حسابات الزبائن والموردين", Icons.Filled.People, ext.emerald)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = accKind == "customer", onClick = { accKind = "customer" }, label = { Text("الزبائن") })
                FilterChip(selected = accKind == "supplier", onClick = { accKind = "supplier" }, label = { Text("الموردين") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text(if (accKind == "customer") "اسم الزبون *" else "اسم المورد / المحل *") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
                OutlinedTextField(
                    value = accPhone,
                    onValueChange = { accPhone = it },
                    label = { Text("الهاتف") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(0.9f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = accNote, onValueChange = { accNote = it }, label = { Text("ملاحظة (اختياري)") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (accName.isNotBlank()) {
                            onAddAccountBook(accKind, accName.trim(), accPhone.trim().takeIf { it.isNotBlank() }, accNote.trim().takeIf { it.isNotBlank() })
                            accName = ""; accPhone = ""; accNote = ""
                        }
                    },
                    modifier = Modifier.height(48.dp).testTag("add_account_book_btn"),
                ) { Text("+ إضافة") }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = accSearch,
                onValueChange = { accSearch = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("بحث في الحسابات...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            if (filteredAccounts.isEmpty()) {
                EmptyHintText("لا توجد حسابات مسجلة.")
            } else {
                filteredAccounts.forEach { acc ->
                    val bal = acc.entries.sumOf { it.delta }
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    listOfNotNull(acc.phone, acc.note ?: if (acc.kind == "customer") "حساب زبون" else "حساب مورد").joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                fmt(bal, privacy),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bal > 0) ext.rose else ext.emerald,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { entryDialogTarget = acc to 1 }) { Text("+ إضافة") }
                                Button(onClick = { entryDialogTarget = acc to -1 }) { Text("− تسديد") }
                                IconButton(
                                    onClick = {
                                        val file = PdfExportHelper.generateAccountBookPdf(context, acc)
                                        PdfExportHelper.sharePdf(context, file, "كشف حساب ${acc.name} PDF")
                                    },
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "تصدير كشف حساب PDF", tint = ext.violet)
                                }
                            }
                            IconButton(onClick = { onDeleteAccountBook(acc.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الحساب", tint = ext.rose)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // Warranties Archive
        SandooqiCard {
            SectionHeader(
                title = "أرشيف ضمانات الأجهزة وقطع الغيار",
                icon = Icons.Filled.Shield,
                iconTint = ext.emerald,
                trailing = { BadgeTag("${db.warranties.size} ملف", ext.emeraldContainer, ext.onEmeraldContainer) },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = devName, onValueChange = { devName = it }, label = { Text("اسم الجهاز / آلة الخياطة / القطعة *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("أجهزة منزلية", "آلات الخياطة", "قطع غيار", "أخرى").forEach { cat ->
                    FilterChip(selected = warCat == cat, onClick = { warCat = cat }, label = { Text(cat) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("المحل أو الماركة") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = cost, onValueChange = { cost = FinanceUtils.normArDigits(it) }, label = { Text("السعر (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = buyDate, onValueChange = { buyDate = it }, label = { Text("تاريخ الشراء") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("انتهاء الضمان (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PhotoAttachmentButton(
                    hasPhoto = photoData.isNotBlank(),
                    labelIdle = "صورة الضمان",
                    labelAttached = "صورة مرفقة ✓",
                    testTag = "warranty_photo_btn",
                    onCompressUri = onCompressUri,
                    onPhotoReady = { photoData = it },
                    onClearPhoto = { photoData = "" },
                )
                Button(
                    onClick = {
                        if (devName.isNotBlank()) {
                            onAddWarranty(
                                devName.trim(),
                                warCat,
                                brand.trim(),
                                buyDate.trim(),
                                endDate.trim(),
                                max(0.0, cost.toDoubleOrNull() ?: 0.0),
                                photoData,
                                null,
                            )
                            devName = ""; brand = ""; endDate = ""; cost = ""; photoData = ""
                        }
                    },
                    modifier = Modifier.height(48.dp).testTag("save_warranty_btn"),
                ) { Text("حفظ") }
            }

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("الكل", "أجهزة منزلية", "آلات الخياطة", "قطع غيار", "أخرى").forEach { c ->
                    FilterChip(selected = warFilter == c, onClick = { warFilter = c }, label = { Text(c) })
                }
            }

            Spacer(Modifier.height(8.dp))
            if (filteredWarranties.isEmpty()) {
                EmptyHintText("لا توجد ضمانات محفوظة بعد.")
            } else {
                filteredWarranties.forEach { w ->
                    val d = daysUntil(w.warrantyEndDateIso)
                    val status = when {
                        d == null -> "بدون تاريخ انتهاء"
                        d < 0 -> "منتهي الصلاحية"
                        d <= 30 -> "ينتهي خلال $d يوم"
                        else -> "سارٍ ($d يوم)"
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(w.deviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${w.category}${if (w.shopOrBrand.isNotBlank()) " · ${w.shopOrBrand}" else ""} · $status${if (w.cost > 0) " · ${fmt(w.cost, privacy)}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (d != null && d < 0) ext.rose else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row {
                            if (w.photoData.isNotBlank()) {
                                TextButton(onClick = { onPreviewPhoto(w.photoData, w.deviceName) }) { Text("الصورة") }
                            }
                            IconButton(onClick = { onDeleteWarranty(w.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الضمان", tint = ext.rose)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    entryDialogTarget?.let { (acc, sign) ->
        InputAmountDialog(
            title = if (sign > 0) "إضافة مبلغ — ${acc.name}" else "تسديد مبلغ — ${acc.name}",
            message = "أدخل المبلغ المسجل في الحساب:",
            confirmLabel = if (sign > 0) "إضافة للحساب" else "تسجيل التسديد",
            onDismiss = { entryDialogTarget = null },
            onSubmit = { amt, _ ->
                onAddAccountBookEntry(acc.id, sign, if (sign > 0) "إضافة" else "تسديد", amt)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsSettingsTab(
    db: SandooqiDB,
    onAddFamilyMember: (String) -> Unit,
    onRemoveFamilyMember: (String) -> Unit,
    onEnableFamilyMode: () -> Unit,
    onUpdateSewingRates: (Double, Double) -> Unit,
    onAddCatalogProduct: (group: String, name: String, emoji: String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleAppLock: () -> Unit,
    onLockNow: () -> Unit,
    onChangePin: (String) -> Unit,
    exportBackupJson: () -> String,
    onImportBackupJson: (String) -> Unit,
    onSelectiveReset: (salary: Boolean, homeTx: Boolean, sewing: Boolean, homeDebts: Boolean, sewingDebts: Boolean) -> Unit,
    onResetAll: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val ext = SandooqiThemeTokens.colors
    val context = LocalContext.current

    var newMember by remember { mutableStateOf("") }
    var meterRate by remember(db.meterPrice) { mutableStateOf(db.meterPrice.roundToInt().toString()) }
    var threadRate by remember(db.threadPrice) { mutableStateOf(db.threadPrice.roundToInt().toString()) }

    var catGroup by remember { mutableStateOf("خضر") }
    var catName by remember { mutableStateOf("") }
    var catEmoji by remember { mutableStateOf("🛍️") }

    var newPin by remember { mutableStateOf("") }
    var selSalary by remember { mutableStateOf(false) }
    var selHomeTx by remember { mutableStateOf(false) }
    var selSewing by remember { mutableStateOf(false) }
    var selHomeDebts by remember { mutableStateOf(false) }
    var selSewingDebts by remember { mutableStateOf(false) }

    var confirmSelective by remember { mutableStateOf(false) }
    var confirmFullReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(exportBackupJson().toByteArray(Charsets.UTF_8))
                }
                onNotify("تم حفظ النسخة الاحتياطية JSON بنجاح")
            }.onFailure { onNotify("تعذر حفظ ملف النسخة الاحتياطية") }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!json.isNullOrBlank()) onImportBackupJson(json)
            }.onFailure { onNotify("تعذر قراءة الملف المحدد") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Family members & Restricted mode
        SandooqiCard {
            SectionHeader("أفراد العائلة ووضع التسجيل المقيّد", Icons.Filled.People, ext.emerald)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newMember, onValueChange = { newMember = it }, label = { Text("إضافة اسم فرد جديد...") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        if (newMember.isNotBlank()) {
                            onAddFamilyMember(newMember.trim())
                            newMember = ""
                        }
                    },
                    modifier = Modifier.height(48.dp),
                ) { Text("إضافة") }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                db.family.forEach { m ->
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                            Text(m, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (db.family.size > 1) {
                                IconButton(onClick = { onRemoveFamilyMember(m) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "حذف $m", tint = ext.rose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEnableFamilyMode,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("enable_family_mode_btn"),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("تفعيل وضع أفراد العائلة (تسجيل المشتريات فقط)")
            }
        }

        // Default Sewing Rates & Add Catalog Item
        SandooqiCard {
            SectionHeader("الأسعار الافتراضية للخياطة والكتالوج", Icons.Filled.ContentCut, ext.violet)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = meterRate, onValueChange = { meterRate = FinanceUtils.normArDigits(it) }, label = { Text("سعر المتر") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = threadRate, onValueChange = { threadRate = FinanceUtils.normArDigits(it) }, label = { Text("سعر الخيط") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onUpdateSewingRates(
                            max(1.0, meterRate.toDoubleOrNull() ?: 300.0),
                            max(0.0, threadRate.toDoubleOrNull() ?: 100.0),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.violet),
                ) { Text("حفظ") }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Text("إضافة صنف جديد لقائمة المنتجات", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("خضر", "فواكه", "لحوم بيضاء", "لحوم حمراء", "أسماك", "مواد غذائية").forEach { g ->
                    FilterChip(selected = catGroup == g, onClick = { catGroup = g }, label = { Text(g) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("اسم الصنف *") }, singleLine = true, modifier = Modifier.weight(1.3f))
                OutlinedTextField(value = catEmoji, onValueChange = { catEmoji = it }, label = { Text("أيقونة") }, singleLine = true, modifier = Modifier.weight(0.6f))
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onAddCatalogProduct(catGroup, catName.trim(), catEmoji.trim().ifBlank { "🛍️" })
                            catName = ""
                        }
                    },
                ) { Text("إضافة") }
            }
        }

        // Theme, Sound, App Lock, PIN & Backup
        SandooqiCard {
            SectionHeader("المظهر والأمان والنسخ الاحتياطي", Icons.Filled.Lock, ext.sky)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (db.darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (db.darkMode) "الوضع الليلي مفعّل" else "الوضع النهاري مفعّل", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Switch(checked = db.darkMode, onCheckedChange = { onToggleDarkMode() })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (db.sound) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("المؤثرات الصوتية", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Switch(checked = db.sound, onCheckedChange = { onToggleSound() })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("قفل التطبيق بالرمز السري عند الفتح", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("حماية الخزينة والبيانات عند فتح التطبيق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = db.appLockEnabled, onCheckedChange = { onToggleAppLock() })
            }
            if (db.appLockEnabled) {
                OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("قفل التطبيق الآن")
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = FinanceUtils.normArDigits(it) },
                    label = { Text("تغيير الرمز السري (4+ أرقام)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f).testTag("new_pin_input"),
                )
                Button(
                    onClick = {
                        if (newPin.trim().length >= 4) {
                            onChangePin(newPin.trim())
                            newPin = ""
                        }
                    },
                    modifier = Modifier.height(48.dp).testTag("save_pin_btn"),
                ) { Text("حفظ") }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("sandooqi-backup-${localDateISO()}.json") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_json_btn"),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تصدير JSON")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("import_json_btn"),
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استيراد JSON")
                }
            }
        }

        // Selective Reset & Full Reset
        SandooqiCard(borderColor = ext.rose) {
            SectionHeader("التصفير الاختياري للأقسام", Icons.Filled.DeleteOutline, ext.rose, subtitle = "صفّر قسماً محدداً دون المساس ببقية البيانات")
            Spacer(Modifier.height(8.dp))
            listOf(
                Triple(selSalary, "الراتب الحالي (الشهر الحالي فقط)") { v: Boolean -> selSalary = v },
                Triple(selHomeTx, "مصاريف ومدخول الصندوق المنزلي للشهر الحالي") { v: Boolean -> selHomeTx = v },
                Triple(selSewing, "حسابات الخياطة (المداخيل والمصاريف والتحويلات)") { v: Boolean -> selSewing = v },
                Triple(selHomeDebts, "سجلات ديون المحلات والبيت") { v: Boolean -> selHomeDebts = v },
                Triple(selSewingDebts, "سجلات ديون الورشة / الخياطة") { v: Boolean -> selSewingDebts = v },
            ).forEach { (checked, label, setter) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { setter(!checked) }.padding(vertical = 2.dp),
                ) {
                    Checkbox(checked = checked, onCheckedChange = setter)
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { confirmSelective = true },
                colors = ButtonDefaults.buttonColors(containerColor = ext.rose),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("تصفير الأقسام المحددة فقط", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmFullReset = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("تصفير جميع البيانات بالكامل", color = ext.rose, fontWeight = FontWeight.Bold) }
        }
    }

    if (confirmSelective) {
        ConfirmActionDialog(
            title = "تصفير الأقسام المحددة",
            message = "هل أنت متأكد من تصفير الأقسام المحددة فقط؟ يمكنك التراجع مباشرة من شريط التنبيه.",
            confirmLabel = "تصفير المحدد",
            danger = true,
            onDismiss = { confirmSelective = false },
            onConfirm = { onSelectiveReset(selSalary, selHomeTx, selSewing, selHomeDebts, selSewingDebts) },
        )
    }

    if (confirmFullReset) {
        ConfirmActionDialog(
            title = "تصفير جميع البيانات بالكامل",
            message = "سيتم مسح جميع العمليات والديون والطلبيات وإعادة التطبيق لحالته الأولى. هل تريد المتابعة؟",
            confirmLabel = "مسح الكل",
            danger = true,
            onDismiss = { confirmFullReset = false },
            onConfirm = onResetAll,
        )
    }
}
