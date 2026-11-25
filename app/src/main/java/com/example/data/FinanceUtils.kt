package com.example.data

import com.example.model.AccountInfo
import com.example.model.AccountSummary
import com.example.model.BudgetAllocationItem
import com.example.model.BudgetOrganizerState
import com.example.model.CashReport
import com.example.model.CatalogItem
import com.example.model.CombinedSummary
import com.example.model.Goal
import com.example.model.HomeDebt
import com.example.model.MonthClosePending
import com.example.model.MonthSalary
import com.example.model.QuickExpensePreset
import com.example.model.SandooqiDB
import com.example.model.Transaction
import com.example.model.VoiceParseResult
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object FinanceUtils {

    val AR_MONTHS = listOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    val ACCOUNT_LABELS = mapOf(
        "family" to "الحساب العائلي",
        "personal" to "الحساب الشخصي",
        "sewing" to "حساب الخياطة",
    )

    private val idCounter = AtomicLong(System.currentTimeMillis())

    fun nextId(): Long {
        val now = System.currentTimeMillis()
        val prev = idCounter.get()
        val next = if (now > prev) now else prev + 1
        idCounter.set(next)
        return next
    }

    fun localDateISO(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(date)
    }

    fun todayLabel(date: Date = Date()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return sdf.format(date)
    }

    fun currentYM(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(date)
    }

    fun shiftYM(ym: String, deltaMonths: Int): String {
        val parts = ym.split("-")
        val cal = Calendar.getInstance()
        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val total = y * 12 + m + deltaMonths
        val ny = Math.floorDiv(total, 12)
        val nm = ((total % 12) + 12) % 12
        return String.format(Locale.US, "%04d-%02d", ny, nm + 1)
    }

    fun formatYM(ym: String): String {
        if (ym == "all") return "كل الأشهر"
        val parts = ym.split("-")
        val y = parts.getOrNull(0) ?: ""
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val idx = m - 1
        val mName = AR_MONTHS.getOrElse(idx) { m.toString() }
        return "$mName $y"
    }

    fun fmt(n: Double, masked: Boolean = false): String {
        if (masked) return "•••• دج"
        val rounded = n.roundToLong()
        val nf = NumberFormat.getIntegerInstance(Locale.FRANCE)
        return "${nf.format(rounded)} دج"
    }

    fun normArDigits(str: String): String {
        val map = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
            '٫' to '.', ',' to '.'
        )
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch == '٬') continue
            sb.append(map[ch] ?: ch)
        }
        return sb.toString()
    }

    fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    fun fmtQty(value: Double): String {
        val r = round2(value)
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }

    fun daysUntil(iso: String?): Int? {
        if (iso.isNullOrBlank()) return null
        val p = iso.trim().split("-")
        if (p.size != 3) return null
        val y = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val d = p[2].toIntOrNull() ?: return null

        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - todayCal.timeInMillis
        return (diffMs / 86_400_000.0).roundToLong().toInt()
    }

    fun getTxYM(t: Transaction): String {
        val ymRegex = Regex("^\\d{4}-\\d{2}$")
        if (ymRegex.matches(t.ym)) return t.ym
        if (t.dateISO.length >= 7 && Regex("^\\d{4}-\\d{2}").containsMatchIn(t.dateISO)) {
            return t.dateISO.substring(0, 7)
        }
        return currentYM()
    }

    fun salaryForMonth(db: SandooqiDB, ym: String): MonthSalary {
        val saved = db.monthSalaries[ym]
        if (saved != null) {
            return MonthSalary(
                s1 = max(0.0, saved.s1),
                s2 = max(0.0, saved.s2),
            )
        }
        if (ym == currentYM()) {
            return MonthSalary(
                s1 = max(0.0, db.s1),
                s2 = max(0.0, db.s2),
            )
        }
        return MonthSalary(0.0, 0.0)
    }

    fun carryoverForMonth(db: SandooqiDB, ym: String): Double {
        return max(0.0, db.monthCarryovers[ym] ?: 0.0)
    }

    fun transactionAccount(tx: Transaction): String {
        if (tx.accountId == "personal" || tx.accountId == "family" || tx.accountId == "sewing") {
            return tx.accountId
        }
        if (tx.box == "personal") return "personal"
        if (tx.box == "sewing") return "sewing"
        return "family"
    }

    private fun internalTransferDirection(tx: Transaction): String? {
        if (tx.cat == "transfer" || tx.cat == "account_transfer") return tx.type
        return null
    }

    private fun isCreditPurchase(tx: Transaction): Boolean {
        return tx.source == "credit-purchase" || tx.cat == "credit_purchase"
    }

    fun computeCashReportForMonth(db: SandooqiDB, month: String): CashReport {
        val rows = db.tx.filter { getTxYM(it) == month }
        val salary = salaryForMonth(db, month)

        var openHome = carryoverForMonth(db, month)
        var openPersonal = db.accounts["personal"]?.openingBalance ?: 0.0
        var openSewing = db.accounts["sewing"]?.openingBalance ?: 0.0

        for (tx in db.tx) {
            val acc = transactionAccount(tx)
            if (acc == "family" || getTxYM(tx) >= month || isCreditPurchase(tx)) continue
            val dir = internalTransferDirection(tx) ?: tx.type
            val delta = if (dir == "in") tx.amount else -tx.amount
            if (acc == "personal") openPersonal += delta
            else if (acc == "sewing") openSewing += delta
        }

        class BoxAcc(var income: Double = 0.0, var out: Double = 0.0, var incT: Double = 0.0, var outT: Double = 0.0)

        val homeBox = BoxAcc(income = salary.s1 + salary.s2)
        val sewingBox = BoxAcc()
        val personalBox = BoxAcc()

        for (tx in rows) {
            val amt = tx.amount
            val acc = transactionAccount(tx)
            val box = when (acc) {
                "sewing" -> sewingBox
                "personal" -> personalBox
                else -> homeBox
            }
            val dir = internalTransferDirection(tx)
            if (dir != null) {
                if (dir == "in") box.incT += amt else box.outT += amt
                continue
            }
            if (isCreditPurchase(tx)) continue
            if (tx.type == "in") box.income += amt
            if (tx.type == "out") box.out += amt
        }

        fun summarize(b: BoxAcc, openVal: Double) = AccountSummary(
            opening = openVal,
            income = b.income,
            out = b.out,
            transferIn = b.incT,
            transferOut = b.outT,
            transfer = b.incT - b.outT,
            balance = openVal + b.income + b.incT - b.outT - b.out,
        )

        val homeSummary = summarize(homeBox, openHome)
        val sewingSummary = summarize(sewingBox, openSewing)
        val personalSummary = summarize(personalBox, openPersonal)

        val totalIncome = homeBox.income + sewingBox.income + personalBox.income
        val totalOut = homeBox.out + sewingBox.out + personalBox.out
        val combinedOpening = openHome + openSewing + openPersonal

        return CashReport(
            month = month,
            arr = rows,
            salary = salary,
            home = homeSummary,
            sewing = sewingSummary,
            personal = personalSummary,
            combined = CombinedSummary(
                opening = combinedOpening,
                income = totalIncome,
                out = totalOut,
                expense = totalOut,
                net = totalIncome - totalOut,
                balance = homeSummary.balance + sewingSummary.balance + personalSummary.balance,
            ),
        )
    }

    fun debtRemaining(debt: HomeDebt): Double {
        val rem = debt.amount - debt.paid
        return if (rem < 0.005) 0.0 else round2(rem)
    }

    fun makeTransaction(
        accountId: String = "family",
        cat: String,
        type: String,
        name: String,
        amount: Double,
        dateISO: String? = null,
        ym: String? = null,
        person: String? = null,
        shop: String? = null,
        source: String? = null,
        linkedCreditor: String? = null,
        homeDebtId: Long? = null,
        rentUnit: String? = null,
        transferId: Long? = null,
        transferFromAccountId: String? = null,
        transferToAccountId: String? = null,
        photo: String? = null,
    ): Transaction {
        val box = if (accountId == "family") "home" else accountId
        return Transaction(
            id = nextId(),
            box = box,
            accountId = accountId,
            cat = cat,
            type = type,
            name = name,
            amount = max(0.0, amount),
            date = todayLabel(),
            dateISO = dateISO ?: localDateISO(),
            ym = ym ?: currentYM(),
            person = person?.takeIf { it.isNotBlank() },
            shop = shop?.takeIf { it.isNotBlank() },
            source = source,
            linkedCreditor = linkedCreditor,
            homeDebtId = homeDebtId,
            transferId = transferId,
            transferFromAccountId = transferFromAccountId,
            transferToAccountId = transferToAccountId,
            rentUnit = rentUnit,
            photo = photo?.takeIf { it.isNotBlank() },
        )
    }

    fun parseArabicVoiceEntry(raw: String): VoiceParseResult {
        val text = normArDigits(raw).trim().replace(Regex("\\s+"), " ")
        if (text.isBlank()) return VoiceParseResult("", "", "cash", "")

        data class NumHit(val valStr: String, val idx: Int, val raw: String)

        val nums = mutableListOf<NumHit>()
        val rx = Regex("(\\d+(?:[.,]\\d+)?)")
        for (m in rx.findAll(text)) {
            val before = text.substring(0, m.range.first)
            if (!Regex("(?:رقم|محل)\\s*$").containsMatchIn(before)) {
                nums.add(NumHit(m.value.replace(',', '.'), m.range.first, m.value))
            }
        }
        val chosen = nums.lastOrNull()
        val hasLah = Regex("(?:^|\\s)(?:له|لي عند|مستحق لي)(?=$|\\s)").containsMatchIn(text)
        val hasAlayh = Regex("(?:^|\\s)(?:عليه|علي|دين|كريدي|بالدين|آجل)(?=$|\\s)").containsMatchIn(text)
        val mode = if (hasLah && !hasAlayh) "in" else if (hasAlayh) "out" else "cash"

        val partyMatch = Regex("(?:^|\\s)(?:من عند|عند|من|لـ)\\s+([^\\s]+(?:\\s+[^\\s]+)?)").find(text)
        val stop = setOf("دين", "كريدي", "بالدين", "دج", "دينار", "له", "عليه")
        val party = partyMatch?.groupValues?.getOrNull(1)
            ?.split(" ")
            ?.filter { it !in stop }
            ?.take(2)
            ?.joinToString(" ")
            ?: ""

        var clean = if (chosen != null) {
            text.substring(0, chosen.idx) + " " + text.substring(chosen.idx + chosen.raw.length)
        } else {
            text
        }
        if (partyMatch != null) {
            clean = clean.replace(partyMatch.value, " ")
        }
        clean = clean
            .replace(Regex("(?:^|\\s)(?:دج|دينار|دين|كريدي|بالدين|آجل|نقدا|نقداً|له|عليه)(?=$|\\s)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return VoiceParseResult(
            title = clean,
            amount = chosen?.valStr ?: "",
            mode = mode,
            party = party,
        )
    }

    fun suggestMarketUnit(name: String): String {
        val n = name.trim()
        if (Regex("حليب|زيت|ماء|عصير").containsMatchIn(n)) return "لتر"
        if (Regex("بطاطا|بطاطس|طماطم|بصل|جزر|لحم|دجاج|سردين|سمك|تفاح|موز|برتقال|سكر|فرينة|سميد|دقيق").containsMatchIn(n)) return "كغ"
        if (Regex("معدنوس|قزبر|نعناع|سلاطة|خس").containsMatchIn(n)) return "ربطة"
        if (Regex("طماطم مصبرة|ياغورت|تونة").containsMatchIn(n)) return "علبة"
        return "حبة"
    }

    fun createDefaultCatalog(): List<CatalogItem> {
        data class GroupSpec(val group: String, val unit: String, val items: List<Pair<String, String>>)
        val groups = listOf(
            GroupSpec(
                "خضر", "كغ", listOf(
                    "بطاطا" to "🥔", "طماطم" to "🍅", "بصل" to "🧅", "ثوم" to "🧄",
                    "جزر" to "🥕", "قرعة (كوسة)" to "🥒", "فلفل حلو" to "🫑", "فلفل حار" to "🌶️",
                    "باذنجان" to "🍆", "خيار" to "🥒", "خس (سلاطة)" to "🥬", "شفلور / بروكلي" to "🥦",
                    "ملفوف (كرنب)" to "🥬", "شمندر" to "🫜", "جلبانة" to "🫛", "معدنوس وقزبر" to "🌿"
                )
            ),
            GroupSpec(
                "فواكه", "كغ", listOf(
                    "تفاح" to "🍎", "موز" to "🍌", "برتقال" to "🍊", "فراولة" to "🍓",
                    "رمان" to "🍎", "ليمون (قارص)" to "🍋", "مشمش" to "🍑", "كرز" to "🍒",
                    "تمر" to "🌴", "عنب" to "🍇", "دلاع (بطيخ)" to "🍉"
                )
            ),
            GroupSpec(
                "لحوم بيضاء", "كغ", listOf(
                    "دجاج كامل" to "🍗", "صدر دجاج" to "🍗", "أفخاذ دجاج" to "🍗",
                    "كبد دجاج" to "🥩", "ديك رومي (داند)" to "🦃"
                )
            ),
            GroupSpec(
                "لحوم حمراء", "كغ", listOf(
                    "لحم بقري" to "🥩", "لحم هبرة" to "🥩", "لحم غنمي" to "🍖", "مرقاز / نقانق" to "🌭"
                )
            ),
            GroupSpec(
                "أسماك", "كغ", listOf(
                    "سردين" to "🐟", "سمك دوراد/أبيض" to "🐟", "سلمون" to "🐟", "جمبري (كروفيت)" to "🦐"
                )
            ),
            GroupSpec(
                "مواد غذائية", "حبة", listOf(
                    "سميد" to "🌾", "فرينة" to "🥖", "زيت المائدة" to "🌻", "زيت زيتون" to "🫒",
                    "سكر" to "🍬", "قهوة" to "☕", "حليب" to "🥛", "بيض" to "🥚",
                    "جبن (فرماج)" to "🧀", "زبدة" to "🧈", "ياغورت" to "🥣", "خبز" to "🥖",
                    "كسكس" to "🍲", "أرز" to "🍚", "سباغيتي / مقرونة" to "🍝", "عدس" to "🫘",
                    "لوبيا يابسة" to "🫘", "حمص" to "🫘", "طماطم مصبرة" to "🥫", "ملح" to "🧂", "توابل" to "🌶️"
                )
            ),
        )
        var id = 1700000000000L
        return buildList {
            for (g in groups) {
                for ((name, emoji) in g.items) {
                    add(CatalogItem(id = id++, name = name, group = g.group, price = 0.0, unit = g.unit, emoji = emoji))
                }
            }
        }
    }

    fun createDefaultDB(): SandooqiDB {
        return SandooqiDB(
            s1 = 120000.0,
            s2 = 30000.0,
            pin = "1234",
            meterPrice = 300.0,
            threadPrice = 100.0,
            groceryBudget = 40000.0,
            marketBudget = 15000.0,
            sound = true,
            privacyMode = false,
            darkMode = false,
            lastActiveYM = currentYM(),
            accounts = mapOf(
                "family" to AccountInfo("family", "الحساب العائلي", 0.0),
                "personal" to AccountInfo("personal", "الحساب الشخصي", 0.0),
                "sewing" to AccountInfo("sewing", "حساب الخياطة", 0.0),
            ),
            goals = listOf(Goal(1L, "حصّالة الطوارئ والعيد", 50000.0, 0.0)),
            budgetOrganizer = BudgetOrganizerState(
                totalBudgetOverride = null,
                items = listOf(
                    BudgetAllocationItem("rent", "الكراء", 0.0, isDefault = true),
                    BudgetAllocationItem("living", "المصروف", 0.0, isDefault = true),
                ),
            ),
            family = listOf("إسلام", "محمد", "يوسف", "نعيمة"),
            catalog = createDefaultCatalog(),
            quickExpenses = listOf(
                QuickExpensePreset("q1", "خبز", 40.0, "🥖"),
                QuickExpensePreset("q2", "حليب", 120.0, "🥛"),
                QuickExpensePreset("q3", "قهوة", 100.0, "☕"),
                QuickExpensePreset("q4", "نقل", 200.0, "🚕"),
            ),
            activeFamilyMember = "إسلام",
        )
    }

    fun normalizeSandooqiDB(raw: SandooqiDB?): SandooqiDB {
        val def = createDefaultDB()
        if (raw == null) return def

        val familyClean = raw.family.filter { it.isNotBlank() }.ifEmpty { def.family }
        val activeMember = if (raw.activeFamilyMember in familyClean) raw.activeFamilyMember else familyClean.first()

        val existingNames = raw.catalog.map { it.name.trim() }.toSet()
        val mergedCatalog = if (raw.catalog.isEmpty()) {
            def.catalog
        } else {
            val missing = def.catalog.filter { it.name.trim() !in existingNames }
            raw.catalog + missing
        }

        val normalizedTx = raw.tx.map { t ->
            val acc = transactionAccount(t)
            t.copy(
                accountId = acc,
                box = if (acc == "family") "home" else acc,
                ym = getTxYM(t),
                amount = max(0.0, t.amount),
            )
        }

        val cur = currentYM()
        val prev = raw.lastActiveYM
        val monthSalaries = raw.monthSalaries.toMutableMap()
        val monthClosePending = raw.monthClosePending.toMutableList()
        if (prev.matches(Regex("^\\d{4}-\\d{2}$")) &&
            prev < cur &&
            !raw.monthArchive.containsKey(prev) &&
            !raw.monthCarryovers.containsKey(cur) &&
            monthClosePending.none { it.month == prev || it.target == cur }
        ) {
            if (!monthSalaries.containsKey(prev)) {
                monthSalaries[prev] = MonthSalary(max(0.0, raw.s1), max(0.0, raw.s2))
            }
            monthClosePending.add(MonthClosePending(prev, cur))
        }

        val accounts = mapOf(
            "family" to (raw.accounts["family"] ?: def.accounts["family"]!!),
            "personal" to (raw.accounts["personal"] ?: def.accounts["personal"]!!),
            "sewing" to (raw.accounts["sewing"] ?: def.accounts["sewing"]!!),
        )

        val organizer = if (raw.budgetOrganizer.items.isEmpty()) {
            def.budgetOrganizer
        } else {
            raw.budgetOrganizer
        }

        return raw.copy(
            s1 = max(0.0, raw.s1),
            s2 = max(0.0, raw.s2),
            meterPrice = max(0.0, raw.meterPrice),
            threadPrice = max(0.0, raw.threadPrice),
            groceryBudget = max(0.0, raw.groceryBudget),
            marketBudget = max(0.0, raw.marketBudget),
            family = familyClean,
            activeFamilyMember = activeMember,
            catalog = mergedCatalog,
            quickExpenses = raw.quickExpenses.ifEmpty { def.quickExpenses },
            tx = normalizedTx,
            accounts = accounts,
            budgetOrganizer = organizer,
            monthSalaries = monthSalaries,
            monthClosePending = monthClosePending,
            lastActiveYM = cur,
        )
    }

    private fun legacyHashPin(pin: String): String {
        val s = "sandouqi_salt_v10:$pin"
        var h = 2166136261L.toInt()
        for (ch in s) {
            h = h xor ch.code
            h *= 16777619
        }
        return "h$" + Integer.toHexString(h)
    }

    fun hashPinSha256(pin: String): String {
        val clean = normArDigits(pin).trim()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("sandooqi_sha256_v1:$clean".toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "s256$$hex"
    }

    fun verifyPin(input: String, stored: String): Boolean {
        val cleanInput = normArDigits(input).trim()
        val raw = stored.ifBlank { "1234" }
        return when {
            raw.startsWith("s256$") -> hashPinSha256(cleanInput) == raw
            raw.startsWith("h$") -> legacyHashPin(cleanInput) == raw
            else -> cleanInput == normArDigits(raw).trim()
        }
    }

    fun daysInCurrentMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = cal.get(Calendar.DAY_OF_MONTH)
        return maxDay to max(1, maxDay - today + 1)
    }

    fun currentWeekDays(): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        val diffToSat = (day % 7)
        cal.add(Calendar.DAY_OF_MONTH, -diffToSat)
        val names = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        return List(7) { idx ->
            val iso = localDateISO(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            iso to names[idx]
        }
    }

    fun formatPercent(part: Double, total: Double): Int {
        if (total <= 0.0) return 0
        return ((part / total) * 100.0).roundToLong().toInt().coerceIn(0, 100)
    }

    fun absVal(v: Double): Double = abs(v)
}
