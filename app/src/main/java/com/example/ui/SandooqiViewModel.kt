package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceUtils
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.formatYM
import com.example.data.FinanceUtils.getTxYM
import com.example.data.FinanceUtils.makeTransaction
import com.example.data.FinanceUtils.nextId
import com.example.data.FinanceUtils.shiftYM
import com.example.data.FinanceUtils.todayLabel
import com.example.data.FinanceUtils.transactionAccount
import com.example.data.SandooqiRepository
import com.example.model.AppPage
import com.example.model.BudgetAllocationItem
import com.example.model.CatalogItem
import com.example.model.DebtAllocation
import com.example.model.DebtPayment
import com.example.model.FacPlan
import com.example.model.Goal
import com.example.model.HomeDebt
import com.example.model.MarketItem
import com.example.model.MeasurementRecord
import com.example.model.MonthArchive
import com.example.model.MonthSalary
import com.example.model.Obligation
import com.example.model.OrderFabricLine
import com.example.model.OrderMeasurements
import com.example.model.OrderSupplyLine
import com.example.model.QuickExpensePreset
import com.example.model.RentDebt
import com.example.model.SandooqiDB
import com.example.model.SeasonalBudget
import com.example.model.SewDebtRecord
import com.example.model.SewMaterial
import com.example.model.SewProjectRecord
import com.example.model.SewServiceRecord
import com.example.model.SewingOrder
import com.example.model.Transaction
import com.example.model.WarrantyRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class SandooqiUiState(
    val db: SandooqiDB = FinanceUtils.createDefaultDB(),
    val isLoading: Boolean = true,
    val currentPage: AppPage = AppPage.HOME,
    val isAppLocked: Boolean = false,
    val toastMessage: String? = null,
    val toastVersion: Long = 0L,
    val canUndo: Boolean = false,
    val previewPhoto: Pair<String, String>? = null,
    val voiceOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val monthCloseOpen: Boolean = false,
    val moreSheetOpen: Boolean = false,
)

