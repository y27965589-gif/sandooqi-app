package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.formatYM
import com.example.data.SandooqiDatabase
import com.example.data.SandooqiRepository
import com.example.model.AppPage
import com.example.ui.SandooqiViewModel
import com.example.ui.components.AppLockOverlay
import com.example.ui.components.FamilyEntryOverlay
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.MonthCloseDialog
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.QuickAddFabDialog
import com.example.ui.components.VoiceEntryDialog
import com.example.ui.screens.BillsDebtsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TailoringScreen
import com.example.ui.screens.ToolsSettingsScreen
import com.example.ui.screens.TreasuryScreen
import com.example.ui.theme.SandooqiTheme
import com.example.ui.theme.SandooqiThemeTokens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = SandooqiDatabase.getInstance(applicationContext)
        val repository = SandooqiRepository(db.sandooqiDao(), applicationContext)

        setContent {
            val viewModel: SandooqiViewModel = viewModel(
                factory = SandooqiViewModel.Factory(repository)
            )
            SandooqiRootApp(viewModel = viewModel)
        }
    }
}

private fun pageIcon(page: AppPage): ImageVector = when (page) {
    AppPage.HOME -> Icons.Filled.Home
    AppPage.TREASURY -> Icons.Filled.AccountBalanceWallet
    AppPage.EXPENSES -> Icons.Filled.ShoppingCart
    AppPage.BILLS -> Icons.Filled.CreditCard
    AppPage.GOALS -> Icons.Filled.Savings
    AppPage.TAILORING -> Icons.Filled.ContentCut
    AppPage.REPORTS -> Icons.Filled.Assessment
    AppPage.TOOLS -> Icons.Filled.Build
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SandooqiRootApp(viewModel: SandooqiViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var quickFabOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.toastVersion, uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (uiState.canUndo) "تراجع" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && uiState.canUndo) {
                viewModel.undo()
            } else {
                viewModel.clearToast()
            }
        }
    }

    SandooqiTheme(darkTheme = uiState.db.darkMode) {
        val ext = SandooqiThemeTokens.colors
        val db = uiState.db

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ext.emerald)
                }
            }

            uiState.isAppLocked -> {
                AppLockOverlay(onUnlock = viewModel::unlockApp)
            }

            db.familyEntryOnlyMode -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    FamilyEntryOverlay(
                        db = db,
                        onSelectMember = viewModel::setActiveFamilyMember,
                        onAddExpense = { title, amount, person, shop, payMode, creditor ->
                            viewModel.addExpense(
                                title = title,
                                amount = amount,
                                person = person,
                                shop = shop,
                                category = null,
                                dateISO = null,
                                payMode = payMode,
                                creditor = creditor,
                                due = null,
                                photo = null,
                            )
                        },
                        onAddFamilyExpense = viewModel::addFamilyExpense,
                        onOpenVoice = { viewModel.setVoiceOpen(true) },
                        onExitFamilyMode = viewModel::exitFamilyMode,
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    )
                }
            }

            else -> {
                BackHandler(enabled = uiState.currentPage != AppPage.HOME) {
                    viewModel.navigate(AppPage.HOME)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isWideScreen) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                Spacer(Modifier.height(12.dp))
                                AppPage.entries.forEach { page ->
                                    NavigationRailItem(
                                        selected = uiState.currentPage == page,
                                        onClick = { viewModel.navigate(page) },
                                        icon = { Icon(pageIcon(page), contentDescription = page.label) },
                                        label = { Text(page.label) },
                                    )
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.sandooqi_icon_src),
                                                contentDescription = "شعار صندوقي",
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "صندوقي",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                )
                                                Text(
                                                    text = "${uiState.currentPage.label} · ${formatYM(currentYM())}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { viewModel.setSearchOpen(true) },
                                            modifier = Modifier.testTag("topbar_search_btn"),
                                        ) {
                                            Icon(Icons.Filled.Search, contentDescription = "بحث شامل")
                                        }
                                        IconButton(
                                            onClick = { viewModel.setVoiceOpen(true) },
                                            modifier = Modifier.testTag("topbar_voice_btn"),
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = "تسجيل صوتي")
                                        }
                                        IconButton(
                                            onClick = viewModel::togglePrivacyMode,
                                            modifier = Modifier.testTag("topbar_privacy_btn"),
                                        ) {
                                            Icon(
                                                imageVector = if (db.privacyMode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (db.privacyMode) "إظهار الأرصدة" else "إخفاء الأرصدة",
                                            )
                                        }
                                        IconButton(
                                            onClick = viewModel::toggleDarkMode,
                                            modifier = Modifier.testTag("topbar_theme_btn"),
                                        ) {
                                            Icon(
                                                imageVector = if (db.darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                contentDescription = "تبديل المظهر",
                                            )
                                        }
                                        if (db.appLockEnabled) {
                                            IconButton(
                                                onClick = viewModel::lockNow,
                                                modifier = Modifier.testTag("topbar_lock_btn"),
                                            ) {
                                                Icon(Icons.Filled.Lock, contentDescription = "قفل التطبيق")
                                            }
                                        }
                                    },
                                )
                            },
                            bottomBar = {
                                if (!isWideScreen) {
                                    val secondaryPages = setOf(AppPage.TREASURY, AppPage.GOALS, AppPage.REPORTS, AppPage.TOOLS)
                                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                        listOf(AppPage.HOME, AppPage.EXPENSES, AppPage.BILLS, AppPage.TAILORING).forEach { page ->
                                            NavigationBarItem(
                                                selected = uiState.currentPage == page,
                                                onClick = { viewModel.navigate(page) },
                                                icon = { Icon(pageIcon(page), contentDescription = page.label) },
                                                label = { Text(page.label) },
                                                modifier = Modifier.testTag("nav_${page.name.lowercase()}"),
                                            )
                                        }
                                        NavigationBarItem(
                                            selected = uiState.currentPage in secondaryPages,
                                            onClick = { viewModel.setMoreSheetOpen(true) },
                                            icon = { Icon(Icons.Filled.Menu, contentDescription = "المزيد") },
                                            label = { Text("المزيد") },
                                            modifier = Modifier.testTag("nav_more"),
                                        )
                                    }
                                }
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { quickFabOpen = true },
                                    containerColor = ext.emerald,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("quick_add_fab"),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "إضافة سريعة")
                                }
                            },
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 760.dp)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                ) {
                                    when (uiState.currentPage) {
                                        AppPage.HOME -> HomeScreen(
                                            db = db,
                                            onNavigate = viewModel::navigate,
                                            onOpenVoice = { viewModel.setVoiceOpen(true) },
                                            onQuickExpense = { title, amt -> viewModel.quickExpense(title, amt) },
                                            onOpenMonthClose = { viewModel.setMonthCloseOpen(true) },
                                            onUpdateBudgetTotal = viewModel::updateBudgetTotal,
                                            onUpdateBudgetItemAmount = viewModel::updateBudgetItemAmount,
                                            onAddBudgetItem = viewModel::addBudgetItem,
                                            onDeleteBudgetItem = viewModel::deleteBudgetItem,
                                            onRecordBudgetExpense = viewModel::recordBudgetExpense,
                                        )

                                        AppPage.TREASURY -> TreasuryScreen(
                                            db = db,
                                            onSaveSalaries = viewModel::saveSalaries,
                                            onAddMovement = viewModel::addMovement,
                                            onTransfer = viewModel::transferAccounts,
                                            onOpenMonthClose = { viewModel.setMonthCloseOpen(true) },
                                        )

                                        AppPage.EXPENSES -> ExpensesScreen(
                                            db = db,
                                            onAddExpense = viewModel::addExpense,
                                            onAddFamilyExpense = viewModel::addFamilyExpense,
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onAddMarketItem = viewModel::addMarketItem,
                                            onUpdateMarketItem = viewModel::updateMarketItem,
                                            onToggleMarketItem = viewModel::toggleMarketItem,
                                            onDeleteMarketItem = viewModel::deleteMarketItem,
                                            onClearDoneMarket = viewModel::clearDoneMarket,
                                            onUpdateMarketBudget = viewModel::updateMarketBudget,
                                            onPreviewPhoto = viewModel::previewPhoto,
                                            onNotify = { viewModel.showToast(it) },
                                            onCompressUri = viewModel::compressImage,
                                        )

                                        AppPage.BILLS -> BillsDebtsScreen(
                                            db = db,
                                            onAddHomeDebt = viewModel::addHomeDebt,
                                            onPayHomeDebt = viewModel::payHomeDebt,
                                            onPayCreditorAll = viewModel::payCreditorAll,
                                            onDeleteHomeDebt = viewModel::deleteHomeDebt,
                                            onPreviewPhoto = viewModel::previewPhoto,
                                            onAddRentRecord = viewModel::addRentRecord,
                                            onCollectRent = viewModel::collectRent,
                                            onDeleteRent = viewModel::deleteRent,
                                            onAddObligation = viewModel::addObligation,
                                            onToggleObligationPaid = viewModel::toggleObligationPaid,
                                            onDeleteObligation = viewModel::deleteObligation,
                                            onAddBillEntry = viewModel::addBillEntry,
                                            onAddSalaryDeduction = viewModel::addSalaryDeduction,
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onAddFacPlan = viewModel::addFacPlan,
                                            onPayFacMonth = viewModel::payFacMonth,
                                            onDeleteFacPlan = viewModel::deleteFacPlan,
                                            onCompressUri = viewModel::compressImage,
                                        )

                                        AppPage.GOALS -> GoalsScreen(
                                            db = db,
                                            onUpdateBudgetTotal = viewModel::updateBudgetTotal,
                                            onUpdateBudgetItemAmount = viewModel::updateBudgetItemAmount,
                                            onAddBudgetItem = viewModel::addBudgetItem,
                                            onDeleteBudgetItem = viewModel::deleteBudgetItem,
                                            onRecordBudgetExpense = viewModel::recordBudgetExpense,
                                            onAddGoal = viewModel::addGoal,
                                            onDepositGoal = viewModel::depositGoal,
                                            onWithdrawGoal = viewModel::withdrawGoal,
                                            onDeleteGoal = viewModel::deleteGoal,
                                            onAddSeasonal = viewModel::addSeasonal,
                                            onDepositSeasonal = viewModel::depositSeasonal,
                                            onDeleteSeasonal = viewModel::deleteSeasonal,
                                            onUpdateBudgets = viewModel::updateBudgets,
                                        )

                                        AppPage.TAILORING -> TailoringScreen(
                                            db = db,
                                            onAddOrder = viewModel::addOrder,
                                            onUpdateOrderStage = viewModel::updateOrderStage,
                                            onCollectOrderPayment = viewModel::collectOrderPayment,
                                            onDeleteOrder = viewModel::deleteOrder,
                                            onPreviewPhoto = viewModel::previewPhoto,
                                            onAddMeasurement = viewModel::addMeasurement,
                                            onDeleteMeasurement = viewModel::deleteMeasurement,
                                            onAddSewService = viewModel::addSewService,
                                            onDeleteSewService = viewModel::deleteSewService,
                                            onAddSewMaterial = viewModel::addSewMaterial,
                                            onUseSewMaterial = viewModel::useSewMaterial,
                                            onDeleteSewMaterial = viewModel::deleteSewMaterial,
                                            onAddSewProject = viewModel::addSewProject,
                                            onDeleteSewProject = viewModel::deleteSewProject,
                                            onSewCashMovement = viewModel::sewCashMovement,
                                            onTransferSewingToFamily = viewModel::transferSewingToFamily,
                                            onAddSewDebt = viewModel::addSewDebt,
                                            onPaySewDebt = viewModel::paySewDebt,
                                            onDeleteSewDebt = viewModel::deleteSewDebt,
                                            onNotify = { viewModel.showToast(it) },
                                            onCompressUri = viewModel::compressImage,
                                        )

                                        AppPage.REPORTS -> ReportsScreen(
                                            db = db,
                                            buildCsv = { month -> viewModel.repository.buildCsvReport(db, month) },
                                            onDeleteTransaction = viewModel::deleteTransaction,
                                            onOpenMonthClose = { viewModel.setMonthCloseOpen(true) },
                                            onNotify = { viewModel.showToast(it) },
                                        )

                                        AppPage.TOOLS -> ToolsSettingsScreen(
                                            db = db,
                                            onAddQuickExpensePreset = viewModel::addQuickExpensePreset,
                                            onDeleteQuickExpensePreset = viewModel::deleteQuickExpensePreset,
                                            onAddAccountBook = viewModel::addAccountBook,
                                            onAddAccountBookEntry = viewModel::addAccountBookEntry,
                                            onDeleteAccountBook = viewModel::deleteAccountBook,
                                            onAddWarranty = viewModel::addWarranty,
                                            onDeleteWarranty = viewModel::deleteWarranty,
                                            onPreviewPhoto = viewModel::previewPhoto,
                                            onAddFamilyMember = viewModel::addFamilyMember,
                                            onRemoveFamilyMember = viewModel::removeFamilyMember,
                                            onEnableFamilyMode = viewModel::enableFamilyMode,
                                            onUpdateSewingRates = viewModel::updateSewingRates,
                                            onAddCatalogProduct = viewModel::addCatalogProduct,
                                            onToggleDarkMode = viewModel::toggleDarkMode,
                                            onToggleSound = viewModel::toggleSound,
                                            onToggleAppLock = viewModel::toggleAppLock,
                                            onLockNow = viewModel::lockNow,
                                            onChangePin = viewModel::changePin,
                                            exportBackupJson = { viewModel.repository.exportJson(db) },
                                            onImportBackupJson = viewModel::importBackupJson,
                                            onSelectiveReset = viewModel::selectiveReset,
                                            onResetAll = viewModel::resetAll,
                                            onNotify = { viewModel.showToast(it) },
                                            onCompressUri = viewModel::compressImage,
                                        )
                                    }
                                    Spacer(Modifier.height(76.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Modals & Sheets
        if (uiState.moreSheetOpen) {
            ModalBottomSheet(onDismissRequest = { viewModel.setMoreSheetOpen(false) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("الأقسام الإضافية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    listOf(
                        Triple(AppPage.TREASURY, "الخزينة", "الرواتب والحسابات والتحويلات"),
                        Triple(AppPage.GOALS, "الميزانية والادخار", "موزّع الميزانية، الحصّالات والمناسبات"),
                        Triple(AppPage.REPORTS, "التقارير المالية", "سجل العمليات، الرسوم، التصدير"),
                        Triple(AppPage.TOOLS, "الأدوات والإعدادات", "المحاكي، الحسابات، الضمانات والنسخ الاحتياطي"),
                    ).forEach { (page, title, subtitle) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (uiState.currentPage == page) ext.emeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigate(page) }
                                .testTag("more_nav_${page.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    pageIcon(page),
                                    contentDescription = title,
                                    tint = if (uiState.currentPage == page) ext.emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        if (uiState.voiceOpen) {
            VoiceEntryDialog(
                creditorNames = db.homeDebts.map { it.creditor }.filter { it.isNotBlank() }.distinct(),
                familyEntryOnlyMode = db.familyEntryOnlyMode,
                onDismiss = { viewModel.setVoiceOpen(false) },
                onSave = viewModel::saveVoiceEntry,
            )
        }

        if (uiState.searchOpen) {
            GlobalSearchDialog(
                db = db,
                onDismiss = { viewModel.setSearchOpen(false) },
                onNavigate = viewModel::navigate,
            )
        }

        if (uiState.monthCloseOpen) {
            MonthCloseDialog(
                db = db,
                onDismiss = { viewModel.setMonthCloseOpen(false) },
                onConfirmClose = viewModel::confirmMonthClose,
            )
        }

        if (quickFabOpen) {
            QuickAddFabDialog(
                db = db,
                onDismiss = { quickFabOpen = false },
                onNavigate = viewModel::navigate,
                onQuickCash = { title, amt, person -> viewModel.quickExpense(title, amt, person) },
                onQuickDebt = { creditor, item, amt ->
                    viewModel.addHomeDebt(
                        creditor = creditor,
                        item = item,
                        activity = "مواد غذائية",
                        amount = amt,
                        due = null,
                        direction = "out",
                        photo = null,
                    )
                },
            )
        }

        uiState.previewPhoto?.let { (dataUrl, title) ->
            val bmp = remember(dataUrl) { viewModel.repository.decodeDataUrlBitmap(dataUrl) }
            PhotoPreviewDialog(
                title = title,
                bitmap = bmp,
                onDismiss = viewModel::closePreviewPhoto,
            )
        }
    }
}
