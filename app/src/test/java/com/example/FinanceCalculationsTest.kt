package com.example

import com.example.data.FinanceUtils
import com.example.model.HomeDebt
import com.example.model.MonthSalary
import com.example.model.SandooqiDB
import com.example.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculationsTest {

    @Test
    fun normArDigits_handlesArabicNumeralsAndDecimalComma() {
        assertEquals("1250.5", FinanceUtils.normArDigits("١٬٢٥٠٫٥"))
        assertEquals("12.75", FinanceUtils.normArDigits("12,75"))
    }

    @Test
    fun round2_eliminatesFloatingPointTruncation() {
        assertEquals(1.05, FinanceUtils.round2(1.15 - 0.1), 0.0)
        assertEquals(0.29, FinanceUtils.round2(0.29), 0.0)
        assertEquals("2", FinanceUtils.fmtQty(2.0))
        assertEquals("1.5", FinanceUtils.fmtQty(1.5))
    }

    @Test
    fun debtRemaining_clearsTinyFloatEpsilon() {
        val debt = HomeDebt(id = 1L, amount = 100.3, paid = 100.29999999999998)
        assertEquals(0.0, FinanceUtils.debtRemaining(debt), 0.0)
        val openDebt = HomeDebt(id = 2L, amount = 500.0, paid = 199.75)
        assertEquals(300.25, FinanceUtils.debtRemaining(openDebt), 0.0)
    }

    @Test
    fun cashReport_accountsForTransfersAndExcludesCreditPurchases() {
        val ym = "2026-11"
        val db = SandooqiDB(
            s1 = 100_000.0,
            s2 = 20_000.0,
            monthSalaries = mapOf(ym to MonthSalary(100_000.0, 20_000.0)),
            monthCarryovers = mapOf(ym to 10_000.0),
            tx = listOf(
                Transaction(id = 1, accountId = "family", cat = "exp", type = "out", amount = 30_000.0, ym = ym),
                Transaction(id = 2, accountId = "family", cat = "transfer", type = "out", amount = 25_000.0, ym = ym, transferId = 99),
                Transaction(id = 3, accountId = "personal", cat = "transfer", type = "in", amount = 25_000.0, ym = ym, transferId = 99),
                Transaction(id = 4, accountId = "sewing", cat = "inc", type = "in", amount = 15_000.0, ym = ym),
                Transaction(id = 5, accountId = "family", cat = "credit_purchase", type = "out", source = "credit-purchase", amount = 9_999.0, ym = ym),
            ),
        )
        val report = FinanceUtils.computeCashReportForMonth(db, ym)
        assertEquals(75_000.0, report.home.balance, 0.001)
        assertEquals(-25_000.0, report.home.transfer, 0.001)
        assertEquals(25_000.0, report.personal.balance, 0.001)
        assertEquals(15_000.0, report.sewing.balance, 0.001)
        assertEquals(115_000.0, report.combined.balance, 0.001)
        assertEquals(105_000.0, report.combined.net, 0.001)
    }
}
