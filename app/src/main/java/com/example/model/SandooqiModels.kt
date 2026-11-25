package com.example.model

import com.squareup.moshi.JsonClass

enum class AppPage(val label: String) {
    HOME("الرئيسية"),
    TREASURY("الخزينة"),
    EXPENSES("المصاريف"),
    BILLS("الديون"),
    GOALS("الميزانية"),
    TAILORING("الخياطة"),
    REPORTS("التقارير"),
    TOOLS("الأدوات"),
}

@JsonClass(generateAdapter = true)
data class AccountInfo(
    val id: String = "family",
    val name: String = "الحساب العائلي",
    val openingBalance: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class DebtAllocation(
    val debtId: Long = 0L,
    val amount: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class Transaction(
    val id: Long = 0L,
    val box: String = "home",
    val accountId: String = "family",
    val cat: String = "exp",
    val type: String = "out",
    val name: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val dateISO: String = "",
    val ym: String = "",
    val person: String? = null,
    val shop: String? = null,
    val source: String? = null,
    val linkedCreditor: String? = null,
    val homeDebtId: Long? = null,
    val homeDebtAllocations: List<DebtAllocation>? = null,
    val transferId: Long? = null,
    val transferFromAccountId: String? = null,
    val transferToAccountId: String? = null,
    val rentUnit: String? = null,
    val photo: String? = null,
)

@JsonClass(generateAdapter = true)
data class Goal(
    val id: Long = 0L,
    val name: String = "",
    val target: Double = 0.0,
    val saved: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class SeasonalBudget(
    val id: Long = 0L,
    val name: String = "",
    val occasionType: String = "",
    val emoji: String = "🌙",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDateIso: String = "",
    val notes: String? = null,
    val createdAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class DebtPayment(
    val amount: Double = 0.0,
    val ym: String = "",
    val date: String = "",
    val transactionId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class HomeDebt(
    val id: Long = 0L,
    val creditor: String = "",
    val item: String = "",
    val activity: String = "مواد غذائية",
    val amount: Double = 0.0,
    val paid: Double = 0.0,
    val payments: List<DebtPayment> = emptyList(),
    val due: String? = null,
    val date: String = "",
    val ym: String = "",
    val direction: String? = "out",
    val rentUnit: String? = null,
    val photo: String? = null,
)

@JsonClass(generateAdapter = true)
data class RentDebt(
    val id: Long = 0L,
    val direction: String = "in",
    val unit: String = "كراء سكن",
    val party: String = "",
    val amount: Double = 0.0,
    val paid: Double = 0.0,
    val settled: Boolean = false,
    val treasuryApplied: Boolean = false,
    val transactionId: Long? = null,
    val due: String? = null,
    val date: String = "",
    val ym: String = "",
)

@JsonClass(generateAdapter = true)
data class Obligation(
    val id: Long = 0L,
    val name: String = "",
    val amount: Double = 0.0,
    val dueDay: Int = 15,
    val paidMonths: List<String> = emptyList(),
    val creditor: String? = null,
    val photo: String? = null,
)

@JsonClass(generateAdapter = true)
data class AccountBookTx(
    val id: Long = 0L,
    val delta: Double = 0.0,
    val note: String = "",
    val date: String = "",
    val ym: String = "",
)

@JsonClass(generateAdapter = true)
data class AccountBookEntry(
    val id: Long = 0L,
    val kind: String = "customer",
    val name: String = "",
    val phone: String? = null,
    val note: String? = null,
    val entries: List<AccountBookTx> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FacPlan(
    val id: Long = 0L,
    val name: String = "",
    val monthly: Double = 0.0,
    val months: Int = 12,
    val paidMonths: Int = 0,
    val startYM: String = "",
)

@JsonClass(generateAdapter = true)
data class OrderFabricLine(
    val id: String = "",
    val name: String = "",
    val source: String = "customer",
    val meters: Double = 0.0,
    val rate: Double = 0.0,
    val buyRate: Double = 0.0,
    val laborRate: Double? = null,
    val charge: Double = 0.0,
    val cost: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class OrderSupplyLine(
    val id: String = "",
    val name: String = "",
    val cost: Double = 0.0,
    val stockId: Long? = null,
    val qty: Double? = null,
    val unit: String? = null,
)

@JsonClass(generateAdapter = true)
data class OrderMeasurements(
    val len: String? = null,
    val sh: String? = null,
    val chest: String? = null,
    val waist: String? = null,
    val hip: String? = null,
    val sleeve: String? = null,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class SewingOrder(
    val id: Long = 0L,
    val client: String = "",
    val phone: String? = null,
    val model: String = "",
    val meters: Double = 0.0,
    val meterRate: Double? = null,
    val flatLabor: Double? = null,
    val adjustment: Double? = null,
    val price: Double = 0.0,
    val paid: Double = 0.0,
    val fabricCost: Double = 0.0,
    val threadCost: Double = 0.0,
    val stage: String = "جديد",
    val fittingDate: String? = null,
    val dueDate: String? = null,
    val date: String = "",
    val ym: String = "",
    val notes: String? = null,
    val photo: String? = null,
    val fabrics: List<OrderFabricLine>? = null,
    val supplies: List<OrderSupplyLine>? = null,
    val measurements: OrderMeasurements? = null,
)

@JsonClass(generateAdapter = true)
data class MeasurementRecord(
    val id: Long = 0L,
    val name: String = "",
    val phone: String? = null,
    val len: String = "",
    val sh: String = "",
    val chest: String = "",
    val waist: String = "",
    val hip: String = "",
    val sleeve: String = "",
    val note: String = "",
    val date: String = "",
)

@JsonClass(generateAdapter = true)
data class SewMaterial(
    val id: Long = 0L,
    val name: String = "",
    val unit: String = "قطعة",
    val qty: Double = 0.0,
    val cost: Double = 0.0,
    val min: Double = 2.0,
    val date: String = "",
)

@JsonClass(generateAdapter = true)
data class SewServiceRecord(
    val id: Long = 0L,
    val client: String = "",
    val item: String = "",
    val meters: Double = 0.0,
    val rate: Double = 0.0,
    val price: Double = 0.0,
    val thread: Double = 0.0,
    val supplies: Double = 0.0,
    val paid: Double = 0.0,
    val date: String = "",
    val ym: String = "",
)

@JsonClass(generateAdapter = true)
data class SewProjectRecord(
    val id: Long = 0L,
    val name: String = "",
    val meters: Double = 0.0,
    val fabricRate: Double = 0.0,
    val fabric: Double = 0.0,
    val supplies: Double = 0.0,
    val other: Double = 0.0,
    val sale: Double = 0.0,
    val paid: Double = 0.0,
    val status: String = "work",
    val date: String = "",
    val ym: String = "",
)

@JsonClass(generateAdapter = true)
data class SewDebtRecord(
    val id: Long = 0L,
    val type: String = "in",
    val name: String = "",
    val amount: Double = 0.0,
    val paid: Double = 0.0,
    val due: String? = null,
    val date: String = "",
    val ym: String = "",
)

@JsonClass(generateAdapter = true)
data class MarketItem(
    val id: Long = 0L,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "حبة",
    val price: Double = 0.0,
    val done: Boolean = false,
    val transactionId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class CatalogItem(
    val id: Long = 0L,
    val name: String = "",
    val group: String = "",
    val price: Double = 0.0,
    val unit: String = "حبة",
    val emoji: String = "🛍️",
)

@JsonClass(generateAdapter = true)
data class QuickExpensePreset(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val emoji: String = "🛍️",
)

@JsonClass(generateAdapter = true)
data class WarrantyRecord(
    val id: Long = 0L,
    val deviceName: String = "",
    val category: String = "أجهزة منزلية",
    val shopOrBrand: String = "",
    val purchaseDateIso: String = "",
    val warrantyEndDateIso: String = "",
    val cost: Double = 0.0,
    val photoData: String = "",
    val notes: String? = null,
    val createdAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class MonthArchive(
    val ym: String = "",
    val targetMonth: String = "",
    val salary1: Double = 0.0,
    val salary2: Double = 0.0,
    val carryIn: Double = 0.0,
    val otherIncome: Double = 0.0,
    val expenses: Double = 0.0,
    val closingBalance: Double = 0.0,
    val savedToGoal: Double = 0.0,
    val carriedForward: Double = 0.0,
    val closedAt: String = "",
    val savedGoalId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class MonthClosePending(
    val month: String = "",
    val target: String = "",
)

@JsonClass(generateAdapter = true)
data class BudgetAllocationItem(
    val id: String = "",
    val label: String = "",
    val amount: Double = 0.0,
    val isDefault: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class BudgetOrganizerState(
    val totalBudgetOverride: Double? = null,
    val items: List<BudgetAllocationItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MonthSalary(
    val s1: Double = 0.0,
    val s2: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class SandooqiDB(
    val s1: Double = 120000.0,
    val s2: Double = 30000.0,
    val pin: String = "1234",
    val meterPrice: Double = 300.0,
    val threadPrice: Double = 100.0,
    val groceryBudget: Double = 40000.0,
    val marketBudget: Double = 15000.0,
    val sound: Boolean = true,
    val privacyMode: Boolean = false,
    val darkMode: Boolean = false,
    val monthSalaries: Map<String, MonthSalary> = emptyMap(),
    val monthCarryovers: Map<String, Double> = emptyMap(),
    val monthArchive: Map<String, MonthArchive> = emptyMap(),
    val monthClosePending: List<MonthClosePending> = emptyList(),
    val lastActiveYM: String = "",
    val accounts: Map<String, AccountInfo> = emptyMap(),
    val goals: List<Goal> = emptyList(),
    val seasonalBudgets: List<SeasonalBudget> = emptyList(),
    val budgetOrganizer: BudgetOrganizerState = BudgetOrganizerState(),
    val facPlans: List<FacPlan> = emptyList(),
    val obligations: List<Obligation> = emptyList(),
    val orders: List<SewingOrder> = emptyList(),
    val measurements: List<MeasurementRecord> = emptyList(),
    val sewMaterials: List<SewMaterial> = emptyList(),
    val sewServices: List<SewServiceRecord> = emptyList(),
    val sewProjects: List<SewProjectRecord> = emptyList(),
    val debts: List<SewDebtRecord> = emptyList(),
    val marketList: List<MarketItem> = emptyList(),
    val family: List<String> = emptyList(),
    val tx: List<Transaction> = emptyList(),
    val homeDebts: List<HomeDebt> = emptyList(),
    val rentDebts: List<RentDebt> = emptyList(),
    val catalog: List<CatalogItem> = emptyList(),
    val warranties: List<WarrantyRecord> = emptyList(),
    val accountsBook: List<AccountBookEntry> = emptyList(),
    val quickExpenses: List<QuickExpensePreset> = emptyList(),
    val familyEntryOnlyMode: Boolean = false,
    val activeFamilyMember: String = "إسلام",
    val appLockEnabled: Boolean = false,
)

data class AccountSummary(
    val opening: Double,
    val income: Double,
    val out: Double,
    val transferIn: Double,
    val transferOut: Double,
    val transfer: Double,
    val balance: Double,
)

data class CombinedSummary(
    val opening: Double,
    val income: Double,
    val out: Double,
    val expense: Double,
    val net: Double,
    val balance: Double,
)

data class CashReport(
    val month: String,
    val arr: List<Transaction>,
    val salary: MonthSalary,
    val home: AccountSummary,
    val sewing: AccountSummary,
    val personal: AccountSummary,
    val combined: CombinedSummary,
)

data class VoiceParseResult(
    val title: String,
    val amount: String,
    val mode: String, // "cash" | "in" | "out"
    val party: String,
)