class SandooqiViewModel(
    val repository: SandooqiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SandooqiUiState())
    val uiState: StateFlow<SandooqiUiState> = _uiState.asStateFlow()

    private var undoSnapshot: SandooqiDB? = null

    init {
        viewModelScope.launch {
            val initial = repository.ensureInitialized()
            _uiState.update {
                it.copy(
                    db = initial,
                    isLoading = false,
                    isAppLocked = initial.appLockEnabled && !initial.familyEntryOnlyMode,
                )
            }
        }
    }

    private fun playChimeIfEnabled(enabled: Boolean) {
        if (!enabled) return
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            viewModelScope.launch {
                kotlinx.coroutines.delay(150)
                runCatching { tg.release() }
            }
        }
    }

    fun showToast(message: String, allowUndo: Boolean = false) {
        if (!allowUndo) undoSnapshot = null
        _uiState.update {
            it.copy(
                toastMessage = message,
                toastVersion = System.currentTimeMillis(),
                canUndo = allowUndo && undoSnapshot != null,
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null, canUndo = false) }
    }

    private fun commitDB(
        toastText: String? = null,
        allowUndo: Boolean = false,
        chime: Boolean = false,
        updater: (SandooqiDB) -> SandooqiDB,
    ) {
        val current = _uiState.value.db
        val next = updater(current)
        if (next === current || next == current) return

        if (allowUndo) {
            undoSnapshot = current
        } else {
            undoSnapshot = null
        }

        _uiState.update {
            it.copy(
                db = next,
                toastMessage = toastText ?: it.toastMessage,
                toastVersion = if (toastText != null) System.currentTimeMillis() else it.toastVersion,
                canUndo = if (toastText != null) allowUndo else it.canUndo,
            )
        }

        viewModelScope.launch {
            repository.saveDB(next)
        }

        if (chime) {
            playChimeIfEnabled(next.sound)
        }
    }

    fun undo() {
        val snap = undoSnapshot ?: return
        undoSnapshot = null
        _uiState.update {
            it.copy(
                db = snap,
                toastMessage = "تم التراجع عن آخر عملية",
                toastVersion = System.currentTimeMillis(),
                canUndo = false,
            )
        }
        viewModelScope.launch {
            repository.saveDB(snap)
        }
    }

    // Navigation & overlays
    fun navigate(page: AppPage) {
        _uiState.update { it.copy(currentPage = page, moreSheetOpen = false) }
    }

    fun setMoreSheetOpen(open: Boolean) {
        _uiState.update { it.copy(moreSheetOpen = open) }
    }

    fun setVoiceOpen(open: Boolean) {
        _uiState.update { it.copy(voiceOpen = open) }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(searchOpen = open) }
    }

    fun setMonthCloseOpen(open: Boolean) {
        _uiState.update { it.copy(monthCloseOpen = open) }
    }

    fun previewPhoto(dataUrl: String, title: String) {
        _uiState.update { it.copy(previewPhoto = dataUrl to title) }
    }

    fun closePreviewPhoto() {
        _uiState.update { it.copy(previewPhoto = null) }
    }

    fun unlockApp(pinAttempt: String): Boolean {
        val valid = FinanceUtils.verifyPin(pinAttempt, _uiState.value.db.pin)
        if (valid) {
            _uiState.update { it.copy(isAppLocked = false) }
        }
        return valid
    }

    fun lockNow() {
        _uiState.update { it.copy(isAppLocked = true) }
    }

    fun togglePrivacyMode() {
        val nextPrivacy = !_uiState.value.db.privacyMode
        commitDB(
            toastText = if (nextPrivacy) "تم إخفاء الأرصدة والمبالغ" else "تم إظهار الأرصدة والمبالغ"
        ) { it.copy(privacyMode = nextPrivacy) }
    }

    fun toggleDarkMode() {
        commitDB { it.copy(darkMode = !it.darkMode) }
    }

    fun toggleSound() {
        val next = !_uiState.value.db.sound
        commitDB(
            toastText = if (next) "تم تفعيل المؤثرات الصوتية" else "تم كتم المؤثرات الصوتية"
        ) { it.copy(sound = next) }
    }

    fun toggleAppLock() {
        val next = !_uiState.value.db.appLockEnabled
        commitDB(
            toastText = if (next) "تم تفعيل قفل التطبيق بالرمز السري عند الفتح" else "تم تعطيل قفل التطبيق عند الفتح"
        ) { it.copy(appLockEnabled = next) }
    }

    fun changePin(newPin: String) {
        val clean = newPin.trim()
        if (clean.length < 4) return
        val hashed = FinanceUtils.hashPinSha256(clean)
        commitDB(toastText = "تم تحديث الرمز السري وتشفيره بنجاح") {
            it.copy(pin = hashed)
        }
    }

    // Family Entry Restricted Mode
    fun setActiveFamilyMember(name: String) {
        commitDB { it.copy(activeFamilyMember = name) }
    }

    fun enableFamilyMode() {
        commitDB(toastText = "تم تفعيل وضع العائلة المقيّد") {
            it.copy(familyEntryOnlyMode = true)
        }
    }

    fun exitFamilyMode(pinAttempt: String): Boolean {
        val valid = FinanceUtils.verifyPin(pinAttempt, _uiState.value.db.pin)
        if (valid) {
            commitDB(toastText = "تم فتح وضع المدير الكامل") {
                it.copy(familyEntryOnlyMode = false)
            }
        }
        return valid
    }

    fun addFamilyMember(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        commitDB(toastText = "تمت إضافة $clean للعائلة") { prev ->
            if (clean in prev.family) prev else prev.copy(family = prev.family + clean)
        }
    }

    fun removeFamilyMember(name: String) {
        commitDB { prev ->
            if (prev.family.size <= 1) prev
            else {
                val nextFam = prev.family.filter { it != name }
                prev.copy(
                    family = nextFam,
                    activeFamilyMember = if (prev.activeFamilyMember == name) nextFam.first() else prev.activeFamilyMember,
                )
            }
        }
    }

    // Budget Organizer
    fun updateBudgetTotal(override: Double?) {
        val msg = if (override == null) {
            "تم ربط الميزانية بدخل الشهر تلقائياً"
        } else {
            "تم تحديد الميزانية بـ ${fmt(override)}"
        }
        commitDB(toastText = msg) { prev ->
            prev.copy(budgetOrganizer = prev.budgetOrganizer.copy(totalBudgetOverride = override))
        }
    }

    fun updateBudgetItemAmount(id: String, amount: Double) {
        commitDB { prev ->
            prev.copy(
                budgetOrganizer = prev.budgetOrganizer.copy(
                    items = prev.budgetOrganizer.items.map { item ->
                        if (item.id == id) item.copy(amount = max(0.0, amount)) else item
                    }
                )
            )
        }
    }

    fun addBudgetItem(label: String, amount: Double) {
        val clean = label.trim()
        if (clean.isBlank() || amount <= 0) return
        commitDB(toastText = "تمت إضافة «$clean» وخصمه من الميزانية", chime = true) { prev ->
            prev.copy(
                budgetOrganizer = prev.budgetOrganizer.copy(
                    items = prev.budgetOrganizer.items + BudgetAllocationItem(
                        id = "b_${nextId()}",
                        label = clean,
                        amount = amount,
                    )
                )
            )
        }
    }

    fun deleteBudgetItem(id: String) {
        commitDB(toastText = "تم حذف البند من الميزانية", allowUndo = true) { prev ->
            prev.copy(
                budgetOrganizer = prev.budgetOrganizer.copy(
                    items = prev.budgetOrganizer.items.filter { it.id != id }
                )
            )
        }
    }

    fun recordBudgetExpense(item: BudgetAllocationItem) {
        if (item.amount <= 0) return
        commitDB(
            toastText = "تم تسجيل صرف «${item.label}» (${fmt(item.amount)}) في الخزينة",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val tx = makeTransaction(
                accountId = "family",
                cat = "exp",
                type = "out",
                name = "بند الميزانية: ${item.label}",
                amount = item.amount,
                person = prev.family.firstOrNull() ?: "",
            )
            prev.copy(tx = listOf(tx) + prev.tx)
        }
    }

    // Quick Expenses
    fun quickExpense(title: String, amount: Double, person: String? = null) {
        if (title.isBlank() || amount <= 0) return
        commitDB(
            toastText = "تم تسجيل $title (${fmt(amount)})",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val tx = makeTransaction(
                accountId = "family",
                cat = "exp",
                type = "out",
                name = title,
                amount = amount,
                person = person?.takeIf { it.isNotBlank() } ?: prev.family.firstOrNull(),
            )
            prev.copy(tx = listOf(tx) + prev.tx)
        }
    }

    fun addQuickExpensePreset(title: String, amount: Double, emoji: String) {
        if (title.isBlank() || amount <= 0) return
        commitDB(toastText = "تمت إضافة الزر السريع «$title»") { prev ->
            prev.copy(
                quickExpenses = prev.quickExpenses + QuickExpensePreset(
                    id = "qe_${nextId()}",
                    title = title.trim(),
                    amount = amount,
                    emoji = emoji.ifBlank { "🛍️" },
                )
            )
        }
    }

    fun deleteQuickExpensePreset(id: String) {
        commitDB(toastText = "تم حذف الزر السريع", allowUndo = true) { prev ->
            prev.copy(quickExpenses = prev.quickExpenses.filter { it.id != id })
        }
    }

    // Treasury
    fun saveSalaries(s1: Double, s2: Double) {
        val ym = currentYM()
        val c1 = max(0.0, s1)
        val c2 = max(0.0, s2)
        commitDB(toastText = "تم حفظ رواتب شهر ${formatYM(ym)}", chime = true) { prev ->
            val nextMap = prev.monthSalaries.toMutableMap()
            nextMap[ym] = MonthSalary(c1, c2)
            prev.copy(s1 = c1, s2 = c2, monthSalaries = nextMap)
        }
    }

    fun addMovement(accountId: String, type: String, name: String, amount: Double) {
        if (name.isBlank() || amount <= 0) return
        commitDB(
            toastText = "تم تسجيل $name (${fmt(amount)})",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val tx = makeTransaction(
                accountId = accountId,
                cat = if (type == "in") "inc" else "exp",
                type = type,
                name = name.trim(),
                amount = amount,
            )
            prev.copy(tx = listOf(tx) + prev.tx)
        }
    }

    fun transferAccounts(from: String, to: String, amount: Double, note: String) {
        if (from == to || amount <= 0) return
        val fromLabel = FinanceUtils.ACCOUNT_LABELS[from] ?: from
        val toLabel = FinanceUtils.ACCOUNT_LABELS[to] ?: to
        val suffix = if (note.isNotBlank()) " — $note" else ""
        commitDB(
            toastText = "تم تحويل ${fmt(amount)} من $fromLabel إلى $toLabel",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val trId = nextId()
            val outTx = makeTransaction(
                accountId = from,
                cat = "transfer",
                type = "out",
                name = "تحويل إلى $toLabel$suffix",
                amount = amount,
                transferId = trId,
                transferFromAccountId = from,
                transferToAccountId = to,
            )
            val inTx = makeTransaction(
                accountId = to,
                cat = "transfer",
                type = "in",
                name = "تحويل من $fromLabel$suffix",
                amount = amount,
                transferId = trId,
                transferFromAccountId = from,
                transferToAccountId = to,
            )
            prev.copy(tx = listOf(inTx, outTx) + prev.tx)
        }
    }

    fun deleteTransaction(id: Long) {
        commitDB(toastText = "تم حذف العملية", allowUndo = true) { prev ->
            val target = prev.tx.find { it.id == id } ?: return@commitDB prev
            val removedIds = if (target.transferId != null) {
                prev.tx.filter { it.transferId == target.transferId }.map { it.id }.toSet()
            } else {
                setOf(id)
            }
            val nextTx = prev.tx.filter { it.id !in removedIds }

            val nextHomeDebts = prev.homeDebts.map { d ->
                val revertedPayments = d.payments.filter { it.transactionId in removedIds }
                if (revertedPayments.isEmpty()) {
                    d
                } else {
                    val revertedTotal = revertedPayments.sumOf { it.amount }
                    d.copy(
                        paid = max(0.0, FinanceUtils.round2(d.paid - revertedTotal)),
                        payments = d.payments.filter { it.transactionId !in removedIds },
                    )
                }
            }

            val nextRentDebts = prev.rentDebts.map { r ->
                if (r.transactionId in removedIds) {
                    r.copy(paid = 0.0, settled = false, treasuryApplied = false, transactionId = null)
                } else r
            }

            val nextMarketList = prev.marketList.map { m ->
                if (m.transactionId != null && m.transactionId in removedIds) {
                    m.copy(done = false, transactionId = null)
                } else m
            }

            prev.copy(
                tx = nextTx,
                homeDebts = nextHomeDebts,
                rentDebts = nextRentDebts,
                marketList = nextMarketList,
            )
        }
    }

    // Expenses & Market List
    fun addExpense(
        title: String,
        amount: Double,
        person: String,
        shop: String,
        category: String?,
        dateISO: String?,
        payMode: String, // "cash" | "debt"
        creditor: String,
        due: String?,
        photo: String?,
    ) {
        if (amount <= 0) return
        val ym = dateISO?.takeIf { it.length >= 7 }?.substring(0, 7) ?: currentYM()
        val finalCreditor = creditor.trim().ifBlank { shop.trim().ifBlank { "محل" } }
        val toast = if (payMode == "debt") {
            "تم تسجيل دين على $finalCreditor (${fmt(amount)})"
        } else {
            "تم تسجيل مصروف $title (${fmt(amount)})"
        }
        commitDB(toastText = toast, allowUndo = true, chime = true) { prev ->
            if (payMode == "debt") {
                val debt = HomeDebt(
                    id = nextId(),
                    creditor = finalCreditor,
                    item = title,
                    activity = category ?: "مواد غذائية",
                    amount = amount,
                    paid = 0.0,
                    payments = emptyList(),
                    due = due?.takeIf { it.isNotBlank() },
                    date = todayLabel(),
                    ym = ym,
                    direction = "out",
                    photo = photo?.takeIf { it.isNotBlank() },
                )
                prev.copy(homeDebts = listOf(debt) + prev.homeDebts)
            } else {
                val tx = makeTransaction(
                    accountId = "family",
                    cat = "exp",
                    type = "out",
                    name = title,
                    amount = amount,
                    dateISO = dateISO?.takeIf { it.isNotBlank() },
                    ym = ym,
                    person = person,
                    shop = shop,
                    photo = photo,
                )
                prev.copy(tx = listOf(tx) + prev.tx)
            }
        }
    }

    fun addFamilyExpense(person: String, category: String, amount: Double) {
        if (person.isBlank() || amount <= 0) return
        commitDB(
            toastText = "تم تسجيل مصروف $person (${fmt(amount)})",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val tx = makeTransaction(
                accountId = "family",
                cat = "fam",
                type = "out",
                name = "$category ($person)",
                amount = amount,
                person = person,
            )
            prev.copy(tx = listOf(tx) + prev.tx)
        }
    }

    fun addMarketItem(name: String, quantity: Double, unit: String, price: Double) {
        if (name.isBlank()) return
        commitDB(toastText = "تمت إضافة «${name.trim()}» لقائمة السوق") { prev ->
            val item = MarketItem(
                id = nextId(),
                name = name.trim(),
                quantity = max(0.1, quantity),
                unit = unit.ifBlank { "حبة" },
                price = max(0.0, price),
                done = false,
            )
            prev.copy(marketList = listOf(item) + prev.marketList)
        }
    }

    fun updateMarketItem(id: Long, quantity: Double? = null, unit: String? = null, price: Double? = null) {
        commitDB { prev ->
            var updatedItem: MarketItem? = null
            val nextList = prev.marketList.map { m ->
                if (m.id == id) {
                    val nextQty = max(0.1, quantity ?: m.quantity)
                    val nextUnit = unit ?: m.unit
                    val nextPrice = max(0.0, price ?: m.price)
                    val u = m.copy(
                        quantity = nextQty,
                        unit = nextUnit,
                        price = nextPrice,
                    )
                    updatedItem = u
                    u
                } else m
            }
            val item = updatedItem
            val txId = item?.transactionId
            val nextTx = if (item != null && item.done && txId != null) {
                val newCost = FinanceUtils.round2(item.price * max(0.0, item.quantity))
                if (newCost > 0) {
                    prev.tx.map { t ->
                        if (t.id == txId) {
                            t.copy(
                                amount = newCost,
                                name = "مشتريات السوق: ${item.name} (${FinanceUtils.fmtQty(item.quantity)} ${item.unit})",
                            )
                        } else t
                    }
                } else {
                    prev.tx.filterNot { it.id == txId }
                }
            } else prev.tx

            val finalList = if (item != null && item.done && txId != null && FinanceUtils.round2(item.price * max(0.0, item.quantity)) <= 0) {
                nextList.map { if (it.id == id) it.copy(transactionId = null) else it }
            } else nextList

            prev.copy(marketList = finalList, tx = nextTx)
        }
    }

    private fun findLinkedMarketTxId(txList: List<Transaction>, target: MarketItem): Long? {
        val explicitId = target.transactionId
        if (explicitId != null && txList.any { it.id == explicitId }) return explicitId
        val expectedCost = FinanceUtils.round2(target.price * max(0.0, target.quantity))
        val prefix = "مشتريات السوق: ${target.name} ("
        return txList.firstOrNull { t ->
            t.type == "out" &&
                t.cat == "exp" &&
                t.name.startsWith(prefix) &&
                kotlin.math.abs(t.amount - expectedCost) < 0.51
        }?.id
    }

    fun toggleMarketItem(id: Long, recordExpense: Boolean) {
        val preview = _uiState.value.db
        val targetPreview = preview.marketList.find { it.id == id } ?: return
        val nextDonePreview = !targetPreview.done
        val previewQty = max(0.0, targetPreview.quantity)
        val previewCost = FinanceUtils.round2(targetPreview.price * previewQty)
        val toastMsg = if (nextDonePreview) {
            if (recordExpense && previewCost > 0) {
                "تم شراء «${targetPreview.name}» وخصم ${fmt(previewCost)}"
            } else {
                "تم تعليم «${targetPreview.name}» كمقتنى"
            }
        } else {
            val linkedId = findLinkedMarketTxId(preview.tx, targetPreview)
            val refunded = linkedId?.let { tid -> preview.tx.find { it.id == tid }?.amount } ?: 0.0
            if (refunded > 0) {
                "تم إلغاء شراء «${targetPreview.name}» وإرجاع ${fmt(refunded)} للرصيد والميزانية"
            } else {
                "تم إلغاء تعليم «${targetPreview.name}»"
            }
        }

        commitDB(toastText = toastMsg, allowUndo = true, chime = true) { prev ->
            val target = prev.marketList.find { it.id == id } ?: return@commitDB prev
            val nextDone = !target.done
            val qty = max(0.0, target.quantity)
            val lineCost = FinanceUtils.round2(target.price * qty)

            if (nextDone) {
                val alreadyLinked = target.transactionId?.let { tid -> prev.tx.any { it.id == tid } } == true
                val createdTx = if (recordExpense && lineCost > 0 && !alreadyLinked) {
                    makeTransaction(
                        accountId = "family",
                        cat = "exp",
                        type = "out",
                        name = "مشتريات السوق: ${target.name} (${FinanceUtils.fmtQty(qty)} ${target.unit})",
                        amount = lineCost,
                        person = prev.family.firstOrNull(),
                    )
                } else null
                val nextList = prev.marketList.map {
                    if (it.id == id) it.copy(done = true, transactionId = createdTx?.id ?: it.transactionId) else it
                }
                val nextTx = if (createdTx != null) listOf(createdTx) + prev.tx else prev.tx
                prev.copy(marketList = nextList, tx = nextTx)
            } else {
                val removeTxId = findLinkedMarketTxId(prev.tx, target)
                val nextList = prev.marketList.map {
                    if (it.id == id) it.copy(done = false, transactionId = null) else it
                }
                val nextTx = if (removeTxId != null) prev.tx.filterNot { it.id == removeTxId } else prev.tx
                prev.copy(marketList = nextList, tx = nextTx)
            }
        }
    }

    fun deleteMarketItem(id: Long) {
        val preview = _uiState.value.db
        val targetPreview = preview.marketList.find { it.id == id }
        val refundPreview = if (targetPreview != null && targetPreview.done) {
            findLinkedMarketTxId(preview.tx, targetPreview)?.let { tid ->
                preview.tx.find { it.id == tid }?.amount ?: 0.0
            } ?: 0.0
        } else 0.0
        val toastMsg = if (refundPreview > 0 && targetPreview != null) {
            "تم حذف «${targetPreview.name}» وإرجاع ${fmt(refundPreview)} للرصيد والميزانية"
        } else {
            "تم الحذف من قائمة السوق"
        }

        commitDB(toastText = toastMsg, allowUndo = true) { prev ->
            val target = prev.marketList.find { it.id == id }
            val removeTxId = if (target != null && target.done) findLinkedMarketTxId(prev.tx, target) else null
            prev.copy(
                marketList = prev.marketList.filter { it.id != id },
                tx = if (removeTxId != null) prev.tx.filterNot { it.id == removeTxId } else prev.tx,
            )
        }
    }

    fun clearDoneMarket() {
        commitDB(toastText = "تم مسح الأغراض المقتناة", allowUndo = true) { prev ->
            prev.copy(marketList = prev.marketList.filter { !it.done })
        }
    }

    fun updateMarketBudget(budget: Double) {
        commitDB(toastText = "تم تحديث سقف قائمة السوق إلى ${fmt(budget)}") { prev ->
            prev.copy(marketBudget = max(0.0, budget))
        }
    }

    // Debts, Rent, Bills & Deductions
    fun addHomeDebt(
        creditor: String,
        item: String,
        activity: String,
        amount: Double,
        due: String?,
        direction: String,
        photo: String?,
    ) {
        if (creditor.isBlank() || amount <= 0) return
        val toast = if (direction == "in") {
            "تم تسجيل مستحق (له) عند $creditor"
        } else {
            "تم تسجيل دين (عليه) لـ $creditor"
        }
        commitDB(toastText = toast, allowUndo = true, chime = true) { prev ->
            val d = HomeDebt(
                id = nextId(),
                creditor = creditor.trim(),
                item = item.trim().ifBlank { activity },
                activity = activity.ifBlank { "دين عام" },
                amount = amount,
                paid = 0.0,
                payments = emptyList(),
                due = due?.takeIf { it.isNotBlank() },
                date = todayLabel(),
                ym = currentYM(),
                direction = direction,
                photo = photo?.takeIf { it.isNotBlank() },
            )
            prev.copy(homeDebts = listOf(d) + prev.homeDebts)
        }
    }

    fun payHomeDebt(debt: HomeDebt, payAmount: Double, applyTreasury: Boolean) {
        val rem = debtRemaining(debt)
        val payAmt = min(rem, max(0.0, payAmount))
        if (payAmt <= 0) return
        val isIn = debt.direction == "in"
        commitDB(
            toastText = "تم ${if (isIn) "تحصيل" else "تسديد"} ${fmt(payAmt)} لـ ${debt.creditor}",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val paymentTx = if (applyTreasury) {
                makeTransaction(
                    accountId = "family",
                    cat = if (isIn) "inc" else "debt_payment",
                    type = if (isIn) "in" else "out",
                    name = "${if (isIn) "تحصيل مستحق" else "تسديد دين"}: ${debt.creditor} (${debt.item})",
                    amount = payAmt,
                    linkedCreditor = debt.creditor,
                    homeDebtId = debt.id,
                )
            } else null

            prev.copy(
                tx = if (paymentTx != null) listOf(paymentTx) + prev.tx else prev.tx,
                homeDebts = prev.homeDebts.map { d ->
                    if (d.id == debt.id) {
                        d.copy(
                            paid = FinanceUtils.round2(d.paid + payAmt),
                            payments = d.payments + DebtPayment(
                                amount = payAmt,
                                ym = currentYM(),
                                date = todayLabel(),
                                transactionId = paymentTx?.id,
                            )
                        )
                    } else d
                }
            )
        }
    }

    fun payCreditorAll(creditor: String, amount: Double, direction: String, applyTreasury: Boolean) {
        if (amount <= 0) return
        val isIn = direction == "in"
        commitDB(
            toastText = "تم ${if (isIn) "تحصيل" else "تسديد"} ${fmt(amount)} لـ $creditor",
            allowUndo = true,
            chime = true,
        ) { prev ->
            val key = creditor.trim()
            val openOldest = prev.homeDebts
                .filter {
                    it.creditor.ifBlank { "غير محدد" }.trim() == key &&
                        (if (it.direction == "in") "in" else "out") == direction &&
                        debtRemaining(it) > 0
                }
                .sortedBy { it.id }

            var budget = amount
            val allocations = mutableListOf<DebtAllocation>()
            val allocMap = mutableMapOf<Long, Double>()
            for (d in openOldest) {
                if (budget <= 0) break
                val rem = debtRemaining(d)
                val slice = min(rem, budget)
                if (slice > 0) {
                    allocations.add(DebtAllocation(d.id, slice))
                    allocMap[d.id] = slice
                    budget -= slice
                }
            }
            val totalPaid = allocations.sumOf { it.amount }
            if (totalPaid <= 0) return@commitDB prev

            val paymentTx = if (applyTreasury) {
                makeTransaction(
                    accountId = "family",
                    cat = if (isIn) "inc" else "debt_payment",
                    type = if (isIn) "in" else "out",
                    name = "${if (isIn) "تحصيل مستحقات" else "تسديد ديون"}: $creditor",
                    amount = totalPaid,
                    linkedCreditor = creditor,
                ).copy(homeDebtAllocations = allocations)
            } else null

            prev.copy(
                tx = if (paymentTx != null) listOf(paymentTx) + prev.tx else prev.tx,
                homeDebts = prev.homeDebts.map { d ->
                    val add = allocMap[d.id] ?: return@map d
                    d.copy(
                        paid = FinanceUtils.round2(d.paid + add),
                        payments = d.payments + DebtPayment(
                            amount = add,
                            ym = currentYM(),
                            date = todayLabel(),
                            transactionId = paymentTx?.id,
                        )
                    )
                }
            )
        }
    }

    fun deleteHomeDebt(id: Long) {
        commitDB(toastText = "تم حذف سجل الدين", allowUndo = true) { prev ->
            prev.copy(homeDebts = prev.homeDebts.filter { it.id != id })
        }
    }

    fun addRentRecord(
        direction: String,
        unit: String,
        party: String,
        amount: Double,
        due: String?,
        immediate: Boolean,
    ) {
        if (party.isBlank() || amount <= 0) return
        commitDB(toastText = "تم تسجيل حركة الكراء/المستحق", allowUndo = true, chime = true) { prev ->
            val tx = if (immediate) {
                makeTransaction(
                    accountId = "family",
                    cat = "rent",
                    type = direction,
                    name = "$unit: $party",
                    amount = amount,
                    rentUnit = unit,
                )
            } else null
            val rent = RentDebt(
                id = nextId(),
                direction = direction,
                unit = unit,
                party = party.trim(),
                amount = amount,
                paid = if (immediate) amount else 0.0,
                settled = immediate,
                treasuryApplied = immediate,
                transactionId = tx?.id,
                due = due?.takeIf { it.isNotBlank() },
                date = todayLabel(),
                ym = currentYM(),
            )
            prev.copy(
                tx = if (tx != null) listOf(tx) + prev.tx else prev.tx,
                rentDebts = listOf(rent) + prev.rentDebts,
            )
        }
    }

    fun collectRent(id: Long) {
        commitDB(toastText = "تم التحصيل وإضافته للخزينة", allowUndo = true, chime = true) { prev ->
            val target = prev.rentDebts.find { it.id == id } ?: return@commitDB prev
            if (target.settled) return@commitDB prev
            val tx = makeTransaction(
                accountId = "family",
                cat = "rent",
                type = target.direction,
                name = "تحصيل ${target.unit}: ${target.party}",
                amount = target.amount,
                rentUnit = target.unit,
            )
            prev.copy(
                tx = listOf(tx) + prev.tx,
                rentDebts = prev.rentDebts.map { r ->
                    if (r.id == id) {
                        r.copy(paid = r.amount, settled = true, treasuryApplied = true, transactionId = tx.id)
                    } else r
                }
            )
        }
    }

    fun deleteRent(id: Long) {
        commitDB(toastText = "تم حذف سجل الكراء", allowUndo = true) { prev ->
            prev.copy(rentDebts = prev.rentDebts.filter { it.id != id })
        }
    }

    fun addObligation(name: String, amount: Double, dueDay: Int, creditor: String?, photo: String?) {
        if (name.isBlank() || amount <= 0) return
        commitDB(toastText = "تمت إضافة فاتورة $name") { prev ->
            val obl = Obligation(
                id = nextId(),
                name = name.trim(),
                amount = amount,
                dueDay = dueDay.coerceIn(1, 28),
                paidMonths = emptyList(),
                creditor = creditor?.takeIf { it.isNotBlank() },
                photo = photo?.takeIf { it.isNotBlank() },
            )
            prev.copy(obligations = listOf(obl) + prev.obligations)
        }
    }

    fun toggleObligationPaid(id: Long) {
        commitDB(toastText = "تم تحديث حالة الفاتورة", allowUndo = true, chime = true) { prev ->
            val ym = currentYM()
            val ob = prev.obligations.find { it.id == id } ?: return@commitDB prev
            val isPaid = ym in ob.paidMonths
            val nextObls = prev.obligations.map { o ->
                if (o.id == id) {
                    o.copy(paidMonths = if (isPaid) o.paidMonths - ym else o.paidMonths + ym)
                } else o
            }
            val nextTx = if (isPaid) prev.tx else {
                listOf(
                    makeTransaction(
                        accountId = "family",
                        cat = "bill",
                        type = "out",
                        name = "تسديد فاتورة: ${ob.name}",
                        amount = ob.amount,
                    )
                ) + prev.tx
            }
            prev.copy(obligations = nextObls, tx = nextTx)
        }
    }

    fun deleteObligation(id: Long) {
        commitDB(toastText = "تم حذف الالتزام", allowUndo = true) { prev ->
            prev.copy(obligations = prev.obligations.filter { it.id != id })
        }
    }

    fun addBillEntry(
        name: String,
        amount: Double,
        creditor: String,
        status: String, // "pending" | "paid"
        due: String?,
        unpaidMonths: Int,
        recurring: Boolean,
        photo: String?,
    ) {
        if (name.isBlank() || amount <= 0) return
        val toast = if (status == "pending") {
            "تم تسجيل فاتورة «$name» في الديون"
        } else {
            "تم دفع فاتورة «$name» وخصمها من الخزينة"
        }
        commitDB(toastText = toast, allowUndo = true, chime = true) { prev ->
            val ym = currentYM()
            val monthsCount = max(1, unpaidMonths)
            val totalDebtAmt = amount * monthsCount
            val finalCreditor = creditor.ifBlank { name }

            val nextDebts = if (status == "pending") {
                listOf(
                    HomeDebt(
                        id = nextId(),
                        creditor = finalCreditor,
                        item = if (monthsCount > 1) "$name ($monthsCount أشهر)" else name,
                        activity = "فواتير",
                        amount = totalDebtAmt,
                        paid = 0.0,
                        payments = emptyList(),
                        due = due?.takeIf { it.isNotBlank() },
                        date = todayLabel(),
                        ym = ym,
                        direction = "out",
                        photo = photo?.takeIf { it.isNotBlank() },
                    )
                ) + prev.homeDebts
            } else prev.homeDebts

            val nextTx = if (status == "paid") {
                listOf(
                    makeTransaction(
                        accountId = "family",
                        cat = "bill",
                        type = "out",
                        name = "فاتورة: $name${if (finalCreditor != name) " ($finalCreditor)" else ""}",
                        amount = amount,
                        photo = photo,
                    )
                ) + prev.tx
            } else prev.tx

            val nextObls = if (recurring) {
                listOf(
                    Obligation(
                        id = nextId(),
                        name = name,
                        amount = amount,
                        dueDay = 15,
                        paidMonths = if (status == "paid") listOf(ym) else emptyList(),
                        creditor = finalCreditor.takeIf { it.isNotBlank() },
                        photo = photo?.takeIf { it.isNotBlank() },
                    )
                ) + prev.obligations
            } else prev.obligations

            prev.copy(homeDebts = nextDebts, tx = nextTx, obligations = nextObls)
        }
    }

    fun addSalaryDeduction(
        facAmount: Double,
        facNote: String,
        srvAmount: Double,
        srvNote: String,
        srvMonth: String,
    ) {
        if (facAmount <= 0 && srvAmount <= 0) return
        commitDB(toastText = "تم تسجيل اقتطاع الراتب", allowUndo = true, chime = true) { prev ->
            val added = mutableListOf<Transaction>()
            if (facAmount > 0) {
                added.add(
                    makeTransaction(
                        accountId = "family",
                        cat = "fac",
                        type = "out",
                        name = if (facNote.isNotBlank()) "اقتطاع فاسيليتي: $facNote" else "اقتطاع فاسيليتي من الراتب",
                        amount = facAmount,
                        ym = currentYM(),
                    )
                )
            }
            if (srvAmount > 0) {
                added.add(
                    makeTransaction(
                        accountId = "family",
                        cat = "srv",
                        type = "out",
                        name = if (srvNote.isNotBlank()) "اقتطاع خدمات: $srvNote" else "اقتطاع خدمات / سلفة من الراتب",
                        amount = srvAmount,
                        ym = srvMonth.ifBlank { currentYM() },
                    )
                )
            }
            prev.copy(tx = added + prev.tx)
        }
    }

    fun addFacPlan(name: String, monthly: Double, months: Int) {
        if (name.isBlank() || monthly <= 0 || months <= 0) return
        commitDB(toastText = "تمت إضافة خطة قسط: $name") { prev ->
            prev.copy(
                facPlans = listOf(
                    FacPlan(id = nextId(), name = name.trim(), monthly = monthly, months = months, paidMonths = 0, startYM = currentYM())
                ) + prev.facPlans
            )
        }
    }

    fun payFacMonth(id: Long) {
        commitDB(toastText = "تم خصم قسط الشهر من الخزينة", allowUndo = true, chime = true) { prev ->
            val plan = prev.facPlans.find { it.id == id } ?: return@commitDB prev
            if (plan.paidMonths >= plan.months) return@commitDB prev
            prev.copy(
                facPlans = prev.facPlans.map { if (it.id == id) it.copy(paidMonths = it.paidMonths + 1) else it },
                tx = listOf(
                    makeTransaction(
                        accountId = "family",
                        cat = "bill",
                        type = "out",
                        name = "قسط (${plan.paidMonths + 1}/${plan.months}): ${plan.name}",
                        amount = plan.monthly,
                    )
                ) + prev.tx
            )
        }
    }

    fun deleteFacPlan(id: Long) {
        commitDB(toastText = "تم حذف خطة القسط", allowUndo = true) { prev ->
            prev.copy(facPlans = prev.facPlans.filter { it.id != id })
        }
    }

    // Goals, Seasonal & Monthly Limits
    fun addGoal(name: String, target: Double) {
        if (name.isBlank() || target <= 0) return
        commitDB(toastText = "تمت إضافة حصّالة: $name") { prev ->
            prev.copy(goals = listOf(Goal(nextId(), name.trim(), target, 0.0)) + prev.goals)
        }
    }

    fun depositGoal(goal: Goal, amount: Double, deductTreasury: Boolean) {
        if (amount <= 0) return
        commitDB(toastText = "تم إيداع ${fmt(amount)} في ${goal.name}", allowUndo = true, chime = true) { prev ->
            prev.copy(
                goals = prev.goals.map { if (it.id == goal.id) it.copy(saved = it.saved + amount) else it },
                tx = if (deductTreasury) {
                    listOf(
                        makeTransaction(
                            accountId = "family",
                            cat = "sav",
                            type = "out",
                            name = "إيداع ادخار: ${goal.name}",
                            amount = amount,
                        )
                    ) + prev.tx
                } else prev.tx
            )
        }
    }

    fun withdrawGoal(goal: Goal, amount: Double, creditTreasury: Boolean) {
        val amt = min(goal.saved, max(0.0, amount))
        if (amt <= 0) return
        commitDB(toastText = "تم سحب ${fmt(amt)} من ${goal.name}", allowUndo = true, chime = true) { prev ->
            prev.copy(
                goals = prev.goals.map { if (it.id == goal.id) it.copy(saved = max(0.0, it.saved - amt)) else it },
                tx = if (creditTreasury) {
                    listOf(
                        makeTransaction(
                            accountId = "family",
                            cat = "inc",
                            type = "in",
                            name = "سحب من حصّالة: ${goal.name}",
                            amount = amt,
                        )
                    ) + prev.tx
                } else prev.tx
            )
        }
    }

    fun deleteGoal(id: Long) {
        commitDB(toastText = "تم حذف الحصّالة", allowUndo = true) { prev ->
            prev.copy(goals = prev.goals.filter { it.id != id })
        }
    }

    fun addSeasonal(
        name: String,
        occasionType: String,
        emoji: String,
        targetAmount: Double,
        targetDateIso: String,
        notes: String?,
    ) {
        if (name.isBlank() || targetAmount <= 0) return
        commitDB(toastText = "تمت إضافة ميزانية $name") { prev ->
            val sb = SeasonalBudget(
                id = nextId(),
                name = name.trim(),
                occasionType = occasionType,
                emoji = emoji,
                targetAmount = targetAmount,
                savedAmount = 0.0,
                targetDateIso = targetDateIso,
                notes = notes?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
            )
            prev.copy(seasonalBudgets = listOf(sb) + prev.seasonalBudgets)
        }
    }

    fun depositSeasonal(budget: SeasonalBudget, amount: Double, deductTreasury: Boolean) {
        if (amount <= 0) return
        commitDB(toastText = "تم ادخار ${fmt(amount)} لـ ${budget.name}", allowUndo = true, chime = true) { prev ->
            prev.copy(
                seasonalBudgets = prev.seasonalBudgets.map {
                    if (it.id == budget.id) it.copy(savedAmount = it.savedAmount + amount) else it
                },
                tx = if (deductTreasury) {
                    listOf(
                        makeTransaction(
                            accountId = "family",
                            cat = "sav",
                            type = "out",
                            name = "ادخار موسمي: ${budget.name}",
                            amount = amount,
                        )
                    ) + prev.tx
                } else prev.tx
            )
        }
    }

    fun deleteSeasonal(id: Long) {
        commitDB(toastText = "تم حذف الميزانية الموسمية", allowUndo = true) { prev ->
            prev.copy(seasonalBudgets = prev.seasonalBudgets.filter { it.id != id })
        }
    }

    fun updateBudgets(groceryBudget: Double, marketBudget: Double) {
        commitDB(toastText = "تم تحديث حدود الميزانية الشهرية") { prev ->
            prev.copy(groceryBudget = max(0.0, groceryBudget), marketBudget = max(0.0, marketBudget))
        }
    }

    // Tailoring
    fun addOrder(order: SewingOrder, stockDeductions: List<Pair<Long, Double>>) {
        commitDB(toastText = "تم تسجيل طلبية ${order.client}", allowUndo = true, chime = true) { prev ->
            val deductMap = mutableMapOf<Long, Double>()
            for ((stockId, q) in stockDeductions) {
                deductMap[stockId] = (deductMap[stockId] ?: 0.0) + q
            }
            val nextMats = if (deductMap.isNotEmpty()) {
                prev.sewMaterials.map { m ->
                    val q = deductMap[m.id] ?: 0.0
                    if (q > 0) m.copy(qty = max(0.0, FinanceUtils.round2(m.qty - q))) else m
                }
            } else prev.sewMaterials

            val savedOrder = order.copy(id = nextId(), date = todayLabel(), ym = currentYM())
            val nextTx = if (order.paid > 0) {
                listOf(
                    makeTransaction(
                        accountId = "sewing",
                        cat = "inc",
                        type = "in",
                        name = "عربون خياطة: ${order.client} (${order.model})",
                        amount = order.paid,
                    )
                ) + prev.tx
            } else prev.tx

            prev.copy(sewMaterials = nextMats, orders = listOf(savedOrder) + prev.orders, tx = nextTx)
        }
    }

    fun updateOrderStage(id: Long, stage: String) {
        commitDB { prev ->
            prev.copy(orders = prev.orders.map { if (it.id == id) it.copy(stage = stage) else it })
        }
    }

    fun collectOrderPayment(order: SewingOrder, amount: Double) {
        val rem = max(0.0, order.price - order.paid)
        val amt = min(rem, max(0.0, amount))
        if (amt <= 0) return
        commitDB(toastText = "تم تحصيل ${fmt(amt)} لحساب الخياطة", allowUndo = true, chime = true) { prev ->
            prev.copy(
                orders = prev.orders.map { if (it.id == order.id) it.copy(paid = FinanceUtils.round2(it.paid + amt)) else it },
                tx = listOf(
                    makeTransaction(
                        accountId = "sewing",
                        cat = "inc",
                        type = "in",
                        name = "دفعة طلبية: ${order.client} (${order.model})",
                        amount = amt,
                    )
                ) + prev.tx
            )
        }
    }

    fun deleteOrder(id: Long) {
        commitDB(toastText = "تم حذف الطلبية", allowUndo = true) { prev ->
            prev.copy(orders = prev.orders.filter { it.id != id })
        }
    }

    fun addMeasurement(m: MeasurementRecord) {
        if (m.name.isBlank()) return
        commitDB(toastText = "تم حفظ مقاسات ${m.name}", chime = true) { prev ->
            prev.copy(measurements = listOf(m.copy(id = nextId(), date = todayLabel())) + prev.measurements)
        }
    }

    fun deleteMeasurement(id: Long) {
        commitDB(toastText = "تم حذف سجل المقاسات", allowUndo = true) { prev ->
            prev.copy(measurements = prev.measurements.filter { it.id != id })
        }
    }

    fun addSewService(s: SewServiceRecord) {
        commitDB(toastText = "تم تسجيل خدمة ${s.client}", allowUndo = true, chime = true) { prev ->
            val extra = mutableListOf<Transaction>()
            if (s.paid > 0) {
                extra.add(
                    makeTransaction(
                        accountId = "sewing",
                        cat = "inc",
                        type = "in",
                        name = "خدمة خياطة: ${s.client} (${s.item})",
                        amount = s.paid,
                    )
                )
            }
            val matCost = s.thread + s.supplies
            if (matCost > 0) {
                extra.add(
                    makeTransaction(
                        accountId = "sewing",
                        cat = "exp",
                        type = "out",
                        name = "خيط ولوازم خدمة: ${s.client} (${s.item})",
                        amount = matCost,
                    )
                )
            }
            prev.copy(
                sewServices = listOf(s.copy(id = nextId(), date = todayLabel(), ym = currentYM())) + prev.sewServices,
                tx = extra + prev.tx,
            )
        }
    }

    fun deleteSewService(id: Long) {
        commitDB(toastText = "تم حذف الخدمة", allowUndo = true) { prev ->
            prev.copy(sewServices = prev.sewServices.filter { it.id != id })
        }
    }

    fun addSewMaterial(name: String, unit: String, qty: Double, cost: Double, minQty: Double) {
        if (name.isBlank() || qty <= 0) return
        commitDB(toastText = "تمت إضافة «${name.trim()}» لمخزون الورشة", chime = true) { prev ->
            val existing = prev.sewMaterials.find {
                it.name.trim().equals(name.trim(), ignoreCase = true) && it.unit == unit
            }
            val nextList = if (existing != null) {
                val totalQty = existing.qty + qty
                val avgCost = if (totalQty > 0) kotlin.math.round((existing.qty * existing.cost + qty * cost) / totalQty) else cost
                prev.sewMaterials.map {
                    if (it.id == existing.id) {
                        it.copy(qty = FinanceUtils.round2(totalQty), cost = avgCost, min = minQty, date = todayLabel())
                    } else it
                }
            } else {
                listOf(SewMaterial(nextId(), name.trim(), unit, FinanceUtils.round2(qty), cost, minQty, todayLabel())) + prev.sewMaterials
            }
            prev.copy(sewMaterials = nextList)
        }
    }

    fun useSewMaterial(mat: SewMaterial, amountUsed: Double) {
        val used = min(mat.qty, max(0.0, amountUsed))
        if (used <= 0) return
        val costAmt = kotlin.math.round(used * mat.cost)
        commitDB(toastText = "تم سحب ${FinanceUtils.fmtQty(used)} ${mat.unit} من ${mat.name}", allowUndo = true, chime = true) { prev ->
            prev.copy(
                sewMaterials = prev.sewMaterials.map {
                    if (it.id == mat.id) it.copy(qty = max(0.0, FinanceUtils.round2(it.qty - used))) else it
                },
                tx = if (costAmt > 0) {
                    listOf(
                        makeTransaction(
                            accountId = "sewing",
                            cat = "exp",
                            type = "out",
                            name = "استهلاك مخزون: ${mat.name} (${FinanceUtils.fmtQty(used)} ${mat.unit})",
                            amount = costAmt,
                        )
                    ) + prev.tx
                } else prev.tx
            )
        }
    }

    fun deleteSewMaterial(id: Long) {
        commitDB(toastText = "تم حذف المادة من المخزون", allowUndo = true) { prev ->
            prev.copy(sewMaterials = prev.sewMaterials.filter { it.id != id })
        }
    }

    fun addSewProject(p: SewProjectRecord) {
        if (p.name.isBlank()) return
        commitDB(toastText = "تم حفظ مشروع «${p.name}»", allowUndo = true, chime = true) { prev ->
            val extra = mutableListOf<Transaction>()
            if (p.paid > 0) {
                extra.add(makeTransaction("sewing", "inc", "in", "مبيعات مشروع: ${p.name}", p.paid))
            }
            val totalCost = p.fabric + p.supplies + p.other
            if (totalCost > 0) {
                extra.add(makeTransaction("sewing", "exp", "out", "تكاليف مشروع: ${p.name}", totalCost))
            }
            prev.copy(
                sewProjects = listOf(p.copy(id = nextId(), date = todayLabel(), ym = currentYM())) + prev.sewProjects,
                tx = extra + prev.tx,
            )
        }
    }

    fun deleteSewProject(id: Long) {
        commitDB(toastText = "تم حذف المشروع", allowUndo = true) { prev ->
            prev.copy(sewProjects = prev.sewProjects.filter { it.id != id })
        }
    }

    fun sewCashMovement(type: String, name: String, amount: Double) {
        if (name.isBlank() || amount <= 0) return
        commitDB(toastText = "تم تسجيل $name (${fmt(amount)})", allowUndo = true, chime = true) { prev ->
            prev.copy(
                tx = listOf(
                    makeTransaction("sewing", if (type == "in") "inc" else "exp", type, name.trim(), amount)
                ) + prev.tx
            )
        }
    }

    fun transferSewingToFamily(amount: Double) {
        if (amount <= 0) return
        transferAccounts("sewing", "family", amount, "")
    }

    fun addSewDebt(type: String, name: String, amount: Double, due: String?) {
        if (name.isBlank() || amount <= 0) return
        commitDB(toastText = "تم تسجيل دين الورشة: $name", allowUndo = true, chime = true) { prev ->
            prev.copy(
                debts = listOf(
                    SewDebtRecord(nextId(), type, name.trim(), amount, 0.0, due?.takeIf { it.isNotBlank() }, todayLabel(), currentYM())
                ) + prev.debts
            )
        }
    }

    fun paySewDebt(debt: SewDebtRecord, amount: Double, applyCashbox: Boolean) {
        val rem = max(0.0, debt.amount - debt.paid)
        val amt = min(rem, max(0.0, amount))
        if (amt <= 0) return
        commitDB(toastText = "تم تسجيل ${fmt(amt)} لـ ${debt.name}", allowUndo = true, chime = true) { prev ->
            prev.copy(
                debts = prev.debts.map { if (it.id == debt.id) it.copy(paid = FinanceUtils.round2(it.paid + amt)) else it },
                tx = if (applyCashbox) {
                    listOf(
                        makeTransaction(
                            accountId = "sewing",
                            cat = if (debt.type == "in") "inc" else "debt_payment",
                            type = debt.type,
                            name = "${if (debt.type == "in") "تحصيل مستحق ورشة" else "تسديد دين ورشة"}: ${debt.name}",
                            amount = amt,
                        )
                    ) + prev.tx
                } else prev.tx
            )
        }
    }

    fun deleteSewDebt(id: Long) {
        commitDB(toastText = "تم حذف سجل دين الورشة", allowUndo = true) { prev ->
            prev.copy(debts = prev.debts.filter { it.id != id })
        }
    }

    // Tools: Warranties, AccountsBook, Rates, Catalog, Voice, MonthClose, Backup & Reset
    fun addWarranty(
        deviceName: String,
        category: String,
        shopOrBrand: String,
        purchaseDateIso: String,
        warrantyEndDateIso: String,
        cost: Double,
        photoData: String,
        notes: String?,
    ) {
        if (deviceName.isBlank()) return
        commitDB(toastText = "تم حفظ ضمان $deviceName", chime = true) { prev ->
            val rec = WarrantyRecord(
                id = nextId(),
                deviceName = deviceName.trim(),
                category = category,
                shopOrBrand = shopOrBrand.trim(),
                purchaseDateIso = purchaseDateIso,
                warrantyEndDateIso = warrantyEndDateIso,
                cost = max(0.0, cost),
                photoData = photoData,
                notes = notes?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
            )
            prev.copy(warranties = listOf(rec) + prev.warranties)
        }
    }

    fun deleteWarranty(id: Long) {
        commitDB(toastText = "تم حذف الضمان", allowUndo = true) { prev ->
            prev.copy(warranties = prev.warranties.filter { it.id != id })
        }
    }

    fun addAccountBook(kind: String, name: String, phone: String?, note: String?) {
        if (name.isBlank()) return
        commitDB(toastText = "تمت إضافة حساب «${name.trim()}»", chime = true) { prev ->
            val entry = com.example.model.AccountBookEntry(
                id = nextId(),
                kind = kind,
                name = name.trim(),
                phone = phone?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                entries = emptyList(),
            )
            prev.copy(accountsBook = listOf(entry) + prev.accountsBook)
        }
    }

    fun addAccountBookEntry(accountId: Long, sign: Int, defaultLabel: String, amount: Double) {
        if (amount <= 0) return
        val accName = _uiState.value.db.accountsBook.find { it.id == accountId }?.name ?: ""
        commitDB(toastText = "تم تسجيل ${fmt(amount)} في حساب $accName", allowUndo = true, chime = true) { prev ->
            prev.copy(
                accountsBook = prev.accountsBook.map { a ->
                    if (a.id == accountId) {
                        a.copy(
                            entries = listOf(
                                com.example.model.AccountBookTx(
                                    id = nextId(),
                                    delta = sign * amount,
                                    note = defaultLabel,
                                    date = todayLabel(),
                                    ym = currentYM(),
                                )
                            ) + a.entries
                        )
                    } else a
                }
            )
        }
    }

    fun deleteAccountBook(accountId: Long) {
        commitDB(toastText = "تم حذف الحساب", allowUndo = true) { prev ->
            prev.copy(accountsBook = prev.accountsBook.filter { it.id != accountId })
        }
    }

    fun updateSewingRates(meterPrice: Double, threadPrice: Double) {
        commitDB(toastText = "تم حفظ أسعار الخياطة الافتراضية", chime = true) { prev ->
            prev.copy(meterPrice = max(0.0, meterPrice), threadPrice = max(0.0, threadPrice))
        }
    }

    fun addCatalogProduct(group: String, name: String, emoji: String) {
        if (name.isBlank()) return
        commitDB(toastText = "تمت إضافة «${name.trim()}» للكتالوج") { prev ->
            prev.copy(
                catalog = prev.catalog + CatalogItem(
                    id = nextId(),
                    name = name.trim(),
                    group = group,
                    price = 0.0,
                    unit = "حبة",
                    emoji = emoji.ifBlank { "🛍️" },
                )
            )
        }
    }

    fun saveVoiceEntry(mode: String, title: String, amount: Double, party: String, applyNow: Boolean) {
        if (title.isBlank() || amount <= 0) return
        when (mode) {
            "cash" -> {
                commitDB(toastText = "تم تسجيل $title (${fmt(amount)})", allowUndo = true, chime = true) { prev ->
                    val tx = makeTransaction(
                        accountId = "family",
                        cat = "exp",
                        type = "out",
                        name = title,
                        amount = amount,
                        person = if (prev.familyEntryOnlyMode) prev.activeFamilyMember else prev.family.firstOrNull(),
                    )
                    prev.copy(tx = listOf(tx) + prev.tx)
                }
            }
            "in" -> {
                commitDB(toastText = "تم تسجيل مستحق لـ ${party.ifBlank { title }}", allowUndo = true, chime = true) { prev ->
                    val tx = if (applyNow) {
                        makeTransaction("family", "rent", "in", "$title${if (party.isNotBlank()) " ($party)" else ""}", amount)
                    } else null
                    val rent = RentDebt(
                        id = nextId(),
                        direction = "in",
                        unit = "مستحق شخصي",
                        party = party.ifBlank { title },
                        amount = amount,
                        paid = if (applyNow) amount else 0.0,
                        settled = applyNow,
                        treasuryApplied = applyNow,
                        transactionId = tx?.id,
                        date = todayLabel(),
                        ym = currentYM(),
                    )
                    prev.copy(
                        tx = if (tx != null) listOf(tx) + prev.tx else prev.tx,
                        rentDebts = listOf(rent) + prev.rentDebts,
                    )
                }
            }
            else -> {
                val creditorName = party.ifBlank { "محل" }
                commitDB(toastText = "تم تسجيل الدين عند $creditorName", allowUndo = true, chime = true) { prev ->
                    val debt = HomeDebt(
                        id = nextId(),
                        creditor = creditorName,
                        item = title,
                        activity = "تسجيل صوتي",
                        amount = amount,
                        paid = if (applyNow) amount else 0.0,
                        payments = if (applyNow) listOf(DebtPayment(amount, currentYM(), todayLabel())) else emptyList(),
                        date = todayLabel(),
                        ym = currentYM(),
                        direction = "out",
                    )
                    val tx = if (applyNow) {
                        makeTransaction("family", "debt_payment", "out", "تسديد فوري: $creditorName ($title)", amount)
                    } else null
                    prev.copy(
                        homeDebts = listOf(debt) + prev.homeDebts,
                        tx = if (tx != null) listOf(tx) + prev.tx else prev.tx,
                    )
                }
            }
        }
    }

    fun confirmMonthClose(sourceMonth: String, targetMonth: String, savePct: Int, targetGoalId: Long?) {
        val previewReport = FinanceUtils.computeCashReportForMonth(_uiState.value.db, sourceMonth)
        val previewClosing = max(0.0, previewReport.home.balance)
        val previewSaved = if (previewClosing > 0 && _uiState.value.db.goals.isNotEmpty()) {
            kotlin.math.round((previewClosing * savePct) / 100.0)
        } else 0.0
        val previewCarry = max(0.0, previewClosing - previewSaved)

        commitDB(
            toastText = "تم إغلاق ${formatYM(sourceMonth)} وترحيل ${fmt(previewCarry)}",
            chime = true,
        ) { prev ->
            val report = FinanceUtils.computeCashReportForMonth(prev, sourceMonth)
            val closing = max(0.0, report.home.balance)
            val savedToGoal = if (closing > 0 && prev.goals.isNotEmpty()) kotlin.math.round((closing * savePct) / 100.0) else 0.0
            val carriedForward = max(0.0, closing - savedToGoal)
            val chosenGoalId = targetGoalId ?: prev.goals.firstOrNull()?.id

            val priorArchive = prev.monthArchive[sourceMonth]
            val priorGoalId = priorArchive?.savedGoalId
            val priorSaved = priorArchive?.savedToGoal ?: 0.0

            val nextGoals = prev.goals.map { g ->
                var updatedSaved = g.saved
                if (priorSaved > 0 && priorGoalId == g.id) {
                    updatedSaved = max(0.0, updatedSaved - priorSaved)
                }
                if (savedToGoal > 0 && chosenGoalId == g.id) {
                    updatedSaved += savedToGoal
                }
                if (updatedSaved != g.saved) g.copy(saved = updatedSaved) else g
            }

            val nextCarry = prev.monthCarryovers.toMutableMap().apply {
                this[targetMonth] = carriedForward
            }
            val nextArchive = prev.monthArchive.toMutableMap().apply {
                this[sourceMonth] = MonthArchive(
                    ym = sourceMonth,
                    targetMonth = targetMonth,
                    salary1 = report.salary.s1,
                    salary2 = report.salary.s2,
                    carryIn = report.home.opening,
                    otherIncome = report.home.income - (report.salary.s1 + report.salary.s2),
                    expenses = report.home.out,
                    closingBalance = closing,
                    savedToGoal = savedToGoal,
                    carriedForward = carriedForward,
                    closedAt = FinanceUtils.localDateISO(),
                    savedGoalId = if (savedToGoal > 0) chosenGoalId else null,
                )
            }
            prev.copy(
                goals = nextGoals,
                monthCarryovers = nextCarry,
                monthArchive = nextArchive,
                monthClosePending = prev.monthClosePending.filter { it.month != sourceMonth },
            )
        }
        setMonthCloseOpen(false)
    }

    fun selectiveReset(
        salary: Boolean,
        homeTx: Boolean,
        sewing: Boolean,
        homeDebts: Boolean,
        sewingDebts: Boolean,
    ) {
        if (!salary && !homeTx && !sewing && !homeDebts && !sewingDebts) {
            showToast("اختر قسماً واحداً على الأقل للتصفير")
            return
        }
        val ym = currentYM()
        commitDB(toastText = "تم تصفير الأقسام المحددة بنجاح", allowUndo = true) { prev ->
            val nextSalaries = prev.monthSalaries.toMutableMap()
            if (salary) nextSalaries[ym] = MonthSalary(0.0, 0.0)
            val nextTx = prev.tx.filter { t ->
                val acc = transactionAccount(t)
                if (homeTx && acc == "family" && getTxYM(t) == ym) return@filter false
                if (sewing && acc == "sewing") return@filter false
                true
            }
            val nextAccounts = if (sewing) {
                prev.accounts.toMutableMap().apply {
                    val existing = this["sewing"]
                    if (existing != null) this["sewing"] = existing.copy(openingBalance = 0.0)
                }
            } else prev.accounts
            prev.copy(
                s1 = if (salary) 0.0 else prev.s1,
                s2 = if (salary) 0.0 else prev.s2,
                monthSalaries = nextSalaries,
                tx = nextTx,
                accounts = nextAccounts,
                homeDebts = if (homeDebts) emptyList() else prev.homeDebts,
                debts = if (sewingDebts) emptyList() else prev.debts,
            )
        }
    }

    fun resetAll() {
        val fresh = FinanceUtils.createDefaultDB()
        undoSnapshot = null
        _uiState.update {
            it.copy(
                db = fresh,
                toastMessage = "تم تصفير البيانات وإعادة الضبط",
                toastVersion = System.currentTimeMillis(),
                canUndo = false,
            )
        }
        viewModelScope.launch { repository.saveDB(fresh) }
    }

    fun importBackupJson(json: String) {
        val parsed = repository.parseImportedJson(json)
        if (parsed == null) {
            showToast("ملف النسخة الاحتياطية غير صالح")
            return
        }
        undoSnapshot = null
        _uiState.update {
            it.copy(
                db = parsed,
                toastMessage = "تم استيراد النسخة الاحتياطية بنجاح",
                toastVersion = System.currentTimeMillis(),
                canUndo = false,
            )
        }
        viewModelScope.launch { repository.saveDB(parsed) }
    }

    suspend fun compressImage(uri: Uri): String? = repository.compressUriToDataUrl(uri)

    class Factory(private val repository: SandooqiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SandooqiViewModel(repository) as T
        }
    }
}
