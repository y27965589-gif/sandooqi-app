package com.example

import com.example.data.FinanceUtils
import com.example.data.SandooqiDao
import com.example.data.SandooqiRepository
import com.example.data.SandooqiStateEntity
import com.example.ui.SandooqiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSandooqiDao : SandooqiDao {
        private val flow = MutableStateFlow<SandooqiStateEntity?>(null)
        override fun observeState(): Flow<SandooqiStateEntity?> = flow
        override suspend fun getStateOnce(): SandooqiStateEntity? = flow.value
        override suspend fun upsertState(entity: SandooqiStateEntity) {
            flow.value = entity
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addingUncheckedItemsDoesNotDeductAndUncheckingOrDeletingRefundsBalance() = runTest(testDispatcher) {
        val repo = SandooqiRepository(FakeSandooqiDao())
        val seeded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            repo.ensureInitialized().also { initDb ->
                repo.saveDB(initDb.copy(sound = false))
            }
        }
        val vm = SandooqiViewModel(repo)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            var waited = 0
            while (vm.uiState.value.isLoading && waited < 5000) {
                advanceUntilIdle()
                Thread.sleep(10)
                waited += 10
            }
        }
        advanceUntilIdle()
        assertEquals(seeded.lastActiveYM, vm.uiState.value.db.lastActiveYM)

        vm.saveSalaries(10_000.0, 0.0)
        advanceUntilIdle()

        val ym = FinanceUtils.currentYM()
        val initialBalance = FinanceUtils.computeCashReportForMonth(vm.uiState.value.db, ym).home.balance
        assertEquals(10_000.0, initialBalance, 0.001)

        // 1. Adding an unchecked item must NOT deduct from cash balance or transactions
        vm.addMarketItem(name = "بطاطا", quantity = 2.0, unit = "كغ", price = 150.0)
        advanceUntilIdle()
        val afterAddDb = vm.uiState.value.db
        assertEquals(1, afterAddDb.marketList.size)
        assertEquals(false, afterAddDb.marketList.first().done)
        assertEquals(0, afterAddDb.tx.size)
        assertEquals(10_000.0, FinanceUtils.computeCashReportForMonth(afterAddDb, ym).home.balance, 0.001)

        // 2. Checking the item deducts 300 from balance and links transactionId
        val itemId = afterAddDb.marketList.first().id
        vm.toggleMarketItem(itemId, recordExpense = true)
        advanceUntilIdle()
        val afterCheckDb = vm.uiState.value.db
        assertEquals(true, afterCheckDb.marketList.first().done)
        assertEquals(1, afterCheckDb.tx.size)
        assertEquals(300.0, afterCheckDb.tx.first().amount, 0.001)
        assertEquals(9_700.0, FinanceUtils.computeCashReportForMonth(afterCheckDb, ym).home.balance, 0.001)

        // 3. Updating quantity (+1) on checked item updates linked expense to 450
        vm.updateMarketItem(itemId, quantity = 3.0, unit = null, price = null)
        advanceUntilIdle()
        val afterUpdateDb = vm.uiState.value.db
        assertEquals(450.0, afterUpdateDb.tx.first().amount, 0.001)
        assertEquals(9_550.0, FinanceUtils.computeCashReportForMonth(afterUpdateDb, ym).home.balance, 0.001)

        // 4. Unchecking (cancelling purchase) restores full 450 back to balance
        vm.toggleMarketItem(itemId, recordExpense = true)
        advanceUntilIdle()
        val afterUncheckDb = vm.uiState.value.db
        assertEquals(false, afterUncheckDb.marketList.first().done)
        assertNull(afterUncheckDb.marketList.first().transactionId)
        assertEquals(0, afterUncheckDb.tx.size)
        assertEquals(10_000.0, FinanceUtils.computeCashReportForMonth(afterUncheckDb, ym).home.balance, 0.001)

        // 5. Re-checking then deleting the item individually also refunds balance
        vm.toggleMarketItem(itemId, recordExpense = true)
        advanceUntilIdle()
        assertEquals(9_550.0, FinanceUtils.computeCashReportForMonth(vm.uiState.value.db, ym).home.balance, 0.001)

        vm.deleteMarketItem(itemId)
        advanceUntilIdle()
        val afterDeleteDb = vm.uiState.value.db
        assertEquals(0, afterDeleteDb.marketList.size)
        assertEquals(0, afterDeleteDb.tx.size)
        assertEquals(10_000.0, FinanceUtils.computeCashReportForMonth(afterDeleteDb, ym).home.balance, 0.001)
    }
}
